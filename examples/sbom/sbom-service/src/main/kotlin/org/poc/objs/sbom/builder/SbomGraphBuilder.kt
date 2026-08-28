package org.poc.objs.sbom.builder

import org.poc.objs.api.domain.Graph
import org.poc.objs.api.typed.PayloadMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import org.poc.objs.api.typed.GraphBuilder
import org.poc.objs.api.typed.NodeRef
import org.poc.objs.api.typed.TypedEdge
import org.poc.objs.api.typed.TypedEntity
import org.poc.objs.api.typed.mergeAnnotations
import org.poc.objs.sbom.annotations.Provenance
import org.poc.objs.sbom.annotations.SbomAnnotationKeys
import org.poc.objs.sbom.annotations.SbomContext
import org.poc.objs.sbom.model.CanonicalEdgePayload
import org.poc.objs.sbom.model.ComponentPayload
import org.poc.objs.sbom.model.ComponentType
import org.poc.objs.sbom.registry.SbomRegistry
import org.poc.objs.sbom.registry.SbomRoles
import java.util.UUID

class SbomGraphBuilder(
    private val context: SbomContext,
    private val payloadMapper: PayloadMapper = PayloadMapper(JsonMapper.builder().addModule(kotlinModule()).build()),
    private val extraDefaults: Map<String, String> = emptyMap(),
) {
    private val builder = GraphBuilder(
        payloadMapper = payloadMapper,
        defaultAnnotations = context.toAnnotations() + extraDefaults,
    )
    private val nodeApps = mutableMapOf<UUID, Pair<String, String>>()
    private val nodeTypes = mutableMapOf<UUID, String>()

    fun add(
        entity: TypedEntity<*>,
        provenance: Provenance,
        key: String? = null,
        extraAnnotations: Map<String, String> = emptyMap(),
    ): NodeRef {
        entity.annotations = mergeAnnotations(
            entity.annotations,
            provenance.toAnnotations() + extraAnnotations,
        )
        val ref = builder.add(entity, key)
        nodeApps[ref.id] = context.app to context.appVersion
        nodeTypes[ref.id] = entity.meta.type
        return ref
    }

    fun addComponent(
        payload: ComponentPayload,
        provenance: Provenance,
        key: String? = null,
        extraAnnotations: Map<String, String> = emptyMap(),
    ): NodeRef = add(ComponentType.entity(payload), provenance, key, extraAnnotations)

    fun ref(key: String): NodeRef = builder.ref(key)

    fun link(
        source: NodeRef,
        role: String,
        target: NodeRef,
        properties: CanonicalEdgePayload? = null,
    ): SbomGraphBuilder {
        assertSameContext(source, target)
        val sourceType = requireNotNull(nodeTypes[source.id])
        val targetType = requireNotNull(nodeTypes[target.id])
        val edge = TypedEdge(
            meta = SbomRegistry.canonicalEdgeMeta(role, sourceType, targetType),
            propertiesType = CanonicalEdgePayload::class.java,
            properties = properties ?: CanonicalEdgePayload(),
        )
        builder.edge(source, edge, target)
        return this
    }

    fun dependsOn(
        source: NodeRef,
        target: NodeRef,
        properties: CanonicalEdgePayload? = null,
    ): SbomGraphBuilder = link(source, SbomRoles.DEPENDS_ON, target, properties)

    fun build(): Graph = builder.build()

    private fun assertSameContext(a: NodeRef, b: NodeRef) {
        val ca = nodeApps[a.id]
        val cb = nodeApps[b.id]
        require(ca != null && cb != null && ca == cb) {
            "Edges must link entities in the same (app, appVersion); got $ca and $cb"
        }
        require(context.app.isNotBlank() && context.appVersion.isNotBlank()) {
            "SbomContext requires ${SbomAnnotationKeys.APP} and ${SbomAnnotationKeys.APP_VERSION}"
        }
    }
}
