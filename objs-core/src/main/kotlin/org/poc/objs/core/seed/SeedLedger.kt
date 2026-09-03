package org.poc.objs.core.seed

import org.poc.objs.core.persistence.SeedLedgerDao
import org.poc.objs.core.persistence.SeedLedgerRecord
import org.poc.objs.core.persistence.tx.UnitOfWork
import java.time.Instant

enum class SeedLedgerStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
}

class SeedLedger(
    private val dao: SeedLedgerDao,
    private val uow: UnitOfWork,
) {
    fun find(seedKey: String): SeedLedgerRecord? = uow.read { dao.findById(seedKey) }

    fun shouldSkip(seedKey: String, fingerprint: String): Boolean {
        val record = find(seedKey) ?: return false
        return record.lastSuccessFingerprint == fingerprint
    }

    fun recordSuccess(seedKey: String, fingerprint: String) {
        uow.writeNew {
            val now = Instant.now()
            val record = dao.findById(seedKey) ?: SeedLedgerRecord(seedKey = seedKey, createdAt = now)
            record.lastSuccessFingerprint = fingerprint
            record.lastSuccessAt = now
            record.lastAttemptFingerprint = fingerprint
            record.lastAttemptStatus = SeedLedgerStatus.SUCCESS.name
            record.lastAttemptAt = now
            record.lastError = null
            record.updatedAt = now
            dao.save(record)
        }
    }

    fun recordFailure(seedKey: String, fingerprint: String, error: String) {
        uow.writeNew {
            val now = Instant.now()
            val record = dao.findById(seedKey) ?: SeedLedgerRecord(seedKey = seedKey, createdAt = now)
            // Preserve lastSuccessFingerprint / lastSuccessAt
            record.lastAttemptFingerprint = fingerprint
            record.lastAttemptStatus = SeedLedgerStatus.FAILED.name
            record.lastAttemptAt = now
            record.lastError = error.take(8000)
            record.updatedAt = now
            dao.save(record)
        }
    }

    fun recordSkipped(seedKey: String, fingerprint: String) {
        uow.writeNew {
            val now = Instant.now()
            val record = dao.findById(seedKey) ?: SeedLedgerRecord(seedKey = seedKey, createdAt = now)
            record.lastAttemptFingerprint = fingerprint
            record.lastAttemptStatus = SeedLedgerStatus.SKIPPED.name
            record.lastAttemptAt = now
            record.lastError = null
            record.updatedAt = now
            dao.save(record)
        }
    }
}
