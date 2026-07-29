package org.poc.objs.core.seed

import org.poc.objs.core.persistence.BoMSeedLedgerRecord
import org.poc.objs.core.persistence.BoMSeedLedgerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

enum class SeedLedgerStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
}

@Service
class BoMSeedLedger(
    private val repository: BoMSeedLedgerRepository,
) {
    fun find(seedKey: String): BoMSeedLedgerRecord? = repository.findById(seedKey).orElse(null)

    fun shouldSkip(seedKey: String, fingerprint: String): Boolean {
        val record = find(seedKey) ?: return false
        return record.lastSuccessFingerprint == fingerprint
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordSuccess(seedKey: String, fingerprint: String) {
        val now = Instant.now()
        val record = repository.findById(seedKey).orElseGet {
            BoMSeedLedgerRecord(seedKey = seedKey, createdAt = now)
        }
        record.lastSuccessFingerprint = fingerprint
        record.lastSuccessAt = now
        record.lastAttemptFingerprint = fingerprint
        record.lastAttemptStatus = SeedLedgerStatus.SUCCESS.name
        record.lastAttemptAt = now
        record.lastError = null
        record.updatedAt = now
        repository.save(record)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordFailure(seedKey: String, fingerprint: String, error: String) {
        val now = Instant.now()
        val record = repository.findById(seedKey).orElseGet {
            BoMSeedLedgerRecord(seedKey = seedKey, createdAt = now)
        }
        // Preserve lastSuccessFingerprint / lastSuccessAt
        record.lastAttemptFingerprint = fingerprint
        record.lastAttemptStatus = SeedLedgerStatus.FAILED.name
        record.lastAttemptAt = now
        record.lastError = error.take(8000)
        record.updatedAt = now
        repository.save(record)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordSkipped(seedKey: String, fingerprint: String) {
        val now = Instant.now()
        val record = repository.findById(seedKey).orElseGet {
            BoMSeedLedgerRecord(seedKey = seedKey, createdAt = now)
        }
        record.lastAttemptFingerprint = fingerprint
        record.lastAttemptStatus = SeedLedgerStatus.SKIPPED.name
        record.lastAttemptAt = now
        record.lastError = null
        record.updatedAt = now
        repository.save(record)
    }
}
