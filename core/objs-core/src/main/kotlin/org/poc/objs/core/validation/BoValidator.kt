package org.poc.objs.core.validation

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.poc.objs.core.domain.BoAllowedEdgeCatalog
import org.poc.objs.core.domain.BoAllowedEdgeRule
import org.poc.objs.core.domain.BoEdge
import org.poc.objs.core.domain.BoEntity
import org.poc.objs.core.domain.BoGraph
import org.poc.objs.core.domain.BoPropertiesPolicy
import org.poc.objs.core.domain.BoSchemaCatalog
import java.util.UUID

/**
 * Resolves entity types for edge endpoints from the write payload and/or an existing store.
 */
fun interface BoEntityTypeLookup {
    /**
     * @return entity type string, or null if the id is unknown
     */
    fun typeOf(id: UUID): String?
}

/**
 * JSON Schema + allow-list validation (audit and persist stages).
 */
class BoValidator(
    private val schemas: BoSchemaCatalog,
    private val allowedEdges: BoAllowedEdgeCatalog,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    private val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)

    /** Stage 1 — validate entity payloads only. */
    fun validateEntities(entities: Collection<BoEntity>): BoValidationResult {
        val issues = mutableListOf<BoValidationIssue>()
        entities.forEachIndexed { index, entity ->
            issues += validateEntity(entity, path = "entities[$index]")
        }
        return BoValidationResult(issues)
    }

    fun validateEntity(entity: BoEntity, path: String = "entity"): List<BoValidationIssue> {
        val issues = mutableListOf<BoValidationIssue>()
        val schema = schemas.get(entity.type, entity.version)
        if (schema == null) {
            issues += BoValidationIssue(
                code = "SCHEMA_NOT_FOUND",
                message = "No schema for type=${entity.type} version=${entity.version}",
                path = path,
            )
            return issues
        }
        issues += validateAgainstSchema(schema.schema, entity.payload, path = "$path.payload")
        return issues
    }

    /**
     * Stage 2 — validate edges using [typeLookup] for source/target types
     * (payload entities ∪ persisted store).
     */
    fun validateEdges(
        edges: Collection<BoEdge>,
        typeLookup: BoEntityTypeLookup,
    ): BoValidationResult {
        val issues = mutableListOf<BoValidationIssue>()
        edges.forEachIndexed { index, edge ->
            issues += validateEdge(edge, typeLookup, path = "edges[$index]")
        }
        return BoValidationResult(issues)
    }

    fun validateEdge(
        edge: BoEdge,
        typeLookup: BoEntityTypeLookup,
        path: String = "edge",
    ): List<BoValidationIssue> {
        val issues = mutableListOf<BoValidationIssue>()
        val sourceType = typeLookup.typeOf(edge.source)
        val targetType = typeLookup.typeOf(edge.target)
        if (sourceType == null) {
            issues += BoValidationIssue(
                code = "SOURCE_NOT_FOUND",
                message = "Edge source ${edge.source} not in payload or store",
                path = "$path.source",
            )
        }
        if (targetType == null) {
            issues += BoValidationIssue(
                code = "TARGET_NOT_FOUND",
                message = "Edge target ${edge.target} not in payload or store",
                path = "$path.target",
            )
        }
        if (sourceType == null || targetType == null) {
            return issues
        }
        val rule = allowedEdges.find(sourceType, edge.role, targetType)
        if (rule == null) {
            issues += BoValidationIssue(
                code = "EDGE_NOT_ALLOWED",
                message = "No allow-list rule for ($sourceType, ${edge.role}, $targetType)",
                path = path,
            )
            return issues
        }
        issues += validateEdgeProperties(edge, rule, path)
        return issues
    }

    private fun validateEdgeProperties(
        edge: BoEdge,
        rule: BoAllowedEdgeRule,
        path: String,
    ): List<BoValidationIssue> {
        val issues = mutableListOf<BoValidationIssue>()
        val props = edge.properties
        when (rule.propertiesPolicy) {
            BoPropertiesPolicy.NONE -> {
                if (!props.isNullOrEmpty()) {
                    issues += BoValidationIssue(
                        code = "PROPERTIES_NOT_ALLOWED",
                        message = "Role ${edge.role} forbids properties (bare edge)",
                        path = "$path.properties",
                    )
                }
            }
            BoPropertiesPolicy.SCHEMA -> {
                val type = edge.type
                val version = edge.version
                if (type.isNullOrBlank() || version.isNullOrBlank()) {
                    issues += BoValidationIssue(
                        code = "EDGE_SCHEMA_REF_MISSING",
                        message = "Edge type+version required for schema properties policy",
                        path = path,
                    )
                    return issues
                }
                if (props == null) {
                    if (!rule.emptyPropertiesAllowed) {
                        issues += BoValidationIssue(
                            code = "PROPERTIES_REQUIRED",
                            message = "Empty properties not allowed for this edge rule",
                            path = "$path.properties",
                        )
                    }
                    return issues
                }
                if (props.isEmpty() && !rule.emptyPropertiesAllowed) {
                    issues += BoValidationIssue(
                        code = "PROPERTIES_REQUIRED",
                        message = "Empty properties not allowed for this edge rule",
                        path = "$path.properties",
                    )
                    return issues
                }
                val schema = schemas.get(type, version)
                if (schema == null) {
                    issues += BoValidationIssue(
                        code = "SCHEMA_NOT_FOUND",
                        message = "No schema for edge type=$type version=$version",
                        path = path,
                    )
                } else {
                    issues += validateAgainstSchema(schema.schema, props, path = "$path.properties")
                }
            }
        }
        return issues
    }

    /** Full audit of an in-memory graph (entities then edges) using only payload entities for types. */
    fun audit(graph: BoGraph): BoValidationResult {
        val entityIssues = validateEntities(graph.entities).issues
        val lookup = payloadLookup(graph.entities)
        val edgeIssues = validateEdges(graph.edges, lookup).issues
        return BoValidationResult(entityIssues + edgeIssues)
    }

    fun payloadLookup(entities: Collection<BoEntity>): BoEntityTypeLookup {
        val map = entities.mapNotNull { e -> e.id?.let { it to e.type } }.toMap()
        return BoEntityTypeLookup { id -> map[id] }
    }

    fun combinedLookup(
        payloadEntities: Collection<BoEntity>,
        storeLookup: BoEntityTypeLookup,
    ): BoEntityTypeLookup {
        val payload = payloadLookup(payloadEntities)
        return BoEntityTypeLookup { id -> payload.typeOf(id) ?: storeLookup.typeOf(id) }
    }

    private fun validateAgainstSchema(
        schemaDoc: Map<String, Any?>,
        data: Map<String, Any?>,
        path: String,
    ): List<BoValidationIssue> {
        return try {
            val schemaNode = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(schemaDoc)
            val dataNode = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(data)
            val schema = schemaFactory.getSchema(schemaNode)
            val errors = schema.validate(dataNode)
            errors.map {
                BoValidationIssue(
                    code = "SCHEMA_VIOLATION",
                    message = it.message,
                    path = path,
                )
            }
        } catch (ex: Exception) {
            listOf(
                BoValidationIssue(
                    code = "SCHEMA_ERROR",
                    message = ex.message ?: "schema validation failed",
                    path = path,
                ),
            )
        }
    }
}
