@file:JvmName("GraphMutations")

package org.poc.objs.api.domain

import java.util.UUID

/** Per-entity outcome of a [MutationRewriter]. */
sealed interface EntityDecision {
    /** Keep the entity as is. */
    data object Keep : EntityDecision

    /** Put [to] in place of the entity; edges pointing at the old id follow `to.id`. */
    data class ReplaceWith(val to: Entity) : EntityDecision

    /**
     * Remove the entity from `entities.set` (this is not an unset). Incident edges are redirected
     * to [redirectTo], or dropped when it is null.
     */
    data class Drop(val redirectTo: UUID? = null) : EntityDecision
}

/**
 * Two-phase mutation rewrite: [prepare] sees the whole mutation once (e.g. to batch-lookup),
 * then [entity] and [edge] decide per element. Run it with [rewrite].
 */
interface MutationRewriter<C> {
    fun prepare(mutation: GraphMutation): C

    fun entity(entity: Entity, ctx: C): EntityDecision

    /** Called with endpoints already remapped; return null to drop the edge. */
    fun edge(edge: Edge, ctx: C): Edge? = edge
}

/**
 * Applies [rewriter] and returns a new mutation; the receiver and its elements are not modified.
 *
 * Entity id changes are collected into one remap (followed transitively) and applied to edge
 * endpoints. Edges touching a dropped entity are removed. The result has entities deduplicated
 * by id and edges deduplicated by (source, target, role), first occurrence wins. Unset lists are
 * copied unchanged.
 */
fun <C> GraphMutation.rewrite(rewriter: MutationRewriter<C>): GraphMutation {
    val ctx = rewriter.prepare(this)

    val remap = mutableMapOf<UUID, UUID>()
    val dropped = mutableSetOf<UUID>()
    val newEntities = mutableListOf<Entity>()
    for (e in entities.set) {
        when (val decision = rewriter.entity(e, ctx)) {
            EntityDecision.Keep -> newEntities += e
            is EntityDecision.ReplaceWith -> {
                val toId = requireNotNull(decision.to.id) { "replacement entity must have an id" }
                newEntities += decision.to
                e.id?.takeIf { it != toId }?.let { remap[it] = toId }
            }
            is EntityDecision.Drop -> e.id?.let { id ->
                val redirect = decision.redirectTo
                if (redirect != null && redirect != id) remap[id] = redirect else dropped += id
            }
        }
    }

    fun resolve(id: UUID): UUID {
        var current = id
        val seen = mutableSetOf(current)
        while (true) {
            val next = remap[current] ?: return current
            require(seen.add(next)) { "cyclic entity remap through $next" }
            current = next
        }
    }

    val newEdges = edges.set.mapNotNull { edge ->
        val source = resolve(edge.source)
        val target = resolve(edge.target)
        if (source in dropped || target in dropped) return@mapNotNull null
        val remapped = if (source == edge.source && target == edge.target) {
            edge
        } else {
            edge.copy(source = source, target = target)
        }
        rewriter.edge(remapped, ctx)
    }

    return copy(
        entities = entities.copy(
            set = newEntities.distinctBy { it.id ?: it }.toMutableList(),
            unset = entities.unset.toMutableList(),
        ),
        edges = edges.copy(
            set = newEdges.distinctBy { Triple(it.source, it.target, it.role) }.toMutableList(),
            unset = edges.unset.toMutableList(),
        ),
    )
}

/**
 * Swaps [replace] for [to] and redirects edges pointing at `replace.id` to `to.id`.
 * [replace] is matched by id when it has one, otherwise by reference.
 */
fun GraphMutation.replace(replace: Entity, to: Entity): GraphMutation {
    requireNotNull(to.id) { "replacement entity must have an id" }
    val oldId = replace.id
    return rewrite(object : MutationRewriter<Unit> {
        override fun prepare(mutation: GraphMutation) = Unit

        override fun entity(entity: Entity, ctx: Unit): EntityDecision {
            val matches = if (oldId != null) entity.id == oldId else entity === replace
            return if (matches) EntityDecision.ReplaceWith(to) else EntityDecision.Keep
        }
    })
}

/**
 * Replaces every entity whose [key] equals `key(to)` with [to], redirecting their edges.
 * Entities with a null key never match.
 */
fun GraphMutation.replace(to: Entity, key: (Entity) -> Any?): GraphMutation {
    requireNotNull(to.id) { "replacement entity must have an id" }
    val wanted = requireNotNull(key(to)) { "replacement entity has no match key" }
    return rewrite(object : MutationRewriter<Unit> {
        override fun prepare(mutation: GraphMutation) = Unit

        override fun entity(entity: Entity, ctx: Unit): EntityDecision =
            if (entity !== to && key(entity) == wanted) EntityDecision.ReplaceWith(to) else EntityDecision.Keep
    })
}

/** Replaces entities of the same type whose top-level payload [field] equals the one on [to]. */
@JvmOverloads
fun GraphMutation.replaceByPayload(to: Entity, field: String = "externalId"): GraphMutation =
    replace(to) { e -> e.payload[field]?.let { e.type to it } }
