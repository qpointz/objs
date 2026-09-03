package org.poc.objs.api.seed

import org.poc.objs.api.domain.CatalogMetadata

fun parseSeedTags(raw: Any?, index: Int): List<String> {
    if (raw == null) return emptyList()
    if (raw !is List<*>) {
        throw SeedDocumentParseException(index, "tags must be a list of strings")
    }
    return CatalogMetadata.tags(raw.map { it?.toString().orEmpty() })
}

fun parseSeedAttributes(raw: Any?, index: Int): Map<String, String> {
    if (raw == null) return emptyMap()
    if (raw !is Map<*, *>) {
        throw SeedDocumentParseException(index, "attributes must be a string map")
    }
    return CatalogMetadata.attributes(
        raw.entries.associate { (key, value) -> key.toString() to (value?.toString() ?: "") },
    )
}

fun emitSeedTags(target: MutableMap<String, Any?>, tags: List<String>) {
    if (tags.isNotEmpty()) target["tags"] = tags
}

fun emitSeedAttributes(target: MutableMap<String, Any?>, attributes: Map<String, String>) {
    if (attributes.isNotEmpty()) target["attributes"] = attributes
}

fun emitSeedText(target: MutableMap<String, Any?>, key: String, value: String?) {
    if (!value.isNullOrBlank()) target[key] = value
}
