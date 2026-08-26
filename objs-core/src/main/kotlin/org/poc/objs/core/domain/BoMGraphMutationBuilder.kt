package org.poc.objs.core.domain

import java.util.UUID

/** DSL entry for [BoMGraphMutation]. */
fun bomMutation(block: BoMGraphMutationBuilder.() -> Unit): BoMGraphMutation =
    BoMGraphMutationBuilder().apply(block).build()

class BoMGraphMutationBuilder {
    private val entities = BoMEntityMutation()
    private val edges = BoMEdgeMutation()
    private var mode: BoMMutateMode = BoMMutateMode.MERGE

    fun mode(mode: BoMMutateMode) {
        this.mode = mode
    }

    fun entities(block: BoMEntityMutationBuilder.() -> Unit) {
        BoMEntityMutationBuilder(entities).block()
    }

    fun edges(block: BoMEdgeMutationBuilder.() -> Unit) {
        BoMEdgeMutationBuilder(edges).block()
    }

    /** Copy a bag into both set lists (set-only). */
    fun setAll(graph: BoMGraph) {
        entities.set.addAll(graph.entities)
        edges.set.addAll(graph.edges)
    }

    fun build(): BoMGraphMutation =
        BoMGraphMutation(entities = entities, edges = edges, mode = mode)
}

class BoMEntityMutationBuilder(
    private val target: BoMEntityMutation,
) {
    fun set(vararg entities: BoMEntity) {
        target.set.addAll(entities)
    }

    fun set(entities: Iterable<BoMEntity>) {
        target.set.addAll(entities)
    }

    fun unset(vararg ids: UUID) {
        target.unset.addAll(ids)
    }

    fun unset(ids: Iterable<UUID>) {
        target.unset.addAll(ids)
    }
}

class BoMEdgeMutationBuilder(
    private val target: BoMEdgeMutation,
) {
    fun set(vararg edges: BoMEdge) {
        target.set.addAll(edges)
    }

    fun set(edges: Iterable<BoMEdge>) {
        target.set.addAll(edges)
    }

    fun unset(vararg ids: UUID) {
        target.unset.addAll(ids)
    }

    fun unset(ids: Iterable<UUID>) {
        target.unset.addAll(ids)
    }
}
