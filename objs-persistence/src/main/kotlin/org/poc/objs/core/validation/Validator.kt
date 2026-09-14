package org.poc.objs.core.validation

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import org.poc.objs.api.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.IdentityProjection
import org.poc.objs.api.domain.PropertiesPolicy
import org.poc.objs.api.domain.SchemaCatalog
import org.poc.objs.api.validation.AllowedEdgeKey
import org.poc.objs.api.validation.EntityTypeLookup
import org.poc.objs.api.validation.PersistValidator
import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.api.validation.ValidationLocus
import org.poc.objs.api.validation.ValidationResult
import org.poc.objs.api.validation.ValidationSchemaRef
import org.poc.objs.api.validation.ValidationSubject
import org.poc.objs.api.validation.ValidationSubjectKind

/**
 * JSON Schema + allow-list validation (audit and persist stages).
 */
class Validator(
    private val schemas: SchemaCatalog,
    private val allowedEdges: AllowedEdgeCatalog,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : PersistValidator {
    private val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)

    /** Stage 1 — validate entity payloads only. */
    override fun validateEntities(entities: Collection<Entity>): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        entities.forEachIndexed { index, entity ->
            issues += validateEntity(entity, index = index, path = "entities[$index]")
        }
        return ValidationResult(issues)
    }

    fun validateEntity(
        entity: Entity,
        index: Int? = null,
        path: String = "entity",
    ): List<ValidationIssue> {
        val subject = entitySubject(entity, index)
        val issues = mutableListOf<ValidationIssue>()
        val schema = schemas.get(entity.type, entity.schemaVersion)
        if (schema == null) {
            issues += ValidationIssue(
                code = "SCHEMA_NOT_FOUND",
                message = "No schema for type=${entity.type} schemaVersion=${entity.schemaVersion}",
                path = path,
                subject = subject,
                schema = ValidationSchemaRef(
                    locus = ValidationLocus.ENTITY_PAYLOAD,
                    schemaType = entity.type,
                    schemaVersion = entity.schemaVersion,
                ),
            )
            return issues
        }
        issues += validateAgainstSchema(
            schemaDoc = schema.toJsonSchema(),
            data = entity.payload,
            path = "$path.payload",
            subject = subject,
            locus = ValidationLocus.ENTITY_PAYLOAD,
            schemaType = entity.type,
            schemaVersion = entity.schemaVersion,
        )
        return issues
    }

    /**
     * Stage 2 — validate edges using [typeLookup] for source/target types
     * (payload entities ∪ persisted store).
     */
    override fun validateEdges(
        edges: Collection<Edge>,
        typeLookup: EntityTypeLookup,
    ): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        edges.forEachIndexed { index, edge ->
            issues += validateEdge(edge, typeLookup, index = index, path = "edges[$index]")
        }
        return ValidationResult(issues)
    }

    fun validateEdge(
        edge: Edge,
        typeLookup: EntityTypeLookup,
        index: Int? = null,
        path: String = "edge",
    ): List<ValidationIssue> {
        val subject = edgeSubject(edge, index)
        val issues = mutableListOf<ValidationIssue>()
        val sourceType = typeLookup.typeOf(edge.source)
        val targetType = typeLookup.typeOf(edge.target)
        if (sourceType == null) {
            issues += ValidationIssue(
                code = "SOURCE_NOT_FOUND",
                message = "Edge source ${edge.source} not in payload or store",
                path = "$path.source",
                subject = subject,
                schema = ValidationSchemaRef(locus = ValidationLocus.OTHER),
            )
        }
        if (targetType == null) {
            issues += ValidationIssue(
                code = "TARGET_NOT_FOUND",
                message = "Edge target ${edge.target} not in payload or store",
                path = "$path.target",
                subject = subject,
                schema = ValidationSchemaRef(locus = ValidationLocus.OTHER),
            )
        }
        if (sourceType == null || targetType == null) {
            return issues
        }
        val rule = allowedEdges.find(sourceType, edge.role, targetType)
        if (rule == null) {
            issues += ValidationIssue(
                code = "EDGE_NOT_ALLOWED",
                message = "No allow-list rule for ($sourceType, ${edge.role}, $targetType)",
                path = path,
                subject = subject,
                schema = ValidationSchemaRef(
                    locus = ValidationLocus.EDGE_ALLOWLIST,
                    allowedEdge = AllowedEdgeKey(sourceType, edge.role, targetType),
                ),
            )
            return issues
        }
        issues += validateEdgeProperties(edge, rule, index, path)
        return issues
    }

    private fun validateEdgeProperties(
        edge: Edge,
        rule: AllowedEdgeRule,
        index: Int?,
        path: String,
    ): List<ValidationIssue> {
        val subject = edgeSubject(edge, index)
        val allowKey = AllowedEdgeKey(
            sourceType = rule.sourceType,
            role = rule.role,
            targetType = rule.targetType,
        )
        val issues = mutableListOf<ValidationIssue>()
        val props = edge.properties
        when (rule.propertiesPolicy) {
            PropertiesPolicy.NONE -> {
                if (!props.isNullOrEmpty()) {
                    issues += ValidationIssue(
                        code = "PROPERTIES_NOT_ALLOWED",
                        message = "Role ${edge.role} forbids properties (bare edge)",
                        path = "$path.properties",
                        subject = subject.copy(document = props),
                        schema = ValidationSchemaRef(
                            locus = ValidationLocus.EDGE_PROPERTIES,
                            allowedEdge = allowKey,
                        ),
                    )
                }
            }
            PropertiesPolicy.SCHEMA -> {
                val type = edge.type
                val schemaVersion = edge.schemaVersion
                if (type.isNullOrBlank() || schemaVersion.isNullOrBlank()) {
                    issues += ValidationIssue(
                        code = "EDGE_SCHEMA_REF_MISSING",
                        message = "Edge type+schemaVersion required for schema properties policy",
                        path = path,
                        subject = subject,
                        schema = ValidationSchemaRef(
                            locus = ValidationLocus.EDGE_PROPERTIES,
                            allowedEdge = allowKey,
                        ),
                    )
                    return issues
                }
                if (
                    rule.propertiesSchemaType != null &&
                    rule.propertiesSchemaVersion != null &&
                    (type != rule.propertiesSchemaType || schemaVersion != rule.propertiesSchemaVersion)
                ) {
                    issues += ValidationIssue(
                        code = "EDGE_SCHEMA_REF_MISMATCH",
                        message = "Edge schema $type@$schemaVersion does not match allowed " +
                            "${rule.propertiesSchemaType}@${rule.propertiesSchemaVersion}",
                        path = path,
                        subject = subject,
                        schema = ValidationSchemaRef(
                            locus = ValidationLocus.EDGE_PROPERTIES,
                            schemaType = type,
                            schemaVersion = schemaVersion,
                            allowedEdge = allowKey,
                        ),
                    )
                    return issues
                }
                if (props == null) {
                    if (!rule.emptyPropertiesAllowed) {
                        issues += ValidationIssue(
                            code = "PROPERTIES_REQUIRED",
                            message = "Empty properties not allowed for this edge rule",
                            path = "$path.properties",
                            subject = subject,
                            schema = ValidationSchemaRef(
                                locus = ValidationLocus.EDGE_PROPERTIES,
                                schemaType = type,
                                schemaVersion = schemaVersion,
                                allowedEdge = allowKey,
                            ),
                        )
                    }
                    return issues
                }
                if (props.isEmpty() && !rule.emptyPropertiesAllowed) {
                    issues += ValidationIssue(
                        code = "PROPERTIES_REQUIRED",
                        message = "Empty properties not allowed for this edge rule",
                        path = "$path.properties",
                        subject = subject.copy(document = props),
                        schema = ValidationSchemaRef(
                            locus = ValidationLocus.EDGE_PROPERTIES,
                            schemaType = type,
                            schemaVersion = schemaVersion,
                            allowedEdge = allowKey,
                        ),
                    )
                    return issues
                }
                val schema = schemas.get(type, schemaVersion)
                if (schema == null) {
                    issues += ValidationIssue(
                        code = "SCHEMA_NOT_FOUND",
                        message = "No schema for edge type=$type schemaVersion=$schemaVersion",
                        path = path,
                        subject = subject.copy(document = props),
                        schema = ValidationSchemaRef(
                            locus = ValidationLocus.EDGE_PROPERTIES,
                            schemaType = type,
                            schemaVersion = schemaVersion,
                            allowedEdge = allowKey,
                        ),
                    )
                } else {
                    issues += validateAgainstSchema(
                        schemaDoc = schema.toJsonSchema(),
                        data = props,
                        path = "$path.properties",
                        subject = subject.copy(document = props),
                        locus = ValidationLocus.EDGE_PROPERTIES,
                        schemaType = type,
                        schemaVersion = schemaVersion,
                    )
                }
            }
        }
        return issues
    }

    /** Full audit of an in-memory graph (entities then edges) using only payload entities for types. */
    fun audit(graph: Graph): ValidationResult {
        val entityIssues = validateEntities(graph.entities).issues
        val lookup = payloadLookup(graph.entities)
        val edgeIssues = validateEdges(graph.edges, lookup).issues
        return ValidationResult(entityIssues + edgeIssues)
    }

    fun payloadLookup(entities: Collection<Entity>): EntityTypeLookup {
        val map = entities.mapNotNull { e -> e.id?.let { it to e.type } }.toMap()
        return EntityTypeLookup { id -> map[id] }
    }

    override fun combinedLookup(
        payloadEntities: Collection<Entity>,
        storeLookup: EntityTypeLookup,
    ): EntityTypeLookup {
        val payload = payloadLookup(payloadEntities)
        return EntityTypeLookup { id -> payload.typeOf(id) ?: storeLookup.typeOf(id) }
    }

    /**
     * Identity immutability (G-2 / G-15): project stored vs incoming with each side's catalog
     * schema. Only paths with a **set** stored identity value (non-null, non-blank string) that
     * remain `identifier` on the **incoming** schema are frozen. Missing / null / blank stored
     * values may be filled; schema migrates that introduce identifiers may set new paths;
     * downgrades (or catalog changes) that drop the `identifier` flag on a path are allowed.
     * Changing or clearing a still-marked identity path fails with `IDENTIFIER_IMMUTABLE`.
     */
    fun validateIdentifierImmutability(
        storedType: String,
        storedSchemaVersion: String,
        storedDocument: Map<String, Any?>,
        incomingType: String,
        incomingSchemaVersion: String,
        incomingDocument: Map<String, Any?>,
        path: String,
        subject: ValidationSubject? = null,
    ): List<ValidationIssue> {
        val storedSchema = schemas.get(storedType, storedSchemaVersion) ?: return emptyList()
        val incomingSchema = schemas.get(incomingType, incomingSchemaVersion) ?: return emptyList()
        val oldMap = IdentityProjection.project(storedSchema.contentSchema, storedDocument)
        if (oldMap.isEmpty()) return emptyList()
        val stillIdentity = IdentityProjection.identifierPaths(incomingSchema.contentSchema)
        if (stillIdentity.isEmpty()) return emptyList()
        val newMap = IdentityProjection.project(incomingSchema.contentSchema, incomingDocument)
        val changed = oldMap.keys.filter { key ->
            if (key !in stillIdentity) return@filter false
            val old = oldMap[key]
            if (IdentityProjection.isUnset(old)) return@filter false
            old != newMap[key]
        }.sorted()
        if (changed.isEmpty()) return emptyList()
        return listOf(
            ValidationIssue(
                code = "IDENTIFIER_IMMUTABLE",
                message = "Identifier fields are immutable on update: ${changed.joinToString(", ")}",
                path = path,
                subject = subject,
                schema = ValidationSchemaRef(
                    locus = ValidationLocus.IDENTITY,
                    schemaType = incomingType,
                    schemaVersion = incomingSchemaVersion,
                    fieldPath = null,
                ),
            ),
        )
    }

    fun validateEntityIdentifierImmutability(
        stored: Entity,
        incoming: Entity,
        path: String,
        index: Int? = null,
    ): List<ValidationIssue> =
        validateIdentifierImmutability(
            storedType = stored.type,
            storedSchemaVersion = stored.schemaVersion,
            storedDocument = stored.payload,
            incomingType = incoming.type,
            incomingSchemaVersion = incoming.schemaVersion,
            incomingDocument = incoming.payload,
            path = "$path.payload",
            subject = entitySubject(incoming, index),
        )

    fun validateEdgeIdentifierImmutability(
        stored: Edge,
        incoming: Edge,
        path: String,
        index: Int? = null,
    ): List<ValidationIssue> {
        val storedType = stored.type ?: return emptyList()
        val storedVersion = stored.schemaVersion ?: return emptyList()
        val incomingType = incoming.type ?: return emptyList()
        val incomingVersion = incoming.schemaVersion ?: return emptyList()
        return validateIdentifierImmutability(
            storedType = storedType,
            storedSchemaVersion = storedVersion,
            storedDocument = stored.properties.orEmpty(),
            incomingType = incomingType,
            incomingSchemaVersion = incomingVersion,
            incomingDocument = incoming.properties.orEmpty(),
            path = "$path.properties",
            subject = edgeSubject(incoming, index),
        )
    }

    private fun validateAgainstSchema(
        schemaDoc: Map<String, Any?>,
        data: Map<String, Any?>,
        path: String,
        subject: ValidationSubject,
        locus: ValidationLocus,
        schemaType: String,
        schemaVersion: String,
    ): List<ValidationIssue> {
        return try {
            val schemaNode = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(schemaDoc)
            val dataNode = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(data)
            val schema = schemaFactory.getSchema(schemaNode)
            val errors = schema.validate(dataNode)
            errors.map { msg ->
                ValidationIssue(
                    code = "SCHEMA_VIOLATION",
                    message = msg.message,
                    path = path,
                    subject = subject,
                    schema = ValidationSchemaRef(
                        locus = locus,
                        schemaType = schemaType,
                        schemaVersion = schemaVersion,
                        fieldPath = jsonPointerFrom(msg),
                    ),
                )
            }
        } catch (ex: Exception) {
            listOf(
                ValidationIssue(
                    code = "SCHEMA_ERROR",
                    message = ex.message ?: "schema validation failed",
                    path = path,
                    subject = subject,
                    schema = ValidationSchemaRef(
                        locus = locus,
                        schemaType = schemaType,
                        schemaVersion = schemaVersion,
                    ),
                ),
            )
        }
    }

    private fun entitySubject(entity: Entity, index: Int?): ValidationSubject =
        ValidationSubject(
            kind = ValidationSubjectKind.ENTITY,
            id = entity.id,
            index = index,
            type = entity.type,
            schemaVersion = entity.schemaVersion,
            document = entity.payload,
        )

    private fun edgeSubject(edge: Edge, index: Int?): ValidationSubject =
        ValidationSubject(
            kind = ValidationSubjectKind.EDGE,
            id = edge.id,
            index = index,
            type = edge.type,
            schemaVersion = edge.schemaVersion,
            document = edge.properties,
            role = edge.role,
            sourceId = edge.source,
            targetId = edge.target,
        )

    companion object {
        internal fun jsonPointerFrom(msg: ValidationMessage): String? {
            val raw = msg.instanceLocation?.toString()?.trim().orEmpty()
            if (raw.isEmpty() || raw == "$" || raw == "#") return null
            return when {
                raw.startsWith("/") -> raw
                raw.startsWith("$.") ->
                    raw.removePrefix("$")
                        .split('.')
                        .filter { it.isNotEmpty() }
                        .joinToString(prefix = "/", separator = "/") { it }
                raw.startsWith("$[") -> "/${raw.removePrefix("$")}"
                else -> if (raw.startsWith("$")) null else "/$raw"
            }
        }
    }
}
