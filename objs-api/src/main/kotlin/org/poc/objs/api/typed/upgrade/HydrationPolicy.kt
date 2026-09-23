package org.poc.objs.api.typed.upgrade

enum class HydrationPolicy {
    /** Exact `(type, schemaVersion)` binding only (C-23 behavior). */
    EXACT_ONLY,

    /**
     * Exact bind, else explicit upgrade chain to latest, else
     * additive Map→latest class, else raw.
     */
    UPGRADE_TO_LATEST,
}
