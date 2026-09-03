package org.poc.objs.core.persistence

import org.poc.objs.core.persistence.tx.UnitOfWork
import java.util.UUID

data class TypeCount(val type: String, val cnt: Long)

class EntityDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun findById(id: UUID): EntityRecord? = em.find(EntityRecord::class.java, id)

    fun existsById(id: UUID): Boolean = findById(id) != null

    fun save(entity: EntityRecord): EntityRecord {
        val id = entity.id
        return if (em.find(EntityRecord::class.java, id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun deleteById(id: UUID) {
        findById(id)?.let { em.remove(it) }
    }

    fun delete(entity: EntityRecord) {
        val managed = if (em.contains(entity)) entity else em.merge(entity)
        em.remove(managed)
    }

    fun saveAll(entities: Iterable<EntityRecord>): List<EntityRecord> = entities.map { save(it) }

    fun flush() {
        em.flush()
    }

    fun findAll(): List<EntityRecord> =
        em.createQuery("select e from BoMEntityRecord e", EntityRecord::class.java).resultList

    fun findAllById(ids: Collection<UUID>): List<EntityRecord> {
        if (ids.isEmpty()) return emptyList()
        return em.createQuery(
            "select e from BoMEntityRecord e where e.id in :ids",
            EntityRecord::class.java,
        ).setParameter("ids", ids.toList()).resultList
    }

    fun deleteAll() {
        em.createQuery("delete from BoMEntityRecord e").executeUpdate()
    }

    fun count(): Long =
        em.createQuery("select count(e) from BoMEntityRecord e", Long::class.javaObjectType).singleResult

    fun countGroupedByType(): List<TypeCount> {
        val rows = em.createQuery("select e.type, count(e) from BoMEntityRecord e group by e.type").resultList
        return rows.map { row ->
            val cols = row as Array<*>
            TypeCount(type = cols[0] as String, cnt = (cols[1] as Number).toLong())
        }
    }

    fun countGroupedByTypeInGraph(graphId: UUID): List<TypeCount> {
        val rows = em.createQuery(
            """
            select e.type, count(e)
            from BoMEntityRecord e, BoMGraphMembershipRecord m
            where m.entityId = e.id and m.graphId = :graphId
            group by e.type
            """.trimIndent(),
        ).setParameter("graphId", graphId).resultList
        return rows.map { row ->
            val cols = row as Array<*>
            TypeCount(type = cols[0] as String, cnt = (cols[1] as Number).toLong())
        }
    }
}

class EdgeDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun findById(id: UUID): EdgeRecord? = em.find(EdgeRecord::class.java, id)

    fun existsById(id: UUID): Boolean = findById(id) != null

    fun save(entity: EdgeRecord): EdgeRecord {
        return if (em.find(EdgeRecord::class.java, entity.id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun deleteById(id: UUID) {
        findById(id)?.let { em.remove(it) }
    }

    fun delete(entity: EdgeRecord) {
        val managed = if (em.contains(entity)) entity else em.merge(entity)
        em.remove(managed)
    }

    fun deleteAllById(ids: Collection<UUID>) {
        ids.forEach { deleteById(it) }
    }

    fun saveAll(entities: Iterable<EdgeRecord>): List<EdgeRecord> = entities.map { save(it) }

    fun flush() {
        em.flush()
    }

    fun deleteAll() {
        em.createQuery("delete from BoMEdgeRecord e").executeUpdate()
    }

    fun findAll(): List<EdgeRecord> =
        em.createQuery("select e from BoMEdgeRecord e", EdgeRecord::class.java).resultList

    fun findBySourceIdInAndTargetIdIn(
        sourceIds: Collection<UUID>,
        targetIds: Collection<UUID>,
    ): List<EdgeRecord> {
        if (sourceIds.isEmpty() || targetIds.isEmpty()) return emptyList()
        return em.createQuery(
            """
            select e from BoMEdgeRecord e
            where e.sourceId in :sourceIds and e.targetId in :targetIds
            """.trimIndent(),
            EdgeRecord::class.java,
        )
            .setParameter("sourceIds", sourceIds.toList())
            .setParameter("targetIds", targetIds.toList())
            .resultList
    }

    fun findBySourceIdInOrTargetIdIn(
        sourceIds: Collection<UUID>,
        targetIds: Collection<UUID>,
    ): List<EdgeRecord> {
        val sources = sourceIds.toList()
        val targets = targetIds.toList()
        if (sources.isEmpty() && targets.isEmpty()) return emptyList()
        if (sources.isEmpty()) {
            return em.createQuery(
                "select e from BoMEdgeRecord e where e.targetId in :targetIds",
                EdgeRecord::class.java,
            ).setParameter("targetIds", targets).resultList
        }
        if (targets.isEmpty()) {
            return em.createQuery(
                "select e from BoMEdgeRecord e where e.sourceId in :sourceIds",
                EdgeRecord::class.java,
            ).setParameter("sourceIds", sources).resultList
        }
        return em.createQuery(
            """
            select e from BoMEdgeRecord e
            where e.sourceId in :sourceIds or e.targetId in :targetIds
            """.trimIndent(),
            EdgeRecord::class.java,
        )
            .setParameter("sourceIds", sources)
            .setParameter("targetIds", targets)
            .resultList
    }

    fun findByGraphId(graphId: UUID): List<EdgeRecord> =
        em.createQuery(
            "select e from BoMEdgeRecord e where e.graphId = :graphId",
            EdgeRecord::class.java,
        ).setParameter("graphId", graphId).resultList

    fun countByGraphId(graphId: UUID): Long =
        em.createQuery(
            "select count(e) from BoMEdgeRecord e where e.graphId = :graphId",
            Long::class.javaObjectType,
        ).setParameter("graphId", graphId).singleResult

    fun findIncident(entityId: UUID): List<EdgeRecord> =
        em.createQuery(
            "select e from BoMEdgeRecord e where e.sourceId = :entityId or e.targetId = :entityId",
            EdgeRecord::class.java,
        ).setParameter("entityId", entityId).resultList

    fun findIncidentInGraph(entityId: UUID, graphId: UUID): List<EdgeRecord> =
        em.createQuery(
            """
            select e from BoMEdgeRecord e
            where e.graphId = :graphId and (e.sourceId = :entityId or e.targetId = :entityId)
            """.trimIndent(),
            EdgeRecord::class.java,
        )
            .setParameter("graphId", graphId)
            .setParameter("entityId", entityId)
            .resultList
}

class GraphDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun findById(id: UUID): GraphRecord? = em.find(GraphRecord::class.java, id)

    fun existsById(id: UUID): Boolean = findById(id) != null

    fun save(entity: GraphRecord): GraphRecord {
        return if (em.find(GraphRecord::class.java, entity.id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun flush() {
        em.flush()
    }

    fun deleteById(id: UUID) {
        findById(id)?.let { em.remove(it) }
    }

    fun findAll(): List<GraphRecord> =
        em.createQuery("select g from BoMGraphRecord g", GraphRecord::class.java).resultList

    fun findAllById(ids: Collection<UUID>): List<GraphRecord> {
        if (ids.isEmpty()) return emptyList()
        return em.createQuery(
            "select g from BoMGraphRecord g where g.id in :ids",
            GraphRecord::class.java,
        ).setParameter("ids", ids.toList()).resultList
    }
}

class GraphMembershipDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun findById(id: GraphMembershipId): GraphMembershipRecord? =
        em.find(GraphMembershipRecord::class.java, id)

    fun save(entity: GraphMembershipRecord): GraphMembershipRecord {
        val id = GraphMembershipId(entity.graphId, entity.entityId)
        return if (em.find(GraphMembershipRecord::class.java, id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun saveAll(entities: Iterable<GraphMembershipRecord>): List<GraphMembershipRecord> =
        entities.map { save(it) }

    fun findByGraphId(graphId: UUID): List<GraphMembershipRecord> =
        em.createQuery(
            "select m from BoMGraphMembershipRecord m where m.graphId = :graphId",
            GraphMembershipRecord::class.java,
        ).setParameter("graphId", graphId).resultList

    fun findByEntityId(entityId: UUID): List<GraphMembershipRecord> =
        em.createQuery(
            "select m from BoMGraphMembershipRecord m where m.entityId = :entityId",
            GraphMembershipRecord::class.java,
        ).setParameter("entityId", entityId).resultList

    fun deleteByGraphId(graphId: UUID) {
        em.createQuery("delete from BoMGraphMembershipRecord m where m.graphId = :graphId")
            .setParameter("graphId", graphId)
            .executeUpdate()
    }

    fun deleteByGraphIdAndEntityId(graphId: UUID, entityId: UUID) {
        em.createQuery(
            "delete from BoMGraphMembershipRecord m where m.graphId = :graphId and m.entityId = :entityId",
        )
            .setParameter("graphId", graphId)
            .setParameter("entityId", entityId)
            .executeUpdate()
    }

    fun countByGraphId(graphId: UUID): Long =
        em.createQuery(
            "select count(m) from BoMGraphMembershipRecord m where m.graphId = :graphId",
            Long::class.javaObjectType,
        ).setParameter("graphId", graphId).singleResult
}
