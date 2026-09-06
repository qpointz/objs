package org.poc.objs.policy.service.seed

import org.poc.objs.api.seed.SeedDocumentParseException
import org.poc.objs.policy.api.PolicyKeys
import org.poc.objs.policy.api.PolicySeedModes

/** Parses the shared `mode` seed field (`apply` default / `replace`, G-P35seed). */
fun parseSeedMode(raw: Any?, index: Int): String =
    try {
        PolicySeedModes.requireValid(raw)
    } catch (ex: IllegalArgumentException) {
        throw SeedDocumentParseException(index, ex.message ?: "Invalid mode", ex)
    }

/** Validates a policy catalog `key` field (G-P36seed / G-P40seed charset rules). */
fun requireSeedKey(raw: Map<String, Any?>, field: String, index: Int): String {
    val value = raw[field]?.toString()?.trim()
    if (value.isNullOrEmpty()) {
        throw SeedDocumentParseException(index, "$field must be a non-blank string")
    }
    return try {
        PolicyKeys.requireValid(value)
    } catch (ex: IllegalArgumentException) {
        throw SeedDocumentParseException(index, "$field: ${ex.message}", ex)
    }
}

/** Parses a string map seed field (e.g. `annotations`), reporting the field [path] on error. */
fun parseSeedStringMap(raw: Any?, index: Int, path: String): Map<String, String> {
    if (raw == null) return emptyMap()
    val map = raw as? Map<*, *> ?: throw SeedDocumentParseException(index, "$path must be a string map")
    return map.entries.associate { (k, v) -> k.toString() to (v?.toString() ?: "") }
}

/** Parses a string list seed field (e.g. `tags`), reporting the field [path] on error. */
fun parseSeedStringList(raw: Any?, index: Int, path: String): List<String> {
    if (raw == null) return emptyList()
    val list = raw as? List<*> ?: throw SeedDocumentParseException(index, "$path must be a list of strings")
    return list.map { it?.toString().orEmpty() }
}

/**
 * Resolves the policy `body` field: inline `body` string, or `bodyRef` / `bodyFile`
 * (classpath: / file: — G-P37seed). Exactly one must be present.
 */
fun resolveSeedBody(raw: Map<String, Any?>, index: Int, loader: SeedBodyLoader): String {
    val inline = raw["body"]?.toString()?.takeIf { it.isNotBlank() }
    val ref = raw["bodyRef"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
    val file = raw["bodyFile"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
    val provided = listOfNotNull(inline, ref, file)
    if (provided.size > 1) {
        throw SeedDocumentParseException(index, "Specify only one of body / bodyRef / bodyFile")
    }
    return try {
        when {
            inline != null -> inline
            ref != null -> loader.load(ref)
            file != null -> loader.load(if (file.contains(":")) file else "${SpringSeedBodyLoader.FILE_PREFIX}$file")
            else -> throw SeedDocumentParseException(index, "Policy requires one of body / bodyRef / bodyFile")
        }
    } catch (ex: SeedDocumentParseException) {
        throw ex
    } catch (ex: Exception) {
        throw SeedDocumentParseException(index, "Failed to load policy body: ${ex.message}", ex)
    }
}
