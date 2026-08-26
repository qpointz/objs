package org.poc.objs.core.persistence

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import org.poc.objs.core.domain.CatalogMetadata
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaKey
import org.poc.objs.core.domain.BoMSchemaNode
import org.poc.objs.core.domain.BoMSchemaNormalizer
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.domain.InMemoryBoMAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemoryBoMSchemaCatalog
import org.poc.objs.core.typed.PayloadMapper
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * PostgreSQL-authoritative [BoMSchemaCatalog] with a write-through in-memory snapshot and
 * Caffeine TTL revalidation.
 *
 * Reads use the snapshot. Writes persist first, then update the snapshot and reset the TTL clock.
 * When the TTL expires and no transaction is active, the next read rehydrates from PostgreSQL
 * (so out-of-band truncates become visible without restart). Mid-TX reads skip TTL reload so seed
 * import keeps write-through visibility. On transaction rollback the snapshot is rehydrated.
 */
open class JpaBoMSchemaCatalog(
    private val repository: BoMSchemaCatalogRepository,
    private val properties: ObjsCatalogProperties = ObjsCatalogProperties(),
    private val ticker: Ticker = Ticker.systemTicker(),
) : BoMSchemaCatalog {

    private val cache = InMemoryBoMSchemaCatalog()
    private val lock = Any()
    private val freshness = buildFreshnessCache(properties.cacheTtl, ticker)

    /** Load all persisted schemas into the snapshot. Call at startup, on refresh, and after TX rollback. */
    open fun hydrate() {
        synchronized(lock) {
            hydrateUnlocked()
        }
    }

    override fun refreshFromStore() = hydrate()

    @Transactional
    override fun register(schema: BoMSchema) {
        val normalized = BoMSchemaNormalizer.normalizeStrict(schema)
        val now = Instant.now()
        val id = BoMSchemaCatalogId(normalized.type, normalized.version)
        synchronized(lock) {
            val record = repository.findById(id).orElseGet {
                BoMSchemaCatalogRecord(
                    type = normalized.type,
                    version = normalized.version,
                    createdAt = now,
                )
            }
            record.definitionDoc = PayloadMapper.toMap(normalized.contentSchema)
            record.usage = normalized.usage.name
            record.tags = normalized.tags.toMutableList()
            record.attributes = normalized.attributes.toMutableMap()
            record.updatedAt = now
            repository.save(record)
            cache.register(normalized)
            markFresh()
            registerRollbackRehydration()
        }
    }

    override fun get(type: String, version: String): BoMSchema? {
        ensureFresh()
        return cache.get(type, version)
    }

    override fun get(key: BoMSchemaKey): BoMSchema? {
        ensureFresh()
        return cache.get(key)
    }

    override fun contains(type: String, version: String): Boolean {
        ensureFresh()
        return cache.contains(type, version)
    }

    override fun all(): Collection<BoMSchema> {
        ensureFresh()
        return cache.all()
    }

    override fun listByType(type: String): List<BoMSchema> {
        ensureFresh()
        return cache.listByType(type)
    }

    override fun types(): Set<String> {
        ensureFresh()
        return cache.types()
    }

    @Transactional
    override fun remove(type: String, version: String): Boolean {
        val id = BoMSchemaCatalogId(type, version)
        synchronized(lock) {
            if (!repository.existsById(id)) return false
            repository.deleteById(id)
            cache.remove(type, version)
            markFresh()
            registerRollbackRehydration()
            return true
        }
    }

    @Transactional
    override fun clear() {
        synchronized(lock) {
            repository.deleteAll()
            cache.clear()
            markFresh()
            registerRollbackRehydration()
        }
    }

    private fun ensureFresh() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) return
        if (isTtlDisabled(properties.cacheTtl)) return
        freshness.get(FRESHNESS_KEY) {
            synchronized(lock) {
                hydrateUnlocked()
            }
            true
        }
    }

    private fun hydrateUnlocked() {
        cache.clear()
        repository.findAll().forEach { record ->
            cache.register(record.toDomain())
        }
        markFresh()
    }

    private fun markFresh() {
        if (!isTtlDisabled(properties.cacheTtl)) {
            freshness.put(FRESHNESS_KEY, true)
        }
    }

    private fun registerRollbackRehydration() {
        registerCatalogRollbackRehydration { hydrate() }
    }
}

/**
 * PostgreSQL-authoritative [BoMAllowedEdgeCatalog] with write-through snapshot + Caffeine TTL.
 */
