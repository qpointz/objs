package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.validation.BoMValidationIssue
import org.poc.objs.core.validation.BoMValidationResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * In-memory schema and edge-rule registry under `/api/v1/objs/registry`.
 */
@RestController
@RequestMapping("/api/v1/objs/registry")
@Tag(name = "registry")
class ObjsRegistryController(
    private val schemas: BoMSchemaCatalog,
    private val edgeRules: BoMAllowedEdgeCatalog,
) {
    @GetMapping("/types")
    @Operation(summary = "List distinct schema type names")
    fun types(): Set<String> = schemas.types()

    @GetMapping("/schemas")
    @Operation(summary = "List all registered schemas")
    fun listSchemas(): Collection<BoMSchema> = schemas.all()

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
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BoMValidationResult.of(
                    BoMValidationIssue("SCHEMA_NOT_FOUND", "No schema for type=$type version=$version"),
                ),
            )
        return ResponseEntity.ok(schema)
    }

    @PutMapping("/schemas/{type}/{version}")
    @Operation(summary = "Register or replace a JSON Schema document")
    fun putSchema(
        @PathVariable type: String,
        @PathVariable version: String,
        @RequestBody body: Map<String, Any?>,
    ): ResponseEntity<Any> {
        if (body.isEmpty()) {
            return ResponseEntity.badRequest().body(
                BoMValidationResult.of(
                    BoMValidationIssue("SCHEMA_BODY_EMPTY", "JSON Schema body must be a non-empty object"),
                ),
            )
        }
        val schema = BoMSchema(type = type, version = version, schema = body)
        schemas.register(schema)
        return ResponseEntity.ok(schema)
    }

    @DeleteMapping("/schemas/{type}/{version}")
    @Operation(summary = "Remove a schema version")
    fun deleteSchema(
        @PathVariable type: String,
        @PathVariable version: String,
    ): ResponseEntity<Any> {
        if (!schemas.remove(type, version)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BoMValidationResult.of(
                    BoMValidationIssue("SCHEMA_NOT_FOUND", "No schema for type=$type version=$version"),
                ),
            )
        }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/edges")
    @Operation(summary = "List registered edge definitions (allow-list rules)")
    fun listEdges(): Collection<BoMAllowedEdgeRule> = edgeRules.all()

    @PutMapping("/edges")
    @Operation(summary = "Register or replace an edge definition")
    fun putEdge(@RequestBody body: EdgeRequest): BoMAllowedEdgeRule {
        val rule = BoMAllowedEdgeRule(
            sourceType = body.sourceType,
            role = body.role,
            targetType = body.targetType,
            propertiesPolicy = body.propertiesPolicy ?: BoMPropertiesPolicy.NONE,
            emptyPropertiesAllowed = body.emptyPropertiesAllowed ?: true,
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

    data class EdgeRequest(
        val sourceType: String,
        val role: String,
        val targetType: String,
        val propertiesPolicy: BoMPropertiesPolicy? = null,
        val emptyPropertiesAllowed: Boolean? = null,
    )
}
