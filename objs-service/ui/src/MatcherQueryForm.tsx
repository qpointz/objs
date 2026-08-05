import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useState,
  type ReactNode,
} from 'react'
import {
  ActionIcon,
  Alert,
  Button,
  Group,
  Select,
  Stack,
  Text,
  Textarea,
  TextInput,
} from '@mantine/core'
import { IconPlus, IconTrash } from '@tabler/icons-react'
import { formatQueryExecStats, type QueryExecStats } from './queryExecStats'

export type MatcherMode = 'anno' | 'anno-expr' | 'chained'
export type AnnoRow = { key: string; value: string }

const SAMPLE_ANNO_ROWS: AnnoRow[] = [
  { key: 'app', value: 'payments-api' },
  { key: 'appVersion', value: '2.3.1' },
]

const SAMPLE_EXPR = "app == 'payments-api' && appVersion == '2.3.1'"

const SAMPLE_CHAINED = JSON.stringify(
  [{ anno: { app: 'payments-api' } }, { 'anno-expr': "appVersion == '2.3.1'" }],
  null,
  2,
)

const EMPTY_ANNO_ROWS: AnnoRow[] = [{ key: '', value: '' }]
const EMPTY_EXPR = ''
const EMPTY_CHAINED = '[]'

const MODE_OPTIONS = [
  { value: 'anno', label: 'anno' },
  { value: 'anno-expr', label: 'anno-expr' },
  { value: 'chained', label: 'chained' },
]

export type MatcherFormState = {
  mode: MatcherMode
  annoRows: AnnoRow[]
  exprText: string
  chainedText: string
}

export function buildMatcherBody(
  mode: MatcherMode,
  annoRows: AnnoRow[],
  exprText: string,
  chainedText: string,
): unknown {
  if (mode === 'anno') {
    const filter: Record<string, string> = {}
    for (const row of annoRows) {
      const key = row.key.trim()
      if (!key) continue
      filter[key] = row.value
    }
    if (Object.keys(filter).length === 0) {
      throw new Error('Provide at least one annotation key/value')
    }
    return { anno: filter }
  }
  if (mode === 'anno-expr') {
    const expression = exprText.trim()
    if (!expression) {
      throw new Error('Provide a non-blank anno-expr expression')
    }
    return { 'anno-expr': expression }
  }
  let parsed: unknown
  try {
    parsed = JSON.parse(chainedText)
  } catch {
    throw new Error('Chained matcher must be valid JSON')
  }
  if (!Array.isArray(parsed) || parsed.length === 0) {
    throw new Error('Chained matcher must be a non-empty JSON array')
  }
  return parsed
}

/** Restore form fields from a matcher DSL body (anno / anno-expr / chained). */
export function hydrateFromMatcher(body: unknown): MatcherFormState | null {
  if (Array.isArray(body)) {
    return {
      mode: 'chained',
      annoRows: EMPTY_ANNO_ROWS.map((row) => ({ ...row })),
      exprText: EMPTY_EXPR,
      chainedText: JSON.stringify(body, null, 2),
    }
  }
  if (!body || typeof body !== 'object') return null
  const obj = body as Record<string, unknown>
  if (obj.anno != null && typeof obj.anno === 'object' && !Array.isArray(obj.anno)) {
    const entries = Object.entries(obj.anno as Record<string, unknown>).map(([key, value]) => ({
      key,
      value: String(value ?? ''),
    }))
    return {
      mode: 'anno',
      annoRows: entries.length > 0 ? entries : [{ key: '', value: '' }],
      exprText: EMPTY_EXPR,
      chainedText: EMPTY_CHAINED,
    }
  }
  if (typeof obj['anno-expr'] === 'string') {
    return {
      mode: 'anno-expr',
      annoRows: EMPTY_ANNO_ROWS.map((row) => ({ ...row })),
      exprText: obj['anno-expr'],
      chainedText: EMPTY_CHAINED,
    }
  }
  return null
}

export type MatcherQueryFormHandle = {
  build: () => unknown
}

type MatcherQueryFormProps = {
  /** Rendered on the same row as the mode selector (e.g. Exec / Load). */
  action?: ReactNode
  /** Last successful query wall time + retrieved counts (API only). */
  stats?: QueryExecStats | null
  error?: string | null
  /** When set, hydrates mode + fields from this matcher body. */
  matcher?: unknown | null
  /** Start with blank fields instead of sample criteria (Graph explorer). */
  emptyDefaults?: boolean
}

