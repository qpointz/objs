package org.poc.objs.api.typed.upgrade

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.typed.PayloadMapper
import org.poc.objs.api.typed.TypedEntityBinding
import org.poc.objs.api.typed.TypedEntityBindingRegistry
import org.poc.objs.api.typed.TypedGraphView
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

class SchemaUpgradeTest {

    data class ProductV1(var name: String? = null)
    data class ProductV2(var displayName: String? = null, var tier: String? = null)
    data class ProductAdditive(var name: String? = null, var note: String? = null)

    private val strictMapper = PayloadMapper(
        JsonMapper.builder()
            .addModule(kotlinModule())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build(),
    )

    private val renameStep = object : ClassToClassUpgradeStep<ProductV1, ProductV2>() {
        override fun type() = "Product"
        override fun fromVersion() = "1.0.0"
        override fun toVersion() = "2.0.0"
        override fun fromClass() = ProductV1::class.java
        override fun toClass() = ProductV2::class.java
        override fun upgrade(from: ProductV1) =
            ProductV2(displayName = from.name, tier = "STANDARD")
    }

    @Test
    fun shouldFindAdjacentChain() {
        val mid = object : ClassToClassUpgradeStep<ProductV1, ProductV1>() {
            override fun type() = "Product"
            override fun fromVersion() = "1.0.0"
            override fun toVersion() = "1.1.0"
            override fun fromClass() = ProductV1::class.java
            override fun toClass() = ProductV1::class.java
            override fun upgrade(from: ProductV1) = from
        }
        val toLatest = object : ClassToClassUpgradeStep<ProductV1, ProductV2>() {
            override fun type() = "Product"
            override fun fromVersion() = "1.1.0"
            override fun toVersion() = "2.0.0"
            override fun fromClass() = ProductV1::class.java
            override fun toClass() = ProductV2::class.java
            override fun upgrade(from: ProductV1) =
                ProductV2(displayName = from.name, tier = "STANDARD")
        }
        val registry = InMemorySchemaUpgradeRegistry.of(mid, toLatest)
        val chain = registry.findChain("Product", "1.0.0", "2.0.0")
        assertThat(chain).hasSize(2)
        assertThat(chain[0].toVersion()).isEqualTo("1.1.0")
        assertThat(chain[1].toVersion()).isEqualTo("2.0.0")
    }

    @Test
    fun shouldRejectAmbiguousPaths() {
        val a = object : ClassToClassUpgradeStep<ProductV1, ProductV2>() {
            override fun type() = "Product"
            override fun fromVersion() = "1.0.0"
            override fun toVersion() = "2.0.0"
            override fun fromClass() = ProductV1::class.java
            override fun toClass() = ProductV2::class.java
            override fun upgrade(from: ProductV1) = ProductV2(displayName = from.name)
        }
        val b = object : ClassToClassUpgradeStep<ProductV1, ProductV2>() {
            override fun type() = "Product"
            override fun fromVersion() = "1.0.0"
            override fun toVersion() = "2.0.0"
            override fun fromClass() = ProductV1::class.java
            override fun toClass() = ProductV2::class.java
            override fun upgrade(from: ProductV1) = ProductV2(displayName = from.name, tier = "X")
        }
        val registry = InMemorySchemaUpgradeRegistry.of(a, b)
        assertThatThrownBy { registry.findChain("Product", "1.0.0", "2.0.0") }
            .isInstanceOf(AmbiguousUpgradePathException::class.java)
    }

