package org.poc.objs.core.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

/**
 * JPA row for [org.poc.objs.core.domain.BoEntity] — generic columns + JSON payload/annotations.
 */
@Entity
@Table(name = "bo_entity")
class BoEntityRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "type", nullable = false)
    var type: String = "",

    @Column(name = "version", nullable = false)
    var version: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "json")
    var payload: MutableMap<String, Any?> = mutableMapOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", nullable = false, columnDefinition = "json")
    var annotations: MutableMap<String, String> = mutableMapOf(),
)

/**
 * JPA row for [org.poc.objs.core.domain.BoEdge].
 */
@Entity
@Table(name = "bo_edge")
class BoEdgeRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "source_id", nullable = false)
    var sourceId: UUID = UUID.randomUUID(),

    @Column(name = "target_id", nullable = false)
    var targetId: UUID = UUID.randomUUID(),

    @Column(name = "role", nullable = false)
    var role: String = "",

    @Column(name = "type")
    var type: String? = null,

    @Column(name = "version")
    var version: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "json")
    var properties: MutableMap<String, Any?>? = null,
)
