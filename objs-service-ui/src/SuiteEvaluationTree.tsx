import { useEffect, useMemo, useState } from 'react'
import { Badge, Box, Group, Table, Text, UnstyledButton } from '@mantine/core'
import { IconChevronDown, IconChevronRight } from '@tabler/icons-react'
import type {
  EvaluationResult,
  Finding,
  PolicyOutcome,
  SuiteFolderResult,
  SuitePolicyLeafResult,
} from './policyTypes'
import { formatQueryDuration } from './queryExecStats'
import { queryResultTableProps } from './QueryResultGrid'
import type { GraphSelection } from './types'

const SEVERITY_COL_WIDTH = 72

const COL_W = {
  status: 118,
  severity: 88,
  result: 140,
} as const

export function severityBadgeColor(raw: string | null | undefined): string {
  switch ((raw ?? '').trim().toUpperCase()) {
    case 'CRITICAL':
      return 'red'
    case 'HIGH':
    case 'EXEC_ERROR':
    case 'FAIL':
      return 'red'
    case 'MEDIUM':
      return 'orange'
    case 'LOW':
      return 'yellow'
    case 'PASS':
      return 'green'
    case 'INFO':
      return 'cyan'
    case 'NOT_APPLICABLE':
    case 'N/A':
      return 'gray'
    default:
      return 'gray'
  }
}

/** Fixed-width severity column + left-aligned message (Policy play findings list). */
export function SeverityMessageRow({
  severity,
  depth,
  children,
  onClick,
  active,
}: {
  severity: string | null | undefined
  depth: number
  children: React.ReactNode
  onClick?: () => void
  active?: boolean
}) {
  const label = (severity ?? '—').trim() || '—'
  return (
    <Group
      gap={8}
      wrap="nowrap"
      align="flex-start"
      onClick={onClick}
      style={{
        paddingLeft: depth * 14,
        cursor: onClick ? 'pointer' : undefined,
      }}
    >
      <Box style={{ width: SEVERITY_COL_WIDTH, flexShrink: 0 }}>
        <Badge
          size="xs"
          fullWidth
          color={severityBadgeColor(label === '—' ? null : label)}
          styles={{
            root: { justifyContent: 'center', width: '100%' },
            label: { overflow: 'hidden', textOverflow: 'ellipsis' },
          }}
        >
          {label}
        </Badge>
      </Box>
      <Box
        style={{
          flex: 1,
          minWidth: 0,
          textAlign: 'left',
          fontWeight: active ? 600 : undefined,
        }}
      >
        {children}
      </Box>
    </Group>
  )
}

function statusPresentation(status: string | null | undefined): {
  label: string
  color: string
  glyph: string
} {
  switch ((status ?? '').trim().toUpperCase()) {
    case 'PASS':
      return { label: 'Passed', color: 'teal', glyph: '●' }
    case 'FAIL':
      return { label: 'Failed', color: 'red', glyph: '✕' }
    case 'EXEC_ERROR':
    case 'ERROR':
      return { label: 'Exec error', color: 'red', glyph: '●' }
    case 'NOT_APPLICABLE':
      return { label: 'N/A', color: 'gray', glyph: '○' }
    default:
      return { label: status?.trim() || '—', color: 'gray', glyph: '○' }
  }
}

function severityLabel(raw: string | null | undefined): string {
  if (!raw?.trim()) return '—'
  const key = raw.trim().toUpperCase()
  const pretty: Record<string, string> = {
    CRITICAL: 'Critical',
    HIGH: 'High',
    MEDIUM: 'Medium',
    LOW: 'Low',
    INFO: 'Info',
  }
  return pretty[key] ?? key
}

function findingMatchesSelection(finding: Finding, selection: GraphSelection | null): boolean {
  if (!selection) return true
  if (selection.kind === 'node') {
    return (finding.entities ?? []).includes(selection.node.id)
  }
  return (finding.edges ?? []).includes(selection.edge.id)
}

