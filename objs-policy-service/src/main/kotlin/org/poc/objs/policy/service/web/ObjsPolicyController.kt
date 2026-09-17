package org.poc.objs.policy.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "policy")
class ObjsPolicyController(
    private val play: PolicyPlayService,
    private val matcherDsl: MatcherDsl = MatcherDsl.create(),
) {
    @GetMapping("/capabilities")
    @Operation(summary = "Policy playground capability probe")
    fun capabilities(): PolicyCapabilities = play.capabilities()

    @GetMapping("/export")
    @Operation(summary = "Export full policy catalog as REPLACE seed YAML")
    fun exportCatalog(
        @RequestParam(defaultValue = "seeds") format: String,
    ): ResponseEntity<String> {
        if (format != "seeds") {
            return ResponseEntity.badRequest().body("Only format=seeds is supported")
        }
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"policy-catalog-seeds.yaml\"")
            .header("Content-Type", "application/x-yaml")
            .body(play.exportCatalogSeeds())
    }

    @GetMapping("/categories")
    @Operation(summary = "List policy categories")
    fun listCategories(): List<Category> = play.listCategories()

    @GetMapping("/categories/{id}")
    fun getCategory(@PathVariable id: UUID): ResponseEntity<Category> =
        play.getCategory(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping("/categories")
    fun createCategory(@RequestBody write: CategoryWrite): Category = play.createCategory(write)

    @PutMapping("/categories/{id}")
    fun updateCategory(
        @PathVariable id: UUID,
        @RequestBody write: CategoryWrite,
    ): ResponseEntity<Category> =
        play.updateCategory(id, write)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @DeleteMapping("/categories/{id}")
    fun deleteCategory(@PathVariable id: UUID): ResponseEntity<Any> =
        try {
            if (play.deleteCategory(id)) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (ex: CategoryInUseException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(ex.message)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @GetMapping("/policies")
    @Operation(summary = "List / query policies in the playground repository")
    fun list(
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) tag: List<String>?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) key: String?,
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
    fun get(@PathVariable id: UUID): ResponseEntity<Policy> =
        play.get(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping("/policies")
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
    fun update(@PathVariable id: UUID, @RequestBody write: PolicyWrite): ResponseEntity<Any> =
        try {
            play.update(id, write)?.let { ResponseEntity.ok(it) }
                ?: ResponseEntity.notFound().build()
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @DeleteMapping("/policies/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> =
        if (play.delete(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    @PostMapping("/check")
    @Operation(summary = "Compile/validate a DROOLS policy body")
    fun check(@RequestBody request: PolicyCheckRequest): PolicyCheckResult =
        play.check(
            body = request.body,
            engineKind = request.engineKind ?: PolicyEngineKinds.DROOLS,
        )

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate a policy against a matcher-selected graph fragment")
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
    @Operation(summary = "List policy suites")
    fun listSuites(): List<PolicySuiteDto> = play.listSuites().map { SuiteHttpMapping.toDto(it) }

    @GetMapping("/suites/{id}")
    fun getSuite(@PathVariable id: UUID): ResponseEntity<PolicySuiteDto> =
        play.getSuite(id)?.let { ResponseEntity.ok(SuiteHttpMapping.toDto(it)) }
            ?: ResponseEntity.notFound().build()

    @PostMapping("/suites")
    fun createSuite(@RequestBody dto: PolicySuiteDto): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(SuiteHttpMapping.toDto(play.createSuite(SuiteHttpMapping.toWrite(dto))))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @PutMapping("/suites/{id}")
    fun updateSuite(@PathVariable id: UUID, @RequestBody dto: PolicySuiteDto): ResponseEntity<Any> =
        try {
            play.updateSuite(id, SuiteHttpMapping.toWrite(dto))
                ?.let { ResponseEntity.ok(SuiteHttpMapping.toDto(it)) }
                ?: ResponseEntity.notFound().build()
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }

    @DeleteMapping("/suites/{id}")
    fun deleteSuite(@PathVariable id: UUID): ResponseEntity<Void> =
        if (play.deleteSuite(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    @PostMapping("/suites/selection")
    @Operation(summary = "Preview effective policy selection for a suite scope")
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
    @Operation(summary = "Evaluate a suite against a matcher-selected graph fragment")
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
    @Operation(summary = "Persist a suite evaluation archive (explicit save)")
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
    @Operation(summary = "List evaluation archive summaries (newest first)")
    fun listEvaluations(
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) offset: Int?,
        @RequestParam(required = false) kind: String?,
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
    @Operation(summary = "Load an evaluation archive (full or STANDARD view)")
    fun getEvaluation(
        @PathVariable id: UUID,
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
    @Operation(summary = "Delete an evaluation archive")
    fun deleteEvaluation(@PathVariable id: UUID): ResponseEntity<Any> =
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
