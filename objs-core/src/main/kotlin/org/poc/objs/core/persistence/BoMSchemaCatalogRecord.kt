package org.poc.objs.core.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.time.Instant

/** Composite key for [BoMSchemaCatalogRecord]. */
data class BoMSchemaCatalogId(
    val type: String = "",
    val version: String = "",
) : Serializable

@Entity
@Table(name = "bom_schema_catalog")
@IdClass(BoMSchemaCatalogId::class)
class BoMSchemaCatalogRecord(
    @Id
    @Column(name = "type", nullable = false)
    var type: String = "",

    @Id
    @Column(name = "version", nullable = false, length = 64)
    var version: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_doc", nullable = false, columnDefinition = "json")
    var schemaDoc: MutableMap<String, Any?> = mutableMapOf(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
