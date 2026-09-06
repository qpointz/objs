package org.poc.objs.core.persistence.policy

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA row for [org.poc.objs.policy.api.Category] (C-28 WI-002). [key] is the human-managed
 * logical identity (G-P36seed); [name] is a separate display label.
 */
@Entity(name = "PolicyCategoryRecord")
@Table(name = "objs_policy_category")
class PolicyCategoryRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "`key`", nullable = false)
    var key: String = "",

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
