package org.poc.objs.core.match

import org.poc.objs.core.typed.PayloadMapper
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

enum class BoMMatcherFormat {
    JSON,
    YAML,
}

/**
 * Parses and serializes the concise matcher DSL to/from [BoMMatcher].
 *
 * Root forms:
 * - object: one matcher
 * - array: ordered [BoMChainedMatcher]
 *
 * C-13 (graphs-from-objects) set: **`all`**, **`graph-expr`**, **`obj-expr`**, chained array.
 * D-2 addition: **`graphs-in`** (explicit graph-id set for multi-graph MI selection).
 * Older keys (`anno`, `anno-expr`, `ids`, `subgraph`, `subg-expr`) are retired — see
 * [defaultHandlers] / [RetiredMatcherKeyHandler] for the clear migrate-error each produces.
 */
class BoMMatcherDsl(
    handlers: List<BoMMatcherKeyHandler> = defaultHandlers(),
) {
    private val handlersByKey: Map<String, BoMMatcherKeyHandler> =
        handlers.associateBy { it.key }

    fun decode(content: String, format: BoMMatcherFormat): BoMMatcher {
        val root = parseRoot(content, format)
        return decodeNode(root, "$")
    }

    fun encode(matcher: BoMMatcher, format: BoMMatcherFormat = BoMMatcherFormat.JSON): String {
        val tree = encodeNode(matcher)
        return when (format) {
            BoMMatcherFormat.JSON -> jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree)
            BoMMatcherFormat.YAML -> yamlMapper.writeValueAsString(tree)
        }
    }

    fun decodeNode(node: JsonNode, path: String = "$"): BoMMatcher {
        return when {
            node.isArray -> decodeArray(node, path)
            node.isObject -> decodeObject(node, path)
            else -> fail("MATCHER_DSL_ROOT_INVALID", "Matcher DSL root must be an object or array", path)
        }
    }

    private fun decodeArray(node: JsonNode, path: String): BoMMatcher {
        if (node.isEmpty) {
            fail("MATCHER_DSL_EMPTY", "Matcher array must not be empty", path)
        }
        val children = node.mapIndexed { index, child ->
            decodeNode(child, "$path[$index]")
        }
        return BoMChainedMatcher(children)
    }

    private fun decodeObject(node: JsonNode, path: String): BoMMatcher {
        if (node.isEmpty) {
            fail("MATCHER_DSL_EMPTY", "Matcher object must not be empty", path)
        }
        val fields = node.properties().toList()
        if (fields.size != 1) {
            fail(
                "MATCHER_DSL_MULTI_KEY",
                "Matcher object must contain exactly one key, found: ${fields.map { it.key }}",
                path,
            )
        }
        val (key, value) = fields.first()
        val handler = handlersByKey[key]
            ?: fail("MATCHER_DSL_UNKNOWN_KEY", "Unknown matcher key '$key'", "$path.$key")
        return handler.decode(jacksonToKotlin(value), "$path.$key")
    }

    private fun encodeNode(matcher: BoMMatcher): JsonNode = when (matcher) {
        is BoMChainedMatcher -> {
            val array = jsonMapper.createArrayNode()
            matcher.matchers.forEach { child -> array.add(encodeNode(child)) }
            array
        }
        is BoMAllGraphsMatcher -> {
            jsonMapper.createObjectNode().put("all", true)
        }
        is BoMGraphIdsMatcher -> {
            val arr = jsonMapper.createArrayNode()
            matcher.graphIds.forEach { arr.add(it.toString()) }
            jsonMapper.createObjectNode().set("graphs-in", arr)
        }
        is BoMGraphExprMatcher -> {
            jsonMapper.createObjectNode().put("graph-expr", matcher.expression)
        }
        is BoMObjExprMatcher -> {
            jsonMapper.createObjectNode().put("obj-expr", matcher.expression)
        }
        else -> fail(
            "MATCHER_DSL_ENCODE_UNSUPPORTED",
            "Cannot encode matcher type ${matcher::class.simpleName}",
            "$",
        )
    }

    private fun parseRoot(content: String, format: BoMMatcherFormat): JsonNode {
        if (content.isBlank()) {
            fail("MATCHER_DSL_EMPTY", "Matcher DSL body must not be blank", "$")
        }
        return try {
            when (format) {
                BoMMatcherFormat.JSON -> jsonMapper.readTree(content)
                BoMMatcherFormat.YAML -> yamlMapper.readTree(content)
            }
        } catch (ex: Exception) {
            fail(
                "MATCHER_DSL_PARSE",
                "Failed to parse matcher DSL as ${format.name.lowercase()}: ${ex.message}",
                "$",
            )
        }
    }

    private fun jacksonToKotlin(node: JsonNode): Any? = when {
        node.isNull -> null
        node.isTextual -> node.asString()
        node.isBoolean -> node.asBoolean()
        node.isIntegralNumber -> node.asLong()
        node.isFloatingPointNumber -> node.asDouble()
        node.isArray -> {
            val list = ArrayList<Any?>(node.size())
            for (i in 0 until node.size()) {
                list.add(jacksonToKotlin(node.get(i)))
            }
            list
        }
        node.isObject -> {
            val map = linkedMapOf<String, Any?>()
            node.properties().forEach { (k, v) -> map[k] = jacksonToKotlin(v) }
            map
        }
        else -> node.toString()
    }

    private fun fail(code: String, message: String, path: String): Nothing {
        throw BoMValidationException(
            "matcher-dsl",
            BoMValidationResult.of(BoMValidationIssue(code = code, message = message, path = path)),
        )
    }

    companion object {
        private val jsonMapper: JsonMapper = PayloadMapper.mapper
        private val yamlMapper: YAMLMapper = YAMLMapper.builder()
            .addModule(kotlinModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        /**
         * C-13 set: `all` + `graph-expr` + `obj-expr`, plus [RetiredMatcherKeyHandler] entries for
         * the retired `anno` / `anno-expr` / `ids` / `subgraph` / `subg-expr` keys so callers get a
         * clear migrate error instead of a generic unknown-key failure.
         * D-2: `graphs-in` for an explicit graph-id set.
         */
        fun defaultHandlers(
            objExprFactory: (String) -> BoMMatcher = { expression -> BoMObjExprMatcher(expression) },
            graphExprFactory: (String) -> BoMMatcher = { expression -> BoMGraphExprMatcher(expression) },
        ): List<BoMMatcherKeyHandler> = listOf(
            AllGraphsMatcherHandler,
            GraphIdsMatcherHandler,
            GraphExprMatcherHandler(graphExprFactory),
            ObjExprMatcherHandler(objExprFactory),
            RetiredMatcherKeyHandler(
                key = "anno",
                migrateTo = "'obj-expr' (e.g. obj-expr: \"a.k == 'v' && a.k2 == 'v2'\")",
            ),
            RetiredMatcherKeyHandler(
                key = "anno-expr",
                migrateTo = "'obj-expr' (annotation keys now live under a.*, e.g. obj-expr: \"a.k == 'v'\")",
            ),
            RetiredMatcherKeyHandler(
                key = "ids",
                migrateTo = "'obj-expr' (e.g. obj-expr: \"id == '...' || id == '...'\") or 'graphs-in' for graph ids",
            ),
            RetiredMatcherKeyHandler(
                key = "subgraph",
                migrateTo = "'graph-expr' (e.g. graph-expr: \"id == '<uuid>'\") or 'graphs-in'",
            ),
            RetiredMatcherKeyHandler(
                key = "subg-expr",
                migrateTo = "'graph-expr' (same id/a header bindings) or 'graphs-in'",
            ),
        )

        fun create(
            objExprFactory: (String) -> BoMMatcher = { expression -> BoMObjExprMatcher(expression) },
            graphExprFactory: (String) -> BoMMatcher = { expression -> BoMGraphExprMatcher(expression) },
        ): BoMMatcherDsl = BoMMatcherDsl(defaultHandlers(objExprFactory, graphExprFactory))
    }
}

