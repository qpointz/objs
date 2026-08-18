import type {
  ApplicationFingerprintSummary,
  ApplicationPortalStats,
  ApplicationSummary,
  ApplicationVersionSummary,
  AssetDetailView,
  AssetDuplicateGroup,
  AssetRelationshipSpec,
  AssetTypeDetail,
  AssetTypeStatistics,
  AssetTypeSummary,
  AssetSearchPage,
  AssetView,
  BomSummary,
  BoMSchema,
  CombinedBomView,
  SchemaCatalogEntry,
  SchemaUsedInRef,
  TypeAllowedEdges,
  InferredAppDependency,
  CategoryAssetPage,
  MiReportTable,
  PortfolioLevelApps,
  PortfolioSummary,
  PortfolioTreeView,
  SubjectAreaView,
  VersionBomView,
} from './types'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    ...init,
  })
  if (!res.ok) {
    let detail = res.statusText
    try {
      const body = await res.json()
      detail = body.message ?? body.error ?? JSON.stringify(body)
    } catch {
      // ignore
    }
    throw new Error(detail || `Request failed (${res.status})`)
  }
  if (res.status === 204) {
    return undefined as T
  }
  const text = await res.text()
  if (!text) {
    return undefined as T
  }
  return JSON.parse(text) as T
}

const apps = '/api/v1/inventory/applications'
const assets = '/api/v1/inventory/assets'
const portfolios = '/api/v1/inventory/portfolios'
const schema = '/api/v1/inventory'

