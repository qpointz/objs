package org.poc.objs.core.match

import org.apache.commons.jexl3.JexlContext
import org.apache.commons.jexl3.JexlException
import org.apache.commons.jexl3.JexlExpression
import org.poc.objs.core.validation.ValidationException
import org.poc.objs.core.validation.ValidationIssue
import org.poc.objs.core.validation.ValidationResult
import java.util.UUID

/**
 * Compiles `graph-expr` using the shared [AnnoExprEngine] sandbox; error codes are `MATCHER_GRAPH_EXPR_*`.
 */
object GraphExprCompile {
    fun compile(expression: String): JexlExpression {
        if (expression.isBlank()) {
            fail("MATCHER_GRAPH_EXPR_EMPTY", "graph-expr must not be blank")
        }
        if (expression.length > AnnoExprEngine.MAX_EXPRESSION_LENGTH) {
            fail(
                "MATCHER_GRAPH_EXPR_TOO_LONG",
                "graph-expr exceeds maximum length of ${AnnoExprEngine.MAX_EXPRESSION_LENGTH} characters",
            )
        }
        return try {
            AnnoExprEngine.engine.createExpression(expression)
        } catch (ex: JexlException) {
            fail("MATCHER_GRAPH_EXPR_SYNTAX", "Invalid graph-expr: ${ex.message}")
        }
    }

    private fun fail(code: String, message: String): Nothing {
        throw ValidationException(
            "graph-expr",
            ValidationResult.of(ValidationIssue(code = code, message = message, path = "graph-expr")),
        )
    }
}

/**
 * DSL `graph-expr` (evolution of the former `subg-expr`) — JEXL over graph headers
 * (`id`, `a` annotations). Selection executor unions **stored** members + graph-local edges
 * of matching graph(s); it never induces edges over the whole pool (G-G15/G-G16).
 *
 * When the expression lowers to equality/inequality with `&&`/`||` over `id` / `a.*`
 * (see [GraphExprLowerer]), Postgres may push it down via PK / `annotations @>` / `->>`;
 * otherwise headers are filtered in memory after a table scan.
 */
class GraphExprMatcher(
    val expression: String,
    private val compiled: JexlExpression = GraphExprCompile.compile(expression),
) : Matcher {
    val pushdown: GraphExprPushdown? = GraphExprLowerer.toPushdown(compiled)

    val localEvalOnly: Boolean
        get() = pushdown == null

    /** Header matchers do not filter individual entity candidates directly. */
    override fun matches(candidate: EntityMatchCandidate): Boolean = true

    fun matchesHeader(id: UUID, annotations: Map<String, String>): Boolean {
        val context = GraphExprVariableContext(id, annotations)
        val result = try {
            compiled.evaluate(context)
        } catch (ex: JexlException) {
            throw ValidationException(
                "graph-expr",
                ValidationResult.of(
                    ValidationIssue(
                        code = "MATCHER_GRAPH_EXPR_EVAL",
                        message = "graph-expr evaluation failed: ${ex.message}",
                        path = "graph-expr",
                    ),
                ),
            )
        }
        return when (result) {
            is Boolean -> result
            null -> false
            else -> throw ValidationException(
                "graph-expr",
                ValidationResult.of(
                    ValidationIssue(
                        code = "MATCHER_GRAPH_EXPR_TYPE",
                        message = "graph-expr must evaluate to a Boolean, got ${result::class.simpleName}",
                        path = "graph-expr",
                    ),
                ),
            )
        }
    }
}

private class GraphExprVariableContext(
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
        throw ValidationException(
            "graph-expr",
            ValidationResult.of(
                ValidationIssue(
                    code = "MATCHER_GRAPH_EXPR_READONLY",
                    message = "graph-expr context is read-only",
                    path = "graph-expr",
                ),
            ),
        )
    }

    override fun has(name: String): Boolean = true
}
