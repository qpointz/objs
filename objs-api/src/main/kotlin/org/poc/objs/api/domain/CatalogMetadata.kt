package org.poc.objs.api.domain

/** Normalize optional catalog tags, string attributes, and blankable text. */
object CatalogMetadata {
    fun optionalText(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }

    fun tags(raw: List<String>?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        val seen = LinkedHashSet<String>()
        for (item in raw) {
            val tag = item.trim()
            if (tag.isNotEmpty()) seen.add(tag)
        }
        return seen.toList()
    }

    fun attributes(raw: Map<String, String>?): Map<String, String> {
        if (raw.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for ((key, value) in raw) {
            val trimmed = key.trim()
            if (trimmed.isNotEmpty()) out[trimmed] = value
        }
        return out
    }
}
