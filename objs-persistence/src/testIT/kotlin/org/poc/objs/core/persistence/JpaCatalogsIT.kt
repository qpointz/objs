package org.poc.objs.core.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.api.domain.EdgeCardinality
import org.poc.objs.api.domain.PropertiesPolicy
import org.poc.objs.api.domain.Schema
import org.poc.objs.api.domain.SchemaDsl
import org.poc.objs.api.domain.SchemaUsage
import org.poc.objs.core.typed.DefaultPayloadMapper
import java.time.Duration

private fun schema(type: String, version: String, title: String = type) =
    Schema(type, version, SchemaDsl.obj(title, "$title payload"))

/**
 * Integration tests for [JpaSchemaCatalog] and [JpaAllowedEdgeCatalog] against real PostgreSQL
 * ([ObjsPostgresPersistenceFixture]: CI JDBC service or local Testcontainers).
 */
class JpaCatalogsIT : ObjsPostgresPersistenceFixture() {

    @BeforeEach
    fun reset() {
        schemaCatalog.clear()
        edgeCatalog.clear()
    }

    @Test
    fun shouldWireJpaCatalogsAsAuthoritativeImplementations() {
        assertThat(schemaCatalog).isInstanceOf(JpaSchemaCatalog::class.java)
        assertThat(edgeCatalog).isInstanceOf(JpaAllowedEdgeCatalog::class.java)
    }

    // ── Schema catalog ──

    @Test
    fun shouldRegisterAndRetrieveSchema() {
        schemaCatalog.register(
            Schema(
                "Person",
                "1",
                SchemaDsl.obj(
                    "Person",
                    "Person payload",
                    listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Person name"))),
                ),
            ),
        )

        assertThat(schemaCatalog.get("Person", "1")).isNotNull
        assertThat(schemaCatalog.get("Person", "1")!!.contentSchema.type.name).isEqualTo("OBJECT")
        assertThat(schemaCatalog.get("Person", "1")!!.usage).isEqualTo(SchemaUsage.ENTITY)
        assertThat(schemaCatalog.contains("Person", "1")).isTrue()
        assertThat(schemaCatalog.types()).contains("Person")
        assertThat(uow.read { schemaRepo.count() }).isEqualTo(1)
    }

    @Test
    fun shouldUpsertSchema() {
        schemaCatalog.register(schema("X", "1"))
        schemaCatalog.register(schema("X", "1", "Updated"))

        val schema = schemaCatalog.get("X", "1")!!
        assertThat(schema.contentSchema.title).isEqualTo("Updated")
        assertThat(schemaCatalog.all()).hasSize(1)
    }

    @Test
    fun shouldRemoveSchema() {
        schemaCatalog.register(schema("A", "1"))
        assertThat(schemaCatalog.remove("A", "1")).isTrue()
        assertThat(schemaCatalog.get("A", "1")).isNull()
        assertThat(schemaCatalog.remove("A", "1")).isFalse()
    }

    @Test
    fun shouldListByType() {
        schemaCatalog.register(schema("P", "1"))
        schemaCatalog.register(schema("P", "2"))
        schemaCatalog.register(schema("Q", "1"))

        assertThat(schemaCatalog.listByType("P")).hasSize(2)
        assertThat(schemaCatalog.types()).containsExactlyInAnyOrder("P", "Q")
    }

    @Test
    fun shouldHydrate() {
        // Insert directly via repo to ensure the row is in the DB
        uow.write {
            schemaRepo.save(
                SchemaCatalogRecord(
                    type = "H",
                    version = "1",
                    definitionDoc = DefaultPayloadMapper.toMap(schema("H", "1").contentSchema),
                    usage = "ENTITY",
                ),
            )
            schemaRepo.flush()
        }
        // Simulate restart: empty snapshot + explicit hydrate (TTL off so get does not auto-reload)
        val fresh = JpaSchemaCatalog(
            schemaRepo,
            uow,
            ObjsCatalogProperties(cacheTtl = Duration.ZERO),
        )
        assertThat(fresh.get("H", "1")).isNull() // cache empty before hydrate
        fresh.hydrate()
        assertThat(fresh.get("H", "1")).isNotNull
    }

    // ── Allowed-edge catalog ──

