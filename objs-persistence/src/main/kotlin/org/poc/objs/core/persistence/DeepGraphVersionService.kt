package org.poc.objs.core.persistence

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.GraphException
import org.poc.objs.api.domain.GraphVersionSummary
import org.poc.objs.api.domain.InstanceVersionStats
import org.poc.objs.api.domain.InstanceVersionSummary
import org.poc.objs.api.domain.ResolvedGraph
import org.poc.objs.core.persistence.tx.UnitOfWork
import java.time.Instant
import java.util.UUID
import kotlin.math.max

class DeepGraphVersionService(
    private val graphDao: GraphDao,
    private val membershipDao: GraphMembershipDao,
    private val entityDao: EntityDao,
    private val edgeDao: EdgeDao,
    private val entityVersions: EntityVersionDao,
    private val graphVersions: GraphVersionDao,
    private val edgeVersions: EdgeVersionDao,
    private val versionMembers: GraphVersionMemberDao,
    private val versionEdges: GraphVersionEdgeDao,
    private val uow: UnitOfWork,
) {
    fun createDeepGraphVersion(
        graphId: UUID,
        versionAnnotations: Map<String, String> = emptyMap(),
        at: Instant? = null,
    ): GraphVersionSummary = uow.write {
        val header = graphDao.findById(graphId)
            ?: throw GraphException(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId")
        val memberIds = membershipDao.findByGraphId(graphId).map { it.entityId }
        val entityRows = if (memberIds.isEmpty()) emptyList() else entityDao.findAllById(memberIds)
        val edgeRows = edgeDao.findByGraphId(graphId)
        val stamp = at ?: Instant.now()
        val graphVersion = nextVersion(header.headVersion, stamp)
        graphVersions.save(
            GraphVersionRecord(
                graphId = graphId,
                version = graphVersion,
                graphAnnotations = header.annotations.toMutableMap(),
                annotations = versionAnnotations.toMutableMap(),
                createdAt = stamp,
                updatedAt = stamp,
            ),
        )
        header.headVersion = graphVersion
        graphDao.save(header)

        for (row in entityRows) {
            val id = row.id
            val version = nextVersion(row.headVersion, stamp)
            entityVersions.save(copyEntityVersion(row, version, stamp))
            row.headVersion = version
            entityDao.save(row)
            versionMembers.save(
                GraphVersionMemberRecord(
                    graphId = graphId,
                    graphVersion = graphVersion,
                    entityId = id,
                    entityVersion = version,
                    createdAt = stamp,
                    updatedAt = stamp,
                ),
            )
        }

        for (row in edgeRows) {
            val id = row.id
            val version = nextVersion(row.headVersion, stamp)
            edgeVersions.save(copyEdgeVersion(row, version, stamp))
            row.headVersion = version
            edgeDao.save(row)
            versionEdges.save(
                GraphVersionEdgeRecord(
                    graphId = graphId,
                    graphVersion = graphVersion,
                    edgeId = id,
                    edgeVersion = version,
                    createdAt = stamp,
                    updatedAt = stamp,
                ),
            )
        }
        GraphVersionSummary(
            graphId = graphId,
            version = graphVersion,
            createdAt = stamp,
            annotations = versionAnnotations,
        )
    }

    fun listGraphVersions(graphId: UUID): List<GraphVersionSummary> = uow.read {
        graphVersions.findByGraphIdOrderByVersionDesc(graphId).map {
            GraphVersionSummary(
                graphId = it.graphId,
                version = it.version,
                createdAt = it.createdAt,
                annotations = it.annotations.toMap(),
            )
        }
    }

    fun getGraphVersion(graphId: UUID, version: Long): ResolvedGraph = uow.read {
        val header = graphVersions.findByGraphIdAndVersion(graphId, version)
            ?: throw GraphException(
                code = "GRAPH_VERSION_NOT_FOUND",
                message = "Graph version not found: $graphId@$version",
            )
        val entities = versionMembers.findByGraphIdAndGraphVersion(graphId, version).map { pin ->
            val row = entityVersions.findByEntityIdAndVersion(pin.entityId, pin.entityVersion)
                ?: throw GraphException(
                    code = "GRAPH_VERSION_NOT_FOUND",
                    message = "Entity version not found: ${pin.entityId}@${pin.entityVersion}",
                )
            Entity(
                id = row.entityId,
                type = row.type,
                schemaVersion = row.schemaVersion,
                payload = row.payload.toMutableMap(),
                annotations = row.annotations.toMutableMap(),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                headVersion = pin.entityVersion,
            )
        }
        val edges = versionEdges.findByGraphIdAndGraphVersion(graphId, version).map { pin ->
            val row = edgeVersions.findByEdgeIdAndVersion(pin.edgeId, pin.edgeVersion)
                ?: throw GraphException(
                    code = "GRAPH_VERSION_NOT_FOUND",
                    message = "Edge version not found: ${pin.edgeId}@${pin.edgeVersion}",
                )
            Edge(
                id = row.edgeId,
                graphId = row.graphId,
                source = row.sourceId,
                target = row.targetId,
                role = row.role,
                type = row.type,
                schemaVersion = row.schemaVersion,
                properties = row.properties?.toMutableMap(),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                headVersion = pin.edgeVersion,
            )
        }
        ResolvedGraph(
            id = graphId,
            annotations = header.graphAnnotations.toMap(),
            contents = GraphContents(entities = entities, edges = edges),
            createdAt = header.createdAt,
            updatedAt = header.updatedAt,
        )
    }

    fun listEntityVersions(entityId: UUID): List<InstanceVersionSummary> = uow.read {
        entityVersions.findByEntityIdOrderByVersionDesc(entityId).map { it.toEntitySummary() }
    }

    fun entityVersionStats(entityId: UUID, recent: Int = 5): InstanceVersionStats = uow.read {
        val n = recent.coerceIn(1, 50)
        val total = entityVersions.countByEntityId(entityId)
        val rows = entityVersions.findByEntityIdOrderByVersionDesc(entityId, n)
        InstanceVersionStats(total = total, recent = rows.map { it.toEntitySummary() })
    }

    fun getEntityVersion(entityId: UUID, version: Long): Entity = uow.read {
        val row = entityVersions.findByEntityIdAndVersion(entityId, version)
            ?: throw GraphException(
                code = "ENTITY_VERSION_NOT_FOUND",
                message = "Entity version not found: $entityId@$version",
            )
        Entity(
            id = row.entityId,
            type = row.type,
            schemaVersion = row.schemaVersion,
            payload = row.payload.toMutableMap(),
            annotations = row.annotations.toMutableMap(),
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            headVersion = row.version,
        )
    }

    fun listEdgeVersions(edgeId: UUID): List<InstanceVersionSummary> = uow.read {
        edgeVersions.findByEdgeIdOrderByVersionDesc(edgeId).map { it.toEdgeSummary() }
    }

    fun edgeVersionStats(edgeId: UUID, recent: Int = 5): InstanceVersionStats = uow.read {
        val n = recent.coerceIn(1, 50)
        val total = edgeVersions.countByEdgeId(edgeId)
        val rows = edgeVersions.findByEdgeIdOrderByVersionDesc(edgeId, n)
        InstanceVersionStats(total = total, recent = rows.map { it.toEdgeSummary() })
    }

    fun getEdgeVersion(edgeId: UUID, version: Long): Edge = uow.read {
        val row = edgeVersions.findByEdgeIdAndVersion(edgeId, version)
            ?: throw GraphException(
                code = "EDGE_VERSION_NOT_FOUND",
                message = "Edge version not found: $edgeId@$version",
            )
        Edge(
            id = row.edgeId,
            graphId = row.graphId,
            source = row.sourceId,
            target = row.targetId,
            role = row.role,
            type = row.type,
            schemaVersion = row.schemaVersion,
            properties = row.properties?.toMutableMap(),
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            headVersion = row.version,
        )
    }

    /**
     * Drop one deep freeze (pins + graph version row). Rejects if [version] is current head.
     * Does not GC entity/edge version orphans (use [compactEntity] / [compactEdge]).
     */
    fun purgeGraphVersion(graphId: UUID, version: Long) = uow.write {
        val header = graphDao.findById(graphId)
            ?: throw GraphException(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId")
        graphVersions.findByGraphIdAndVersion(graphId, version)
            ?: throw GraphException(
                code = "GRAPH_VERSION_NOT_FOUND",
                message = "Graph version not found: $graphId@$version",
            )
        if (header.headVersion == version) {
            throw GraphException(
                code = "GRAPH_VERSION_IS_HEAD",
                message = "Cannot purge current head version $version of graph $graphId",
            )
        }
        versionMembers.deleteByGraphIdAndGraphVersion(graphId, version)
        versionEdges.deleteByGraphIdAndGraphVersion(graphId, version)
        graphVersions.delete(graphId, version)
    }

    /**
     * Drop all deep freezes for [graphId] and set [GraphRecord.headVersion] to null.
     * Live HEAD membership/edges/payloads unchanged.
     */
    fun purgeAllGraphVersions(graphId: UUID) = uow.write {
        val header = graphDao.findById(graphId)
            ?: throw GraphException(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId")
        versionMembers.deleteAllByGraphId(graphId)
        versionEdges.deleteAllByGraphId(graphId)
        graphVersions.deleteAllByGraphId(graphId)
        header.headVersion = null
        graphDao.save(header)
    }

    /** Delete all graph version rows/pins for [graphId] (used by destroy; graph may still exist). */
    fun deleteGraphHistory(graphId: UUID) = uow.write {
        versionMembers.deleteAllByGraphId(graphId)
        versionEdges.deleteAllByGraphId(graphId)
        graphVersions.deleteAllByGraphId(graphId)
    }

    /** Purge freezes with version strictly greater than [afterVersion] (no head check). */
    fun purgeGraphVersionsAfter(graphId: UUID, afterVersion: Long) = uow.write {
        for (row in graphVersions.findByGraphIdOrderByVersionDesc(graphId)) {
            if (row.version <= afterVersion) continue
            versionMembers.deleteByGraphIdAndGraphVersion(graphId, row.version)
            versionEdges.deleteByGraphIdAndGraphVersion(graphId, row.version)
            graphVersions.delete(graphId, row.version)
        }
    }

    /**
     * Delete entity version rows not referenced by any graph pin and not equal to live head_version.
     * @return number of rows removed
     */
    fun compactEntity(entityId: UUID): Int = uow.write {
        val live = entityDao.findById(entityId)?.headVersion
        var removed = 0
        for (row in entityVersions.findByEntityIdOrderByVersionDesc(entityId)) {
            if (live != null && row.version == live) continue
            if (versionMembers.countByEntityIdAndEntityVersion(entityId, row.version) > 0L) continue
            entityVersions.delete(entityId, row.version)
            removed++
        }
        removed
    }

    /**
     * Delete edge version rows not referenced by any graph pin and not equal to live head_version.
     * @return number of rows removed
     */
    fun compactEdge(edgeId: UUID): Int = uow.write {
        val live = edgeDao.findById(edgeId)?.headVersion
        var removed = 0
        for (row in edgeVersions.findByEdgeIdOrderByVersionDesc(edgeId)) {
            if (live != null && row.version == live) continue
            if (versionEdges.countByEdgeIdAndEdgeVersion(edgeId, row.version) > 0L) continue
            edgeVersions.delete(edgeId, row.version)
            removed++
        }
        removed
    }

    companion object {
        fun nextVersion(previous: Long?, at: Instant = Instant.now()): Long {
            val millis = at.toEpochMilli()
            return max(millis, (previous ?: 0L) + 1L)
        }

        private fun EntityVersionRecord.toEntitySummary() = InstanceVersionSummary(
            id = entityId,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt,
            annotations = annotations.toMap(),
        )

        private fun EdgeVersionRecord.toEdgeSummary() = InstanceVersionSummary(
            id = edgeId,
            version = version,
            createdAt = createdAt,
            updatedAt = updatedAt,
            annotations = emptyMap(),
        )

        private fun copyEntityVersion(
            row: EntityRecord,
            version: Long,
            now: Instant,
        ) = EntityVersionRecord(
            entityId = row.id,
            version = version,
            type = row.type,
            schemaVersion = row.schemaVersion,
            payload = row.payload.toMutableMap(),
            annotations = row.annotations.toMutableMap(),
            createdAt = now,
            updatedAt = now,
        )

        private fun copyEdgeVersion(
            row: EdgeRecord,
            version: Long,
            now: Instant,
        ) = EdgeVersionRecord(
            edgeId = row.id,
            version = version,
            graphId = row.graphId,
            sourceId = row.sourceId,
            targetId = row.targetId,
            role = row.role,
            type = row.type,
            schemaVersion = row.schemaVersion,
            properties = row.properties?.toMutableMap(),
            createdAt = now,
            updatedAt = now,
        )
    }
}
