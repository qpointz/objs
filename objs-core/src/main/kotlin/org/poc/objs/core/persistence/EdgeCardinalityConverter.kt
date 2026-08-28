package org.poc.objs.core.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.poc.objs.api.domain.EdgeCardinality

/** Persist [EdgeCardinality] using wire values (`UNSPECIFIED`, `1:1`, `1:*`). */
@Converter(autoApply = true)
class EdgeCardinalityConverter : AttributeConverter<EdgeCardinality, String> {
    override fun convertToDatabaseColumn(attribute: EdgeCardinality?): String =
        (attribute ?: EdgeCardinality.UNSPECIFIED).wire

    override fun convertToEntityAttribute(dbData: String?): EdgeCardinality =
        if (dbData.isNullOrBlank()) {
            EdgeCardinality.UNSPECIFIED
        } else {
            EdgeCardinality.fromWire(dbData)
        }
}
