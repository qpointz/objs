package org.poc.objs.policy.service.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.persistence.tx.PassthroughUnitOfWork
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.policy.api.CategoryWrite
import org.poc.objs.policy.api.PolicyEngineKinds
import org.poc.objs.policy.api.PolicyWrite
import org.poc.objs.policy.core.InMemoryPolicyStores

class PolicyCatalogExportTest {
    @Test
    fun shouldExportReplaceYaml_andReimport() {
        val stores = InMemoryPolicyStores()
        val cat = stores.categories.save(CategoryWrite(name = "General", key = "general"))
        stores.policies.save(
            PolicyWrite(
                key = "gate",
                name = "Gate",
                engineKind = PolicyEngineKinds.DROOLS,
                body = "package x\n",
                categoryId = cat.id,
                tags = listOf("t"),
            ),
        )
        val exporter = PolicyCatalogSeedExporter(stores.categories, stores.policies, stores.suites)
        val yaml = exporter.exportReplaceYaml()
        assertThat(yaml).contains("Category")
        assertThat(yaml).contains("replace")
        assertThat(yaml).contains("general")
        assertThat(yaml).contains("Policy")
        assertThat(yaml).contains("gate")
        assertThat(yaml).contains("body")

        val stores2 = InMemoryPolicyStores()
        val importer = SeedImporter(
            listOf(
                CategorySeedHandler(stores2.categories, stores2.policies),
                PolicySeedHandler(stores2.policies, stores2.categories, SpringSeedBodyLoader()),
                PolicySuiteSeedHandler(stores2.suites, stores2.policies),
            ),
            PassthroughUnitOfWork(),
        )
        assertThat(importer.importYaml(yaml).isSuccess).isTrue()
        assertThat(stores2.categories.findByKey("general")).isNotNull
        assertThat(stores2.policies.findByKey("gate")).isNotEmpty
    }
}
