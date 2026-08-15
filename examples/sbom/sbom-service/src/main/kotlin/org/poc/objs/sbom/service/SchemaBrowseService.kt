package org.poc.objs.sbom.service

import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.sbom.domain.SchemaCatalogEntry
import org.poc.objs.sbom.domain.SchemaUsedInRef
import org.poc.objs.sbom.persistence.SbomApplicationRepository
import org.poc.objs.sbom.persistence.SbomApplicationVersionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class SchemaBrowseService(
    private val schemas: BoMSchemaCatalog,
    private val sbom: SbomService,
    private val applications: SbomApplicationRepository,
    private val versions: SbomApplicationVersionRepository,
    private val namedGraphs: BoMNamedGraphStore,
) {
    fun list(typeFilter: String?): List<BoMSchema> {
        sbom.ensureRegistry()
        val type = typeFilter?.trim().orEmpty()
        if (type.isEmpty()) {
            return schemas.all().sortedWith(compareBy({ it.type }, { it.version }))
        }
        return listByType(type)
    }

    fun listByType(type: String): List<BoMSchema> {
        sbom.ensureRegistry()
        val rows = schemas.listByType(type).sortedBy { it.version }
        if (rows.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No schemas for type=$type")
        }
        return rows
    }

    fun get(type: String, version: String): BoMSchema {
        sbom.ensureRegistry()
        return schemas.get(type, version)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Schema not found: $type@$version")
    }

    fun catalog(): List<SchemaCatalogEntry> {
        sbom.ensureRegistry()
        val byType = linkedMapOf<String, MutableList<BoMSchema>>()
        schemas.all()
            .filter { it.usage == BoMSchemaUsage.ENTITY }
            .sortedWith(compareBy({ it.type }, { it.version }))
            .forEach { byType.getOrPut(it.type) { mutableListOf() }.add(it) }
        return byType.map { (type, versions) ->
            val latest = versions.last()
            SchemaCatalogEntry(
                type = type,
                latestVersion = latest.version,
                versions = versions.map { it.version },
                title = latest.contentSchema.title,
                description = latest.contentSchema.description,
                usage = latest.usage.name,
                usedIn = emptyList(),
            )
        }
    }

    fun usedInForType(type: String): List<SchemaUsedInRef> {
        sbom.ensureRegistry()
        return usageByType()[type].orEmpty()
    }

    @Volatile
    private var usageCache: Map<String, List<SchemaUsedInRef>>? = null

    private fun usageByType(): Map<String, List<SchemaUsedInRef>> {
        usageCache?.let { return it }
        synchronized(this) {
            usageCache?.let { return it }
            val computed = applicationsUsingTypes()
            usageCache = computed
            return computed
        }
    }

    private fun applicationsUsingTypes(): Map<String, List<SchemaUsedInRef>> {
        val byType = linkedMapOf<String, MutableMap<UUID, SchemaUsedInRef>>()
        fun add(appId: UUID, types: Set<String>) {
            val app = applications.findById(appId).orElse(null) ?: return
            val ref = SchemaUsedInRef(app.id, app.name)
            for (t in types) {
                byType.getOrPut(t) { linkedMapOf() }[app.id] = ref
            }
        }
        for (version in versions.findAll()) {
            val graph = namedGraphs.get(version.graphId) ?: continue
            add(version.applicationId, graph.contents.entities.map { it.type }.toSet())
        }
        return byType.mapValues { it.value.values.toList() }
    }
}
