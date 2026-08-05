import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Code,
  Collapse,
  Group,
  Modal,
  Paper,
  Stack,
  Tabs,
  Text,
  Title,
} from '@mantine/core'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { mutationShapeError, normalizeGraphMutation } from './graphDraft'
import { putGraphMutation, queryGraph, validateGraphMutation } from './api'
import { JsonYamlEditor, type JsonYamlEditorHandle } from './JsonYamlEditor'
import { MatcherLoadPanel } from './MatcherLoadPanel'
import { NewUuidButton } from './NewUuidButton'
import { ObjectLinterVisualPanel } from './ObjectLinterVisualPanel'
import type { GraphSelection, GraphValidationResult } from './types'
import { useGraphDraft } from './useGraphDraft'

export { graphShapeError, mutationShapeError } from './graphDraft'

type ObjectLinterNavState = {
  matcher?: unknown
}

export function ObjectLinterPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const {
    document,
    pendingDeleteCount,
    mutationBody,
    emptyMutation,
    state,
    applyParsedMutation,
    resetToRollback,
    clearDraft,
    loadSubgraph,
    upsertEntity,
    upsertEdge,
    removeEntity,
    removeEdge,
    markApplied,
    canvasDocument,
    restoreDeletedEntity,
    restoreDeletedEdge,
    revertEntityChanges,
    revertEdgeChanges,
  } = useGraphDraft()

  const editorRef = useRef<JsonYamlEditorHandle>(null)
  const [tab, setTab] = useState<'visual' | 'text'>('visual')
  const [textError, setTextError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [loadBusy, setLoadBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<GraphValidationResult | null>(null)
  const [selection, setSelection] = useState<GraphSelection | null>(null)
  const [loadOpen, setLoadOpen] = useState(false)
  const [loadConfirmOpen, setLoadConfirmOpen] = useState(false)
  const [pendingMatcher, setPendingMatcher] = useState<unknown>(null)
  const [activeMatcher, setActiveMatcher] = useState<unknown | null>(null)

  const onDraftParsed = useCallback(
    (parsed: { valid: boolean; value?: unknown; error?: string }) => {
      // Text editor stays mounted under the Visual tab; ignore its sync while Visual is active
      // so a stale YAML round-trip cannot resurrect cascaded soft-deleted edges.
      if (tab !== 'text') return
      setResult(null)
      if (!parsed.valid) {
        setTextError(parsed.error ?? 'Invalid mutation')
        return
      }
      const shape = mutationShapeError(parsed.value)
      if (shape) {
        setTextError(shape)
        return
      }
      const err = applyParsedMutation(parsed.value)
      if (err) {
        setTextError(err)
        return
      }
      setTextError(null)
    },
    [applyParsedMutation, tab],
  )

  function trySwitchTab(next: string | null) {
    if (!next || next === tab) return
    if (next === 'visual' && textError) {
      setError('Fix YAML/JSON before switching to Visual (last good draft is preserved).')
      return
    }
    setError(null)
    setTab(next as 'visual' | 'text')
  }

  async function syncTextIntoDraft(): Promise<{ body: typeof mutationBody } | null> {
    if (tab !== 'text') return { body: mutationBody }
    const parsed = editorRef.current?.getParsedForSubmit()
    if (!parsed?.ok) {
      setError(parsed?.error ?? 'Invalid mutation')
      setResult(null)
      return null
    }
    const shapeErr = mutationShapeError(parsed.value)
    if (shapeErr) {
      setError(shapeErr)
      setResult(null)
      return null
    }
    const normalized = normalizeGraphMutation(parsed.value)
    if (!normalized) {
      setError('Invalid mutation')
      return null
    }
    const err = applyParsedMutation(parsed.value)
    if (err) {
      setError(err)
      return null
    }
    return { body: normalized }
  }

  async function validate() {
    const synced = await syncTextIntoDraft()
    if (!synced) return
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      setResult(await validateGraphMutation(synced.body))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function apply() {
    const synced = await syncTextIntoDraft()
    if (!synced) return
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      const validation = await validateGraphMutation(synced.body)
      setResult(validation)
      if (validation.issues.length > 0) {
        setError('Fix validation issues before Apply')
        return
      }
      await putGraphMutation(synced.body)
      markApplied()
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  async function doLoad(matcherBody: unknown) {
    setLoadBusy(true)
    setError(null)
    try {
      const subgraph = await queryGraph(matcherBody)
      loadSubgraph(subgraph)
      setSelection(null)
      setResult(null)
      setActiveMatcher(matcherBody)
      setLoadOpen(false)
      setLoadConfirmOpen(false)
      setPendingMatcher(null)
      setTab('visual')
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoadBusy(false)
    }
  }

  async function onLoadRequest(matcherBody: unknown) {
    if (document.entities.length > 0 || document.edges.length > 0 || pendingDeleteCount > 0) {
      setPendingMatcher(matcherBody)
      setLoadConfirmOpen(true)
      return
    }
    await doLoad(matcherBody)
  }

  const onLoadRequestRef = useRef(onLoadRequest)
  onLoadRequestRef.current = onLoadRequest

  useEffect(() => {
    const navState = location.state as ObjectLinterNavState | null
    if (navState == null || typeof navState !== 'object' || !('matcher' in navState)) return
    if (navState.matcher === undefined) return
    const matcher = navState.matcher
    setActiveMatcher(matcher)
    setLoadOpen(false)
    navigate('.', { replace: true, state: null })
    void onLoadRequestRef.current(matcher)
  }, [location.state, navigate])

  const valid = result != null && result.issues.length === 0

  return (
    <Stack gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Group justify="space-between" align="flex-start" style={{ flexShrink: 0 }}>
        <div>
          <Title order={3}>Object linter</Title>
          <Text size="sm" c="dimmed">
            Load an optional subgraph, manipulate the draft visually or as YAML/JSON, then Validate
            or Apply (transactional upsert + delete).
          </Text>
        </div>
        <Group>
          <Button variant="default" component={Link} to="/model">
            Browse schemas
          </Button>
          <Button variant="light" onClick={() => setLoadOpen((v) => !v)}>
            {loadOpen ? 'Hide load' : 'Load…'}
          </Button>
          <Button variant="default" onClick={resetToRollback}>
            Reset
          </Button>
          <Button
            variant="default"
            color="red"
            onClick={() => {
              clearDraft()
              setSelection(null)
              setResult(null)
            }}
          >
            Clear
          </Button>
          <Button loading={busy} variant="light" onClick={() => void validate()}>
            Validate
          </Button>
          <Button loading={busy} onClick={() => void apply()}>
            Apply
          </Button>
        </Group>
      </Group>

      <Collapse in={loadOpen}>
        <Paper withBorder p="md">
          <MatcherLoadPanel
            loading={loadBusy}
            matcher={activeMatcher}
            onLoad={onLoadRequest}
          />
        </Paper>
      </Collapse>

      <Tabs
        value={tab}
        onChange={trySwitchTab}
        style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
      >
        <Group justify="space-between" align="center" gap="sm" wrap="nowrap" style={{ flexShrink: 0 }}>
          <Tabs.List style={{ flex: 1 }}>
            <Tabs.Tab value="visual">Visual</Tabs.Tab>
            <Tabs.Tab value="text">Text</Tabs.Tab>
          </Tabs.List>
          <Group gap={6} wrap="nowrap">
            {mutationBody.upsert.entities.length + mutationBody.upsert.edges.length > 0 && (
              <Badge color="blue" variant="light" size="sm">
                {mutationBody.upsert.entities.length + mutationBody.upsert.edges.length} upsert
                {mutationBody.upsert.entities.length + mutationBody.upsert.edges.length === 1
                  ? ''
                  : 's'}
              </Badge>
            )}
            {pendingDeleteCount > 0 && (
              <Badge color="orange" variant="filled" size="sm">
                {pendingDeleteCount} pending delete{pendingDeleteCount === 1 ? '' : 's'}
              </Badge>
            )}
            <Badge variant="light" size="sm">
              {canvasDocument.entities.length} on canvas
            </Badge>
          </Group>
        </Group>

        <Tabs.Panel
          value="visual"
          pt="sm"
          style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
        >
          <ObjectLinterVisualPanel
            draftState={state}
            canvasDocument={canvasDocument}
            selection={selection}
            onSelect={setSelection}
            onUpsertEntity={upsertEntity}
            onUpsertEdge={upsertEdge}
            onRemoveEntity={removeEntity}
            onRemoveEdge={removeEdge}
            onRestoreDeletedEntity={restoreDeletedEntity}
            onRestoreDeletedEdge={restoreDeletedEdge}
            onRevertEntityChanges={revertEntityChanges}
            onRevertEdgeChanges={revertEdgeChanges}
          />
        </Tabs.Panel>

        <Tabs.Panel
          value="text"
          pt="sm"
          style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
        >
          <Paper
            withBorder
            p="sm"
            style={{
              flex: 1,
              minHeight: 0,
              display: 'flex',
              flexDirection: 'column',
            }}
          >
            <JsonYamlEditor
              ref={editorRef}
              value={mutationBody}
              rollbackValue={emptyMutation}
              minHeight={240}
              fillHeight
              onDraftParsed={onDraftParsed}
              onRollback={() => {
                resetToRollback()
                setTextError(null)
                setResult(null)
              }}
              extraActions={<NewUuidButton />}
            />
          </Paper>
          {textError && (
            <Alert mt="sm" color="orange" title="Text parse error" style={{ flexShrink: 0 }}>
              {textError}
            </Alert>
          )}
        </Tabs.Panel>
      </Tabs>

      {error && (
        <Alert color="red" title="Error" style={{ flexShrink: 0 }}>
          {error}
        </Alert>
      )}

      {result && (
        <Paper withBorder p="md" style={{ flexShrink: 0, maxHeight: 220, overflow: 'auto' }}>
          <Group mb="sm">
            <Text fw={700}>Validation result</Text>
            <Badge color={valid ? 'green' : 'red'}>{valid ? 'valid' : 'invalid'}</Badge>
          </Group>
          {valid ? (
            <Text size="sm">The mutation conforms to the registered schemas and edge rules.</Text>
          ) : (
            <Stack gap="xs">
              {result.issues.map((issue, index) => (
                <Alert key={`${issue.code}:${issue.path}:${index}`} color="red" title={issue.code}>
                  <Text size="sm">{issue.message}</Text>
                  {issue.path && (
                    <Code mt={4} style={{ display: 'inline-block' }}>
                      {issue.path}
                    </Code>
                  )}
                </Alert>
              ))}
            </Stack>
          )}
        </Paper>
      )}

      <Modal
        opened={loadConfirmOpen}
        onClose={() => {
          setLoadConfirmOpen(false)
          setPendingMatcher(null)
        }}
        title="Replace draft?"
      >
        <Stack>
          <Text size="sm">
            Loading replaces the current draft and pending deletes. Continue?
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              onClick={() => {
                setLoadConfirmOpen(false)
                setPendingMatcher(null)
              }}
            >
              Cancel
            </Button>
            <Button
              loading={loadBusy}
              onClick={() => {
                if (pendingMatcher != null) void doLoad(pendingMatcher)
              }}
            >
              Replace
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
