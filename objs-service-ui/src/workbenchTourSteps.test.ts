import { describe, expect, it } from 'vitest'
import { WORKBENCH_TOUR_STEPS } from './workbenchTourSteps'

describe('workbench tour steps', () => {
  it('covers each product view in order', () => {
    const ids = WORKBENCH_TOUR_STEPS.map((s) => s.id)
    expect(ids).toEqual([
      'nav',
      'explorer',
      'graph-context',
      'graph-context-open',
      'graph-context-version',
      'explorer-view-actions',
      'object-inspect',
      'objects',
      'objects-actions',
      'objects-side',
      'query',
      'query-actions',
      'query-options',
      'composer',
      'composer-graph-bar',
      'composer-version',
      'schema',
      'schema-context-bar',
      'schema-context-version',
      'schema-view-actions',
    ])
    expect(WORKBENCH_TOUR_STEPS.every((s) => s.title && s.body && s.selector)).toBe(true)
  })
})
