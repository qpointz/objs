package org.poc.objs.policy.core

import org.poc.objs.policy.api.InvalidSuiteException
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.PolicySuiteWrite
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderRollUpModes
import org.poc.objs.policy.api.SuiteFolderWrite
import org.poc.objs.policy.api.SuiteRepository
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemorySuiteRepository : SuiteRepository {
    private val byId = ConcurrentHashMap<UUID, PolicySuite>()

    override fun save(write: PolicySuiteWrite): PolicySuite {
        val suite = materialize(UUID.randomUUID(), write)
        byId[suite.id] = suite
        return suite
    }

    override fun update(id: UUID, write: PolicySuiteWrite): PolicySuite? {
        if (!byId.containsKey(id)) return null
        val suite = materialize(id, write)
        byId[id] = suite
        return suite
    }

    override fun delete(id: UUID): Boolean = byId.remove(id) != null

    override fun findById(id: UUID): PolicySuite? = byId[id]

    override fun list(): List<PolicySuite> = byId.values.sortedBy { it.name.lowercase() }

    private fun materialize(id: UUID, write: PolicySuiteWrite): PolicySuite {
        val folders = materializeFolders(write.folders)
        SuiteTreeValidator.validate(folders)
        val rollUpKind = write.rollUpStrategyKind.trim().ifBlank {
            throw InvalidSuiteException("rollUpStrategyKind is required")
        }
        if (rollUpKind != SuiteRollUpStrategyKinds.BUILTIN) {
            throw InvalidSuiteException(
                "Unknown rollUpStrategyKind '$rollUpKind' (C-27 supports BUILTIN only)",
            )
        }
        return PolicySuite(
            id = id,
            name = write.name.trim().ifBlank { throw InvalidSuiteException("Suite name is required") },
            rollUpStrategyKind = rollUpKind,
            executionStrategyKind = write.executionStrategyKind.trim().ifBlank {
                throw InvalidSuiteException("executionStrategyKind is required")
            },
            folders = folders,
            tags = write.tags,
            annotations = write.annotations,
        )
    }

    private fun materializeFolders(writes: List<SuiteFolderWrite>): List<SuiteFolder> =
        writes.map { w ->
            val mode = w.rollUpMode.trim().ifBlank { SuiteFolderRollUpModes.ALL_PASS }
            if (mode != SuiteFolderRollUpModes.ALL_PASS && mode != SuiteFolderRollUpModes.ANY_PASS) {
                throw InvalidSuiteException(
                    "Unknown folder rollUpMode '$mode' (use ALL_PASS or ANY_PASS)",
                )
            }
            SuiteFolder(
                id = w.id ?: UUID.randomUUID(),
                key = w.key.trim().ifBlank { throw InvalidSuiteException("Folder key is required") },
                name = w.name.trim().ifBlank { throw InvalidSuiteException("Folder name is required") },
                parentId = w.parentId,
                sortOrder = w.sortOrder,
                participation = w.participation,
                rollUpMode = mode,
                matchers = w.matchers,
                tags = w.tags,
                annotations = w.annotations,
                severityConfig = w.severityConfig,
            )
        }
}
