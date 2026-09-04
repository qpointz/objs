package org.poc.objs.policy.drools

import org.drools.model.codegen.ExecutableModelProject
import org.kie.api.KieServices
import org.kie.api.builder.Message
import org.kie.api.builder.ReleaseId
import org.kie.api.runtime.KieContainer
import org.poc.objs.policy.api.Policy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Compiles and caches [KieContainer]s keyed by single policy revision (G-P20).
 */
class PolicyKnowledgeBaseCache(
    private val kieServices: KieServices = KieServices.Factory.get(),
) {
    private val containers = ConcurrentHashMap<UUID, KieContainer>()

    fun containerFor(policy: Policy): KieContainer =
        containers.computeIfAbsent(policy.id) { compile(policy) }

    /** Drop a cached revision (e.g. tests). */
    fun invalidate(policyId: UUID) {
        containers.remove(policyId)
    }

    fun clear() {
        containers.clear()
    }

    fun size(): Int = containers.size

    /**
     * Compile [policy.body] without caching. Empty list = success; otherwise Drools ERROR texts
     * with optional line/column from the builder [Message] API.
     */
    fun tryCompile(policy: Policy): List<DroolsCompileIssue> {
        val releaseId = kieServices.newReleaseId(
            "org.poc.objs.policy.drools",
            "validate-${policy.id}-${System.nanoTime()}",
            policy.version.toString(),
        )
        val kfs = kieServices.newKieFileSystem()
        kfs.generateAndWritePomXML(releaseId)
        val moduleModel = kieServices.newKieModuleModel()
        val baseModel = moduleModel.newKieBaseModel("defaultKieBase").setDefault(true)
        baseModel.newKieSessionModel("defaultKieSession").setDefault(true)
        kfs.writeKModuleXML(moduleModel.toXML())
        kfs.write("src/main/resources/rules/policy.drl", policy.body)

        val builder = kieServices.newKieBuilder(kfs).buildAll(ExecutableModelProject::class.java)
        return builder.results.getMessages(Message.Level.ERROR).map { it.toCompileIssue() }
    }

    private fun compile(policy: Policy): KieContainer {
        val releaseId = releaseIdFor(policy)
        val kfs = kieServices.newKieFileSystem()
        kfs.generateAndWritePomXML(releaseId)

        val moduleModel = kieServices.newKieModuleModel()
        val baseModel = moduleModel.newKieBaseModel("defaultKieBase").setDefault(true)
        baseModel.newKieSessionModel("defaultKieSession").setDefault(true)
        kfs.writeKModuleXML(moduleModel.toXML())
        kfs.write("src/main/resources/rules/policy.drl", policy.body)

        val builder = kieServices.newKieBuilder(kfs).buildAll(ExecutableModelProject::class.java)
        val errors = builder.results.getMessages(Message.Level.ERROR).map { it.toCompileIssue() }
        if (errors.isNotEmpty()) {
            val detail = errors.joinToString("; ") { it.display() }
            throw IllegalStateException(
                "Drools compile failed for policy '${policy.name}'@${policy.version}: $detail",
            )
        }
        return kieServices.newKieContainer(releaseId)
    }

    private fun releaseIdFor(policy: Policy): ReleaseId =
        kieServices.newReleaseId(
            "org.poc.objs.policy.drools",
            "policy-${policy.id}",
            policy.version.toString(),
        )
}
