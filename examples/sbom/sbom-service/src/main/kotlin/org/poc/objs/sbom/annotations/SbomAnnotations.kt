package org.poc.objs.sbom.annotations

/**
 * Canonical annotation keys for the SBOM example app.
 */
object SbomAnnotationKeys {
    const val APP = "app"
    const val APP_VERSION = "appVersion"
    const val SOURCE = "source"
    const val SOURCE_DETAIL = "sourceDetail"
    const val CAPTURED_BY = "capturedBy"
    const val ORIGIN = "origin"
    /** Owning application **name** (G-P5) — not UUID. */
    const val OWNER = "owner"
}

enum class CaptureSource(val value: String) {
    MANUAL("manual"),
    DETECTED("detected"),
    ENRICHED("enriched"),
    ;

    companion object {
        fun from(value: String): CaptureSource =
            entries.firstOrNull { it.value == value }
                ?: error("Unknown source: $value")
    }
}

/** BOM identity: application slug + release version. */
data class SbomContext(
    val app: String,
    val appVersion: String,
) {
    fun toAnnotations(): Map<String, String> = mapOf(
        SbomAnnotationKeys.APP to app,
        SbomAnnotationKeys.APP_VERSION to appVersion,
    )

    fun filter(extra: Map<String, String> = emptyMap()): Map<String, String> =
        toAnnotations() + extra
}

/**
 * How an object entered the BOM (distinct from [SbomAnnotationKeys.ORIGIN] caller channel).
 */
sealed class Provenance {
    abstract fun toAnnotations(): Map<String, String>

    data class Manual(val capturedBy: String, val sourceDetail: String? = null) : Provenance() {
        init {
            require(capturedBy.isNotBlank()) { "capturedBy is required for manual provenance" }
        }

        override fun toAnnotations(): Map<String, String> = buildMap {
            put(SbomAnnotationKeys.SOURCE, CaptureSource.MANUAL.value)
            put(SbomAnnotationKeys.CAPTURED_BY, capturedBy)
            sourceDetail?.let { put(SbomAnnotationKeys.SOURCE_DETAIL, it) }
        }
    }

    data class Detected(val sourceDetail: String? = null) : Provenance() {
        override fun toAnnotations(): Map<String, String> = buildMap {
            put(SbomAnnotationKeys.SOURCE, CaptureSource.DETECTED.value)
            sourceDetail?.let { put(SbomAnnotationKeys.SOURCE_DETAIL, it) }
        }
    }

    data class Enriched(val catalogId: String) : Provenance() {
        init {
            require(catalogId.isNotBlank()) { "sourceDetail (catalog id) is required for enriched provenance" }
        }

        override fun toAnnotations(): Map<String, String> = mapOf(
            SbomAnnotationKeys.SOURCE to CaptureSource.ENRICHED.value,
            SbomAnnotationKeys.SOURCE_DETAIL to catalogId,
        )
    }

    companion object {
        fun manual(capturedBy: String, sourceDetail: String? = null) = Manual(capturedBy, sourceDetail)
        fun detected(sourceDetail: String? = null) = Detected(sourceDetail)
        fun enriched(catalogId: String) = Enriched(catalogId)
    }
}
