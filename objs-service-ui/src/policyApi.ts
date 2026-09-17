import type {
  Category,
  CategoryWrite,
  EvaluationArchiveDocument,
  EvaluationArchiveListResponse,
  EvaluationResult,
  PersistEvaluationResponse,
  PersistSuiteEvaluationRequest,
  Policy,
  PolicyCapabilities,
  PolicyCheckResult,
  PolicyListQuery,
  PolicySuite,
  PolicyWrite,
  SuiteEvaluationResult,
  SuiteSelectionResult,
} from './policyTypes'

async function parseResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(policyErrorMessage(text, res.status, res.statusText))
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

/** Prefer a short message; never surface Spring Boot `trace` / stack dumps. */
function policyErrorMessage(body: string, status: number, statusText: string): string {
  const trimmed = body.trim()
  if (!trimmed) return `${status} ${statusText}`
  if (trimmed.startsWith('{')) {
    try {
      const json = JSON.parse(trimmed) as {
        message?: unknown
        error?: unknown
        detail?: unknown
        title?: unknown
      }
      const msg = [json.message, json.detail, json.error, json.title]
        .find((v) => typeof v === 'string' && v.trim().length > 0)
      if (typeof msg === 'string') return stripStackish(msg)
    } catch {
      /* fall through */
    }
  }
  return stripStackish(trimmed)
}

function stripStackish(raw: string): string {
  let s = raw.trim()
  const cutAt = ['Java source of ', '\n\tat ', '\r\n\tat ']
  for (const m of cutAt) {
    const i = s.indexOf(m)
    if (i >= 0) s = s.slice(0, i).trimEnd()
  }
  if (s.length > 480) s = `${s.slice(0, 479).trimEnd()}…`
  return s || 'Request failed'
}

/** Soft-fail when `:objs-policy-service` is absent. */
export async function fetchPolicyCapabilities(): Promise<PolicyCapabilities | null> {
  try {
    const res = await fetch('/api/v1/objs/policy/capabilities')
    if (res.status === 404) return null
    return await parseResponse<PolicyCapabilities>(res)
  } catch {
    return null
  }
}

export async function listCategories(): Promise<Category[]> {
  const res = await fetch('/api/v1/objs/policy/categories')
  return parseResponse<Category[]>(res)
}

export async function createCategory(write: CategoryWrite): Promise<Category> {
  const res = await fetch('/api/v1/objs/policy/categories', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(write),
  })
  return parseResponse<Category>(res)
}

export async function updateCategory(id: string, write: CategoryWrite): Promise<Category> {
  const res = await fetch(`/api/v1/objs/policy/categories/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(write),
  })
  return parseResponse<Category>(res)
}

export async function deleteCategory(id: string): Promise<void> {
  const res = await fetch(`/api/v1/objs/policy/categories/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
  await parseResponse<void>(res)
}

export async function listPolicies(query: PolicyListQuery = {}): Promise<Policy[]> {
  const params = new URLSearchParams()
  if (query.categoryId) params.set('categoryId', query.categoryId)
  for (const t of query.tags ?? []) {
    if (t.trim()) params.append('tag', t.trim())
  }
  if (query.key?.trim()) params.set('key', query.key.trim())
  for (const [k, v] of Object.entries(query.annotations ?? {})) {
    params.append('annotation', `${k}=${v}`)
  }
  const qs = params.toString()
  const res = await fetch(`/api/v1/objs/policy/policies${qs ? `?${qs}` : ''}`)
  return parseResponse<Policy[]>(res)
}

export async function createPolicy(write: PolicyWrite): Promise<Policy> {
  const res = await fetch('/api/v1/objs/policy/policies', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(write),
  })
  return parseResponse<Policy>(res)
}

export async function updatePolicy(id: string, write: PolicyWrite): Promise<Policy> {
  const res = await fetch(`/api/v1/objs/policy/policies/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(write),
  })
  return parseResponse<Policy>(res)
}

export async function deletePolicy(id: string): Promise<void> {
  const res = await fetch(`/api/v1/objs/policy/policies/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
  await parseResponse<void>(res)
}

export async function checkPolicy(body: string, engineKind = 'DROOLS'): Promise<PolicyCheckResult> {
  const res = await fetch('/api/v1/objs/policy/check', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ body, engineKind }),
  })
  return parseResponse<PolicyCheckResult>(res)
}

