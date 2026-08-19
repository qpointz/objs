import { describe, expect, it } from 'vitest'
import { WORKBENCH_TOUR_STEPS } from './workbenchTourSteps'

describe('workbench tour steps', () => {
  it('covers each product view in order', () => {
    const ids = WORKBENCH_TOUR_STEPS.map((s) => s.id)
    expect(ids).toEqual([
      'nav',
      'explorer',
      'explorer-scope',
      'explorer-open',
      'explorer-versions',
      'objects',
      'composer',
      'composer-version',
      'query',
      'schema',
    ])
    expect(WORKBENCH_TOUR_STEPS.every((s) => s.title && s.body && s.selector)).toBe(true)
  })
})
