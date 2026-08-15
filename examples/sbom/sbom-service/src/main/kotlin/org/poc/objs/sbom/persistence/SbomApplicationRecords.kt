package org.poc.objs.sbom.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sbom_application")
class SbomApplicationRecord(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 255)
    var name: String = "",
    @Column(length = 2048)
    var description: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "sbom_application_version")
class SbomApplicationVersionRecord(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "application_id", nullable = false)
    var applicationId: UUID = UUID.randomUUID(),
    @Column(length = 255)
    var label: String? = null,
    @Column(name = "captured_at", nullable = false)
    var capturedAt: Instant = Instant.now(),
    @Column(name = "graph_id", nullable = false, unique = true)
    var graphId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 16)
    var status: String = ApplicationVersionStatus.DRAFT,
    @Column(length = 255)
    var version: String? = null,
    @Column(name = "promoted_at")
    var promotedAt: Instant? = null,
)

object ApplicationVersionStatus {
    const val DRAFT = "DRAFT"
    const val RELEASED = "RELEASED"
}

@Entity
@Table(name = "sbom_application_fingerprint")
class SbomApplicationFingerprintRecord(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "version_id", nullable = false)
    var versionId: UUID = UUID.randomUUID(),
    @Column(name = "graph_id", nullable = false, unique = true)
    var graphId: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(length = 2048)
    var note: String? = null,
    @Column(name = "content_sha256", nullable = false, length = 64)
    var contentSha256: String = "",
)
