package org.poc.objs.policy.api

import org.poc.objs.api.domain.ResolvedGraphFragment

/**
 * Evaluation input after fragment resolve: normalized fragment plus a mutable sidecar fact bag.
 *
 * [PolicyContextWirer] implementations only write into [facts] (PolicyContextWiring).
 */
class PolicyEvaluationContext(
    val fragment: ResolvedGraphFragment,
    val facts: MutableMap<String, Any?> = mutableMapOf(),
)
