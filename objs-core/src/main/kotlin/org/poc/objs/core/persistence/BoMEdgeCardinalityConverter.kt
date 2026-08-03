package org.poc.objs.core.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.poc.objs.core.domain.BoMEdgeCardinality

/** Persist [BoMEdgeCardinality] using wire values (`UNSPECIFIED`, `1:1`, `1:*`). */
@Converter(autoApply = true)
class BoMEdgeCardinalityConverter : AttributeConverter<BoMEdgeCardinality, String> {
    override fun convertToDatabaseColumn(attribute: BoMEdgeCardinality?): String =
        (attribute ?: BoMEdgeCardinality.UNSPECIFIED).wire

    override fun convertToEntityAttribute(dbData: String?): BoMEdgeCardinality =
        if (dbData.isNullOrBlank()) {
            BoMEdgeCardinality.UNSPECIFIED
        } else {
            BoMEdgeCardinality.fromWire(dbData)
        }
}
