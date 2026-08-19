package org.poc.objs.core.persistence

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.domain.BoMGraphException
import org.poc.objs.core.domain.BoMGraphVersionSummary
import org.poc.objs.core.domain.BoMResolvedGraph
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.math.max

@Service
class BoMDeepGraphVersionService(
    private val graphRepository: BoMGraphRepository,
    private val membershipRepository: BoMGraphMembershipRepository,
    private val entityRepository: BoMEntityRepository,
    private val edgeRepository: BoMEdgeRepository,
    private val entityVersions: BoMEntityVersionRepository,
    private val graphVersions: BoMGraphVersionRepository,
    private val edgeVersions: BoMEdgeVersionRepository,
    private val versionMembers: BoMGraphVersionMemberRepository,
    private val versionEdges: BoMGraphVersionEdgeRepository,
) {
    @Transactional
    fun createDeepGraphVersion(
        graphId: UUID,
        versionAnnotations: Map<String, String> = emptyMap(),
    ): BoMGraphVersionSummary {
        val header = graphRepository.findById(graphId).orElse(null)
            ?: throw BoMGraphException(code = "GRAPH_NOT_FOUND", message = "Graph not found: $graphId")
        val memberIds = membershipRepository.findByGraphId(graphId).map { it.entityId }
        val entityRows = if (memberIds.isEmpty()) emptyList() else entityRepository.findAllById(memberIds)
        val edgeRows = edgeRepository.findByGraphId(graphId)
        val now = Instant.now()
        val graphVersion = nextVersion(header.headVersion)
        graphVersions.save(
            BoMGraphVersionRecord(
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
                BoMGraphVersionMemberRecord(
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
                BoMGraphVersionEdgeRecord(
                    graphId = graphId,
                    graphVersion = graphVersion,
                    edgeId = id,
                    edgeVersion = version,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        return BoMGraphVersionSummary(
            graphId = graphId,
            version = graphVersion,
            createdAt = now,
            annotations = versionAnnotations,
        )
    }

    @Transactional(readOnly = true)
    fun listGraphVersions(graphId: UUID): List<BoMGraphVersionSummary> =
        graphVersions.findByGraphIdOrderByVersionDesc(graphId).map {
            BoMGraphVersionSummary(
                graphId = it.graphId,
                version = it.version,
                createdAt = it.createdAt,
                annotations = it.annotations.toMap(),
            )
        }

    @Transactional(readOnly = true)
    fun getGraphVersion(graphId: UUID, version: Long): BoMResolvedGraph {
        val header = graphVersions.findByGraphIdAndVersion(graphId, version)
            ?: throw BoMGraphException(
                code = "GRAPH_VERSION_NOT_FOUND",
                message = "Graph version not found: $graphId@$version",
            )
        val entities = versionMembers.findByGraphIdAndGraphVersion(graphId, version).map { pin ->
            val row = entityVersions.findByEntityIdAndVersion(pin.entityId, pin.entityVersion)
                ?: throw BoMGraphException(
                    code = "GRAPH_VERSION_NOT_FOUND",
                    message = "Entity version not found: ${pin.entityId}@${pin.entityVersion}",
                )
            BoMEntity(
                id = row.entityId,
                type = row.type,
                schemaVersion = row.schemaVersion,
                payload = row.payload.toMutableMap(),
                annotations = row.annotations.toMutableMap(),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
            )
        }
        val edges = versionEdges.findByGraphIdAndGraphVersion(graphId, version).map { pin ->
            val row = edgeVersions.findByEdgeIdAndVersion(pin.edgeId, pin.edgeVersion)
                ?: throw BoMGraphException(
                    code = "GRAPH_VERSION_NOT_FOUND",
                    message = "Edge version not found: ${pin.edgeId}@${pin.edgeVersion}",
                )
            BoMEdge(
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
            )
        }
        return BoMResolvedGraph(
            id = graphId,
            annotations = header.graphAnnotations.toMap(),
            contents = BoMGraphContents(entities = entities, edges = edges),
            createdAt = header.createdAt,
            updatedAt = header.updatedAt,
        )
    }

    companion object {
        fun nextVersion(previous: Long?): Long {
            val millis = Instant.now().toEpochMilli()
            return max(millis, (previous ?: 0L) + 1L)
        }

        private fun copyEntityVersion(
            row: BoMEntityRecord,
            version: Long,
            now: Instant,
        ) = BoMEntityVersionRecord(
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
            row: BoMEdgeRecord,
            version: Long,
            now: Instant,
        ) = BoMEdgeVersionRecord(
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
