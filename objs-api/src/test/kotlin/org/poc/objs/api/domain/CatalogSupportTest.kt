package org.poc.objs.api.domain

import org.poc.objs.api.domain.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CatalogSupportTest {
    private lateinit var schemas: InMemorySchemaCatalog
    private lateinit var edges: InMemoryAllowedEdgeCatalog
    private lateinit var catalog: CatalogSupport

    @BeforeEach
    fun setUp() {
        schemas = InMemorySchemaCatalog()
        edges = InMemoryAllowedEdgeCatalog()
        catalog = CatalogSupport(schemas, edges)
        schemas.register(
            Schema(
                type = "Component",
                version = "1.2.0",
                usage = SchemaUsage.ENTITY,
                contentSchema =
                    SchemaDsl.obj(
                        "Component",
                        "older",
                        listOf(
                            SchemaDsl.field(
                                "name",
                                SchemaDsl.string("Name", "Component name"),
                                identifier = true,
                                searchable = true,
                            ),
                            SchemaDsl.field(
                                "kind",
                                SchemaDsl.string("Text", "Kind"),
                            ),
                        ),
                    ),
            ),
        )
        schemas.register(
            Schema(
                type = "Component",
                version = "1.10.0",
                usage = SchemaUsage.ENTITY,
                contentSchema =
                    SchemaDsl.obj(
                        "Component",
                        "numeric latest on 1.x",
                        listOf(
                            SchemaDsl.field(
                                "name",
                                SchemaDsl.string("Name", "Component name"),
                                identifier = true,
                                searchable = true,
                            ),
                            SchemaDsl.field(
                                "purl",
                                SchemaDsl.string("purl", "Package URL"),
                                searchable = true,
                            ),
                        ),
                    ),
            ),
        )
        edges.register(AllowedEdgeRule("Product", "CONTAINS", "Component"))
        edges.register(AllowedEdgeRule(AllowedEdgeRule.ANY, "ANNOTATES", AllowedEdgeRule.ANY))
    }

    @Test
    fun shouldPickNumericLatestVersion_notLexicographic() {
        assertThat(SchemaVersion.compare("1.10.0", "1.2.0")).isGreaterThan(0)
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
        assertThat(catalog.filterMapToObjExpr(mapOf("name" to "Log4*")))
            .isEqualTo("p['name'] =~ '^Log4'")
        assertThat(catalog.filterMapToObjExpr(mapOf("version" to ">2.0")))
            .isEqualTo("p['version'] > '2.0'")
    }
}
