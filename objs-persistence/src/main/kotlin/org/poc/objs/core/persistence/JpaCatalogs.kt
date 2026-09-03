package org.poc.objs.core.persistence

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import org.poc.objs.api.domain.CatalogMetadata
import org.poc.objs.api.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.api.domain.Schema
import org.poc.objs.api.domain.SchemaCatalog
import org.poc.objs.api.domain.SchemaKey
import org.poc.objs.api.domain.SchemaNode
import org.poc.objs.api.domain.SchemaNormalizer
import org.poc.objs.api.domain.SchemaUsage
import org.poc.objs.api.domain.InMemoryAllowedEdgeCatalog
import org.poc.objs.api.domain.InMemorySchemaCatalog
import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.core.typed.DefaultPayloadMapper
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * PostgreSQL-authoritative [SchemaCatalog] with a write-through in-memory snapshot and
 * Caffeine TTL revalidation.
 *
 * Reads use the snapshot. Writes persist first, then update the snapshot and reset the TTL clock.
 * When the TTL expires and no transaction is active, the next read rehydrates from PostgreSQL
 * (so out-of-band truncates become visible without restart). Mid-TX reads skip TTL reload so seed
 * import keeps write-through visibility. On transaction rollback the snapshot is rehydrated.
 */
open class JpaSchemaCatalog(
    private val dao: SchemaCatalogDao,
    private val uow: UnitOfWork,
    private val properties: ObjsCatalogProperties = ObjsCatalogProperties(),
    private val ticker: Ticker = Ticker.systemTicker(),
) : SchemaCatalog {

    private val cache = InMemorySchemaCatalog()
    private val lock = Any()
    private val freshness = buildFreshnessCache(properties.cacheTtl, ticker)

    /** Load all persisted schemas into the snapshot. Call at startup, on refresh, and after TX rollback. */
    open fun hydrate() {
        uow.read {
            synchronized(lock) {
                hydrateUnlocked()
            }
        }
    }

    override fun refreshFromStore() = hydrate()

    override fun register(schema: Schema) {
        uow.write {
            val normalized = SchemaNormalizer.normalizeStrict(schema)
            val now = Instant.now()
            val id = SchemaCatalogId(normalized.type, normalized.version)
            synchronized(lock) {
                val record = dao.findById(id) ?: SchemaCatalogRecord(
                    type = normalized.type,
                    version = normalized.version,
                    createdAt = now,
                )
                record.definitionDoc = DefaultPayloadMapper.toMap(normalized.contentSchema)
                record.usage = normalized.usage.name
                record.tags = normalized.tags.toMutableList()
                record.attributes = normalized.attributes.toMutableMap()
                record.updatedAt = now
                dao.save(record)
                cache.register(normalized)
                markFresh()
                registerRollbackRehydration()
            }
        }
    }

    override fun get(type: String, version: String): Schema? {
        ensureFresh()
        return cache.get(type, version)
    }

    override fun get(key: SchemaKey): Schema? {
        ensureFresh()
        return cache.get(key)
    }

    override fun contains(type: String, version: String): Boolean {
        ensureFresh()
        return cache.contains(type, version)
    }

    override fun all(): Collection<Schema> {
        ensureFresh()
        return cache.all()
    }

    override fun listByType(type: String): List<Schema> {
        ensureFresh()
        return cache.listByType(type)
    }

    override fun types(): Set<String> {
        ensureFresh()
        return cache.types()
    }

    override fun remove(type: String, version: String): Boolean {
        val id = SchemaCatalogId(type, version)
        return uow.write {
            synchronized(lock) {
                if (!dao.existsById(id)) return@write false
                dao.deleteById(id)
                cache.remove(type, version)
                markFresh()
                registerRollbackRehydration()
                true
            }
        }
    }

    override fun clear() {
        uow.write {
            synchronized(lock) {
                dao.deleteAll()
                cache.clear()
                markFresh()
                registerRollbackRehydration()
            }
        }
    }

    private fun ensureFresh() {
        if (uow.isActive()) return
        if (isTtlDisabled(properties.cacheTtl)) return
        freshness.get(FRESHNESS_KEY) {
            uow.read {
                synchronized(lock) {
                    hydrateUnlocked()
                }
            }
            true
        }
    }

    private fun hydrateUnlocked() {
        cache.clear()
        dao.findAll().forEach { record ->
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
        registerCatalogRollbackRehydration(uow) { hydrate() }
    }
}

/**
 * PostgreSQL-authoritative [AllowedEdgeCatalog] with write-through snapshot + Caffeine TTL.
 */
open class JpaAllowedEdgeCatalog(
    private val dao: AllowedEdgeRuleDao,
    private val uow: UnitOfWork,
    private val properties: ObjsCatalogProperties = ObjsCatalogProperties(),
    private val ticker: Ticker = Ticker.systemTicker(),
) : AllowedEdgeCatalog {

    private val cache = InMemoryAllowedEdgeCatalog()
    private val lock = Any()
    private val freshness = buildFreshnessCache(properties.cacheTtl, ticker)

    /** Load all persisted rules into the snapshot. Call at startup, on refresh, and after TX rollback. */
    open fun hydrate() {
        uow.read {
            synchronized(lock) {
                hydrateUnlocked()
            }
        }
    }

    override fun refreshFromStore() = hydrate()

    override fun register(rule: AllowedEdgeRule) {
        uow.write {
            val now = Instant.now()
            val id = AllowedEdgeRuleId(rule.sourceType, rule.role, rule.targetType)
            synchronized(lock) {
                val record = dao.findById(id) ?: AllowedEdgeRuleRecord(
                    sourceType = rule.sourceType,
                    role = rule.role,
                    targetType = rule.targetType,
                    createdAt = now,
                )
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
                dao.save(record)
                cache.register(record.toDomain())
                markFresh()
                registerRollbackRehydration()
            }
        }
    }

    override fun find(sourceType: String, role: String, targetType: String): AllowedEdgeRule? {
        ensureFresh()
        return cache.find(sourceType, role, targetType)
    }

    override fun all(): Collection<AllowedEdgeRule> {
        ensureFresh()
        return cache.all()
    }

    override fun remove(sourceType: String, role: String, targetType: String): Boolean {
        val id = AllowedEdgeRuleId(sourceType, role, targetType)
        return uow.write {
            synchronized(lock) {
                if (!dao.existsById(id)) return@write false
                dao.deleteById(id)
                cache.remove(sourceType, role, targetType)
                markFresh()
                registerRollbackRehydration()
                true
            }
        }
    }

    override fun clear() {
        uow.write {
            synchronized(lock) {
                dao.deleteAll()
                cache.clear()
                markFresh()
                registerRollbackRehydration()
            }
        }
    }

    private fun ensureFresh() {
        if (uow.isActive()) return
        if (isTtlDisabled(properties.cacheTtl)) return
        freshness.get(FRESHNESS_KEY) {
            uow.read {
                synchronized(lock) {
                    hydrateUnlocked()
                }
            }
            true
        }
    }

    private fun hydrateUnlocked() {
        cache.clear()
        dao.findAll().forEach { record ->
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
        registerCatalogRollbackRehydration(uow) { hydrate() }
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

private fun registerCatalogRollbackRehydration(uow: UnitOfWork, hydrate: () -> Unit) {
    if (!uow.isActive()) return
    uow.afterRollback(hydrate)
}

// ── Record ↔ Domain mappers ──

fun SchemaCatalogRecord.toDomain() = Schema(
    type = type,
    version = version,
    contentSchema = DefaultPayloadMapper.fromMap(definitionDoc, SchemaNode::class.java),
    usage = if (usage.isBlank()) SchemaUsage.ENTITY else SchemaUsage.valueOf(usage),
    tags = CatalogMetadata.tags(tags),
    attributes = CatalogMetadata.attributes(attributes),
)

fun AllowedEdgeRuleRecord.toDomain() = AllowedEdgeRule(
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
