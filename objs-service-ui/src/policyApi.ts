import type {
  Category,
  CategoryWrite,
  EvaluationResult,
  Policy,
  PolicyCapabilities,
  PolicyCheckResult,
  PolicyListQuery,
  PolicyWrite,
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
  if (query.name?.trim()) params.set('name', query.name.trim())
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
