package org.poc.objs.api.validation

/**
 * Outcome of audit or persist-gate validation.
 */
data class ValidationIssue(
    val code: String,
    val message: String,
    val path: String? = null,
)

data class ValidationResult(
    val issues: List<ValidationIssue> = emptyList(),
) {
    val isValid: Boolean get() = issues.isEmpty()

    fun requireValid(operation: String = "validation") {
        if (!isValid) {
            throw ValidationException(operation, this)
        }
    }

    companion object {
        fun ok(): ValidationResult = ValidationResult()
        fun of(vararg issues: ValidationIssue) = ValidationResult(issues.toList())
        fun of(issues: List<ValidationIssue>) = ValidationResult(issues)
    }
}

class ValidationException(
    val operation: String,
    val result: ValidationResult,
) : RuntimeException(
    "$operation failed: ${result.issues.joinToString("; ") { it.message }}",
)
