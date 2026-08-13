import { describe, expect, it } from 'vitest'
import { formatQueryDuration, formatQueryExecStats } from './queryExecStats'

describe('queryExecStats', () => {
  it('formats sub-second durations in ms', () => {
    expect(formatQueryDuration(42.2)).toBe('42ms')
  })

  it('formats longer durations in seconds', () => {
    expect(formatQueryDuration(1530)).toBe('1.53s')
    expect(formatQueryDuration(12500)).toBe('12.5s')
  })

  it('includes nodes and edges', () => {
    expect(formatQueryExecStats({ durationMs: 120, nodes: 40, edges: 12 })).toBe(
      '120ms · 40 nodes · 12 edges',
    )
  })
})
