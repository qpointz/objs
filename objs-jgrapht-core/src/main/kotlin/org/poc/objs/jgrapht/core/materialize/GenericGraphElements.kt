package org.poc.objs.jgrapht.core.materialize

import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import java.util.UUID

/** Default GENERIC vertex holding the resolved entity identity and payload. */
data class GenericGraphVertex(
    val entityId: UUID,
    val entity: Entity,
)

/** Default GENERIC edge holding the resolved edge identity and endpoints. */
data class GenericGraphEdge(
    val edgeId: UUID,
    val edge: Edge,
    val role: String,
)
