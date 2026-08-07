package org.poc.objs.core.match

import org.apache.commons.jexl3.JexlContext
import org.apache.commons.jexl3.JexlException
import org.apache.commons.jexl3.JexlExpression
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import java.util.UUID

/**
 * Compiles `obj-expr` using the shared [BoMAnnoExprEngine] sandbox; error codes are `MATCHER_OBJ_EXPR_*`.
 */
object BoMObjExprCompile {
    fun compile(expression: String): JexlExpression {
        if (expression.isBlank()) {
            fail("MATCHER_OBJ_EXPR_EMPTY", "obj-expr must not be blank")
        }
        if (expression.length > BoMAnnoExprEngine.MAX_EXPRESSION_LENGTH) {
            fail(
                "MATCHER_OBJ_EXPR_TOO_LONG",
                "obj-expr exceeds maximum length of ${BoMAnnoExprEngine.MAX_EXPRESSION_LENGTH} characters",
            )
        }
        return try {
            BoMAnnoExprEngine.engine.createExpression(expression)
        } catch (ex: JexlException) {
            fail("MATCHER_OBJ_EXPR_SYNTAX", "Invalid obj-expr: ${ex.message}")
        }
    }

    private fun fail(code: String, message: String): Nothing {
        throw BoMValidationException(
            "obj-expr",
            BoMValidationResult.of(BoMValidationIssue(code = code, message = message, path = "obj-expr")),
        )
    }
}

/**
 * Object expression matcher (DSL `obj-expr`).
 *
 * JEXL bindings: `id`, `type`, `schemaVersion`, `a` (annotations map), `p` (payload map).
 * Pushdown when the expression lowers to equality/`&&` over those fields (see [BoMObjExprLowerer]).
 */
class BoMObjExprMatcher(
    val expression: String,
    private val compiled: JexlExpression = BoMObjExprCompile.compile(expression),
) : BoMMatcher, BoMSourceCapableMatcher {

    val pushdown: BoMObjExprPushdown? = BoMObjExprLowerer.toPushdown(compiled)

    val localEvalOnly: Boolean
        get() = pushdown == null

    override fun toCandidateSource(backend: BoMEntityCandidateBackend): BoMCandidateSource? {
        val plan = pushdown ?: return null
        return backend.objExprPushdownSource(plan)
    }

    override fun matches(candidate: BoMEntityMatchCandidate): Boolean {
        val context = ObjExprVariableContext(candidate)
        val result = try {
            compiled.evaluate(context)
        } catch (ex: JexlException) {
            throw BoMValidationException(
                "obj-expr",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_OBJ_EXPR_EVAL",
                        message = "obj-expr evaluation failed: ${ex.message}",
                        path = "obj-expr",
                    ),
                ),
            )
        }
        return when (result) {
            is Boolean -> result
            null -> false
            else -> throw BoMValidationException(
                "obj-expr",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_OBJ_EXPR_TYPE",
                        message = "obj-expr must evaluate to a Boolean, got ${result::class.simpleName}",
                        path = "obj-expr",
                    ),
                ),
            )
        }
    }
}

/** Equality/`&&` pushdown plan for [BoMObjExprMatcher] (no OR across heterogeneous fields). */
data class BoMObjExprPushdown(
    val typeEquals: String? = null,
    val idEquals: UUID? = null,
    val schemaVersionEquals: String? = null,
    val annotationEquals: Map<String, String> = emptyMap(),
    val payloadEquals: Map<String, String> = emptyMap(),
) {
    init {
        require(
            typeEquals != null ||
                idEquals != null ||
                schemaVersionEquals != null ||
                annotationEquals.isNotEmpty() ||
                payloadEquals.isNotEmpty(),
        ) { "obj-expr pushdown must constrain at least one field" }
    }
}

private class ObjExprVariableContext(
    private val candidate: BoMEntityMatchCandidate,
) : JexlContext {
    /**
     * JEXL [JexlPermissions.RESTRICTED] allows map-key property access on `java.util.Map`
     * implementations from the JDK, but not on custom maps such as [org.poc.objs.core.persistence.LazyJsonMap].
     * Copy into [LinkedHashMap] so `a.app` / `p.name` (and bracket forms) work under local eval.
     */
    override fun get(name: String): Any? =
        when (name) {
            "id" -> candidate.id?.toString()
            "type" -> candidate.type
            "schemaVersion" -> candidate.schemaVersion
            "a" -> LinkedHashMap(candidate.annotations)
            "p" -> LinkedHashMap(candidate.payload)
            else -> null
        }

    override fun set(name: String, value: Any?) {
        throw BoMValidationException(
            "obj-expr",
            BoMValidationResult.of(
                BoMValidationIssue(
                    code = "MATCHER_OBJ_EXPR_READONLY",
                    message = "obj-expr context is read-only",
                    path = "obj-expr",
                ),
            ),
        )
    }

    override fun has(name: String): Boolean = true
}
