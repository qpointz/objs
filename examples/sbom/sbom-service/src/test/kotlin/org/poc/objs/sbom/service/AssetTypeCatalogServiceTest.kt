package org.poc.objs.sbom.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Schema
import org.poc.objs.api.domain.SchemaCatalog
import org.poc.objs.api.domain.SchemaDsl
import org.poc.objs.api.domain.SchemaUsage
import org.poc.objs.autoconfigure.ObjsCoreAutoConfiguration
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
    lateinit var schemas: SchemaCatalog

    @Autowired
    lateinit var catalog: AssetTypeCatalogService

    @BeforeEach
    fun seedSchema() {
        schemas.clear()
        schemas.register(
            Schema(
                type = "Component",
                version = "1.0.0",
                usage = SchemaUsage.ENTITY,
                contentSchema = SchemaDsl.obj(
                    "Component",
                    "Software component",
                    listOf(
                        SchemaDsl.field(
                            "name",
                            SchemaDsl.string("Name", "Component name"),
                            identifier = true,
                            searchable = true,
                        ),
                        SchemaDsl.field(
                            "version",
                            SchemaDsl.string("Version", "Component version"),
                            searchable = true,
                        ),
                        SchemaDsl.field(
                            "kind",
                            SchemaDsl.string("Kind", "library/framework"),
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
    fun shouldPreferHighestSchemaVersion_whenTypeHasMultipleVersions() {
        schemas.register(
            Schema(
                type = "Component",
                version = "1.2.0",
                usage = SchemaUsage.ENTITY,
                contentSchema = SchemaDsl.obj("Component 1.2", "older patch line"),
            ),
        )
        schemas.register(
            Schema(
                type = "Component",
                version = "2.0.0",
                usage = SchemaUsage.ENTITY,
                contentSchema = SchemaDsl.obj("Component v2", "latest"),
            ),
        )
        schemas.register(
            Schema(
                type = "Component",
                version = "1.10.0",
                usage = SchemaUsage.ENTITY,
                contentSchema = SchemaDsl.obj("Component 1.10", "not latest"),
            ),
        )
        val types = catalog.listEntityTypes()
        assertThat(types).hasSize(1)
        assertThat(types[0].version).isEqualTo("2.0.0")
        assertThat(types[0].title).isEqualTo("Component v2")
        assertThat(catalog.getEntityType("Component")!!.version).isEqualTo("2.0.0")
        assertThat(catalog.getEntityType("Component", "1.10.0")!!.version).isEqualTo("1.10.0")
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
            Schema(
                type = "Thing",
                version = "1.0.0",
                usage = SchemaUsage.ENTITY,
                contentSchema = SchemaDsl.obj(
                    "Thing",
                    "Thing payload",
                    listOf(
                        SchemaDsl.field(
                            "purl",
                            SchemaDsl.string("Text", "Text value"),
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
