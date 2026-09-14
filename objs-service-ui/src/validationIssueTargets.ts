import type { BoMEdge, BoMEntity, BoMValidationIssue } from './types'

const ENTITY_PATH = /(?:^|\.)(?:entities\.set|entities)\[(\d+)\]/
const EDGE_PATH = /(?:^|\.)(?:edges\.set|edges)\[(\d+)\]/

export type ValidationTarget =
  | { kind: 'entity'; id: string; index: number }
  | { kind: 'edge'; id: string; index: number }

/**
 * Resolve a validation issue to a set entity/edge id.
 * Prefers structured [BoMValidationIssue.subject]; falls back to path parse for legacy/seed issues.
 */
export function validationTargetFromIssue(
  issue: BoMValidationIssue,
  entities: Pick<BoMEntity, 'id'>[],
  edges: Pick<BoMEdge, 'id'>[],
): ValidationTarget | null {
  const subject = issue.subject
  if (subject?.kind === 'ENTITY') {
    const index = subject.index ?? -1
    const id = subject.id ?? (index >= 0 ? entities[index]?.id : undefined)
    if (id != null && id !== '') {
      return { kind: 'entity', id: String(id), index: index >= 0 ? index : 0 }
    }
  }
  if (subject?.kind === 'EDGE') {
    const index = subject.index ?? -1
    const id = subject.id ?? (index >= 0 ? edges[index]?.id : undefined)
    if (id != null && id !== '') {
      return { kind: 'edge', id: String(id), index: index >= 0 ? index : 0 }
    }
  }

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
