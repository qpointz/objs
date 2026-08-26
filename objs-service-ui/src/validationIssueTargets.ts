import type { BoMEdge, BoMEntity, BoMValidationIssue } from './types'

const ENTITY_PATH = /(?:^|\.)(?:entities\.set|entities)\[(\d+)\]/
const EDGE_PATH = /(?:^|\.)(?:edges\.set|edges)\[(\d+)\]/

export type ValidationTarget =
  | { kind: 'entity'; id: string; index: number }
  | { kind: 'edge'; id: string; index: number }

/** Map a validation issue path to a set entity/edge id using mutation order. */
export function validationTargetFromIssue(
  issue: BoMValidationIssue,
  entities: Pick<BoMEntity, 'id'>[],
  edges: Pick<BoMEdge, 'id'>[],
): ValidationTarget | null {
  const path = issue.path ?? ''
  const entityMatch = path.match(ENTITY_PATH)
  if (entityMatch) {
    const index = Number(entityMatch[1])
    const id = entities[index]?.id
    if (id) return { kind: 'entity', id, index }
  }
  const edgeMatch = path.match(EDGE_PATH)
  if (edgeMatch) {
    const index = Number(edgeMatch[1])
    const id = edges[index]?.id
    if (id) return { kind: 'edge', id, index }
  }
  return null
}

export function entityIdsFromValidationIssues(
  issues: BoMValidationIssue[],
  entities: Pick<BoMEntity, 'id'>[],
  edges: Pick<BoMEdge, 'id'>[] = [],
): Set<string> {
  const ids = new Set<string>()
  for (const issue of issues) {
    const target = validationTargetFromIssue(issue, entities, edges)
    if (target?.kind === 'entity') ids.add(target.id)
  }
  return ids
}

export function edgeIdsFromValidationIssues(
  issues: BoMValidationIssue[],
  entities: Pick<BoMEntity, 'id'>[],
  edges: Pick<BoMEdge, 'id'>[],
): Set<string> {
  const ids = new Set<string>()
  for (const issue of issues) {
    const target = validationTargetFromIssue(issue, entities, edges)
    if (target?.kind === 'edge') ids.add(target.id)
  }
  return ids
}
