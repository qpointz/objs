package org.poc.objs.policy.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.InvalidSuiteException
import org.poc.objs.policy.api.MetadataMatcher
import org.poc.objs.policy.api.PolicySuiteWrite
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.StaticPolicyEntry
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteFolderWrite
import org.poc.objs.policy.api.SuiteMatcherMode
import org.poc.objs.policy.api.SuiteRollUpStrategyKinds
import java.util.UUID

class InMemorySuiteRepositoryTest {

    private val repo = InMemorySuiteRepository()

    @Test
    fun shouldSaveSuiteWithFolderTree() {
        val rootId = UUID.randomUUID()
        val childId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        val saved = repo.save(
            PolicySuiteWrite(
                name = "Baseline",
                rollUpStrategyKind = SuiteRollUpStrategyKinds.BUILTIN,
                folders = listOf(
                    SuiteFolderWrite(
                        id = rootId,
                        key = "root",
                        name = "Root",
                        matchers = listOf(
                            StaticListMatcher(
                                entries = listOf(StaticPolicyEntry(policyId)),
                            ),
                        ),
                        tags = listOf("suite-tag"),
                        annotations = mapOf("owner" to "sec"),
                    ),
                    SuiteFolderWrite(
                        id = childId,
                        key = "child",
                        name = "Child",
                        parentId = rootId,
                        participation = SuiteFolderParticipation.IGNORED,
                        severityConfig = "HIGH",
                        matchers = listOf(
                            MetadataMatcher(
                                mode = SuiteMatcherMode.INCLUDE,
                                categorySlug = "general",
                                tags = listOf("baseline"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertThat(saved.id).isNotNull()
        assertThat(saved.folders).hasSize(2)
        assertThat(repo.findById(saved.id)).isEqualTo(saved)
        assertThat(repo.list()).containsExactly(saved)
    }

    @Test
    fun shouldRejectCyclesAndMultiRoot() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        assertThatThrownBy {
            repo.save(
                PolicySuiteWrite(
                    name = "bad",
                    folders = listOf(
                        SuiteFolderWrite(id = a, key = "a", name = "A", parentId = b),
                        SuiteFolderWrite(id = b, key = "b", name = "B", parentId = a),
                    ),
                ),
            )
        }.isInstanceOf(InvalidSuiteException::class.java)

        assertThatThrownBy {
            repo.save(
                PolicySuiteWrite(
                    name = "multi",
                    folders = listOf(
                        SuiteFolderWrite(key = "r1", name = "R1"),
                        SuiteFolderWrite(key = "r2", name = "R2"),
                    ),
                ),
            )
        }.isInstanceOf(InvalidSuiteException::class.java)
    }

    @Test
    fun shouldUpdateAndDelete() {
        val saved = repo.save(
            PolicySuiteWrite(
                name = "S",
                folders = listOf(SuiteFolderWrite(key = "r", name = "Root")),
            ),
        )
        val updated = repo.update(
            saved.id,
            PolicySuiteWrite(
                name = "S2",
                folders = listOf(SuiteFolderWrite(key = "r", name = "Root")),
            ),
        )
        assertThat(updated?.name).isEqualTo("S2")
        assertThat(repo.delete(saved.id)).isTrue()
        assertThat(repo.findById(saved.id)).isNull()
    }
}
