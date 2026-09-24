package org.poc.objs.sbom.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
@Tag(name = "assets", description = "Shared pool assets: search, CRUD, duplicates, statistics, ownership")
class AssetInventoryController(
    private val assets: AssetInventoryService,
) {
    @GetMapping
    @Operation(summary = "List pool assets by optional type and schema version")
    @ApiResponse(
        responseCode = "200",
        description = "Matching pool assets",
        content = [Content(array = ArraySchema(schema = Schema(implementation = AssetView::class)))],
    )
    fun list(
        @Parameter(description = "Asset schema type name; omit for all types")
        @RequestParam(required = false) type: String?,
        @Parameter(description = "Asset schema version; omit for all versions")
        @RequestParam(required = false) schemaVersion: String?,
    ): List<AssetView> =
        assets.search(AssetSearchRequest(type = type, schemaVersion = schemaVersion))

    @PostMapping("/search")
    @Operation(
        summary = "Search pool assets with a JSON filter body",
        description = "Unpaged; use POST /assets/search/page for large result sets.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Matching pool assets",
        content = [Content(array = ArraySchema(schema = Schema(implementation = AssetView::class)))],
    )
    fun search(@RequestBody body: AssetSearchRequest): List<AssetView> =
        assets.search(body)

    @PostMapping("/search/page")
    @Operation(summary = "Paged search of pool assets")
    @ApiResponse(
        responseCode = "200",
        description = "One page of matching assets plus the total count",
        content = [Content(schema = Schema(implementation = AssetSearchPage::class))],
    )
    fun searchPage(
        @RequestBody body: AssetSearchRequest,
        @Parameter(description = "One-based page number") @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
    ): AssetSearchPage = assets.searchPage(body, page, size)

    @PostMapping
    @Operation(
        summary = "Create a pool asset",
        description = "Creates the asset in the shared pool without attaching it to an application BOM.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Created asset",
            content = [Content(schema = Schema(implementation = AssetView::class))],
        ),
        ApiResponse(responseCode = "400", description = "Payload fails the asset schema"),
    )
    fun create(@RequestBody body: CreatePoolAssetRequest): AssetView =
        assets.create(body)

    @GetMapping("/duplicates")
    @Operation(
        summary = "Find duplicate assets for a type",
        description = "Groups pool assets that share the identity fields of their asset type.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Duplicate groups",
        content = [
            Content(array = ArraySchema(schema = Schema(implementation = AssetDuplicateGroup::class))),
        ],
    )
    fun duplicates(
        @Parameter(description = "Asset schema type name") @RequestParam type: String,
        @Parameter(description = "Restrict to one schema version; omit for all versions")
        @RequestParam(required = false) schemaVersion: String?,
    ): List<AssetDuplicateGroup> = assets.findDuplicates(type, schemaVersion)

    @GetMapping("/statistics")
    @Operation(summary = "Asset counts and owner breakdown for a type")
    @ApiResponse(
        responseCode = "200",
        description = "Counts per owner for the type",
        content = [Content(schema = Schema(implementation = AssetTypeStatistics::class))],
    )
    fun statistics(
        @Parameter(description = "Asset schema type name") @RequestParam type: String,
    ): AssetTypeStatistics =
        assets.statistics(type)

    @PutMapping("/{id}")
    @Operation(summary = "Update a pool asset payload or metadata")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Updated asset",
            content = [Content(schema = Schema(implementation = AssetView::class))],
        ),
        ApiResponse(responseCode = "400", description = "Payload fails the asset schema"),
        ApiResponse(responseCode = "404", description = "Asset not found"),
    )
    fun update(
        @Parameter(description = "Pool asset id") @PathVariable id: UUID,
        @RequestBody body: UpdatePoolAssetRequest,
    ): AssetView = assets.update(id, body)

    @GetMapping("/{id}")
    @Operation(
        summary = "Fetch a pool asset with usage detail",
        description = "Includes where the asset is used (applications, versions, BOMs).",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Asset with usage detail",
            content = [Content(schema = Schema(implementation = AssetDetailView::class))],
        ),
        ApiResponse(responseCode = "404", description = "Asset not found"),
    )
    fun get(
        @Parameter(description = "Pool asset id") @PathVariable id: UUID,
    ): AssetDetailView =
        assets.get(id)

    @PutMapping("/{id}/owner")
    @Operation(summary = "Set the owning application for a pool asset")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Asset with its new owner",
            content = [Content(schema = Schema(implementation = AssetView::class))],
        ),
        ApiResponse(responseCode = "404", description = "Asset or owning application not found"),
    )
    fun setOwner(
        @Parameter(description = "Pool asset id") @PathVariable id: UUID,
        @RequestBody body: SetAssetOwnerRequest,
    ): AssetView = assets.setOwner(id, body)
}
