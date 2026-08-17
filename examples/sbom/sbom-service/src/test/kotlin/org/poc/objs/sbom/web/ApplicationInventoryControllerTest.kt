package org.poc.objs.sbom.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.poc.objs.sbom.domain.ApplicationFingerprintSummary
import org.poc.objs.sbom.domain.ApplicationPortalStats
import org.poc.objs.sbom.domain.ApplicationSummary
import org.poc.objs.sbom.domain.ApplicationVersionSummary
import org.poc.objs.sbom.domain.BomSummary
import org.poc.objs.sbom.domain.CombinedBomView
import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.domain.CreateBomRequest
import org.poc.objs.sbom.domain.CreateDraftVersionRequest
import org.poc.objs.sbom.domain.CreateFingerprintRequest
import org.poc.objs.sbom.domain.PatchVersionRequest
import org.poc.objs.sbom.domain.VersionBomView
import org.poc.objs.sbom.service.ApplicationBomService
import org.poc.objs.sbom.service.ApplicationInventoryService
import org.poc.objs.sbom.service.ApplicationVersionService
import org.poc.objs.sbom.service.CycloneDxExportService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID

class ApplicationInventoryControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var inventory: ApplicationInventoryService
    private lateinit var versions: ApplicationVersionService
    private lateinit var boms: ApplicationBomService

    @BeforeEach
    fun setUp() {
        inventory = mock(ApplicationInventoryService::class.java)
        versions = mock(ApplicationVersionService::class.java)
        boms = mock(ApplicationBomService::class.java)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    ApplicationInventoryController(
                        inventory,
                        versions,
                        boms,
                        mock(CycloneDxExportService::class.java),
                    ),
                )
                .setMessageConverters(JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
                .build()
    }

    @Test
    fun shouldRequireTargetVersionOnCreate() {
        mockMvc.perform(
            post("/api/v1/inventory/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Payments"}"""),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldCreateApplicationWithTargetVersion() {
        val id = UUID.randomUUID()
        given(inventory.create(CreateApplicationRequest(name = "Payments", targetVersion = "1.0.0")))
            .willReturn(ApplicationSummary(id, "Payments", null, emptyList()))
        mockMvc.perform(
            post("/api/v1/inventory/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Payments","targetVersion":"1.0.0"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Payments"))
    }

    @Test
    fun shouldReturnPortalStats() {
        val id = UUID.randomUUID()
        given(inventory.portalStats(id)).willReturn(
            ApplicationPortalStats(
                applicationId = id,
                versionCount = 2,
                bomCount = 3,
                latestVersion = null,
                latestMultiBom = false,
            ),
        )
        mockMvc.perform(get("/api/v1/inventory/applications/$id/stats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.versionCount").value(2))
            .andExpect(jsonPath("$.bomCount").value(3))
    }

    @Test
    fun shouldRejectCombinedPut() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        mockMvc.perform(
            put("/api/v1/inventory/applications/$appId/versions/$versionId/combined")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun shouldListBoms() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        val bomId = UUID.randomUUID()
        given(boms.list(appId, versionId)).willReturn(
            listOf(BomSummary(bomId, versionId, "BOM", null, emptyList(), 0)),
        )
        mockMvc.perform(get("/api/v1/inventory/applications/$appId/versions/$versionId/sboms"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("BOM"))
    }

    @Test
    fun shouldCreateBom() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        val bomId = UUID.randomUUID()
        given(boms.create(appId, versionId, CreateBomRequest(name = "runtime")))
            .willReturn(BomSummary(bomId, versionId, "runtime", null, emptyList(), 1))
        mockMvc.perform(
            post("/api/v1/inventory/applications/$appId/versions/$versionId/sboms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"runtime"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("runtime"))
    }

    @Test
    fun shouldRequireFingerprintNameAndCategory() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        mockMvc.perform(
            post("/api/v1/inventory/applications/$appId/versions/$versionId/fingerprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldCreateFingerprintWithNameAndCategory() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        val fpId = UUID.randomUUID()
        given(versions.fingerprint(appId, versionId, CreateFingerprintRequest(name = "gate", category = "approval")))
            .willReturn(
                ApplicationFingerprintSummary(
                    id = fpId,
                    versionId = versionId,
                    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                    note = "gate",
                    name = "gate",
                    category = "approval",
                    contentSha256 = "abc",
                ),
            )
        mockMvc.perform(
            post("/api/v1/inventory/applications/$appId/versions/$versionId/fingerprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"gate","category":"approval"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("gate"))
            .andExpect(jsonPath("$.category").value("approval"))
    }

    @Test
    fun shouldReturnCombinedView() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        given(boms.combined(appId, versionId, null)).willReturn(sampleCombined(appId, versionId))
        mockMvc.perform(get("/api/v1/inventory/applications/$appId/versions/$versionId/combined"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.combinedTags[0]").value("app"))
    }

    @Test
    fun shouldReturnVersionAsCombinedSbom() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        given(boms.combined(appId, versionId, null)).willReturn(sampleCombined(appId, versionId))
        mockMvc.perform(get("/api/v1/inventory/applications/$appId/versions/$versionId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.combinedTags[0]").value("app"))
            .andExpect(jsonPath("$.version.version").value("0.1.0"))
    }

    @Test
    fun shouldRequireBasedOnWhenCreatingDraft() {
        val appId = UUID.randomUUID()
        mockMvc.perform(
            post("/api/v1/inventory/applications/$appId/versions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"targetVersion":"1.1.0"}"""),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldCreateDraftFromVersion() {
        val appId = UUID.randomUUID()
        val fromId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        given(
            versions.createDraft(
                appId,
                CreateDraftVersionRequest(targetVersion = "1.1.0", fromVersionId = fromId),
            ),
        ).willReturn(
            VersionBomView(
                version = sampleVersion(appId, versionId),
                applicationName = "Payments",
                assets = emptyList(),
                relations = emptyList(),
                combinedTags = emptyList(),
            ),
        )
        mockMvc.perform(
            post("/api/v1/inventory/applications/$appId/versions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"targetVersion":"1.1.0","fromVersionId":"$fromId"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.version.id").value(versionId.toString()))
    }

    @Test
    fun shouldPatchDraftMetadata() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        given(
            versions.patchDraft(
                appId,
                versionId,
                PatchVersionRequest(version = "1.2.0", tags = listOf("core")),
            ),
        ).willReturn(sampleVersion(appId, versionId, version = "1.2.0"))
        mockMvc.perform(
            patch("/api/v1/inventory/applications/$appId/versions/$versionId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"version":"1.2.0","tags":["core"]}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value("1.2.0"))
    }

    @Test
    fun shouldRejectDeletingLastBom() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        val bomId = UUID.randomUUID()
        doThrow(ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the last BOM"))
            .`when`(boms)
            .delete(appId, versionId, bomId)
        mockMvc.perform(delete("/api/v1/inventory/applications/$appId/versions/$versionId/sboms/$bomId"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectInvalidFingerprintCategory() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        given(
            versions.fingerprint(appId, versionId, CreateFingerprintRequest(name = "gate", category = "nope")),
        ).willThrow(ResponseStatusException(HttpStatus.BAD_REQUEST, "category must be approval, history, or unknown"))
        mockMvc.perform(
            post("/api/v1/inventory/applications/$appId/versions/$versionId/fingerprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"gate","category":"nope"}"""),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldPreviewDeleteDependents() {
        val appId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        val childId = UUID.randomUUID()
        given(versions.deleteImpact(appId, versionId)).willReturn(listOf(sampleVersion(appId, childId, version = "0.2.0")))
        mockMvc.perform(get("/api/v1/inventory/applications/$appId/versions/$versionId/dependents"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(childId.toString()))
    }

    private fun sampleCombined(appId: UUID, versionId: UUID) =
        CombinedBomView(
            version = sampleVersion(appId, versionId),
            applicationName = "Payments",
            assets = emptyList(),
            relations = emptyList(),
            combinedTags = listOf("app"),
            selectedBomIds = emptyList(),
        )

    private fun sampleVersion(appId: UUID, versionId: UUID, version: String = "0.1.0") =
        ApplicationVersionSummary(
            id = versionId,
            applicationId = appId,
            status = "DRAFT",
            version = version,
            label = version,
            capturedAt = Instant.parse("2026-01-01T00:00:00Z"),
            promotedAt = null,
        )
}
