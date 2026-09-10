package org.poc.objs.policy.core

import org.poc.objs.api.domain.GraphFragment
import org.poc.objs.policy.api.CategoryRepository
import org.poc.objs.policy.api.Policy
import org.poc.objs.policy.api.PolicyEvaluationKinds
import org.poc.objs.policy.api.PolicyEvaluationMeta
import org.poc.objs.policy.api.PolicyEvaluator
import org.poc.objs.policy.api.PolicyOutcome
import org.poc.objs.policy.api.PolicyOutcomeStatus
import org.poc.objs.policy.api.PolicyRef
import org.poc.objs.policy.api.PolicyRepository
import org.poc.objs.policy.api.PolicySuite
import org.poc.objs.policy.api.RollUpChild
import org.poc.objs.policy.api.SuiteEffectiveSet
import org.poc.objs.policy.api.SuiteEvaluateScope
import org.poc.objs.policy.api.SuiteEvaluationResult
import org.poc.objs.policy.api.SuiteEvaluator
import org.poc.objs.policy.api.SuiteFolder
import org.poc.objs.policy.api.SuiteFolderParticipation
import org.poc.objs.policy.api.SuiteFolderResult
import org.poc.objs.policy.api.SuitePolicyLeafResult
import org.poc.objs.policy.api.SuiteSelectionException
import org.poc.objs.policy.api.SuiteStrategy
import java.util.UUID

/**
 * C-27 suite evaluate: expand matchers → [ExecutionStrategy] → flat [PolicyEvaluator.evaluate]
 * → map onto tree → [SuiteRollUpStrategy]. Drops N/A leaves (G-P33). No input persist.
 */
