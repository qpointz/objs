package org.poc.objs.policy.api

import java.util.UUID

/** How to resolve a stored [Policy] revision for evaluation. */
sealed class PolicyRef {
    data class ById(val id: UUID) : PolicyRef()

    /**
     * Resolve by logical [name]. When [serial] is null, resolve **latest** serial for that name.
     */
    data class ByName(val name: String, val serial: Long? = null) : PolicyRef()
}
