package org.poc.objs.api.domain

import java.util.UUID

/** Kotlin DSL entry point for [GraphMutation]. */
fun graphMutation(block: GraphMutationBuilder.() -> Unit): GraphMutation =
    GraphMutationBuilder().apply(block).build()

class GraphMutationBuilder {
    private val entities = EntityMutation()
    private val edges = EdgeMutation()
    private var mode: MutationMode = MutationMode.MERGE

    fun mode(mode: MutationMode) {
        this.mode = mode
    }

    fun entities(block: EntityMutationBuilder.() -> Unit) {
        EntityMutationBuilder(entities).block()
    }

    fun edges(block: EdgeMutationBuilder.() -> Unit) {
        EdgeMutationBuilder(edges).block()
    }

    /** Copy a bag into both set lists. */
    fun setAll(graph: Graph) {
        entities.set.addAll(graph.entities)
        edges.set.addAll(graph.edges)
    }

    fun build(): GraphMutation =
        GraphMutation(entities = entities, edges = edges, mode = mode)
}

class EntityMutationBuilder(
    private val target: EntityMutation,
) {
    fun set(vararg entities: Entity) {
        target.set.addAll(entities)
    }

    fun set(entities: Iterable<Entity>) {
        target.set.addAll(entities)
    }

    fun unset(vararg ids: UUID) {
        target.unset.addAll(ids)
    }

    fun unset(ids: Iterable<UUID>) {
        target.unset.addAll(ids)
    }
}

class EdgeMutationBuilder(
    private val target: EdgeMutation,
) {
    fun set(vararg edges: Edge) {
        target.set.addAll(edges)
    }

    fun set(edges: Iterable<Edge>) {
        target.set.addAll(edges)
    }

    fun unset(vararg ids: UUID) {
        target.unset.addAll(ids)
    }

    fun unset(ids: Iterable<UUID>) {
        target.unset.addAll(ids)
    }
}
