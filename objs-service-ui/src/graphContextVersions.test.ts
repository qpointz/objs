import { describe, expect, it } from 'vitest'
import {
  EXPLORER_NODE_CAP,
  filterGraphVersionsByTime,
  graphVersionPageCount,
  pageGraphVersions,
  parseGraphVersionTime,
} from './graphContextVersions'
import { formatInstanceVersionLabel } from './InstanceVersionInspect'
import type { BoMGraphVersionSummary } from './types'

function row(version: number, createdAt: string): BoMGraphVersionSummary {
  return { graphId: 'g', version, createdAt, annotations: {} }
}

describe('graphContextVersions', () => {
  it('pages newest-first lists', () => {
    const rows = Array.from({ length: 25 }, (_, i) => row(25 - i, `2026-01-${String(25 - i).padStart(2, '0')}T00:00:00Z`))
    expect(pageGraphVersions(rows, 0)).toHaveLength(10)
    expect(pageGraphVersions(rows, 0)[0]?.version).toBe(25)
    expect(pageGraphVersions(rows, 2)).toHaveLength(5)
    expect(graphVersionPageCount(25)).toBe(3)
  })

  it('filters by from/to', () => {
    const rows = [
      row(3, '2026-03-01T12:00:00Z'),
      row(2, '2026-02-01T12:00:00Z'),
      row(1, '2026-01-01T12:00:00Z'),
    ]
    const filtered = filterGraphVersionsByTime(rows, {
      from: new Date('2026-01-15T00:00:00Z'),
      to: new Date('2026-02-15T00:00:00Z'),
    })
    expect(filtered.map((r) => r.version)).toEqual([2])
  })

  it('parses createdAt and epoch-ish versions', () => {
    expect(parseGraphVersionTime('2026-01-02T03:04:05Z', 1)?.toISOString()).toContain('2026-01-02')
    const epoch = 1_700_000_000_000
    expect(parseGraphVersionTime(undefined, epoch)?.getTime()).toBe(epoch)
  })

  it('exports explorer node cap', () => {
    expect(EXPLORER_NODE_CAP).toBe(300)
  })
})

describe('formatInstanceVersionLabel', () => {
  it('shows LATEST when nullish', () => {
    expect(formatInstanceVersionLabel(null)).toBe('LATEST')
    expect(formatInstanceVersionLabel(undefined)).toBe('LATEST')
    expect(formatInstanceVersionLabel(99)).toBe('99')
  })
})
