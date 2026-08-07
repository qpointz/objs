package org.poc.objs.core.match

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMEntity
import org.poc.objs.core.validation.BoMValidationException
import java.util.UUID

class BoMObjExprMatcherTest {
    @Test
    fun shouldMatchAcrossNamespaces() {
        val id = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val matcher = BoMObjExprMatcher(
            "type == 'Product' && p.name == 'App' && a.env == 'prod' && id == '11111111-1111-4111-8111-111111111111'",
        )
        val hit = BoMEntityDomainCandidate(
            BoMEntity(
                id = id,
                type = "Product",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "App"),
                annotations = mutableMapOf("env" to "prod"),
            ),
        )
        val miss = BoMEntityDomainCandidate(
            BoMEntity(
                id = id,
                type = "Product",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Other"),
                annotations = mutableMapOf("env" to "prod"),
            ),
        )
        assertThat(matcher.matches(hit)).isTrue()
        assertThat(matcher.matches(miss)).isFalse()
    }

    @Test
    fun shouldLowerEqualityAndTree() {
        val id = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val matcher = BoMObjExprMatcher(
            "type == 'Dataset' && a.env == 'prod' && p.datasetType == 'table' && id == '$id'",
        )
        assertThat(matcher.localEvalOnly).isFalse()
        val plan = matcher.pushdown!!
        assertThat(plan.typeEquals).isEqualTo("Dataset")
        assertThat(plan.idEquals).isEqualTo(id)
        assertThat(plan.annotationEquals).isEqualTo(mapOf("env" to "prod"))
        assertThat(plan.payloadEquals).isEqualTo(mapOf("datasetType" to "table"))
    }

    @Test
    fun shouldNotLowerOrExpressions() {
        val matcher = BoMObjExprMatcher("type == 'A' || type == 'B'")
        assertThat(matcher.localEvalOnly).isTrue()
        assertThat(matcher.pushdown).isNull()
    }

    @Test
    fun shouldRejectBlank() {
        assertThatThrownBy { BoMObjExprMatcher("  ") }
            .isInstanceOf(BoMValidationException::class.java)
    }
}
