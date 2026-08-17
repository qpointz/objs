package org.poc.objs.sbom.domain

import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMGraphContents
import org.poc.objs.core.domain.BoMResolvedGraph
import java.util.UUID

/**
 * Ephemeral Combined SBOM / multi-select union (G-A3). Never persisted on the version.
 */
object BomUnion {
    fun of(graphs: List<BoMResolvedGraph>): BoMGraphContents {
        val entityIds = linkedSetOf<UUID>()
        val entities = ArrayList<org.poc.objs.core.domain.BoMEntity>()
        for (graph in graphs) {
            for (entity in graph.contents.entities) {
                val id = entity.id ?: continue
                if (entityIds.add(id)) {
                    entities += entity
                }
            }
        }
        val seenEdges = linkedSetOf<String>()
        val edges = ArrayList<BoMEdge>()
        for (graph in graphs) {
            for (edge in graph.contents.edges) {
                val key = "${edge.source}>${edge.role}>${edge.target}"
                if (seenEdges.add(key)) {
                    edges += edge
                }
            }
        }
        return BoMGraphContents(entities = entities, edges = edges)
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
