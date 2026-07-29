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
        is MatchAllAnnotationMatcher -> {
            jsonMapper.createObjectNode().set("anno", jsonMapper.valueToTree(matcher.filter))
        }
        is BoMAnnoExprMatcher -> {
            jsonMapper.createObjectNode().put("anno-expr", matcher.expression)
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
        node.isArray -> node.map { jacksonToKotlin(it) }
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

        fun defaultHandlers(
            annoExprFactory: (String) -> BoMMatcher = { expression -> BoMAnnoExprMatcher(expression) },
        ): List<BoMMatcherKeyHandler> = listOf(
            AnnoMatcherHandler,
            AnnoExprMatcherHandler(annoExprFactory),
        )

        fun create(
            annoExprFactory: (String) -> BoMMatcher = { expression -> BoMAnnoExprMatcher(expression) },
        ): BoMMatcherDsl = BoMMatcherDsl(defaultHandlers(annoExprFactory))
    }
}

object AnnoMatcherHandler : BoMMatcherKeyHandler {
    override val key: String = "anno"

    @Suppress("UNCHECKED_CAST")
    override fun decode(value: Any?, path: String): BoMMatcher {
        if (value !is Map<*, *>) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_DSL_ANNO_TYPE",
                        message = "'anno' value must be an object of string key/value pairs",
                        path = path,
                    ),
                ),
            )
        }
        if (value.isEmpty()) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_DSL_ANNO_EMPTY",
                        message = "'anno' filter must not be empty",
                        path = path,
                    ),
                ),
            )
        }
        val filter = linkedMapOf<String, String>()
        value.forEach { (rawKey, rawValue) ->
            val key = rawKey?.toString()
                ?: throw BoMValidationException(
                    "matcher-dsl",
                    BoMValidationResult.of(
                        BoMValidationIssue(
                            code = "MATCHER_DSL_ANNO_KEY",
                            message = "'anno' keys must be strings",
                            path = path,
                        ),
                    ),
                )
            if (rawValue == null) {
                throw BoMValidationException(
                    "matcher-dsl",
                    BoMValidationResult.of(
                        BoMValidationIssue(
                            code = "MATCHER_DSL_ANNO_VALUE",
                            message = "'anno' values must be strings",
                            path = "$path.$key",
                        ),
                    ),
                )
            }
            filter[key] = rawValue.toString()
        }
        return MatchAllAnnotationMatcher(filter)
    }
}

class AnnoExprMatcherHandler(
    private val factory: (String) -> BoMMatcher,
) : BoMMatcherKeyHandler {
    override val key: String = "anno-expr"

    override fun decode(value: Any?, path: String): BoMMatcher {
        if (value !is String || value.isBlank()) {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_DSL_ANNO_EXPR_TYPE",
                        message = "'anno-expr' value must be a non-blank string expression",
                        path = path,
                    ),
                ),
            )
        }
        return factory(value)
    }
}
