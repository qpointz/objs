package org.poc.objs.core.seed

import org.poc.objs.api.seed.*

import org.poc.objs.api.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.AllowedEdgeRule
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Schema
import org.poc.objs.api.domain.SchemaCatalog
import java.util.UUID

/**
 * Canonical multi-document YAML serializer for seed kinds.
 * Kind exporters remain extensible; graph export requires an explicit name + key maps.
 */
class CanonicalSeedSerializer(
    private val schemas: SchemaCatalog,
    private val edgeRules: AllowedEdgeCatalog,
    private val objectSchemaHandler: ObjectSchemaSeedHandler,
    private val allowedEdgeRuleHandler: AllowedEdgeRuleSeedHandler,
    private val graphHandler: GraphSeedHandler,
) {
    fun serializeCatalogs(
        includeSchemas: Boolean = true,
        includeEdgeRules: Boolean = true,
        graphs: List<GraphExport> = emptyList(),
    ): String {
        val documents = mutableListOf<Map<String, Any?>>()
        if (includeSchemas) {
            schemas.all()
                .sortedWith(compareBy({ it.type }, { it.version }))
                .forEach { documents += objectSchemaHandler.serialize(it) }
        }
        if (includeEdgeRules) {
            edgeRules.all()
                .sortedWith(compareBy({ it.sourceType }, { it.role }, { it.targetType }))
                .forEach { documents += allowedEdgeRuleHandler.serialize(it) }
        }
        for (graph in graphs) {
            documents += graphHandler.serialize(
                name = graph.name,
                entities = graph.entities,
                edges = graph.edges,
                entityKeys = graph.entityKeys,
                edgeKeys = graph.edgeKeys,
            )
        }
        return SeedYaml.writeDocuments(documents)
    }

    fun serializeSchemas(schemas: Collection<Schema>): String =
        SeedYaml.writeDocuments(
            schemas.sortedWith(compareBy({ it.type }, { it.version }))
                .map { objectSchemaHandler.serialize(it) },
        )

    fun serializeEdgeRules(rules: Collection<AllowedEdgeRule>): String =
        SeedYaml.writeDocuments(
            rules.sortedWith(compareBy({ it.sourceType }, { it.role }, { it.targetType }))
                .map { allowedEdgeRuleHandler.serialize(it) },
        )

    data class GraphExport(
        val name: String,
        val entities: List<Entity>,
        val edges: List<Edge>,
        val entityKeys: Map<UUID, String>,
        val edgeKeys: Map<UUID, String>,
    )
}
