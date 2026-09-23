package org.poc.objs.sbom.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.typed.PayloadMapper
import org.poc.objs.api.typed.TypedEntityBindingRegistry
import org.poc.objs.api.typed.upgrade.HydrationPolicy
import org.poc.objs.api.typed.upgrade.InMemorySchemaUpgradeRegistry
import org.poc.objs.api.typed.upgrade.PayloadUpgrade
import org.poc.objs.api.typed.upgrade.SchemaUpgradeTarget
import org.poc.objs.api.typed.upgrade.SchemaUpgradeTargetRegistry
import org.poc.objs.sbom.domain.PayloadRepresentation
import org.poc.objs.sbom.model.COMPONENT_SCHEMA_V2
import org.poc.objs.sbom.model.ComponentPayloadV2
import org.poc.objs.sbom.model.SCHEMA_VERSION
import org.poc.objs.sbom.service.AssetExamine
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

/**
 * Regression-style proof: Component@1.0.0 evidence upgrades to @2.0.0 examine (C-35 / G-30).
 */
class ComponentUpgradeRegressionTest {

    private val mapper = PayloadMapper(
        JsonMapper.builder()
            .addModule(kotlinModule())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build(),
    )
    private val upgrades = InMemorySchemaUpgradeRegistry.of(Component_1_0_0_to_2_0_0())
    private val targets = SchemaUpgradeTargetRegistry {
        if (it == "Component") SchemaUpgradeTarget(COMPONENT_SCHEMA_V2, ComponentPayloadV2::class.java) else null
    }

    private val evidencePayload = mapOf(
        "name" to "openssl",
        "version" to "3.0.0",
        "ecosystem" to "native",
        "kind" to "library",
    )

    @Test
    fun shouldUpgradeComponentEvidenceToExamineShape() {
        val entity = Entity(
            id = UUID.randomUUID(),
            type = "Component",
            schemaVersion = SCHEMA_VERSION,
            payload = evidencePayload.toMutableMap(),
        )
        val outcome = PayloadUpgrade.hydrate(
            entity,
            TypedEntityBindingRegistry { _, _ -> null },
            mapper,
            HydrationPolicy.UPGRADE_TO_LATEST,
            upgrades,
            targets,
        )
        val latest = outcome.hydrated as ComponentPayloadV2
        assertThat(latest.displayName).isEqualTo("openssl")
        assertThat(latest.tier).isEqualTo("STANDARD")
        assertThat(outcome.diagnostics!!.effectiveSchemaVersion).isEqualTo(COMPONENT_SCHEMA_V2)
        assertThat(outcome.diagnostics!!.additiveFallback).isFalse()
    }

    @Test
    fun shouldExposeBothEvidenceAndExamineOnAssetView() {
        val entity = Entity(
            id = UUID.randomUUID(),
            type = "Component",
            schemaVersion = SCHEMA_VERSION,
            payload = evidencePayload.toMutableMap(),
        )
        val view = AssetExamine.project(
            entity,
            PayloadRepresentation.BOTH,
            mapper,
            upgrades,
            targets,
        )
        assertThat(view.schemaVersion).isEqualTo(SCHEMA_VERSION)
        assertThat(view.payload["name"]).isEqualTo("openssl")
        assertThat(view.latestSchemaVersion).isEqualTo(COMPONENT_SCHEMA_V2)
        assertThat(view.latestPayload!!["displayName"]).isEqualTo("openssl")
        assertThat(view.latestPayload!!["tier"]).isEqualTo("STANDARD")
    }
}
