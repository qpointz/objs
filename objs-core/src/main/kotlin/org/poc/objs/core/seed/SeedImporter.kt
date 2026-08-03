package org.poc.objs.core.seed

import org.poc.objs.core.validation.BoMValidationIssue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

/**
 * Transactional multi-document seed importer.
 *
 * Parses and validates all documents first, then applies in dependency order
 * (ObjectSchema → AllowedEdgeRule → Graph) regardless of declaration order.
 * Any failure rolls back the entire resource transaction.
 */
@Service
class SeedImporter(
    handlers: List<SeedDocumentHandler>,
) {
    private val handlersByKind: Map<String, SeedDocumentHandler> =
        handlers.associateBy { it.kind }

    @Transactional
    fun importYaml(yaml: String, allowedKinds: Set<String>? = null): SeedImportResult =
        importDocuments(SeedYaml.parseDocuments(yaml), allowedKinds)

    @Transactional
    fun importYaml(stream: InputStream, allowedKinds: Set<String>? = null): SeedImportResult =
        importYaml(stream.bufferedReader().use { it.readText() }, allowedKinds)

    @Transactional
    fun importDocuments(
        rawDocuments: List<SeedRawDocument>,
        allowedKinds: Set<String>? = null,
    ): SeedImportResult {
        if (rawDocuments.isEmpty()) {
            return SeedImportResult(warnings = listOf("No YAML documents found"))
        }

        val parsed = mutableListOf<Pair<SeedDocumentHandler, ParsedSeedDocument>>()
        val failures = mutableListOf<SeedDocumentResult>()

        for (doc in rawDocuments) {
            val envelopeError = validateEnvelope(doc)
            if (envelopeError != null) {
                failures += envelopeError
                continue
            }
            if (allowedKinds != null && doc.kind !in allowedKinds) {
                failures += SeedDocumentResult(
                    index = doc.index,
                    kind = doc.kind,
                    apiVersion = doc.apiVersion,
                    errors = listOf(
                        BoMValidationIssue(
                            "SEED_KIND_NOT_ALLOWED",
                            "Seed kind '${doc.kind}' is not allowed for this endpoint",
                            path = "document[${doc.index}].kind",
                        ),
                    ),
                )
                continue
            }
            val handler = handlersByKind[doc.kind]
            if (handler == null) {
                failures += SeedDocumentResult(
                    index = doc.index,
                    kind = doc.kind,
                    apiVersion = doc.apiVersion,
                    errors = listOf(
                        BoMValidationIssue(
                            "SEED_KIND_UNSUPPORTED",
                            "Unsupported seed kind '${doc.kind}'",
                            path = "document[${doc.index}].kind",
                        ),
                    ),
                )
                continue
            }
            try {
                parsed += handler to handler.parse(doc)
            } catch (ex: SeedDocumentParseException) {
                failures += SeedDocumentResult(
                    index = doc.index,
                    kind = doc.kind,
                    apiVersion = doc.apiVersion,
                    errors = listOf(
                        BoMValidationIssue(
                            "SEED_DOCUMENT_INVALID",
                            ex.message ?: "Invalid seed document",
                            path = "document[${doc.index}]",
                        ),
                    ),
                )
            } catch (ex: Exception) {
                failures += SeedDocumentResult(
                    index = doc.index,
                    kind = doc.kind,
                    apiVersion = doc.apiVersion,
                    errors = listOf(
                        BoMValidationIssue(
                            "SEED_DOCUMENT_INVALID",
                            ex.message ?: "Invalid seed document",
                            path = "document[${doc.index}]",
                        ),
                    ),
                )
            }
        }

        if (failures.isNotEmpty()) {
            val result = SeedImportResult(documents = failures.sortedBy { it.index })
            throw SeedImportException("Seed import failed during parse/validate", result)
        }

        val ordered = parsed.sortedWith(
            compareBy<Pair<SeedDocumentHandler, ParsedSeedDocument>> { applyOrder(it.first.kind) }
                .thenBy { it.second.document.index },
        )

        val applied = mutableListOf<SeedDocumentResult>()
        for ((handler, doc) in ordered) {
            try {
                applied += handler.apply(doc)
            } catch (ex: SeedDocumentValidationException) {
                val result = SeedImportResult(
                    documents = applied + SeedDocumentResult(
                        index = doc.document.index,
                        kind = handler.kind,
                        apiVersion = doc.document.apiVersion,
                        identity = doc.identity,
                        errors = ex.issues.ifEmpty {
                            listOf(
                                BoMValidationIssue(
                                    "SEED_APPLY_FAILED",
                                    ex.message ?: "Seed apply failed",
                                    path = "document[${doc.document.index}]",
                                ),
                            )
                        },
                    ),
                )
                throw SeedImportException("Seed import failed during apply", result)
            } catch (ex: Exception) {
                val result = SeedImportResult(
                    documents = applied + SeedDocumentResult(
                        index = doc.document.index,
                        kind = handler.kind,
                        apiVersion = doc.document.apiVersion,
                        identity = doc.identity,
                        errors = listOf(
                            BoMValidationIssue(
                                "SEED_APPLY_FAILED",
                                ex.message ?: "Seed apply failed",
                                path = "document[${doc.document.index}]",
                            ),
                        ),
                    ),
                )
                throw SeedImportException("Seed import failed during apply", result)
            }
        }

        return SeedImportResult(documents = applied.sortedBy { it.index })
    }

    private fun validateEnvelope(doc: SeedRawDocument): SeedDocumentResult? {
        if (doc.apiVersion.isNullOrBlank()) {
            return SeedDocumentResult(
                index = doc.index,
                kind = doc.kind,
                apiVersion = doc.apiVersion,
                errors = listOf(
                    BoMValidationIssue(
                        "SEED_APIVERSION_MISSING",
                        "apiVersion is required",
                        path = "document[${doc.index}].apiVersion",
                    ),
                ),
            )
        }
        if (doc.apiVersion != SEED_API_VERSION_V1) {
            return SeedDocumentResult(
                index = doc.index,
                kind = doc.kind,
                apiVersion = doc.apiVersion,
                errors = listOf(
                    BoMValidationIssue(
                        "SEED_APIVERSION_UNSUPPORTED",
                        "Unsupported apiVersion '${doc.apiVersion}'",
                        path = "document[${doc.index}].apiVersion",
                    ),
                ),
            )
        }
        if (doc.kind.isNullOrBlank()) {
            return SeedDocumentResult(
                index = doc.index,
                kind = doc.kind,
                apiVersion = doc.apiVersion,
                errors = listOf(
                    BoMValidationIssue(
                        "SEED_KIND_MISSING",
                        "kind is required",
                        path = "document[${doc.index}].kind",
                    ),
                ),
            )
        }
        return null
    }

    private fun applyOrder(kind: String): Int = when (kind) {
        SEED_KIND_OBJECT_SCHEMA -> 0
        SEED_KIND_ALLOWED_EDGE_RULE -> 1
        SEED_KIND_GRAPH -> 2
        else -> 99
    }
}
