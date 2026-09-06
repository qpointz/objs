package org.poc.objs.core.persistence.policy

import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyQuery
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicyTags
import org.poc.objs.policy.api.PolicyWrite
import java.time.Instant
import java.util.UUID
import kotlin.math.max

/**
 * JPA-backed [PolicyRepository] (C-28 WI-002). Each [save] / [update] allocates a new
 * [Policy.serial] using the same timestamp rule as object head versions
 * (`max(nowMillis, previous + 1)`), mirroring `InMemoryPolicyRepository`. Requires a known
 * [PolicyWrite.categoryId]. Identity is [Policy.key] (G-P36seed); [Policy.name] is display.
 */
class JpaPolicyRepository(
    private val dao: PolicyDao,
    private val categoryDao: PolicyCategoryDao,
    private val uow: UnitOfWork,
) : PolicyRepository {

    override fun save(write: PolicyWrite): Policy = uow.write {
        val normalized = normalizeWrite(write)
        val nextSerial = nextSerial(dao.maxSerialByKey(normalized.key))
        val now = Instant.now()
        val record = PolicyRecord(
            id = UUID.randomUUID(),
            key = normalized.key,
            serial = nextSerial,
            name = normalized.name,
            engineKind = normalized.engineKind,
            body = normalized.body,
            contentType = normalized.contentType,
            applicabilityKind = normalized.applicabilityKind,
            applicabilityBody = normalized.applicabilityBody,
            categoryId = normalized.categoryId,
            tags = normalized.tags.toMutableList(),
            annotations = normalized.annotations.toMutableMap(),
            version = normalized.version,
            description = normalized.description,
            createdAt = now,
            updatedAt = now,
        )
        dao.save(record)
        record.toDomain()
    }

    override fun update(id: UUID, write: PolicyWrite): Policy? = uow.write {
        val existing = dao.findById(id) ?: return@write null
        val normalized = normalizeWrite(write)
        existing.key = normalized.key
        existing.serial = nextSerial(existing.serial)
        existing.name = normalized.name
        existing.engineKind = normalized.engineKind
        existing.body = normalized.body
        existing.contentType = normalized.contentType
        existing.applicabilityKind = normalized.applicabilityKind
        existing.applicabilityBody = normalized.applicabilityBody
        existing.categoryId = normalized.categoryId
        existing.tags = normalized.tags.toMutableList()
        existing.annotations = normalized.annotations.toMutableMap()
        existing.version = normalized.version
        existing.description = normalized.description
        existing.updatedAt = Instant.now()
        dao.save(existing)
        existing.toDomain()
    }

    override fun delete(id: UUID): Boolean = uow.write {
        if (dao.findById(id) == null) return@write false
        dao.deleteById(id)
        true
    }

    override fun resolve(ref: PolicyRef): Policy? = uow.read {
        when (ref) {
            is PolicyRef.ById -> dao.findById(ref.id)?.toDomain()
            is PolicyRef.ByKey -> {
                val serial = ref.serial
                if (serial != null) {
                    dao.findByKeyAndSerial(ref.key, serial)?.toDomain()
                } else {
                    dao.findLatestByKey(ref.key)?.toDomain()
                }
            }
        }
    }

    override fun findById(id: UUID): Policy? = uow.read { dao.findById(id)?.toDomain() }

    override fun findByKey(key: String): List<Policy> = uow.read {
        dao.findByKey(key).map { it.toDomain() }
    }

    override fun list(): List<Policy> = uow.read {
        dao.findAll().sortedWith(compareBy({ it.key }, { it.serial })).map { it.toDomain() }
    }

    override fun query(query: PolicyQuery): List<Policy> = uow.read {
        val nameNeedle = query.nameContains?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
        val keyNeedle = query.keyContains?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
        val wantTags = PolicyTags.normalize(query.tags)
        dao.findAll().sortedWith(compareBy({ it.key }, { it.serial })).map { it.toDomain() }.filter { p ->
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

    private fun normalizeWrite(write: PolicyWrite): PolicyWrite {
        require(write.key.isNotBlank()) { "Policy key must not be blank" }
        require(write.engineKind.isNotBlank()) { "engineKind must not be blank" }
        val version = write.version.trim()
        require(version.isNotEmpty()) { "version must not be blank" }
        require(categoryDao.findById(write.categoryId) != null) {
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

private fun PolicyRecord.toDomain() = Policy(
    id = id,
    key = key,
    name = name,
    serial = serial,
    engineKind = engineKind,
    body = body,
    contentType = contentType,
    applicabilityKind = applicabilityKind,
    applicabilityBody = applicabilityBody,
    categoryId = categoryId,
    tags = tags.toList(),
    annotations = annotations.toMap(),
    version = version,
    description = description,
)
