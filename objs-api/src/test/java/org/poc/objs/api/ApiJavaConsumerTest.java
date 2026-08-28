package org.poc.objs.api;

import org.junit.jupiter.api.Test;
import org.poc.objs.api.domain.Entity;
import org.poc.objs.api.domain.EntityMutation;
import org.poc.objs.api.domain.EdgeMutation;
import org.poc.objs.api.domain.GraphMutation;
import org.poc.objs.api.domain.MutationMode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ApiJavaConsumerTest {
    @Test
    void shouldCompileAgainstCallerSuppliedJacksonMapper() {
        Class<? extends ObjectMapper> mapperType = ObjectMapper.class;

        assertThat(mapperType).isEqualTo(ObjectMapper.class);
    }

    @Test
    void shouldConstructReplaceMutationFromJava() {
        Entity entity = new Entity(
                null,
                "Person",
                "1",
                new java.util.HashMap<>(),
                new java.util.HashMap<>(),
                null,
                null,
                null
        );
        EntityMutation entities = new EntityMutation();
        entities.getSet().add(entity);
        GraphMutation mutation = new GraphMutation(
                entities,
                new EdgeMutation(),
                MutationMode.REPLACE
        );

        assertThat(mutation.getMode()).isEqualTo(MutationMode.REPLACE);
        assertThat(mutation.getEntities().getSet()).containsExactly(entity);
    }
}
