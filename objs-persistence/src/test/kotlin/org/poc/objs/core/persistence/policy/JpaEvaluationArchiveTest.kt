package org.poc.objs.core.persistence.policy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.GraphContents
import org.poc.objs.core.persistence.ObjsPersistenceFixture
import org.poc.objs.policy.api.EvaluationResult
import org.poc.objs.policy.api.Finding
import org.poc.objs.policy.api.PersistContentAxes
import org.poc.objs.policy.api.PersistPresets
import org.poc.objs.policy.api.PersistResultFilters
import org.poc.objs.policy.api.PersistSpec
import org.poc.objs.policy.api.PolicyEvaluationKinds
import org.poc.objs.policy.api.PolicyEvaluationMeta
import org.poc.objs.policy.api.PolicyOutcome
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.SuiteEvaluationResult
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteFolderResult
import org.poc.objs.policy.api.SuitePolicyLeafResult
import java.util.UUID

class JpaEvaluationArchiveTest : ObjsPersistenceFixture() {

    @Test
    fun shouldReturnNull_whenEphemeralPreset() {
        val result =
            EvaluationResult(
                outcomes =
                    listOf(
                        PolicyOutcome("p", 1, "CUSTOM", PolicyOutcomeStatus.PASS),
                    ),
            )
        val id = evaluationArchives.saveFlat(result, PersistPresets.ephemeral())
        assertThat(id).isNull()
    }

    @Test
    fun shouldRoundTripFlatStandard_withLabelingAndFilters() {
        val entityId = UUID.randomUUID()
        val result =
            EvaluationResult(
                outcomes =
                    listOf(
                        PolicyOutcome(
                            policyName = "ok",
                            policySerial = 1,
                            engineKind = "CUSTOM",
                            status = PolicyOutcomeStatus.PASS,
                            findings = listOf(Finding("info", severity = "INFO")),
                        ),
                        PolicyOutcome(
                            policyName = "bad",
                            policySerial = 2,
                            engineKind = "CUSTOM",
                            status = PolicyOutcomeStatus.FAIL,
                            findings =
                                listOf(
                                    Finding("warn", severity = "WARNING", entities = listOf(entityId)),
                                    Finding("err", severity = "ERROR"),
                                ),
                        ),
                    ),
            )
        val spec =
            PersistPresets.standard().copy(
                name = "nightly",
                description = "demo run",
                tags = listOf("ci"),
                annotations = mapOf("ticket" to "T-1"),
                durationMs = 42L,
                filters =
                    PersistResultFilters(
                        outcomeStatuses = setOf(PolicyOutcomeStatus.FAIL),
                        findingSeverities = setOf("ERROR", "WARNING"),
                    ),
            )
        val context = mapOf("policies" to listOf(mapOf("key" to "bad", "serial" to 2)))
        val id =
            evaluationArchives.saveFlat(
                result = result,
                spec = spec,
                executionContext = context,
            )
        assertThat(id).isNotNull()

        val loaded = evaluationArchives.load(id!!)!!
        assertThat(loaded.kind).isEqualTo(PolicyEvaluationKinds.FLAT)
        assertThat(loaded.name).isEqualTo("nightly")
        assertThat(loaded.description).isEqualTo("demo run")
        assertThat(loaded.durationMs).isEqualTo(42L)
        assertThat(loaded.meta.tags).containsExactly("ci")
        assertThat(loaded.meta.annotations).containsEntry("ticket", "T-1")
        // Custom / programmatic policy set — no suite overlay (evaluation ≠ suite hard-link)
        assertThat(loaded.meta.suiteId).isNull()
        assertThat(loaded.meta.suiteName).isNull()
        assertThat(loaded.tree).isNull()
        assertThat(loaded.axes.results).isTrue()
        assertThat(loaded.axes.executionContext).isTrue()
        assertThat(loaded.axes.input).isFalse()
        assertThat(loaded.outcomes).hasSize(1)
        assertThat(loaded.outcomes.single().policyName).isEqualTo("bad")
        assertThat(loaded.outcomes.single().findings).extracting("severity").containsExactly("WARNING", "ERROR")
        assertThat(loaded.executionContext).isEqualTo(context)
        assertThat(loaded.input).isNull()
    }

