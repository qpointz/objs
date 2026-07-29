package org.poc.objs.core.seed

import java.net.URI
import java.security.MessageDigest
import java.util.HexFormat

/** Fingerprints and ledger-key sanitization for seed resources. */
object BoMSeedResourceIdentity {
    fun fingerprint(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return "sha256:" + HexFormat.of().formatHex(digest)
    }

    /**
     * Prefer an explicit name; otherwise normalize the resource location by stripping
     * credentials, query, and fragment.
     */
    fun ledgerKey(explicitName: String?, location: String): String {
        val name = explicitName?.trim().orEmpty()
        if (name.isNotEmpty()) return name
        return normalizeLocation(location)
    }

    fun normalizeLocation(location: String): String {
        val trimmed = location.trim()
        if (trimmed.startsWith("classpath:")) {
            return "classpath:" + trimmed.removePrefix("classpath:").trimStart('/')
        }
        if (trimmed.startsWith("file:")) {
            return try {
                val uri = URI(trimmed)
                val path = uri.path ?: trimmed.removePrefix("file:")
                "file:" + path
            } catch (_: Exception) {
                trimmed.substringBefore('?').substringBefore('#')
            }
        }
        return try {
            val uri = URI(trimmed)
            buildString {
                if (uri.scheme != null) append(uri.scheme).append(':')
                if (uri.host != null) {
                    append("//")
                    append(uri.host)
                    if (uri.port > 0) append(':').append(uri.port)
                }
                append(uri.path ?: "")
            }
        } catch (_: Exception) {
            trimmed.substringBefore('?').substringBefore('#')
        }
    }
}
