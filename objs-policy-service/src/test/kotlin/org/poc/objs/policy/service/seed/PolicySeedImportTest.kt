package org.poc.objs.policy.service.seed

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.api.seed.SeedImportException
import org.poc.objs.core.persistence.tx.PassthroughUnitOfWork
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.policy.core.InMemoryPolicyStores

/**
 * WI-003 — policy catalog [org.poc.objs.api.seed.SeedDocumentHandler] import behaviour
 * (G-P35seed…G-P40seed), against [InMemoryPolicyStores] + a [PassthroughUnitOfWork] (no
 * Spring / DB needed — mirrors `SeedImporterTest` in `:objs-persistence`).
 */
class PolicySeedImportTest {
    private lateinit var stores: InMemoryPolicyStores
    private lateinit var importer: SeedImporter

    @BeforeEach
    fun setUp() {
        stores = InMemoryPolicyStores()
        val handlers = listOf(
            CategorySeedHandler(stores.categories, stores.policies),
            PolicySeedHandler(stores.policies, stores.categories, SpringSeedBodyLoader()),
            PolicySuiteSeedHandler(stores.suites, stores.policies),
            DropCategorySeedHandler(stores.categories, stores.policies),
            DropPolicySeedHandler(stores.policies),
            DropPolicySuiteSeedHandler(stores.suites),
        )
        importer = SeedImporter(handlers, PassthroughUnitOfWork())
    }

    @Test
    fun shouldImportCategoryAndPolicy_apply() {
        val result = importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: Category
            key: security
            name: Security
            ---
            apiVersion: objs.poc.org/v1
            kind: Policy
            key: gate-1
            name: Gate One
            categoryKey: security
            engineKind: CUSTOM
            body: "always pass"
            tags: [gate]
            version: "0.1"
            """.trimIndent(),
        )

        assertThat(result.isSuccess).isTrue()
        val category = stores.categories.findByKey("security")
        assertThat(category).isNotNull
        val revisions = stores.policies.findByKey("gate-1")
        assertThat(revisions).hasSize(1)
        assertThat(revisions.single().name).isEqualTo("Gate One")
        assertThat(revisions.single().body).isEqualTo("always pass")
        assertThat(revisions.single().categoryId).isEqualTo(category!!.id)
        assertThat(revisions.single().tags).containsExactly("gate")
    }

    @Test
    fun shouldSaveNewRevision_onPolicyApplyReimport() {
        importCategoryAndPolicy("security", "gate-1", body = "v1")
        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: Policy
            key: gate-1
            categoryKey: security
            engineKind: CUSTOM
            body: "v2"
            tags: [gate]
            """.trimIndent(),
        )

