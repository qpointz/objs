package org.poc.objs.core.persistence

import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaKey
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.InMemoryBoMAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemoryBoMSchemaCatalog
import org.poc.objs.core.domain.findMostSpecificRule
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * PostgreSQL-authoritative [BoMSchemaCatalog] with an in-memory write-through cache.
 * Reads come from the cache; writes go to PostgreSQL first, then update the cache.
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
        val now = Instant.now()
        val id = BoMSchemaCatalogId(schema.type, schema.version)
        val record = repository.findById(id).orElseGet {
            BoMSchemaCatalogRecord(
                type = schema.type,
                version = schema.version,
                createdAt = now,
            )
        }
        record.schemaDoc = schema.schema.toMutableMap()
        record.updatedAt = now
        repository.save(record)
        cache.register(schema)
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
        return true
    }

    @Transactional
    override fun clear() {
        repository.deleteAll()
        cache.clear()
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
        record.updatedAt = now
        repository.save(record)
        cache.register(rule)
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
        return true
    }

    @Transactional
    override fun clear() {
        repository.deleteAll()
        cache.clear()
    }
}

// ── Record ↔ Domain mappers ──

fun BoMSchemaCatalogRecord.toDomain() = BoMSchema(
    type = type,
    version = version,
    schema = schemaDoc.toMap(),
)

fun BoMAllowedEdgeRuleRecord.toDomain() = BoMAllowedEdgeRule(
    sourceType = sourceType,
    role = role,
    targetType = targetType,
    propertiesPolicy = propertiesPolicy,
    emptyPropertiesAllowed = emptyPropertiesAllowed,
)
