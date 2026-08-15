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
        assertThat(plan.dnf).hasSize(1)
        assertThat(plan.typeEquals).isEqualTo("Dataset")
        assertThat(plan.idEquals).isEqualTo(id)
        assertThat(plan.annotationEquals).isEqualTo(mapOf("env" to "prod"))
        assertThat(plan.payloadEquals).isEqualTo(mapOf("datasetType" to "table"))
    }

    @Test
    fun shouldLowerOrAndNotEquals() {
        val matcher = BoMObjExprMatcher(
            "type == 'A' || type == 'B' || (type != 'Policy' && a.env != 'test')",
        )
        assertThat(matcher.localEvalOnly).isFalse()
        val plan = matcher.pushdown!!
        assertThat(plan.dnf).hasSize(3)
        assertThat(plan.dnf.map { it.typeEquals }).containsExactlyInAnyOrder("A", "B", null)
        val neGroup = plan.dnf.first { it.typeEquals == null }
        assertThat(neGroup.typeNotEquals).containsExactly("Policy")
        assertThat(neGroup.annotationNotEquals).isEqualTo(mapOf("env" to "test"))
    }

    @Test
    fun shouldLowerUnsatisfiableAndToEmptyDnf() {
        val matcher = BoMObjExprMatcher("type == 'A' && type == 'B'")
        assertThat(matcher.pushdown!!.isUnsatisfiable).isTrue()
    }

    @Test
    fun shouldNotLowerUnsupportedShapes() {
        val matcher = BoMObjExprMatcher("a.env > 'a'")
        assertThat(matcher.localEvalOnly).isTrue()
        assertThat(matcher.pushdown).isNull()
    }

    @Test
    fun shouldMatchRegexAsSubstring() {
        val matcher = BoMObjExprMatcher("p.name =~ \"Apache\"")
        val hit = BoMEntityDomainCandidate(
            BoMEntity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Apache Kafka"),
            ),
        )
        val exact = BoMEntityDomainCandidate(
            BoMEntity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Apache"),
            ),
        )
        val miss = BoMEntityDomainCandidate(
            BoMEntity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Log4j"),
            ),
        )
        assertThat(matcher.matches(hit)).isTrue()
        assertThat(matcher.matches(exact)).isTrue()
        assertThat(matcher.matches(miss)).isFalse()
        val anchored = BoMObjExprMatcher("p.name =~ '^Apache$'")
        assertThat(anchored.matches(hit)).isFalse()
        assertThat(anchored.matches(exact)).isTrue()
    }

    @Test
    fun shouldAcceptDoubleQuotedPayloadEquality() {
        val matcher = BoMObjExprMatcher("p.ecosystem == \"Maven\"")
        val hit = BoMEntityDomainCandidate(
            BoMEntity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("ecosystem" to "Maven"),
            ),
        )
        val miss = BoMEntityDomainCandidate(
            BoMEntity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("ecosystem" to "npm"),
            ),
        )
        assertThat(matcher.matches(hit)).isTrue()
        assertThat(matcher.matches(miss)).isFalse()
    }

    @Test
    fun shouldCompilePayloadExprAndedWithManyTypeEquals() {
        val types = listOf(
            "API", "Artifact", "Build", "Component", "Container Image", "Container Layer",
            "Database", "Dataset", "Deployment", "Environment", "Host", "Kubernetes Cluster",
            "License", "Namespace", "Operating System", "Organization", "Policy", "Product",
            "Runtime", "Service", "Source Module", "Source Repository", "Vulnerability",
        )
        val typeScope = types.joinToString(" || ") { "type == '$it'" }
        val expr = "($typeScope) && (p.ecosystem == \"Maven\")"
        BoMObjExprMatcher(expr)
    }

    @Test
    fun shouldRejectBlank() {
        assertThatThrownBy { BoMObjExprMatcher("  ") }
            .isInstanceOf(BoMValidationException::class.java)
    }
}
