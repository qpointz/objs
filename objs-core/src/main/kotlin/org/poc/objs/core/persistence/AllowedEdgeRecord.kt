package org.poc.objs.core.persistence

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.poc.objs.api.domain.EdgeCardinality
import org.poc.objs.api.domain.PropertiesPolicy
import java.io.Serializable
import java.time.Instant

/** Composite key for [AllowedEdgeRuleRecord]. */
data class AllowedEdgeRuleId(
    val sourceType: String = "",
    val role: String = "",
    val targetType: String = "",
) : Serializable

@Entity(name = "BoMAllowedEdgeRuleRecord")
@Table(name = "objs_edge_schema")
@IdClass(AllowedEdgeRuleId::class)
class AllowedEdgeRuleRecord(
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
    var propertiesPolicy: PropertiesPolicy = PropertiesPolicy.NONE,

    @Column(name = "empty_properties_allowed", nullable = false)
    var emptyPropertiesAllowed: Boolean = true,

    @Column(name = "properties_schema_type")
    var propertiesSchemaType: String? = null,

    @Column(name = "properties_schema_version", length = 64)
    var propertiesSchemaVersion: String? = null,

    @Convert(converter = EdgeCardinalityConverter::class)
    @Column(name = "cardinality", nullable = false, length = 32)
    var cardinality: EdgeCardinality = EdgeCardinality.UNSPECIFIED,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "source_verb", length = 255)
    var sourceVerb: String? = null,

    @Column(name = "target_verb", length = 255)
    var targetVerb: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags")
    var tags: MutableList<String> = mutableListOf(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    var attributes: MutableMap<String, String> = mutableMapOf(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
