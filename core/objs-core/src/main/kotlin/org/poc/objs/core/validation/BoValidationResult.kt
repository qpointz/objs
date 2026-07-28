package org.poc.objs.core.validation

/**
 * Outcome of audit or persist-gate validation.
 */
data class BoValidationIssue(
    val code: String,
    val message: String,
    val path: String? = null,
)

data class BoValidationResult(
    val issues: List<BoValidationIssue> = emptyList(),
) {
    val isValid: Boolean get() = issues.isEmpty()

    fun requireValid(operation: String = "validation") {
        if (!isValid) {
            throw BoValidationException(operation, this)
        }
    }

    companion object {
        fun ok(): BoValidationResult = BoValidationResult()
        fun of(vararg issues: BoValidationIssue) = BoValidationResult(issues.toList())
        fun of(issues: List<BoValidationIssue>) = BoValidationResult(issues)
    }
}

class BoValidationException(
    val operation: String,
    val result: BoValidationResult,
) : RuntimeException(
    "$operation failed: ${result.issues.joinToString("; ") { it.message }}",
)
