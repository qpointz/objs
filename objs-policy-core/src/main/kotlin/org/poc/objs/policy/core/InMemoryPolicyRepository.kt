package org.poc.objs.policy.core

import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyQuery
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicyTags
import org.poc.objs.policy.api.PolicyWrite
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * Process-local [PolicyRepository]. Each [save] / [update] allocates a new [Policy.serial]
 * using the same timestamp rule as object head versions (`max(nowMillis, previous + 1)`).
 * Requires a known [PolicyWrite.categoryId] in [categories]. Identity is [Policy.key]
 * (G-P36seed); [Policy.name] is a separate display label.
 */
class InMemoryPolicyRepository(
    private val categories: CategoryRepository,
) : PolicyRepository {
    private val byId = ConcurrentHashMap<UUID, Policy>()
    private val serialsByKey = ConcurrentHashMap<String, ConcurrentHashMap<Long, Policy>>()
    private val latestByKey = ConcurrentHashMap<String, AtomicLong>()

    override fun save(write: PolicyWrite): Policy {
        val normalized = normalizeWrite(write)
        val previous = latestByKey[normalized.key]?.get()
        val nextSerial = nextSerial(previous)

        val stored = Policy(
            id = UUID.randomUUID(),
            key = normalized.key,
            name = normalized.name,
            serial = nextSerial,
            engineKind = normalized.engineKind,
            body = normalized.body,
            contentType = normalized.contentType,
            applicabilityKind = normalized.applicabilityKind,
            applicabilityBody = normalized.applicabilityBody,
            categoryId = normalized.categoryId,
            tags = normalized.tags,
            annotations = normalized.annotations,
            version = normalized.version,
            description = normalized.description,
        )

        byId[stored.id] = stored
        index(stored)
        return stored
    }

    override fun update(id: UUID, write: PolicyWrite): Policy? {
        val existing = byId[id] ?: return null
        val normalized = normalizeWrite(write)
        val nextSerial = nextSerial(existing.serial)

        unindex(existing)

        val updated = existing.copy(
            key = normalized.key,
            name = normalized.name,
            serial = nextSerial,
            engineKind = normalized.engineKind,
            body = normalized.body,
            contentType = normalized.contentType,
            applicabilityKind = normalized.applicabilityKind,
            applicabilityBody = normalized.applicabilityBody,
            categoryId = normalized.categoryId,
            tags = normalized.tags,
            annotations = normalized.annotations,
            version = normalized.version,
            description = normalized.description,
        )

        byId[id] = updated
        index(updated)
        return updated
    }

    override fun delete(id: UUID): Boolean {
        val existing = byId.remove(id) ?: return false
        unindex(existing)
        return true
    }

    override fun resolve(ref: PolicyRef): Policy? =
        when (ref) {
            is PolicyRef.ById -> findById(ref.id)
            is PolicyRef.ByKey -> {
                val serials = serialsByKey[ref.key] ?: return null
                if (ref.serial == null) {
                    val latest = latestByKey[ref.key]?.get() ?: return null
                    serials[latest]
                } else {
                    serials[ref.serial]
                }
            }
        }

    override fun findById(id: UUID): Policy? = byId[id]

    override fun findByKey(key: String): List<Policy> =
        serialsByKey[key]?.values?.sortedBy { it.serial } ?: emptyList()

    override fun list(): List<Policy> =
        byId.values.sortedWith(compareBy({ it.key }, { it.serial }))

    override fun query(query: PolicyQuery): List<Policy> {
        val nameNeedle = query.nameContains?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
        val keyNeedle = query.keyContains?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
        val wantTags = PolicyTags.normalize(query.tags)
        return list().filter { p ->
            if (query.categoryId != null && p.categoryId != query.categoryId) return@filter false
            if (wantTags.isNotEmpty() && !p.tags.containsAll(wantTags)) return@filter false
            if (query.annotations.isNotEmpty()) {
                for ((k, v) in query.annotations) {
                    if (p.annotations[k] != v) return@filter false
                }
            }
            if (nameNeedle != null && !p.name.lowercase().contains(nameNeedle)) return@filter false
            if (keyNeedle != null && !p.key.lowercase().contains(keyNeedle)) return@filter false
            true
        }
    }

    private fun index(policy: Policy) {
        serialsByKey
            .computeIfAbsent(policy.key) { ConcurrentHashMap() }[policy.serial] = policy
        latestByKey.computeIfAbsent(policy.key) { AtomicLong(policy.serial) }
            .updateAndGet { maxOf(it, policy.serial) }
    }

    private fun unindex(policy: Policy) {
        serialsByKey[policy.key]?.remove(policy.serial)
        if (serialsByKey[policy.key].isNullOrEmpty()) {
            serialsByKey.remove(policy.key)
            latestByKey.remove(policy.key)
        } else {
            val maxLeft = serialsByKey[policy.key]!!.keys.maxOrNull()
            if (maxLeft != null) {
                latestByKey[policy.key] = AtomicLong(maxLeft)
            } else {
                latestByKey.remove(policy.key)
            }
        }
    }

    private fun normalizeWrite(write: PolicyWrite): PolicyWrite {
        require(write.key.isNotBlank()) { "Policy key must not be blank" }
        require(write.engineKind.isNotBlank()) { "engineKind must not be blank" }
        val version = write.version.trim()
        require(version.isNotEmpty()) { "version must not be blank" }
        require(categories.findById(write.categoryId) != null) {
            "Unknown categoryId: ${write.categoryId}"
        }
        val tags = PolicyTags.requireNonEmpty(write.tags)
        val key = write.key.trim()
        val name = write.name.trim().ifBlank { key }
        return write.copy(
            key = key,
            name = name,
            tags = tags,
            annotations = write.annotations.toMap(),
            version = version,
            description = write.description.trim(),
        )
    }

    companion object {
        /** Same rule as object head versions: max(nowMillis, previous + 1). */
        fun nextSerial(previous: Long?): Long {
            val millis = Instant.now().toEpochMilli()
            return max(millis, (previous ?: 0L) + 1L)
        }
    }
}
