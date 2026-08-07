package org.poc.objs.core.match

import org.poc.objs.core.validation.BoMValidationException
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import java.util.UUID

/**
 * DSL `ids`: select entities whose ids are in [ids], then induced edges among survivors.
 */
class BoMIdsMatcher(
    val ids: List<UUID>,
) : BoMMatcher, BoMSourceCapableMatcher {

    override fun toCandidateSource(backend: BoMEntityCandidateBackend): BoMCandidateSource? {
        if (ids.isEmpty()) {
            return BoMCandidateSource { emptyList() }
        }
        return backend.entityIdsSource(ids)
    }

    override fun matches(candidate: BoMEntityMatchCandidate): Boolean {
        val id = candidate.id ?: return false
        return id in idSet
    }

    private val idSet: Set<UUID> = ids.toSet()

    companion object {
        fun fromRaw(values: List<*>, path: String): BoMIdsMatcher {
            if (values.isEmpty()) {
                return BoMIdsMatcher(emptyList())
            }
            val parsed = ArrayList<UUID>(values.size)
            values.forEachIndexed { index, raw ->
                val text = raw?.toString()?.trim().orEmpty()
                if (text.isEmpty()) {
                    fail(path, "MATCHER_DSL_IDS_VALUE", "'ids' entries must be non-blank UUID strings", "$path[$index]")
                }
                try {
                    parsed.add(UUID.fromString(text))
                } catch (_: IllegalArgumentException) {
                    fail(path, "MATCHER_DSL_IDS_INVALID", "Invalid UUID in 'ids': $text", "$path[$index]")
                }
            }
            return BoMIdsMatcher(parsed)
        }

        private fun fail(root: String, code: String, message: String, issuePath: String): Nothing {
            throw BoMValidationException(
                "matcher-dsl",
                BoMValidationResult.of(BoMValidationIssue(code = code, message = message, path = issuePath)),
            )
        }
    }
}
