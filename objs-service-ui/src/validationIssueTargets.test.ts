import { describe, expect, it } from 'vitest'
import {
  edgeIdsFromValidationIssues,
  entityIdsFromValidationIssues,
  validationTargetFromIssue,
} from './validationIssueTargets'

describe('validationIssueTargets', () => {
  const entities = [{ id: 'e0' }, { id: 'e1' }]
  const edges = [{ id: 'edge0' }, { id: 'edge1' }]

  it('shouldResolveEntityFromEntitiesIndexPath', () => {
    expect(
      validationTargetFromIssue(
        { code: 'SCHEMA_VIOLATION', message: 'bad', path: 'entities[1].payload.name' },
        entities,
        edges,
      ),
    ).toEqual({ kind: 'entity', id: 'e1', index: 1 })
  })

  it('shouldResolveEntityFromUpsertPath', () => {
    expect(
      validationTargetFromIssue(
        { code: 'SCHEMA_NOT_FOUND', message: 'missing', path: 'upsert.entities[0]' },
        entities,
        edges,
      ),
    ).toEqual({ kind: 'entity', id: 'e0', index: 0 })
  })

  it('shouldResolveEdgeFromEdgesIndexPath', () => {
    expect(
      validationTargetFromIssue(
        { code: 'EDGE_NOT_ALLOWED', message: 'no', path: 'edges[0].role' },
        entities,
        edges,
      ),
    ).toEqual({ kind: 'edge', id: 'edge0', index: 0 })
  })

  it('shouldCollectDistinctEntityIds', () => {
    const ids = entityIdsFromValidationIssues(
      [
        { code: 'A', message: 'a', path: 'entities[0].payload' },
        { code: 'B', message: 'b', path: 'entities[0].payload.x' },
        { code: 'C', message: 'c', path: 'entities[1]' },
        { code: 'D', message: 'd', path: 'edges[0]' },
      ],
      entities,
      edges,
    )
    expect([...ids].sort()).toEqual(['e0', 'e1'])
  })

  it('shouldCollectEdgeIds', () => {
    const ids = edgeIdsFromValidationIssues(
      [{ code: 'A', message: 'a', path: 'edges[1].properties' }],
      entities,
      edges,
    )
    expect([...ids]).toEqual(['edge1'])
  })
})
