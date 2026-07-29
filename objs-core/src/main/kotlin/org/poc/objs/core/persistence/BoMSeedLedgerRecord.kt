package org.poc.objs.core.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "bom_seed_ledger")
class BoMSeedLedgerRecord(
    @Id
    @Column(name = "seed_key", nullable = false, length = 512)
    var seedKey: String = "",

    @Column(name = "last_success_fingerprint", length = 128)
    var lastSuccessFingerprint: String? = null,

    @Column(name = "last_success_at")
    var lastSuccessAt: Instant? = null,

    @Column(name = "last_attempt_fingerprint", length = 128)
    var lastAttemptFingerprint: String? = null,

    @Column(name = "last_attempt_status", nullable = false, length = 32)
    var lastAttemptStatus: String = "PENDING",

    @Column(name = "last_attempt_at", nullable = false)
    var lastAttemptAt: Instant = Instant.now(),

    @Column(name = "last_error", columnDefinition = "text")
    var lastError: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
