package org.poc.objs.core.seed

/**
 * Extensible seed document handler.
 * New kinds register additional Spring beans; the importer discovers them by [kind].
 */
interface SeedDocumentHandler {
    val kind: String

    /** Parse and validate the document without applying side effects. */
    fun parse(document: SeedRawDocument): ParsedSeedDocument

    /** Apply a previously validated document (MERGE upsert). */
    fun apply(parsed: ParsedSeedDocument): SeedDocumentResult
}

/** Kind-specific parsed payload ready for application. */
data class ParsedSeedDocument(
    val document: SeedRawDocument,
    val identity: String,
    val payload: Any,
)
