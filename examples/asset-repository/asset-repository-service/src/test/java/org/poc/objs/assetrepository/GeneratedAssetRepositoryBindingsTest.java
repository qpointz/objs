package org.poc.objs.assetrepository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.poc.objs.api.domain.Edge;
import org.poc.objs.api.domain.Entity;
import org.poc.objs.api.domain.Graph;
import org.poc.objs.api.domain.GraphMutation;
import org.poc.objs.api.domain.MutationMode;
import org.poc.objs.api.typed.PayloadMapper;
import org.poc.objs.assetrepository.codegen.generated.AiAgent;
import org.poc.objs.assetrepository.codegen.generated.AiAgentNode;
import org.poc.objs.assetrepository.codegen.generated.GeneratedReadView;
import org.poc.objs.assetrepository.codegen.generated.Guardrail;
import org.poc.objs.assetrepository.codegen.generated.GuardrailNode;
import org.poc.objs.assetrepository.codegen.generated.GraphMutationBuilder;
import tools.jackson.databind.json.JsonMapper;

class GeneratedAssetRepositoryBindingsTest {

    @Test
    void shouldBuildAndNavigateAllowedAssetRelations() {
        PayloadMapper mapper = new PayloadMapper(JsonMapper.builder().build());
        GraphMutationBuilder mutations = new GraphMutationBuilder(mapper);
        AiAgentNode agent = mutations.addAiAgent(new AiAgent().withName("support-agent"));
        GuardrailNode guardrail = mutations.addGuardrail(new Guardrail().withName("pii-protection"));

        mutations.protectedByGuardrail(agent, guardrail);
        GraphMutation mutation = mutations.build();
        assertThat(mutation.getMode()).isEqualTo(MutationMode.MERGE);
        assertThat(mutation.getEntities().getSet()).hasSize(2);
        Entity agentEntity = mutation.getEntities().getSet().stream()
                .filter(entity -> entity.getId().equals(agent.id()))
                .findFirst()
                .orElseThrow();
        assertThat(agentEntity.getType()).isEqualTo("AiAgent");
        assertThat(agentEntity.getSchemaVersion()).isEqualTo("1.0.0");
        assertThat(agentEntity.getPayload()).containsEntry("name", "support-agent");

        Graph graph = new Graph(
                new ArrayList<>(mutation.getEntities().getSet()),
                new ArrayList<>(mutation.getEdges().getSet()));

        GeneratedReadView view = GeneratedReadView.from(graph, mapper);

        assertThat(view.aiAgents()).hasSize(1);
        assertThat(view.aiAgents().get(0).getProtectedByGuardrails()).hasSize(1);
        assertThat(view.aiAgents().get(0).getProtectedByGuardrailEdges()).hasSize(1);

        Edge edge = mutation.getEdges().getSet().get(0);
        assertThat(edge.getSource()).isEqualTo(agent.id());
        assertThat(edge.getTarget()).isEqualTo(guardrail.id());
        assertThat(edge.getRole()).isEqualTo("PROTECTED_BY");
        assertThat(edge.getProperties()).isNull();
    }

    @Test
    void shouldBuildEntityAndEdgeDeletionMutation() {
        PayloadMapper mapper = new PayloadMapper(JsonMapper.builder().build());
        GraphMutationBuilder create = new GraphMutationBuilder(mapper);
        AiAgentNode agent = create.addAiAgent(new AiAgent().withName("support-agent"));
        GuardrailNode guardrail = create.addGuardrail(new Guardrail().withName("pii-protection"));
        Edge edge = create.protectedByGuardrail(agent, guardrail);
        create.build();

        GraphMutation deletion = new GraphMutationBuilder(mapper)
                .unsetEntity(agent.id())
                .unsetEdge(edge.getId())
                .build();

        assertThat(deletion.getMode()).isEqualTo(MutationMode.MERGE);
        assertThat(deletion.getEntities().getSet()).isEmpty();
        assertThat(deletion.getEntities().getUnset()).containsExactly(agent.id());
        assertThat(deletion.getEdges().getSet()).isEmpty();
        assertThat(deletion.getEdges().getUnset()).containsExactly(edge.getId());
    }
}
