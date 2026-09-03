package org.poc.objs.sbom.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import org.poc.objs.api.domain.AllowedEdgeCatalog
import org.poc.objs.api.domain.SchemaCatalog
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.persistence.NamedGraphStore
import org.poc.objs.autoconfigure.ObjsCoreAutoConfiguration
import org.poc.objs.sbom.domain.CreateApplicationRequest
import org.poc.objs.sbom.domain.CreateBomRequest
import org.poc.objs.sbom.domain.CreateDraftVersionRequest
import org.poc.objs.sbom.domain.CreateFingerprintRequest
import org.poc.objs.sbom.domain.DraftAssetWrite
import org.poc.objs.sbom.domain.DraftRelationWrite
import org.poc.objs.sbom.domain.PromoteVersionRequest
import org.poc.objs.sbom.domain.RenameVersionRequest
import org.poc.objs.sbom.domain.ReplaceVersionBomRequest
import org.poc.objs.sbom.domain.UpdateBomRequest
import org.poc.objs.sbom.persistence.SbomApplicationSbomRepository
import org.poc.objs.sbom.persistence.SbomPersistenceConfiguration
import org.poc.objs.sbom.registry.SbomRoles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import java.util.UUID
import javax.sql.DataSource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(
    SbomPersistenceConfiguration::class,
    SbomService::class,
    ApplicationInventoryService::class,
    ApplicationVersionService::class,
    AssetTypeCatalogService::class,
    ApplicationBomService::class,
    BomGraphSupport::class,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-versions;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class ApplicationVersionServiceTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var inventory: ApplicationInventoryService

    @Autowired
    lateinit var versions: ApplicationVersionService

    @Autowired
    lateinit var boms: SbomApplicationSbomRepository

    @Autowired
    lateinit var bomService: ApplicationBomService

    @Autowired
    lateinit var namedGraphs: NamedGraphStore

    @Autowired
    lateinit var graphStore: GraphStore

    @Autowired
    lateinit var dataSource: DataSource

    @Autowired
    lateinit var sbom: SbomService

    @Autowired
    lateinit var schemas: SchemaCatalog

    @Autowired
    lateinit var edges: AllowedEdgeCatalog

    @BeforeEach
    fun reset() {
        schemas.clear()
        edges.clear()
        val field = SbomService::class.java.getDeclaredField("packRegistered")
        field.isAccessible = true
        field.setBoolean(sbom, false)
        sbom.ensureRegistry()
    }

    @Test
    fun shouldPersistBootstrapBomNamedBom() {
        val app = inventory.create(CreateApplicationRequest(name = "BomApp"))
        val draft = versions.draft(app.id)!!
        assertThat(draft.version).isEqualTo("0.1.0")
        val rows = boms.findByVersionIdOrderBySortOrderAscIdAsc(draft.id)
        assertThat(rows).hasSize(1)
        assertThat(rows[0].name).isEqualTo("BOM")
        assertThat(rows[0].tags).isEmpty()
        val graph = namedGraphs.get(rows[0].graphId)!!
        assertThat(graph.annotations["kind"]).isEqualTo("application-bom")
        assertThat(graph.annotations["bomId"]).isEqualTo(rows[0].id.toString())
    }

    @Test
    fun shouldMigrateSchemaOffVersionGraphId() {
        dataSource.connection.use { connection ->
            connection.metaData.getColumns(null, null, "SBOM_APPLICATION_VERSION", "GRAPH_ID").use { rs ->
                assertThat(rs.next()).isFalse()
            }
            connection.metaData.getTables(null, null, "SBOM_APPLICATION_SBOM", null).use { rs ->
                assertThat(rs.next()).isTrue()
            }
        }
    }

    @Test
    fun shouldPromoteDraftInPlaceAndKeepPoolAssetIds() {
        val app = inventory.create(CreateApplicationRequest(name = "VerApp"))
        val assetId = addComponent(app.id, "lib")
        val second = addComponent(app.id, "other")
        inventory.addRelation(
            app.id,
            DraftRelationWrite(fromAssetId = assetId, toAssetId = second, role = SbomRoles.DEPENDS_ON),
        )
        val draftId = versions.draft(app.id)!!.id
        val promoted = versions.promote(app.id, draftId, PromoteVersionRequest("1.0.0"))
        assertThat(promoted.version.status).isEqualTo("RELEASED")
        assertThat(promoted.version.version).isEqualTo("1.0.0")
        assertThat(promoted.version.id).isEqualTo(draftId)
        assertThat(promoted.assets.map { it.id }).containsExactlyInAnyOrder(assetId, second)
        assertThat(versions.latest(app.id)!!.id).isEqualTo(draftId)
        assertThat(versions.draft(app.id)).isNull()
    }

    @Test
    fun shouldReplaceBomOnReleasedVersion() {
        val app = inventory.create(CreateApplicationRequest(name = "EditReleased"))
        val first = addComponent(app.id, "keep")
        val extra = addComponent(app.id, "drop")
        val released = versions.promote(app.id, versions.draft(app.id)!!.id, PromoteVersionRequest("2.0"))
        val updated =
            versions.replaceBom(
                app.id,
                released.version.id,
                ReplaceVersionBomRequest(assetIds = listOf(first), relations = emptyList()),
            )
        assertThat(updated.assets.map { it.id }).containsExactly(first)
        assertThat(updated.assets.map { it.id }).doesNotContain(extra)
        assertThat(updated.version.status).isEqualTo("RELEASED")
    }

    @Test
    fun shouldCreateDraftFromReleasedVersion() {
        val app = inventory.create(CreateApplicationRequest(name = "CloneApp"))
        val assetId = addComponent(app.id, "lib")
        val released = versions.promote(app.id, versions.draft(app.id)!!.id, PromoteVersionRequest("1.0"))
        val draft = versions.createDraft(app.id, CreateDraftVersionRequest(fromVersionId = released.version.id))
        assertThat(draft.version.status).isEqualTo("DRAFT")
        assertThat(draft.assets.map { it.id }).containsExactly(assetId)
        assertThat(draft.version.id).isNotEqualTo(released.version.id)
    }

    @Test
    fun shouldFingerprintWithoutChangingLiveGraph() {
        val app = inventory.create(CreateApplicationRequest(name = "FpApp"))
        val first = addComponent(app.id, "a")
        val draftId = versions.draft(app.id)!!.id
        val fp = versions.fingerprint(app.id, draftId, CreateFingerprintRequest(note = "gate"))
        assertThat(fp.contentSha256).isNotBlank()
        assertThat(fp.note).isEqualTo("gate")
        assertThat(fp.name).isEqualTo("gate")
        assertThat(fp.category).isEqualTo("unknown")
        addComponent(app.id, "b")
        val later = versions.fingerprint(app.id, draftId)
        assertThat(later.contentSha256).isNotEqualTo(fp.contentSha256)
        val listed = versions.listFingerprints(app.id, draftId)
        assertThat(listed).hasSize(2)
        assertThat(listed.map { it.id }).contains(fp.id, later.id)
        val live = versions.getBom(app.id, draftId)
        assertThat(live.assets.map { it.id }).contains(first)
        assertThat(live.assets).hasSize(2)
        val snapshot = versions.getFingerprintBom(app.id, draftId, fp.id)
        assertThat(snapshot.assets).hasSize(1)
        assertThat(snapshot.assets.map { it.id }).containsExactly(first)
        val pinnedName = snapshot.assets.single().label
        val liveEntity = graphStore.getEntity(first)!!
        liveEntity.payload["name"] = "a-edited"
        graphStore.upsertEntities(listOf(liveEntity))
        val snapshotAfterEdit = versions.getFingerprintBom(app.id, draftId, fp.id)
        assertThat(snapshotAfterEdit.assets.single().label).isEqualTo(pinnedName)
        assertThat(graphStore.getEntity(first)!!.payload["name"]).isEqualTo("a-edited")
        val thrown =
            assertThrows<ResponseStatusException> {
                versions.rejectFingerprintWrite(app.id, draftId, fp.id)
            }
        assertThat(thrown.statusCode.value()).isEqualTo(403)
        assertThat(thrown.reason).contains("immutable")
    }

    @Test
    fun shouldReverseLookupPinnedGraphAfterLiveDetach() {
        val app = inventory.create(CreateApplicationRequest(name = "PinUsage"))
        val assetId = addComponent(app.id, "pinned-lib")
        val draftId = versions.draft(app.id)!!.id
        val before = namedGraphs.listGraphIdsForEntity(assetId).toSet()
        versions.fingerprint(app.id, draftId, CreateFingerprintRequest(note = "gate"))
        val fpGraphId = (namedGraphs.listGraphIdsForEntity(assetId).toSet() - before).single()
        namedGraphs.detach(fpGraphId, assetId)

        assertThat(namedGraphs.listGraphIdsForEntity(assetId)).contains(fpGraphId)
    }

    @Test
    fun shouldInferVersionDepsViaSharedPoolAssets() {
        val a = inventory.create(CreateApplicationRequest(name = "A"))
        val b = inventory.create(CreateApplicationRequest(name = "B"))
        val assetId = addComponent(a.id, "shared")
        inventory.addAsset(b.id, DraftAssetWrite(assetId = assetId))
        val verA = versions.promote(a.id, versions.draft(a.id)!!.id, PromoteVersionRequest("1"))
        versions.promote(b.id, versions.draft(b.id)!!.id, PromoteVersionRequest("1"))
        val deps = versions.inferDependsOn(a.id, verA.version.id)
        assertThat(deps).hasSize(1)
        assertThat(deps[0].applicationName).isEqualTo("B")
        assertThat(deps[0].sharedAssetIds).containsExactly(assetId)
    }

    @Test
    fun shouldAllowParallelDraftsWithDistinctTargets() {
        val app = inventory.create(CreateApplicationRequest(name = "Parallel", targetVersion = "1.0.0"))
        val first = versions.draft(app.id)!!
        val second =
            versions.createDraft(
                app.id,
                CreateDraftVersionRequest(fromVersionId = first.id, targetVersion = "2.0.0"),
            )
        assertThat(versions.drafts(app.id).map { it.version }).containsExactlyInAnyOrder("1.0.0", "2.0.0")
        assertThat(second.version.basedOnVersionId).isEqualTo(first.id)
        val renamed = versions.renameDraft(app.id, first.id, RenameVersionRequest("1.1.0"))
        assertThat(renamed.version).isEqualTo("1.1.0")
        val clash =
            assertThrows<ResponseStatusException> {
                versions.renameDraft(app.id, first.id, RenameVersionRequest("2.0.0"))
            }
        assertThat(clash.statusCode.value()).isEqualTo(409)
    }

    @Test
    fun shouldKeepSplitOrFlattenWhenCopyingMultiBomDraft() {
        val app = inventory.create(CreateApplicationRequest(name = "SplitApp", targetVersion = "0.1.0"))
        val draftId = versions.draft(app.id)!!.id
        addComponent(app.id, "core")
        bomService.create(app.id, draftId, CreateBomRequest(name = "runtime", tags = listOf("rt")))
        val kept =
            versions.createDraft(
                app.id,
                CreateDraftVersionRequest(fromVersionId = draftId, targetVersion = "0.2.0", combineConstituents = false),
            )
        assertThat(bomService.list(app.id, kept.version.id).map { it.name }).containsExactly("BOM", "runtime")
        val flat =
            versions.createDraft(
                app.id,
                CreateDraftVersionRequest(fromVersionId = draftId, targetVersion = "0.3.0", combineConstituents = true),
            )
        val flatBoms = bomService.list(app.id, flat.version.id)
        assertThat(flatBoms).hasSize(1)
        assertThat(flatBoms[0].name).isEqualTo("BOM")
        assertThat(flatBoms[0].tags).containsExactly("rt")
        assertThat(flat.assets.map { it.label }).anyMatch { it.startsWith("core") }
    }

    @Test
    fun shouldReuseCopiedBomNamesOnNextDraft() {
        val app = inventory.create(CreateApplicationRequest(name = "SeedCopy", targetVersion = "1.0.0"))
        val draftId = versions.draft(app.id)!!.id
        val primary = bomService.list(app.id, draftId).single()
        bomService.update(app.id, draftId, primary.id, UpdateBomRequest(name = "Build", tags = listOf("build")))
        bomService.create(app.id, draftId, CreateBomRequest(name = "Runtime", tags = listOf("runtime")))
        val released = versions.promote(app.id, draftId, PromoteVersionRequest("1.0.0"))
        val next =
            versions.createDraft(
                app.id,
                CreateDraftVersionRequest(
                    fromVersionId = released.version.id,
                    targetVersion = "1.1.0",
                    combineConstituents = false,
                ),
            )
        val copied = bomService.list(app.id, next.version.id)
        assertThat(copied.map { it.name }).containsExactly("Build", "Runtime")
        val clash =
            assertThrows<ResponseStatusException> {
                bomService.create(app.id, next.version.id, CreateBomRequest(name = "Runtime"))
            }
        assertThat(clash.statusCode.value()).isEqualTo(409)
        copied.forEach { row ->
            versions.replaceBom(app.id, next.version.id, ReplaceVersionBomRequest(emptyList(), emptyList()), row.id)
        }
        assertThat(bomService.list(app.id, next.version.id).map { it.name }).containsExactly("Build", "Runtime")
    }

    @Test
    fun shouldFingerprintUnionWithoutCopyingBomRows() {
        val app = inventory.create(CreateApplicationRequest(name = "FpUnion"))
        val draftId = versions.draft(app.id)!!.id
        addComponent(app.id, "a")
        bomService.create(app.id, draftId, CreateBomRequest(name = "extra"))
        val before = boms.countByVersionId(draftId)
        val fp = versions.fingerprint(app.id, draftId, CreateFingerprintRequest(name = "gate", category = "history"))
        assertThat(fp.name).isEqualTo("gate")
        assertThat(fp.category).isEqualTo("history")
        assertThat(boms.countByVersionId(draftId)).isEqualTo(before)
        assertThat(bomService.list(app.id, draftId)).hasSize(2)
    }

    @Test
    fun shouldForbidDeletingLastBomAndCascadeDependentDrafts() {
        val app = inventory.create(CreateApplicationRequest(name = "DelApp", targetVersion = "0.1.0"))
        val parent = versions.draft(app.id)!!
        val only = bomService.list(app.id, parent.id).single()
        val last =
            assertThrows<ResponseStatusException> {
                bomService.delete(app.id, parent.id, only.id)
            }
        assertThat(last.statusCode.value()).isEqualTo(400)
        val child =
            versions.createDraft(
                app.id,
                CreateDraftVersionRequest(fromVersionId = parent.id, targetVersion = "0.2.0"),
            )
        val blocked =
            assertThrows<ResponseStatusException> {
                versions.deleteDraft(app.id, parent.id)
            }
        assertThat(blocked.statusCode.value()).isEqualTo(409)
        versions.deleteDraft(app.id, parent.id, confirmDependents = true)
        assertThat(versions.list(app.id).map { it.id }).doesNotContain(parent.id, child.version.id)
    }

    private fun addComponent(appId: UUID, name: String) =
        inventory.addAsset(
            appId,
            DraftAssetWrite(
                type = "Component",
                payload =
                    mapOf(
                        "name" to name,
                        "version" to "1",
                        "ecosystem" to "maven",
                        "kind" to "library",
                    ),
            ),
        ).assets.first { it.label.startsWith(name) }.id
}