export const api = {
  listApplications: (q?: string) =>
    request<ApplicationSummary[]>(q ? `${apps}?q=${encodeURIComponent(q)}` : apps),

  createApplication: (body: { name: string; description?: string; targetVersion: string; tags?: string[] }) =>
    request<ApplicationSummary>(apps, { method: 'POST', body: JSON.stringify(body) }),

  getApplication: (id: string) => request<ApplicationSummary>(`${apps}/${id}`),

  updateApplication: (id: string, body: { name?: string; description?: string | null; tags?: string[] }) =>
    request<ApplicationSummary>(`${apps}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),

  getApplicationStats: (id: string) => request<ApplicationPortalStats>(`${apps}/${id}/stats`),

  listVersions: (id: string) => request<ApplicationVersionSummary[]>(`${apps}/${id}/versions`),

  createDraftVersion: (
    id: string,
    body: {
      targetVersion: string
      fromVersionId?: string
      fromFingerprintId?: string
      combineConstituents?: boolean
    },
  ) =>
    request<VersionBomView>(`${apps}/${id}/versions`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  getVersion: (id: string, versionId: string) =>
    request<VersionBomView>(`${apps}/${id}/versions/${versionId}`),

  patchVersion: (id: string, versionId: string, body: { version?: string; tags?: string[] }) =>
    request<ApplicationVersionSummary>(`${apps}/${id}/versions/${versionId}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }),

  deleteVersion: (id: string, versionId: string, confirmDependents = false) =>
    request<void>(
      `${apps}/${id}/versions/${versionId}?confirmDependents=${confirmDependents}`,
      { method: 'DELETE' },
    ),

  listVersionDependents: (id: string, versionId: string) =>
    request<ApplicationVersionSummary[]>(`${apps}/${id}/versions/${versionId}/dependents`),

  getCombined: (id: string, versionId: string, sbomIds?: string[]) => {
    const q =
      sbomIds && sbomIds.length > 0
        ? `?${sbomIds.map((bomId) => `sbomIds=${encodeURIComponent(bomId)}`).join('&')}`
        : ''
    return request<CombinedBomView>(`${apps}/${id}/versions/${versionId}/combined${q}`)
  },

  listBoms: (id: string, versionId: string) =>
    request<BomSummary[]>(`${apps}/${id}/versions/${versionId}/sboms`),

  createBom: (id: string, versionId: string, body: { name: string; description?: string; tags?: string[] }) =>
    request<BomSummary>(`${apps}/${id}/versions/${versionId}/sboms`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  getBom: (id: string, versionId: string, sbomId: string) =>
    request<VersionBomView>(`${apps}/${id}/versions/${versionId}/sboms/${sbomId}`),

  saveBom: (
    id: string,
    versionId: string,
    sbomId: string,
    body: { assetIds: string[]; relations: { fromAssetId: string; toAssetId: string; role: string }[] },
  ) =>
    request<VersionBomView>(`${apps}/${id}/versions/${versionId}/sboms/${sbomId}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  patchBom: (
    id: string,
    versionId: string,
    sbomId: string,
    body: { name?: string; description?: string | null; tags?: string[] },
  ) =>
    request<BomSummary>(`${apps}/${id}/versions/${versionId}/sboms/${sbomId}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }),

  deleteBom: (id: string, versionId: string, sbomId: string) =>
    request<void>(`${apps}/${id}/versions/${versionId}/sboms/${sbomId}`, { method: 'DELETE' }),

  saveVersionBom: (
    id: string,
    versionId: string,
    body: { assetIds: string[]; relations: { fromAssetId: string; toAssetId: string; role: string }[] },
  ) =>
    request<VersionBomView>(`${apps}/${id}/versions/${versionId}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  promoteVersion: (id: string, versionId: string, version: string) =>
    request<VersionBomView>(`${apps}/${id}/versions/${versionId}/promote`, {
      method: 'POST',
      body: JSON.stringify({ version }),
    }),

  listFingerprints: (id: string, versionId: string) =>
    request<ApplicationFingerprintSummary[]>(`${apps}/${id}/versions/${versionId}/fingerprints`),

  getFingerprint: (id: string, versionId: string, fingerprintId: string) =>
    request<VersionBomView>(`${apps}/${id}/versions/${versionId}/fingerprints/${fingerprintId}`),

  createFingerprint: (id: string, versionId: string, body: { name: string; category: string }) =>
    request<ApplicationFingerprintSummary>(`${apps}/${id}/versions/${versionId}/fingerprints`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  dependsOnVersion: (id: string, versionId: string) =>
    request<InferredAppDependency[]>(`${apps}/${id}/versions/${versionId}/depends-on`),

  exportVersionCycloneDxUrl: (id: string, versionId: string) =>
    `${apps}/${id}/versions/${versionId}/export/cyclonedx`,

  createAsset: (body: { type: string; schemaVersion?: string; payload: Record<string, unknown>; owner?: string }) =>
    request<AssetView>(assets, { method: 'POST', body: JSON.stringify(body) }),

  relationshipsForType: (type: string) =>
    request<AssetRelationshipSpec[]>(`${schema}/asset-types/${encodeURIComponent(type)}/relationships`),

  listAssetTypes: () => request<AssetTypeSummary[]>(`${schema}/asset-types`),

  getAssetType: (type: string) =>
    request<AssetTypeDetail>(`${schema}/asset-types/${encodeURIComponent(type)}`),

  listSchemaCatalog: () => request<SchemaCatalogEntry[]>(`${schema}/schema-catalog`),

  getSchemaUsedIn: (type: string) =>
    request<SchemaUsedInRef[]>(`${schema}/schema-catalog/${encodeURIComponent(type)}/used-in`),

  getSchema: (type: string, version: string) =>
    request<BoMSchema>(
      `${schema}/schemas/${encodeURIComponent(type)}/${encodeURIComponent(version)}`,
    ),

  getAllowedEdges: (type: string) =>
    request<TypeAllowedEdges>(
      `${schema}/schema-catalog/${encodeURIComponent(type)}/allowed-edges`,
    ),

  searchAssets: (body: {
    type?: string
    schemaVersion?: string
    filters?: Record<string, string>
    objExpr?: string
  }) => request<AssetView[]>(`${assets}/search`, { method: 'POST', body: JSON.stringify(body) }),

  searchAssetsPage: async (
    body: {
      type?: string
      schemaVersion?: string
      filters?: Record<string, string>
      objExpr?: string
    },
    page = 1,
    size = 20,
  ) => {
    try {
      return await request<AssetSearchPage>(`${assets}/search/page?page=${page}&size=${size}`, {
        method: 'POST',
        body: JSON.stringify(body),
      })
    } catch {
      const all = await request<AssetView[]>(`${assets}/search`, {
        method: 'POST',
        body: JSON.stringify(body),
      })
      const p = Math.max(1, page)
      const from = (p - 1) * size
      return {
        items: all.slice(from, from + size),
        total: all.length,
        page: p,
        size,
      }
    }
  },

  getAsset: (id: string) => request<AssetDetailView>(`${assets}/${id}`),

  updateAsset: (id: string, payload: Record<string, unknown>) =>
    request<AssetView>(`${assets}/${id}`, { method: 'PUT', body: JSON.stringify({ payload }) }),

  setAssetOwner: (id: string, owner: string | null) =>
    request<AssetView>(`${assets}/${id}/owner`, {
      method: 'PUT',
      body: JSON.stringify({ owner }),
    }),

  findDuplicates: (type: string) =>
    request<AssetDuplicateGroup[]>(`${assets}/duplicates?type=${encodeURIComponent(type)}`),

  getAssetTypeStatistics: (type: string) =>
    request<AssetTypeStatistics>(`${assets}/statistics?type=${encodeURIComponent(type)}`),

  listPortfolios: () => request<PortfolioSummary[]>(portfolios),

  createPortfolio: (body: {
    name: string
    description?: string
    uniqueness?: string
    origin?: string
    source?: string
  }) => request<PortfolioSummary>(portfolios, { method: 'POST', body: JSON.stringify(body) }),

  updatePortfolio: (
    id: string,
    body: { name?: string; description?: string | null; uniqueness?: string },
  ) => request<PortfolioSummary>(`${portfolios}/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),

  getPortfolio: (id: string) => request<PortfolioTreeView>(`${portfolios}/${id}`),

  addSubjectArea: (
    id: string,
    body: { name: string; description?: string; parentId?: string | null },
  ) =>
    request<SubjectAreaView>(`${portfolios}/${id}/subject-areas`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  updateSubjectArea: (id: string, nodeId: string, body: { name?: string; description?: string | null }) =>
    request<SubjectAreaView>(`${portfolios}/${id}/subject-areas/${nodeId}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }),

  deleteSubjectArea: (id: string, nodeId: string) =>
    request<void>(`${portfolios}/${id}/subject-areas/${nodeId}`, { method: 'DELETE' }),

  placeApplication: (
    id: string,
    body: { applicationId: string; subjectAreaId?: string | null; versionId?: string | null },
  ) =>
    request<PortfolioTreeView>(`${portfolios}/${id}/placements`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  removePlacement: (id: string, placementId: string) =>
    request<PortfolioTreeView>(`${portfolios}/${id}/placements/${placementId}`, { method: 'DELETE' }),

  movePlacements: (id: string, placementIds: string[], subjectAreaId: string | null) =>
    request<PortfolioTreeView>(`${portfolios}/${id}/placements/move`, {
      method: 'POST',
      body: JSON.stringify({ placementIds, subjectAreaId }),
    }),

  deletePlacements: (id: string, placementIds: string[]) =>
    request<PortfolioTreeView>(`${portfolios}/${id}/placements/delete`, {
      method: 'POST',
      body: JSON.stringify({ placementIds }),
    }),

  portfolioLevelApps: (
    id: string,
    level = 'root',
    includeSubcategories = true,
    page = 1,
    size = 50,
    q = '',
  ) => {
    const query = new URLSearchParams({
      level,
      includeSubcategories: String(includeSubcategories),
      page: String(page),
      size: String(size),
    })
    if (q.trim()) query.set('q', q.trim())
    return request<PortfolioLevelApps>(`${portfolios}/${id}/applications?${query}`)
  },

  portfolioAssets: (
    id: string,
    level = 'root',
    includeSubcategories = true,
    page = 1,
    size = 20,
  ) => {
    const q = new URLSearchParams({
      level,
      includeSubcategories: String(includeSubcategories),
      page: String(page),
      size: String(size),
    })
    return request<CategoryAssetPage>(`${portfolios}/${id}/assets?${q}`)
  },

  runMiReport: (
    id: string,
    body: {
      level: string
      includeSubcategories?: boolean
      report: string
      page?: number
      size?: number
    },
  ) =>
    request<MiReportTable>(`${portfolios}/${id}/reports`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  miReportCsvUrl: (id: string, report: string, level: string, includeSubcategories: boolean) => {
    const q = new URLSearchParams({
      level,
      includeSubcategories: String(includeSubcategories),
    })
    return `${portfolios}/${id}/reports/${encodeURIComponent(report)}.csv?${q}`
  },
}
