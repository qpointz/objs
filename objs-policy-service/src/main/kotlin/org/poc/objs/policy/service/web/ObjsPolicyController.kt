package org.poc.objs.policy.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.poc.objs.api.domain.GraphException
import org.poc.objs.api.domain.GraphMaterializationException
import org.poc.objs.api.match.MatcherDsl
import org.poc.objs.api.validation.ValidationException
import org.poc.objs.policy.api.ApplicabilityKinds
import org.poc.objs.policy.api.Category
import org.poc.objs.policy.api.CategoryInUseException
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.PersistPresets
import org.poc.objs.policy.api.PersistSpec
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyQuery
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.service.PolicyCapabilities
import org.poc.objs.policy.service.PolicyCheckResult
import org.poc.objs.policy.service.PolicyPlayService
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/objs/policy")
class ObjsPolicyController(
    private val play: PolicyPlayService,
    private val matcherDsl: MatcherDsl = MatcherDsl.create(),
) {
    @GetMapping("/capabilities")
    @Operation(
        tags = ["catalog"],
        summary = "Policy playground capability probe",
        description = "Reports which engines and archive features are on the classpath; use it to " +
            "hide unsupported UI affordances rather than probing endpoints for failures.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Available engines and playground features",
        content = [Content(schema = Schema(implementation = PolicyCapabilities::class))],
    )
    fun capabilities(): PolicyCapabilities = play.capabilities()

    @GetMapping("/export")
    @Operation(
        tags = ["catalog"],
        summary = "Export full policy catalog as REPLACE seed YAML",
        description = "Round-trippable dump of categories, policies, and suites; importing it replaces " +
            "the catalog.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Seed YAML attachment",
            content = [Content(mediaType = "application/x-yaml", schema = Schema(type = "string"))],
        ),
        ApiResponse(responseCode = "400", description = "Unsupported format"),
    )
    fun exportCatalog(
        @Parameter(description = "Payload format; only `seeds` is supported")
        @RequestParam(defaultValue = "seeds") format: String,
    ): ResponseEntity<String> {
        if (format != "seeds") {
            return ResponseEntity.badRequest().body("Only format=seeds is supported")
        }
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"catalog-seeds.yaml\"")
            .header("Content-Type", "application/x-yaml")
            .body(play.exportCatalogSeeds())
    }

    @GetMapping("/categories")
    @Operation(tags = ["catalog"], summary = "List policy categories")
    @ApiResponse(
        responseCode = "200",
        description = "All categories",
        content = [Content(array = ArraySchema(schema = Schema(implementation = Category::class)))],
    )
    fun listCategories(): List<Category> = play.listCategories()

    @GetMapping("/categories/{id}")
    @Operation(tags = ["catalog"], summary = "Get a policy category by id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Category",
            content = [Content(schema = Schema(implementation = Category::class))],
        ),
        ApiResponse(responseCode = "404", description = "Category not found"),
    )
    fun getCategory(
        @Parameter(description = "Category id") @PathVariable id: UUID,
    ): ResponseEntity<Category> =
        play.getCategory(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping("/categories")
    @Operation(tags = ["catalog"], summary = "Create a policy category")
    @ApiResponse(
        responseCode = "200",
        description = "Created category",
        content = [Content(schema = Schema(implementation = Category::class))],
    )
    fun createCategory(@RequestBody write: CategoryWrite): Category = play.createCategory(write)

    @PutMapping("/categories/{id}")
    @Operation(tags = ["catalog"], summary = "Update a policy category")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Updated category",
            content = [Content(schema = Schema(implementation = Category::class))],
        ),
        ApiResponse(responseCode = "404", description = "Category not found"),
    )
    fun updateCategory(
        @Parameter(description = "Category id") @PathVariable id: UUID,
        @RequestBody write: CategoryWrite,
    ): ResponseEntity<Category> =
        play.updateCategory(id, write)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @DeleteMapping("/categories/{id}")
    @Operation(
        tags = ["catalog"],
        summary = "Delete a policy category (rejected while in use)",
        description = "Reassign or delete the policies in the category first.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "404", description = "Category not found"),
        ApiResponse(responseCode = "409", description = "Category still referenced by policies"),
    )
    fun deleteCategory(
        @Parameter(description = "Category id") @PathVariable id: UUID,
    ): ResponseEntity<Any> =
        try {
            if (play.deleteCategory(id)) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (ex: CategoryInUseException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(ex.message)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @GetMapping("/policies")
    @Operation(
        tags = ["catalog"],
        summary = "List / query policies in the playground repository",
        description = "All filters are ANDed; omit every filter to list the whole catalog.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Matching policies",
        content = [Content(array = ArraySchema(schema = Schema(implementation = Policy::class)))],
    )
    fun list(
        @Parameter(description = "Restrict to policies in this category")
        @RequestParam(required = false) categoryId: UUID?,
        @Parameter(description = "Repeatable: policy must carry every given tag")
        @RequestParam(required = false) tag: List<String>?,
        @Parameter(description = "Case-sensitive substring match on policy name")
        @RequestParam(required = false) name: String?,
        @Parameter(description = "Case-sensitive substring match on policy key")
        @RequestParam(required = false) key: String?,
        @Parameter(description = "Repeatable `key=value` annotation filter; entries without `=` are ignored")
        @RequestParam(required = false) annotation: List<String>?,
    ): List<Policy> {
        val annotations = linkedMapOf<String, String>()
        for (raw in annotation.orEmpty()) {
            val i = raw.indexOf('=')
            if (i > 0) annotations[raw.substring(0, i)] = raw.substring(i + 1)
        }
        return play.list(
            PolicyQuery(
                categoryId = categoryId,
                tags = tag.orEmpty(),
                annotations = annotations,
                nameContains = name,
                keyContains = key,
            ),
        )
    }

    @GetMapping("/policies/{id}")
    @Operation(tags = ["catalog"], summary = "Get a policy by id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Policy",
            content = [Content(schema = Schema(implementation = Policy::class))],
        ),
        ApiResponse(responseCode = "404", description = "Policy not found"),
    )
    fun get(
        @Parameter(description = "Policy id") @PathVariable id: UUID,
    ): ResponseEntity<Policy> =
        play.get(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping("/policies")
    @Operation(
        tags = ["catalog"],
        summary = "Create a policy",
        description = "`engineKind` defaults to DROOLS and `applicabilityKind` to ALWAYS_APPLY when blank.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Created policy",
            content = [Content(schema = Schema(implementation = Policy::class))],
        ),
        ApiResponse(responseCode = "400", description = "Invalid policy write (message body)"),
    )
    fun create(@RequestBody write: PolicyWrite): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(
                play.create(
                    write.copy(
                        engineKind = write.engineKind.ifBlank { PolicyEngineKinds.DROOLS },
                        applicabilityKind = write.applicabilityKind ?: ApplicabilityKinds.ALWAYS_APPLY,
                    ),
                ),
            )
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @PutMapping("/policies/{id}")
    @Operation(tags = ["catalog"], summary = "Update a policy")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Updated policy",
            content = [Content(schema = Schema(implementation = Policy::class))],
        ),
        ApiResponse(responseCode = "400", description = "Invalid policy write (message body)"),
        ApiResponse(responseCode = "404", description = "Policy not found"),
    )
    fun update(
        @Parameter(description = "Policy id") @PathVariable id: UUID,
        @RequestBody write: PolicyWrite,
    ): ResponseEntity<Any> =
        try {
            play.update(id, write)?.let { ResponseEntity.ok(it) }
                ?: ResponseEntity.notFound().build()
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @DeleteMapping("/policies/{id}")
    @Operation(tags = ["catalog"], summary = "Delete a policy")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "404", description = "Policy not found"),
    )
    fun delete(
        @Parameter(description = "Policy id") @PathVariable id: UUID,
    ): ResponseEntity<Void> =
        if (play.delete(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    @PostMapping("/check")
    @Operation(
        tags = ["evaluate"],
        summary = "Compile/validate a DROOLS policy body",
        description = "Always 200: compilation problems come back inside the result rather than as an " +
            "error status. Nothing is stored or executed.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Compilation outcome with any diagnostics",
        content = [Content(schema = Schema(implementation = PolicyCheckResult::class))],
    )
    fun check(@RequestBody request: PolicyCheckRequest): PolicyCheckResult =
        play.check(
            body = request.body,
            engineKind = request.engineKind ?: PolicyEngineKinds.DROOLS,
        )

    @PostMapping("/evaluate")
    @Operation(
        tags = ["evaluate"],
        summary = "Evaluate a policy against a matcher-selected graph fragment",
        description = "Runs a stored policy (`policyId`) or an inline `body`. The matcher selects the " +
            "input fragment the same way as graph query; nothing is archived — use " +
            "POST /policy/evaluations/suite to save a suite run.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Evaluation result",
            content = [
                Content(
                    schema = Schema(implementation = org.poc.objs.policy.api.EvaluationResult::class),
                ),
            ],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher, unresolved fragment, or bad policy reference",
            content = [
                Content(
                    schema = Schema(implementation = org.poc.objs.api.validation.ValidationResult::class),
                ),
            ],
        ),
        ApiResponse(responseCode = "404", description = "Graph or graph version not found"),
    )
    fun evaluate(@RequestBody request: PolicyEvaluateRequest): ResponseEntity<Any> =
        try {
            val matcherNode =
                request.matcher
                    ?: tools.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("all", true)
            val matcher = matcherDsl.decodeNode(matcherNode, "$.matcher")
            ResponseEntity.ok(
                play.evaluate(
                    matcher = matcher,
                    graphId = request.graphId?.let(UUID::fromString),
                    graphVersion = request.graphVersion,
                    policyId = request.policyId?.let(UUID::fromString),
                    body = request.body,
                    engineKind = request.engineKind,
                    policyName = request.policyName,
                ),
            )
        } catch (ex: ValidationException) {
            ResponseEntity.badRequest().body(ex.result)
        } catch (ex: GraphMaterializationException) {
            ResponseEntity.badRequest().body(ex.message)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        } catch (ex: GraphException) {
            val status =
                if (ex.code == "GRAPH_VERSION_NOT_FOUND" || ex.code == "GRAPH_NOT_FOUND") {
                    HttpStatus.NOT_FOUND
                } else {
                    HttpStatus.BAD_REQUEST
                }
            ResponseEntity.status(status).body(ex.message)
        }

    @GetMapping("/suites")
    @Operation(tags = ["suites"], summary = "List policy suites")
    @ApiResponse(
        responseCode = "200",
        description = "All suites with their folder trees",
        content = [Content(array = ArraySchema(schema = Schema(implementation = PolicySuiteDto::class)))],
    )
    fun listSuites(): List<PolicySuiteDto> = play.listSuites().map { SuiteHttpMapping.toDto(it) }

    @GetMapping("/suites/{id}")
    @Operation(tags = ["suites"], summary = "Get a policy suite by id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Suite with its folder tree",
            content = [Content(schema = Schema(implementation = PolicySuiteDto::class))],
        ),
        ApiResponse(responseCode = "404", description = "Suite not found"),
    )
    fun getSuite(
        @Parameter(description = "Suite id") @PathVariable id: UUID,
    ): ResponseEntity<PolicySuiteDto> =
        play.getSuite(id)?.let { ResponseEntity.ok(SuiteHttpMapping.toDto(it)) }
            ?: ResponseEntity.notFound().build()

    @PostMapping("/suites")
    @Operation(
        tags = ["suites"],
        summary = "Create a policy suite",
        description = "The whole folder tree is written in one call; folder ids are assigned by the store.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Created suite",
            content = [Content(schema = Schema(implementation = PolicySuiteDto::class))],
        ),
        ApiResponse(responseCode = "400", description = "Unknown matcher kind, mode, or participation"),
    )
    fun createSuite(@RequestBody dto: PolicySuiteDto): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(SuiteHttpMapping.toDto(play.createSuite(SuiteHttpMapping.toWrite(dto))))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @PutMapping("/suites/{id}")
    @Operation(
        tags = ["suites"],
        summary = "Update a policy suite",
        description = "Full replace of the suite and its folder tree; folders absent from the body are removed.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Updated suite",
            content = [Content(schema = Schema(implementation = PolicySuiteDto::class))],
        ),
        ApiResponse(responseCode = "400", description = "Unknown matcher kind, mode, or participation"),
        ApiResponse(responseCode = "404", description = "Suite not found"),
    )
    fun updateSuite(
        @Parameter(description = "Suite id") @PathVariable id: UUID,
        @RequestBody dto: PolicySuiteDto,
    ): ResponseEntity<Any> =
        try {
            play.updateSuite(id, SuiteHttpMapping.toWrite(dto))
                ?.let { ResponseEntity.ok(SuiteHttpMapping.toDto(it)) }
                ?: ResponseEntity.notFound().build()
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @DeleteMapping("/suites/{id}")
    @Operation(
        tags = ["suites"],
        summary = "Delete a policy suite",
        description = "Removes the suite and its folders; the referenced policies are kept.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "404", description = "Suite not found"),
    )
    fun deleteSuite(
        @Parameter(description = "Suite id") @PathVariable id: UUID,
    ): ResponseEntity<Void> =
        if (play.deleteSuite(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    @PostMapping("/suites/selection")
    @Operation(
        tags = ["suites"],
        summary = "Preview effective policy selection for a suite scope",
        description = "Resolves matchers without evaluating anything; `missing` lists pinned policies " +
            "that no longer resolve.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Envelope with `policies`, `placementByPolicyId`, and `missing`",
        ),
        ApiResponse(responseCode = "400", description = "Unknown scope, or missing folderId/folderIds/policyIds"),
    )
    fun suiteSelection(@RequestBody request: SuiteSelectionRequest): ResponseEntity<Any> =
        try {
            val scope = SuiteHttpMapping.parseScope(
                request.scope,
                request.folderId,
                request.folderIds,
                request.policyIds,
            )
            val set = play.suiteSelection(request.suiteId, scope)
            ResponseEntity.ok(
                mapOf(
                    "policies" to set.policies,
                    "placementByPolicyId" to set.placementByPolicyId,
                    "missing" to set.missing,
                ),
            )
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @PostMapping("/suites/evaluate")
    @Operation(
        tags = ["suites"],
        summary = "Evaluate a suite against a matcher-selected graph fragment",
        description = "Runs the selection resolved by POST /policy/suites/selection. The result is not " +
            "archived; POST it to /policy/evaluations/suite to save it.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Suite evaluation result",
            content = [
                Content(
                    schema = Schema(implementation = org.poc.objs.policy.api.SuiteEvaluationResult::class),
                ),
            ],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher, unresolved fragment, or bad scope",
            content = [
                Content(
                    schema = Schema(implementation = org.poc.objs.api.validation.ValidationResult::class),
                ),
            ],
        ),
        ApiResponse(responseCode = "404", description = "Graph or graph version not found"),
    )
    fun evaluateSuite(@RequestBody request: SuiteEvaluateRequest): ResponseEntity<Any> =
        try {
            val matcherNode =
                request.matcher
                    ?: tools.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("all", true)
            val matcher = matcherDsl.decodeNode(matcherNode, "$.matcher")
            val scope = SuiteHttpMapping.parseScope(
                request.scope,
                request.folderId,
                request.folderIds,
                request.policyIds,
            )
            ResponseEntity.ok(
                play.evaluateSuite(
                    matcher = matcher,
                    graphId = request.graphId?.let(UUID::fromString),
                    graphVersion = request.graphVersion,
                    suiteId = request.suiteId,
                    scope = scope,
                ),
            )
        } catch (ex: ValidationException) {
            ResponseEntity.badRequest().body(ex.result)
        } catch (ex: GraphMaterializationException) {
            ResponseEntity.badRequest().body(ex.message)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        } catch (ex: GraphException) {
            val status =
                if (ex.code == "GRAPH_VERSION_NOT_FOUND" || ex.code == "GRAPH_NOT_FOUND") {
                    HttpStatus.NOT_FOUND
                } else {
                    HttpStatus.BAD_REQUEST
                }
            ResponseEntity.status(status).body(ex.message)
        }

    @PostMapping("/evaluations/suite")
    @Operation(
        tags = ["suites"],
        summary = "Persist a suite evaluation archive (explicit save)",
        description = "Content axes come from `axes` when given, otherwise from `presetName` " +
            "(STANDARD default). When the input axis is on, the fragment is rematerialized from " +
            "`matcher` + graph scope. Requires an evaluation archive on the classpath.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Archive id",
            content = [Content(schema = Schema(implementation = PersistEvaluationResponse::class))],
        ),
        ApiResponse(responseCode = "400", description = "No durable axes selected, or invalid matcher/scope"),
        ApiResponse(responseCode = "503", description = "No evaluation archive configured"),
    )
    fun persistSuiteEvaluation(@RequestBody request: PersistSuiteEvaluationRequest): ResponseEntity<Any> {
        return try {
            val axesDto = request.axes
            val baseSpec =
                when {
                    axesDto != null ->
                        PersistSpec(
                            axes =
                                org.poc.objs.policy.api.PersistContentAxes(
                                    results = axesDto.results,
                                    executionContext = axesDto.executionContext,
                                    input = axesDto.input,
                                ),
                            presetName = request.presetName,
                        )
                    request.presetName.equals(PersistPresets.FULL, ignoreCase = true) ->
                        PersistPresets.full()
                    request.presetName.equals(PersistPresets.EPHEMERAL, ignoreCase = true) ->
                        PersistPresets.ephemeral()
                    else -> PersistPresets.standard()
                }
            val spec =
                baseSpec.copy(
                    name = request.name,
                    description = request.description,
                    tags = request.tags,
                    annotations = request.annotations,
                    evaluationId = request.result.meta.evaluationId,
                    presetName = request.presetName ?: baseSpec.presetName,
                )
            if (!spec.axes.anyDurable) {
                return ResponseEntity.badRequest().body("No durable content axes selected (EPHEMERAL)")
            }
            val executionContext =
                if (spec.axes.executionContext) {
                    buildSuiteExecutionContext(request.result)
                } else {
                    null
                }
            val input =
                if (spec.axes.input) {
                    val matcherNode =
                        request.matcher
                            ?: tools.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("all", true)
                    val matcher = matcherDsl.decodeNode(matcherNode, "$.matcher")
                    play.resolveFragment(
                        matcher = matcher,
                        graphId = request.graphId?.let(UUID::fromString),
                        graphVersion = request.graphVersion,
                    )
                } else {
                    null
                }
            val id =
                play.saveSuiteEvaluation(
                    result = request.result,
                    spec = spec,
                    executionContext = executionContext,
                    input = input,
                ) ?: return ResponseEntity.badRequest().body("Archive write returned no id")
            ResponseEntity.ok(PersistEvaluationResponse(evaluationId = id))
        } catch (ex: IllegalStateException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.message)
        } catch (ex: ValidationException) {
            ResponseEntity.badRequest().body(ex.result)
        } catch (ex: GraphMaterializationException) {
            ResponseEntity.badRequest().body(ex.message)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        } catch (ex: GraphException) {
            ResponseEntity.badRequest().body(ex.message)
        }
    }

    @GetMapping("/evaluations")
    @Operation(tags = ["archives"], summary = "List evaluation archive summaries (newest first)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Archive summaries",
            content = [Content(schema = Schema(implementation = EvaluationArchiveListResponse::class))],
        ),
        ApiResponse(responseCode = "503", description = "No evaluation archive configured"),
    )
    fun listEvaluations(
        @Parameter(description = "Page size; defaults to the archive list limit")
        @RequestParam(required = false) limit: Int?,
        @Parameter(description = "Row offset for paging; defaults to 0")
        @RequestParam(required = false) offset: Int?,
        @Parameter(description = "Filter by archive kind, e.g. the suite evaluation kind")
        @RequestParam(required = false) kind: String?,
        @Parameter(description = "Filter by a single archive tag")
        @RequestParam(required = false) tag: String?,
    ): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(
                EvaluationArchiveListResponse(
                    items =
                        play.listEvaluations(
                            limit = limit ?: org.poc.objs.policy.api.EvaluationArchive.LIST_LIMIT_DEFAULT,
                            offset = offset ?: 0,
                            kind = kind,
                            tag = tag,
                        ),
                ),
            )
        } catch (ex: IllegalStateException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.message)
        }

    @GetMapping("/evaluations/{id}")
    @Operation(
        tags = ["archives"],
        summary = "Load an evaluation archive (full or STANDARD view)",
        description = "`view=standard` drops the stored input fragment and returns results plus " +
            "execution context only.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Archive document",
            content = [
                Content(
                    schema = Schema(
                        implementation = org.poc.objs.policy.api.EvaluationArchiveDocument::class,
                    ),
                ),
            ],
        ),
        ApiResponse(responseCode = "400", description = "Unknown `view` value"),
        ApiResponse(responseCode = "404", description = "Archive not found"),
        ApiResponse(responseCode = "503", description = "No evaluation archive configured"),
    )
    fun getEvaluation(
        @Parameter(description = "Archive id") @PathVariable id: UUID,
        @Parameter(description = "Omit for the full document, or pass `standard` for the STANDARD view")
        @RequestParam(required = false) view: String?,
    ): ResponseEntity<Any> {
        val standardView =
            when {
                view == null || view.isBlank() -> false
                view.equals("standard", ignoreCase = true) -> true
                else -> return ResponseEntity.badRequest().body("Unknown view='$view' (use standard or omit)")
            }
        return try {
            play.loadEvaluation(id, standardView)?.let { ResponseEntity.ok(it) }
                ?: ResponseEntity.notFound().build()
        } catch (ex: IllegalStateException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.message)
        }
    }

    @DeleteMapping("/evaluations/{id}")
    @Operation(tags = ["archives"], summary = "Delete an evaluation archive")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "404", description = "Archive not found"),
        ApiResponse(responseCode = "503", description = "No evaluation archive configured"),
    )
    fun deleteEvaluation(
        @Parameter(description = "Archive id") @PathVariable id: UUID,
    ): ResponseEntity<Any> =
        try {
            if (play.deleteEvaluation(id)) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (ex: IllegalStateException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.message)
        }

    private fun buildSuiteExecutionContext(
        result: org.poc.objs.policy.api.SuiteEvaluationResult,
    ): Map<String, Any?> =
        mapOf(
            "source" to "workbench",
            "kind" to result.meta.kind,
            "suiteId" to result.meta.suiteId?.toString(),
            "suiteName" to result.meta.suiteName,
            "executionStrategyKind" to result.meta.executionStrategyKind,
            "rollUpStrategyKind" to result.meta.rollUpStrategyKind,
            "policies" to
                result.outcomes.map { o ->
                    mapOf(
                        "name" to o.policyName,
                        "serial" to o.policySerial,
                        "engineKind" to o.engineKind,
                        "status" to o.status.name,
                    )
                },
        )
}
