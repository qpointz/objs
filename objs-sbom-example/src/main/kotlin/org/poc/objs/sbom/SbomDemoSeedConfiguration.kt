package org.poc.objs.sbom

import org.poc.objs.sbom.annotations.Provenance
import org.poc.objs.sbom.annotations.SbomContext
import org.poc.objs.sbom.builder.SbomGraphBuilder
import org.poc.objs.sbom.model.ComponentPayload
import org.poc.objs.sbom.model.LicensePayload
import org.poc.objs.sbom.model.LicenseType
import org.poc.objs.sbom.model.OrganizationPayload
import org.poc.objs.sbom.model.OrganizationType
import org.poc.objs.sbom.model.ProductPayload
import org.poc.objs.sbom.model.ProductType
import org.poc.objs.sbom.registry.SbomRoles
import org.poc.objs.sbom.service.SbomService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Writes a tiny multi-app sample graph when [objs.sbom.demo-seed] is true (default for objs-app).
 * Skips if `payments-api` / `2.3.1` already has entities.
 */
@Configuration
class SbomDemoSeedConfiguration {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    @ConditionalOnProperty(prefix = "objs.sbom", name = ["demo-seed"], havingValue = "true", matchIfMissing = false)
    fun sbomDemoGraphSeed(sbom: SbomService): ApplicationRunner = ApplicationRunner {
        sbom.ensureRegistry()
        val existing = sbom.getSbom("payments-api", "2.3.1")
        if (existing.entities.isNotEmpty()) {
            log.info("SBOM demo seed skipped — payments-api@2.3.1 already present")
            return@ApplicationRunner
        }

        val payments = SbomContext("payments-api", "2.3.1")
        val b = SbomGraphBuilder(payments)
        val product = b.add(
            ProductType.entity(ProductPayload("Payments API", "2.3.1")),
            Provenance.detected("demo-seed"),
            key = "product",
        )
        val org = b.add(
            OrganizationType.entity(OrganizationPayload("Acme Corp", domain = "acme.example")),
            Provenance.enriched("catalog1"),
            key = "org",
        )
        val lic = b.add(
            LicenseType.entity(LicensePayload("Apache License 2.0", "Apache-2.0")),
            Provenance.detected("demo-seed"),
            key = "lic",
        )
        val boot = b.addComponent(
            ComponentPayload("Spring Boot", "3.3.0", "Maven", "framework"),
            Provenance.detected("demo-seed"),
            key = "boot",
        )
        val jackson = b.addComponent(
            ComponentPayload("Jackson Databind", "2.17.0", "Maven", "library"),
            Provenance.manual("demo-user"),
            key = "jackson",
        )
        b.link(product, SbomRoles.CONTAINS, boot)
        b.link(product, SbomRoles.OWNED_BY, org)
        b.link(boot, SbomRoles.DEPENDS_ON, jackson)
        b.link(boot, SbomRoles.PROVIDED_BY, org)
        b.link(boot, SbomRoles.LICENSED_UNDER, lic)
        val result = sbom.save(b.build())
        if (!result.isValid) {
            log.warn("SBOM demo seed failed: {}", result.issues.joinToString { "${it.code}: ${it.message}" })
            return@ApplicationRunner
        }

        val billing = SbomContext("billing-api", "1.0.0")
        val b2 = SbomGraphBuilder(billing)
        b2.addComponent(
            ComponentPayload("Guava", "33.0.0", "Maven", "library"),
            Provenance.enriched("catalog1"),
        )
        val billingResult = sbom.save(b2.build())
        if (!billingResult.isValid) {
            log.warn(
                "SBOM billing demo seed failed: {}",
                billingResult.issues.joinToString { "${it.code}: ${it.message}" },
            )
        } else {
            log.info("SBOM demo seed loaded (payments-api@2.3.1, billing-api@1.0.0)")
        }
    }
}