        val revisions = stores.policies.findByKey("gate-1")
        assertThat(revisions).hasSize(2)
        assertThat(revisions.last().body).isEqualTo("v2")
    }

    @Test
    fun shouldDeletePreviousRevisions_onPolicyReplace() {
        importCategoryAndPolicy("security", "gate-1", body = "v1")
        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: Policy
            mode: replace
            key: gate-1
            categoryKey: security
            engineKind: CUSTOM
            body: "v2"
            tags: [gate]
            """.trimIndent(),
        )

        val revisions = stores.policies.findByKey("gate-1")
        assertThat(revisions).hasSize(1)
        assertThat(revisions.single().body).isEqualTo("v2")
    }

    @Test
    fun shouldWipePolicies_onCategoryReplace() {
        importCategoryAndPolicy("security", "gate-1")
        assertThat(stores.policies.findByKey("gate-1")).hasSize(1)

        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: Category
            mode: replace
            key: security
            name: Security
            """.trimIndent(),
        )

        assertThat(stores.policies.findByKey("gate-1")).isEmpty()
        val category = stores.categories.findByKey("security")
        assertThat(category).isNotNull
    }

    @Test
    fun shouldKeepPolicies_onCategoryApplyMerge() {
        importCategoryAndPolicy("security", "gate-1")

        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: Category
            key: security
            name: Security (renamed)
            """.trimIndent(),
        )

        assertThat(stores.policies.findByKey("gate-1")).hasSize(1)
        assertThat(stores.categories.findByKey("security")!!.name).isEqualTo("Security (renamed)")
    }

    @Test
    fun shouldDropAllRevisions_onDropPolicy() {
        importCategoryAndPolicy("security", "gate-1", body = "v1")
        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: Policy
            key: gate-1
            categoryKey: security
            engineKind: CUSTOM
            body: "v2"
            tags: [gate]
            """.trimIndent(),
        )
        assertThat(stores.policies.findByKey("gate-1")).hasSize(2)

        val result = importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: DropPolicy
            key: gate-1
            """.trimIndent(),
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(stores.policies.findByKey("gate-1")).isEmpty()
    }

    @Test
    fun shouldCascadeWipePoliciesThenDropCategory() {
        importCategoryAndPolicy("security", "gate-1")

        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: DropCategory
            key: security
            """.trimIndent(),
        )

        assertThat(stores.policies.findByKey("gate-1")).isEmpty()
        assertThat(stores.categories.findByKey("security")).isNull()
    }

    @Test
    fun shouldFailWholeResource_onDanglingCategoryRef() {
        assertThatThrownBy {
            importer.importYaml(
                """
                apiVersion: objs.poc.org/v1
                kind: Policy
                key: gate-1
                categoryKey: unknown-category
                engineKind: CUSTOM
                body: "always pass"
                tags: [gate]
                """.trimIndent(),
            )
        }.isInstanceOf(SeedImportException::class.java)

        assertThat(stores.policies.findByKey("gate-1")).isEmpty()
    }

    @Test
    fun shouldFailWholeResource_onDanglingPolicyKeyInSuiteMatcher() {
        assertThatThrownBy {
            importer.importYaml(
                """
                apiVersion: objs.poc.org/v1
                kind: PolicySuite
                key: my-suite
                folders:
                  - key: root
                    matchers:
                      - kind: STATIC_LIST
                        entries:
                          - policyKey: unknown-policy
                """.trimIndent(),
            )
        }.isInstanceOf(SeedImportException::class.java)

        assertThat(stores.suites.findByKey("my-suite")).isNull()
    }

    @Test
    fun shouldLoadBodyFromClasspathRef() {
        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: Category
            key: security
            name: Security
            ---
            apiVersion: objs.poc.org/v1
            kind: Policy
            key: gate-1
            categoryKey: security
            engineKind: CUSTOM
            bodyRef: classpath:seeds/gate-1-body.txt
            tags: [gate]
            """.trimIndent(),
        )

        val policy = stores.policies.findByKey("gate-1").single()
        assertThat(policy.body.trim()).isEqualTo("rule body loaded from classpath")
    }

    @Test
    fun shouldUpsertSuiteInPlace_onApply() {
        importCategoryAndPolicy("security", "gate-1")
        val policyId = stores.policies.findByKey("gate-1").single().id

        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: PolicySuite
            key: my-suite
            name: My Suite
            folders:
              - key: root
                name: Root
                matchers:
                  - kind: STATIC_LIST
                    entries:
                      - policyKey: gate-1
              - key: child
                name: Child
                parentKey: root
                matchers:
                  - kind: METADATA
                    categoryKey: security
            """.trimIndent(),
        )

        val firstId = stores.suites.findByKey("my-suite")!!.id
        val firstSuite = stores.suites.findByKey("my-suite")!!
        assertThat(firstSuite.folders).hasSize(2)
        val rootFolder = firstSuite.folders.single { it.key == "root" }
        val matcher = rootFolder.matchers.single() as org.poc.objs.policy.api.StaticListMatcher
        assertThat(matcher.entries.single().policyId).isEqualTo(policyId)

        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: PolicySuite
            key: my-suite
            name: My Suite Renamed
            folders:
              - key: root
                name: Root
            """.trimIndent(),
        )

        val updated = stores.suites.findByKey("my-suite")!!
        assertThat(updated.id).isEqualTo(firstId)
        assertThat(updated.name).isEqualTo("My Suite Renamed")
        assertThat(updated.folders).hasSize(1)
    }

    @Test
    fun shouldReplaceSuite_deletingThenSaving() {
        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: PolicySuite
            key: my-suite
            name: My Suite
            folders:
              - key: root
                name: Root
            """.trimIndent(),
        )
        val firstId = stores.suites.findByKey("my-suite")!!.id

        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: PolicySuite
            mode: replace
            key: my-suite
            name: My Suite
            folders:
              - key: root
                name: Root
            """.trimIndent(),
        )
        val secondId = stores.suites.findByKey("my-suite")!!.id

        assertThat(secondId).isNotEqualTo(firstId)
    }

    @Test
    fun shouldDropPolicySuite() {
        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: PolicySuite
            key: my-suite
            name: My Suite
            folders:
              - key: root
                name: Root
            """.trimIndent(),
        )
        assertThat(stores.suites.findByKey("my-suite")).isNotNull

        val result = importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: DropPolicySuite
            key: my-suite
            """.trimIndent(),
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(stores.suites.findByKey("my-suite")).isNull()
    }

    @Test
    fun shouldSkip_whenDroppingUnknownKeys() {
        val result = importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: DropPolicy
            key: never-existed
            ---
            apiVersion: objs.poc.org/v1
            kind: DropCategory
            key: never-existed
            ---
            apiVersion: objs.poc.org/v1
            kind: DropPolicySuite
            key: never-existed
            """.trimIndent(),
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.documents).allSatisfy { assertThat(it.skipped).isTrue() }
    }

    private fun importCategoryAndPolicy(categoryKey: String, policyKey: String, body: String = "always pass") {
        importer.importYaml(
            """
            apiVersion: objs.poc.org/v1
            kind: Category
            key: $categoryKey
            name: ${categoryKey.replaceFirstChar { it.uppercase() }}
            ---
            apiVersion: objs.poc.org/v1
            kind: Policy
            key: $policyKey
            categoryKey: $categoryKey
            engineKind: CUSTOM
            body: "$body"
            tags: [gate]
            """.trimIndent(),
        )
    }
}
