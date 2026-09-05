package org.poc.objs.policy.api

import java.util.UUID

/**
 * Persist / load / resolve policy revisions. S1 ships an in-memory impl in `:objs-policy-core`.
 */
interface PolicyRepository {
    /** Create: allocates a **new** [Policy.serial]. */
    fun save(write: PolicyWrite): Policy

    /**
     * Replace body/metadata for an existing policy row (same [Policy.id]).
     * Allocates a new timestamp [Policy.serial] (object head-version rule).
     * Playground Save uses this so the UI selection stays stable by id.
     */
    fun update(id: UUID, write: PolicyWrite): Policy?

    /** Remove one revision by id. */
    fun delete(id: UUID): Boolean

    fun resolve(ref: PolicyRef): Policy?

    fun findById(id: UUID): Policy?

    fun findByName(name: String): List<Policy>

    fun list(): List<Policy>

    /** Filtered list (C-32). No paging. */
    fun query(query: PolicyQuery): List<Policy>
}