open class JpaBoMAllowedEdgeCatalog(
    private val repository: BoMAllowedEdgeRuleRepository,
    private val properties: ObjsCatalogProperties = ObjsCatalogProperties(),
    private val ticker: Ticker = Ticker.systemTicker(),
) : BoMAllowedEdgeCatalog {

    private val cache = InMemoryBoMAllowedEdgeCatalog()
    private val lock = Any()
    private val freshness = buildFreshnessCache(properties.cacheTtl, ticker)

    /** Load all persisted rules into the snapshot. Call at startup, on refresh, and after TX rollback. */
    open fun hydrate() {
        synchronized(lock) {
            hydrateUnlocked()
        }
    }

    override fun refreshFromStore() = hydrate()

    @Transactional
    override fun register(rule: BoMAllowedEdgeRule) {
        val now = Instant.now()
        val id = BoMAllowedEdgeRuleId(rule.sourceType, rule.role, rule.targetType)
        synchronized(lock) {
            val record = repository.findById(id).orElseGet {
                BoMAllowedEdgeRuleRecord(
                    sourceType = rule.sourceType,
                    role = rule.role,
                    targetType = rule.targetType,
                    createdAt = now,
                )
            }
            record.propertiesPolicy = rule.propertiesPolicy
            record.emptyPropertiesAllowed = rule.emptyPropertiesAllowed
            record.propertiesSchemaType = rule.propertiesSchemaType
            record.propertiesSchemaVersion = rule.propertiesSchemaVersion
            record.cardinality = rule.cardinality
            record.description = CatalogMetadata.optionalText(rule.description)
            record.sourceVerb = CatalogMetadata.optionalText(rule.sourceVerb)
            record.targetVerb = CatalogMetadata.optionalText(rule.targetVerb)
            record.tags = CatalogMetadata.tags(rule.tags).toMutableList()
            record.attributes = CatalogMetadata.attributes(rule.attributes).toMutableMap()
            record.updatedAt = now
            repository.save(record)
            cache.register(record.toDomain())
            markFresh()
            registerRollbackRehydration()
        }
    }

    override fun find(sourceType: String, role: String, targetType: String): BoMAllowedEdgeRule? {
        ensureFresh()
        return cache.find(sourceType, role, targetType)
    }

    override fun all(): Collection<BoMAllowedEdgeRule> {
        ensureFresh()
        return cache.all()
    }

    @Transactional
    override fun remove(sourceType: String, role: String, targetType: String): Boolean {
        val id = BoMAllowedEdgeRuleId(sourceType, role, targetType)
        synchronized(lock) {
            if (!repository.existsById(id)) return false
            repository.deleteById(id)
            cache.remove(sourceType, role, targetType)
            markFresh()
            registerRollbackRehydration()
            return true
        }
    }

    @Transactional
    override fun clear() {
        synchronized(lock) {
            repository.deleteAll()
            cache.clear()
            markFresh()
            registerRollbackRehydration()
        }
    }

    private fun ensureFresh() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) return
        if (isTtlDisabled(properties.cacheTtl)) return
        freshness.get(FRESHNESS_KEY) {
            synchronized(lock) {
                hydrateUnlocked()
            }
            true
        }
    }

    private fun hydrateUnlocked() {
        cache.clear()
        repository.findAll().forEach { record ->
            cache.register(record.toDomain())
        }
        markFresh()
    }

    private fun markFresh() {
        if (!isTtlDisabled(properties.cacheTtl)) {
            freshness.put(FRESHNESS_KEY, true)
        }
    }

    private fun registerRollbackRehydration() {
        registerCatalogRollbackRehydration { hydrate() }
    }
}

private const val FRESHNESS_KEY = true

private fun isTtlDisabled(ttl: Duration): Boolean = ttl.isZero || ttl.isNegative

private fun buildFreshnessCache(ttl: Duration, ticker: Ticker) =
    Caffeine.newBuilder()
        .ticker(ticker)
        .expireAfterWrite(
            if (isTtlDisabled(ttl)) Long.MAX_VALUE else ttl.toNanos(),
            TimeUnit.NANOSECONDS,
        )
        .build<Boolean, Boolean>()

private fun registerCatalogRollbackRehydration(hydrate: () -> Unit) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) return
    TransactionSynchronizationManager.registerSynchronization(
        object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    hydrate()
                }
            }
        },
    )
}

// ── Record ↔ Domain mappers ──

fun BoMSchemaCatalogRecord.toDomain() = BoMSchema(
    type = type,
    version = version,
    contentSchema = PayloadMapper.fromMap(definitionDoc, BoMSchemaNode::class.java),
    usage = if (usage.isBlank()) BoMSchemaUsage.ENTITY else BoMSchemaUsage.valueOf(usage),
    tags = CatalogMetadata.tags(tags),
    attributes = CatalogMetadata.attributes(attributes),
)

fun BoMAllowedEdgeRuleRecord.toDomain() = BoMAllowedEdgeRule(
    sourceType = sourceType,
    role = role,
    targetType = targetType,
    propertiesPolicy = propertiesPolicy,
    emptyPropertiesAllowed = emptyPropertiesAllowed,
    propertiesSchemaType = propertiesSchemaType,
    propertiesSchemaVersion = propertiesSchemaVersion,
    cardinality = cardinality,
    description = CatalogMetadata.optionalText(description),
    sourceVerb = CatalogMetadata.optionalText(sourceVerb),
    targetVerb = CatalogMetadata.optionalText(targetVerb),
    tags = CatalogMetadata.tags(tags),
    attributes = CatalogMetadata.attributes(attributes),
)