    @Test
    fun shouldPersistProgrammaticFlat_withoutSuiteOverlay() {
        val result =
            EvaluationResult(
                outcomes =
                    listOf(
                        PolicyOutcome("selected-a", 1, "CUSTOM", PolicyOutcomeStatus.PASS),
                        PolicyOutcome("selected-b", 2, "CUSTOM", PolicyOutcomeStatus.FAIL),
                    ),
            )
        val id =
            evaluationArchives.saveFlat(
                result = result,
                spec =
                    PersistPresets.standard().copy(
                        name = "ad-hoc-policy-set",
                        tags = listOf("programmatic"),
                        annotations = mapOf("source" to "caller-selected-refs"),
                    ),
                executionContext = mapOf("policyRefs" to listOf("selected-a", "selected-b")),
            )!!

        val loaded = evaluationArchives.load(id)!!
        assertThat(loaded.kind).isEqualTo(PolicyEvaluationKinds.FLAT)
        assertThat(loaded.meta.suiteId).isNull()
        assertThat(loaded.meta.suiteName).isNull()
        assertThat(loaded.tree).isNull()
        assertThat(loaded.meta.tags).containsExactly("programmatic")
        assertThat(loaded.meta.annotations).containsEntry("source", "caller-selected-refs")
        assertThat(loaded.outcomes).extracting("policyName").containsExactly("selected-a", "selected-b")
        assertThat(loaded.executionContext).containsEntry("policyRefs", listOf("selected-a", "selected-b"))
    }

    @Test
    fun shouldOmitInput_whenLoadAsStandardOnFull() {
        val entityId = UUID.randomUUID()
        val fragment =
            GraphContents(
                entities = listOf(Entity(id = entityId, type = "Component", schemaVersion = "1")),
                edges = emptyList(),
            )
        val result =
            EvaluationResult(
                outcomes = listOf(PolicyOutcome("p", 1, "CUSTOM", PolicyOutcomeStatus.PASS)),
            )
        val id =
            evaluationArchives.saveFlat(
                result = result,
                spec = PersistPresets.full().copy(name = "audit"),
                executionContext = mapOf("note" to "ctx"),
                input = fragment,
            )!!

        val full = evaluationArchives.load(id)!!
        assertThat(full.input).isNotNull()
        assertThat(full.input!!.entities).hasSize(1)
        assertThat(full.executionContext).containsEntry("note", "ctx")

        val standard = evaluationArchives.loadAsStandard(id)!!
        assertThat(standard.input).isNull()
        assertThat(standard.executionContext).containsEntry("note", "ctx")
        assertThat(standard.outcomes).hasSize(1)
    }

    @Test
    fun shouldRoundTripSuiteTree() {
        val folderId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        val outcomes =
            listOf(
                PolicyOutcome("leaf-policy", 3, "DROOLS", PolicyOutcomeStatus.FAIL),
            )
        val tree =
            SuiteFolderResult(
                folderId = folderId,
                key = "root",
                name = "Root",
                participation = SuiteFolderParticipation.ENABLED,
                status = PolicyOutcomeStatus.FAIL,
                votes = true,
                leaves =
                    listOf(
                        SuitePolicyLeafResult(
                            policyId = policyId,
                            policyName = "leaf-policy",
                            policySerial = 3,
                            policyVersion = "1.0",
                            status = PolicyOutcomeStatus.FAIL,
                            outcomeIndex = 0,
                        ),
                    ),
            )
        val suiteResult =
            SuiteEvaluationResult(
                meta =
                    PolicyEvaluationMeta(
                        evaluationId = UUID.randomUUID(),
                        kind = PolicyEvaluationKinds.SUITE,
                        evaluatedAtEpochMs = System.currentTimeMillis(),
                        suiteId = UUID.randomUUID(),
                        suiteName = "demo-suite",
                        overallStatus = PolicyOutcomeStatus.FAIL,
                    ),
                tree = tree,
                outcomes = outcomes,
            )
        val id = evaluationArchives.saveSuite(suiteResult, PersistPresets.standard())!!
        val loaded = evaluationArchives.load(id)!!
        assertThat(loaded.kind).isEqualTo(PolicyEvaluationKinds.SUITE)
        assertThat(loaded.meta.suiteId).isEqualTo(suiteResult.meta.suiteId)
        assertThat(loaded.meta.suiteName).isEqualTo("demo-suite")
        assertThat(loaded.tree).isNotNull()
        assertThat(loaded.tree!!.key).isEqualTo("root")
        assertThat(loaded.tree!!.leaves).hasSize(1)
        assertThat(loaded.outcomes.single().policyName).isEqualTo("leaf-policy")
    }

    @Test
    fun shouldPersistResultsOnly_whenCustomAxes() {
        val result =
            EvaluationResult(
                outcomes = listOf(PolicyOutcome("p", 1, "CUSTOM", PolicyOutcomeStatus.PASS)),
            )
        val id =
            evaluationArchives.saveFlat(
                result = result,
                spec =
                    PersistSpec(
                        axes = PersistContentAxes(results = true),
                        name = "results-only",
                    ),
                executionContext = mapOf("ignored" to true),
            )!!
        val loaded = evaluationArchives.load(id)!!
        assertThat(loaded.axes.results).isTrue()
        assertThat(loaded.axes.executionContext).isFalse()
        assertThat(loaded.executionContext).isNull()
        assertThat(loaded.outcomes).hasSize(1)
    }
}
