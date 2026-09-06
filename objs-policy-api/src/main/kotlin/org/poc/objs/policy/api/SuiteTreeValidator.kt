package org.poc.objs.policy.api

import java.util.UUID

/**
 * Suite folder DAG: single parent, exactly one root, no cycles, unique keys, parent refs exist.
 * Shared by in-memory (`:objs-policy-core`) and JPA (`:objs-persistence`) [SuiteRepository] impls.
 */
object SuiteTreeValidator {
    fun validate(folders: List<SuiteFolder>) {
        if (folders.isEmpty()) return
        val byId = folders.associateBy { it.id }
        if (byId.size != folders.size) {
            throw InvalidSuiteException("Duplicate folder ids in suite")
        }
        val keys = folders.map { it.key }
        if (keys.toSet().size != keys.size) {
            throw InvalidSuiteException("Duplicate folder keys in suite")
        }
        val roots = folders.filter { it.parentId == null }
        if (roots.size != 1) {
            throw InvalidSuiteException("Suite must have exactly one root folder (found ${roots.size})")
        }
        for (f in folders) {
            val p = f.parentId ?: continue
            if (!byId.containsKey(p)) {
                throw InvalidSuiteException("Folder '${f.key}' parentId $p not found")
            }
        }
        // Cycle detection via parent walk
        for (f in folders) {
            val seen = linkedSetOf<UUID>()
            var cur: SuiteFolder? = f
            while (cur != null) {
                if (!seen.add(cur.id)) {
                    throw InvalidSuiteException("Cycle detected involving folder '${f.key}'")
                }
                cur = cur.parentId?.let { byId[it] }
            }
        }
        // Orphans: every non-root must reach the single root
        val rootId = roots.single().id
        for (f in folders) {
            if (f.id == rootId) continue
            var cur: SuiteFolder? = f
            var hops = 0
            while (cur != null && cur.id != rootId) {
                hops++
                if (hops > folders.size) {
                    throw InvalidSuiteException("Folder '${f.key}' does not reach root")
                }
                cur = cur.parentId?.let { byId[it] }
            }
            if (cur?.id != rootId) {
                throw InvalidSuiteException("Folder '${f.key}' does not reach root")
            }
        }
    }
}
