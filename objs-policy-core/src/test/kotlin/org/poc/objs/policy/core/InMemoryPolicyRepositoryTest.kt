package org.poc.objs.policy.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyWrite

class InMemoryPolicyRepositoryTest {

    @Test
    fun shouldAllocateSerialVersions_onSave() {
        val repo = InMemoryPolicyRepository()
        val v1 = repo.save(write("gate", "PASS"))
        val v2 = repo.save(write("gate", "FAIL"))

        assertThat(v1.version).isEqualTo(1L)
        assertThat(v2.version).isEqualTo(2L)
        assertThat(v1.id).isNotEqualTo(v2.id)
        assertThat(repo.resolve(PolicyRef.ByName("gate"))).isEqualTo(v2)
        assertThat(repo.resolve(PolicyRef.ByName("gate", version = 1L))).isEqualTo(v1)
        assertThat(repo.resolve(PolicyRef.ById(v1.id))).isEqualTo(v1)
    }

    @Test
    fun shouldUpdateAndDeleteById() {
        val repo = InMemoryPolicyRepository()
        val saved = repo.save(write("gate", "PASS"))
        val updated = repo.update(
            saved.id,
            PolicyWrite(name = "gate", engineKind = PolicyEngineKinds.CUSTOM, body = "FAIL"),
        )
        assertThat(updated?.body).isEqualTo("FAIL")
        assertThat(updated?.id).isEqualTo(saved.id)
        assertThat(repo.delete(saved.id)).isTrue()
        assertThat(repo.findById(saved.id)).isNull()
    }

    private fun write(name: String, body: String) = PolicyWrite(
        name = name,
        engineKind = PolicyEngineKinds.CUSTOM,
        body = body,
    )
}
