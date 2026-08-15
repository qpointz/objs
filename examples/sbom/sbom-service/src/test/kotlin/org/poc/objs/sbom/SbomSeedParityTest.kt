package org.poc.objs.sbom

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.InMemoryBoMAllowedEdgeCatalog
import org.poc.objs.core.domain.InMemoryBoMSchemaCatalog
import org.poc.objs.core.seed.AllowedEdgeRuleSeedHandler
import org.poc.objs.core.seed.ObjectSchemaSeedHandler
import org.poc.objs.core.seed.SEED_KIND_ALLOWED_EDGE_RULE
import org.poc.objs.core.seed.SEED_KIND_OBJECT_SCHEMA
import org.poc.objs.core.seed.SeedImporter
import org.poc.objs.sbom.registry.SbomRegistry
import org.springframework.core.io.ClassPathResource

class SbomSeedParityTest {
    @Test
    fun shouldMatchTypedPack_whenOntologyYamlImported() {
        val expectedSchemas = InMemoryBoMSchemaCatalog()
        val expectedRules = InMemoryBoMAllowedEdgeCatalog()
        SbomRegistry.pack().registerInto(expectedSchemas, expectedRules)

        val schemas = InMemoryBoMSchemaCatalog()
        val rules = InMemoryBoMAllowedEdgeCatalog()
        val importer = SeedImporter(
            listOf(
                ObjectSchemaSeedHandler(schemas),
                AllowedEdgeRuleSeedHandler(rules),
            ),
        )
        val yaml = ClassPathResource("seeds/sbom-ontology.yaml").inputStream
            .bufferedReader()
            .readText()
        val result = importer.importYaml(yaml)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.appliedByKind()[SEED_KIND_OBJECT_SCHEMA]).isEqualTo(24)
        assertThat(result.appliedByKind()[SEED_KIND_ALLOWED_EDGE_RULE]).isEqualTo(28)

        assertThat(schemas.all().map { it.type to it.version }.toSet())
            .isEqualTo(expectedSchemas.all().map { it.type to it.version }.toSet())
        for (expected in expectedSchemas.all()) {
            val actual = schemas.get(expected.type, expected.version)
            assertThat(actual).isNotNull
            assertThat(actual!!.usage).isEqualTo(expected.usage)
            assertThat(actual.contentSchema).isEqualTo(expected.contentSchema)
        }

        assertThat(rules.all().map { Triple(it.sourceType, it.role, it.targetType) }.toSet())
            .isEqualTo(expectedRules.all().map { Triple(it.sourceType, it.role, it.targetType) }.toSet())
        for (expected in expectedRules.all()) {
            val actual = rules.find(expected.sourceType, expected.role, expected.targetType)
            assertThat(actual).isEqualTo(expected)
        }
    }
}