/**
 * DSL key **`all`**: value must be boolean `true` → [BoMAllGraphsMatcher].
 */
object AllGraphsMatcherHandler : BoMMatcherKeyHandler {
    override val key: String = "all"

    override fun decode(value: Any?, path: String): BoMMatcher {
        if (value != true) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_DSL_ALL_TYPE",
                        message = "'all' value must be boolean true (e.g. {\"all\": true})",
                        path = path,
                    ),
                ),
            )
        }
        return BoMAllGraphsMatcher
    }
}

/**
 * DSL key **`graphs-in`**: non-null JSON/YAML array of UUID strings → [BoMGraphIdsMatcher].
 * Empty array is allowed (empty selection).
 */
object GraphIdsMatcherHandler : BoMMatcherKeyHandler {
    override val key: String = "graphs-in"

    override fun decode(value: Any?, path: String): BoMMatcher {
        val raw: List<*> = when (value) {
            null -> {
                failType(path)
            }
            is List<*> -> value
            is Array<*> -> value.toList()
            else -> {
                failType(path)
            }
        }
        val ids = raw.mapIndexed { index, item ->
            parseUuid(item, "$path[$index]")
        }
        return BoMGraphIdsMatcher(ids)
    }

    private fun parseUuid(item: Any?, path: String): UUID {
        val text = when (item) {
            is UUID -> return item
            is String -> item.trim()
            else -> null
        }
        if (text.isNullOrBlank()) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_DSL_GRAPHS_IN_ITEM",
                        message = "'graphs-in' items must be UUID strings",
                        path = path,
                    ),
                ),
            )
        }
        return try {
            UUID.fromString(text)
        } catch (_: IllegalArgumentException) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_DSL_GRAPHS_IN_UUID",
                        message = "'graphs-in' item is not a valid UUID: $text",
                        path = path,
                    ),
                ),
            )
        }
    }

    private fun failType(path: String): Nothing {
        throw BoMValidationException(
            "matcher-dsl",
            BoMValidationResult.of(
                BoMValidationIssue(
                    code = "MATCHER_DSL_GRAPHS_IN_TYPE",
                    message = "'graphs-in' value must be an array of UUID strings " +
                        "(e.g. {\"graphs-in\": [\"…\", \"…\"]})",
                    path = path,
                ),
            ),
        )
    }
}

