package org.poc.objs.policy.api

/**
 * SPI for **PolicyContextWiring**: write facts into [PolicyEvaluationContext] after fragment
 * resolve and before applicability / engine evaluation.
 *
 * Not an Enricher — topology must not be rewritten; no product predicates in foundation.
 */
fun interface PolicyContextWirer {
    fun wire(context: PolicyEvaluationContext)
}
