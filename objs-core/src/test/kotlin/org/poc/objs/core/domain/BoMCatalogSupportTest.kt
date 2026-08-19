package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BoMCatalogSupportTest {
    private lateinit var schemas: InMemoryBoMSchemaCatalog
    private lateinit var edges: InMemoryBoMAllowedEdgeCatalog
    private lateinit var catalog: BoMCatalogSupport

    @BeforeEach
    fun setUp() {
        schemas = InMemoryBoMSchemaCatalog()
        edges = InMemoryBoMAllowedEdgeCatalog()
        catalog = BoMCatalogSupport(schemas, edges)
        schemas.register(
            BoMSchema(
                type = "Component",
                version = "1.2.0",
                usage = BoMSchemaUsage.ENTITY,
                contentSchema =
                    BoMSchemaDsl.obj(
                        "Component",
                        "older",
                        listOf(
                            BoMSchemaDsl.field(
                                "name",
                                BoMSchemaDsl.string("Name", "Component name"),
                                identifier = true,
                                searchable = true,
                            ),
                            BoMSchemaDsl.field(
                                "kind",
                                BoMSchemaDsl.string("Text", "Kind"),
                            ),
                        ),
                    ),
            ),
        )
        schemas.register(
            BoMSchema(
                type = "Component",
                version = "1.10.0",
                usage = BoMSchemaUsage.ENTITY,
                contentSchema =
                    BoMSchemaDsl.obj(
                        "Component",
                        "numeric latest on 1.x",
                        listOf(
                            BoMSchemaDsl.field(
                                "name",
                                BoMSchemaDsl.string("Name", "Component name"),
                                identifier = true,
                                searchable = true,
                            ),
                            BoMSchemaDsl.field(
                                "purl",
                                BoMSchemaDsl.string("purl", "Package URL"),
                                searchable = true,
                            ),
                        ),
                    ),
            ),
        )
        edges.register(BoMAllowedEdgeRule("Product", "CONTAINS", "Component"))
        edges.register(BoMAllowedEdgeRule(BoMAllowedEdgeRule.ANY, "ANNOTATES", BoMAllowedEdgeRule.ANY))
    }

    @Test
    fun shouldPickNumericLatestVersion_notLexicographic() {
        assertThat(BoMSchemaVersion.compare("1.10.0", "1.2.0")).isGreaterThan(0)
        assertThat(catalog.latestEntitySchema("Component")!!.version).isEqualTo("1.10.0")
    }

    @Test
    fun shouldListSearchableAndIdentifierHints() {
        val hints = catalog.fieldHints(catalog.latestEntitySchema("Component")!!)
        assertThat(hints.map { it.path }).containsExactlyInAnyOrder("name", "purl")
        assertThat(hints.single { it.path == "name" }.identifier).isTrue()
        assertThat(hints.none { it.path == "kind" }).isTrue()
        assertThat(hints.single { it.path == "purl" }.title).isEqualTo("purl")
    }

    @Test
    fun shouldListAllowedEdgesIncludingWildcards() {
        val forType = catalog.allowedEdgesForType("Component")
        assertThat(forType.incoming).anyMatch { it.sourceType == "Product" && it.role == "CONTAINS" }
        assertThat(forType.incoming).anyMatch { it.role == "ANNOTATES" && it.sourceType == "*" }
        assertThat(forType.outgoing).anyMatch { it.role == "ANNOTATES" && it.targetType == "*" }
    }

    @Test
    fun shouldBuildDisplayLabelAndFilterExpr() {
        val schema = catalog.latestEntitySchema("Component")!!
        assertThat(catalog.displayLabel(mapOf("name" to "Log4j"), "Component", schema)).isEqualTo("Log4j")
        assertThat(catalog.displayLabel(mapOf("name" to "  "), "Component", schema)).isEqualTo("Component")
        assertThat(catalog.filterMapToObjExpr(mapOf("type" to "Component", "name" to "Log4j")))
            .isEqualTo("type == 'Component' && p['name'] == 'Log4j'")
    }
}
