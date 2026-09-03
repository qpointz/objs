package org.poc.objs.core.persistence

import org.poc.objs.core.persistence.tx.UnitOfWork
import java.util.UUID

class EntityVersionDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun findById(id: EntityVersionId): EntityVersionRecord? =
        em.find(EntityVersionRecord::class.java, id)

    fun save(entity: EntityVersionRecord): EntityVersionRecord {
        val id = EntityVersionId(entity.entityId, entity.version)
        return if (em.find(EntityVersionRecord::class.java, id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun findByEntityIdAndVersion(entityId: UUID, version: Long): EntityVersionRecord? =
        em.createQuery(
            """
            select v from BoMEntityVersionRecord v
            where v.entityId = :entityId and v.version = :version
            """.trimIndent(),
            EntityVersionRecord::class.java,
        )
            .setParameter("entityId", entityId)
            .setParameter("version", version)
            .resultList
            .firstOrNull()

    fun findByEntityIdOrderByVersionDesc(entityId: UUID): List<EntityVersionRecord> =
        em.createQuery(
            """
            select v from BoMEntityVersionRecord v
            where v.entityId = :entityId
            order by v.version desc
            """.trimIndent(),
            EntityVersionRecord::class.java,
        ).setParameter("entityId", entityId).resultList

    fun findByEntityIdOrderByVersionDesc(entityId: UUID, limit: Int): List<EntityVersionRecord> =
        em.createQuery(
            """
            select v from BoMEntityVersionRecord v
            where v.entityId = :entityId
            order by v.version desc
            """.trimIndent(),
            EntityVersionRecord::class.java,
        )
            .setParameter("entityId", entityId)
            .setMaxResults(limit)
            .resultList

    fun countByEntityId(entityId: UUID): Long =
        em.createQuery(
            "select count(v) from BoMEntityVersionRecord v where v.entityId = :entityId",
            Long::class.javaObjectType,
        ).setParameter("entityId", entityId).singleResult
}

class GraphVersionDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun save(entity: GraphVersionRecord): GraphVersionRecord {
        val id = GraphVersionId(entity.graphId, entity.version)
        return if (em.find(GraphVersionRecord::class.java, id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun findByGraphIdAndVersion(graphId: UUID, version: Long): GraphVersionRecord? =
        em.createQuery(
            """
            select v from BoMGraphVersionRecord v
            where v.graphId = :graphId and v.version = :version
            """.trimIndent(),
            GraphVersionRecord::class.java,
        )
            .setParameter("graphId", graphId)
            .setParameter("version", version)
            .resultList
            .firstOrNull()

    fun findByGraphIdOrderByVersionDesc(graphId: UUID): List<GraphVersionRecord> =
        em.createQuery(
            """
            select v from BoMGraphVersionRecord v
            where v.graphId = :graphId
            order by v.version desc
            """.trimIndent(),
            GraphVersionRecord::class.java,
        ).setParameter("graphId", graphId).resultList
}

class EdgeVersionDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun save(entity: EdgeVersionRecord): EdgeVersionRecord {
        val id = EdgeVersionId(entity.edgeId, entity.version)
        return if (em.find(EdgeVersionRecord::class.java, id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun findByEdgeIdAndVersion(edgeId: UUID, version: Long): EdgeVersionRecord? =
        em.createQuery(
            """
            select v from BoMEdgeVersionRecord v
            where v.edgeId = :edgeId and v.version = :version
            """.trimIndent(),
            EdgeVersionRecord::class.java,
        )
            .setParameter("edgeId", edgeId)
            .setParameter("version", version)
            .resultList
            .firstOrNull()

    fun findByEdgeIdOrderByVersionDesc(edgeId: UUID): List<EdgeVersionRecord> =
        em.createQuery(
            """
            select v from BoMEdgeVersionRecord v
            where v.edgeId = :edgeId
            order by v.version desc
            """.trimIndent(),
            EdgeVersionRecord::class.java,
        ).setParameter("edgeId", edgeId).resultList

    fun findByEdgeIdOrderByVersionDesc(edgeId: UUID, limit: Int): List<EdgeVersionRecord> =
        em.createQuery(
            """
            select v from BoMEdgeVersionRecord v
            where v.edgeId = :edgeId
            order by v.version desc
            """.trimIndent(),
            EdgeVersionRecord::class.java,
        )
            .setParameter("edgeId", edgeId)
            .setMaxResults(limit)
            .resultList

    fun countByEdgeId(edgeId: UUID): Long =
        em.createQuery(
            "select count(v) from BoMEdgeVersionRecord v where v.edgeId = :edgeId",
            Long::class.javaObjectType,
        ).setParameter("edgeId", edgeId).singleResult
}

class GraphVersionMemberDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun save(entity: GraphVersionMemberRecord): GraphVersionMemberRecord {
        val id = GraphVersionMemberId(entity.graphId, entity.graphVersion, entity.entityId)
        return if (em.find(GraphVersionMemberRecord::class.java, id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun findByGraphIdAndGraphVersion(graphId: UUID, graphVersion: Long): List<GraphVersionMemberRecord> =
        em.createQuery(
            """
            select m from BoMGraphVersionMemberRecord m
            where m.graphId = :graphId and m.graphVersion = :graphVersion
            """.trimIndent(),
            GraphVersionMemberRecord::class.java,
        )
            .setParameter("graphId", graphId)
            .setParameter("graphVersion", graphVersion)
            .resultList

    fun findDistinctGraphIdsByEntityId(entityId: UUID): List<UUID> =
        em.createQuery(
            """
            select distinct m.graphId from BoMGraphVersionMemberRecord m
            where m.entityId = :entityId
            """.trimIndent(),
            UUID::class.java,
        ).setParameter("entityId", entityId).resultList
}

class GraphVersionEdgeDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()
    fun save(entity: GraphVersionEdgeRecord): GraphVersionEdgeRecord {
        val id = GraphVersionEdgeId(entity.graphId, entity.graphVersion, entity.edgeId)
        return if (em.find(GraphVersionEdgeRecord::class.java, id) == null) {
            em.persist(entity)
            entity
        } else {
            em.merge(entity)
        }
    }

    fun findByGraphIdAndGraphVersion(graphId: UUID, graphVersion: Long): List<GraphVersionEdgeRecord> =
        em.createQuery(
            """
            select e from BoMGraphVersionEdgeRecord e
            where e.graphId = :graphId and e.graphVersion = :graphVersion
            """.trimIndent(),
            GraphVersionEdgeRecord::class.java,
        )
            .setParameter("graphId", graphId)
            .setParameter("graphVersion", graphVersion)
            .resultList
}
