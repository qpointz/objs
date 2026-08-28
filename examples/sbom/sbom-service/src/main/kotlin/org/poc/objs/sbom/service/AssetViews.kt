package org.poc.objs.sbom.service

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.sbom.annotations.SbomAnnotationKeys
import org.poc.objs.sbom.domain.AssetView
import org.poc.objs.sbom.domain.RelationView

/** Product-language mapping from objs entities/edges (no BoM* in public shapes). */
object AssetViews {
    fun asset(entity: Entity): AssetView {
        val id = requireNotNull(entity.id) { "asset missing id" }
        return AssetView(
            id = id,
            type = entity.type,
            schemaVersion = entity.schemaVersion,
            label = label(entity.payload, entity.type),
            payload = entity.payload.toMap(),
            owner = entity.annotations[SbomAnnotationKeys.OWNER],
        )
    }

    fun relation(edge: Edge): RelationView {
        val id = requireNotNull(edge.id) { "relation missing id" }
        return RelationView(
            id = id,
            role = edge.role,
            label = RelationLabels.display(edge.role),
            fromAssetId = edge.source,
            toAssetId = edge.target,
        )
    }

    fun label(payload: Map<String, Any?>, type: String): String {
        val name = payload["name"]?.toString()?.takeIf { it.isNotBlank() }
        val version = payload["version"]?.toString()?.takeIf { it.isNotBlank() }
        return when {
            name != null && version != null -> "$name@$version"
            name != null -> name
            else -> type
        }
    }
}
