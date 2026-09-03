package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.api.domain.CatalogMetadata
import org.poc.objs.api.domain.CatalogSupport
import org.poc.objs.api.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.api.domain.EdgeCardinality
import org.poc.objs.api.domain.PropertiesPolicy
import org.poc.objs.api.domain.Schema
import org.poc.objs.api.domain.SchemaCatalog
import org.poc.objs.api.domain.SchemaDefinitionException
import org.poc.objs.api.domain.SchemaNode
import org.poc.objs.api.domain.SchemaUsage
import org.poc.objs.api.domain.SchemaVersioning
import org.poc.objs.api.seed.CATALOG_SEED_KINDS
import org.poc.objs.core.seed.CanonicalSeedSerializer
import org.poc.objs.api.seed.SeedImportException
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.api.domain.JsonSchemaExportOptions
import org.poc.objs.api.domain.JsonSchemaExportOptionsException
import org.poc.objs.api.domain.FullCatalogJsonSchemaExporter
import org.poc.objs.api.validation.ValidationIssue
import org.poc.objs.api.validation.ValidationResult
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** Persistent object-schema and edge-rule registry under `/api/v1/objs/registry`. */
@RestController
@RequestMapping("/api/v1/objs/registry")
@Tag(name = "registry")
class ObjsRegistryController(
    private val schemas: SchemaCatalog,
    private val edgeRules: AllowedEdgeCatalog,
    private val catalog: CatalogSupport,
    private val seedImporter: SeedImporter,
    private val seedSerializer: CanonicalSeedSerializer,
    private val fullCatalogJsonSchema: FullCatalogJsonSchemaExporter,
) {
    @PostMapping("/refresh")
    @Operation(
        summary = "Rehydrate schema and allowed-edge catalogs from the durable store",
        description = "Forces both catalogs to reload from PostgreSQL, discarding the in-memory " +
            "snapshot. Use after out-of-band catalog changes (e.g. truncate) when waiting for " +
            "`objs.catalogs.cache-ttl` is not acceptable. No-op for pure in-memory catalogs.",
    )
    fun refreshCatalogs(): Map<String, Any> {
        schemas.refreshFromStore()
        edgeRules.refreshFromStore()
        return mapOf(
            "schemas" to schemas.all().size,
            "edgeRules" to edgeRules.all().size,
        )
    }

    @PostMapping(
        "/import",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(summary = "Import ontology seed documents (MERGE, transactional)")
    fun importRegistry(
        @RequestParam format: String,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Any> {
        if (format != ObjsIoFormats.SEEDS) {
            return ObjsIoFormats.unknownFormat(format)
        }
        val yaml = file.bytes.toString(Charsets.UTF_8)
        return try {
            ResponseEntity.ok(seedImporter.importYaml(yaml, CATALOG_SEED_KINDS))
        } catch (ex: SeedImportException) {
            ResponseEntity.badRequest().body(ex.result)
        }
    }

    @GetMapping("/export")
    @Operation(
        summary = "Export ontology catalogs in the requested format",
        description = "Formats: seeds | json-schema | json-schema-codegen. " +
            "For JSON Schema formats, optional dialect / includeEdges / includeEdgePropertySchemas " +
            "configure the full-catalog projection (defaults: 2020-12, outbound, true). " +
            "dialect: 2020-12 | draft-07. includeEdges: none | outbound | linked. " +
            "json-schema-codegen adds a synthetic root that \$refs every catalog def (POJO tools).",
    )
    fun exportRegistry(
        @RequestParam format: String,
        @RequestParam(required = false) dialect: String?,
        @RequestParam(required = false) includeEdges: String?,
        @RequestParam(required = false) includeEdgePropertySchemas: Boolean?,
    ): ResponseEntity<Any> {
        return when (format) {
            ObjsIoFormats.SEEDS -> {
                val yaml = seedSerializer.serializeCatalogs(
                    includeSchemas = true,
                    includeEdgeRules = true,
                    graphs = emptyList(),
                )
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, ObjsIoFormats.YAML_MEDIA_TYPE)
                    .body(yaml)
            }
            ObjsIoFormats.JSON_SCHEMA, ObjsIoFormats.JSON_SCHEMA_CODEGEN -> {
                val options = try {
                    JsonSchemaExportOptions.fromWire(
                        dialect = dialect,
                        includeEdges = includeEdges,
                        includeEdgePropertySchemas = includeEdgePropertySchemas,
                    )
                } catch (ex: JsonSchemaExportOptionsException) {
                    return ResponseEntity.badRequest().body(
                        ValidationResult.of(
                            ValidationIssue(
                                code = "JSON_SCHEMA_OPTIONS_INVALID",
                                message = ex.message ?: "Invalid JSON Schema export options",
                                path = "options",
                            ),
                        ),
                    )
                }
                val body = if (format == ObjsIoFormats.JSON_SCHEMA_CODEGEN) {
                    fullCatalogJsonSchema.exportForCodegen(options)
                } else {
                    fullCatalogJsonSchema.export(options)
                }
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, ObjsIoFormats.JSON_SCHEMA_MEDIA_TYPE)
                    .body(body)
            }
            else -> ObjsIoFormats.unknownFormat(format)
        }
    }

    @GetMapping("/types")
    @Operation(summary = "List distinct schema type names")
    fun types(@RequestParam(required = false) usage: SchemaUsage?): Set<String> {
        val all = schemas.all()
        val filtered = if (usage == null) all else all.filter { it.usage == usage }
        return filtered.map { it.type }.toSortedSet()
    }

    @GetMapping("/schemas")
    @Operation(summary = "List registered schemas, optionally filtered by usage")
    fun listSchemas(@RequestParam(required = false) usage: SchemaUsage?): Collection<Schema> {
        val all = schemas.all()
        return if (usage == null) all else all.filter { it.usage == usage }
    }

    @GetMapping("/schemas/{type}")
    @Operation(summary = "List schema versions for a type")
    fun listSchemasByType(@PathVariable type: String): ResponseEntity<Any> {
        val list = schemas.listByType(type)
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ValidationResult.of(
                    ValidationIssue("SCHEMA_TYPE_NOT_FOUND", "No schemas for type=$type"),
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
        if (schema.usage != SchemaUsage.EDGE_PROPERTIES) {
            return ResponseEntity.badRequest().body(
                ValidationResult.of(
                    ValidationIssue(
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
        if (schema.usage != SchemaUsage.EDGE_PROPERTIES) {
            return ResponseEntity.badRequest().body(
                ValidationResult.of(
                    ValidationIssue(
                        "SCHEMA_USAGE_INVALID",
                        "Schema $type@$version is not an EDGE_PROPERTIES schema",
                    ),
                ),
            )
        }

        val issues = mutableListOf<ValidationIssue>()
        val keys = mutableSetOf<Triple<String, String, String>>()
        body.forEachIndexed { index, request ->
            if (request.sourceType.isBlank()) {
                issues += ValidationIssue("EDGE_SOURCE_REQUIRED", "Source type must not be blank", "[$index].sourceType")
            }
            if (request.role.isBlank()) {
                issues += ValidationIssue("EDGE_ROLE_REQUIRED", "Role must not be blank", "[$index].role")
            }
            if (request.targetType.isBlank()) {
                issues += ValidationIssue("EDGE_TARGET_REQUIRED", "Target type must not be blank", "[$index].targetType")
            }
            val key = Triple(request.sourceType.trim(), request.role.trim(), request.targetType.trim())
            if (!keys.add(key)) {
                issues += ValidationIssue(
                    "EDGE_RELATION_DUPLICATE",
                    "Duplicate relation (${key.first}, ${key.second}, ${key.third})",
                    "[$index]",
                )
            }
            if (
                request.sourceType != AllowedEdgeRule.ANY &&
                schemas.listByType(request.sourceType.trim()).none { it.usage == SchemaUsage.ENTITY }
            ) {
                issues += ValidationIssue(
                    "EDGE_SOURCE_SCHEMA_NOT_FOUND",
                    "No ENTITY schema for source type=${request.sourceType}",
                    "[$index].sourceType",
                )
            }
            if (
                request.targetType != AllowedEdgeRule.ANY &&
                schemas.listByType(request.targetType.trim()).none { it.usage == SchemaUsage.ENTITY }
            ) {
                issues += ValidationIssue(
                    "EDGE_TARGET_SCHEMA_NOT_FOUND",
                    "No ENTITY schema for target type=${request.targetType}",
                    "[$index].targetType",
                )
            }
        }
        if (issues.isNotEmpty()) {
            return ResponseEntity.badRequest().body(ValidationResult.of(issues))
        }

        val previous = edgeRules.all().filter {
            it.propertiesSchemaType == type && it.propertiesSchemaVersion == version
        }
        val replacements = body.map {
            AllowedEdgeRule(
                sourceType = it.sourceType.trim(),
                role = it.role.trim(),
                targetType = it.targetType.trim(),
                propertiesPolicy = PropertiesPolicy.SCHEMA,
                emptyPropertiesAllowed = it.emptyPropertiesAllowed,
                propertiesSchemaType = type,
                propertiesSchemaVersion = version,
                cardinality = it.cardinality ?: EdgeCardinality.UNSPECIFIED,
                description = CatalogMetadata.optionalText(it.description),
                sourceVerb = CatalogMetadata.optionalText(it.sourceVerb),
                targetVerb = CatalogMetadata.optionalText(it.targetVerb),
                tags = CatalogMetadata.tags(it.tags),
                attributes = CatalogMetadata.attributes(it.attributes),
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
        } catch (ex: SchemaDefinitionException) {
            SchemaLintResponse(
                issues = listOf(
                    ValidationIssue("SCHEMA_DEFINITION_INVALID", ex.message ?: "Invalid schema definition"),
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
                schema.usage != SchemaUsage.EDGE_PROPERTIES &&
                edgeRules.all().any {
                    it.propertiesSchemaType == type && it.propertiesSchemaVersion == version
                }
            ) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ValidationResult.of(
                        ValidationIssue(
                            "SCHEMA_IN_USE",
                            "Remove associated edge relations before removing EDGE_PROPERTIES usage",
                        ),
                    ),
                )
            }
            schemas.register(schema)
            ResponseEntity.ok(schemas.get(type, version))
        } catch (ex: SchemaDefinitionException) {
            ResponseEntity.badRequest().body(
                ValidationResult.of(
                    ValidationIssue("SCHEMA_DEFINITION_INVALID", ex.message ?: "Invalid schema definition"),
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
            val nextVersion = SchemaVersioning.nextMajor(schemas.listByType(type).map { it.version })
            if (schemas.contains(type, nextVersion)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ValidationResult.of(
                        ValidationIssue(
                            "SCHEMA_VERSION_EXISTS",
                            "Schema already exists for type=$type version=$nextVersion",
                        ),
                    ),
                )
            }
            val schema = normalizeRequest(type, nextVersion, body)
            schemas.register(schema)
            ResponseEntity.status(HttpStatus.CREATED).body(schemas.get(type, nextVersion))
        } catch (ex: SchemaDefinitionException) {
            ResponseEntity.badRequest().body(
                ValidationResult.of(
                    ValidationIssue("SCHEMA_DEFINITION_INVALID", ex.message ?: "Invalid schema definition"),
                ),
            )
        }
    }

    @DeleteMapping("/schemas/{type}")
    @Operation(
        summary = "Remove all versions of a schema type and incident allow-list rules",
        description = "Deletes every version of {type}, plus edge rules where the type is source or " +
            "target (including wildcards that match), and rules that reference the type as a " +
            "properties schema.",
    )
    fun deleteSchemaType(@PathVariable type: String): ResponseEntity<Any> {
        val versions = schemas.listByType(type)
        if (versions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ValidationResult.of(
                    ValidationIssue("SCHEMA_TYPE_NOT_FOUND", "No schema versions for type=$type"),
                ),
            )
        }
        val rulesToRemove = edgeRules.all().filter { rule ->
            matchesType(rule.sourceType, type) ||
                matchesType(rule.targetType, type) ||
                rule.propertiesSchemaType == type
        }
        for (rule in rulesToRemove) {
            edgeRules.remove(rule.sourceType, rule.role, rule.targetType)
        }
        for (schema in versions) {
            schemas.remove(schema.type, schema.version)
        }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/schemas/{type}/{version}")
    @Operation(summary = "Remove a schema version")
    fun deleteSchemaVersion(
        @PathVariable type: String,
        @PathVariable version: String,
    ): ResponseEntity<Any> {
        if (
            edgeRules.all().any {
                it.propertiesSchemaType == type && it.propertiesSchemaVersion == version
            }
        ) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ValidationResult.of(
                    ValidationIssue(
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
    fun listEdges(): Collection<AllowedEdgeRule> = edgeRules.all()

    @GetMapping("/types/{type}/edges")
    @Operation(summary = "List incoming and outgoing edge rules for an entity type, including wildcards")
    fun edgesForType(@PathVariable type: String): TypeEdgesResponse {
        val allowed = catalog.allowedEdgesForType(type)
        return TypeEdgesResponse(incoming = allowed.incoming, outgoing = allowed.outgoing)
    }

    @PutMapping("/edges")
    @Operation(summary = "Register or replace an edge definition")
    fun putEdge(@RequestBody body: EdgeRequest): AllowedEdgeRule {
        val rule = AllowedEdgeRule(
            sourceType = body.sourceType,
            role = body.role,
            targetType = body.targetType,
            propertiesPolicy = body.propertiesPolicy ?: PropertiesPolicy.NONE,
            emptyPropertiesAllowed = body.emptyPropertiesAllowed ?: true,
            propertiesSchemaType = body.propertiesSchemaType,
            propertiesSchemaVersion = body.propertiesSchemaVersion,
            cardinality = body.cardinality ?: EdgeCardinality.UNSPECIFIED,
            description = CatalogMetadata.optionalText(body.description),
            sourceVerb = CatalogMetadata.optionalText(body.sourceVerb),
            targetVerb = CatalogMetadata.optionalText(body.targetVerb),
            tags = CatalogMetadata.tags(body.tags),
            attributes = CatalogMetadata.attributes(body.attributes),
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
                ValidationResult.of(
                    ValidationIssue(
                        "EDGE_DEFINITION_NOT_FOUND",
                        "No edge definition for ($sourceType, $role, $targetType)",
                    ),
                ),
            )
        }
        return ResponseEntity.noContent().build()
    }

    private fun normalizeRequest(type: String, version: String, body: SchemaDefinitionRequest): Schema {
        val usage = body.usage ?: SchemaUsage.ENTITY
        return org.poc.objs.api.domain.SchemaNormalizer.normalizeStrict(
            Schema(
                type = type,
                version = version,
                contentSchema = body.contentSchema,
                usage = usage,
                tags = CatalogMetadata.tags(body.tags),
                attributes = CatalogMetadata.attributes(body.attributes),
            ),
        )
    }

    private fun notFoundSchema(type: String, version: String): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ValidationResult.of(
                ValidationIssue("SCHEMA_NOT_FOUND", "No schema for type=$type version=$version"),
            ),
        )

    private fun matchesType(pattern: String, type: String): Boolean =
        pattern == AllowedEdgeRule.ANY || pattern == type

    data class SchemaDefinitionRequest(
        val contentSchema: SchemaNode,
        val usage: SchemaUsage? = null,
        val tags: List<String> = emptyList(),
        val attributes: Map<String, String> = emptyMap(),
    )

    data class SchemaLintResponse(
        val issues: List<ValidationIssue> = emptyList(),
        val schema: Schema? = null,
        val jsonSchema: Map<String, Any?>? = null,
    ) {
        val valid: Boolean get() = issues.isEmpty()
    }

    data class TypeEdgesResponse(
        val incoming: List<AllowedEdgeRule>,
        val outgoing: List<AllowedEdgeRule>,
    )

    data class EdgeRequest(
        val sourceType: String,
        val role: String,
        val targetType: String,
        val propertiesPolicy: PropertiesPolicy? = null,
        val emptyPropertiesAllowed: Boolean? = null,
        val propertiesSchemaType: String? = null,
        val propertiesSchemaVersion: String? = null,
        val cardinality: EdgeCardinality? = null,
        val description: String? = null,
        val sourceVerb: String? = null,
        val targetVerb: String? = null,
        val tags: List<String> = emptyList(),
        val attributes: Map<String, String> = emptyMap(),
    )

    data class EdgeRelationRequest(
        val sourceType: String,
        val role: String,
        val targetType: String,
        val emptyPropertiesAllowed: Boolean = true,
        val cardinality: EdgeCardinality? = null,
        val description: String? = null,
        val sourceVerb: String? = null,
        val targetVerb: String? = null,
        val tags: List<String> = emptyList(),
        val attributes: Map<String, String> = emptyMap(),
    )
}