class GraphExprMatcherHandler(
    private val factory: (String) -> BoMMatcher,
) : BoMMatcherKeyHandler {
    override val key: String = "graph-expr"

    override fun decode(value: Any?, path: String): BoMMatcher {
        if (value !is String || value.isBlank()) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_DSL_GRAPH_EXPR_TYPE",
                        message = "'graph-expr' value must be a non-blank string expression",
                        path = path,
                    ),
                ),
            )
        }
        return factory(value)
    }
}

class ObjExprMatcherHandler(
    private val factory: (String) -> BoMMatcher,
) : BoMMatcherKeyHandler {
    override val key: String = "obj-expr"

    override fun decode(value: Any?, path: String): BoMMatcher {
        if (value !is String || value.isBlank()) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_DSL_OBJ_EXPR_TYPE",
                        message = "'obj-expr' value must be a non-blank string expression",
                        path = path,
                    ),
                ),
            )
        }
        return factory(value)
    }
}

/**
 * Handles a DSL key retired in C-13 (graphs-from-objects): `anno`, `anno-expr`, `ids`,
 * `subgraph`, `subg-expr`. Always fails with a clear `MATCHER_DSL_RETIRED_KEY` migrate message
 * instead of the generic `MATCHER_DSL_UNKNOWN_KEY` (G-G17).
 */
class RetiredMatcherKeyHandler(
    override val key: String,
    private val migrateTo: String,
) : BoMMatcherKeyHandler {
    override fun decode(value: Any?, path: String): BoMMatcher {
        throw BoMValidationException(
            "matcher-dsl",
            BoMValidationResult.of(
                BoMValidationIssue(
                    code = "MATCHER_DSL_RETIRED_KEY",
                    message = "Matcher key '$key' was retired in C-13 (graphs-from-objects); migrate to $migrateTo",
                    path = path,
                ),
            ),
        )
    }
}
