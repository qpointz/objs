package org.poc.objs.sbom.service

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.core.domain.GraphSpec
import org.poc.objs.core.domain.ResolvedGraph
import org.poc.objs.api.domain.graphMutation
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.sbom.domain.BomUnion
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class BomGraphSupport(
    private val namedGraphs: NamedGraphStore,
) {
    fun load(graphIds: List<UUID>): List<ResolvedGraph> =
        graphIds.mapNotNull { namedGraphs.get(it) }

    fun union(graphIds: List<UUID>): GraphContents = BomUnion.of(load(graphIds))

    fun copy(sourceGraphId: UUID, annotations: Map<String, String>): UUID =
        namedGraphs.copyGraph(sourceGraphId, annotations).id

    fun materialize(contents: GraphContents, annotations: Map<String, String>): UUID {
        val graph =
            namedGraphs.create(
                GraphSpec(
                    annotations = annotations,
                    entityIds = contents.entities.mapNotNull { it.id }.toSet(),
                ),
            )
        val edgeCopies =
            contents.edges
                .map { edge ->
                    Edge(
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
                    graphMutation { edges { set(edgeCopies) } },
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
