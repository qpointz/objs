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
}
