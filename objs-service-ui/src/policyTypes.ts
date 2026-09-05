export type Category = {
  id: string
  displayName: string
  slug: string
}

export type CategoryWrite = {
  displayName: string
  slug: string
}

export type Policy = {
  id: string
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
  name: string
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
  name?: string | null
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

export const SEVERITY_RANK: Record<string, number> = {
  ERROR: 40,
  WARN: 30,
  WARNING: 30,
  INFO: 20,
  OK: 10,
}

export function severityRank(raw: string | null | undefined): number {
  if (!raw) return 0
  return SEVERITY_RANK[raw.trim().toUpperCase()] ?? 5
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
