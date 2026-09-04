package org.poc.objs.policy.api

import java.util.UUID

/**
 * Persist / load / resolve policy revisions. S1 ships an in-memory impl in `:objs-policy-core`.
 */
interface PolicyRepository {
    /** Create or update-by-name: allocates a **new** serial [Policy.version]. */
    fun save(write: PolicyWrite): Policy

    fun resolve(ref: PolicyRef): Policy?

    fun findById(id: UUID): Policy?

    fun findByName(name: String): List<Policy>

    fun list(): List<Policy>
}