    @Test
    fun shouldRegisterAndFindEdgeRule() {
        edgeCatalog.register(
            AllowedEdgeRule(
                "Person",
                "knows",
                "Person",
                PropertiesPolicy.SCHEMA,
                false,
                propertiesSchemaType = "CanonicalEdge",
                propertiesSchemaVersion = "1.0.0",
            ),
        )
        val rule = edgeCatalog.find("Person", "knows", "Person")
        assertThat(rule).isNotNull
        assertThat(rule!!.propertiesPolicy).isEqualTo(PropertiesPolicy.SCHEMA)
        assertThat(rule.emptyPropertiesAllowed).isFalse()
        assertThat(rule.propertiesSchemaType).isEqualTo("CanonicalEdge")
        assertThat(rule.propertiesSchemaVersion).isEqualTo("1.0.0")
        assertThat(rule.cardinality).isEqualTo(EdgeCardinality.UNSPECIFIED)
        assertThat(uow.read { edgeRuleRepo.count() }).isEqualTo(1)
    }

    @Test
    fun shouldPersistCardinalityWireValues() {
        edgeCatalog.register(
            AllowedEdgeRule(
                "Product",
                "CONTAINS",
                "Component",
                cardinality = EdgeCardinality.ONE_TO_ONE,
            ),
        )
        edgeCatalog.register(
            AllowedEdgeRule(
                "Component",
                "DEPENDS_ON",
                "Component",
                cardinality = EdgeCardinality.ONE_TO_MANY,
            ),
        )
        assertThat(edgeCatalog.find("Product", "CONTAINS", "Component")!!.cardinality)
            .isEqualTo(EdgeCardinality.ONE_TO_ONE)
        assertThat(edgeCatalog.find("Component", "DEPENDS_ON", "Component")!!.cardinality)
            .isEqualTo(EdgeCardinality.ONE_TO_MANY)
        assertThat(
            uow.read {
                edgeRuleRepo.findById(
                    AllowedEdgeRuleId("Product", "CONTAINS", "Component"),
                )!!.cardinality
            },
        ).isEqualTo(EdgeCardinality.ONE_TO_ONE)
    }

    @Test
    fun shouldUpsertEdgeRule() {
        edgeCatalog.register(AllowedEdgeRule("A", "r", "B", PropertiesPolicy.NONE, true))
        edgeCatalog.register(
            AllowedEdgeRule(
                "A",
                "r",
                "B",
                PropertiesPolicy.SCHEMA,
                false,
                cardinality = EdgeCardinality.ONE_TO_MANY,
            ),
        )

        val rule = edgeCatalog.find("A", "r", "B")!!
        assertThat(rule.propertiesPolicy).isEqualTo(PropertiesPolicy.SCHEMA)
        assertThat(rule.emptyPropertiesAllowed).isFalse()
        assertThat(rule.cardinality).isEqualTo(EdgeCardinality.ONE_TO_MANY)
        assertThat(edgeCatalog.all()).hasSize(1)
    }

    @Test
    fun shouldRemoveEdgeRule() {
        edgeCatalog.register(AllowedEdgeRule("A", "r", "B"))
        assertThat(edgeCatalog.remove("A", "r", "B")).isTrue()
        assertThat(edgeCatalog.find("A", "r", "B")).isNull()
        assertThat(edgeCatalog.remove("A", "r", "B")).isFalse()
    }

    @Test
    fun shouldPreferMostSpecificWildcard() {
        edgeCatalog.register(AllowedEdgeRule("*", "x", "*", PropertiesPolicy.NONE))
        edgeCatalog.register(AllowedEdgeRule("A", "x", "B", PropertiesPolicy.SCHEMA))
        assertThat(edgeCatalog.find("A", "x", "B")!!.propertiesPolicy).isEqualTo(PropertiesPolicy.SCHEMA)
        assertThat(edgeCatalog.find("C", "x", "D")!!.propertiesPolicy).isEqualTo(PropertiesPolicy.NONE)
    }

    @Test
    fun shouldHydrateEdges() {
        uow.write {
            edgeRuleRepo.save(AllowedEdgeRuleRecord(
                sourceType = "X", role = "y", targetType = "Z",
            ))
            edgeRuleRepo.flush()
        }
        val fresh = JpaAllowedEdgeCatalog(
            edgeRuleRepo,
            uow,
            ObjsCatalogProperties(cacheTtl = Duration.ZERO),
        )
        assertThat(fresh.find("X", "y", "Z")).isNull()
        fresh.hydrate()
        assertThat(fresh.find("X", "y", "Z")).isNotNull
    }
}
