package org.poc.objs.sbom.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RelationLabelsTest {
    @Test
    fun shouldBeautifyRoleCodes() {
        assertThat(RelationLabels.display("DEPENDS_ON")).isEqualTo("Depends On")
        assertThat(RelationLabels.display("HAS_VULNERABILITY")).isEqualTo("Has Vulnerability")
        assertThat(RelationLabels.display("OWNED_BY")).isEqualTo("Owned By")
    }
}
