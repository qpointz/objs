package org.poc.objs.api.seed

import org.poc.objs.api.validation.ValidationIssue

const val SEED_API_VERSION_V1 = "objs.poc.org/v1"

const val SEED_KIND_OBJECT_SCHEMA = "ObjectSchema"
const val SEED_KIND_ALLOWED_EDGE_RULE = "AllowedEdgeRule"
const val SEED_KIND_GRAPH = "Graph"

/** Catalog (ontology) kinds for registry seed I/O. */
val CATALOG_SEED_KINDS: Set<String> = setOf(SEED_KIND_OBJECT_SCHEMA, SEED_KIND_ALLOWED_EDGE_RULE)

/** Graph instance kinds for graph seed I/O. */
val GRAPH_SEED_KINDS: Set<String> = setOf(SEED_KIND_GRAPH)

/** Parsed flat multi-document seed before kind-specific validation. */
data class SeedRawDocument(
    val index: Int,
    val apiVersion: String?,
    val kind: String?,
    val raw: Map<String, Any?>,
)

data class SeedDocumentResult(
    val index: Int,
    val kind: String?,
    val apiVersion: String?,
    val identity: String? = null,
    val applied: Boolean = false,
    val skipped: Boolean = false,
    val errors: List<ValidationIssue> = emptyList(),
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

    fun allErrors(): List<ValidationIssue> = documents.flatMap { it.errors }

    companion object {
        fun empty(): SeedImportResult = SeedImportResult()
    }
}

class SeedImportException(
    message: String,
    val result: SeedImportResult,
) : RuntimeException(message)
