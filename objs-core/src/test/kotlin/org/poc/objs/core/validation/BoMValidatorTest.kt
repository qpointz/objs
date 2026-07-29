package org.poc.objs.core.validation

import java.util.UUID

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.InMemoryBoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMEdge
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.domain.BoMGraph
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.domain.InMemoryBoMSchemaCatalog

private fun personSchema() = BoMSchema(
    "Person",
    "1",
    BoMSchemaDsl.obj(
        "Person",
        "Person payload",
        listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Person name"))),
    ),
)

class BoMValidatorTest {
    private lateinit var schemas: InMemoryBoMSchemaCatalog
    private lateinit var allowed: InMemoryBoMAllowedEdgeCatalog
    private lateinit var validator: BoMValidator

    @BeforeEach
    fun setUp() {
        schemas = InMemoryBoMSchemaCatalog()
        allowed = InMemoryBoMAllowedEdgeCatalog()
        schemas.register(personSchema())
        schemas.register(
            BoMSchema(
                "LinkProps",
                "1",
                BoMSchemaDsl.obj(
                    "Link properties",
                    "Weighted relationship properties",
                    listOf(
                        BoMSchemaDsl.field(
                            "weight",
                            BoMSchemaDsl.number("Weight", "Relationship weight"),
                            required = false,
                        ),
                    ),
                ),
            ),
        )
        allowed.register(
            BoMAllowedEdgeRule(
                sourceType = "Person",
                role = "knows",
                targetType = "Person",
                propertiesPolicy = BoMPropertiesPolicy.NONE,
            ),
        )
        allowed.register(
            BoMAllowedEdgeRule(
                sourceType = "Person",
                role = "weighted",
                targetType = "Person",
                propertiesPolicy = BoMPropertiesPolicy.SCHEMA,
                emptyPropertiesAllowed = false,
                propertiesSchemaType = "LinkProps",
                propertiesSchemaVersion = "1",
            ),
        )
        validator = BoMValidator(schemas, allowed)
    }

    @Test
    fun shouldAcceptValidEntity() {
        val entity = BoMEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Ada"))
        assertThat(validator.validateEntities(listOf(entity)).isValid).isTrue()
    }

    @Test
    fun shouldRejectInvalidEntityPayload() {
        val entity = BoMEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf())
        val result = validator.validateEntities(listOf(entity))
        assertThat(result.isValid).isFalse()
        assertThat(result.issues.map { it.code }).contains("SCHEMA_VIOLATION")
    }

    @Test
    fun shouldDenyUnknownEdgeRule() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val edge = BoMEdge(source = a, target = b, role = "unknown")
        val lookup = BoMEntityTypeLookup { "Person" }
        val result = validator.validateEdges(listOf(edge), lookup)
        assertThat(result.isValid).isFalse()
        assertThat(result.issues.map { it.code }).contains("EDGE_NOT_ALLOWED")
    }

    @Test
    fun shouldRejectPropertiesOnBareEdge() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val edge = BoMEdge(
            source = a,
            target = b,
            role = "knows",
            properties = mutableMapOf("x" to 1),
        )
        val result = validator.validateEdges(listOf(edge), BoMEntityTypeLookup { "Person" })
        assertThat(result.issues.map { it.code }).contains("PROPERTIES_NOT_ALLOWED")
    }

    @Test
    fun shouldRequirePropertiesWhenEmptyForbidden() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val edge = BoMEdge(
            source = a,
            target = b,
            role = "weighted",
            type = "LinkProps",
            schemaVersion = "1",
            properties = mutableMapOf(),
        )
        val result = validator.validateEdges(listOf(edge), BoMEntityTypeLookup { "Person" })
        assertThat(result.issues.map { it.code }).contains("PROPERTIES_REQUIRED")
    }

    @Test
    fun shouldRejectEdgeSchemaThatDoesNotMatchAllowedRelation() {
        val edge = BoMEdge(
            source = UUID.randomUUID(),
            target = UUID.randomUUID(),
            role = "weighted",
            type = "Person",
            schemaVersion = "1",
            properties = mutableMapOf("name" to "wrong schema"),
        )

        val result = validator.validateEdges(listOf(edge), BoMEntityTypeLookup { "Person" })

        assertThat(result.issues.map { it.code }).contains("EDGE_SCHEMA_REF_MISMATCH")
    }

    @Test
    fun shouldReportMissingEndpoint() {
        val edge = BoMEdge(source = UUID.randomUUID(), target = UUID.randomUUID(), role = "knows")
        val result = validator.validateEdges(listOf(edge), BoMEntityTypeLookup { null })
        assertThat(result.issues.map { it.code }).contains("SOURCE_NOT_FOUND", "TARGET_NOT_FOUND")
    }
}

class BoMPersistGateTest {
    private lateinit var schemas: InMemoryBoMSchemaCatalog
    private lateinit var allowed: InMemoryBoMAllowedEdgeCatalog
    private lateinit var validator: BoMValidator

    @BeforeEach
    fun setUp() {
        schemas = InMemoryBoMSchemaCatalog()
        allowed = InMemoryBoMAllowedEdgeCatalog()
        schemas.register(personSchema())
        allowed.register(
            BoMAllowedEdgeRule("Person", "knows", "Person", BoMPropertiesPolicy.NONE),
        )
        validator = BoMValidator(schemas, allowed)
    }

    @Test
    fun shouldFailFastOnInvalidEntitiesBeforeEdges() {
        val store = mutableMapOf<java.util.UUID, String>()
        val gate = BoMPersistGate(
            validator,
            storeLookup = BoMEntityTypeLookup { store[it] },
            existsEntity = { it in store },
            existsEdge = { false },
        )
        val graph = BoMGraph(
            entities = mutableListOf(BoMEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf())),
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
        val gate = BoMPersistGate(
            validator,
            storeLookup = BoMEntityTypeLookup { store[it] },
            existsEntity = { it in store },
            existsEdge = { false },
        )
        val neu = UUID.randomUUID()
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(id = neu, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Bob")),
            ),
            edges = mutableListOf(BoMEdge(source = neu, target = existing, role = "knows")),
        )
        val result = gate.validateWrite(graph)
        assertThat(result.isValid).isTrue()
        assertThat(graph.entities[0].id).isNotNull()
    }

    @Test
    fun shouldAssignIdsWhenMissing() {
        val gate = BoMPersistGate(
            validator,
            storeLookup = BoMEntityTypeLookup { null },
            existsEntity = { false },
            existsEdge = { false },
        )
        val graph = BoMGraph(
            entities = mutableListOf(
                BoMEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Ada")),
            ),
        )
        assertThat(gate.validateWrite(graph).isValid).isTrue()
        assertThat(graph.entities[0].id).isNotNull()
    }
}
