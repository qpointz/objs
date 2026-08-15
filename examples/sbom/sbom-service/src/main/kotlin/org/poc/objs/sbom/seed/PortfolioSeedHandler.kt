package org.poc.objs.sbom.seed

import org.poc.objs.core.seed.ParsedSeedDocument
import org.poc.objs.core.seed.SeedDocumentHandler
import org.poc.objs.core.seed.SeedDocumentParseException
import org.poc.objs.core.seed.SeedDocumentResult
import org.poc.objs.core.seed.SeedRawDocument
import org.poc.objs.core.seed.requireText
import org.poc.objs.sbom.domain.CreatePortfolioRequest
import org.poc.objs.sbom.domain.CreateSubjectAreaRequest
import org.poc.objs.sbom.domain.PlaceApplicationRequest
import org.poc.objs.sbom.service.PortfolioService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PortfolioSeedHandler(
    private val portfolios: PortfolioService,
) : SeedDocumentHandler {
    override val kind: String = KIND
    override val applyOrder: Int = 50

    override fun parse(document: SeedRawDocument): ParsedSeedDocument {
        val id = requireUuid(document.raw["id"], document.index, "id")
        val name = requireText(document.raw, "name", document.index)
        val categoriesRaw = document.raw["categories"] as? List<*> ?: emptyList<Any?>()
        val categories = categoriesRaw.mapIndexed { i, raw ->
            val map = asObject(raw, document.index, "categories[$i]")
            CategorySeed(
                id = requireUuid(map["id"], document.index, "categories[$i].id"),
                name = requireText(map, "name", document.index, "categories[$i].name"),
                description = map["description"]?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                parentId = optionalUuid(map["parentId"], document.index, "categories[$i].parentId"),
            )
        }
        val ids = categories.map { it.id }.toSet()
        if (ids.size != categories.size) {
            throw SeedDocumentParseException(document.index, "Duplicate category id")
        }
        for (cat in categories) {
            if (cat.parentId != null && cat.parentId !in ids) {
                throw SeedDocumentParseException(
                    document.index,
                    "categories parentId ${cat.parentId} is not a category id in this document",
                )
            }
        }
        val placementsRaw = document.raw["placements"] as? List<*> ?: emptyList<Any?>()
        val placements = placementsRaw.mapIndexed { i, raw ->
            val map = asObject(raw, document.index, "placements[$i]")
            PlacementSeed(
                applicationId = requireUuid(map["applicationId"], document.index, "placements[$i].applicationId"),
                categoryId = optionalUuid(map["categoryId"], document.index, "placements[$i].categoryId"),
                versionId = optionalUuid(map["versionId"], document.index, "placements[$i].versionId"),
            )
        }
        for (p in placements) {
            if (p.categoryId != null && p.categoryId !in ids) {
                throw SeedDocumentParseException(
                    document.index,
                    "placements categoryId ${p.categoryId} is not a category id in this document",
                )
            }
        }
        val payload =
            PortfolioSeed(
                id = id,
                name = name,
                description = document.raw["description"]?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                uniqueness = document.raw["uniqueness"]?.toString(),
                origin = document.raw["origin"]?.toString(),
                source = document.raw["source"]?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                categories = sortParentsFirst(categories),
                placements = placements,
            )
        return ParsedSeedDocument(document, id.toString(), payload)
    }

    override fun apply(parsed: ParsedSeedDocument): SeedDocumentResult {
        val seed = parsed.payload as PortfolioSeed
        portfolios.upsertById(
            CreatePortfolioRequest(
                id = seed.id,
                name = seed.name,
                description = seed.description,
                uniqueness = seed.uniqueness,
                origin = seed.origin,
                source = seed.source,
            ),
        )
        for (cat in seed.categories) {
            portfolios.upsertSubjectArea(
                seed.id,
                CreateSubjectAreaRequest(
                    id = cat.id,
                    name = cat.name,
                    description = cat.description,
                    parentId = cat.parentId,
                ),
            )
        }
        for (p in seed.placements) {
            portfolios.placeApplication(
                seed.id,
                PlaceApplicationRequest(
                    applicationId = p.applicationId,
                    subjectAreaId = p.categoryId,
                    versionId = p.versionId,
                ),
            )
        }
        return SeedDocumentResult(
            index = parsed.document.index,
            kind = KIND,
            apiVersion = parsed.document.apiVersion,
            identity = parsed.identity,
            applied = true,
        )
    }

    private fun sortParentsFirst(categories: List<CategorySeed>): List<CategorySeed> {
        val byId = categories.associateBy { it.id }
        val out = mutableListOf<CategorySeed>()
        val seen = mutableSetOf<UUID>()
        fun visit(id: UUID) {
            if (!seen.add(id)) return
            val cat = byId[id] ?: return
            cat.parentId?.let { visit(it) }
            out += cat
        }
        categories.forEach { visit(it.id) }
        return out
    }

    private fun asObject(raw: Any?, index: Int, path: String): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return raw as? Map<String, Any?>
            ?: throw SeedDocumentParseException(index, "$path must be an object")
    }

    private fun requireUuid(raw: Any?, index: Int, path: String): UUID {
        val text = raw?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            throw SeedDocumentParseException(index, "$path is required")
        }
        return try {
            UUID.fromString(text)
        } catch (_: IllegalArgumentException) {
            throw SeedDocumentParseException(index, "$path must be a UUID")
        }
    }

    private fun optionalUuid(raw: Any?, index: Int, path: String): UUID? {
        val text = raw?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return null
        return try {
            UUID.fromString(text)
        } catch (_: IllegalArgumentException) {
            throw SeedDocumentParseException(index, "$path must be a UUID")
        }
    }

    data class PortfolioSeed(
        val id: UUID,
        val name: String,
        val description: String?,
        val uniqueness: String?,
        val origin: String?,
        val source: String?,
        val categories: List<CategorySeed>,
        val placements: List<PlacementSeed>,
    )

    data class CategorySeed(
        val id: UUID,
        val name: String,
        val description: String?,
        val parentId: UUID?,
    )

    data class PlacementSeed(
        val applicationId: UUID,
        val categoryId: UUID?,
        val versionId: UUID?,
    )

    companion object {
        const val KIND = "Portfolio"
    }
}
