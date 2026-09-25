package org.poc.objs.sbom.codegen

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.typed.PayloadMapper
import org.poc.objs.sbom.codegen.generated.CanonicalEdge
import org.poc.objs.sbom.codegen.generated.Dataset
import org.poc.objs.sbom.codegen.generated.DatasetNode
import org.poc.objs.sbom.codegen.generated.EdgeCatalog
import org.poc.objs.sbom.codegen.generated.EntityCatalog
import tools.jackson.databind.json.JsonMapper

/** Minimal smoke: regenerated SBOM bindings expose EntityCatalog / EdgeCatalog (G-W7). */
class GeneratedSbomEntityCatalogSmokeTest {

    private val mapper = PayloadMapper(JsonMapper.builder().build())

    @Test
    fun shouldConvertDatasetViaEntityCatalog() {
        val payload = Dataset().withName("orders").withDatasetType("table")
        assertThat(EntityCatalog.supports(Dataset::class.java)).isTrue

        val node = EntityCatalog.toNode(payload)
        assertThat(node).isInstanceOf(DatasetNode::class.java)
        assertThat((node as DatasetNode).payload().name).isEqualTo("orders")

        val entity = EntityCatalog.toEntity(payload, mapper)
        assertThat(entity.type).isEqualTo("Dataset")
        assertThat(entity.payload).containsEntry("name", "orders")
    }

    @Test
    fun shouldExposeCanonicalEdgeInEdgeCatalog() {
        assertThat(EdgeCatalog.supports(CanonicalEdge::class.java)).isTrue
        assertThat(EdgeCatalog.meta(CanonicalEdge::class.java).type).isEqualTo("CanonicalEdge")
    }
}