export const MatcherQueryForm = forwardRef<MatcherQueryFormHandle, MatcherQueryFormProps>(
  function MatcherQueryForm({ action, stats, error, matcher, emptyDefaults = false }, ref) {
    const [mode, setMode] = useState<MatcherMode>('anno')
    const [annoRows, setAnnoRows] = useState<AnnoRow[]>(() =>
      emptyDefaults
        ? EMPTY_ANNO_ROWS.map((row) => ({ ...row }))
        : SAMPLE_ANNO_ROWS.map((row) => ({ ...row })),
    )
    const [exprText, setExprText] = useState(() => (emptyDefaults ? EMPTY_EXPR : SAMPLE_EXPR))
    const [chainedText, setChainedText] = useState(() =>
      emptyDefaults ? EMPTY_CHAINED : SAMPLE_CHAINED,
    )

    useEffect(() => {
      if (matcher === undefined || matcher === null) return
      const hydrated = hydrateFromMatcher(matcher)
      if (!hydrated) return
      setMode(hydrated.mode)
      setAnnoRows(hydrated.annoRows)
      setExprText(hydrated.exprText)
      setChainedText(hydrated.chainedText)
    }, [matcher])

    useImperativeHandle(
      ref,
      () => ({
        build: () => buildMatcherBody(mode, annoRows, exprText, chainedText),
      }),
      [mode, annoRows, exprText, chainedText],
    )

    return (
      <Stack gap="xs">
        <Group align="center" gap="xs" wrap="wrap">
          <Text size="sm" fw={500} style={{ flexShrink: 0 }}>
            Matcher
          </Text>
          <Select
            size="xs"
            w={140}
            allowDeselect={false}
            aria-label="Matcher mode"
            data={MODE_OPTIONS}
            value={mode}
            onChange={(v) => setMode((v as MatcherMode) ?? 'anno')}
          />
          {action}
          {stats != null && (
            <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
              {formatQueryExecStats(stats)}
            </Text>
          )}
        </Group>

        {mode === 'anno' && (
          <Stack gap={6}>
            {annoRows.map((row, index) => (
              <Group key={index} align="flex-end" wrap="nowrap" gap="xs">
                <TextInput
                  size="xs"
                  label={index === 0 ? 'Key' : undefined}
                  value={row.key}
                  onChange={(e) => {
                    const next = [...annoRows]
                    next[index] = { ...row, key: e.currentTarget.value }
                    setAnnoRows(next)
                  }}
                  style={{ flex: 1 }}
                />
                <TextInput
                  size="xs"
                  label={index === 0 ? 'Value' : undefined}
                  value={row.value}
                  onChange={(e) => {
                    const next = [...annoRows]
                    next[index] = { ...row, value: e.currentTarget.value }
                    setAnnoRows(next)
                  }}
                  style={{ flex: 1 }}
                />
                <ActionIcon
                  size="sm"
                  variant="subtle"
                  color="red"
                  aria-label="Remove annotation row"
                  onClick={() => setAnnoRows(annoRows.filter((_, i) => i !== index))}
                  disabled={annoRows.length <= 1}
                  mb={1}
                >
                  <IconTrash size={14} />
                </ActionIcon>
              </Group>
            ))}
            <Button
              variant="light"
              size="compact-xs"
              w="fit-content"
              leftSection={<IconPlus size={12} />}
              onClick={() => setAnnoRows([...annoRows, { key: '', value: '' }])}
            >
              Add key/value
            </Button>
          </Stack>
        )}

        {mode === 'anno-expr' && (
          <Textarea
            size="xs"
            label="Expression"
            minRows={2}
            maxRows={4}
            autosize
            value={exprText}
            onChange={(e) => setExprText(e.currentTarget.value)}
            styles={{ input: { fontFamily: 'var(--mantine-font-family-monospace)' } }}
          />
        )}

        {mode === 'chained' && (
          <Textarea
            size="xs"
            label="Chained (JSON array)"
            minRows={4}
            autosize
            value={chainedText}
            onChange={(e) => setChainedText(e.currentTarget.value)}
            styles={{ input: { fontFamily: 'var(--mantine-font-family-monospace)' } }}
          />
        )}

        {error && (
          <Alert color="red" title="Matcher error">
            {error}
          </Alert>
        )}
      </Stack>
    )
  },
)
