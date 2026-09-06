package org.poc.objs.core.persistence.policy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.poc.objs.core.persistence.ObjsPersistenceFixture
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.CategoryInUseException
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyWrite

class JpaCategoryRepositoryTest : ObjsPersistenceFixture() {

    @Test
    fun shouldCreateUpdateAndListByKey() {
        val saved = policyCategories.save(CategoryWrite(name = "General", key = "general"))
        assertThat(saved.id).isNotNull()
        assertThat(saved.key).isEqualTo("general")

        assertThat(policyCategories.findByKey("general")).isEqualTo(saved)
        assertThat(policyCategories.findById(saved.id)).isEqualTo(saved)
        assertThat(policyCategories.list()).containsExactly(saved)

        val updated = policyCategories.update(saved.id, CategoryWrite(name = "General 2", key = "general2"))
        assertThat(updated?.name).isEqualTo("General 2")
        assertThat(updated?.key).isEqualTo("general2")
        assertThat(policyCategories.findByKey("general")).isNull()
        assertThat(policyCategories.findByKey("general2")).isEqualTo(updated)
    }

    @Test
    fun shouldRejectDuplicateKeys() {
        policyCategories.save(CategoryWrite(name = "General", key = "general"))
        assertThatThrownBy {
            policyCategories.save(CategoryWrite(name = "Other", key = "general"))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("already exists")
    }

    @Test
    fun shouldValidateKeyCharset() {
        assertThatThrownBy {
            policyCategories.save(CategoryWrite(name = "Bad", key = "Bad-Key"))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("lowercase")
    }

    @Test
    fun shouldRefuseDelete_whenReferencedByPolicy() {
        val category = policyCategories.save(CategoryWrite(name = "General", key = "general"))
        policies.save(
            PolicyWrite(
                key = "gate",
                name = "gate",
                engineKind = PolicyEngineKinds.CUSTOM,
                body = "PASS",
                categoryId = category.id,
                tags = listOf("fixture"),
            ),
        )

        assertThatThrownBy { policyCategories.delete(category.id) }
            .isInstanceOf(CategoryInUseException::class.java)

        policies.delete(policies.list().single().id)
        assertThat(policyCategories.delete(category.id)).isTrue()
        assertThat(policyCategories.findById(category.id)).isNull()
    }

    @Test
    fun shouldReturnFalse_whenDeletingUnknownId() {
        assertThat(policyCategories.delete(java.util.UUID.randomUUID())).isFalse()
    }
}
