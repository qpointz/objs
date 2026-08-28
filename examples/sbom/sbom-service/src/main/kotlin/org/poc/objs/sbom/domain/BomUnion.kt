package org.poc.objs.sbom.domain

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.core.domain.ResolvedGraph
import java.util.UUID

/**
 * Ephemeral Combined SBOM / multi-select union (G-A3). Never persisted on the version.
 */
object BomUnion {
    fun of(graphs: List<ResolvedGraph>): GraphContents {
        val entityIds = linkedSetOf<UUID>()
        val entities = ArrayList<org.poc.objs.api.domain.Entity>()
        for (graph in graphs) {
            for (entity in graph.contents.entities) {
                val id = entity.id ?: continue
                if (entityIds.add(id)) {
                    entities += entity
                }
            }
        }
        val seenEdges = linkedSetOf<String>()
        val edges = ArrayList<Edge>()
        for (graph in graphs) {
            for (edge in graph.contents.edges) {
                val key = "${edge.source}>${edge.role}>${edge.target}"
                if (seenEdges.add(key)) {
                    edges += edge
                }
            }
        }
        return GraphContents(entities = entities, edges = edges)
    }

    fun combinedTags(
        appTags: Array<String>,
        versionTags: Array<String>,
        bomTags: List<Array<String>>,
    ): List<String> {
        val out = linkedSetOf<String>()
        out += appTags
        out += versionTags
        for (tags in bomTags) {
            out += tags
        }
        return out.toList()
    }

    fun sanitizeTags(tags: Collection<String>?): Array<String> {
        val out = linkedSetOf<String>()
        for (raw in tags.orEmpty()) {
            val tag = raw.trim()
            if (tag.isNotEmpty()) out += tag
        }
        return out.toTypedArray()
    }
}
