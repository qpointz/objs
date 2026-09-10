export type Category = {
  id: string
  name: string
  key: string
}

export type CategoryWrite = {
  name: string
  key: string
}

export type Policy = {
  id: string
  /** Logical identity (G-P36seed). */
  key: string
  /** Display label. */
  name: string
  /** Timestamp serial (pin / latest key). */
  serial: number
  engineKind: string
  body: string
  contentType?: string | null
  applicabilityKind?: string | null
  applicabilityBody?: string | null
  categoryId: string
  tags: string[]
  annotations?: Record<string, string>
  /** User-managed major.minor string (e.g. `0.1`). */
  version: string
  description?: string
}

export type PolicyWrite = {
  key: string
  name?: string
  engineKind: string
  body: string
  categoryId: string
  tags: string[]
  contentType?: string | null
  applicabilityKind?: string | null
  applicabilityBody?: string | null
  annotations?: Record<string, string>
  version?: string
  description?: string
}

export type PolicyListQuery = {
  categoryId?: string | null
  tags?: string[]
  key?: string | null
  annotations?: Record<string, string>
}

export type PolicyCapabilities = {
  engines: string[]
  operations: string[]
}

export type PolicyCheckIssue = {
  message: string
  line?: number | null
  column?: number | null
}

export type PolicyCheckResult = {
  ok: boolean
  issues?: PolicyCheckIssue[]
  messages: string[]
}

export type Finding = {
  message: string
  severity?: string | null
  code?: string | null
  entities?: string[]
  edges?: string[]
  extras?: Record<string, unknown> | null
}

export function findingRuleName(f: Finding): string | undefined {
  const raw = f.extras?.rule
  return typeof raw === 'string' && raw.trim() ? raw : undefined
}

export type PolicyOutcome = {
  policyName: string
  policySerial: number
  engineKind: string
  status: string
  notApplicableReason?: string | null
  findings?: Finding[]
  message?: string | null
}

export type EvaluationResult = {
  outcomes: PolicyOutcome[]
  overall?: string | null
}

export type SuiteMatcherMode = 'INCLUDE' | 'EXCLUDE'
export type SuiteFolderParticipation = 'ENABLED' | 'DISABLED' | 'IGNORED'

export type StaticPolicyEntry = {
  policyId: string
  version?: string | null
}

export type SuiteMatcher = {
  kind: 'STATIC_LIST' | 'METADATA' | string
  mode?: SuiteMatcherMode | string
  skipMissing?: boolean
  entries?: StaticPolicyEntry[]
  categoryKey?: string | null
  tags?: string[]
  annotations?: Record<string, string>
}

export type SuiteFolder = {
  id?: string | null
  key: string
  name: string
  parentId?: string | null
  sortOrder?: number
  participation?: SuiteFolderParticipation | string
  /** ALL_PASS | ANY_PASS — interpreted by suite rollUpStrategyKind. */
  rollUpMode?: string
  matchers?: SuiteMatcher[]
  tags?: string[]
  annotations?: Record<string, string>
  severityConfig?: string | null
}

export type PolicySuite = {
  id?: string | null
  key: string
  name: string
  /** BUILTIN (C-27); later DROOLS etc. */
  rollUpStrategyKind?: string
  executionStrategyKind?: string
  folders?: SuiteFolder[]
  tags?: string[]
  annotations?: Record<string, string>
}

export type SuitePolicyLeafResult = {
  policyId: string
  policyName: string
  policySerial: number
  policyVersion: string
  status: string
  severity?: string | null
  outcomeIndex: number
}

export type SuiteFolderResult = {
  folderId: string
  key: string
  name: string
  parentFolderId?: string | null
  participation: string
  status: string
  severity?: string | null
  votes: boolean
  tags?: string[]
  annotations?: Record<string, string>
  children?: SuiteFolderResult[]
  leaves?: SuitePolicyLeafResult[]
}

export type SuiteEvaluationMeta = {
  evaluationId: string
  kind: string
  evaluatedAtEpochMs: number
  executionStrategyKind?: string | null
  rollUpStrategyKind?: string | null
  overallStatus?: string | null
  overallSeverity?: string | null
  tags?: string[]
  annotations?: Record<string, string>
  suiteId?: string | null
  suiteName?: string | null
}

export type SuiteEvaluationResult = {
  meta: SuiteEvaluationMeta
  tree?: SuiteFolderResult | null
  outcomes: PolicyOutcome[]
}

export type PersistContentAxes = {
  results: boolean
  executionContext: boolean
  input: boolean
}

export type PersistSuiteEvaluationRequest = {
  result: SuiteEvaluationResult
  name?: string | null
  description?: string | null
  tags?: string[]
  annotations?: Record<string, string>
  axes?: PersistContentAxes
  presetName?: string | null
  graphId?: string | null
  graphVersion?: number | null
  matcher?: unknown
}

export type PersistEvaluationResponse = {
  evaluationId: string
}

export type SuiteSelectionResult = {
  policies: Policy[]
  placementByPolicyId?: Record<string, string>
  missing?: StaticPolicyEntry[]
}

export const SEVERITY_RANK: Record<string, number> = {
  CRITICAL: 50,
  HIGH: 40,
  MEDIUM: 30,
  LOW: 20,
  INFO: 10,
}

export function severityRank(raw: string | null | undefined): number {
  if (!raw) return 0
  return SEVERITY_RANK[raw.trim().toUpperCase()] ?? 0
}

export function maxSeverity(a: string | undefined, b: string | undefined): string | undefined {
  if (!a) return b
  if (!b) return a
  return severityRank(a) >= severityRank(b) ? a : b
}

/** Display: major.minor · serial */
export function formatPolicyVersion(p: Pick<Policy, 'version' | 'serial'>): string {
  return `${p.version} · ${p.serial}`
}
