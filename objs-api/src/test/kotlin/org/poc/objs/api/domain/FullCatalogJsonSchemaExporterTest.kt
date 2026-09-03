package org.poc.objs.api.domain

import org.poc.objs.api.domain.*

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FullCatalogJsonSchemaExporterTest {
    private lateinit var schemas: InMemorySchemaCatalog
    private lateinit var rules: InMemoryAllowedEdgeCatalog
    private lateinit var exporter: FullCatalogJsonSchemaExporter

    @BeforeEach
    fun setUp() {
        schemas = InMemorySchemaCatalog()
        rules = InMemoryAllowedEdgeCatalog()
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
        assertThat(doc["\$schema"]).isEqualTo(JsonSchema.DIALECT)
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
            JsonSchemaExportOptions(includeEdges = JsonSchemaEdgeInclusion.NONE),
        )
        @Suppress("UNCHECKED_CAST")
        val props = (doc["\$defs"] as Map<String, Map<String, Any?>>)
            .getValue("Product")["properties"] as Map<String, Any?>
        assertThat(props.keys).containsExactlyInAnyOrder("name", "sku")
    }

    @Test
    fun shouldAddInverseProps_whenIncludeEdgesLinked() {
        schemas.register(
            Schema(
                "Database",
                "1.0.0",
                SchemaDsl.obj(
                    "Database",
                    "Database payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        schemas.register(
            Schema(
                "Dataset",
                "1.0.0",
                SchemaDsl.obj(
                    "Dataset",
                    "Dataset payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        rules.register(
            AllowedEdgeRule(
                sourceType = "Database",
                role = "CONTAINS",
                targetType = "Dataset",
                cardinality = EdgeCardinality.ONE_TO_MANY,
            ),
        )

        val doc = exporter.export(
            JsonSchemaExportOptions(includeEdges = JsonSchemaEdgeInclusion.LINKED),
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
            JsonSchemaExportOptions(includeEdges = JsonSchemaEdgeInclusion.LINKED),
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
        val opts = JsonSchemaExportOptions.fromWire(
            dialect = "2020-12",
            includeEdges = "linked",
            includeEdgePropertySchemas = false,
        )
        assertThat(opts.includeEdges).isEqualTo(JsonSchemaEdgeInclusion.LINKED)
        assertThat(opts.includeEdgePropertySchemas).isFalse()

        val draft07 = JsonSchemaExportOptions.fromWire(dialect = "draft-07")
        assertThat(draft07.dialect).isEqualTo(JsonSchemaDialect.DRAFT_07)

        assertThatThrownBy { JsonSchemaExportOptions.fromWire(includeEdges = "both") }
            .isInstanceOf(JsonSchemaExportOptionsException::class.java)
        assertThatThrownBy { JsonSchemaExportOptions.fromWire(dialect = "draft-04") }
            .isInstanceOf(JsonSchemaExportOptionsException::class.java)
    }

    @Test
    fun shouldExportDraft07_withDefinitionsKeyword_andAllOfSingularRefs() {
        registerProductCatalog()

        val doc = exporter.export(
            JsonSchemaExportOptions(dialect = JsonSchemaDialect.DRAFT_07),
        )
        assertThat(doc["\$schema"]).isEqualTo(JsonSchemaDialect.DRAFT_07.schemaUri)
        assertThat(doc).doesNotContainKey("\$defs")
        @Suppress("UNCHECKED_CAST")
        val opts = doc["x-objs-json-schema-options"] as Map<String, Any?>
        assertThat(opts["dialect"]).isEqualTo("draft-07")

        @Suppress("UNCHECKED_CAST")
        val defs = doc["definitions"] as Map<String, Map<String, Any?>>
        assertThat(defs.keys).contains("Product", "Component", "Organization")

        @Suppress("UNCHECKED_CAST")
        val props = defs.getValue("Product")["properties"] as Map<String, Map<String, Any?>>
        val contains = props.getValue("containsComponent")
        assertThat(contains["type"]).isEqualTo("array")
        @Suppress("UNCHECKED_CAST")
        assertThat((contains["items"] as Map<String, Any?>)["\$ref"])
            .isEqualTo("#/definitions/Component")

        val owned = props.getValue("ownedByOrganization")
        assertThat(owned["\$ref"]).isNull()
        @Suppress("UNCHECKED_CAST")
        val allOf = owned["allOf"] as List<Map<String, Any?>>
        assertThat(allOf).hasSize(1)
        assertThat(allOf[0]["\$ref"]).isEqualTo("#/definitions/Organization")
        assertThat(owned["x-objs-direction"]).isEqualTo("outbound")
    }

    @Test
    fun shouldExportCodegenDraft07_refsDefinitions() {
        schemas.register(
            Schema(
                "Product",
                "1.0.0",
                SchemaDsl.obj(
                    "Product",
                    "Product payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
                ),
            ),
        )

        val doc = exporter.exportForCodegen(
            JsonSchemaExportOptions(dialect = JsonSchemaDialect.DRAFT_07),
        )
        assertThat(doc["\$schema"]).isEqualTo(JsonSchemaDialect.DRAFT_07.schemaUri)
        assertThat(doc).doesNotContainKey("\$defs")
        @Suppress("UNCHECKED_CAST")
        val props = doc["properties"] as Map<String, Map<String, Any?>>
        assertThat(props.getValue("Product")["\$ref"]).isEqualTo("#/definitions/Product")
        @Suppress("UNCHECKED_CAST")
        val defs = doc["definitions"] as Map<String, Map<String, Any?>>
        assertThat(defs.getValue("Product")["title"]).isEqualTo("Product")
    }

    @Test
    fun shouldExportCodegenRoot_refsAllDefs_andUsesDefKeysAsTitles() {
        schemas.register(
            Schema(
                "Container Image",
                "1.0.0",
                SchemaDsl.obj(
                    "Container Image",
                    "Image payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        schemas.register(
            Schema(
                "Product",
                "1.0.0",
                SchemaDsl.obj(
                    "Product",
                    "Product payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
                ),
            ),
        )

        val doc = exporter.exportForCodegen()
        assertThat(doc["x-objs-export"]).isEqualTo("full-catalog-codegen")
        assertThat(doc["type"]).isEqualTo("object")
        assertThat(doc["title"]).isEqualTo("ObjsCatalog")

        @Suppress("UNCHECKED_CAST")
        val defs = doc["\$defs"] as Map<String, Map<String, Any?>>
        assertThat(defs.keys).contains(
            "ContainerImage",
            "Product",
            "Entity",
            "Edge",
            "EntityMutation",
            "EdgeMutation",
            "GraphMutation",
        )
        assertThat(defs.getValue("ContainerImage")["title"]).isEqualTo("ContainerImage")
        assertThat(defs.getValue("Product")["title"]).isEqualTo("Product")

        @Suppress("UNCHECKED_CAST")
        val props = doc["properties"] as Map<String, Map<String, Any?>>
        assertThat(props.keys).containsExactly("ContainerImage", "Product")
        assertThat(props.getValue("ContainerImage")["\$ref"]).isEqualTo("#/\$defs/ContainerImage")
        assertThat(props.getValue("Product")["\$ref"]).isEqualTo("#/\$defs/Product")
    }

    @Test
    fun shouldExportCodegenContract_withoutSyntheticRelationProperties() {
        schemas.register(
            Schema(
                type = "Product",
                version = "1.0.0",
                contentSchema = SchemaDsl.obj(
                    "Product",
                    "Product payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
                ),
                tags = listOf("domain.product"),
                attributes = mapOf(
                    "codegen.java.typeName" to "ProductDto",
                    "codegen.baseClass" to "com.example.BaseEntity",
                    "codegen.interfaces" to "java.io.Serializable, com.example.Named",
                ),
            ),
        )
        schemas.register(
            Schema(
                type = "Component",
                version = "1.0.0",
                contentSchema = SchemaDsl.obj("Component", "Component payload"),
            ),
        )
        rules.register(
            AllowedEdgeRule(
                sourceType = "Product",
                role = "CONTAINS",
                targetType = "Component",
                cardinality = EdgeCardinality.ONE_TO_MANY,
                tags = listOf("codegen.java.noInverse"),
                attributes = mapOf("codegen.java.outboundMethod" to "components"),
            ),
        )

        val doc = exporter.exportForCodegen()

        @Suppress("UNCHECKED_CAST")
        val product = (doc["\$defs"] as Map<String, Map<String, Any?>>).getValue("Product")
        @Suppress("UNCHECKED_CAST")
        val productProps = product["properties"] as Map<String, Any?>
        assertThat(product["title"]).isEqualTo("ProductDto")
        assertThat(productProps.keys).containsExactly("name")

        @Suppress("UNCHECKED_CAST")
        val codegen = doc["x-objs-codegen"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val definitions = codegen["definitions"] as List<Map<String, Any?>>
        val productMetadata = definitions.first { it["type"] == "Product" }
        assertThat(productMetadata["kind"]).isEqualTo("ENTITY")
        assertThat(productMetadata["schemaVersion"]).isEqualTo("1.0.0")
        @Suppress("UNCHECKED_CAST")
        assertThat(productMetadata["tags"] as List<String>).containsExactly("domain.product")
        assertThat(productMetadata["baseClass"]).isEqualTo("com.example.BaseEntity")
        @Suppress("UNCHECKED_CAST")
        assertThat(productMetadata["interfaces"] as List<String>).containsExactly(
            "java.io.Serializable",
            "com.example.Named",
        )

        @Suppress("UNCHECKED_CAST")
        val relations = doc["x-objs-relations"] as List<Map<String, Any?>>
        val relation = relations.single()
        assertThat(relation["sourceDefinition"]).isEqualTo("Product")
        assertThat(relation["targetDefinition"]).isEqualTo("Component")
        assertThat((relation["codegen"] as Map<String, Any?>)["outboundMethod"])
            .isEqualTo("components")
        assertThat((relation["navigation"] as Map<String, Any?>)["inbound"]).isNull()
    }

    @Test
    fun shouldExportCodegenMutationDefinitions_andHistoricalSchemaIdentity() {
        schemas.register(Schema("Product", "1.0.0", SchemaDsl.obj("Product", "Product v1")))
        schemas.register(Schema("Product", "2.0.0", SchemaDsl.obj("Product", "Product v2")))
        schemas.register(
            Schema(
                "ProductEdge",
                "1.0.0",
                SchemaDsl.obj("Product edge", "Product edge properties"),
                usage = SchemaUsage.EDGE_PROPERTIES,
            ),
        )
        rules.register(
            AllowedEdgeRule(
                sourceType = "Product",
                role = "HAS_EDGE",
                targetType = "Product",
                propertiesPolicy = PropertiesPolicy.SCHEMA,
                propertiesSchemaType = "ProductEdge",
                propertiesSchemaVersion = "1.0.0",
            ),
        )

        val doc = exporter.exportForCodegen(JsonSchemaExportOptions(dialect = JsonSchemaDialect.DRAFT_07))

        @Suppress("UNCHECKED_CAST")
        val defs = doc["definitions"] as Map<String, Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val mutation = defs.getValue("GraphMutation")
        val mutationProps = mutation["properties"] as Map<String, Map<String, Any?>>
        assertThat(mutationProps.getValue("entities")["\$ref"]).isEqualTo("#/definitions/EntityMutation")
        assertThat(mutationProps.getValue("edges")["\$ref"]).isEqualTo("#/definitions/EdgeMutation")

        @Suppress("UNCHECKED_CAST")
        val codegen = doc["x-objs-codegen"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val versions = codegen["schemas"] as List<Map<String, Any?>>
        assertThat(versions.map { it["schemaVersion"] })
            .containsExactly("1.0.0", "2.0.0", "1.0.0")
        @Suppress("UNCHECKED_CAST")
        val relation = (doc["x-objs-relations"] as List<Map<String, Any?>>).single()
        assertThat(relation["propertiesPolicy"]).isEqualTo("SCHEMA")
        assertThat((relation["propertySchema"] as Map<String, Any?>)["definitionKey"])
            .isEqualTo("ProductEdge")
        assertThat((codegen["diagnostics"] as List<*>)).isEmpty()
    }

    @Test
    fun shouldKeepWildcardRelation_runtimeOnly_withoutBaseClass() {
        schemas.register(Schema("Product", "1", SchemaDsl.obj("Product", "Product payload")))
        rules.register(
            AllowedEdgeRule(
                sourceType = AllowedEdgeRule.ANY,
                role = "RELATED",
                targetType = "Product",
            ),
        )

        val doc = exporter.exportForCodegen()

        @Suppress("UNCHECKED_CAST")
        val relation = (doc["x-objs-relations"] as List<Map<String, Any?>>).single()
        assertThat(relation["sourceDefinition"]).isNull()
        assertThat((relation["codegen"] as Map<String, Any?>)["sourceStaticBinding"] as Boolean).isFalse()
        assertThat((relation["codegen"] as Map<String, Any?>)["targetStaticBinding"] as Boolean).isFalse()
    }

    @Test
    fun shouldExposeWildcardBinding_andNonBlockingPropertyDiagnostics() {
        schemas.register(Schema("Product", "1", SchemaDsl.obj("Product", "Product payload")))
        rules.register(
            AllowedEdgeRule(
                sourceType = AllowedEdgeRule.ANY,
                role = "RELATED",
                targetType = "Product",
                propertiesPolicy = PropertiesPolicy.SCHEMA,
                propertiesSchemaType = "MissingEdgeProperties",
                attributes = mapOf("codegen.baseClass" to "com.example.BaseNode"),
            ),
        )

        val doc = exporter.exportForCodegen()

        @Suppress("UNCHECKED_CAST")
        val relation = (doc["x-objs-relations"] as List<Map<String, Any?>>).single()
        val codegen = relation["codegen"] as Map<String, Any?>
        assertThat(codegen["baseClass"]).isEqualTo("com.example.BaseNode")
        assertThat(codegen["sourceStaticBinding"] as Boolean).isTrue()
        assertThat(codegen["targetStaticBinding"] as Boolean).isTrue()
        @Suppress("UNCHECKED_CAST")
        val property = relation["propertySchema"] as Map<String, Any?>
        assertThat(property["representation"]).isEqualTo("generic")
        @Suppress("UNCHECKED_CAST")
        val diagnostics = (doc["x-objs-codegen"] as Map<String, Any?>)["diagnostics"] as List<Map<String, Any?>>
        assertThat(diagnostics.single()["code"]).isEqualTo("EDGE_PROPERTIES_SCHEMA_NOT_FOUND")
    }

    @Test
    fun shouldRejectCodegenJavaSymbolCollisions() {
        schemas.register(Schema("a-b", "1", SchemaDsl.obj("A-B", "Payload")))
        schemas.register(Schema("a b", "1", SchemaDsl.obj("A B", "Payload")))

        assertThatThrownBy { exporter.exportForCodegen() }
            .isInstanceOf(JsonSchemaCodegenException::class.java)
            .hasMessageContaining("Definition key collision")
    }

    @Test
    fun shouldRejectBlankCodegenOverride() {
        schemas.register(
            Schema(
                "Product",
                "1",
                SchemaDsl.obj("Product", "Payload"),
                attributes = mapOf("codegen.java.typeName" to " "),
            ),
        )

        assertThatThrownBy { exporter.exportForCodegen() }
            .isInstanceOf(JsonSchemaCodegenException::class.java)
            .hasMessageContaining("must not be blank")
    }

    private fun registerProductCatalog() {
        schemas.register(
            Schema(
                "Product",
                "1.0.0",
                SchemaDsl.obj(
                    "Product",
                    "Product payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        schemas.register(
            Schema(
                "Product",
                "2.0.0",
                SchemaDsl.obj(
                    "Product",
                    "Product payload v2",
                    listOf(
                        SchemaDsl.field("name", SchemaDsl.string("Name", "Name")),
                        SchemaDsl.field("sku", SchemaDsl.string("SKU", "SKU"), required = false),
                    ),
                ),
            ),
        )
        schemas.register(
            Schema(
                "Component",
                "1.0.0",
                SchemaDsl.obj(
                    "Component",
                    "Component payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        schemas.register(
            Schema(
                "Organization",
                "1.0.0",
                SchemaDsl.obj(
                    "Organization",
                    "Org payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
                ),
            ),
        )
        rules.register(
            AllowedEdgeRule(
                sourceType = "Product",
                role = "CONTAINS",
                targetType = "Component",
                cardinality = EdgeCardinality.ONE_TO_MANY,
            ),
        )
        rules.register(
            AllowedEdgeRule(
                sourceType = "Product",
                role = "OWNED_BY",
                targetType = "Organization",
                cardinality = EdgeCardinality.ONE_TO_ONE,
            ),
        )
        rules.register(
            AllowedEdgeRule(
                sourceType = "*",
                role = "ANY_REL",
                targetType = "Component",
                cardinality = EdgeCardinality.ONE_TO_MANY,
            ),
        )
    }
}
