package org.poc.objs.policy.api

import java.util.UUID

/**
 * Persist / load / resolve policy revisions. S1 ships an in-memory impl in `:objs-policy-core`.
 */
interface PolicyRepository {
    /** Create or update-by-name: allocates a **new** serial [Policy.version]. */
    fun save(write: PolicyWrite): Policy

    /**
     * Replace body/metadata for an existing revision (same [Policy.id] + [Policy.version]).
     * Playground Save uses this so the UI selection stays stable.
     */
    fun update(id: UUID, write: PolicyWrite): Policy?

    /** Remove one revision by id. */
    fun delete(id: UUID): Boolean

    fun resolve(ref: PolicyRef): Policy?

    fun findById(id: UUID): Policy?

    fun findByName(name: String): List<Policy>

    fun list(): List<Policy>
}
