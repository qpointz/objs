package org.poc.objs.sbom.migration

import org.poc.objs.api.typed.PayloadMapper
import org.poc.objs.api.typed.upgrade.InMemorySchemaUpgradeRegistry
import org.poc.objs.api.typed.upgrade.SchemaUpgradeRegistry
import org.poc.objs.api.typed.upgrade.SchemaUpgradeTarget
import org.poc.objs.api.typed.upgrade.SchemaUpgradeTargetRegistry
import org.poc.objs.sbom.model.COMPONENT_SCHEMA_V2
import org.poc.objs.sbom.model.ComponentPayloadV2
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Configuration
class SbomSchemaUpgradeConfiguration {
    @Bean
    fun sbomUpgradePayloadMapper(): PayloadMapper =
        PayloadMapper(
            JsonMapper.builder()
                .addModule(kotlinModule())
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build(),
        )

    @Bean
    fun sbomSchemaUpgradeRegistry(): SchemaUpgradeRegistry =
        InMemorySchemaUpgradeRegistry.of(Component_1_0_0_to_2_0_0())

    @Bean
    fun sbomSchemaUpgradeTargets(): SchemaUpgradeTargetRegistry =
        SchemaUpgradeTargetRegistry { type ->
            when (type) {
                "Component" -> SchemaUpgradeTarget(COMPONENT_SCHEMA_V2, ComponentPayloadV2::class.java)
                else -> null
            }
        }
}