class DefaultSuiteEvaluator(
    private val policyEvaluator: PolicyEvaluator,
    private val policies: PolicyRepository,
    private val categories: CategoryRepository,
    private val expander: SuiteMatcherExpander = SuiteMatcherExpander(policies, categories),
) : SuiteEvaluator {

    override fun evaluateSuite(
        fragment: GraphFragment,
        suite: PolicySuite,
        scope: SuiteEvaluateScope,
        evaluationId: UUID,
        evaluatedAtEpochMs: Long,
    ): SuiteEvaluationResult {
        val selection = expander.resolveSelection(suite, scope)
        val strategy = SuiteStrategies.suite(suite.resolvedSuiteStrategyKind())
        val execution = strategy.selectExecution(suite.executionStrategyKind)
        val toRun = execution.selectForEvaluate(selection.policies)
        val flat = if (toRun.isEmpty()) {
            org.poc.objs.policy.api.EvaluationResult(emptyList(), PolicyOutcomeStatus.NOT_APPLICABLE)
        } else {
            policyEvaluator.evaluate(
                fragment,
                toRun.map { PolicyRef.ById(it.id) },
            )
        }
        // Key outcomes by policy id (dedupe path) and by (name, serial) for leaf citation.
        val outcomesById = linkedMapOf<UUID, IndexedOutcome>()
        for ((idx, outcome) in flat.outcomes.withIndex()) {
            val policy = toRun.getOrNull(idx)
                ?: policies.list().find { it.name == outcome.policyName && it.serial == outcome.policySerial }
            if (policy != null) {
                outcomesById[policy.id] = IndexedOutcome(idx, outcome, policy)
            }
        }

        val byId = suite.folders.associateBy { it.id }
        val rootFolderIds = rootFolderIdsForScope(suite, scope, byId)

        val roots = rootFolderIds.mapNotNull { fid ->
            buildFolderResult(
                folder = byId.getValue(fid),
                byId = byId,
                selectedIds = selection.policies.map { it.id }.toSet(),
                outcomesById = outcomesById,
                strategy = strategy,
            )
        }

        val tree = when {
            roots.isEmpty() -> null
            roots.size == 1 -> roots.single()
            else -> synthesizeMultiRoot(
                roots = roots,
                suite = suite,
                strategy = strategy,
            )
        }

        val outcomes = flat.outcomes.filter { it.status != PolicyOutcomeStatus.NOT_APPLICABLE }
        // Re-index leaf outcomeIndex against filtered list
        val filteredIndex = outcomes.mapIndexed { i, o -> (o.policyName to o.policySerial) to i }.toMap()
        val remappedTree = tree?.let { remapOutcomeIndexes(it, filteredIndex) }

        return SuiteEvaluationResult(
            meta = PolicyEvaluationMeta(
                evaluationId = evaluationId,
                kind = PolicyEvaluationKinds.SUITE,
                evaluatedAtEpochMs = evaluatedAtEpochMs,
                executionStrategyKind = suite.executionStrategyKind,
                rollUpStrategyKind = suite.rollUpStrategyKind,
                overallStatus = remappedTree?.status,
                overallSeverity = remappedTree?.severity,
                tags = suite.tags,
                annotations = suite.annotations,
                suiteId = suite.id,
                suiteName = suite.name,
            ),
            tree = remappedTree,
            outcomes = outcomes,
        )
    }

    override fun effectivePolicies(suite: PolicySuite, folderId: UUID): SuiteEffectiveSet =
        expander.effectivePolicies(suite, folderId)

    override fun resolveSelection(suite: PolicySuite, scope: SuiteEvaluateScope): SuiteEffectiveSet =
        expander.resolveSelection(suite, scope)

    private fun rootFolderIdsForScope(
        suite: PolicySuite,
        scope: SuiteEvaluateScope,
        byId: Map<UUID, SuiteFolder>,
    ): List<UUID> =
        when (scope) {
            is SuiteEvaluateScope.Full ->
                listOfNotNull(suite.folders.find { it.parentId == null }?.id)
            is SuiteEvaluateScope.Subfolder -> listOf(scope.folderId)
            is SuiteEvaluateScope.SubfolderSet -> scope.folderIds
            is SuiteEvaluateScope.PolicySubset ->
                listOfNotNull(suite.folders.find { it.parentId == null }?.id)
        }.also { ids ->
            for (id in ids) {
                if (!byId.containsKey(id)) {
                    throw IllegalArgumentException("Unknown folderId: $id")
                }
            }
        }

    private fun buildFolderResult(
        folder: SuiteFolder,
        byId: Map<UUID, SuiteFolder>,
        selectedIds: Set<UUID>,
        outcomesById: Map<UUID, IndexedOutcome>,
        strategy: SuiteStrategy,
    ): SuiteFolderResult? {
        if (folder.participation == SuiteFolderParticipation.DISABLED) {
            return null
        }

        val childFolders = byId.values
            .filter { it.parentId == folder.id }
            .sortedBy { it.name.lowercase() }
            .mapNotNull { child ->
                buildFolderResult(child, byId, selectedIds, outcomesById, strategy)
            }

        // Direct leaves: policies from this folder's own matchers that were selected and ran.
        val direct = localMatcherPolicies(folder)
            .filter { it.id in selectedIds }
            .mapNotNull { policy ->
                val indexed = outcomesById[policy.id] ?: return@mapNotNull null
                if (indexed.outcome.status == PolicyOutcomeStatus.NOT_APPLICABLE) {
                    return@mapNotNull null
                }
                SuitePolicyLeafResult(
                    policyId = policy.id,
                    policyName = indexed.outcome.policyName,
                    policySerial = indexed.outcome.policySerial,
                    policyVersion = policy.version,
                    status = indexed.outcome.status,
                    severity = strategy.leafReportedSeverity(indexed.outcome),
                    outcomeIndex = indexed.index,
                )
            }

        val votingChildren = mutableListOf<RollUpChild>()
        for (child in childFolders) {
            if (child.votes) {
                votingChildren += RollUpChild(child.status, child.severity)
            }
        }
        for (leaf in direct) {
            votingChildren += RollUpChild(leaf.status, leaf.severity)
        }

        val rolled = strategy.rollUp(folder, votingChildren)
        val votes = folder.participation == SuiteFolderParticipation.ENABLED

        return SuiteFolderResult(
            folderId = folder.id,
            key = folder.key,
            name = folder.name,
            parentFolderId = folder.parentId,
            participation = folder.participation,
            status = rolled.status,
            severity = rolled.severity,
            votes = votes,
            tags = folder.tags,
            annotations = folder.annotations,
            children = childFolders,
            leaves = direct,
        )
    }

    private fun localMatcherPolicies(folder: SuiteFolder): List<Policy> {
        // Apply own matchers on empty accumulator (children merged separately in tree).
        val stub = PolicySuite(
            id = UUID.randomUUID(),
            key = "_",
            name = "_",
            folders = listOf(folder.copy(parentId = null, participation = SuiteFolderParticipation.ENABLED)),
        )
        return try {
            expander.effectivePolicies(stub, folder.id).policies
        } catch (ex: SuiteSelectionException) {
            throw ex
        }
    }

    private fun synthesizeMultiRoot(
        roots: List<SuiteFolderResult>,
        suite: PolicySuite,
        strategy: SuiteStrategy,
    ): SuiteFolderResult {
        val synthetic = SuiteFolder(
            id = UUID.randomUUID(),
            key = "_scope",
            name = suite.name,
            participation = SuiteFolderParticipation.ENABLED,
        )
        val voting = roots.filter { it.votes }.map { RollUpChild(it.status, it.severity) }
        val rolled = strategy.rollUp(synthetic, voting)
        return SuiteFolderResult(
            folderId = synthetic.id,
            key = synthetic.key,
            name = synthetic.name,
            participation = SuiteFolderParticipation.ENABLED,
            status = rolled.status,
            severity = rolled.severity,
            votes = true,
            children = roots,
        )
    }

    private fun remapOutcomeIndexes(
        node: SuiteFolderResult,
        indexByKey: Map<Pair<String, Long>, Int>,
    ): SuiteFolderResult =
        node.copy(
            children = node.children.map { remapOutcomeIndexes(it, indexByKey) },
            leaves = node.leaves.map { leaf ->
                val idx = indexByKey[leaf.policyName to leaf.policySerial] ?: leaf.outcomeIndex
                leaf.copy(outcomeIndex = idx)
            },
        )

    private data class IndexedOutcome(
        val index: Int,
        val outcome: PolicyOutcome,
        val policy: Policy,
    )
}
