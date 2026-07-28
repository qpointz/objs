package org.poc.objs.core.validation

import java.util.UUID

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoAllowedEdgeCatalog
import org.poc.objs.core.domain.BoAllowedEdgeRule
import org.poc.objs.core.domain.BoEdge
import org.poc.objs.core.domain.BoEntity
import org.poc.objs.core.domain.BoGraph
import org.poc.objs.core.domain.BoPropertiesPolicy
import org.poc.objs.core.domain.BoSchema
import org.poc.objs.core.domain.BoSchemaCatalog

class BoValidatorTest {
    private lateinit var schemas: BoSchemaCatalog
    private lateinit var allowed: BoAllowedEdgeCatalog
    private lateinit var validator: BoValidator

    @BeforeEach
    fun setUp() {
        schemas = BoSchemaCatalog()
        allowed = BoAllowedEdgeCatalog()
        schemas.register(
            BoSchema(
                type = "Person",
                version = "1",
                schema = mapOf(
                    "type" to "object",
                    "required" to listOf("name"),
                    "properties" to mapOf(
                        "name" to mapOf("type" to "string"),
                    ),
                ),
            ),
        )
        schemas.register(
            BoSchema(
                type = "LinkProps",
                version = "1",
                schema = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "weight" to mapOf("type" to "number"),
                    ),
                ),
            ),
        )
        allowed.register(
            BoAllowedEdgeRule(
                sourceType = "Person",
                role = "knows",
                targetType = "Person",
                propertiesPolicy = BoPropertiesPolicy.NONE,
            ),
        )
        allowed.register(
            BoAllowedEdgeRule(
                sourceType = "Person",
                role = "weighted",
                targetType = "Person",
                propertiesPolicy = BoPropertiesPolicy.SCHEMA,
                emptyPropertiesAllowed = false,
            ),
        )
        validator = BoValidator(schemas, allowed)
    }

    @Test
    fun shouldAcceptValidEntity() {
        val entity = BoEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Ada"))
        assertThat(validator.validateEntities(listOf(entity)).isValid).isTrue()
    }

    @Test
    fun shouldRejectInvalidEntityPayload() {
        val entity = BoEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf())
        val result = validator.validateEntities(listOf(entity))
        assertThat(result.isValid).isFalse()
        assertThat(result.issues.map { it.code }).contains("SCHEMA_VIOLATION")
    }

    @Test
    fun shouldDenyUnknownEdgeRule() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val edge = BoEdge(source = a, target = b, role = "unknown")
        val lookup = BoEntityTypeLookup { "Person" }
        val result = validator.validateEdges(listOf(edge), lookup)
        assertThat(result.isValid).isFalse()
        assertThat(result.issues.map { it.code }).contains("EDGE_NOT_ALLOWED")
    }

    @Test
    fun shouldRejectPropertiesOnBareEdge() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val edge = BoEdge(
            source = a,
            target = b,
            role = "knows",
            properties = mutableMapOf("x" to 1),
        )
        val result = validator.validateEdges(listOf(edge), BoEntityTypeLookup { "Person" })
        assertThat(result.issues.map { it.code }).contains("PROPERTIES_NOT_ALLOWED")
    }

    @Test
    fun shouldRequirePropertiesWhenEmptyForbidden() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val edge = BoEdge(
            source = a,
            target = b,
            role = "weighted",
            type = "LinkProps",
            schemaVersion = "1",
            properties = mutableMapOf(),
        )
        val result = validator.validateEdges(listOf(edge), BoEntityTypeLookup { "Person" })
        assertThat(result.issues.map { it.code }).contains("PROPERTIES_REQUIRED")
    }

    @Test
    fun shouldReportMissingEndpoint() {
        val edge = BoEdge(source = UUID.randomUUID(), target = UUID.randomUUID(), role = "knows")
        val result = validator.validateEdges(listOf(edge), BoEntityTypeLookup { null })
        assertThat(result.issues.map { it.code }).contains("SOURCE_NOT_FOUND", "TARGET_NOT_FOUND")
    }
}

class BoPersistGateTest {
    private lateinit var schemas: BoSchemaCatalog
    private lateinit var allowed: BoAllowedEdgeCatalog
    private lateinit var validator: BoValidator

    @BeforeEach
    fun setUp() {
        schemas = BoSchemaCatalog()
        allowed = BoAllowedEdgeCatalog()
        schemas.register(
            BoSchema(
                type = "Person",
                version = "1",
                schema = mapOf(
                    "type" to "object",
                    "required" to listOf("name"),
                    "properties" to mapOf("name" to mapOf("type" to "string")),
                ),
            ),
        )
        allowed.register(
            BoAllowedEdgeRule("Person", "knows", "Person", BoPropertiesPolicy.NONE),
        )
        validator = BoValidator(schemas, allowed)
    }

    @Test
    fun shouldFailFastOnInvalidEntitiesBeforeEdges() {
        val store = mutableMapOf<java.util.UUID, String>()
        val gate = BoPersistGate(
            validator,
            storeLookup = BoEntityTypeLookup { store[it] },
            existsEntity = { it in store },
            existsEdge = { false },
        )
        val graph = BoGraph(
            entities = mutableListOf(BoEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf())),
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
        val gate = BoPersistGate(
            validator,
            storeLookup = BoEntityTypeLookup { store[it] },
            existsEntity = { it in store },
            existsEdge = { false },
        )
        val neu = UUID.randomUUID()
        val graph = BoGraph(
            entities = mutableListOf(
                BoEntity(id = neu, type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Bob")),
            ),
            edges = mutableListOf(BoEdge(source = neu, target = existing, role = "knows")),
        )
        val result = gate.validateWrite(graph)
        assertThat(result.isValid).isTrue()
        assertThat(graph.entities[0].id).isNotNull()
    }

    @Test
    fun shouldAssignIdsWhenMissing() {
        val gate = BoPersistGate(
            validator,
            storeLookup = BoEntityTypeLookup { null },
            existsEntity = { false },
            existsEdge = { false },
        )
        val graph = BoGraph(
            entities = mutableListOf(
                BoEntity(type = "Person", schemaVersion = "1", payload = mutableMapOf("name" to "Ada")),
            ),
        )
        assertThat(gate.validateWrite(graph).isValid).isTrue()
        assertThat(graph.entities[0].id).isNotNull()
    }
}
