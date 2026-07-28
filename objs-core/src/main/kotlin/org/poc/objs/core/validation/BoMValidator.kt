package org.poc.objs.core.validation

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchemaCatalog
import java.util.UUID

/**
 * Resolves entity types for edge endpoints from the write payload and/or an existing store.
 */
fun interface BoMEntityTypeLookup {
    /**
     * @return entity type string, or null if the id is unknown
     */
    fun typeOf(id: UUID): String?
}

/**
 * JSON Schema + allow-list validation (audit and persist stages).
 */
class BoMValidator(
    private val schemas: BoMSchemaCatalog,
    private val allowedEdges: BoMAllowedEdgeCatalog,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    private val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)

    /** Stage 1 — validate entity payloads only. */
    fun validateEntities(entities: Collection<BoMEntity>): BoMValidationResult {
        val issues = mutableListOf<BoMValidationIssue>()
        entities.forEachIndexed { index, entity ->
            issues += validateEntity(entity, path = "entities[$index]")
        }
        return BoMValidationResult(issues)
    }

    fun validateEntity(entity: BoMEntity, path: String = "entity"): List<BoMValidationIssue> {
        val issues = mutableListOf<BoMValidationIssue>()
        val schema = schemas.get(entity.type, entity.schemaVersion)
        if (schema == null) {
            issues += BoMValidationIssue(
                code = "SCHEMA_NOT_FOUND",
                message = "No schema for type=${entity.type} schemaVersion=${entity.schemaVersion}",
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
        edges: Collection<BoMEdge>,
        typeLookup: BoMEntityTypeLookup,
    ): BoMValidationResult {
        val issues = mutableListOf<BoMValidationIssue>()
        edges.forEachIndexed { index, edge ->
            issues += validateEdge(edge, typeLookup, path = "edges[$index]")
        }
        return BoMValidationResult(issues)
    }

    fun validateEdge(
        edge: BoMEdge,
        typeLookup: BoMEntityTypeLookup,
        path: String = "edge",
    ): List<BoMValidationIssue> {
        val issues = mutableListOf<BoMValidationIssue>()
        val sourceType = typeLookup.typeOf(edge.source)
        val targetType = typeLookup.typeOf(edge.target)
        if (sourceType == null) {
            issues += BoMValidationIssue(
                code = "SOURCE_NOT_FOUND",
                message = "Edge source ${edge.source} not in payload or store",
                path = "$path.source",
            )
        }
        if (targetType == null) {
            issues += BoMValidationIssue(
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
            issues += BoMValidationIssue(
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
        edge: BoMEdge,
        rule: BoMAllowedEdgeRule,
        path: String,
    ): List<BoMValidationIssue> {
        val issues = mutableListOf<BoMValidationIssue>()
        val props = edge.properties
        when (rule.propertiesPolicy) {
            BoMPropertiesPolicy.NONE -> {
                if (!props.isNullOrEmpty()) {
                    issues += BoMValidationIssue(
                        code = "PROPERTIES_NOT_ALLOWED",
                        message = "Role ${edge.role} forbids properties (bare edge)",
                        path = "$path.properties",
                    )
                }
            }
            BoMPropertiesPolicy.SCHEMA -> {
                val type = edge.type
                val schemaVersion = edge.schemaVersion
                if (type.isNullOrBlank() || schemaVersion.isNullOrBlank()) {
                    issues += BoMValidationIssue(
                        code = "EDGE_SCHEMA_REF_MISSING",
                        message = "Edge type+schemaVersion required for schema properties policy",
                        path = path,
                    )
                    return issues
                }
                if (props == null) {
                    if (!rule.emptyPropertiesAllowed) {
                        issues += BoMValidationIssue(
                            code = "PROPERTIES_REQUIRED",
                            message = "Empty properties not allowed for this edge rule",
                            path = "$path.properties",
                        )
                    }
                    return issues
                }
                if (props.isEmpty() && !rule.emptyPropertiesAllowed) {
                    issues += BoMValidationIssue(
                        code = "PROPERTIES_REQUIRED",
                        message = "Empty properties not allowed for this edge rule",
                        path = "$path.properties",
                    )
                    return issues
                }
                val schema = schemas.get(type, schemaVersion)
                if (schema == null) {
                    issues += BoMValidationIssue(
                        code = "SCHEMA_NOT_FOUND",
                        message = "No schema for edge type=$type schemaVersion=$schemaVersion",
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
    fun audit(graph: BoMGraph): BoMValidationResult {
        val entityIssues = validateEntities(graph.entities).issues
        val lookup = payloadLookup(graph.entities)
        val edgeIssues = validateEdges(graph.edges, lookup).issues
        return BoMValidationResult(entityIssues + edgeIssues)
    }

    fun payloadLookup(entities: Collection<BoMEntity>): BoMEntityTypeLookup {
        val map = entities.mapNotNull { e -> e.id?.let { it to e.type } }.toMap()
        return BoMEntityTypeLookup { id -> map[id] }
    }

    fun combinedLookup(
        payloadEntities: Collection<BoMEntity>,
        storeLookup: BoMEntityTypeLookup,
    ): BoMEntityTypeLookup {
        val payload = payloadLookup(payloadEntities)
        return BoMEntityTypeLookup { id -> payload.typeOf(id) ?: storeLookup.typeOf(id) }
    }

    private fun validateAgainstSchema(
        schemaDoc: Map<String, Any?>,
        data: Map<String, Any?>,
        path: String,
    ): List<BoMValidationIssue> {
        return try {
            val schemaNode = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(schemaDoc)
            val dataNode = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(data)
            val schema = schemaFactory.getSchema(schemaNode)
            val errors = schema.validate(dataNode)
            errors.map {
                BoMValidationIssue(
                    code = "SCHEMA_VIOLATION",
                    message = it.message,
                    path = path,
                )
            }
        } catch (ex: Exception) {
            listOf(
                BoMValidationIssue(
                    code = "SCHEMA_ERROR",
                    message = ex.message ?: "schema validation failed",
                    path = path,
                ),
            )
        }
    }
}
