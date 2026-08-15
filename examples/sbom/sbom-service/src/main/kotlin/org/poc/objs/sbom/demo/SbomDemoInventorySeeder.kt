package org.poc.objs.sbom.demo

import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.domain.CreateDraftVersionRequest
import org.poc.objs.sbom.domain.CreateFingerprintRequest
import org.poc.objs.sbom.domain.CreatePoolAssetRequest
import org.poc.objs.sbom.domain.DraftRelationWrite
import org.poc.objs.sbom.domain.PlaceApplicationRequest
import org.poc.objs.sbom.domain.PromoteVersionRequest
import org.poc.objs.sbom.domain.ReplaceVersionBomRequest
import org.poc.objs.sbom.persistence.SbomApplicationRepository
import org.poc.objs.sbom.registry.SbomRoles
import org.poc.objs.sbom.service.ApplicationInventoryService
import org.poc.objs.sbom.service.ApplicationVersionService
import org.poc.objs.sbom.service.AssetInventoryService
import org.poc.objs.sbom.service.PortfolioService
import org.poc.objs.sbom.service.SbomService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Demo-profile inventory: 70 LOB applications, version lineages, shared Maven/npm/PyPI pins.
 * Portfolio taxonomy comes from `kind: Portfolio` seeds; this runner places apps.
 */
