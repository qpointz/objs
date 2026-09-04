package org.poc.objs.policy.service.web

import tools.jackson.databind.JsonNode

data class PolicyCheckRequest(
    val body: String,
    val engineKind: String? = null,
)

data class PolicyEvaluateRequest(
    val matcher: JsonNode? = null,
    val graphId: String? = null,
    val graphVersion: Long? = null,
    val policyId: String? = null,
    val body: String? = null,
    val engineKind: String? = null,
    val policyName: String? = null,
)
