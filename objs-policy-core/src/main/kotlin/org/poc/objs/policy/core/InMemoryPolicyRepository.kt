package org.poc.objs.policy.core

import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicyWrite
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-local [PolicyRepository]. Each [save] allocates a new serial [Policy.version]
 * for the logical name (never overwrites an existing serial in place).
 */
class InMemoryPolicyRepository : PolicyRepository {
    private val byId = ConcurrentHashMap<UUID, Policy>()
    private val versionsByName = ConcurrentHashMap<String, ConcurrentHashMap<Long, Policy>>()
    private val latestByName = ConcurrentHashMap<String, AtomicLong>()

    override fun save(write: PolicyWrite): Policy {
        require(write.name.isNotBlank()) { "Policy name must not be blank" }
        require(write.engineKind.isNotBlank()) { "engineKind must not be blank" }

        val nextVersion = latestByName
            .computeIfAbsent(write.name) { AtomicLong(0) }
            .incrementAndGet()

        val stored = Policy(
            id = UUID.randomUUID(),
            name = write.name,
            version = nextVersion,
            engineKind = write.engineKind,
            body = write.body,
            contentType = write.contentType,
            applicabilityKind = write.applicabilityKind,
            applicabilityBody = write.applicabilityBody,
        )

        byId[stored.id] = stored
        versionsByName
            .computeIfAbsent(stored.name) { ConcurrentHashMap() }[stored.version] = stored
        return stored
    }

    override fun resolve(ref: PolicyRef): Policy? =
        when (ref) {
            is PolicyRef.ById -> findById(ref.id)
            is PolicyRef.ByName -> {
                val versions = versionsByName[ref.name] ?: return null
                if (ref.version == null) {
                    val latest = latestByName[ref.name]?.get() ?: return null
                    versions[latest]
                } else {
                    versions[ref.version]
                }
            }
        }

    override fun findById(id: UUID): Policy? = byId[id]

    override fun findByName(name: String): List<Policy> =
        versionsByName[name]?.values?.sortedBy { it.version } ?: emptyList()

    override fun list(): List<Policy> =
        byId.values.sortedWith(compareBy({ it.name }, { it.version }))
}
