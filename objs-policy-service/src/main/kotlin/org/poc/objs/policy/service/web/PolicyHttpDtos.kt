package org.poc.objs.policy.service.web

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.JsonNode

@Schema(description = "Compile/validate a policy body without storing or running it")
data class PolicyCheckRequest(
    @field:Schema(description = "Policy source, e.g. a DRL rule unit for the DROOLS engine")
    val body: String,
    @field:Schema(description = "Engine that compiles the body; defaults to DROOLS")
    val engineKind: String? = null,
)

@Schema(
    description = "Evaluate one policy against a matcher-selected graph fragment. Provide either " +
        "`policyId` (stored policy) or an inline `body`.",
)
data class PolicyEvaluateRequest(
    @field:Schema(description = "Matcher DSL document selecting the input fragment; defaults to `{ \"all\": true }`")
    val matcher: JsonNode? = null,
    @field:Schema(description = "Graph to scope selection to; omit to select across graphs")
    val graphId: String? = null,
    @field:Schema(description = "Deep graph version pin; requires `graphId`")
    val graphVersion: Long? = null,
    @field:Schema(description = "Stored policy to run; omit when supplying an inline `body`")
    val policyId: String? = null,
    @field:Schema(description = "Inline policy source to run instead of a stored policy")
    val body: String? = null,
    @field:Schema(description = "Engine used for an inline `body`; defaults to DROOLS")
    val engineKind: String? = null,
    @field:Schema(description = "Display name recorded for an inline policy run")
    val policyName: String? = null,
)
