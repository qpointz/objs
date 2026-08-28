package org.poc.objs.core.validation

import java.util.UUID

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.core.domain.InMemoryAllowedEdgeCatalog
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.PropertiesPolicy
import org.poc.objs.core.domain.Schema
import org.poc.objs.core.domain.SchemaDsl
import org.poc.objs.core.domain.InMemorySchemaCatalog

private fun personSchema() = Schema(
    "Person",
    "1",
    SchemaDsl.obj(
        "Person",
        "Person payload",
        listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Person name"))),
    ),
)

class ValidatorTest {
    private lateinit var schemas: InMemorySchemaCatalog
    private lateinit var allowed: InMemoryAllowedEdgeCatalog
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        schemas = InMemorySchemaCatalog()
        allowed = InMemoryAllowedEdgeCatalog()
        schemas.register(personSchema())
        schemas.register(
            Schema(
                "LinkProps",
                "1",
                SchemaDsl.obj(
                    "Link properties",
                    "Weighted relationship properties",
                    listOf(
                        SchemaDsl.field(
                            "weight",
                            SchemaDsl.number("Weight", "Relationship weight"),
                            required = false,
                        ),
                    ),
                ),
            ),
        )
        allowed.register(
            AllowedEdgeRule(
                sourceType = "Person",
                role = "knows",
                targetType = "Person",
                propertiesPolicy = PropertiesPolicy.NONE,
            ),
        )
        allowed.register(
            AllowedEdgeRule(
                sourceType = "Person",
                role = "weighted",
                targetType = "Person",
                propertiesPolicy = PropertiesPolicy.SCHEMA,
                emptyPropertiesAllowed = false,
                propertiesSchemaType = "LinkProps",
                propertiesSchemaVersion = "1",
            ),
        )
        validator = Validator(schemas, allowed)
    }

    @Test
    fun shouldAcceptValidEntity() {
        val entity = Entity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Ada"))
        assertThat(validator.validateEntities(listOf(entity)).isValid).isTrue()
    }

    @Test
    fun shouldRejectInvalidEntityPayload() {
        val entity = Entity(type = "Person", schemaVersion = "1", payload = mutableMapOf())
        val result = validator.validateEntities(listOf(entity))
        assertThat(result.isValid).isFalse()
        assertThat(result.issues.map { it.code }).contains("SCHEMA_VIOLATION")
    }

    @Test
    fun shouldDenyUnknownEdgeRule() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val edge = Edge(source = a, target = b, role = "unknown")
        val lookup = EntityTypeLookup { "Person" }
        val result = validator.validateEdges(listOf(edge), lookup)
        assertThat(result.isValid).isFalse()
        assertThat(result.issues.map { it.code }).contains("EDGE_NOT_ALLOWED")
    }

    @Test
    fun shouldRejectPropertiesOnBareEdge() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val edge = Edge(
            source = a,
            target = b,
            role = "knows",
            properties = mutableMapOf("x" to 1),
        )
        val result = validator.validateEdges(listOf(edge), EntityTypeLookup { "Person" })
        assertThat(result.issues.map { it.code }).contains("PROPERTIES_NOT_ALLOWED")
    }

    @Test
    fun shouldRequirePropertiesWhenEmptyForbidden() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val edge = Edge(
            source = a,
            target = b,
            role = "weighted",
            type = "LinkProps",
            schemaVersion = "1",
            properties = mutableMapOf(),
        )
        val result = validator.validateEdges(listOf(edge), EntityTypeLookup { "Person" })
        assertThat(result.issues.map { it.code }).contains("PROPERTIES_REQUIRED")
    }

    @Test
    fun shouldRejectEdgeSchemaThatDoesNotMatchAllowedRelation() {
        val edge = Edge(
            source = UUID.randomUUID(),
            target = UUID.randomUUID(),
            role = "weighted",
            type = "Person",
            schemaVersion = "1",
            properties = mutableMapOf("name" to "wrong schema"),
        )

        val result = validator.validateEdges(listOf(edge), EntityTypeLookup { "Person" })

        assertThat(result.issues.map { it.code }).contains("EDGE_SCHEMA_REF_MISMATCH")
    }

    @Test
    fun shouldReportMissingEndpoint() {
        val edge = Edge(source = UUID.randomUUID(), target = UUID.randomUUID(), role = "knows")
        val result = validator.validateEdges(listOf(edge), EntityTypeLookup { null })
        assertThat(result.issues.map { it.code }).contains("SOURCE_NOT_FOUND", "TARGET_NOT_FOUND")
    }
}

class PersistGateTest {
    private lateinit var schemas: InMemorySchemaCatalog
    private lateinit var allowed: InMemoryAllowedEdgeCatalog
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        schemas = InMemorySchemaCatalog()
        allowed = InMemoryAllowedEdgeCatalog()
        schemas.register(personSchema())
        allowed.register(
            AllowedEdgeRule("Person", "knows", "Person", PropertiesPolicy.NONE),
        )
        validator = Validator(schemas, allowed)
    }

    @Test
    fun shouldFailFastOnInvalidEntitiesBeforeEdges() {
        val store = mutableMapOf<java.util.UUID, String>()
        val gate = PersistGate(
            validator,
            storeLookup = EntityTypeLookup { store[it] },
            existsEntity = { it in store },
            existsEdge = { false },
        )
        val graph = Graph(
            entities = mutableListOf(Entity(type = "Person", schemaVersion = "1", payload = mutableMapOf())),
            edges = mutableListOf(),
        )
        val result = gate.validateWrite(graph)
        assertThat(result.isValid).isFalse()
        assertThat(result.issues.map { it.code }).contains("SCHEMA_VIOLATION")
    }

    @Test
    fun shouldValidateEdgeAgainstStoreEntity() {
        val existing = UUID.randomUUID()
        val store = mutableMapOf(existing to "Person")
        val gate = PersistGate(
            validator,
            storeLookup = EntityTypeLookup { store[it] },
            existsEntity = { it in store },
            existsEdge = { false },
        )
        val neu = UUID.randomUUID()
        val graph = Graph(
            entities = mutableListOf(
                Entity(id = neu, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Bob")),
            ),
            edges = mutableListOf(Edge(source = neu, target = existing, role = "knows")),
        )
        val result = gate.validateWrite(graph)
        assertThat(result.isValid).isTrue()
        assertThat(graph.entities[0].id).isNotNull()
    }

    @Test
    fun shouldAssignIdsWhenMissing() {
        val gate = PersistGate(
            validator,
            storeLookup = EntityTypeLookup { null },
            existsEntity = { false },
            existsEdge = { false },
        )
        val graph = Graph(
            entities = mutableListOf(
                Entity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Ada")),
            ),
        )
        assertThat(gate.validateWrite(graph).isValid).isTrue()
        assertThat(graph.entities[0].id).isNotNull()
    }
}
