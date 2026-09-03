package org.poc.objs.sbom.domain

import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.ResolvedGraphFragment
import org.poc.objs.api.domain.ResolvedGraph

/**
 * Ephemeral Combined SBOM / multi-select union (G-A3). Never persisted on the version.
 */
object BomUnion {
    fun of(graphs: List<ResolvedGraph>): ResolvedGraphFragment =
        DefaultGraphFragmentPolicy.resolve(
            GraphContents(
                entities = graphs.flatMap { it.contents.entities },
                edges = graphs.flatMap { it.contents.edges },
            ),
        )

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