    @Test
    fun shouldHydrateViaClassToClassStep() {
        val id = UUID.randomUUID()
        val entity = Entity(
            id = id,
            type = "Product",
            schemaVersion = "1.0.0",
            payload = mutableMapOf("name" to "Widget"),
        )
        val bindings = TypedEntityBindingRegistry { type, version ->
            if (type == "Product" && version == "2.0.0") {
                TypedEntityBinding { e, m -> m.fromMap(e.payload, ProductV2::class.java) }
            } else {
                null
            }
        }
        val view = TypedGraphView.from(
            Graph(mutableListOf(entity)),
            bindings,
            strictMapper,
            HydrationPolicy.UPGRADE_TO_LATEST,
            InMemorySchemaUpgradeRegistry.of(renameStep),
            SchemaUpgradeTargetRegistry {
                if (it == "Product") SchemaUpgradeTarget("2.0.0", ProductV2::class.java) else null
            },
        )
        val node = view.node(id)!!
        assertThat(node.schemaVersion).isEqualTo("1.0.0")
        assertThat(node.effectiveSchemaVersion).isEqualTo("2.0.0")
        val payload = node.hydratedPayload as ProductV2
        assertThat(payload.displayName).isEqualTo("Widget")
        assertThat(payload.tier).isEqualTo("STANDARD")
        assertThat(node.upgradeDiagnostics!!.stepsApplied).containsExactly("1.0.0->2.0.0")
        assertThat(node.upgradeDiagnostics!!.additiveFallback).isFalse()
    }

    @Test
    fun shouldUseAdditiveFallbackWhenNoExplicitStep() {
        val id = UUID.randomUUID()
        val entity = Entity(
            id = id,
            type = "Product",
            schemaVersion = "1.0.0",
            payload = mutableMapOf("name" to "Widget"),
        )
        val bindings = TypedEntityBindingRegistry { _, _ -> null }
        val view = TypedGraphView.from(
            Graph(mutableListOf(entity)),
            bindings,
            strictMapper,
            HydrationPolicy.UPGRADE_TO_LATEST,
            InMemorySchemaUpgradeRegistry.of(),
            SchemaUpgradeTargetRegistry {
                if (it == "Product") {
                    SchemaUpgradeTarget("1.1.0", ProductAdditive::class.java)
                } else {
                    null
                }
            },
        )
        val node = view.node(id)!!
        assertThat(node.hydratedPayload).isInstanceOf(ProductAdditive::class.java)
        assertThat((node.hydratedPayload as ProductAdditive).name).isEqualTo("Widget")
        assertThat(node.upgradeDiagnostics!!.additiveFallback).isTrue()
        assertThat(node.effectiveSchemaVersion).isEqualTo("1.1.0")
    }

    @Test
    fun shouldFailOpenToRawWhenRenameWithoutStep() {
        val id = UUID.randomUUID()
        val entity = Entity(
            id = id,
            type = "Product",
            schemaVersion = "1.0.0",
            payload = mutableMapOf("name" to "Widget"),
        )
        val bindings = TypedEntityBindingRegistry { _, _ -> null }
        val view = TypedGraphView.from(
            Graph(mutableListOf(entity)),
            bindings,
            strictMapper,
            HydrationPolicy.UPGRADE_TO_LATEST,
            InMemorySchemaUpgradeRegistry.of(),
            SchemaUpgradeTargetRegistry {
                if (it == "Product") SchemaUpgradeTarget("2.0.0", ProductV2::class.java) else null
            },
        )
        val node = view.node(id)!!
        assertThat(node.hydratedPayload).isNull()
        assertThat(node.schemaVersion).isEqualTo("1.0.0")
        assertThat(node.upgradeDiagnostics!!.additiveFallback).isTrue()
        assertThat(node.upgradeDiagnostics!!.failure).isNotBlank()
    }

    @Test
    fun shouldPreferExplicitStepOverAdditiveFallback() {
        val id = UUID.randomUUID()
        val entity = Entity(
            id = id,
            type = "Product",
            schemaVersion = "1.0.0",
            payload = mutableMapOf("name" to "Widget"),
        )
        val bindings = TypedEntityBindingRegistry { _, _ -> null }
        val view = TypedGraphView.from(
            Graph(mutableListOf(entity)),
            bindings,
            strictMapper,
            HydrationPolicy.UPGRADE_TO_LATEST,
            InMemorySchemaUpgradeRegistry.of(renameStep),
            SchemaUpgradeTargetRegistry {
                if (it == "Product") SchemaUpgradeTarget("2.0.0", ProductV2::class.java) else null
            },
        )
        val node = view.node(id)!!
        assertThat(node.upgradeDiagnostics!!.additiveFallback).isFalse()
        assertThat((node.hydratedPayload as ProductV2).tier).isEqualTo("STANDARD")
    }
}
