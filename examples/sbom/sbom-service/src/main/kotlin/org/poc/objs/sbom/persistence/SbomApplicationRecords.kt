package org.poc.objs.sbom.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
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
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    var tags: Array<String> = emptyArray(),
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
    @Column(nullable = false, length = 16)
    var status: String = ApplicationVersionStatus.DRAFT,
    @Column(nullable = false, length = 255)
    var version: String = "",
    @Column(name = "version_serial", nullable = false, precision = 40, scale = 16)
    var versionSerial: BigDecimal = BigDecimal("-1"),
    @Column(name = "promoted_at")
    var promotedAt: Instant? = null,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    var tags: Array<String> = emptyArray(),
    @Column(name = "based_on_version_id")
    var basedOnVersionId: UUID? = null,
    @Column(name = "based_on_fingerprint_id")
    var basedOnFingerprintId: UUID? = null,
)

object ApplicationVersionStatus {
    const val DRAFT = "DRAFT"
    const val RELEASED = "RELEASED"
}

@Entity
@Table(name = "sbom_application_sbom")
class SbomApplicationSbomRecord(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "version_id", nullable = false)
    var versionId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 255)
    var name: String = "BOM",
    @Column(length = 2048)
    var description: String? = null,
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    var tags: Array<String> = emptyArray(),
    @Column(name = "graph_id", nullable = false, unique = true)
    var graphId: UUID = UUID.randomUUID(),
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
)

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
    @Column(nullable = false, length = 255)
    var name: String = "",
    @Column(nullable = false, length = 32)
    var category: String = FingerprintCategory.UNKNOWN,
    @Column(name = "content_sha256", nullable = false, length = 64)
    var contentSha256: String = "",
)

object FingerprintCategory {
    const val APPROVAL = "approval"
    const val HISTORY = "history"
    const val UNKNOWN = "unknown"
    val ALL = setOf(APPROVAL, HISTORY, UNKNOWN)
}
