package org.poc.objs.policy.core

import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyWrite
import java.util.UUID

/** Shared fixtures for policy repo tests after C-32 metadata. */
internal object PolicyTestFixtures {
    fun storesWithCategory(
        displayName: String = "General",
        slug: String = "general",
    ): Pair<InMemoryPolicyStores, UUID> {
        val stores = InMemoryPolicyStores()
        val cat = stores.categories.save(CategoryWrite(displayName = displayName, slug = slug))
        return stores to cat.id
    }

    fun write(
        name: String,
        body: String,
        categoryId: UUID,
        engineKind: String = PolicyEngineKinds.CUSTOM,
        tags: List<String> = listOf("test"),
        version: String = "0.1",
    ) = PolicyWrite(
        name = name,
        engineKind = engineKind,
        body = body,
        categoryId = categoryId,
        tags = tags,
        version = version,
    )
}
