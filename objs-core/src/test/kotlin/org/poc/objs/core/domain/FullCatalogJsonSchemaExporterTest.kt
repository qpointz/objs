package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
        assertThat(FullCatalogJsonSchemaExporter.inverseRelationPropertyName("CONTAINS", "Database"))
            .isEqualTo("containsFromDatabase")
        assertThat(FullCatalogJsonSchemaExporter.jsonSchemaDefKey("Container Image"))
            .isEqualTo("ContainerImage")
    }

    @Test
    fun shouldExportLatestEntityDefs_withRelationProps() {
        registerProductCatalog()

        val doc = exporter.export()
        assertThat(doc["\$schema"]).isEqualTo(BoMJsonSchema.DIALECT)
        assertThat(doc["x-objs-export"]).isEqualTo("full-catalog")
        @Suppress("UNCHECKED_CAST")
        val opts = doc["x-objs-json-schema-options"] as Map<String, Any?>
        assertThat(opts["dialect"]).isEqualTo("2020-12")
        assertThat(opts["includeEdges"]).isEqualTo("outbound")
        assertThat(opts["includeEdgePropertySchemas"]).isEqualTo(true)

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
        assertThat(contains["x-objs-direction"]).isEqualTo("outbound")

        val owned = props.getValue("ownedByOrganization")
        assertThat(owned["\$ref"]).isEqualTo("#/\$defs/Organization")
        assertThat(owned["type"]).isNull()
        assertThat(owned["x-objs-cardinality"]).isEqualTo("1:1")

        assertThat(props.keys).doesNotContain("anyRelComponent")

        @Suppress("UNCHECKED_CAST")
        val componentProps = defs.getValue("Component")["properties"] as Map<String, Any?>
        assertThat(componentProps.keys).doesNotContain("containsFromProduct")
    }

    @Test
    fun shouldOmitEdges_whenIncludeEdgesNone() {
        registerProductCatalog()
        val doc = exporter.export(
            BoMJsonSchemaExportOptions(includeEdges = BoMJsonSchemaEdgeInclusion.NONE),
        )
        @Suppress("UNCHECKED_CAST")
        val props = (doc["\$defs"] as Map<String, Map<String, Any?>>)
            .getValue("Product")["properties"] as Map<String, Any?>
        assertThat(props.keys).containsExactlyInAnyOrder("name", "sku")
    }

    @Test
    fun shouldAddInverseProps_whenIncludeEdgesLinked() {
        schemas.register(
            BoMSchema(
                "Database",
                "1.0.0",
                BoMSchemaDsl.obj(
                    "Database",
                    "Database payload",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        schemas.register(
            BoMSchema(
                "Dataset",
                "1.0.0",
                BoMSchemaDsl.obj(
                    "Dataset",
                    "Dataset payload",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        rules.register(
            BoMAllowedEdgeRule(
                sourceType = "Database",
                role = "CONTAINS",
                targetType = "Dataset",
                cardinality = BoMEdgeCardinality.ONE_TO_MANY,
            ),
        )

        val doc = exporter.export(
            BoMJsonSchemaExportOptions(includeEdges = BoMJsonSchemaEdgeInclusion.LINKED),
        )
        @Suppress("UNCHECKED_CAST")
        val defs = doc["\$defs"] as Map<String, Map<String, Any?>>

        @Suppress("UNCHECKED_CAST")
        val dbProps = defs.getValue("Database")["properties"] as Map<String, Map<String, Any?>>
        val contains = dbProps.getValue("containsDataset")
        assertThat(contains["type"]).isEqualTo("array")
        assertThat(contains["x-objs-direction"]).isEqualTo("outbound")

        @Suppress("UNCHECKED_CAST")
        val dsProps = defs.getValue("Dataset")["properties"] as Map<String, Map<String, Any?>>
        val parent = dsProps.getValue("containsFromDatabase")
        assertThat(parent["\$ref"]).isEqualTo("#/\$defs/Database")
        assertThat(parent["type"]).isNull()
        assertThat(parent["x-objs-direction"]).isEqualTo("inbound")
        assertThat(parent["x-objs-cardinality"]).isEqualTo("1:1")
        assertThat(parent["x-objs-source-type"]).isEqualTo("Database")
    }

    @Test
    fun shouldInverseOneToOneAsArray() {
        registerProductCatalog()
        val doc = exporter.export(
            BoMJsonSchemaExportOptions(includeEdges = BoMJsonSchemaEdgeInclusion.LINKED),
        )
        @Suppress("UNCHECKED_CAST")
        val orgProps = (doc["\$defs"] as Map<String, Map<String, Any?>>)
            .getValue("Organization")["properties"] as Map<String, Map<String, Any?>>
        val inverse = orgProps.getValue("ownedByFromProduct")
        assertThat(inverse["type"]).isEqualTo("array")
        @Suppress("UNCHECKED_CAST")
        assertThat((inverse["items"] as Map<String, Any?>)["\$ref"]).isEqualTo("#/\$defs/Product")
        assertThat(inverse["x-objs-cardinality"]).isEqualTo("1:*")
    }

    @Test
    fun shouldParseOptionsFromWire_andRejectUnknown() {
        val opts = BoMJsonSchemaExportOptions.fromWire(
            dialect = "2020-12",
            includeEdges = "linked",
            includeEdgePropertySchemas = false,
        )
        assertThat(opts.includeEdges).isEqualTo(BoMJsonSchemaEdgeInclusion.LINKED)
        assertThat(opts.includeEdgePropertySchemas).isFalse()

        assertThatThrownBy { BoMJsonSchemaExportOptions.fromWire(includeEdges = "both") }
            .isInstanceOf(BoMJsonSchemaExportOptionsException::class.java)
        assertThatThrownBy { BoMJsonSchemaExportOptions.fromWire(dialect = "draft-07") }
            .isInstanceOf(BoMJsonSchemaExportOptionsException::class.java)
    }

    @Test
    fun shouldExportCodegenRoot_refsAllDefs_andUsesDefKeysAsTitles() {
        schemas.register(
            BoMSchema(
                "Container Image",
                "1.0.0",
                BoMSchemaDsl.obj(
                    "Container Image",
                    "Image payload",
                    listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
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

        val doc = exporter.exportForCodegen()
        assertThat(doc["x-objs-export"]).isEqualTo("full-catalog-codegen")
        assertThat(doc["type"]).isEqualTo("object")
        assertThat(doc["title"]).isEqualTo("ObjsCatalog")

        @Suppress("UNCHECKED_CAST")
        val defs = doc["\$defs"] as Map<String, Map<String, Any?>>
        assertThat(defs.keys).containsExactlyInAnyOrder("ContainerImage", "Product")
        assertThat(defs.getValue("ContainerImage")["title"]).isEqualTo("ContainerImage")
        assertThat(defs.getValue("Product")["title"]).isEqualTo("Product")

        @Suppress("UNCHECKED_CAST")
        val props = doc["properties"] as Map<String, Map<String, Any?>>
        assertThat(props.keys).containsExactly("ContainerImage", "Product")
        assertThat(props.getValue("ContainerImage")["\$ref"]).isEqualTo("#/\$defs/ContainerImage")
        assertThat(props.getValue("Product")["\$ref"]).isEqualTo("#/\$defs/Product")
    }

    private fun registerProductCatalog() {
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
    }
}
