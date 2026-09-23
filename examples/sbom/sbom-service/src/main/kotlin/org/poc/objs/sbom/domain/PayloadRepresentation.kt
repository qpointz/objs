package org.poc.objs.sbom.domain

enum class PayloadRepresentation {
    SAVED,
    LATEST,
    BOTH,
    ;

    companion object {
        fun parse(raw: String?, default: PayloadRepresentation): PayloadRepresentation {
            if (raw.isNullOrBlank()) return default
            return when (raw.trim().lowercase()) {
                "saved", "evidence" -> SAVED
                "latest", "examine" -> LATEST
                "both" -> BOTH
                else -> throw IllegalArgumentException(
                    "representation must be saved|latest|both (got '$raw')",
                )
            }
        }
    }
}
