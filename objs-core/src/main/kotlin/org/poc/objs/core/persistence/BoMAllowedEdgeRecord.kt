package org.poc.objs.core.persistence

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.poc.objs.core.domain.BoMEdgeCardinality
import org.poc.objs.core.domain.BoMPropertiesPolicy
import java.io.Serializable
import java.time.Instant

/** Composite key for [BoMAllowedEdgeRuleRecord]. */
data class BoMAllowedEdgeRuleId(
    val sourceType: String = "",
    val role: String = "",
    val targetType: String = "",
) : Serializable

@Entity
@Table(name = "bom_graph_edge_schema")
@IdClass(BoMAllowedEdgeRuleId::class)
class BoMAllowedEdgeRuleRecord(
    @Id
    @Column(name = "source_type", nullable = false)
    var sourceType: String = "",

    @Id
    @Column(name = "role", nullable = false)
    var role: String = "",

    @Id
    @Column(name = "target_type", nullable = false)
    var targetType: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "properties_policy", nullable = false, length = 32)
    var propertiesPolicy: BoMPropertiesPolicy = BoMPropertiesPolicy.NONE,

    @Column(name = "empty_properties_allowed", nullable = false)
    var emptyPropertiesAllowed: Boolean = true,

    @Column(name = "properties_schema_type")
    var propertiesSchemaType: String? = null,

    @Column(name = "properties_schema_version", length = 64)
    var propertiesSchemaVersion: String? = null,

    @Convert(converter = BoMEdgeCardinalityConverter::class)
    @Column(name = "cardinality", nullable = false, length = 32)
    var cardinality: BoMEdgeCardinality = BoMEdgeCardinality.UNSPECIFIED,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
