package org.poc.objs.core.persistence

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
import java.time.Instant

/**
 * PostgreSQL-authoritative [BoMSchemaCatalog] with an in-memory write-through cache.
 * Reads come from the cache; writes go to PostgreSQL first, then update the cache.
 * On transaction rollback the cache is rehydrated from PostgreSQL so mid-transaction
 * visibility (needed by seed imports) never outlives a failed resource transaction.
 */
open class JpaBoMSchemaCatalog(
    private val repository: BoMSchemaCatalogRepository,
) : BoMSchemaCatalog {

    private val cache = InMemoryBoMSchemaCatalog()

    /** Load all persisted schemas into the cache. Call once before traffic. */
    open fun hydrate() {
        cache.clear()
        repository.findAll().forEach { record ->
            cache.register(record.toDomain())
        }
    }

    @Transactional
    override fun register(schema: BoMSchema) {
        val normalized = BoMSchemaNormalizer.normalizeStrict(schema)
        val now = Instant.now()
        val id = BoMSchemaCatalogId(normalized.type, normalized.version)
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
        registerRollbackRehydration()
    }

    override fun get(type: String, version: String): BoMSchema? = cache.get(type, version)

    override fun get(key: BoMSchemaKey): BoMSchema? = cache.get(key)

    override fun contains(type: String, version: String): Boolean = cache.contains(type, version)

    override fun all(): Collection<BoMSchema> = cache.all()

    override fun listByType(type: String): List<BoMSchema> = cache.listByType(type)

    override fun types(): Set<String> = cache.types()

    @Transactional
    override fun remove(type: String, version: String): Boolean {
        val id = BoMSchemaCatalogId(type, version)
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        cache.remove(type, version)
        registerRollbackRehydration()
        return true
    }

    @Transactional
    override fun clear() {
        repository.deleteAll()
        cache.clear()
        registerRollbackRehydration()
    }

    private fun registerRollbackRehydration() {
        registerCatalogRollbackRehydration { hydrate() }
    }
}

/**
 * PostgreSQL-authoritative [BoMAllowedEdgeCatalog] with an in-memory write-through cache.
 */
open class JpaBoMAllowedEdgeCatalog(
    private val repository: BoMAllowedEdgeRuleRepository,
) : BoMAllowedEdgeCatalog {

    private val cache = InMemoryBoMAllowedEdgeCatalog()

    /** Load all persisted rules into the cache. Call once before traffic. */
    open fun hydrate() {
        cache.clear()
        repository.findAll().forEach { record ->
            cache.register(record.toDomain())
        }
    }

    @Transactional
    override fun register(rule: BoMAllowedEdgeRule) {
        val now = Instant.now()
        val id = BoMAllowedEdgeRuleId(rule.sourceType, rule.role, rule.targetType)
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
        registerRollbackRehydration()
    }

    override fun find(sourceType: String, role: String, targetType: String): BoMAllowedEdgeRule? =
        cache.find(sourceType, role, targetType)

    override fun all(): Collection<BoMAllowedEdgeRule> = cache.all()

    @Transactional
    override fun remove(sourceType: String, role: String, targetType: String): Boolean {
        val id = BoMAllowedEdgeRuleId(sourceType, role, targetType)
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        cache.remove(sourceType, role, targetType)
        registerRollbackRehydration()
        return true
    }

    @Transactional
    override fun clear() {
        repository.deleteAll()
        cache.clear()
        registerRollbackRehydration()
    }

    private fun registerRollbackRehydration() {
        registerCatalogRollbackRehydration { hydrate() }
    }
}

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
