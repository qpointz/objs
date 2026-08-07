package org.poc.objs.core.match

import org.apache.commons.jexl3.JexlContext
import org.apache.commons.jexl3.JexlException
import org.apache.commons.jexl3.JexlExpression
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import java.util.UUID

/**
 * DSL `subgraph: { id }` — load one soft-link pack's stored members (not induced).
 */
class BoMSubgraphIdMatcher(
    val id: UUID,
) : BoMMatcher {
    override fun matches(candidate: BoMEntityMatchCandidate): Boolean = true

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromRaw(value: Any?, path: String): BoMSubgraphIdMatcher {
            if (value !is Map<*, *>) {
                fail(path, "MATCHER_DSL_SUBGRAPH_TYPE", "'subgraph' value must be an object with 'id'")
            }
            if (value.size != 1 || !value.containsKey("id")) {
                fail(path, "MATCHER_DSL_SUBGRAPH_KEYS", "'subgraph' must contain exactly key 'id'")
            }
            val text = value["id"]?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                fail("$path.id", "MATCHER_DSL_SUBGRAPH_ID", "'subgraph.id' must be a non-blank UUID string")
            }
            return try {
                BoMSubgraphIdMatcher(UUID.fromString(text))
            } catch (_: IllegalArgumentException) {
                fail("$path.id", "MATCHER_DSL_SUBGRAPH_ID_INVALID", "Invalid UUID in subgraph.id: $text")
            }
        }

        private fun fail(path: String, code: String, message: String): Nothing {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(BoMValidationIssue(code = code, message = message, path = path)),
            )
        }
    }
}

object BoMSubgExprCompile {
    fun compile(expression: String): JexlExpression {
        if (expression.isBlank()) {
            fail("MATCHER_SUBG_EXPR_EMPTY", "subg-expr must not be blank")
        }
        if (expression.length > BoMAnnoExprEngine.MAX_EXPRESSION_LENGTH) {
            fail(
                "MATCHER_SUBG_EXPR_TOO_LONG",
                "subg-expr exceeds maximum length of ${BoMAnnoExprEngine.MAX_EXPRESSION_LENGTH} characters",
            )
        }
        return try {
            BoMAnnoExprEngine.engine.createExpression(expression)
        } catch (ex: JexlException) {
            fail("MATCHER_SUBG_EXPR_SYNTAX", "Invalid subg-expr: ${ex.message}")
        }
    }

    private fun fail(code: String, message: String): Nothing {
        throw BoMValidationException(
            "subg-expr",
            BoMValidationResult.of(BoMValidationIssue(code = code, message = message, path = "subg-expr")),
        )
    }
}

/**
 * DSL `subg-expr` — JEXL over soft-link pack headers (`id`, `a` annotations).
 * Selection executor unions stored members of matching packs.
 */
class BoMSubgExprMatcher(
    val expression: String,
    private val compiled: JexlExpression = BoMSubgExprCompile.compile(expression),
) : BoMMatcher {
    override fun matches(candidate: BoMEntityMatchCandidate): Boolean = true

    fun matchesHeader(id: UUID, annotations: Map<String, String>): Boolean {
        val context = SubgExprVariableContext(id, annotations)
        val result = try {
            compiled.evaluate(context)
        } catch (ex: JexlException) {
            throw BoMValidationException(
                "subg-expr",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_SUBG_EXPR_EVAL",
                        message = "subg-expr evaluation failed: ${ex.message}",
                        path = "subg-expr",
                    ),
                ),
            )
        }
        return when (result) {
            is Boolean -> result
            null -> false
            else -> throw BoMValidationException(
                "subg-expr",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_SUBG_EXPR_TYPE",
                        message = "subg-expr must evaluate to a Boolean, got ${result::class.simpleName}",
                        path = "subg-expr",
                    ),
                ),
            )
        }
    }
}

private class SubgExprVariableContext(
    private val id: UUID,
    private val annotations: Map<String, String>,
) : JexlContext {
    override fun get(name: String): Any? =
        when (name) {
            "id" -> id.toString()
            "a" -> LinkedHashMap(annotations)
            else -> null
        }

    override fun set(name: String, value: Any?) {
        throw BoMValidationException(
            "subg-expr",
            BoMValidationResult.of(
                BoMValidationIssue(
                    code = "MATCHER_SUBG_EXPR_READONLY",
                    message = "subg-expr context is read-only",
                    path = "subg-expr",
                ),
            ),
        )
    }

    override fun has(name: String): Boolean = true
}
