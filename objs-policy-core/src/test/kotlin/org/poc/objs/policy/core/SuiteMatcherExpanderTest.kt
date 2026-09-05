package org.poc.objs.policy.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.MetadataMatcher
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.StaticPolicyEntry
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteMatcherMode
import org.poc.objs.policy.api.SuiteSelectionException
import org.poc.objs.policy.core.PolicyTestFixtures.storesWithCategory
import org.poc.objs.policy.core.PolicyTestFixtures.write
import java.util.UUID

class SuiteMatcherExpanderTest {

    @Test
    fun shouldExpandStaticListAndMetadata() {
        val (stores, categoryId) = storesWithCategory(slug = "general")
        val p1 = stores.policies.save(write("alpha", "PASS", categoryId, tags = listOf("baseline")))
        val p2 = stores.policies.save(write("beta", "FAIL", categoryId, tags = listOf("other")))
        val rootId = UUID.randomUUID()
        val suite = PolicySuite(
            id = UUID.randomUUID(),
            name = "S",
            folders = listOf(
                SuiteFolder(
                    id = rootId,
                    key = "root",
                    name = "Root",
                    matchers = listOf(
                        StaticListMatcher(entries = listOf(StaticPolicyEntry(p1.id))),
                        MetadataMatcher(categorySlug = "general", tags = listOf("other")),
                    ),
                ),
            ),
        )
        val expander = SuiteMatcherExpander(stores.policies, stores.categories)
        val set = expander.effectivePolicies(suite, rootId)
        assertThat(set.policies.map { it.id }).containsExactly(p1.id, p2.id)
    }

    @Test
    fun shouldErrorOnMissingStaticListEntry() {
        val (stores, _) = storesWithCategory()
        val rootId = UUID.randomUUID()
        val missingId = UUID.randomUUID()
        val suite = PolicySuite(
            id = UUID.randomUUID(),
            name = "S",
            folders = listOf(
                SuiteFolder(
                    id = rootId,
                    key = "root",
                    name = "Root",
                    matchers = listOf(
                        StaticListMatcher(entries = listOf(StaticPolicyEntry(missingId))),
                    ),
                ),
            ),
        )
        val expander = SuiteMatcherExpander(stores.policies, stores.categories)
        assertThatThrownBy { expander.effectivePolicies(suite, rootId) }
            .isInstanceOf(SuiteSelectionException::class.java)
    }

    @Test
    fun shouldSkipDisabledSubtree() {
        val (stores, categoryId) = storesWithCategory()
        val p = stores.policies.save(write("alpha", "PASS", categoryId))
        val rootId = UUID.randomUUID()
        val childId = UUID.randomUUID()
        val suite = PolicySuite(
            id = UUID.randomUUID(),
            name = "S",
            folders = listOf(
                SuiteFolder(id = rootId, key = "root", name = "Root"),
                SuiteFolder(
                    id = childId,
                    key = "off",
                    name = "Off",
                    parentId = rootId,
                    participation = SuiteFolderParticipation.DISABLED,
                    matchers = listOf(
                        StaticListMatcher(entries = listOf(StaticPolicyEntry(p.id))),
                    ),
                ),
            ),
        )
        val expander = SuiteMatcherExpander(stores.policies, stores.categories)
        assertThat(expander.effectivePolicies(suite, rootId).policies).isEmpty()
    }

    @Test
    fun shouldExcludeViaMatcherMode() {
        val (stores, categoryId) = storesWithCategory()
        val p1 = stores.policies.save(write("alpha", "PASS", categoryId, tags = listOf("a")))
        val p2 = stores.policies.save(write("beta", "PASS", categoryId, tags = listOf("b")))
        val rootId = UUID.randomUUID()
        val suite = PolicySuite(
            id = UUID.randomUUID(),
            name = "S",
            folders = listOf(
                SuiteFolder(
                    id = rootId,
                    key = "root",
                    name = "Root",
                    matchers = listOf(
                        StaticListMatcher(
                            entries = listOf(
                                StaticPolicyEntry(p1.id),
                                StaticPolicyEntry(p2.id),
                            ),
                        ),
                        StaticListMatcher(
                            mode = SuiteMatcherMode.EXCLUDE,
                            entries = listOf(StaticPolicyEntry(p2.id)),
                        ),
                    ),
                ),
            ),
        )
        val expander = SuiteMatcherExpander(stores.policies, stores.categories)
        assertThat(expander.effectivePolicies(suite, rootId).policies.map { it.id })
            .containsExactly(p1.id)
    }
}
