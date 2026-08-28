package org.poc.objs.core.persistence

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.core.domain.GraphException
import org.poc.objs.core.domain.GraphVersionSummary
import org.poc.objs.core.domain.InstanceVersionStats
import org.poc.objs.core.domain.InstanceVersionSummary
import org.poc.objs.core.domain.ResolvedGraph
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.math.max

@Service
class DeepGraphVersionService(
    private val graphRepository: GraphRepository,
    private val membershipRepository: GraphMembershipRepository,
    private val entityRepository: EntityRepository,
    private val edgeRepository: EdgeRepository,
    private val entityVersions: EntityVersionRepository,
    private val graphVersions: GraphVersionRepository,
    private val edgeVersions: EdgeVersionRepository,
    private val versionMembers: GraphVersionMemberRepository,
    private val versionEdges: GraphVersionEdgeRepository,
) {
    @Transactional
    fun createDeepGraphVersion(
        graphId: UUID,
        versionAnnotations: Map<String, String> = emptyMap(),
    ): GraphVersionSummary {
        val header = graphRepository.findById(graphId).orElse(null)
            ?: throw GraphException(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId")
        val memberIds = membershipRepository.findByGraphId(graphId).map { it.entityId }
        val entityRows = if (memberIds.isEmpty()) emptyList() else entityRepository.findAllById(memberIds)
        val edgeRows = edgeRepository.findByGraphId(graphId)
        val now = Instant.now()
        val graphVersion = nextVersion(header.headVersion)
        graphVersions.save(
            GraphVersionRecord(
                graphId = graphId,
                version = graphVersion,
                graphAnnotations = header.annotations.toMutableMap(),
                annotations = versionAnnotations.toMutableMap(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        header.headVersion = graphVersion
        graphRepository.save(header)

        for (row in entityRows) {
            val id = row.id
            val version = nextVersion(row.headVersion)
            entityVersions.save(copyEntityVersion(row, version, now))
            row.headVersion = version
            entityRepository.save(row)
            versionMembers.save(
                GraphVersionMemberRecord(
                    graphId = graphId,
                    graphVersion = graphVersion,
                    entityId = id,
                    entityVersion = version,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        for (row in edgeRows) {
            val id = row.id
            val version = nextVersion(row.headVersion)
            edgeVersions.save(copyEdgeVersion(row, version, now))
            row.headVersion = version
            edgeRepository.save(row)
            versionEdges.save(
                GraphVersionEdgeRecord(
                    graphId = graphId,
                    graphVersion = graphVersion,
                    edgeId = id,
                    edgeVersion = version,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        return GraphVersionSummary(
            graphId = graphId,
            version = graphVersion,
            createdAt = now,
            annotations = versionAnnotations,
        )
    }

    @Transactional(readOnly = true)
    fun listGraphVersions(graphId: UUID): List<GraphVersionSummary> =
        graphVersions.findByGraphIdOrderByVersionDesc(graphId).map {
            GraphVersionSummary(
                graphId = it.graphId,
                version = it.version,
                createdAt = it.createdAt,
                annotations = it.annotations.toMap(),
            )
        }

    @Transactional(readOnly = true)
    fun getGraphVersion(graphId: UUID, version: Long): ResolvedGraph {
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
        return ResolvedGraph(
            id = graphId,
            annotations = header.graphAnnotations.toMap(),
            contents = GraphContents(entities = entities, edges = edges),
            createdAt = header.createdAt,
            updatedAt = header.updatedAt,
        )
    }

    @Transactional(readOnly = true)
    fun listEntityVersions(entityId: UUID): List<InstanceVersionSummary> =
        entityVersions.findByEntityIdOrderByVersionDesc(entityId).map { it.toEntitySummary() }

    @Transactional(readOnly = true)
    fun entityVersionStats(entityId: UUID, recent: Int = 5): InstanceVersionStats {
        val n = recent.coerceIn(1, 50)
        val total = entityVersions.countByEntityId(entityId)
        val rows = entityVersions.findByEntityIdOrderByVersionDesc(entityId, PageRequest.of(0, n))
        return InstanceVersionStats(total = total, recent = rows.map { it.toEntitySummary() })
    }

    @Transactional(readOnly = true)
    fun getEntityVersion(entityId: UUID, version: Long): Entity {
        val row = entityVersions.findByEntityIdAndVersion(entityId, version)
            ?: throw GraphException(
                code = "ENTITY_VERSION_NOT_FOUND",
                message = "Entity version not found: $entityId@$version",
            )
        return Entity(
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

    @Transactional(readOnly = true)
    fun listEdgeVersions(edgeId: UUID): List<InstanceVersionSummary> =
        edgeVersions.findByEdgeIdOrderByVersionDesc(edgeId).map { it.toEdgeSummary() }

    @Transactional(readOnly = true)
    fun edgeVersionStats(edgeId: UUID, recent: Int = 5): InstanceVersionStats {
        val n = recent.coerceIn(1, 50)
        val total = edgeVersions.countByEdgeId(edgeId)
        val rows = edgeVersions.findByEdgeIdOrderByVersionDesc(edgeId, PageRequest.of(0, n))
        return InstanceVersionStats(total = total, recent = rows.map { it.toEdgeSummary() })
    }

    @Transactional(readOnly = true)
    fun getEdgeVersion(edgeId: UUID, version: Long): Edge {
        val row = edgeVersions.findByEdgeIdAndVersion(edgeId, version)
            ?: throw GraphException(
                code = "EDGE_VERSION_NOT_FOUND",
                message = "Edge version not found: $edgeId@$version",
            )
        return Edge(
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

    companion object {
        fun nextVersion(previous: Long?): Long {
            val millis = Instant.now().toEpochMilli()
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