function leafFindings(
  leafOutcomeIndex: number,
  outcomes: PolicyOutcome[],
): { id: string; finding: Finding }[] {
  const outcome = outcomes[leafOutcomeIndex]
  if (!outcome) return []
  // Do not synthesize severity from status (VOCAB-MATRIX §10.4).
  const raw = outcome.findings ?? []
  if (raw.length === 0 && (outcome.message || outcome.status)) {
    return [
      {
        id: `f-${leafOutcomeIndex}-0`,
        finding: { message: outcome.message || outcome.status, severity: undefined },
      },
    ]
  }
  return raw.map((f, fi) => ({
    id: `f-${leafOutcomeIndex}-${fi}-${f.message}`,
    finding: f,
  }))
}

export function pruneSuiteEvalTree(
  node: SuiteFolderResult,
  outcomes: PolicyOutcome[],
  graphSelection: GraphSelection | null,
): SuiteFolderResult | null {
  if (graphSelection == null) return node

  const leaves = (node.leaves ?? []).filter((leaf) =>
    leafFindings(leaf.outcomeIndex, outcomes).some((row) =>
      findingMatchesSelection(row.finding, graphSelection),
    ),
  )

  const children = (node.children ?? [])
    .map((c) => pruneSuiteEvalTree(c, outcomes, graphSelection))
    .filter((c): c is SuiteFolderResult => c != null)

  if (leaves.length === 0 && children.length === 0) return null
  return { ...node, leaves, children }
}

function countFindingsUnder(
  node: SuiteFolderResult,
  outcomes: PolicyOutcome[],
  graphSelection: GraphSelection | null,
): number {
  let n = 0
  for (const leaf of node.leaves ?? []) {
    n += leafFindings(leaf.outcomeIndex, outcomes).filter((row) =>
      findingMatchesSelection(row.finding, graphSelection),
    ).length
  }
  for (const c of node.children ?? []) {
    n += countFindingsUnder(c, outcomes, graphSelection)
  }
  return n
}

function resultSummary(findingCount: number, fallback?: string | null): string {
  if (findingCount === 1) return '1 finding'
  if (findingCount > 1) return `${findingCount} findings`
  return fallback?.trim() || '—'
}

function fixedColStyle(width: number): React.CSSProperties {
  return { width, maxWidth: width, minWidth: width }
}

