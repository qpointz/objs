package org.poc.objs.policy.api

import java.util.UUID

/**
 * Persist / load policy suites (C-27). Split from [PolicyRepository].
 */
interface SuiteRepository {
    fun save(write: PolicySuiteWrite): PolicySuite

    fun update(id: UUID, write: PolicySuiteWrite): PolicySuite?

    fun delete(id: UUID): Boolean

    fun findById(id: UUID): PolicySuite?

    fun list(): List<PolicySuite>
}

/** Thrown when suite folder tree is invalid (cycles, multi-root, orphans, blank keys). */
class InvalidSuiteException(
    message: String,
) : IllegalArgumentException(message)
