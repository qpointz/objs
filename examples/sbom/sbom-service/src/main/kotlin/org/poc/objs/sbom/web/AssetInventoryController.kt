package org.poc.objs.sbom.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.poc.objs.sbom.domain.AssetDetailView
import org.poc.objs.sbom.domain.AssetDuplicateGroup
import org.poc.objs.sbom.domain.AssetSearchPage
import org.poc.objs.sbom.domain.AssetSearchRequest
import org.poc.objs.sbom.domain.AssetTypeStatistics
import org.poc.objs.sbom.domain.AssetView
import org.poc.objs.sbom.domain.CreatePoolAssetRequest
import org.poc.objs.sbom.domain.SetAssetOwnerRequest
import org.poc.objs.sbom.domain.UpdatePoolAssetRequest
import org.poc.objs.sbom.service.AssetInventoryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/assets")
@Tag(name = "assets")
class AssetInventoryController(
    private val assets: AssetInventoryService,
) {
    @GetMapping
    @Operation(summary = "List pool assets by optional type and schema version")
    fun list(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) schemaVersion: String?,
    ): List<AssetView> =
        assets.search(AssetSearchRequest(type = type, schemaVersion = schemaVersion))

    @PostMapping("/search")
    @Operation(summary = "Search pool assets with a JSON filter body")
    fun search(@RequestBody body: AssetSearchRequest): List<AssetView> =
        assets.search(body)

    @PostMapping("/search/page")
    @Operation(summary = "Paged search of pool assets")
    fun searchPage(
        @RequestBody body: AssetSearchRequest,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): AssetSearchPage = assets.searchPage(body, page, size)

    @PostMapping
    @Operation(summary = "Create a pool asset")
    fun create(@RequestBody body: CreatePoolAssetRequest): AssetView =
        assets.create(body)

    @GetMapping("/duplicates")
    @Operation(summary = "Find duplicate assets for a type")
    fun duplicates(
        @RequestParam type: String,
        @RequestParam(required = false) schemaVersion: String?,
    ): List<AssetDuplicateGroup> = assets.findDuplicates(type, schemaVersion)

    @GetMapping("/statistics")
    @Operation(summary = "Asset counts and owner breakdown for a type")
    fun statistics(@RequestParam type: String): AssetTypeStatistics =
        assets.statistics(type)

    @PutMapping("/{id}")
    @Operation(summary = "Update a pool asset payload or metadata")
    fun update(
        @PathVariable id: UUID,
        @RequestBody body: UpdatePoolAssetRequest,
    ): AssetView = assets.update(id, body)

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a pool asset with usage detail")
    fun get(@PathVariable id: UUID): AssetDetailView =
        assets.get(id)

    @PutMapping("/{id}/owner")
    @Operation(summary = "Set the owning application for a pool asset")
    fun setOwner(
        @PathVariable id: UUID,
        @RequestBody body: SetAssetOwnerRequest,
    ): AssetView = assets.setOwner(id, body)
}
