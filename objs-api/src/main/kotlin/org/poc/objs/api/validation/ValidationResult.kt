package org.poc.objs.api.validation

import java.util.UUID

/**
 * Outcome of audit or persist-gate validation.
 */
enum class ValidationSubjectKind {
    ENTITY,
    EDGE,
    GRAPH,
    OTHER,
}

/**
 * Addressable failing subject. Redundancy OK (id + index + document).
 */
data class ValidationSubject(
    val kind: ValidationSubjectKind,
    val id: UUID? = null,
    val index: Int? = null,
    val type: String? = null,
    val schemaVersion: String? = null,
    /** Entity payload or edge properties when that document was validated. */
    val document: Map<String, Any?>? = null,
    val role: String? = null,
    val sourceId: UUID? = null,
    val targetId: UUID? = null,
)

enum class ValidationLocus {
    ENTITY_PAYLOAD,
    EDGE_PROPERTIES,
    EDGE_ALLOWLIST,
    IDENTITY,
    MEMBERSHIP,
    OTHER,
}

data class AllowedEdgeKey(
    val sourceType: String,
    val role: String,
    val targetType: String,
)

/**
 * Where in the catalog / rule the check failed — addressable without parsing [ValidationIssue.path].
 */
data class ValidationSchemaRef(
    val locus: ValidationLocus,
    val schemaType: String? = null,
    val schemaVersion: String? = null,
    /** JSON Pointer inside [ValidationSubject.document] (e.g. `/name`). */
    val fieldPath: String? = null,
    val allowedEdge: AllowedEdgeKey? = null,
)

data class ValidationIssue(
    val code: String,
    val message: String,
    val path: String? = null,
    val subject: ValidationSubject? = null,
    val schema: ValidationSchemaRef? = null,
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
