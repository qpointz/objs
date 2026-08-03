package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMEdgeCardinality
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaDefinitionException
import org.poc.objs.core.domain.BoMSchemaNode
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.domain.BoMSchemaVersioning
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Persistent object-schema and edge-rule registry under `/api/v1/objs/registry`. */
@RestController
@RequestMapping("/api/v1/objs/registry")
@Tag(name = "registry")
class ObjsRegistryController(
    private val schemas: BoMSchemaCatalog,
    private val edgeRules: BoMAllowedEdgeCatalog,
) {
    @GetMapping("/types")
    @Operation(summary = "List distinct schema type names")
    fun types(@RequestParam(required = false) usage: BoMSchemaUsage?): Set<String> {
        val all = schemas.all()
        val filtered = if (usage == null) all else all.filter { usage in it.usages }
        return filtered.map { it.type }.toSortedSet()
    }

    @GetMapping("/schemas")
    @Operation(summary = "List registered schemas, optionally filtered by usage")
    fun listSchemas(@RequestParam(required = false) usage: BoMSchemaUsage?): Collection<BoMSchema> {
        val all = schemas.all()
        return if (usage == null) all else all.filter { usage in it.usages }
    }

    @GetMapping("/schemas/{type}")
    @Operation(summary = "List schema versions for a type")
    fun listSchemasByType(@PathVariable type: String): ResponseEntity<Any> {
        val list = schemas.listByType(type)
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BoMValidationResult.of(
                    BoMValidationIssue("SCHEMA_TYPE_NOT_FOUND", "No schemas for type=$type"),
                ),
            )
        }
        return ResponseEntity.ok(list)
    }

    @GetMapping("/schemas/{type}/{version}")
    @Operation(summary = "Get one schema by type and version")
    fun getSchema(
        @PathVariable type: String,
        @PathVariable version: String,
    ): ResponseEntity<Any> {
        val schema = schemas.get(type, version)
            ?: return notFoundSchema(type, version)
        return ResponseEntity.ok(schema)
    }

    @GetMapping("/schemas/{type}/{version}/json-schema")
    @Operation(summary = "Generate JSON Schema from an object-schema definition")
    fun getJsonSchema(
        @PathVariable type: String,
        @PathVariable version: String,
    ): ResponseEntity<Any> {
        val schema = schemas.get(type, version)
            ?: return notFoundSchema(type, version)
        return ResponseEntity.ok(schema.toJsonSchema())
    }

    @GetMapping("/schemas/{type}/{version}/edges")
    @Operation(summary = "List allowed relations that use an edge-property schema")
    fun listSchemaEdges(
        @PathVariable type: String,
        @PathVariable version: String,
    ): ResponseEntity<Any> {
        val schema = schemas.get(type, version) ?: return notFoundSchema(type, version)
        if (BoMSchemaUsage.EDGE_PROPERTIES !in schema.usages) {
            return ResponseEntity.badRequest().body(
                BoMValidationResult.of(
                    BoMValidationIssue(
                        "SCHEMA_USAGE_INVALID",
                        "Schema $type@$version is not an EDGE_PROPERTIES schema",
                    ),
                ),
            )
        }
        return ResponseEntity.ok(
            edgeRules.all().filter {
                it.propertiesSchemaType == type && it.propertiesSchemaVersion == version
            },
        )
    }

    @PutMapping("/schemas/{type}/{version}/edges")
    @Operation(summary = "Replace allowed relations associated with an edge-property schema")
    fun replaceSchemaEdges(
        @PathVariable type: String,
        @PathVariable version: String,
        @RequestBody body: List<EdgeRelationRequest>,
    ): ResponseEntity<Any> {
        val schema = schemas.get(type, version) ?: return notFoundSchema(type, version)
        if (BoMSchemaUsage.EDGE_PROPERTIES !in schema.usages) {
            return ResponseEntity.badRequest().body(
                BoMValidationResult.of(
                    BoMValidationIssue(
                        "SCHEMA_USAGE_INVALID",
                        "Schema $type@$version is not an EDGE_PROPERTIES schema",
                    ),
                ),
            )
        }

        val issues = mutableListOf<BoMValidationIssue>()
        val keys = mutableSetOf<Triple<String, String, String>>()
        body.forEachIndexed { index, request ->
            if (request.sourceType.isBlank()) {
                issues += BoMValidationIssue("EDGE_SOURCE_REQUIRED", "Source type must not be blank", "[$index].sourceType")
            }
            if (request.role.isBlank()) {
                issues += BoMValidationIssue("EDGE_ROLE_REQUIRED", "Role must not be blank", "[$index].role")
            }
            if (request.targetType.isBlank()) {
                issues += BoMValidationIssue("EDGE_TARGET_REQUIRED", "Target type must not be blank", "[$index].targetType")
            }
            val key = Triple(request.sourceType.trim(), request.role.trim(), request.targetType.trim())
            if (!keys.add(key)) {
                issues += BoMValidationIssue(
                    "EDGE_RELATION_DUPLICATE",
                    "Duplicate relation (${key.first}, ${key.second}, ${key.third})",
                    "[$index]",
                )
            }
            if (
                request.sourceType != BoMAllowedEdgeRule.ANY &&
                schemas.listByType(request.sourceType.trim()).none { BoMSchemaUsage.ENTITY in it.usages }
            ) {
                issues += BoMValidationIssue(
                    "EDGE_SOURCE_SCHEMA_NOT_FOUND",
                    "No ENTITY schema for source type=${request.sourceType}",
                    "[$index].sourceType",
                )
            }
            if (
                request.targetType != BoMAllowedEdgeRule.ANY &&
                schemas.listByType(request.targetType.trim()).none { BoMSchemaUsage.ENTITY in it.usages }
            ) {
                issues += BoMValidationIssue(
                    "EDGE_TARGET_SCHEMA_NOT_FOUND",
                    "No ENTITY schema for target type=${request.targetType}",
                    "[$index].targetType",
                )
            }
        }
        if (issues.isNotEmpty()) {
            return ResponseEntity.badRequest().body(BoMValidationResult.of(issues))
        }

        val previous = edgeRules.all().filter {
            it.propertiesSchemaType == type && it.propertiesSchemaVersion == version
        }
        val replacements = body.map {
            BoMAllowedEdgeRule(
                sourceType = it.sourceType.trim(),
                role = it.role.trim(),
                targetType = it.targetType.trim(),
                propertiesPolicy = BoMPropertiesPolicy.SCHEMA,
                emptyPropertiesAllowed = it.emptyPropertiesAllowed,
                propertiesSchemaType = type,
                propertiesSchemaVersion = version,
                cardinality = it.cardinality ?: BoMEdgeCardinality.UNSPECIFIED,
            )
        }
        val replacementKeys = replacements.map { Triple(it.sourceType, it.role, it.targetType) }.toSet()
        previous
            .filter { Triple(it.sourceType, it.role, it.targetType) !in replacementKeys }
            .forEach { edgeRules.remove(it.sourceType, it.role, it.targetType) }
        replacements.forEach(edgeRules::register)
        return ResponseEntity.ok(replacements)
    }

    @PostMapping("/schemas/{type}/{version}/lint")
    @Operation(summary = "Normalize and lint a schema draft without persisting it")
    fun lintSchema(
        @PathVariable type: String,
        @PathVariable version: String,
        @RequestBody body: SchemaDefinitionRequest,
    ): SchemaLintResponse {
        return try {
            val normalized = normalizeRequest(type, version, body)
            SchemaLintResponse(
                issues = emptyList(),
                schema = normalized,
                jsonSchema = normalized.toJsonSchema(),
            )
        } catch (ex: BoMSchemaDefinitionException) {
            SchemaLintResponse(
                issues = listOf(
                    BoMValidationIssue("SCHEMA_DEFINITION_INVALID", ex.message ?: "Invalid schema definition"),
                ),
            )
        }
    }

    @PutMapping("/schemas/{type}/{version}")
    @Operation(summary = "Register or replace an object-schema DSL definition for an exact version")
    fun putSchema(
        @PathVariable type: String,
        @PathVariable version: String,
        @RequestBody body: SchemaDefinitionRequest,
    ): ResponseEntity<Any> {
        return try {
            val schema = normalizeRequest(type, version, body)
            if (
                BoMSchemaUsage.EDGE_PROPERTIES !in schema.usages &&
                edgeRules.all().any {
                    it.propertiesSchemaType == type && it.propertiesSchemaVersion == version
                }
            ) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    BoMValidationResult.of(
                        BoMValidationIssue(
                            "SCHEMA_IN_USE",
                            "Remove associated edge relations before removing EDGE_PROPERTIES usage",
                        ),
                    ),
                )
            }
            schemas.register(schema)
            ResponseEntity.ok(schemas.get(type, version))
        } catch (ex: BoMSchemaDefinitionException) {
            ResponseEntity.badRequest().body(
                BoMValidationResult.of(
                    BoMValidationIssue("SCHEMA_DEFINITION_INVALID", ex.message ?: "Invalid schema definition"),
                ),
            )
        }
    }

    @PostMapping("/schemas/{type}/versions/next-major")
    @Operation(summary = "Create the next major version for a schema type without overwriting existing versions")
    fun createNextMajor(
        @PathVariable type: String,
        @RequestBody body: SchemaDefinitionRequest,
    ): ResponseEntity<Any> {
        return try {
            val nextVersion = BoMSchemaVersioning.nextMajor(schemas.listByType(type).map { it.version })
            if (schemas.contains(type, nextVersion)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    BoMValidationResult.of(
                        BoMValidationIssue(
                            "SCHEMA_VERSION_EXISTS",
                            "Schema already exists for type=$type version=$nextVersion",
                        ),
                    ),
                )
            }
            val schema = normalizeRequest(type, nextVersion, body)
            schemas.register(schema)
            ResponseEntity.status(HttpStatus.CREATED).body(schemas.get(type, nextVersion))
        } catch (ex: BoMSchemaDefinitionException) {
            ResponseEntity.badRequest().body(
                BoMValidationResult.of(
                    BoMValidationIssue("SCHEMA_DEFINITION_INVALID", ex.message ?: "Invalid schema definition"),
                ),
            )
        }
    }

    @DeleteMapping("/schemas/{type}/{version}")
    @Operation(summary = "Remove a schema version")
    fun deleteSchema(
        @PathVariable type: String,
        @PathVariable version: String,
    ): ResponseEntity<Any> {
        if (
            edgeRules.all().any {
                it.propertiesSchemaType == type && it.propertiesSchemaVersion == version
            }
        ) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                BoMValidationResult.of(
                    BoMValidationIssue(
                        "SCHEMA_IN_USE",
                        "Schema $type@$version is referenced by allowed edge relations",
                    ),
                ),
            )
        }
        if (!schemas.remove(type, version)) {
            return notFoundSchema(type, version)
        }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/edges")
    @Operation(summary = "List registered edge definitions (allow-list rules)")
    fun listEdges(): Collection<BoMAllowedEdgeRule> = edgeRules.all()

    @GetMapping("/types/{type}/edges")
    @Operation(summary = "List incoming and outgoing edge rules for an entity type, including wildcards")
    fun edgesForType(@PathVariable type: String): TypeEdgesResponse {
        val incoming = mutableListOf<BoMAllowedEdgeRule>()
        val outgoing = mutableListOf<BoMAllowedEdgeRule>()
        for (rule in edgeRules.all()) {
            if (matchesType(rule.targetType, type)) incoming += rule
            if (matchesType(rule.sourceType, type)) outgoing += rule
        }
        return TypeEdgesResponse(incoming = incoming, outgoing = outgoing)
    }

    @PutMapping("/edges")
    @Operation(summary = "Register or replace an edge definition")
    fun putEdge(@RequestBody body: EdgeRequest): BoMAllowedEdgeRule {
        val rule = BoMAllowedEdgeRule(
            sourceType = body.sourceType,
            role = body.role,
            targetType = body.targetType,
            propertiesPolicy = body.propertiesPolicy ?: BoMPropertiesPolicy.NONE,
            emptyPropertiesAllowed = body.emptyPropertiesAllowed ?: true,
            propertiesSchemaType = body.propertiesSchemaType,
            propertiesSchemaVersion = body.propertiesSchemaVersion,
            cardinality = body.cardinality ?: BoMEdgeCardinality.UNSPECIFIED,
        )
        edgeRules.register(rule)
        return rule
    }

    @DeleteMapping("/edges")
    @Operation(summary = "Remove an edge definition by exact (sourceType, role, targetType)")
    fun deleteEdge(
        @RequestParam sourceType: String,
        @RequestParam role: String,
        @RequestParam targetType: String,
    ): ResponseEntity<Any> {
        if (!edgeRules.remove(sourceType, role, targetType)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BoMValidationResult.of(
                    BoMValidationIssue(
                        "EDGE_DEFINITION_NOT_FOUND",
                        "No edge definition for ($sourceType, $role, $targetType)",
                    ),
                ),
            )
        }
        return ResponseEntity.noContent().build()
    }

    private fun normalizeRequest(type: String, version: String, body: SchemaDefinitionRequest): BoMSchema {
        val usages = body.usages?.takeIf { it.isNotEmpty() } ?: setOf(BoMSchemaUsage.ENTITY)
        return org.poc.objs.core.domain.BoMSchemaNormalizer.normalizeStrict(
            BoMSchema(
                type = type,
                version = version,
                contentSchema = body.contentSchema,
                usages = usages,
            ),
        )
    }

    private fun notFoundSchema(type: String, version: String): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            BoMValidationResult.of(
                BoMValidationIssue("SCHEMA_NOT_FOUND", "No schema for type=$type version=$version"),
            ),
        )

    private fun matchesType(pattern: String, type: String): Boolean =
        pattern == BoMAllowedEdgeRule.ANY || pattern == type

    data class SchemaDefinitionRequest(
        val contentSchema: BoMSchemaNode,
        val usages: Set<BoMSchemaUsage>? = null,
    )

    data class SchemaLintResponse(
        val issues: List<BoMValidationIssue> = emptyList(),
        val schema: BoMSchema? = null,
        val jsonSchema: Map<String, Any?>? = null,
    ) {
        val valid: Boolean get() = issues.isEmpty()
    }

    data class TypeEdgesResponse(
        val incoming: List<BoMAllowedEdgeRule>,
        val outgoing: List<BoMAllowedEdgeRule>,
    )

    data class EdgeRequest(
        val sourceType: String,
        val role: String,
        val targetType: String,
        val propertiesPolicy: BoMPropertiesPolicy? = null,
        val emptyPropertiesAllowed: Boolean? = null,
        val propertiesSchemaType: String? = null,
        val propertiesSchemaVersion: String? = null,
        val cardinality: BoMEdgeCardinality? = null,
    )

    data class EdgeRelationRequest(
        val sourceType: String,
        val role: String,
        val targetType: String,
        val emptyPropertiesAllowed: Boolean = true,
        val cardinality: BoMEdgeCardinality? = null,
    )
}
