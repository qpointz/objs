import { Badge, Box, Group, Stack, Text } from '@mantine/core'
import type { Finding, PolicyOutcome, SuiteFolderResult } from './policyTypes'
import type { GraphSelection } from './types'

const SEVERITY_COL_WIDTH = 72

export function severityBadgeColor(raw: string | null | undefined): string {
  switch ((raw ?? '').trim().toUpperCase()) {
    case 'ERROR':
    case 'FAIL':
      return 'red'
    case 'WARN':
    case 'WARNING':
      return 'orange'
    case 'OK':
    case 'PASS':
      return 'green'
    case 'INFO':
      return 'cyan'
    default:
      return 'gray'
  }
}

/** Fixed-width severity column + left-aligned message (same depth → same left edge). */
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
  const raw =
    (outcome.findings ?? []).length > 0
      ? outcome.findings!
      : [
          {
            message: outcome.message || outcome.status,
            severity: outcome.status === 'PASS' ? 'OK' : outcome.status,
          } satisfies Finding,
        ]
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

export function SuiteEvaluationTree({
  node,
  depth,
  outcomes,
  graphSelection,
  focusedFindingId,
  onFocusFinding,
}: {
  node: SuiteFolderResult
  depth: number
  outcomes: PolicyOutcome[]
  graphSelection: GraphSelection | null
  focusedFindingId: string | null
  onFocusFinding: (payload: { id: string; finding: Finding }) => void
}) {
  const filtering = graphSelection != null

  const visibleLeaves = (node.leaves ?? [])
    .map((leaf) => {
      const findings = leafFindings(leaf.outcomeIndex, outcomes).filter((row) =>
        findingMatchesSelection(row.finding, graphSelection),
      )
      return { leaf, findings }
    })
    .filter((row) => !filtering || row.findings.length > 0)

  return (
    <Stack gap={4}>
      <SeverityMessageRow severity={node.severity ?? node.status} depth={depth}>
        <Text size="sm" fw={600}>
          {node.name}
          {!node.votes ? (
            <Text span size="xs" c="dimmed" fw={400}>
              {' '}
              (no vote)
            </Text>
          ) : null}
        </Text>
      </SeverityMessageRow>
      {visibleLeaves.map(({ leaf, findings }) => (
        <Stack key={`${leaf.policyId}-${leaf.outcomeIndex}`} gap={2}>
          <SeverityMessageRow severity={leaf.severity ?? leaf.status} depth={depth + 1}>
            <Text size="sm">{leaf.policyName}</Text>
          </SeverityMessageRow>
          {findings.map((row) => (
            <SeverityMessageRow
              key={row.id}
              severity={row.finding.severity ?? leaf.status}
              depth={depth + 2}
              active={focusedFindingId === row.id}
              onClick={() => onFocusFinding({ id: row.id, finding: row.finding })}
            >
              <Text size="sm" style={{ wordBreak: 'break-word' }}>
                {row.finding.message}
              </Text>
            </SeverityMessageRow>
          ))}
        </Stack>
      ))}
      {(node.children ?? []).map((child) => (
        <SuiteEvaluationTree
          key={child.folderId}
          node={child}
          depth={depth + 1}
          outcomes={outcomes}
          graphSelection={graphSelection}
          focusedFindingId={focusedFindingId}
          onFocusFinding={onFocusFinding}
        />
      ))}
    </Stack>
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
  let n = 0
  const walk = (f: SuiteFolderResult) => {
    for (const leaf of f.leaves ?? []) {
      n += leafFindings(leaf.outcomeIndex, outcomes).filter((row) =>
        findingMatchesSelection(row.finding, graphSelection),
      ).length
    }
    for (const c of f.children ?? []) walk(c)
  }
  walk(pruned)
  return n
}

export type { SuiteFolderResult }
