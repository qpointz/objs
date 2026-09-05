package org.poc.objs.policy.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.CategoryInUseException
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyQuery
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.core.PolicyTestFixtures.storesWithCategory
import org.poc.objs.policy.core.PolicyTestFixtures.write

class InMemoryPolicyRepositoryTest {

    @Test
    fun shouldAllocateTimestampSerialVersions_onSave() {
        val (stores, categoryId) = storesWithCategory()
        val repo = stores.policies
        val v1 = repo.save(write("gate", "PASS", categoryId))
        val v2 = repo.save(write("gate", "FAIL", categoryId))

        assertThat(v1.serial).isGreaterThan(0L)
        assertThat(v2.serial).isGreaterThan(v1.serial)
        assertThat(v1.id).isNotEqualTo(v2.id)
        assertThat(v1.version).isEqualTo("0.1")
        assertThat(v2.version).isEqualTo("0.1")
        assertThat(repo.resolve(PolicyRef.ByName("gate"))).isEqualTo(v2)
        assertThat(repo.resolve(PolicyRef.ByName("gate", serial = v1.serial))).isEqualTo(v1)
        assertThat(repo.resolve(PolicyRef.ById(v1.id))).isEqualTo(v1)
    }

    @Test
    fun shouldUpdateAndDeleteById() {
        val (stores, categoryId) = storesWithCategory()
        val repo = stores.policies
        val saved = repo.save(write("gate", "PASS", categoryId))
        val updated = repo.update(
            saved.id,
            write("gate", "FAIL", categoryId),
        )
        assertThat(updated?.body).isEqualTo("FAIL")
        assertThat(updated?.id).isEqualTo(saved.id)
        assertThat(updated?.serial).isGreaterThan(saved.serial)
        assertThat(repo.delete(saved.id)).isTrue()
        assertThat(repo.findById(saved.id)).isNull()
    }

    @Test
    fun shouldNormalizeTags_andRequireCategory() {
        val (stores, categoryId) = storesWithCategory()
        val repo = stores.policies
        val saved = repo.save(
            PolicyWrite(
                name = "gate",
                engineKind = PolicyEngineKinds.CUSTOM,
                body = "PASS",
                categoryId = categoryId,
                tags = listOf("  Foo ", "foo", "BAR"),
            ),
        )
        assertThat(saved.tags).containsExactly("foo", "bar")

        assertThatThrownBy {
            repo.save(
                PolicyWrite(
                    name = "x",
                    engineKind = PolicyEngineKinds.CUSTOM,
                    body = "PASS",
                    categoryId = categoryId,
                    tags = emptyList(),
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("non-empty")
    }

    @Test
    fun shouldQueryByCategoryTagsAnnotationsAndName() {
        val (stores, catA) = storesWithCategory("A", "alpha")
        val catB = stores.categories.save(CategoryWrite("B", "beta")).id
        val repo = stores.policies
        repo.save(
            write("mongo-gate", "PASS", catA, tags = listOf("db", "mongo")).copy(
                annotations = mapOf("pack" to "gov"),
            ),
        )
        repo.save(write("api-gate", "PASS", catB, tags = listOf("api")))

        assertThat(repo.query(PolicyQuery(categoryId = catA))).hasSize(1)
        assertThat(repo.query(PolicyQuery(tags = listOf("db")))).hasSize(1)
        assertThat(repo.query(PolicyQuery(annotations = mapOf("pack" to "gov")))).hasSize(1)
        assertThat(repo.query(PolicyQuery(nameContains = "MONGO"))).hasSize(1)
        assertThat(repo.query(PolicyQuery(nameContains = "nope"))).isEmpty()
    }

    @Test
    fun shouldRefuseCategoryDelete_whenReferenced() {
        val (stores, categoryId) = storesWithCategory()
        stores.policies.save(write("gate", "PASS", categoryId))

        assertThatThrownBy { stores.categories.delete(categoryId) }
            .isInstanceOf(CategoryInUseException::class.java)

        stores.policies.delete(stores.policies.list().single().id)
        assertThat(stores.categories.delete(categoryId)).isTrue()
    }

    @Test
    fun shouldValidateCategorySlug() {
        val stores = InMemoryPolicyStores()
        assertThatThrownBy {
            stores.categories.save(CategoryWrite("Bad", "Bad-Slug"))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("lowercase")
    }
}
