package org.poc.objs.core.persistence.policy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.poc.objs.core.persistence.ObjsPersistenceFixture
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyQuery
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyWrite
import java.util.UUID

class JpaPolicyRepositoryTest : ObjsPersistenceFixture() {

    private fun categoryId(key: String = "general"): UUID =
        policyCategories.save(CategoryWrite(name = key, key = key)).id

    private fun write(key: String, body: String, categoryId: UUID, tags: List<String> = listOf("fixture")) =
        PolicyWrite(
            key = key,
            name = key,
            engineKind = PolicyEngineKinds.CUSTOM,
            body = body,
            categoryId = categoryId,
            tags = tags,
        )

    @Test
    fun shouldAllocateTimestampSerialVersions_onSave() {
        val categoryId = categoryId()
        val v1 = policies.save(write("gate", "PASS", categoryId))
        val v2 = policies.save(write("gate", "FAIL", categoryId))

        assertThat(v1.serial).isGreaterThan(0L)
        assertThat(v2.serial).isGreaterThan(v1.serial)
        assertThat(v1.id).isNotEqualTo(v2.id)
        assertThat(policies.resolve(PolicyRef.ByKey("gate"))).isEqualTo(v2)
        assertThat(policies.resolve(PolicyRef.ByKey("gate", serial = v1.serial))).isEqualTo(v1)
        assertThat(policies.resolve(PolicyRef.ById(v1.id))).isEqualTo(v1)
    }

    @Test
    fun shouldUpdateAndDeleteById() {
        val categoryId = categoryId()
        val saved = policies.save(write("gate", "PASS", categoryId))
        val updated = policies.update(saved.id, write("gate", "FAIL", categoryId))

        assertThat(updated?.body).isEqualTo("FAIL")
        assertThat(updated?.id).isEqualTo(saved.id)
        assertThat(updated?.serial).isGreaterThan(saved.serial)
        assertThat(policies.delete(saved.id)).isTrue()
        assertThat(policies.findById(saved.id)).isNull()
    }

    @Test
    fun shouldNormalizeTags_andRequireCategory() {
        val categoryId = categoryId()
        val saved = policies.save(
            PolicyWrite(
                key = "gate",
                name = "gate",
                engineKind = PolicyEngineKinds.CUSTOM,
                body = "PASS",
                categoryId = categoryId,
                tags = listOf("  Foo ", "foo", "BAR"),
            ),
        )
        assertThat(saved.tags).containsExactly("foo", "bar")

        assertThatThrownBy {
            policies.save(write("x", "PASS", categoryId, tags = emptyList()))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("non-empty")

        assertThatThrownBy {
            policies.save(write("y", "PASS", UUID.randomUUID()))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown categoryId")
    }

    @Test
    fun shouldQueryByCategoryTagsAnnotationsKeyAndName() {
        val catA = categoryId("alpha")
        val catB = categoryId("beta")
        policies.save(
            write("mongo-gate", "PASS", catA, tags = listOf("db", "mongo")).copy(
                annotations = mapOf("pack" to "gov"),
            ),
        )
        policies.save(write("api-gate", "PASS", catB, tags = listOf("api")))

        assertThat(policies.query(PolicyQuery(categoryId = catA))).hasSize(1)
        assertThat(policies.query(PolicyQuery(tags = listOf("db")))).hasSize(1)
        assertThat(policies.query(PolicyQuery(annotations = mapOf("pack" to "gov")))).hasSize(1)
        assertThat(policies.query(PolicyQuery(nameContains = "MONGO"))).hasSize(1)
        assertThat(policies.query(PolicyQuery(keyContains = "api"))).hasSize(1)
        assertThat(policies.query(PolicyQuery(nameContains = "nope"))).isEmpty()
    }

    @Test
    fun shouldFindByKey_acrossMultipleSerials() {
        val categoryId = categoryId()
        val v1 = policies.save(write("gate", "PASS", categoryId))
        val v2 = policies.save(write("gate", "FAIL", categoryId))

        val history = policies.findByKey("gate")
        assertThat(history).extracting<UUID> { it.id }.containsExactly(v1.id, v2.id)
    }
}
