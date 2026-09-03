package org.poc.objs.api.match

import org.apache.commons.jexl3.JexlContext
import org.apache.commons.jexl3.JexlException
import org.apache.commons.jexl3.JexlExpression
import org.poc.objs.api.validation.ValidationException
import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.api.validation.ValidationResult
import java.util.UUID

/**
 * Compiles `obj-expr` using the shared [AnnoExprEngine] sandbox; error codes are `MATCHER_OBJ_EXPR_*`.
 */
object ObjExprCompile {
    fun compile(expression: String): JexlExpression {
        if (expression.isBlank()) {
            fail("MATCHER_OBJ_EXPR_EMPTY", "obj-expr must not be blank")
        }
        if (expression.length > AnnoExprEngine.MAX_EXPRESSION_LENGTH) {
            fail(
                "MATCHER_OBJ_EXPR_TOO_LONG",
                "obj-expr exceeds maximum length of ${AnnoExprEngine.MAX_EXPRESSION_LENGTH} characters",
            )
        }
        return try {
            AnnoExprEngine.engine.createExpression(expression)
        } catch (ex: JexlException) {
            fail("MATCHER_OBJ_EXPR_SYNTAX", "Invalid obj-expr: ${ex.message}")
        }
    }

    private fun fail(code: String, message: String): Nothing {
        throw ValidationException(
            "obj-expr",
            ValidationResult.of(ValidationIssue(code = code, message = message, path = "obj-expr")),
        )
    }
}

/**
 * Object expression matcher (DSL `obj-expr`).
 *
 * JEXL bindings: `id`, `type`, `schemaVersion`, `a` (annotations map), `p` (payload map).
 * Pushdown when the expression lowers to supported predicates (`==`/`!=`, ordered compares,
 * anchored prefix `=~ '^…'`) with `&&`/`||` over those fields (see [ObjExprLowerer]).
 */
class ObjExprMatcher(
    val expression: String,
    private val compiled: JexlExpression = ObjExprCompile.compile(expression),
) : Matcher, SourceCapableMatcher {

    val pushdown: ObjExprPushdown? = ObjExprLowerer.toPushdown(compiled)

    val localEvalOnly: Boolean
        get() = pushdown == null

    override fun toCandidateSource(backend: EntityCandidateBackend): CandidateSource? {
        val plan = pushdown ?: return null
        return backend.objExprPushdownSource(plan)
    }

    override fun matches(candidate: EntityMatchCandidate): Boolean {
        val context = ObjExprVariableContext(candidate)
        val result = try {
            compiled.evaluate(context)
        } catch (ex: JexlException) {
            throw ValidationException(
                "obj-expr",
                ValidationResult.of(
                    ValidationIssue(
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
            else -> throw ValidationException(
                "obj-expr",
                ValidationResult.of(
                    ValidationIssue(
                        code = "MATCHER_OBJ_EXPR_TYPE",
                        message = "obj-expr must evaluate to a Boolean, got ${result::class.simpleName}",
                        path = "obj-expr",
                    ),
                ),
            )
        }
    }
}

/** DNF pushdown plan for [ObjExprMatcher] (equality, compares, prefix on supported fields). */
data class ObjExprPushdown(
    val dnf: List<ObjExprAndGroup>,
) {
    /** Convenience when the plan is a single conjunction (common equality/`&&` case). */
    val typeEquals: String? get() = dnf.singleOrNull()?.typeEquals
    val idEquals: UUID? get() = dnf.singleOrNull()?.idEquals
    val schemaVersionEquals: String? get() = dnf.singleOrNull()?.schemaVersionEquals
    val annotationEquals: Map<String, String> get() = dnf.singleOrNull()?.annotationEquals.orEmpty()
    val payloadEquals: Map<String, String> get() = dnf.singleOrNull()?.payloadEquals.orEmpty()

    val needsJsonbContainment: Boolean
        get() = dnf.any { it.annotationEquals.isNotEmpty() || it.payloadEquals.isNotEmpty() }

    val needsPayloadScalarPredicates: Boolean
        get() =
            dnf.any { group ->
                group.payloadNotEquals.isNotEmpty() ||
                    group.payloadGt.isNotEmpty() ||
                    group.payloadGe.isNotEmpty() ||
                    group.payloadLt.isNotEmpty() ||
                    group.payloadLe.isNotEmpty() ||
                    group.payloadPrefix.isNotEmpty()
            }

    val isUnsatisfiable: Boolean get() = dnf.isEmpty()
}

/** One AND-group inside an [ObjExprPushdown] DNF. */
data class ObjExprAndGroup(
    val typeEquals: String? = null,
    val typeNotEquals: Set<String> = emptySet(),
    val idEquals: UUID? = null,
    val idNotEquals: Set<UUID> = emptySet(),
    val schemaVersionEquals: String? = null,
    val schemaVersionNotEquals: Set<String> = emptySet(),
    val annotationEquals: Map<String, String> = emptyMap(),
    val annotationNotEquals: Map<String, String> = emptyMap(),
    val payloadEquals: Map<String, String> = emptyMap(),
    val payloadNotEquals: Map<String, String> = emptyMap(),
    val payloadGt: Map<String, String> = emptyMap(),
    val payloadGe: Map<String, String> = emptyMap(),
    val payloadLt: Map<String, String> = emptyMap(),
    val payloadLe: Map<String, String> = emptyMap(),
    val payloadPrefix: Map<String, String> = emptyMap(),
) {
    val hasConstraint: Boolean
        get() =
            typeEquals != null ||
                typeNotEquals.isNotEmpty() ||
                idEquals != null ||
                idNotEquals.isNotEmpty() ||
                schemaVersionEquals != null ||
                schemaVersionNotEquals.isNotEmpty() ||
                annotationEquals.isNotEmpty() ||
                annotationNotEquals.isNotEmpty() ||
                payloadEquals.isNotEmpty() ||
                payloadNotEquals.isNotEmpty() ||
                payloadGt.isNotEmpty() ||
                payloadGe.isNotEmpty() ||
                payloadLt.isNotEmpty() ||
                payloadLe.isNotEmpty() ||
                payloadPrefix.isNotEmpty()
}

private class ObjExprVariableContext(
    private val candidate: EntityMatchCandidate,
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
        throw ValidationException(
            "obj-expr",
            ValidationResult.of(
                ValidationIssue(
                    code = "MATCHER_OBJ_EXPR_READONLY",
                    message = "obj-expr context is read-only",
                    path = "obj-expr",
                ),
            ),
        )
    }

    override fun has(name: String): Boolean = true
}
