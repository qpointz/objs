package org.poc.objs.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema as ApiSchema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
        tags = ["catalog"],
        summary = "Rehydrate schema and allowed-edge catalogs from the durable store",
        description = "Forces both catalogs to reload from PostgreSQL, discarding the in-memory " +
            "snapshot. Use after out-of-band catalog changes (e.g. truncate) when waiting for " +
            "`objs.catalogs.cache-ttl` is not acceptable. No-op for pure in-memory catalogs.",
    )
    @ApiResponse(responseCode = "200", description = "Reloaded counts: `schemas` and `edgeRules`")
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
    @Operation(
        tags = ["catalog"],
        summary = "Import ontology seed documents (MERGE, transactional)",
        description = "Multipart upload of a seed YAML holding catalog-kind documents (schemas, edge " +
            "rules). All documents apply in one transaction: any failure rolls the whole import back.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Import summary (applied seed documents)"),
        ApiResponse(
            responseCode = "400",
            description = "Unknown format, or seed parse/validation failure",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun importRegistry(
        @Parameter(description = "Payload format; only `seeds` is supported")
        @RequestParam format: String,
        @Parameter(description = "Seed YAML file holding Schema / AllowedEdge documents")
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
        tags = ["catalog"],
        summary = "Export ontology catalogs in the requested format",
        description = "Formats: seeds | json-schema | json-schema-codegen. " +
            "For JSON Schema formats, optional dialect / includeEdges / includeEdgePropertySchemas " +
            "configure the full-catalog projection (defaults: 2020-12, outbound, true). " +
            "dialect: 2020-12 | draft-07. includeEdges: none | outbound | linked. " +
            "json-schema-codegen adds a synthetic root that \$refs every catalog def (POJO tools).",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Seed YAML (format=seeds) or a JSON Schema document (JSON Schema formats)",
            content = [
                Content(mediaType = ObjsIoFormats.YAML_MEDIA_TYPE, schema = ApiSchema(type = "string")),
                Content(mediaType = ObjsIoFormats.JSON_SCHEMA_MEDIA_TYPE, schema = ApiSchema(type = "object")),
            ],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Unknown format or invalid JSON Schema export options",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun exportRegistry(
        @Parameter(description = "One of `seeds`, `json-schema`, `json-schema-codegen`")
        @RequestParam format: String,
        @Parameter(description = "JSON Schema dialect: `2020-12` (default) or `draft-07`")
        @RequestParam(required = false) dialect: String?,
        @Parameter(description = "Edge projection: `none`, `outbound` (default), or `linked`")
        @RequestParam(required = false) includeEdges: String?,
        @Parameter(description = "Inline edge property schemas in the projection (default true)")
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
    @Operation(
        tags = ["schemas"],
        summary = "List distinct schema type names",
        description = "Sorted type names across all versions; filter by usage to separate entity types " +
            "from edge-property schemas.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Sorted distinct type names",
        content = [Content(array = ArraySchema(schema = ApiSchema(type = "string")))],
    )
    fun types(
        @Parameter(description = "Restrict to schemas with this usage (`ENTITY` or `EDGE_PROPERTIES`)")
        @RequestParam(required = false) usage: SchemaUsage?,
    ): Set<String> {
        val all = schemas.all()
        val filtered = if (usage == null) all else all.filter { it.usage == usage }
        return filtered.map { it.type }.toSortedSet()
    }

    @GetMapping("/schemas")
    @Operation(tags = ["schemas"], summary = "List registered schemas, optionally filtered by usage")
    @ApiResponse(
        responseCode = "200",
        description = "All registered schema versions",
        content = [Content(array = ArraySchema(schema = ApiSchema(implementation = Schema::class)))],
    )
    fun listSchemas(
        @Parameter(description = "Restrict to schemas with this usage (`ENTITY` or `EDGE_PROPERTIES`)")
        @RequestParam(required = false) usage: SchemaUsage?,
    ): Collection<Schema> {
        val all = schemas.all()
        return if (usage == null) all else all.filter { it.usage == usage }
    }

    @GetMapping("/schemas/{type}")
    @Operation(tags = ["schemas"], summary = "List schema versions for a type")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Every registered version of the type",
            content = [Content(array = ArraySchema(schema = ApiSchema(implementation = Schema::class)))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "SCHEMA_TYPE_NOT_FOUND",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun listSchemasByType(
        @Parameter(description = "Schema type name") @PathVariable type: String,
    ): ResponseEntity<Any> {
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
    @Operation(tags = ["schemas"], summary = "Get one schema by type and version")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Schema definition",
            content = [Content(schema = ApiSchema(implementation = Schema::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "SCHEMA_NOT_FOUND",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun getSchema(
        @Parameter(description = "Schema type name") @PathVariable type: String,
        @Parameter(description = "Exact schema version") @PathVariable version: String,
    ): ResponseEntity<Any> {
        val schema = schemas.get(type, version)
            ?: return notFoundSchema(type, version)
        return ResponseEntity.ok(schema)
    }

    @GetMapping("/schemas/{type}/{version}/json-schema")
    @Operation(
        tags = ["schemas"],
        summary = "Generate JSON Schema from an object-schema definition",
        description = "Single-type projection; use GET /registry/export for the full-catalog document.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "JSON Schema document for this type@version",
            content = [Content(schema = ApiSchema(type = "object"))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "SCHEMA_NOT_FOUND",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun getJsonSchema(
        @Parameter(description = "Schema type name") @PathVariable type: String,
        @Parameter(description = "Exact schema version") @PathVariable version: String,
    ): ResponseEntity<Any> {
        val schema = schemas.get(type, version)
            ?: return notFoundSchema(type, version)
        return ResponseEntity.ok(schema.toJsonSchema())
    }

    @GetMapping("/schemas/{type}/{version}/edges")
    @Operation(tags = ["schemas"], summary = "List allowed relations that use an edge-property schema")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Edge rules whose property schema is this type@version",
            content = [
                Content(array = ArraySchema(schema = ApiSchema(implementation = AllowedEdgeRule::class))),
            ],
        ),
        ApiResponse(
            responseCode = "400",
            description = "SCHEMA_USAGE_INVALID (not an EDGE_PROPERTIES schema)",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "SCHEMA_NOT_FOUND",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun listSchemaEdges(
        @Parameter(description = "EDGE_PROPERTIES schema type name") @PathVariable type: String,
        @Parameter(description = "Exact schema version") @PathVariable version: String,
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
    @Operation(
        tags = ["schemas"],
        summary = "Replace allowed relations associated with an edge-property schema",
        description = "Full replace: relations absent from the body are removed. Every relation gets " +
            "propertiesPolicy=SCHEMA pointing at this type@version.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The relations now registered for this schema",
            content = [
                Content(array = ArraySchema(schema = ApiSchema(implementation = AllowedEdgeRule::class))),
            ],
        ),
        ApiResponse(
            responseCode = "400",
            description = "SCHEMA_USAGE_INVALID, duplicate relations, or unknown source/target types",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "SCHEMA_NOT_FOUND",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun replaceSchemaEdges(
        @Parameter(description = "EDGE_PROPERTIES schema type name") @PathVariable type: String,
        @Parameter(description = "Exact schema version") @PathVariable version: String,
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
    @Operation(
        tags = ["schemas"],
        summary = "Normalize and lint a schema draft without persisting it",
        description = "Always 200: a rejected draft comes back with `valid=false` and `issues` rather " +
            "than an error status.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lint outcome with the normalized schema and its JSON Schema when valid",
        content = [Content(schema = ApiSchema(implementation = SchemaLintResponse::class))],
    )
    fun lintSchema(
        @Parameter(description = "Schema type name the draft would be stored under")
        @PathVariable type: String,
        @Parameter(description = "Exact schema version the draft would be stored under")
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
    @Operation(
        tags = ["schemas"],
        summary = "Register or replace an object-schema DSL definition for an exact version",
        description = "In-place overwrite of this version; use POST …/versions/next-major to add a new " +
            "version instead. Dropping EDGE_PROPERTIES usage is rejected while edge rules reference it.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Stored (normalized) schema",
            content = [Content(schema = ApiSchema(implementation = Schema::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "SCHEMA_DEFINITION_INVALID",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "SCHEMA_IN_USE (edge relations still reference this properties schema)",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun putSchema(
        @Parameter(description = "Schema type name") @PathVariable type: String,
        @Parameter(description = "Exact schema version to write") @PathVariable version: String,
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
    @Operation(
        tags = ["schemas"],
        summary = "Create the next major version for a schema type without overwriting existing versions",
        description = "Derives the version number from the highest existing major for this type.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Newly created schema version",
            content = [Content(schema = ApiSchema(implementation = Schema::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "SCHEMA_DEFINITION_INVALID",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "SCHEMA_VERSION_EXISTS",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun createNextMajor(
        @Parameter(description = "Schema type name") @PathVariable type: String,
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
        tags = ["schemas"],
        summary = "Remove all versions of a schema type and incident allow-list rules",
        description = "Deletes every version of {type}, plus edge rules where the type is source or " +
            "target (including wildcards that match), and rules that reference the type as a " +
            "properties schema.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Type and its incident rules removed"),
        ApiResponse(
            responseCode = "404",
            description = "SCHEMA_TYPE_NOT_FOUND",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun deleteSchemaType(
        @Parameter(description = "Schema type name") @PathVariable type: String,
    ): ResponseEntity<Any> {
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
    @Operation(
        tags = ["schemas"],
        summary = "Remove a schema version",
        description = "Other versions of the type and its edge rules are kept; use DELETE " +
            "/registry/schemas/{type} to drop everything for the type.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Version removed"),
        ApiResponse(
            responseCode = "404",
            description = "SCHEMA_NOT_FOUND",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "SCHEMA_IN_USE (referenced by allowed edge relations)",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun deleteSchemaVersion(
        @Parameter(description = "Schema type name") @PathVariable type: String,
        @Parameter(description = "Exact schema version") @PathVariable version: String,
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
    @Operation(
        tags = ["edges"],
        summary = "List registered edge definitions (allow-list rules)",
        description = "An edge is only writable when some rule matches its (sourceType, role, targetType); " +
            "`*` acts as a wildcard on source or target.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "All allow-list rules",
        content = [Content(array = ArraySchema(schema = ApiSchema(implementation = AllowedEdgeRule::class)))],
    )
    fun listEdges(): Collection<AllowedEdgeRule> = edgeRules.all()

    @GetMapping("/types/{type}/edges")
    @Operation(
        tags = ["edges"],
        summary = "List incoming and outgoing edge rules for an entity type, including wildcards",
        description = "Resolved view used by editors to offer the relations available on an entity.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Rules where the type is a valid target (incoming) or source (outgoing)",
        content = [Content(schema = ApiSchema(implementation = TypeEdgesResponse::class))],
    )
    fun edgesForType(
        @Parameter(description = "Entity schema type name") @PathVariable type: String,
    ): TypeEdgesResponse {
        val allowed = catalog.allowedEdgesForType(type)
        return TypeEdgesResponse(incoming = allowed.incoming, outgoing = allowed.outgoing)
    }

    @PutMapping("/edges")
    @Operation(
        tags = ["edges"],
        summary = "Register or replace an edge definition",
        description = "Upsert keyed by (sourceType, role, targetType).",
    )
    @ApiResponse(
        responseCode = "200",
        description = "The stored rule",
        content = [Content(schema = ApiSchema(implementation = AllowedEdgeRule::class))],
    )
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
    @Operation(
        tags = ["edges"],
        summary = "Remove an edge definition by exact (sourceType, role, targetType)",
        description = "Exact key match; a wildcard rule must be removed with `*` spelled out.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Rule removed"),
        ApiResponse(
            responseCode = "404",
            description = "EDGE_DEFINITION_NOT_FOUND",
            content = [Content(schema = ApiSchema(implementation = ValidationResult::class))],
        ),
    )
    fun deleteEdge(
        @Parameter(description = "Source entity type, or `*` for the wildcard rule")
        @RequestParam sourceType: String,
        @Parameter(description = "Relation role name")
        @RequestParam role: String,
        @Parameter(description = "Target entity type, or `*` for the wildcard rule")
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

    @ApiSchema(description = "Object-schema DSL draft; type and version come from the path")
    data class SchemaDefinitionRequest(
        @field:ApiSchema(description = "Root node of the object-schema DSL definition")
        val contentSchema: SchemaNode,
        @field:ApiSchema(description = "Schema usage; defaults to ENTITY")
        val usage: SchemaUsage? = null,
        @field:ApiSchema(description = "Catalog tags for grouping and search")
        val tags: List<String> = emptyList(),
        @field:ApiSchema(description = "Free-form catalog attributes (string key/value)")
        val attributes: Map<String, String> = emptyMap(),
    )

    @ApiSchema(description = "Lint outcome for a schema draft (never persisted)")
    data class SchemaLintResponse(
        @field:ApiSchema(description = "Problems found; empty when the draft is valid")
        val issues: List<ValidationIssue> = emptyList(),
        @field:ApiSchema(description = "Normalized schema; null when the draft is invalid")
        val schema: Schema? = null,
        @field:ApiSchema(description = "JSON Schema generated from the normalized draft; null when invalid")
        val jsonSchema: Map<String, Any?>? = null,
    ) {
        @get:ApiSchema(description = "True when [issues] is empty")
        val valid: Boolean get() = issues.isEmpty()
    }

    @ApiSchema(description = "Allowed edge rules resolved for one entity type, wildcards included")
    data class TypeEdgesResponse(
        @field:ApiSchema(description = "Rules where this type is a valid target")
        val incoming: List<AllowedEdgeRule>,
        @field:ApiSchema(description = "Rules where this type is a valid source")
        val outgoing: List<AllowedEdgeRule>,
    )

    @ApiSchema(description = "Allowed-edge rule write body")
    data class EdgeRequest(
        @field:ApiSchema(description = "Source entity type, or `*` to match any source")
        val sourceType: String,
        @field:ApiSchema(description = "Relation role name, e.g. `dependsOn`")
        val role: String,
        @field:ApiSchema(description = "Target entity type, or `*` to match any target")
        val targetType: String,
        @field:ApiSchema(description = "How edge properties are validated; defaults to NONE")
        val propertiesPolicy: PropertiesPolicy? = null,
        @field:ApiSchema(description = "Allow edges with no properties; defaults to true")
        val emptyPropertiesAllowed: Boolean? = null,
        @field:ApiSchema(description = "EDGE_PROPERTIES schema type when propertiesPolicy=SCHEMA")
        val propertiesSchemaType: String? = null,
        @field:ApiSchema(description = "EDGE_PROPERTIES schema version when propertiesPolicy=SCHEMA")
        val propertiesSchemaVersion: String? = null,
        @field:ApiSchema(description = "Relation cardinality; defaults to UNSPECIFIED")
        val cardinality: EdgeCardinality? = null,
        @field:ApiSchema(description = "Human-readable description of the relation")
        val description: String? = null,
        @field:ApiSchema(description = "Verb phrase rendered from the source side")
        val sourceVerb: String? = null,
        @field:ApiSchema(description = "Verb phrase rendered from the target side")
        val targetVerb: String? = null,
        @field:ApiSchema(description = "Catalog tags for grouping and search")
        val tags: List<String> = emptyList(),
        @field:ApiSchema(description = "Free-form catalog attributes (string key/value)")
        val attributes: Map<String, String> = emptyMap(),
    )

    @ApiSchema(
        description = "Relation entry for schema-scoped replace; propertiesPolicy is forced to SCHEMA " +
            "against the path type@version",
    )
    data class EdgeRelationRequest(
        @field:ApiSchema(description = "Source entity type, or `*` to match any source")
        val sourceType: String,
        @field:ApiSchema(description = "Relation role name, e.g. `dependsOn`")
        val role: String,
        @field:ApiSchema(description = "Target entity type, or `*` to match any target")
        val targetType: String,
        @field:ApiSchema(description = "Allow edges with no properties")
        val emptyPropertiesAllowed: Boolean = true,
        @field:ApiSchema(description = "Relation cardinality; defaults to UNSPECIFIED")
        val cardinality: EdgeCardinality? = null,
        @field:ApiSchema(description = "Human-readable description of the relation")
        val description: String? = null,
        @field:ApiSchema(description = "Verb phrase rendered from the source side")
        val sourceVerb: String? = null,
        @field:ApiSchema(description = "Verb phrase rendered from the target side")
        val targetVerb: String? = null,
        @field:ApiSchema(description = "Catalog tags for grouping and search")
        val tags: List<String> = emptyList(),
        @field:ApiSchema(description = "Free-form catalog attributes (string key/value)")
        val attributes: Map<String, String> = emptyMap(),
    )
}