@Component
@Profile("demo")
@Order(1000)
class SbomDemoInventorySeeder(
    private val sbom: SbomService,
    private val applications: SbomApplicationRepository,
    private val inventory: ApplicationInventoryService,
    private val versions: ApplicationVersionService,
    private val assets: AssetInventoryService,
    private val portfolioService: PortfolioService,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (applications.count() > 0) {
            log.info("Demo inventory already present; skipping seed")
            return
        }
        sbom.ensureRegistry()
        require(SbomDemoApps.all.size == 70) { "Demo catalog must contain 70 applications" }

        val pool = SharedPool()
        for (spec in SbomDemoApps.all) {
            inventory.create(CreateApplicationRequest(spec.name, spec.description, id = spec.id))
            var lastReleasedId: UUID? = null
            spec.releasedVersions.forEachIndexed { index, versionName ->
                val draft = versions.draft(spec.id) ?: error("Missing draft for ${spec.name}")
                val generation = index.coerceAtMost(2)
                val (assetIds, rels) = assemble(spec, pool, versionName, generation)
                versions.replaceBom(spec.id, draft.id, ReplaceVersionBomRequest(assetIds, rels))
                val released = versions.promote(spec.id, draft.id, PromoteVersionRequest(versionName))
                lastReleasedId = released.version.id
                if (spec.fingerprint && index == spec.releasedVersions.lastIndex) {
                    versions.fingerprint(spec.id, released.version.id, CreateFingerprintRequest("Baseline demo fingerprint"))
                }
                val needAnother =
                    index < spec.releasedVersions.lastIndex || spec.openDraft
                if (needAnother) {
                    versions.createDraft(spec.id, CreateDraftVersionRequest(fromVersionId = released.version.id))
                }
            }
            if (spec.releasedVersions.isEmpty()) {
                val draft = versions.draft(spec.id) ?: error("Missing draft for ${spec.name}")
                val (assetIds, rels) = assemble(spec, pool, "0.1.0-SNAPSHOT", 0)
                versions.replaceBom(spec.id, draft.id, ReplaceVersionBomRequest(assetIds, rels))
                if (spec.fingerprint) {
                    versions.fingerprint(spec.id, draft.id, CreateFingerprintRequest("Baseline demo fingerprint"))
                }
            } else if (spec.openDraft && lastReleasedId != null && versions.draft(spec.id) == null) {
                versions.createDraft(spec.id, CreateDraftVersionRequest(fromVersionId = lastReleasedId))
            }
            portfolioService.placeApplication(
                SbomDemoIds.PORTFOLIO,
                PlaceApplicationRequest(applicationId = spec.id, subjectAreaId = spec.categoryId),
            )
        }

        log.info(
            "Demo inventory seeded: apps={}, shared pool assets, portfolio={}",
            SbomDemoApps.all.size,
            SbomDemoIds.PORTFOLIO,
        )
    }

    private inner class SharedPool {
        val meridian = org("Meridian Financial Group", "meridian.example", "https://www.meridian.example", "GB")
        val apache = org("Apache Software Foundation", "apache.org", "https://www.apache.org", "US")
        val vmware = org("VMware Tanzu", "tanzu.vmware.com", "https://tanzu.vmware.com", "US")
        val pythonOrg = org("Python Software Foundation", "python.org", "https://www.python.org", "US")
        val meta = org("Meta Platforms", "meta.com", "https://opensource.fb.com", "US")
        val google = org("Google LLC", "google.com", "https://opensource.google", "US")
        val redhat = org("Red Hat", "redhat.com", "https://www.redhat.com", "US")
        val postgresOrg = org("PostgreSQL Global Development Group", "postgresql.org", "https://www.postgresql.org", "US")
        val redisLtd = org("Redis Ltd", "redis.io", "https://redis.io", "US")

        val apache2 = license("Apache License 2.0", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0")
        val mit = license("MIT License", "MIT", "https://opensource.org/licenses/MIT")
        val bsd3 = license("BSD 3-Clause", "BSD-3-Clause", "https://opensource.org/licenses/BSD-3-Clause")
        val proprietary = license("Meridian Proprietary", "LicenseRef-Meridian", "https://legal.meridian.example/licenses/proprietary")

        val log4shell =
            vuln("Apache Log4j JNDI injection", "CVE-2021-44228", "CRITICAL", 10.0)
        val spring4shell =
            vuln("Spring Framework RCE via data binding", "CVE-2022-22965", "HIGH", 9.8)
        val http2reset =
            vuln("HTTP/2 Rapid Reset", "CVE-2023-44487", "HIGH", 7.5)

        private val componentCache = mutableMapOf<String, org.poc.objs.sbom.domain.AssetView>()

        fun lib(name: String, version: String, eco: String, kind: String, coordinates: String) =
            componentCache.getOrPut("$name|$version") {
                val coord =
                    when (eco) {
                        "Maven" -> "$coordinates:$version"
                        "PyPI" -> "$coordinates==$version"
                        else -> "$coordinates@$version"
                    }
                create(
                    "Component",
                    mapOf(
                        "name" to name,
                        "version" to version,
                        "ecosystem" to eco,
                        "kind" to kind,
                        "coordinates" to coord,
                    ),
                )
            }

        val jacksonDup =
            create(
                "Component",
                mapOf(
                    "name" to "Jackson Databind",
                    "version" to "2.21.4",
                    "ecosystem" to "Maven",
                    "kind" to "library",
                    "coordinates" to "com.fasterxml.jackson.core:jackson-databind:2.21.4",
                ),
            )
        val boot = { g: Int -> lib("Spring Boot", pin("3.3.2", "3.5.16", "4.1.0", g), "Maven", "framework", "org.springframework.boot:spring-boot") }
        val jackson = { g: Int -> lib("Jackson Databind", pin("2.17.2", "2.18.4", "2.21.4", g), "Maven", "library", "com.fasterxml.jackson.core:jackson-databind") }
        val security = { g: Int -> lib("Spring Security", pin("6.3.1", "6.5.3", "7.0.0", g), "Maven", "library", "org.springframework.security:spring-security-core") }
        val hibernate = { g: Int -> lib("Hibernate ORM", pin("6.5.2.Final", "6.6.15.Final", "7.4.1.Final", g), "Maven", "library", "org.hibernate.orm:hibernate-core") }
        val kafka = { g: Int -> lib("Apache Kafka Clients", pin("3.7.0", "3.9.0", "4.2.1", g), "Maven", "library", "org.apache.kafka:kafka-clients") }
        val log4j = { g: Int -> lib("Apache Log4j Core", pin("2.17.2", "2.24.3", "2.25.4", g), "Maven", "library", "org.apache.logging.log4j:log4j-core") }
        val guava = { g: Int -> lib("Guava", pin("33.2.1-jre", "33.4.0-jre", "33.4.8-jre", g), "Maven", "library", "com.google.guava:guava") }
        val django = { g: Int -> lib("Django", pin("5.0.6", "5.2.1", "6.1", g), "PyPI", "framework", "Django") }
        val fastapi = { g: Int -> lib("FastAPI", pin("0.111.0", "0.115.12", "0.141.1", g), "PyPI", "framework", "fastapi") }
        val numpy = { g: Int -> lib("NumPy", pin("1.26.4", "2.2.6", "2.5.2", g), "PyPI", "library", "numpy") }
        val pandas = { g: Int -> lib("pandas", pin("2.2.2", "2.3.3", "3.0.5", g), "PyPI", "library", "pandas") }
        val sklearn = { g: Int -> lib("scikit-learn", pin("1.5.0", "1.6.1", "1.9.0", g), "PyPI", "library", "scikit-learn") }
        val celery = { g: Int -> lib("Celery", pin("5.4.0", "5.5.1", "5.6.3", g), "PyPI", "library", "celery") }
        val sqlalchemy = { g: Int -> lib("SQLAlchemy", pin("2.0.30", "2.0.40", "2.0.52", g), "PyPI", "library", "SQLAlchemy") }
        val react = { g: Int -> lib("React", pin("18.3.1", "19.0.0", "19.2.8", g), "npm", "library", "react") }
        val vue = { g: Int -> lib("Vue.js", pin("3.4.27", "3.5.13", "3.5.18", g), "npm", "framework", "vue") }
        val next = { g: Int -> lib("Next.js", pin("14.2.3", "15.3.2", "16.3.1", g), "npm", "framework", "next") }
        val axios = { g: Int -> lib("axios", pin("1.7.2", "1.8.4", "1.19.0", g), "npm", "library", "axios") }
        val typescript = { g: Int -> lib("TypeScript", pin("5.4.5", "5.8.3", "5.9.2", g), "npm", "library", "typescript") }

        val temurin = runtime("Eclipse Temurin", "JRE", "21.0.3")
        val cpython = runtime("CPython", "interpreter", "3.12.3")
        val node = runtime("Node.js", "runtime", "20.14.0")
        val ubuntu = os("Ubuntu LTS", "Ubuntu", "22.04", "amd64")
        val rhel = os("Red Hat Enterprise Linux", "RHEL", "9.4", "amd64")
        val alpine = os("Alpine Linux", "Alpine", "3.19.1", "amd64")
        val prodCluster = k8s("meridian-prod-eks", "1.29.4")
        val nonprodCluster = k8s("meridian-nonprod-eks", "1.28.9")
        val prod = env("Production", "prod")
        val staging = env("Staging", "staging")
        val development = env("Development", "dev")
        val prodHost = host("eks-prod-node-a", "ip-10-12-4-21.eu-west-1.compute.internal", "10.12.4.21", "AWS")
        val stagingHost = host("eks-staging-node-a", "ip-10-22-8-14.eu-west-1.compute.internal", "10.22.8.14", "AWS")
        val nsPayments = ns("payments", "payments")
        val nsDigital = ns("digital", "digital")
        val nsWealth = ns("wealth", "wealth")
        val nsInsurance = ns("insurance", "insurance")
        val nsCorp = ns("corporate-banking", "corporate-banking")
        val nsData = ns("data-analytics", "data-analytics")
        val nsPlatform = ns("platform", "platform")
        val nsCorpFn = ns("corporate", "corporate")
        val pci = policy("PCI-DSS v4 Control Set", "compliance", "4.0")
        val oss = policy("Open Source Review Policy", "oss-governance", "2024.2")
        val residency = policy("EU Data Residency", "privacy", "1.3")
        val datasets =
            SbomDemoDatasets.all.map { spec ->
                create(
                    "Dataset",
                    mapOf(
                        "name" to spec.name,
                        "datasetType" to spec.datasetType,
                        "classification" to spec.classification,
                        "description" to spec.description,
                    ),
                )
            }

        fun datasetsFor(app: DemoAppSpec): List<org.poc.objs.sbom.domain.AssetView> {
            val apps = SbomDemoApps.all
            val appIndex = apps.indexOfFirst { it.id == app.id }.coerceAtLeast(0)
            val assigned = datasets.filterIndexed { i, _ -> i % apps.size == appIndex }
            val byCategory =
                SbomDemoDatasets.all.mapIndexedNotNull { i, spec ->
                    if (spec.categoryId == app.categoryId) datasets[i] else null
                }
            val extra =
                if (app.includeDataset) byCategory.take(8) else byCategory.take(2)
            return (assigned + extra).distinctBy { it.id }
        }

        fun namespaceFor(categoryId: UUID) =
            when (categoryId) {
                SbomDemoIds.CATEGORY_PAYMENTS, SbomDemoIds.CATEGORY_RETAIL -> nsPayments
                SbomDemoIds.CATEGORY_DIGITAL -> nsDigital
                SbomDemoIds.CATEGORY_WEALTH -> nsWealth
                SbomDemoIds.CATEGORY_INSURANCE -> nsInsurance
                SbomDemoIds.CATEGORY_CORPORATE_BANKING -> nsCorp
                SbomDemoIds.CATEGORY_DATA -> nsData
                SbomDemoIds.CATEGORY_PLATFORM -> nsPlatform
                else -> nsCorpFn
            }

        fun stackComponents(stack: DemoStack, generation: Int) =
            when (stack) {
                DemoStack.JAVA -> listOf(boot(generation), jackson(generation), security(generation), hibernate(generation), kafka(generation), log4j(generation), guava(generation))
                DemoStack.PYTHON -> listOf(fastapi(generation), django(generation), numpy(generation), pandas(generation), sklearn(generation), celery(generation), sqlalchemy(generation))
                DemoStack.WEB -> listOf(react(generation), next(generation), axios(generation), typescript(generation), vue(generation))
                DemoStack.MIXED -> listOf(boot(generation), jackson(generation), react(generation), typescript(generation), fastapi(generation))
            }

        fun runtimeFor(stack: DemoStack) =
            when (stack) {
                DemoStack.JAVA -> temurin
                DemoStack.PYTHON -> cpython
                DemoStack.WEB -> node
                DemoStack.MIXED -> temurin
            }

        fun providerFor(componentName: String) =
            when {
                componentName.startsWith("Spring") || componentName.contains("Hibernate") -> vmware
                componentName.startsWith("Apache") || componentName.startsWith("Kafka") || componentName.startsWith("Log4") -> apache
                componentName in setOf("Django", "FastAPI", "NumPy", "pandas", "scikit-learn", "Celery", "SQLAlchemy") -> pythonOrg
                componentName in setOf("React", "axios") -> meta
                componentName in setOf("Angular") -> google
                componentName == "Guava" -> google
                else -> apache
            }

        fun licenseFor(stack: DemoStack) =
            when (stack) {
                DemoStack.JAVA -> apache2
                DemoStack.PYTHON -> bsd3
                DemoStack.WEB -> mit
                DemoStack.MIXED -> apache2
            }
    }

    private fun assemble(
        spec: DemoAppSpec,
        pool: SharedPool,
        version: String,
        generation: Int,
    ): Pair<List<UUID>, List<DraftRelationWrite>> {
        val slug = spec.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val language =
            when (spec.stack) {
                DemoStack.JAVA -> "Java"
                DemoStack.PYTHON -> "Python"
                DemoStack.WEB -> "TypeScript"
                DemoStack.MIXED -> "Kotlin"
            }
        val product =
            create(
                "Product",
                mapOf(
                    "name" to spec.name,
                    "version" to version,
                    "supplier" to "Meridian Financial Group",
                    "lifecycle" to if (spec.releasedVersions.isNotEmpty()) "production" else "development",
                    "homepage" to "https://apps.meridian.example/$slug",
                    "description" to spec.description,
                ),
                spec.name,
            )
        val repo =
            create(
                "Source Repository",
                mapOf(
                    "name" to "meridian-fg/$slug",
                    "url" to "https://github.com/meridian-fg/$slug",
                    "revision" to "c0ffee${slug.hashCode().toUInt().toString(16).take(8)}",
                    "branch" to if (spec.releasedVersions.isNotEmpty()) "release/$version" else "main",
                ),
                spec.name,
            )
        val module =
            create(
                "Source Module",
                mapOf(
                    "name" to slug,
                    "path" to if (spec.stack == DemoStack.JAVA) "src/main/kotlin" else "src",
                    "language" to language,
                ),
                spec.name,
            )
        val artifactName =
            when (spec.stack) {
                DemoStack.JAVA, DemoStack.MIXED -> "$slug-$version.jar"
                DemoStack.PYTHON -> "$slug-$version-py3-none-any.whl"
                DemoStack.WEB -> "$slug-$version.tgz"
            }
        val artifact =
            create(
                "Artifact",
                mapOf(
                    "name" to artifactName,
                    "artifactType" to
                        when (spec.stack) {
                            DemoStack.JAVA, DemoStack.MIXED -> "jar"
                            DemoStack.PYTHON -> "wheel"
                            DemoStack.WEB -> "npm-pack"
                        },
                    "checksum" to "sha256:${slug.hashCode().toUInt().toString(16).padStart(16, '0')}",
                    "size" to (1_200_000 + (slug.length * 4096)),
                ),
                spec.name,
            )
        val image =
            create(
                "Container Image",
                mapOf(
                    "name" to "ghcr.io/meridian-fg/$slug",
                    "tag" to version,
                    "digest" to "sha256:${(slug + version).hashCode().toUInt().toString(16).padStart(16, '0')}",
                    "registry" to "ghcr.io",
                ),
                spec.name,
            )
        val layer =
            create(
                "Container Layer",
                mapOf(
                    "name" to "$slug-app-layer",
                    "digest" to "sha256:${(slug + "layer").hashCode().toUInt().toString(16).padStart(16, '0')}",
                    "size" to 88_000_000,
                ),
                spec.name,
            )
        val build =
            create(
                "Build",
                mapOf(
                    "name" to "$slug-build",
                    "buildNumber" to "${2000 + (slug.hashCode() and 0x7fff)}",
                    "builder" to "Tekton",
                    "status" to if (!version.endsWith("SNAPSHOT")) "succeeded" else "running",
                ),
                spec.name,
            )
        val deployment =
            create(
                "Deployment",
                mapOf(
                    "name" to "$slug-deploy",
                    "status" to if (!version.endsWith("SNAPSHOT")) "healthy" else "progressing",
                    "replicas" to if (!version.endsWith("SNAPSHOT")) 3 else 1,
                    "deployedAt" to "2026-07-15T08:30:00Z",
                ),
                spec.name,
            )
        val service =
            create(
                "Service",
                mapOf(
                    "name" to "$slug-svc",
                    "protocol" to "https",
                    "endpoint" to "https://$slug.svc.meridian.example",
                ),
                spec.name,
            )
        val api =
            create(
                "API",
                mapOf(
                    "name" to "${spec.name} API",
                    "protocol" to "REST",
                    "version" to "v1",
                ),
                spec.name,
            )
        val dbEngine =
            when (spec.stack) {
                DemoStack.PYTHON -> "PostgreSQL"
                DemoStack.WEB -> "PostgreSQL"
                else -> "PostgreSQL"
            }
        val database =
            create(
                "Database",
                mapOf(
                    "name" to "$slug-db",
                    "engine" to dbEngine,
                    "version" to "16.3",
                ),
                spec.name,
            )
        val datasets = pool.datasetsFor(spec)

        val components =
            buildList {
                addAll(pool.stackComponents(spec.stack, generation).take(5))
                if (spec.id == SbomDemoIds.APP_BILLING) add(pool.jacksonDup)
            }
        val runtime = pool.runtimeFor(spec.stack)
        val os = if (spec.stack == DemoStack.JAVA) pool.rhel else pool.ubuntu
        val env = if (!version.endsWith("SNAPSHOT")) pool.prod else pool.development
        val host = if (!version.endsWith("SNAPSHOT")) pool.prodHost else pool.stagingHost
        val cluster = if (!version.endsWith("SNAPSHOT")) pool.prodCluster else pool.nonprodCluster
        val ns = pool.namespaceFor(spec.categoryId)
        val license = pool.licenseFor(spec.stack)

        val rels = mutableListOf<DraftRelationWrite>()
        fun link(from: UUID, to: UUID, role: String) {
            rels += DraftRelationWrite(from, to, role)
        }

        link(product.id, pool.meridian.id, SbomRoles.OWNED_BY)
        link(product.id, runtime.id, SbomRoles.RUNS_ON)
        link(product.id, api.id, SbomRoles.CALLS)
        link(product.id, database.id, SbomRoles.CONNECTS_TO)
        link(product.id, pool.pci.id, SbomRoles.COMPLIES_WITH)
        link(product.id, pool.oss.id, SbomRoles.COMPLIES_WITH)
        if (spec.includeDataset || datasets.isNotEmpty()) {
            link(product.id, pool.residency.id, SbomRoles.COMPLIES_WITH)
        }
        link(runtime.id, os.id, SbomRoles.RUNS_ON)
        link(repo.id, module.id, SbomRoles.CONTAINS)
        link(module.id, artifact.id, SbomRoles.PRODUCES)
        link(build.id, artifact.id, SbomRoles.BUILDS)
        link(build.id, image.id, SbomRoles.BUILDS)
        link(image.id, artifact.id, SbomRoles.PACKAGES)
        link(image.id, layer.id, SbomRoles.CONTAINS)
        link(image.id, pool.alpine.id, SbomRoles.BASED_ON)
        link(deployment.id, image.id, SbomRoles.DEPLOYS)
        link(deployment.id, env.id, SbomRoles.TARGETS)
        link(deployment.id, host.id, SbomRoles.RUNS_ON)
        link(deployment.id, ns.id, SbomRoles.LOCATED_IN)
        link(host.id, cluster.id, SbomRoles.MEMBER_OF)
        link(service.id, api.id, SbomRoles.IMPLEMENTS)
        link(product.id, artifact.id, SbomRoles.CONTAINS)
        datasets.forEach { link(database.id, it.id, SbomRoles.CONTAINS) }

        components.forEachIndexed { index, component ->
            link(product.id, component.id, SbomRoles.CONTAINS)
            link(build.id, component.id, SbomRoles.USES)
            if (index > 0) {
                link(components[0].id, component.id, SbomRoles.DEPENDS_ON)
            }
            link(component.id, pool.providerFor(component.payload["name"].toString()).id, SbomRoles.PROVIDED_BY)
            link(component.id, license.id, SbomRoles.LICENSED_UNDER)
        }
        if (spec.attachVuln) {
            val target = components.first()
            val vuln = if (spec.stack == DemoStack.JAVA) pool.log4shell else pool.http2reset
            link(target.id, vuln.id, SbomRoles.HAS_VULNERABILITY)
            if (spec.stack == DemoStack.JAVA) {
                link(pool.boot(generation).id, pool.spring4shell.id, SbomRoles.HAS_VULNERABILITY)
            }
            link(image.id, pool.http2reset.id, SbomRoles.HAS_VULNERABILITY)
        }
        components.firstOrNull()?.let { link(it.id, pool.proprietary.id, SbomRoles.LICENSED_UNDER) }

        val ids =
            (
                listOf(
                    product.id,
                    repo.id,
                    module.id,
                    artifact.id,
                    image.id,
                    layer.id,
                    build.id,
                    deployment.id,
                    service.id,
                    api.id,
                    database.id,
                    runtime.id,
                    os.id,
                    env.id,
                    host.id,
                    cluster.id,
                    ns.id,
                ) +
                    datasets.map { it.id } +
                    components.map { it.id } +
                    rels.flatMap { listOf(it.fromAssetId, it.toAssetId) }
            ).distinct()

        return ids to rels
    }

    private fun pin(older: String, mid: String, current: String, generation: Int): String =
        listOf(older, mid, current)[generation.coerceIn(0, 2)]

    private fun create(type: String, payload: Map<String, Any?>, owner: String? = null) =
        assets.create(CreatePoolAssetRequest(type = type, payload = payload, owner = owner))

    private fun org(name: String, domain: String, website: String, country: String) =
        create("Organization", mapOf("name" to name, "domain" to domain, "website" to website, "country" to country))

    private fun license(name: String, spdx: String, url: String) =
        create("License", mapOf("name" to name, "spdxId" to spdx, "url" to url))

    private fun vuln(name: String, cve: String, severity: String, cvss: Double) =
        create("Vulnerability", mapOf("name" to name, "cve" to cve, "severity" to severity, "cvss" to cvss))

    private fun runtime(name: String, type: String, version: String) =
        create("Runtime", mapOf("name" to name, "runtimeType" to type, "version" to version))

    private fun os(name: String, dist: String, version: String, arch: String) =
        create("Operating System", mapOf("name" to name, "distribution" to dist, "version" to version, "architecture" to arch))

    private fun k8s(name: String, version: String) =
        create("Kubernetes Cluster", mapOf("name" to name, "version" to version))

    private fun env(name: String, environment: String) =
        create("Environment", mapOf("name" to name, "environment" to environment))

    private fun host(name: String, hostname: String, ip: String, provider: String) =
        create("Host", mapOf("name" to name, "hostname" to hostname, "ip" to ip, "provider" to provider))

    private fun ns(name: String, namespace: String) =
        create("Namespace", mapOf("name" to name, "namespace" to namespace))

    private fun policy(name: String, type: String, version: String) =
        create("Policy", mapOf("name" to name, "policyType" to type, "version" to version))
}
