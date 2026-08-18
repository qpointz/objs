import type { BoMAllowedEdgeRule } from './types'

export type AllowedEdgeRef = {
  sourceType: string
  role: string
  targetType: string
  /** UI-only: which list a self-edge was clicked in. */
  direction?: 'incoming' | 'outbound'
}

export function allowedEdgeKey(ref: AllowedEdgeRef): string {
  return `${ref.sourceType}|${ref.role}|${ref.targetType}`
}

export function directedEdgeKey(
  ref: AllowedEdgeRef & { direction: 'incoming' | 'outbound' },
): string {
  return `${ref.direction}|${allowedEdgeKey(ref)}`
}

export function uniqueEdgeRules(edges: {
  incoming: BoMAllowedEdgeRule[]
  outgoing: BoMAllowedEdgeRule[]
}): BoMAllowedEdgeRule[] {
  const map = new Map<string, BoMAllowedEdgeRule>()
  for (const rule of [...edges.incoming, ...edges.outgoing]) {
    map.set(allowedEdgeKey(rule), rule)
  }
  return [...map.values()]
}

export function edgesForType(
  type: string,
  rules: BoMAllowedEdgeRule[],
): { incoming: BoMAllowedEdgeRule[]; outgoing: BoMAllowedEdgeRule[] } {
  const incoming: BoMAllowedEdgeRule[] = []
  const outgoing: BoMAllowedEdgeRule[] = []
  for (const rule of rules) {
    if (rule.targetType === '*' || rule.targetType === type) incoming.push(rule)
    if (rule.sourceType === '*' || rule.sourceType === type) outgoing.push(rule)
  }
  return { incoming, outgoing }
}

export function cloneEdgeRules(rules: BoMAllowedEdgeRule[]): BoMAllowedEdgeRule[] {
  return JSON.parse(JSON.stringify(rules)) as BoMAllowedEdgeRule[]
}

function edgePayload(rule: BoMAllowedEdgeRule): string {
  return JSON.stringify({
    sourceType: rule.sourceType,
    role: rule.role,
    targetType: rule.targetType,
    cardinality: rule.cardinality ?? 'UNSPECIFIED',
    propertiesPolicy: rule.propertiesPolicy,
    emptyPropertiesAllowed: rule.emptyPropertiesAllowed,
    propertiesSchemaType: rule.propertiesSchemaType ?? null,
    propertiesSchemaVersion: rule.propertiesSchemaVersion ?? null,
    description: rule.description ?? null,
    sourceVerb: rule.sourceVerb ?? null,
    targetVerb: rule.targetVerb ?? null,
    tags: rule.tags ?? [],
    attributes: rule.attributes ?? {},
  })
}

export function edgeRulesEqual(a: BoMAllowedEdgeRule, b: BoMAllowedEdgeRule): boolean {
  return edgePayload(a) === edgePayload(b)
}