export async function evaluatePolicy(request: {
  graphId?: string | null
  graphVersion?: number | null
  policyId?: string | null
  body?: string | null
  engineKind?: string
  policyName?: string
  matcher?: unknown
}): Promise<EvaluationResult> {
  const res = await fetch('/api/v1/objs/policy/evaluate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      matcher: request.matcher ?? { all: true },
      graphId: request.graphId ?? undefined,
      graphVersion: request.graphVersion ?? undefined,
      policyId: request.policyId ?? undefined,
      body: request.body ?? undefined,
      engineKind: request.engineKind ?? 'DROOLS',
      policyName: request.policyName ?? undefined,
    }),
  })
  return parseResponse<EvaluationResult>(res)
}

export async function listSuites(): Promise<PolicySuite[]> {
  const res = await fetch('/api/v1/objs/policy/suites')
  return parseResponse<PolicySuite[]>(res)
}

export async function createSuite(suite: PolicySuite): Promise<PolicySuite> {
  const res = await fetch('/api/v1/objs/policy/suites', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(suite),
  })
  return parseResponse<PolicySuite>(res)
}

export async function updateSuite(id: string, suite: PolicySuite): Promise<PolicySuite> {
  const res = await fetch(`/api/v1/objs/policy/suites/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(suite),
  })
  return parseResponse<PolicySuite>(res)
}

export async function deleteSuite(id: string): Promise<void> {
  const res = await fetch(`/api/v1/objs/policy/suites/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
  await parseResponse<void>(res)
}

export async function fetchSuiteSelection(request: {
  suiteId: string
  scope?: string
  folderId?: string | null
}): Promise<SuiteSelectionResult> {
  const res = await fetch('/api/v1/objs/policy/suites/selection', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      suiteId: request.suiteId,
      scope: request.scope ?? 'FULL',
      folderId: request.folderId ?? undefined,
    }),
  })
  return parseResponse<SuiteSelectionResult>(res)
}

export async function evaluateSuite(request: {
  suiteId: string
  graphId?: string | null
  graphVersion?: number | null
  matcher?: unknown
  scope?: string
  folderId?: string | null
}): Promise<SuiteEvaluationResult> {
  const res = await fetch('/api/v1/objs/policy/suites/evaluate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      suiteId: request.suiteId,
      matcher: request.matcher ?? { all: true },
      graphId: request.graphId ?? undefined,
      graphVersion: request.graphVersion ?? undefined,
      scope: request.scope ?? 'FULL',
      folderId: request.folderId ?? undefined,
    }),
  })
  return parseResponse<SuiteEvaluationResult>(res)
}

export async function persistSuiteEvaluation(
  request: PersistSuiteEvaluationRequest,
): Promise<PersistEvaluationResponse> {
  const res = await fetch('/api/v1/objs/policy/evaluations/suite', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return parseResponse<PersistEvaluationResponse>(res)
}

export async function listEvaluations(query: {
  limit?: number
  offset?: number
  kind?: string | null
  tag?: string | null
} = {}): Promise<EvaluationArchiveListResponse> {
  const params = new URLSearchParams()
  if (query.limit != null) params.set('limit', String(query.limit))
  if (query.offset != null) params.set('offset', String(query.offset))
  if (query.kind?.trim()) params.set('kind', query.kind.trim())
  if (query.tag?.trim()) params.set('tag', query.tag.trim())
  const qs = params.toString()
  const res = await fetch(`/api/v1/objs/policy/evaluations${qs ? `?${qs}` : ''}`)
  return parseResponse(res)
}

export async function loadEvaluation(id: string): Promise<EvaluationArchiveDocument> {
  const res = await fetch(`/api/v1/objs/policy/evaluations/${encodeURIComponent(id)}`)
  return parseResponse(res)
}

export async function deleteEvaluation(id: string): Promise<void> {
  const res = await fetch(`/api/v1/objs/policy/evaluations/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
  await parseResponse<void>(res)
}

/** Download full policy catalog as REPLACE seed YAML (WI-005). */
export async function exportPolicyCatalogSeeds(): Promise<Blob> {
  const res = await fetch('/api/v1/objs/policy/export?format=seeds')
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `${res.status} ${res.statusText}`)
  }
  return res.blob()
}
