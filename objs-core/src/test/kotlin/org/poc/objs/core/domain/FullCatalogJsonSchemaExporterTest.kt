package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FullCatalogJsonSchemaExporterTest {
    private lateinit var schemas: InMemoryBoMSchemaCatalog
    private lateinit var rules: InMemoryBoMAllowedEdgeCatalog
    private lateinit var exporter: FullCatalogJsonSchemaExporter

    @BeforeEach
    fun setUp() {
        schemas = InMemoryBoMSchemaCatalog()
        rules = InMemoryBoMAllowedEdgeCatalog()
        exporter = FullCatalogJsonSchemaExporter(schemas, rules)
    }

    @Test
    fun shouldNameRelationProperties_andRespectCardinality() {
        assertThat(FullCatalogJsonSchemaExporter.relationPropertyName("CONTAINS", "Component"))
            .isEqualTo("containsComponent")
        assertThat(FullCatalogJsonSchemaExporter.relationPropertyName("OWNED_BY", "Organization"))
            .isEqualTo("ownedByOrganization")
        assertThat(FullCatalogJsonSchemaExporter.jsonSchemaDefKey("Container Image"))
            .isEqualTo("ContainerImage")
    }

    @Test
    fun shouldExportLatestEntityDefs_withRelationProps() {
        schemas.register(
            BoMSchema(
                "Product",
                "1.0.0",
                BoMSchemaDsl.obj(
                    "Product",
                    "Product payload",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        schemas.register(
            BoMSchema(
                "Product",
                "2.0.0",
                BoMSchemaDsl.obj(
                    "Product",
                    "Product payload v2",
                    listOf(
                        BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name")),
                        BoMSchemaDsl.field("sku", BoMSchemaDsl.string("SKU", "SKU"), required = false),
                    ),
                ),
            ),
        )
        schemas.register(
            BoMSchema(
                "Component",
                "1.0.0",
                BoMSchemaDsl.obj(
                    "Component",
                    "Component payload",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        schemas.register(
            BoMSchema(
                "Organization",
                "1.0.0",
                BoMSchemaDsl.obj(
                    "Organization",
                    "Org payload",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        rules.register(
            BoMAllowedEdgeRule(
                sourceType = "Product",
                role = "CONTAINS",
                targetType = "Component",
                cardinality = BoMEdgeCardinality.ONE_TO_MANY,
            ),
        )
        rules.register(
            BoMAllowedEdgeRule(
                sourceType = "Product",
                role = "OWNED_BY",
                targetType = "Organization",
                cardinality = BoMEdgeCardinality.ONE_TO_ONE,
            ),
        )
        rules.register(
            BoMAllowedEdgeRule(
                sourceType = "*",
                role = "ANY_REL",
                targetType = "Component",
                cardinality = BoMEdgeCardinality.ONE_TO_MANY,
            ),
        )

        val doc = exporter.export()
        assertThat(doc["\$schema"]).isEqualTo(BoMJsonSchema.DIALECT)
        assertThat(doc["x-objs-export"]).isEqualTo("full-catalog")

        @Suppress("UNCHECKED_CAST")
        val defs = doc["\$defs"] as Map<String, Map<String, Any?>>
        assertThat(defs.keys).containsExactlyInAnyOrder("Product", "Component", "Organization")

        val product = defs.getValue("Product")
        assertThat(product["x-objs-version"]).isEqualTo("2.0.0")

        @Suppress("UNCHECKED_CAST")
        val props = product["properties"] as Map<String, Map<String, Any?>>
        assertThat(props.keys).contains("name", "sku", "containsComponent", "ownedByOrganization")

        val contains = props.getValue("containsComponent")
        assertThat(contains["type"]).isEqualTo("array")
        @Suppress("UNCHECKED_CAST")
        assertThat((contains["items"] as Map<String, Any?>)["\$ref"]).isEqualTo("#/\$defs/Component")
        assertThat(contains["x-objs-cardinality"]).isEqualTo("1:*")

        val owned = props.getValue("ownedByOrganization")
        assertThat(owned["\$ref"]).isEqualTo("#/\$defs/Organization")
        assertThat(owned["type"]).isNull()
        assertThat(owned["x-objs-cardinality"]).isEqualTo("1:1")

        assertThat(props.keys).doesNotContain("anyRelComponent")
    }
}
