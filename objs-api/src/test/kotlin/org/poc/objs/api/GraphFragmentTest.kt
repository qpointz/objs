package org.poc.objs.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.api.domain.GraphFragmentDiagnosticSeverity
import org.poc.objs.api.domain.GraphMutation
import org.poc.objs.api.typed.GraphBuilder
import org.poc.objs.api.typed.PayloadMapper
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class GraphFragmentTest {
    @Test
    fun shouldDeduplicateSemanticallyEqualRecordsAndSortOutput() {
        val first = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val second = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val edgeId = UUID.fromString("00000000-0000-0000-0000-000000000010")
        val edge = Edge(edgeId, source = first, target = second, role = "depends_on")
        val fragment = Graph(
            entities = mutableListOf(
                Entity(first, "Component", "1", mutableMapOf("name" to "B")),
                Entity(second, "Component", "1", mutableMapOf("name" to "A")),
                Entity(first, "Component", "1", mutableMapOf("name" to "B")),
            ),
            edges = mutableListOf(edge, edge.copy(graphId = UUID.randomUUID())),
        )

        val resolved = DefaultGraphFragmentPolicy.resolve(fragment)

        assertThat(resolved.entities.map { it.id }).containsExactly(second, first)
        assertThat(resolved.edges).hasSize(1)
        assertThat(resolved.edges.single().graphId).isNull()
        assertThat(resolved.diagnostics).isEmpty()
    }

    @Test
    fun shouldReportConflictingRecordsWithoutSilentlyUsingInputOrder() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val fragment = GraphContents(
            entities = listOf(
                Entity(id, "Component", "1", mutableMapOf("name" to "z")),
                Entity(id, "Component", "1", mutableMapOf("name" to "a")),
            ),
            edges = emptyList(),
        )

        val resolved = DefaultGraphFragmentPolicy.resolve(fragment)

        assertThat(resolved.hasErrors()).isTrue()
        assertThat(resolved.diagnostics).anySatisfy {
            assertThat(it.severity).isEqualTo(GraphFragmentDiagnosticSeverity.ERROR)
            assertThat(it.nodes).containsExactly(id)
        }
        assertThat(resolved.entities.single().payload["name"]).isEqualTo("a")
    }

    @Test
    fun shouldReportDanglingEndpointsAndPreserveIdlessCandidates() {
        val existing = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val missing = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val fragment = GraphContents(
            entities = listOf(Entity(existing, "Component", "1"), Entity(type = "Unidentified", schemaVersion = "1")),
            edges = listOf(Edge(source = existing, target = missing, role = "depends_on")),
        )

        val resolved = DefaultGraphFragmentPolicy.resolve(fragment)

        assertThat(resolved.entities).hasSize(2)
        assertThat(resolved.hasErrors()).isTrue()
        assertThat(resolved.diagnostics).anySatisfy {
            assertThat(it.nodes).containsExactly(missing)
            assertThat(it.edges).isEmpty()
        }
    }

    @Test
    fun shouldExposeMutableGraphMutationAndTypedBuilderAtFragmentBoundary() {
        val entity = Entity(UUID.randomUUID(), "Component", "1")
        val mutation = GraphMutation.of(Graph(mutableListOf(entity)))
        val mapper = PayloadMapper(JsonMapper.builder().build())
        val builder = GraphBuilder(mapper)
        builder.add(entity.copy())

        assertThat(mutation.fragment().entities).containsExactly(entity)
        assertThat(builder.buildResolved().entities).hasSize(1)
        assertThat(builder.buildFragment().entities).hasSize(1)
    }
}
