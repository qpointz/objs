package org.poc.objs.core.match

import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.JexlContext
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.JexlException
import org.apache.commons.jexl3.JexlExpression
import org.apache.commons.jexl3.JexlFeatures
import org.apache.commons.jexl3.introspection.JexlPermissions
import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Shared, immutable, default-deny JEXL engine for annotation-only predicates.
 */
object BoMAnnoExprEngine {
    const val MAX_EXPRESSION_LENGTH = 4 * 1024
    const val CACHE_SIZE = 256

    val engine: JexlEngine = JexlBuilder()
        .features(
            JexlFeatures()
                .script(false)
                .methodCall(false)
                .lambda(false)
                .loops(false)
                .sideEffect(false)
                .sideEffectGlobal(false)
                .newInstance(false)
                .annotation(false)
                .pragma(false)
                .namespacePragma(false)
                .namespaceIdentifier(false)
                .localVar(false)
                .register(false),
        )
        .permissions(JexlPermissions.RESTRICTED)
        .cache(CACHE_SIZE)
        .strict(true)
        .silent(false)
        .create()

    fun compile(expression: String): JexlExpression {
        if (expression.isBlank()) {
            fail("MATCHER_ANNO_EXPR_EMPTY", "anno-expr must not be blank")
        }
        if (expression.length > MAX_EXPRESSION_LENGTH) {
            fail(
                "MATCHER_ANNO_EXPR_TOO_LONG",
                "anno-expr exceeds maximum length of $MAX_EXPRESSION_LENGTH characters",
            )
        }
        return try {
            engine.createExpression(expression)
        } catch (ex: JexlException) {
            fail("MATCHER_ANNO_EXPR_SYNTAX", "Invalid anno-expr: ${ex.message}")
        }
    }

    private fun fail(code: String, message: String): Nothing {
        throw BoMValidationException(
            "anno-expr",
            BoMValidationResult.of(BoMValidationIssue(code = code, message = message, path = "anno-expr")),
        )
    }
}

/**
 * Annotation expression matcher (DSL `anno-expr`) backed by the shared JEXL engine.
 *
 * Each annotation map entry is bound as a top-level variable, for example:
 * `version == '1.0.0' && app == 'aapp-lala'`.
 *
 * Execution:
 * - If the JEXL AST lowers to equality with `&&` / `||`, [toCandidateSource] may return a Postgres
 *   JSONB `@>` source (single map or OR of maps after DNF).
 * - If it cannot be converted to SQL ([sqlContainmentDisjuncts] is null), or the backend
 *   rejects the source, executors switch to **local eval mode**: all-entities scan +
 *   [matches] (JEXL) via [BoMEntitySelectionPlan].
 */
class BoMAnnoExprMatcher(
    val expression: String,
    private val compiled: JexlExpression = BoMAnnoExprEngine.compile(expression),
) : BoMMatcher, BoMSourceCapableMatcher {

    /**
     * DNF containment maps for SQL pushdown; null means local JEXL evaluation only.
     */
    val sqlContainmentDisjuncts: List<Map<String, String>>? =
        BoMAnnoExprLowerer.toContainmentDisjuncts(compiled)

    /** Single-map convenience when there is exactly one disjunct (match-all shape). */
    val sqlContainmentFilter: Map<String, String>?
        get() = sqlContainmentDisjuncts?.singleOrNull()

    /** True when the expression cannot be converted to a containment SQL source. */
    val localEvalOnly: Boolean
        get() = sqlContainmentDisjuncts == null

    override fun toCandidateSource(backend: BoMEntityCandidateBackend): BoMCandidateSource? {
        val disjuncts = sqlContainmentDisjuncts ?: return null
        return backend.annotationContainmentAnySource(disjuncts)
    }

    override fun matches(candidate: BoMEntityMatchCandidate): Boolean {
        val vars = LinkedHashMap<String, Any?>(candidate.annotations.size)
        candidate.annotations.forEach { (key, value) -> vars[key] = value }
        val context = AnnotationVariableContext(Collections.unmodifiableMap(vars))
        val result = try {
            compiled.evaluate(context)
        } catch (ex: JexlException) {
            throw BoMValidationException(
                "anno-expr",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_ANNO_EXPR_EVAL",
                        message = "anno-expr evaluation failed: ${ex.message}",
                        path = "anno-expr",
                    ),
                ),
            )
        }
        return when (result) {
            is Boolean -> result
            null -> false
            else -> throw BoMValidationException(
                "anno-expr",
                BoMValidationResult.of(
                    BoMValidationIssue(
                        code = "MATCHER_ANNO_EXPR_TYPE",
                        message = "anno-expr must evaluate to a Boolean, got ${result::class.simpleName}",
                        path = "anno-expr",
                    ),
                ),
            )
        }
    }
}

/**
 * Read-only context that exposes annotation keys as variables and returns null for missing keys.
 */
private class AnnotationVariableContext(
    private val annotations: Map<String, Any?>,
) : JexlContext {
    override fun get(name: String): Any? = annotations[name]

    override fun set(name: String, value: Any?) {
        throw BoMValidationException(
            "anno-expr",
            BoMValidationResult.of(
                BoMValidationIssue(
                    code = "MATCHER_ANNO_EXPR_READONLY",
                    message = "anno-expr context is read-only",
                    path = "anno-expr",
                ),
            ),
        )
    }

    override fun has(name: String): Boolean = true
}
