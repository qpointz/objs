package org.poc.objs.core.persistence.policy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.poc.objs.core.persistence.ObjsPersistenceFixture
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.InvalidSuiteException
import org.poc.objs.policy.api.MetadataMatcher
import org.poc.objs.policy.api.PolicySuiteWrite
import org.poc.objs.policy.api.StaticListMatcher
import org.poc.objs.policy.api.StaticPolicyEntry
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteFolderWrite
import org.poc.objs.policy.api.SuiteMatcherMode
import java.util.UUID

class JpaSuiteRepositoryTest : ObjsPersistenceFixture() {

    @Test
    fun shouldSaveSuiteWithFolderTreeAndMatchers() {
        val rootId = UUID.randomUUID()
        val childId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        val saved = policySuites.save(
            PolicySuiteWrite(
                key = "baseline",
                name = "Baseline",
                folders = listOf(
                    SuiteFolderWrite(
                        id = rootId,
                        key = "root",
                        name = "Root",
                        matchers = listOf(
                            StaticListMatcher(
                                entries = listOf(StaticPolicyEntry(policyId, "1.0")),
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
                                categoryKey = "general",
                                tags = listOf("baseline"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertThat(saved.id).isNotNull()
        assertThat(saved.folders).hasSize(2)

        val reloaded = policySuites.findById(saved.id)
        assertThat(reloaded).isEqualTo(saved)
        assertThat(policySuites.findByKey("baseline")).isEqualTo(saved)
        assertThat(policySuites.list()).containsExactly(saved)

        val root = reloaded!!.folders.single { it.key == "root" }
        val staticMatcher = root.matchers.single() as StaticListMatcher
        assertThat(staticMatcher.entries.single().policyId).isEqualTo(policyId)
        assertThat(staticMatcher.entries.single().version).isEqualTo("1.0")

        val child = reloaded.folders.single { it.key == "child" }
        val metadataMatcher = child.matchers.single() as MetadataMatcher
        assertThat(metadataMatcher.categoryKey).isEqualTo("general")
        assertThat(child.participation).isEqualTo(SuiteFolderParticipation.IGNORED)
        assertThat(child.severityConfig).isEqualTo("HIGH")
    }

    @Test
    fun shouldRejectCyclesAndMultiRoot() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        assertThatThrownBy {
            policySuites.save(
                PolicySuiteWrite(
                    key = "bad",
                    name = "bad",
                    folders = listOf(
                        SuiteFolderWrite(id = a, key = "a", name = "A", parentId = b),
                        SuiteFolderWrite(id = b, key = "b", name = "B", parentId = a),
                    ),
                ),
            )
        }.isInstanceOf(InvalidSuiteException::class.java)

        assertThatThrownBy {
            policySuites.save(
                PolicySuiteWrite(
                    key = "multi",
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
    fun shouldRejectDuplicateKeys() {
        policySuites.save(
            PolicySuiteWrite(key = "s", name = "S", folders = listOf(SuiteFolderWrite(key = "r", name = "Root"))),
        )
        assertThatThrownBy {
            policySuites.save(
                PolicySuiteWrite(
                    key = "s",
                    name = "S2",
                    folders = listOf(SuiteFolderWrite(key = "r", name = "Root")),
                ),
            )
        }.isInstanceOf(InvalidSuiteException::class.java)
            .hasMessageContaining("already exists")
    }

    @Test
    fun shouldUpdateReplacingFolderTree_andDelete() {
        val saved = policySuites.save(
            PolicySuiteWrite(key = "s", name = "S", folders = listOf(SuiteFolderWrite(key = "r", name = "Root"))),
        )
        val updated = policySuites.update(
            saved.id,
            PolicySuiteWrite(
                key = "s",
                name = "S2",
                folders = listOf(
                    SuiteFolderWrite(key = "r2", name = "Root 2"),
                ),
            ),
        )
        assertThat(updated?.name).isEqualTo("S2")
        assertThat(updated?.folders).extracting<String> { it.key }.containsExactly("r2")

        assertThat(policySuites.delete(saved.id)).isTrue()
        assertThat(policySuites.findById(saved.id)).isNull()
        assertThat(policySuites.delete(saved.id)).isFalse()
    }
}
