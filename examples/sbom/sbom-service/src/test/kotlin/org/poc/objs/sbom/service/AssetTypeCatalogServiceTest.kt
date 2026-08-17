package org.poc.objs.sbom.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(AssetTypeCatalogService::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-types;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class AssetTypeCatalogServiceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var schemas: BoMSchemaCatalog

    @Autowired
    lateinit var catalog: AssetTypeCatalogService

    @BeforeEach
    fun seedSchema() {
        schemas.clear()
        schemas.register(
            BoMSchema(
                type = "Component",
                version = "1.0.0",
                usage = BoMSchemaUsage.ENTITY,
                contentSchema = BoMSchemaDsl.obj(
                    "Component",
                    "Software component",
                    listOf(
                        BoMSchemaDsl.field(
                            "name",
                            BoMSchemaDsl.string("Name", "Component name"),
                            identifier = true,
                            searchable = true,
                        ),
                        BoMSchemaDsl.field(
                            "version",
                            BoMSchemaDsl.string("Version", "Component version"),
                            searchable = true,
                        ),
                        BoMSchemaDsl.field(
                            "kind",
                            BoMSchemaDsl.string("Kind", "library/framework"),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun shouldListEntityTypesFromCatalog() {
        val types = catalog.listEntityTypes()
        assertThat(types).hasSize(1)
        assertThat(types[0].type).isEqualTo("Component")
        assertThat(types[0].title).isEqualTo("Component")
        assertThat(types[0].description).isEqualTo("Software component")
    }

    @Test
    fun shouldExposeSearchableAndIdentifierFieldsOnly() {
        val detail = catalog.getEntityType("Component")
        assertThat(detail).isNotNull
        assertThat(detail!!.searchableFields.map { it.path }).containsExactlyInAnyOrder("name", "version")
        assertThat(detail.identifierFields.map { it.path }).containsExactly("name")
        assertThat(detail.searchableFields.none { it.path == "kind" }).isTrue()
        assertThat(detail.searchableFields.associate { it.path to it.title })
            .containsEntry("name", "Name")
            .containsEntry("version", "Version")
        assertThat(detail.firstLevelScalarFields.map { it.path }).containsExactly("name", "version", "kind")
    }

    @Test
    fun shouldFallBackToFieldName_whenSchemaTitleIsGenericScalar() {
        schemas.clear()
        schemas.register(
            BoMSchema(
                type = "Thing",
                version = "1.0.0",
                usage = BoMSchemaUsage.ENTITY,
                contentSchema = BoMSchemaDsl.obj(
                    "Thing",
                    "Thing payload",
                    listOf(
                        BoMSchemaDsl.field(
                            "purl",
                            BoMSchemaDsl.string("Text", "Text value"),
                            searchable = true,
                        ),
                    ),
                ),
            ),
        )
        val detail = catalog.getEntityType("Thing")
        assertThat(detail!!.searchableFields.single().title).isEqualTo("purl")
    }
}
