package org.poc.objs.api.typed.upgrade

import org.poc.objs.api.domain.SchemaVersion

/**
 * In-memory registry. Builds a single path; throws [AmbiguousUpgradePathException] if more than one.
 */
class InMemorySchemaUpgradeRegistry(
    steps: Collection<SchemaUpgradeStep>,
) : SchemaUpgradeRegistry {
    private val byType: Map<String, List<SchemaUpgradeStep>> =
        steps.groupBy { it.type() }

    override fun findChain(type: String, fromVersion: String, toVersion: String): List<SchemaUpgradeStep> {
        if (fromVersion == toVersion) return emptyList()
        val edges = byType[type].orEmpty()
        if (edges.isEmpty()) return emptyList()

        data class Node(val version: String, val path: List<SchemaUpgradeStep>)

        val queue = ArrayDeque<Node>()
        queue.add(Node(fromVersion, emptyList()))
        val visited = mutableSetOf(fromVersion)
        val found = mutableListOf<List<SchemaUpgradeStep>>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (step in edges.filter { it.fromVersion() == current.version }) {
                val next = step.toVersion()
                val nextPath = current.path + step
                if (next == toVersion) {
                    found.add(nextPath)
                    continue
                }
                if (next !in visited) {
                    visited.add(next)
                    queue.add(Node(next, nextPath))
                }
            }
        }

        return when (found.size) {
            0 -> emptyList()
            1 -> found[0]
            else -> throw AmbiguousUpgradePathException(type, fromVersion, toVersion, found.size)
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg steps: SchemaUpgradeStep): InMemorySchemaUpgradeRegistry =
            InMemorySchemaUpgradeRegistry(steps.toList())

        @JvmStatic
        fun of(steps: Collection<SchemaUpgradeStep>): InMemorySchemaUpgradeRegistry =
            InMemorySchemaUpgradeRegistry(steps)
    }
}

class AmbiguousUpgradePathException(
    type: String,
    fromVersion: String,
    toVersion: String,
    pathCount: Int,
) : IllegalStateException(
    "Ambiguous upgrade path for $type $fromVersion → $toVersion ($pathCount paths)",
)

/** Optional helper: sort steps by fromVersion using [SchemaVersion.compare]. */
fun SchemaUpgradeStep.compareFromVersionTo(other: SchemaUpgradeStep): Int =
    SchemaVersion.compare(fromVersion(), other.fromVersion())
