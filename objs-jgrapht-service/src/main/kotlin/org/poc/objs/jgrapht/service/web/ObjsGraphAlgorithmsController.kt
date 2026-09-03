package org.poc.objs.jgrapht.service.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.api.domain.GraphException
import org.poc.objs.api.match.MatcherDsl
import org.poc.objs.api.validation.ValidationException
import org.poc.objs.api.validation.ValidationResult
import org.poc.objs.jgrapht.core.GraphAlgorithmCapabilities
import org.poc.objs.jgrapht.core.GraphCycleAnalysis
import org.poc.objs.jgrapht.service.GraphAlgorithmService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/objs")
@Tag(name = "graph-algorithms")
class ObjsGraphAlgorithmsController(
    private val algorithms: GraphAlgorithmService,
    private val matcherDsl: MatcherDsl = MatcherDsl.create(),
) {
    @GetMapping("/graph/algorithms/capabilities")
    @Operation(summary = "List supported graph analysis algorithms and materialization modes")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Supported algorithms",
            content = [Content(schema = Schema(implementation = GraphAlgorithmCapabilities::class))],
        ),
    )
    fun capabilities(): GraphAlgorithmCapabilities = algorithms.capabilities()

    @PostMapping("/graph/algorithms/cycles")
    @Operation(
        summary = "Analyze directed cycle regions for a matcher-selected graph fragment",
        description = "Selects entities and edges using the same matcher DSL as graph query, resolves the " +
            "fragment through GraphFragmentPolicy, then runs directed SCC cycle-region analysis.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Cycle analysis result",
            content = [Content(schema = Schema(implementation = GraphCycleAnalysis::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid matcher, materialization mode, or unresolved fragment",
            content = [Content(schema = Schema(implementation = ValidationResult::class))],
        ),
    )
    fun cycles(@RequestBody body: GraphCycleAnalysisRequest): ResponseEntity<Any> =
        try {
            val matcher = matcherDsl.decodeNode(body.matcher, "$.matcher")
            ResponseEntity.ok(
                algorithms.analyzeCycles(
                    matcher = matcher,
                    graphId = body.graphId,
                    graphVersion = body.graphVersion,
                    algorithm = body.algorithm,
                    materialization = body.materialization,
                ),
            )
        } catch (ex: ValidationException) {
            ResponseEntity.badRequest().body(ex.result)
        } catch (ex: GraphException) {
            val status =
                if (ex.code == "GRAPH_VERSION_NOT_FOUND" || ex.code == "GRAPH_NOT_FOUND") {
                    HttpStatus.NOT_FOUND
                } else {
                    HttpStatus.BAD_REQUEST
                }
            ResponseEntity.status(status).body(mapOf("error" to (ex.message ?: ex.code), "code" to ex.code))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (ex.message ?: "bad request")))
        }
}
