/** Severity pill for assessment findings (C-34 closed set). */

const FINDING_PILL: Record<string, { bg: string; label: string }> = {
  CRITICAL: { bg: '#c92a2a', label: 'Critical' },
  HIGH: { bg: '#fa5252', label: 'High' },
  MEDIUM: { bg: '#fd7e14', label: 'Medium' },
  LOW: { bg: '#fab005', label: 'Low' },
  INFO: { bg: '#228be6', label: 'Info' },
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

/** Outcome status cell color (Axis A) — not finding severity. */
export function assessmentCellColor(status: string | undefined | null): string {
  const key = (status ?? '').trim().toUpperCase()
  if (key === 'PASS') return 'teal'
  if (key === 'FAIL' || key === 'EXEC_ERROR' || key === 'ERROR') return 'red'
  if (key === 'NOT_APPLICABLE') return 'gray'
  return 'gray'
}

const SEVERITY_RANK: Record<string, number> = {
  CRITICAL: 50,
  HIGH: 40,
  MEDIUM: 30,
  LOW: 20,
  INFO: 10,
}

export function severityRank(raw: string | null | undefined): number {
  if (!raw) return 0
  return SEVERITY_RANK[raw.trim().toUpperCase()] ?? 0
}

/** Worst finding severity (CRITICAL > … > INFO). */
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
