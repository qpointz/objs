package org.poc.objs.assetrepository.domain;

/**
 * How object writes resolve identity within a collection (G-P3).
 */
public enum ObjectWriteMode {
    /** Missing id always creates; updates require UUID. */
    UUID,
    /** Missing id resolves via schema identifier fields; id honored when present. */
    IDENTIFIER,
    /** Default: UUID when present, otherwise identifier resolve. */
    UUID_OR_IDENTIFIER
}
