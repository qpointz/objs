package org.poc.objs.api.match

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.validation.ValidationException
import java.util.UUID

class ObjExprMatcherTest {
    @Test
    fun shouldMatchAcrossNamespaces() {
        val id = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val matcher = ObjExprMatcher(
            "type == 'Product' && p.name == 'App' && a.env == 'prod' && id == '11111111-1111-4111-8111-111111111111'",
        )
        val hit = EntityDomainCandidate(
            Entity(
                id = id,
                type = "Product",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "App"),
                annotations = mutableMapOf("env" to "prod"),
            ),
        )
        val miss = EntityDomainCandidate(
            Entity(
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
        val matcher = ObjExprMatcher(
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
        val matcher = ObjExprMatcher(
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
        val matcher = ObjExprMatcher("type == 'A' && type == 'B'")
        assertThat(matcher.pushdown!!.isUnsatisfiable).isTrue()
    }

    @Test
    fun shouldNotLowerUnsupportedShapes() {
        val matcher = ObjExprMatcher("a.env > 'a'")
        assertThat(matcher.localEvalOnly).isTrue()
        assertThat(matcher.pushdown).isNull()
    }

    @Test
    fun shouldLowerPayloadCompareAndPrefix() {
        val compare = ObjExprMatcher("type == 'Component' && p.version > '2.0'")
        assertThat(compare.localEvalOnly).isFalse()
        assertThat(compare.pushdown!!.dnf.single().payloadGt).isEqualTo(mapOf("version" to "2.0"))

        val prefix = ObjExprMatcher("type == 'Component' && p.name =~ '^Apache'")
        assertThat(prefix.localEvalOnly).isFalse()
        assertThat(prefix.pushdown!!.dnf.single().payloadPrefix).isEqualTo(mapOf("name" to "Apache"))
    }

    @Test
    fun shouldNotLowerUnanchoredRegexForPushdown() {
        val matcher = ObjExprMatcher("p.name =~ \"Apache\"")
        assertThat(matcher.localEvalOnly).isTrue()
        assertThat(matcher.pushdown).isNull()
    }

    @Test
    fun shouldMatchCombinedTypeAndAnchoredPrefix() {
        val matcher = ObjExprMatcher("type == 'Component' && p.name =~ '^Apache'")
        val hit = EntityDomainCandidate(
            Entity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Apache Kafka"),
            ),
        )
        assertThat(matcher.matches(hit)).isTrue()
        assertThat(matcher.localEvalOnly).isFalse()
    }

    @Test
    fun shouldMatchAnchoredPrefixRegex() {
        val matcher = ObjExprMatcher("p.name =~ '^Apache'")
        val hit = EntityDomainCandidate(
            Entity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Apache Kafka"),
            ),
        )
        val miss = EntityDomainCandidate(
            Entity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Log4j"),
            ),
        )
        assertThat(matcher.matches(hit)).isTrue()
        assertThat(matcher.matches(miss)).isFalse()
    }

    @Test
    fun shouldMatchRegexAsSubstring() {
        val matcher = ObjExprMatcher("p.name =~ \"Apache\"")
        val hit = EntityDomainCandidate(
            Entity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Apache Kafka"),
            ),
        )
        val exact = EntityDomainCandidate(
            Entity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Apache"),
            ),
        )
        val miss = EntityDomainCandidate(
            Entity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("name" to "Log4j"),
            ),
        )
        assertThat(matcher.matches(hit)).isTrue()
        assertThat(matcher.matches(exact)).isTrue()
        assertThat(matcher.matches(miss)).isFalse()
        val anchored = ObjExprMatcher("p.name =~ '^Apache$'")
        assertThat(anchored.matches(hit)).isFalse()
        assertThat(anchored.matches(exact)).isTrue()
    }

    @Test
    fun shouldAcceptDoubleQuotedPayloadEquality() {
        val matcher = ObjExprMatcher("p.ecosystem == \"Maven\"")
        val hit = EntityDomainCandidate(
            Entity(
                type = "Component",
                schemaVersion = "1.0.0",
                payload = mutableMapOf("ecosystem" to "Maven"),
            ),
        )
        val miss = EntityDomainCandidate(
            Entity(
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
        ObjExprMatcher(expr)
    }

    @Test
    fun shouldRejectBlank() {
        assertThatThrownBy { ObjExprMatcher("  ") }
            .isInstanceOf(ValidationException::class.java)
    }
}