function TreeRow({
  depth,
  expandable,
  expanded,
  onToggle,
  task,
  status,
  severity,
  result,
  active,
  onClick,
  muted,
}: {
  depth: number
  expandable?: boolean
  expanded?: boolean
  onToggle?: () => void
  task: React.ReactNode
  status: string | null | undefined
  severity?: string | null
  result: string
  active?: boolean
  onClick?: () => void
  muted?: boolean
}) {
  const st = statusPresentation(status)
  return (
    <Table.Tr
      onClick={onClick}
      style={{
        cursor: onClick ? 'pointer' : undefined,
        background: active ? 'var(--mantine-color-blue-light)' : undefined,
        opacity: muted ? 0.85 : 1,
      }}
    >
      <Table.Td>
        <Group
          gap={4}
          wrap="nowrap"
          style={{ paddingLeft: depth * 14, minWidth: 0 }}
        >
          {expandable ? (
            <UnstyledButton
              onClick={(e) => {
                e.stopPropagation()
                onToggle?.()
              }}
              aria-label={expanded ? 'Collapse' : 'Expand'}
              style={{ display: 'inline-flex', width: 16, justify: 'center', flexShrink: 0 }}
            >
              {expanded ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
            </UnstyledButton>
          ) : (
            <Box style={{ width: 16, flexShrink: 0 }} />
          )}
          <Box style={{ minWidth: 0, flex: 1 }}>{task}</Box>
        </Group>
      </Table.Td>
      <Table.Td style={fixedColStyle(COL_W.status)}>
        <Group gap={6} wrap="nowrap">
          <Text size="xs" c={st.color} fw={700} style={{ width: 12, textAlign: 'center' }}>
            {st.glyph}
          </Text>
          <Text size="xs" fw={600}>
            {st.label}
          </Text>
        </Group>
      </Table.Td>
      <Table.Td style={fixedColStyle(COL_W.severity)}>
        {severity?.trim() ? (
          <Badge size="xs" variant="light" color={severityBadgeColor(severity)}>
            {severityLabel(severity)}
          </Badge>
        ) : (
          <Text size="xs" c="dimmed">
            —
          </Text>
        )}
      </Table.Td>
      <Table.Td style={{ ...fixedColStyle(COL_W.result), textAlign: 'right' }}>
        <Text size="xs" c="dimmed" truncate>
          {result}
        </Text>
      </Table.Td>
    </Table.Tr>
  )
}

function FindingRows({
  leaf,
  findings,
  depth,
  focusedFindingId,
  onFocusFinding,
}: {
  leaf: SuitePolicyLeafResult
  findings: { id: string; finding: Finding }[]
  depth: number
  focusedFindingId: string | null
  onFocusFinding: (payload: { id: string; finding: Finding }) => void
}) {
  return (
    <>
      {findings.map((row) => (
        <TreeRow
          key={row.id}
          depth={depth}
          task={
            <Text size="xs" style={{ wordBreak: 'break-word' }} lineClamp={2}>
              {row.finding.message}
            </Text>
          }
          status={leaf.status}
          severity={row.finding.severity}
          result="Finding"
          active={focusedFindingId === row.id}
          onClick={() => onFocusFinding({ id: row.id, finding: row.finding })}
          muted
        />
      ))}
    </>
  )
}

function FolderBranch({
  node,
  depth,
  outcomes,
  graphSelection,
  focusedFindingId,
  onFocusFinding,
  defaultExpanded,
}: {
  node: SuiteFolderResult
  depth: number
  outcomes: PolicyOutcome[]
  graphSelection: GraphSelection | null
  focusedFindingId: string | null
  onFocusFinding: (payload: { id: string; finding: Finding }) => void
  defaultExpanded: boolean
}) {
  const [expanded, setExpanded] = useState(defaultExpanded)
  const filtering = graphSelection != null

  const visibleLeaves = useMemo(
    () =>
      (node.leaves ?? [])
        .map((leaf) => {
          const findings = leafFindings(leaf.outcomeIndex, outcomes).filter((row) =>
            findingMatchesSelection(row.finding, graphSelection),
          )
          return { leaf, findings }
        })
        .filter((row) => !filtering || row.findings.length > 0),
    [node.leaves, outcomes, graphSelection, filtering],
  )

  const findingCount = countFindingsUnder(node, outcomes, graphSelection)
  const hasKids = visibleLeaves.length > 0 || (node.children ?? []).length > 0

  return (
    <>
      <TreeRow
        depth={depth}
        expandable={hasKids}
        expanded={expanded}
        onToggle={() => setExpanded((v) => !v)}
        task={
          <Text size="xs" fw={600} truncate>
            {node.name}
            {!node.votes ? (
              <Text span size="xs" c="dimmed" fw={400}>
                {' '}
                (no vote)
              </Text>
            ) : null}
          </Text>
        }
        status={node.status}
        severity={node.severity}
        result={resultSummary(findingCount)}
      />
      {expanded
        ? visibleLeaves.map(({ leaf, findings }) => (
            <LeafBranch
              key={`${leaf.policyId}-${leaf.outcomeIndex}`}
              leaf={leaf}
              findings={findings}
              depth={depth + 1}
              focusedFindingId={focusedFindingId}
              onFocusFinding={onFocusFinding}
              defaultExpanded={filtering || findings.length > 0}
            />
          ))
        : null}
      {expanded
        ? (node.children ?? []).map((child) => (
            <FolderBranch
              key={child.folderId}
              node={child}
              depth={depth + 1}
              outcomes={outcomes}
              graphSelection={graphSelection}
              focusedFindingId={focusedFindingId}
              onFocusFinding={onFocusFinding}
              defaultExpanded={depth < 1}
            />
          ))
        : null}
    </>
  )
}

function LeafBranch({
  leaf,
  findings,
  depth,
  focusedFindingId,
  onFocusFinding,
  defaultExpanded,
}: {
  leaf: SuitePolicyLeafResult
  findings: { id: string; finding: Finding }[]
  depth: number
  focusedFindingId: string | null
  onFocusFinding: (payload: { id: string; finding: Finding }) => void
  defaultExpanded: boolean
}) {
  const [expanded, setExpanded] = useState(defaultExpanded)
  const expandable = findings.length > 0

  useEffect(() => {
    if (defaultExpanded) setExpanded(true)
  }, [defaultExpanded])

  return (
    <>
      <TreeRow
        depth={depth}
        expandable={expandable}
        expanded={expanded}
        onToggle={() => setExpanded((v) => !v)}
        task={
          <Text size="xs" truncate>
            {leaf.policyName}
          </Text>
        }
        status={leaf.status}
        severity={leaf.severity}
        result={resultSummary(findings.length)}
      />
      {expanded && expandable ? (
        <FindingRows
          leaf={leaf}
          findings={findings}
          depth={depth + 1}
          focusedFindingId={focusedFindingId}
          onFocusFinding={onFocusFinding}
        />
      ) : null}
    </>
  )
}

/**
 * Structured suite evaluation tree for Policy Suites Output → Evaluation.
 * Columns: Task | Status | Severity | Result. Uses Query Data table chrome.
 */
export function SuiteEvaluationTree({
  node,
  outcomes,
  graphSelection,
  focusedFindingId,
  onFocusFinding,
  rootLabel,
  rootStatus,
  rootSeverity,
  durationMs,
}: {
  node: SuiteFolderResult
  /** @deprecated ignored — table layout is depth-aware */
  depth?: number
  outcomes: PolicyOutcome[]
  graphSelection: GraphSelection | null
  focusedFindingId: string | null
  onFocusFinding: (payload: { id: string; finding: Finding }) => void
  rootLabel?: string
  rootStatus?: string | null
  rootSeverity?: string | null
  /** Client-measured wall time for the Evaluate call (root row only). */
  durationMs?: number | null
}) {
  const totalFindings = countFindingsUnder(node, outcomes, graphSelection)
  const showSyntheticRoot = Boolean(rootLabel)

  return (
    <Box style={{ overflow: 'auto', minHeight: 0 }}>
      <Table {...queryResultTableProps}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Task</Table.Th>
            <Table.Th style={fixedColStyle(COL_W.status)}>Status</Table.Th>
            <Table.Th style={fixedColStyle(COL_W.severity)}>Severity</Table.Th>
            <Table.Th style={{ ...fixedColStyle(COL_W.result), textAlign: 'right' }}>
              Result
            </Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {showSyntheticRoot ? (
            <>
              <TreeRow
                depth={0}
                task={
                  <Text size="xs" fw={700} truncate>
                    {rootLabel}
                  </Text>
                }
                status={rootStatus ?? node.status}
                severity={rootSeverity ?? node.severity}
                result={
                  durationMs != null && Number.isFinite(durationMs)
                    ? `${resultSummary(totalFindings)} · ${formatQueryDuration(durationMs)}`
                    : resultSummary(totalFindings)
                }
              />
              <FolderBranch
                node={node}
                depth={1}
                outcomes={outcomes}
                graphSelection={graphSelection}
                focusedFindingId={focusedFindingId}
                onFocusFinding={onFocusFinding}
                defaultExpanded
              />
            </>
          ) : (
            <FolderBranch
              node={node}
              depth={0}
              outcomes={outcomes}
              graphSelection={graphSelection}
              focusedFindingId={focusedFindingId}
              onFocusFinding={onFocusFinding}
              defaultExpanded
            />
          )}
        </Table.Tbody>
      </Table>
    </Box>
  )
}

export function countSuiteTreeFindings(
  node: SuiteFolderResult | null | undefined,
  outcomes: PolicyOutcome[],
  graphSelection: GraphSelection | null,
): number {
  if (!node) return 0
  const pruned = pruneSuiteEvalTree(node, outcomes, graphSelection)
  if (!pruned) return 0
  return countFindingsUnder(pruned, outcomes, graphSelection)
}

function outcomeFindingRows(
  outcomeIndex: number,
  outcome: PolicyOutcome,
  graphSelection: GraphSelection | null,
): { id: string; finding: Finding }[] {
  const raw = outcome.findings ?? []
  const rows: { id: string; finding: Finding }[] =
    raw.length === 0 && (outcome.message || outcome.status)
      ? [
          {
            id: `o-${outcomeIndex}`,
            finding: { message: outcome.message || outcome.status, severity: undefined },
          },
        ]
      : raw.map((f, fi) => ({
          id: `f-${outcomeIndex}-${fi}-${f.message}`,
          finding: f,
        }))
  return rows.filter((row) => findingMatchesSelection(row.finding, graphSelection))
}

function OutcomeBranch({
  outcome,
  outcomeIndex,
  depth,
  graphSelection,
  focusedFindingId,
  onFocusFinding,
  defaultExpanded,
}: {
  outcome: PolicyOutcome
  outcomeIndex: number
  depth: number
  graphSelection: GraphSelection | null
  focusedFindingId: string | null
  onFocusFinding: (payload: { id: string; finding: Finding }) => void
  defaultExpanded: boolean
}) {
  const findings = useMemo(
    () => outcomeFindingRows(outcomeIndex, outcome, graphSelection),
    [outcome, outcomeIndex, graphSelection],
  )
  const [expanded, setExpanded] = useState(defaultExpanded)
  const expandable = findings.length > 0

  useEffect(() => {
    if (defaultExpanded) setExpanded(true)
  }, [defaultExpanded])

  return (
    <>
      <TreeRow
        depth={depth}
        expandable={expandable}
        expanded={expanded}
        onToggle={() => setExpanded((v) => !v)}
        task={
          <Text size="xs" fw={600} truncate>
            {outcome.policyName}
          </Text>
        }
        status={outcome.status}
        severity={null}
        result={resultSummary(findings.length)}
      />
      {expanded && expandable
        ? findings.map((row) => (
            <TreeRow
              key={row.id}
              depth={depth + 1}
              task={
                <Text size="xs" style={{ wordBreak: 'break-word' }} lineClamp={2}>
                  {row.finding.message}
                </Text>
              }
              status={outcome.status}
              severity={row.finding.severity}
              result="Finding"
              active={focusedFindingId === row.id}
              onClick={() => onFocusFinding({ id: row.id, finding: row.finding })}
              muted
            />
          ))
        : null}
    </>
  )
}

/**
 * Single-policy evaluation results (Policy Play → Evaluations).
 * Same Query Data table chrome as {@link SuiteEvaluationTree}.
 */
export function PolicyEvaluationTable({
  result,
  graphSelection,
  focusedFindingId,
  onFocusFinding,
  rootLabel,
  durationMs,
}: {
  result: EvaluationResult
  graphSelection: GraphSelection | null
  focusedFindingId: string | null
  onFocusFinding: (payload: { id: string; finding: Finding }) => void
  rootLabel?: string
  durationMs?: number | null
}) {
  const filtering = graphSelection != null
  const visibleOutcomes = useMemo(() => {
    return (result.outcomes ?? [])
      .map((outcome, outcomeIndex) => ({
        outcome,
        outcomeIndex,
        findings: outcomeFindingRows(outcomeIndex, outcome, graphSelection),
      }))
      .filter((row) => !filtering || row.findings.length > 0)
  }, [result.outcomes, graphSelection, filtering])

  const totalFindings = visibleOutcomes.reduce((n, row) => n + row.findings.length, 0)
  const label = rootLabel?.trim() || 'Policy evaluation'

  return (
    <Box style={{ overflow: 'auto', minHeight: 0 }}>
      <Table {...queryResultTableProps}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Task</Table.Th>
            <Table.Th style={fixedColStyle(COL_W.status)}>Status</Table.Th>
            <Table.Th style={fixedColStyle(COL_W.severity)}>Severity</Table.Th>
            <Table.Th style={{ ...fixedColStyle(COL_W.result), textAlign: 'right' }}>
              Result
            </Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          <TreeRow
            depth={0}
            task={
              <Text size="xs" fw={700} truncate>
                {label}
              </Text>
            }
            status={result.overall ?? visibleOutcomes[0]?.outcome.status}
            severity={null}
            result={
              durationMs != null && Number.isFinite(durationMs)
                ? `${resultSummary(totalFindings)} · ${formatQueryDuration(durationMs)}`
                : resultSummary(totalFindings)
            }
          />
          {visibleOutcomes.map(({ outcome, outcomeIndex }) => (
            <OutcomeBranch
              key={`${outcome.policyName}-${outcomeIndex}`}
              outcome={outcome}
              outcomeIndex={outcomeIndex}
              depth={1}
              graphSelection={graphSelection}
              focusedFindingId={focusedFindingId}
              onFocusFinding={onFocusFinding}
              defaultExpanded
            />
          ))}
        </Table.Tbody>
      </Table>
    </Box>
  )
}

export type { SuiteFolderResult }
