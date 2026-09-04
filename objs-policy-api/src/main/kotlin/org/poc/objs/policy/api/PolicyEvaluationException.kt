package org.poc.objs.policy.api

/** Thrown when evaluation is refused (e.g. fragment resolve ERROR diagnostics). */
class PolicyEvaluationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
