package org.poc.objs.core.validation

/**
 * Outcome of audit or persist-gate validation.
 */
data class BoMValidationIssue(
    val code: String,
    val message: String,
    val path: String? = null,
)

data class BoMValidationResult(
    val issues: List<BoMValidationIssue> = emptyList(),
) {
    val isValid: Boolean get() = issues.isEmpty()

    fun requireValid(operation: String = "validation") {
        if (!isValid) {
            throw BoMValidationException(operation, this)
        }
    }

    companion object {
        fun ok(): BoMValidationResult = BoMValidationResult()
        fun of(vararg issues: BoMValidationIssue) = BoMValidationResult(issues.toList())
        fun of(issues: List<BoMValidationIssue>) = BoMValidationResult(issues)
    }
}

class BoMValidationException(
    val operation: String,
    val result: BoMValidationResult,
) : RuntimeException(
    "$operation failed: ${result.issues.joinToString("; ") { it.message }}",
)
