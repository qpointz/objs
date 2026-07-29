package org.poc.objs.core.seed

import org.poc.objs.core.validation.BoMValidationIssue

const val SEED_API_VERSION_V1 = "objs.poc.org/v1"

const val SEED_KIND_OBJECT_SCHEMA = "ObjectSchema"
const val SEED_KIND_ALLOWED_EDGE_RULE = "AllowedEdgeRule"
const val SEED_KIND_GRAPH = "Graph"

/** Parsed multi-document seed envelope before kind-specific validation. */
data class SeedRawDocument(
    val index: Int,
    val apiVersion: String?,
    val kind: String?,
    val metadata: Map<String, Any?> = emptyMap(),
    val spec: Map<String, Any?> = emptyMap(),
    val raw: Map<String, Any?>,
)

data class SeedDocumentResult(
    val index: Int,
    val kind: String?,
    val apiVersion: String?,
    val identity: String? = null,
    val applied: Boolean = false,
    val skipped: Boolean = false,
    val errors: List<BoMValidationIssue> = emptyList(),
    val warnings: List<String> = emptyList(),
)

data class SeedImportResult(
    val documents: List<SeedDocumentResult> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    val isSuccess: Boolean
        get() = !hasErrors

    val hasErrors: Boolean
        get() = documents.any { it.errors.isNotEmpty() }

    fun appliedByKind(): Map<String, Int> =
        documents.filter { it.applied && it.kind != null }
            .groupingBy { it.kind!! }
            .eachCount()

    fun skippedByKind(): Map<String, Int> =
        documents.filter { it.skipped && it.kind != null }
            .groupingBy { it.kind!! }
            .eachCount()

    fun allErrors(): List<BoMValidationIssue> = documents.flatMap { it.errors }

    companion object {
        fun empty(): SeedImportResult = SeedImportResult()
    }
}

class SeedImportException(
    message: String,
    val result: SeedImportResult,
) : RuntimeException(message)
