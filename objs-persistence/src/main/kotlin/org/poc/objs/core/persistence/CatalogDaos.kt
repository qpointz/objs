package org.poc.objs.core.persistence

import org.poc.objs.core.persistence.tx.UnitOfWork

class SchemaCatalogDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun findById(id: SchemaCatalogId): SchemaCatalogRecord? =
        em.find(SchemaCatalogRecord::class.java, id)

    fun existsById(id: SchemaCatalogId): Boolean = findById(id) != null

    fun save(entity: SchemaCatalogRecord): SchemaCatalogRecord {
        val id = SchemaCatalogId(entity.type, entity.version)
        return if (em.find(SchemaCatalogRecord::class.java, id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun deleteById(id: SchemaCatalogId) {
        findById(id)?.let { em.remove(it) }
    }

    fun findAll(): List<SchemaCatalogRecord> =
        em.createQuery("select s from BoMSchemaCatalogRecord s", SchemaCatalogRecord::class.java)
            .resultList

    fun deleteAll() {
        em.createQuery("delete from BoMSchemaCatalogRecord s").executeUpdate()
    }

    fun flush() {
        em.flush()
    }

    fun count(): Long =
        em.createQuery("select count(s) from BoMSchemaCatalogRecord s", Long::class.javaObjectType)
            .singleResult
}

class AllowedEdgeRuleDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun findById(id: AllowedEdgeRuleId): AllowedEdgeRuleRecord? =
        em.find(AllowedEdgeRuleRecord::class.java, id)

    fun existsById(id: AllowedEdgeRuleId): Boolean = findById(id) != null

    fun save(entity: AllowedEdgeRuleRecord): AllowedEdgeRuleRecord {
        val id = AllowedEdgeRuleId(entity.sourceType, entity.role, entity.targetType)
        return if (em.find(AllowedEdgeRuleRecord::class.java, id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun deleteById(id: AllowedEdgeRuleId) {
        findById(id)?.let { em.remove(it) }
    }

    fun findAll(): List<AllowedEdgeRuleRecord> =
        em.createQuery(
            "select r from BoMAllowedEdgeRuleRecord r",
            AllowedEdgeRuleRecord::class.java,
        ).resultList

    fun deleteAll() {
        em.createQuery("delete from BoMAllowedEdgeRuleRecord r").executeUpdate()
    }

    fun flush() {
        em.flush()
    }

    fun count(): Long =
        em.createQuery("select count(r) from BoMAllowedEdgeRuleRecord r", Long::class.javaObjectType)
            .singleResult
}

class SeedLedgerDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun findById(seedKey: String): SeedLedgerRecord? =
        em.find(SeedLedgerRecord::class.java, seedKey)

    fun save(entity: SeedLedgerRecord): SeedLedgerRecord {
        return if (em.find(SeedLedgerRecord::class.java, entity.seedKey) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun deleteAll() {
        em.createQuery("delete from BoMSeedLedgerRecord s").executeUpdate()
    }
}
