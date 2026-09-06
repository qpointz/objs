/** Severity pill for assessment findings (mirrors workbench FindingSeverityPill). */

const FINDING_PILL: Record<string, { bg: string; label: string }> = {
  ERROR: { bg: '#fa5252', label: 'ERROR' },
  FAIL: { bg: '#fa5252', label: 'FAIL' },
  WARN: { bg: '#fd7e14', label: 'WARN' },
  WARNING: { bg: '#fd7e14', label: 'WARN' },
  OK: { bg: '#228be6', label: 'INFO' },
  INFO: { bg: '#228be6', label: 'INFO' },
  PASS: { bg: '#12b886', label: 'PASS' },
}

export function FindingSeverityPill({
  severity,
  ml,
}: {
  severity: string | undefined | null
  ml?: number | string
}) {
  if (!severity) return null
  const key = severity.trim().toUpperCase()
  const style = FINDING_PILL[key] ?? { bg: '#868e96', label: key.slice(0, 8) || 'FIND' }
  return (
    <span
      title={severity}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        height: 14,
        marginLeft: ml ?? 0,
        padding: '0 5px',
        borderRadius: 3,
        background: style.bg,
        color: '#fff',
        fontSize: 8,
        fontWeight: 800,
        letterSpacing: '0.04em',
        lineHeight: 1,
        flexShrink: 0,
      }}
    >
      {style.label}
    </span>
  )
}

export function assessmentCellColor(status: string | undefined | null): string {
  const key = (status ?? '').trim().toUpperCase()
  if (key === 'PASS') return 'teal'
  if (key === 'FAIL' || key === 'ERROR') return 'red'
  if (key === 'WARN' || key === 'WARNING') return 'orange'
  return 'gray'
}

const SEVERITY_RANK: Record<string, number> = {
  ERROR: 40,
  FAIL: 40,
  WARN: 30,
  WARNING: 30,
  INFO: 20,
  OK: 10,
  PASS: 10,
}

export function severityRank(raw: string | null | undefined): number {
  if (!raw) return 0
  return SEVERITY_RANK[raw.trim().toUpperCase()] ?? 5
}

/** Worst finding severity (ERROR > WARN > …). */
export function maxFindingSeverity(
  ...values: Array<string | null | undefined>
): string | undefined {
  let best: string | undefined
  let bestRank = 0
  for (const v of values) {
    if (!v) continue
    const r = severityRank(v)
    if (r > bestRank) {
      bestRank = r
      best = v
    }
  }
  return best
}
