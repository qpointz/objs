package org.poc.objs.sbom.service

import org.poc.objs.sbom.domain.CreatePortfolioRequest
import org.poc.objs.sbom.domain.CreateSubjectAreaRequest
import org.poc.objs.sbom.domain.PlaceApplicationRequest
import org.poc.objs.sbom.domain.PortfolioAppRef
import org.poc.objs.sbom.domain.PortfolioLevelApps
import org.poc.objs.sbom.domain.PortfolioOrigin
import org.poc.objs.sbom.domain.PortfolioSummary
import org.poc.objs.sbom.domain.PortfolioTreeView
import org.poc.objs.sbom.domain.PortfolioUniqueness
import org.poc.objs.sbom.domain.SubjectAreaView
import org.poc.objs.sbom.domain.UpdatePortfolioRequest
import org.poc.objs.sbom.domain.UpdateSubjectAreaRequest
import org.poc.objs.sbom.persistence.SbomApplicationRecord
import org.poc.objs.sbom.persistence.SbomApplicationRepository
import org.poc.objs.sbom.persistence.SbomApplicationVersionRepository
import org.poc.objs.sbom.persistence.SbomPortfolioMembershipRecord
import org.poc.objs.sbom.persistence.SbomPortfolioMembershipRepository
import org.poc.objs.sbom.persistence.SbomPortfolioNodeRecord
import org.poc.objs.sbom.persistence.SbomPortfolioNodeRepository
import org.poc.objs.sbom.persistence.SbomPortfolioRecord
import org.poc.objs.sbom.persistence.SbomPortfolioRepository
import org.poc.objs.sbom.uniqueness.PlacementCandidate
import org.poc.objs.sbom.uniqueness.PortfolioUniquenessRules
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class PortfolioService(
    private val portfolios: SbomPortfolioRepository,
    private val nodes: SbomPortfolioNodeRepository,
    private val memberships: SbomPortfolioMembershipRepository,
    private val applications: SbomApplicationRepository,
    private val versions: SbomApplicationVersionRepository,
    private val uniquenessRules: PortfolioUniquenessRules,
) {
    fun list(): List<PortfolioSummary> =
        portfolios.findAll().sortedBy { it.name.lowercase() }.map { it.toSummary() }

    fun getTree(portfolioId: UUID): PortfolioTreeView {
        val portfolio = requirePortfolio(portfolioId)
        val allNodes = nodes.findByPortfolioIdOrderBySortOrderAscNameAsc(portfolioId)
        val allMemberships = memberships.findByPortfolioId(portfolioId)
        val appsById = applications.findAllById(allMemberships.map { it.applicationId }.distinct())
            .associateBy { it.id }

        fun appsAt(nodeId: UUID?): List<PortfolioAppRef> =
            allMemberships
                .filter { it.nodeId == nodeId }
                .mapNotNull { m -> toAppRef(m, appsById) }
                .sortedBy { it.applicationName.lowercase() }

        fun leafCount(nodeId: UUID): Int {
            val ids = subtreeNodeIds(nodeId, allNodes)
            return allMemberships.count { it.nodeId != null && it.nodeId in ids }
        }

        fun build(parentId: UUID?): List<SubjectAreaView> =
            allNodes
                .filter { it.parentId == parentId }
                .map { n ->
                    SubjectAreaView(
                        id = n.id,
                        name = n.name,
                        description = n.description,
                        parentId = n.parentId,
                        leafCount = leafCount(n.id),
                        applications = appsAt(n.id),
                        children = build(n.id),
                    )
                }

        return PortfolioTreeView(
            portfolio = portfolio.toSummary(),
            subjectAreas = build(null),
            rootApplications = appsAt(null),
            rootLeafCount = allMemberships.size,
        )
    }

    @Transactional
    fun create(request: CreatePortfolioRequest): PortfolioSummary {
        val name = request.name.trim()
        if (name.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required")
        }
        val uniqueness = parseUniqueness(request.uniqueness)
        val origin = parseOrigin(request.origin)
        if (portfolios.findByNameIgnoreCase(name) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Portfolio name already exists: $name")
        }
        val now = Instant.now()
        val id = request.id ?: UUID.randomUUID()
        if (portfolios.existsById(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Portfolio already exists: $id")
        }
        return portfolios.save(
            SbomPortfolioRecord(
                id = id,
                name = name,
                description = request.description?.trim()?.takeIf { it.isNotEmpty() },
                uniqueness = uniqueness.name,
                origin = origin.name,
                source = request.source?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = now,
                updatedAt = now,
            ),
        ).toSummary()
    }

    @Transactional
    fun upsertById(request: CreatePortfolioRequest): PortfolioSummary {
        val id = request.id ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "id is required")
        val existing = portfolios.findById(id).orElse(null)
        return if (existing == null) {
            create(request)
        } else {
            update(
                id,
                UpdatePortfolioRequest(
                    name = request.name,
                    description = request.description,
                    uniqueness = request.uniqueness,
                    origin = request.origin,
                    source = request.source,
                ),
            )
        }
    }

    @Transactional
    fun update(portfolioId: UUID, request: UpdatePortfolioRequest): PortfolioSummary {
        val portfolio = requirePortfolio(portfolioId)
        request.name?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            val clash = portfolios.findByNameIgnoreCase(name)
            if (clash != null && clash.id != portfolioId) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Portfolio name already exists: $name")
            }
            portfolio.name = name
        }
        if (request.description != null) {
            portfolio.description = request.description.trim().takeIf { it.isNotEmpty() }
        }
        if (request.origin != null) {
            portfolio.origin = parseOrigin(request.origin).name
        }
        if (request.source != null) {
            portfolio.source = request.source.trim().takeIf { it.isNotEmpty() }
        }
        if (request.uniqueness != null) {
            val next = parseUniqueness(request.uniqueness)
            validatePolicyChange(portfolio, next)
            portfolio.uniqueness = next.name
        }
        portfolio.updatedAt = Instant.now()
        return portfolios.save(portfolio).toSummary()
    }

    @Transactional
    fun addSubjectArea(portfolioId: UUID, request: CreateSubjectAreaRequest): SubjectAreaView {
        requirePortfolio(portfolioId)
        val name = request.name.trim()
        if (name.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required")
        }
        request.parentId?.let { parentId ->
            nodes.findByIdAndPortfolioId(parentId, portfolioId)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown parent subject area")
        }
        val id = request.id ?: UUID.randomUUID()
        if (nodes.existsById(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Category already exists: $id")
        }
        val siblings = nodes.findByPortfolioIdOrderBySortOrderAscNameAsc(portfolioId)
            .filter { it.parentId == request.parentId }
        val saved =
            nodes.save(
                SbomPortfolioNodeRecord(
                    id = id,
                    portfolioId = portfolioId,
                    parentId = request.parentId,
                    name = name,
                    description = request.description?.trim()?.takeIf { it.isNotEmpty() },
                    sortOrder = siblings.size,
                ),
            )
        touch(portfolioId)
        return SubjectAreaView(
            id = saved.id,
            name = saved.name,
            description = saved.description,
            parentId = saved.parentId,
            leafCount = 0,
            applications = emptyList(),
            children = emptyList(),
        )
    }

    @Transactional
    fun upsertSubjectArea(portfolioId: UUID, request: CreateSubjectAreaRequest): SubjectAreaView {
        val id = request.id ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "id is required")
        val existing = nodes.findById(id).orElse(null)
        return if (existing == null) {
            addSubjectArea(portfolioId, request)
        } else {
            if (existing.portfolioId != portfolioId) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Category $id belongs to another portfolio")
            }
            existing.parentId = request.parentId
            nodes.save(existing)
            updateSubjectArea(
                portfolioId,
                id,
                UpdateSubjectAreaRequest(name = request.name, description = request.description),
            )
        }
    }

    @Transactional
    fun updateSubjectArea(
        portfolioId: UUID,
        nodeId: UUID,
        request: UpdateSubjectAreaRequest,
    ): SubjectAreaView {
        val node =
            nodes.findByIdAndPortfolioId(nodeId, portfolioId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")
        request.name?.trim()?.takeIf { it.isNotEmpty() }?.let { node.name = it }
        if (request.description != null) {
            node.description = request.description.trim().takeIf { it.isNotEmpty() }
        }
        nodes.save(node)
        touch(portfolioId)
        val tree = getTree(portfolioId)
        return findNode(tree.subjectAreas, nodeId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")
    }

    @Transactional
    fun deleteSubjectArea(portfolioId: UUID, nodeId: UUID) {
        requirePortfolio(portfolioId)
        val node =
            nodes.findByIdAndPortfolioId(nodeId, portfolioId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")
        val allNodes = nodes.findByPortfolioIdOrderBySortOrderAscNameAsc(portfolioId)
        val children = allNodes.any { it.parentId == nodeId }
        val placed = memberships.findByPortfolioId(portfolioId).any { it.nodeId == nodeId }
        if (children || placed) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Category still has subcategories or applications",
            )
        }
        nodes.delete(node)
        touch(portfolioId)
    }

    @Transactional
    fun placeApplication(portfolioId: UUID, request: PlaceApplicationRequest): PortfolioTreeView {
        val portfolio = requirePortfolio(portfolioId)
        applications.findById(request.applicationId).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown application")
        }
        request.subjectAreaId?.let { nodeId ->
            nodes.findByIdAndPortfolioId(nodeId, portfolioId)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown subject area")
        }
        request.versionId?.let { versionId ->
            versions.findByIdAndApplicationId(versionId, request.applicationId)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown application version")
        }
        val existing = memberships.findByPortfolioId(portfolioId)
        if (existing.any { it.nodeId == request.subjectAreaId && it.applicationId == request.applicationId }) {
            return getTree(portfolioId)
        }
        val policy = PortfolioUniqueness.parse(portfolio.uniqueness)
        uniquenessRules.rule(policy).conflict(
            portfolio,
            existing,
            PlacementCandidate(request.applicationId, request.subjectAreaId, request.versionId),
        )?.let { throw ResponseStatusException(HttpStatus.CONFLICT, it) }
        memberships.save(
            SbomPortfolioMembershipRecord(
                portfolioId = portfolioId,
                nodeId = request.subjectAreaId,
                applicationId = request.applicationId,
                versionId = request.versionId,
            ),
        )
        touch(portfolioId)
        return getTree(portfolioId)
    }

    @Transactional
    fun removePlacement(portfolioId: UUID, placementId: UUID): PortfolioTreeView {
        requirePortfolio(portfolioId)
        val row =
            memberships.findByIdAndPortfolioId(placementId, portfolioId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Placement not found")
        memberships.delete(row)
        touch(portfolioId)
        return getTree(portfolioId)
    }

    @Transactional
    fun removePlacements(portfolioId: UUID, placementIds: List<UUID>): PortfolioTreeView {
        for (pid in placementIds.distinct()) {
            removePlacement(portfolioId, pid)
        }
        return getTree(portfolioId)
    }

    @Transactional
    fun movePlacements(portfolioId: UUID, placementIds: List<UUID>, subjectAreaId: UUID?): PortfolioTreeView {
        requirePortfolio(portfolioId)
        subjectAreaId?.let { nodeId ->
            nodes.findByIdAndPortfolioId(nodeId, portfolioId)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown subject area")
        }
        val existing = memberships.findByPortfolioId(portfolioId)
        for (pid in placementIds.distinct()) {
            val row =
                memberships.findByIdAndPortfolioId(pid, portfolioId)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Placement not found")
            if (row.nodeId == subjectAreaId) continue
            if (existing.any { it.id != row.id && it.nodeId == subjectAreaId && it.applicationId == row.applicationId }) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Application already in this category")
            }
            row.nodeId = subjectAreaId
            memberships.save(row)
        }
        touch(portfolioId)
        return getTree(portfolioId)
    }

    @Transactional
    fun removeApplication(portfolioId: UUID, applicationId: UUID): PortfolioTreeView {
        requirePortfolio(portfolioId)
        val row =
            memberships.findByPortfolioIdAndApplicationId(portfolioId, applicationId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Application not in portfolio")
        memberships.delete(row)
        touch(portfolioId)
        return getTree(portfolioId)
    }

    fun applicationsForLevel(
        portfolioId: UUID,
        level: String,
        includeSubcategories: Boolean = true,
        page: Int = 1,
        size: Int = 50,
        q: String? = null,
    ): PortfolioLevelApps {
        val query = q?.trim().orEmpty()
        val refs =
            applicationsInScope(portfolioId, level, includeSubcategories).filter { ref ->
                if (query.isEmpty()) true
                else {
                    val hay = "${ref.applicationName} ${ref.applicationDescription.orEmpty()}".lowercase()
                    hay.contains(query.lowercase())
                }
            }
        val p = page.coerceAtLeast(1)
        val s = size.coerceIn(1, 200)
        val from = ((p - 1) * s).coerceAtMost(refs.size)
        val to = (from + s).coerceAtMost(refs.size)
        return PortfolioLevelApps(
            portfolioId = portfolioId,
            level = level.trim(),
            includeSubcategories = includeSubcategories,
            applications = refs.subList(from, to),
            total = refs.size,
        )
    }

    fun applicationsInScope(
        portfolioId: UUID,
        level: String,
        includeSubcategories: Boolean,
    ): List<PortfolioAppRef> {
        requirePortfolio(portfolioId)
        val allNodes = nodes.findByPortfolioIdOrderBySortOrderAscNameAsc(portfolioId)
        val allMemberships = memberships.findByPortfolioId(portfolioId)
        val trimmed = level.trim()
        val selected =
            if (trimmed.equals("root", ignoreCase = true)) {
                if (includeSubcategories) {
                    allMemberships
                } else {
                    allMemberships.filter { it.nodeId == null }
                }
            } else {
                val rootNode =
                    runCatching { UUID.fromString(trimmed) }.getOrNull()
                        ?.let { nodes.findByIdAndPortfolioId(it, portfolioId) }
                        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown level: $level")
                if (includeSubcategories) {
                    val subtree = subtreeNodeIds(rootNode.id, allNodes)
                    allMemberships.filter { it.nodeId != null && it.nodeId in subtree }
                } else {
                    allMemberships.filter { it.nodeId == rootNode.id }
                }
            }
        val appsById = applications.findAllById(selected.map { it.applicationId }.distinct())
            .associateBy { it.id }
        return selected.mapNotNull { toAppRef(it, appsById) }
            .sortedBy { it.applicationName.lowercase() }
    }

    private fun validatePolicyChange(portfolio: SbomPortfolioRecord, next: PortfolioUniqueness) {
        val existing = memberships.findByPortfolioId(portfolio.id)
        val rule = uniquenessRules.rule(next)
        for (row in existing) {
            rule.conflict(
                portfolio,
                existing,
                PlacementCandidate(row.applicationId, row.nodeId, row.versionId, row.id),
            )?.let {
                throw ResponseStatusException(HttpStatus.CONFLICT, it)
            }
        }
    }

    private fun parseUniqueness(raw: String?): PortfolioUniqueness =
        try {
            PortfolioUniqueness.parse(raw)
        } catch (ex: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
        }

    private fun parseOrigin(raw: String?): PortfolioOrigin =
        try {
            PortfolioOrigin.parse(raw)
        } catch (ex: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
        }

    private fun toAppRef(
        m: SbomPortfolioMembershipRecord,
        apps: Map<UUID, SbomApplicationRecord>,
    ): PortfolioAppRef? {
        val app = apps[m.applicationId] ?: return null
        return PortfolioAppRef(
            applicationId = m.applicationId,
            applicationName = app.name,
            applicationDescription = app.description,
            placementId = m.id,
            nodeId = m.nodeId,
            versionId = m.versionId,
        )
    }

    private fun findNode(nodes: List<SubjectAreaView>, id: UUID): SubjectAreaView? {
        for (n in nodes) {
            if (n.id == id) return n
            findNode(n.children, id)?.let { return it }
        }
        return null
    }

    private fun subtreeNodeIds(rootId: UUID, all: List<SbomPortfolioNodeRecord>): Set<UUID> {
        val byParent = all.groupBy { it.parentId }
        val out = linkedSetOf(rootId)
        val queue = ArrayDeque<UUID>().apply { add(rootId) }
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            for (child in byParent[id].orEmpty()) {
                if (out.add(child.id)) queue.add(child.id)
            }
        }
        return out
    }

    private fun touch(portfolioId: UUID) {
        val p = requirePortfolio(portfolioId)
        p.updatedAt = Instant.now()
        portfolios.save(p)
    }

    private fun requirePortfolio(id: UUID): SbomPortfolioRecord =
        portfolios.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found: $id")
        }

    private fun SbomPortfolioRecord.toSummary() =
        PortfolioSummary(
            id = id,
            name = name,
            description = description,
            uniqueness = PortfolioUniqueness.parse(uniqueness),
            origin = PortfolioOrigin.parse(origin),
            source = source,
        )
}
