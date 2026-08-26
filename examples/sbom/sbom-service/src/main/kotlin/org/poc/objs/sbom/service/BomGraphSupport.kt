package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.domain.BoMGraphSpec
import org.poc.objs.core.domain.BoMResolvedGraph
import org.poc.objs.core.domain.bomMutation
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.sbom.domain.BomUnion
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class BomGraphSupport(
    private val namedGraphs: BoMNamedGraphStore,
) {
    fun load(graphIds: List<UUID>): List<BoMResolvedGraph> =
        graphIds.mapNotNull { namedGraphs.get(it) }

    fun union(graphIds: List<UUID>): BoMGraphContents = BomUnion.of(load(graphIds))

    fun copy(sourceGraphId: UUID, annotations: Map<String, String>): UUID =
        namedGraphs.copyGraph(sourceGraphId, annotations).id

    fun materialize(contents: BoMGraphContents, annotations: Map<String, String>): UUID {
        val graph =
            namedGraphs.create(
                BoMGraphSpec(
                    annotations = annotations,
                    entityIds = contents.entities.mapNotNull { it.id }.toSet(),
                ),
            )
        val edgeCopies =
            contents.edges
                .map { edge ->
                    BoMEdge(
                        source = edge.source,
                        target = edge.target,
                        role = edge.role,
                        type = edge.type,
                        schemaVersion = edge.schemaVersion,
                        properties = edge.properties?.toMutableMap(),
                    )
                }.toMutableList()
        if (edgeCopies.isNotEmpty()) {
            val result =
                namedGraphs.mutate(
                    graph.id,
                    bomMutation { edges { set(edgeCopies) } },
                )
            if (!result.isValid) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    result.issues.joinToString("; ") { it.message },
                )
            }
        }
        return graph.id
    }
}
