package org.poc.objs.sbom.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SbomApplicationRepository : JpaRepository<SbomApplicationRecord, UUID> {
    fun findByNameIgnoreCase(name: String): SbomApplicationRecord?

    @org.springframework.data.jpa.repository.Query(
        """
        SELECT a FROM SbomApplicationRecord a
        WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY a.name ASC
        """,
    )
    fun search(@org.springframework.data.repository.query.Param("q") q: String): List<SbomApplicationRecord>
}

interface SbomApplicationVersionRepository : JpaRepository<SbomApplicationVersionRecord, UUID> {
    fun findByApplicationIdOrderByCapturedAtDescIdDesc(
        applicationId: UUID,
    ): List<SbomApplicationVersionRecord>

    fun findByIdAndApplicationId(id: UUID, applicationId: UUID): SbomApplicationVersionRecord?

    fun findByApplicationIdAndStatus(applicationId: UUID, status: String): List<SbomApplicationVersionRecord>
}

interface SbomApplicationFingerprintRepository : JpaRepository<SbomApplicationFingerprintRecord, UUID> {
    fun findByVersionIdOrderByCreatedAtDesc(versionId: UUID): List<SbomApplicationFingerprintRecord>

    fun findByIdAndVersionId(id: UUID, versionId: UUID): SbomApplicationFingerprintRecord?
}

interface SbomApplicationSbomRepository : JpaRepository<SbomApplicationSbomRecord, UUID> {
    fun findByVersionIdOrderBySortOrderAscIdAsc(versionId: UUID): List<SbomApplicationSbomRecord>

    fun findByIdAndVersionId(id: UUID, versionId: UUID): SbomApplicationSbomRecord?

    fun countByVersionId(versionId: UUID): Long
}
