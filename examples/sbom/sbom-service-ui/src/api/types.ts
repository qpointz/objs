export type ApplicationSummary = {
  id: string
  name: string
  description: string | null
  tags?: string[]
}

export type ApplicationPortalStats = {
  applicationId: string
  versionCount: number
  bomCount: number
  latestVersion: ApplicationVersionSummary | null
  latestMultiBom: boolean
}

export type AssetView = {
  id: string
  type: string
  schemaVersion: string
  label: string
  payload: Record<string, unknown>
  owner: string | null
}

export type AssetSearchPage = {
  items: AssetView[]
  total: number
  page: number
  size: number
}

export type RelationView = {
  id: string
  role: string
  label: string
  fromAssetId: string
  toAssetId: string
}

export type ApplicationVersionSummary = {
  id: string
  applicationId: string
  status: 'DRAFT' | 'RELEASED' | string
  version: string | null
  label: string | null
  capturedAt: string
  promotedAt: string | null
  tags?: string[]
  basedOnVersionId?: string | null
  basedOnFingerprintId?: string | null
  bomCount?: number
}

export type VersionBomView = {
  version: ApplicationVersionSummary
  applicationName: string
  assets: AssetView[]
  relations: RelationView[]
  combinedTags?: string[]
}

export type BomSummary = {
  id: string
  versionId: string
  name: string
  description: string | null
  tags: string[]
  sortOrder: number
}

export type CombinedBomView = {
  version: ApplicationVersionSummary
  applicationName: string
  assets: AssetView[]
  relations: RelationView[]
  combinedTags: string[]
  selectedBomIds: string[]
}

export type ApplicationFingerprintSummary = {
  id: string
  versionId: string
  createdAt: string
  note: string | null
  name?: string
  category?: string
  contentSha256: string
}

export type AssetRelationshipSpec = {
  role: string
  label: string
  targetType: string
  section: string
  cardinality: string
  direction?: 'OUT' | 'IN' | string
}

export type InferredAppDependency = {
  applicationId: string
  applicationName: string
  sharedAssetIds: string[]
}

export type AssetTypeSummary = {
  type: string
  version: string
  title: string
  description: string
}

export type AssetTypeStatistics = {
  type: string
  objectCount: number
}

export type AssetFieldHint = {
  path: string
  title: string
  fieldType: string
  searchable: boolean
  identifier: boolean
}

export type AssetTypeDetail = {
  type: string
  version: string
  title: string
  description: string
  searchableFields: AssetFieldHint[]
  identifierFields: AssetFieldHint[]
  firstLevelScalarFields: AssetFieldHint[]
}

export type SchemaUsedInRef = {
  id: string
  name: string
}

export type SchemaCatalogEntry = {
  type: string
  latestVersion: string
  versions: string[]
  title: string
  description: string
  usage: string
  usedIn: SchemaUsedInRef[]
}

export type BoMSchemaType =
  | 'OBJECT'
  | 'ARRAY'
  | 'STRING'
  | 'NUMBER'
  | 'INTEGER'
  | 'BOOLEAN'
  | 'ENUM'

export type BoMEnumValue = {
  value: string
  description: string
  /** Short UI label; omit to show [value] in editors. */
  caption?: string | null
}

export function enumCaption(entry: { value: string; caption?: string | null }): string {
  const caption = entry.caption?.trim()
  return caption || entry.value
}

export type BoMSchemaField = {
  name: string
  schema: BoMSchemaNode
  required?: boolean
  identifier?: boolean
  searchable?: boolean
}

export type BoMSchemaNode = {
  type: BoMSchemaType
  title: string
  description: string
  fields?: BoMSchemaField[]
  items?: BoMSchemaNode
  values?: BoMEnumValue[]
  format?: string | null
  default?: unknown
}

export type BoMSchema = {
  type: string
  version: string
  contentSchema: BoMSchemaNode
  usage?: string
  tags?: string[]
  attributes?: Record<string, string>
}

export type AllowedEdgeRuleView = {
  sourceType: string
  role: string
  targetType: string
  propertiesPolicy: string
  emptyPropertiesAllowed?: boolean
  propertiesSchemaType?: string | null
  propertiesSchemaVersion?: string | null
  cardinality?: string
  description?: string | null
  sourceVerb?: string | null
  targetVerb?: string | null
  tags?: string[]
  attributes?: Record<string, string>
}

export type TypeAllowedEdges = {
  incoming: AllowedEdgeRuleView[]
  outgoing: AllowedEdgeRuleView[]
}

export type AssetUsageRelation = {
  role: string
  label: string
  direction: string
  otherAssetId: string
}

export type AssetUsageEntry = {
  applicationId: string
  applicationName: string
  context: string
  versionId?: string | null
  versionLabel?: string | null
  relations: AssetUsageRelation[]
}

export type AssetDetailView = {
  asset: AssetView
  usage: AssetUsageEntry[]
}

export type AssetDuplicateGroup = {
  type: string
  schemaVersion: string
  identity: Record<string, unknown>
  assets: AssetView[]
}

export type PortfolioUniqueness = 'UNIQUE_APP' | 'UNIQUE_APP_VERSION' | 'NOT_UNIQUE'
export type PortfolioOrigin = 'MANUAL' | 'AUTOMATED'

export type PortfolioSummary = {
  id: string
  name: string
  description: string | null
  uniqueness?: PortfolioUniqueness
  origin?: PortfolioOrigin
  source?: string | null
}

export type PortfolioAppRef = {
  applicationId: string
  applicationName: string
  applicationDescription?: string | null
  placementId?: string | null
  nodeId?: string | null
  versionId?: string | null
}

export type SubjectAreaView = {
  id: string
  name: string
  description?: string | null
  parentId: string | null
  leafCount?: number
  applications: PortfolioAppRef[]
  children: SubjectAreaView[]
}

export type PortfolioTreeView = {
  portfolio: PortfolioSummary
  subjectAreas: SubjectAreaView[]
  rootApplications: PortfolioAppRef[]
  rootLeafCount?: number
}

export type PortfolioLevelApps = {
  portfolioId: string
  level: string
  includeSubcategories?: boolean
  applications: PortfolioAppRef[]
  total?: number
}

export type CategoryAssetRow = {
  assetId: string
  type: string
  label: string
  identity: Record<string, unknown>
  usedInApplicationIds: string[]
  usedInApplicationNames: string[]
}

export type CategoryAssetPage = {
  items: CategoryAssetRow[]
  total: number
  page: number
  size: number
  notes?: string[]
}

export type MiReportTable = {
  report: string
  title: string
  columns: string[]
  rows: Record<string, string>[]
  total: number
  page: number
  size: number
  notes: string[]
}

export type MiReportResult = MiReportTable
