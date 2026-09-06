package org.poc.objs.core.persistence.policy

import org.poc.objs.core.persistence.tx.UnitOfWork
import org.poc.objs.policy.api.InvalidSuiteException
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.PolicySuiteWrite
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteFolderRollUpModes
import org.poc.objs.policy.api.SuiteFolderWrite
import org.poc.objs.policy.api.SuiteRepository
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds
import org.poc.objs.policy.api.SuiteTreeValidator
import java.time.Instant
import java.util.UUID

/**
 * JPA-backed [SuiteRepository] (C-28 WI-002). Folders are replaced wholesale on every
 * [save] / [update] (delete-then-reinsert), mirroring `InMemorySuiteRepository`'s materialize
 * semantics — simpler than diffing the folder tree and safe since folder ids are opaque
 * to callers other than the round-tripped [PolicySuite].
 */
class JpaSuiteRepository(
    private val suiteDao: PolicySuiteDao,
    private val folderDao: PolicySuiteFolderDao,
    private val uow: UnitOfWork,
) : SuiteRepository {

    override fun save(write: PolicySuiteWrite): PolicySuite = uow.write {
        val suite = materialize(UUID.randomUUID(), write)
        requireUniqueKey(suite.id, suite.key)
        persist(suite)
        suite
    }

    override fun update(id: UUID, write: PolicySuiteWrite): PolicySuite? = uow.write {
        if (suiteDao.findById(id) == null) return@write null
        val suite = materialize(id, write)
        requireUniqueKey(suite.id, suite.key)
        persist(suite)
        suite
    }

    override fun delete(id: UUID): Boolean = uow.write {
        if (suiteDao.findById(id) == null) return@write false
        folderDao.deleteBySuiteId(id)
        suiteDao.deleteById(id)
        true
    }

    override fun findById(id: UUID): PolicySuite? = uow.read {
        suiteDao.findById(id)?.let { toDomain(it) }
    }

    override fun findByKey(key: String): PolicySuite? = uow.read {
        suiteDao.findByKey(key)?.let { toDomain(it) }
    }

    override fun list(): List<PolicySuite> = uow.read {
        suiteDao.findAll().sortedBy { it.key.lowercase() }.map { toDomain(it) }
    }

    private fun requireUniqueKey(id: UUID, key: String) {
        val other = suiteDao.findByKey(key)
        if (other != null && other.id != id) {
            throw InvalidSuiteException("Suite key already exists: $key")
        }
    }

    private fun persist(suite: PolicySuite) {
        val now = Instant.now()
        val existing = suiteDao.findById(suite.id)
        val record = existing ?: PolicySuiteRecord(id = suite.id, createdAt = now)
        record.key = suite.key
        record.name = suite.name
        record.rollUpStrategyKind = suite.rollUpStrategyKind
        record.executionStrategyKind = suite.executionStrategyKind
        record.tags = suite.tags.toMutableList()
        record.annotations = suite.annotations.toMutableMap()
        record.updatedAt = now
        suiteDao.save(record)

        folderDao.deleteBySuiteId(suite.id)
        suite.folders.forEach { folder ->
            folderDao.save(
                PolicySuiteFolderRecord(
                    id = folder.id,
                    suiteId = suite.id,
                    key = folder.key,
                    name = folder.name,
                    parentId = folder.parentId,
                    sortOrder = folder.sortOrder,
                    participation = folder.participation.name,
                    rollUpMode = folder.rollUpMode,
                    matchers = folder.matchers.map { it.toRecordMap() }.toMutableList(),
                    tags = folder.tags.toMutableList(),
                    annotations = folder.annotations.toMutableMap(),
                    severityConfig = folder.severityConfig,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    private fun toDomain(record: PolicySuiteRecord): PolicySuite {
        val folders = folderDao.findBySuiteId(record.id).map { f ->
            SuiteFolder(
                id = f.id,
                key = f.key,
                name = f.name,
                parentId = f.parentId,
                sortOrder = f.sortOrder,
                participation = SuiteFolderParticipation.valueOf(f.participation),
                rollUpMode = f.rollUpMode,
                matchers = f.matchers.map { matcherFromRecordMap(it) },
                tags = f.tags.toList(),
                annotations = f.annotations.toMap(),
                severityConfig = f.severityConfig,
            )
        }
        return PolicySuite(
            id = record.id,
            key = record.key,
            name = record.name,
            rollUpStrategyKind = record.rollUpStrategyKind,
            executionStrategyKind = record.executionStrategyKind,
            folders = folders,
            tags = record.tags.toList(),
            annotations = record.annotations.toMap(),
        )
    }

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
        val key = write.key.trim().ifBlank { throw InvalidSuiteException("Suite key is required") }
        return PolicySuite(
            id = id,
            key = key,
            name = write.name.trim().ifBlank { key },
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
