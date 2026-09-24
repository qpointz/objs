package org.poc.objs.assetrepository.perf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.poc.objs.assetrepository.service.CollectionService;
import org.poc.objs.core.persistence.NamedGraphStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("perf")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ar_perf_filler;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "ar.perf.graphs=2",
        "ar.perf.objects=10",
        "ar.perf.edges=6",
        "ar.perf.batch-size=4",
        "ar.perf.versions=1",
        "ar.perf.collection=perf-noise"
})
class PerfDataFillerTest {

    @Autowired
    CollectionService collections;

    @Autowired
    PerfDataFiller filler;

    @Autowired
    NamedGraphStore namedGraphs;

    @Autowired
    PerfProperties props;

    @Test
    void shouldDistributeAcrossGraphs_andTopUp() {
        assertThat(PerfDataFiller.share(10, 2, 0)).isEqualTo(5);
        assertThat(PerfDataFiller.share(10, 2, 1)).isEqualTo(5);
        assertThat(PerfDataFiller.collectionName("perf-noise", 2, 0)).isEqualTo("perf-noise-0");

        var g0 = collections.requireByName("perf-noise-0");
        var g1 = collections.requireByName("perf-noise-1");
        assertThat(collections.objectCount(g0)).isEqualTo(5);
        assertThat(collections.objectCount(g1)).isEqualTo(5);
        assertThat(collections.edgeCount(g0)).isEqualTo(3);
        assertThat(collections.edgeCount(g1)).isEqualTo(3);
        assertThat(namedGraphs.listGraphVersions(g0.getGraphId())).hasSize(1);
        assertThat(namedGraphs.listGraphVersions(g1.getGraphId())).hasSize(1);
        assertThat(g0.getGraphId()).isNotEqualTo(g1.getGraphId());

        filler.run(null);
        assertThat(collections.objectCount(g0)).isEqualTo(5);

        props.setObjects(14);
        props.setEdges(8);
        props.setVersions(2);
        filler.run(null);

        // 14 objects → 7+7; 8 edges → 4+4; versions 2 each
        assertThat(collections.objectCount(collections.requireByName("perf-noise-0"))).isEqualTo(7);
        assertThat(collections.objectCount(collections.requireByName("perf-noise-1"))).isEqualTo(7);
        assertThat(collections.edgeCount(collections.requireByName("perf-noise-0"))).isEqualTo(4);
        assertThat(collections.edgeCount(collections.requireByName("perf-noise-1"))).isEqualTo(4);
        assertThat(namedGraphs.listGraphVersions(
                collections.requireByName("perf-noise-0").getGraphId())).hasSize(2);
    }
}
