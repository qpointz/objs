package org.poc.objs.core.typed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchemaCatalog

class TypedToolkitTest {

    data class PersonPayload(val name: String, val age: Int? = null)

    private val personMeta = EntityTypeMeta(type = "Person", schemaVersion = "1.0.0")

    @Test
    fun shouldRoundTripTypedEntity() {
        val typed = TypedEntity(
            meta = personMeta,
            payloadType = PersonPayload::class.java,
            payload = PersonPayload(name = "Ada", age = 36),
        ).annotate("env", "test")

        val entity = typed.toBoMEntity()
        assertThat(entity.type).isEqualTo("Person")
        assertThat(entity.payload["name"]).isEqualTo("Ada")
        assertThat(entity.annotations["env"]).isEqualTo("test")

        val back = TypedEntity.fromBoMEntity(entity, personMeta, PersonPayload::class.java)
        assertThat(back.payload.name).isEqualTo("Ada")
        assertThat(back.payload.age).isEqualTo(36)
    }

    @Test
    fun shouldBuildGraphWithProvisionalIdsAndDefaultAnnotations() {
        val builder = GraphBuilder(defaultAnnotations = mapOf("app" to "demo"))
        val a = builder.add(
            TypedEntity(personMeta, PersonPayload::class.java, payload = PersonPayload("A")),
            key = "a",
        )
        val b = builder.add(
            TypedEntity(personMeta, PersonPayload::class.java, payload = PersonPayload("B")),
            key = "b",
        )
        builder.edge(a, "knows", b)

        val graph = builder.build()
        assertThat(graph.entities).hasSize(2)
        assertThat(graph.edges).hasSize(1)
        assertThat(graph.entities).allMatch { it.id != null }
        assertThat(graph.entities).allMatch { it.annotations["app"] == "demo" }
        assertThat(graph.edges[0].source).isEqualTo(a.id)
        assertThat(graph.edges[0].target).isEqualTo(b.id)
    }

    @Test
    fun shouldRegisterRegistryPack() {
        val schemas = BoMSchemaCatalog()
        val edges = BoMAllowedEdgeCatalog()
        val pack = RegistryPack(
            schemas = listOf(
                RegistryPack.objectSchema(
                    type = "Person",
                    version = "1.0.0",
                    required = listOf("name"),
                    properties = mapOf("name" to mapOf("type" to "string")),
                ),
            ),
            edgeRules = listOf(
                BoMAllowedEdgeRule("Person", "knows", "Person", BoMPropertiesPolicy.NONE),
            ),
        )
        pack.registerInto(schemas, edges)
        assertThat(schemas.get("Person", "1.0.0")).isNotNull
        assertThat(edges.find("Person", "knows", "Person")).isNotNull
    }
}
