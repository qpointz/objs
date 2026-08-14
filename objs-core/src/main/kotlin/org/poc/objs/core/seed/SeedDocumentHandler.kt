package org.poc.objs.core.seed

/**
 * Extensible seed document handler.
 * New kinds register additional Spring beans; the importer discovers them by [kind].
 */
interface SeedDocumentHandler {
    val kind: String

    /**
     * Lower runs first. Built-in: ObjectSchema `0`, AllowedEdgeRule `10`, Graph `30`.
     * Application kinds should pick a gap (e.g. Collection `20`, CollectionObjects `40`).
     */
    val applyOrder: Int
        get() = 50

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
