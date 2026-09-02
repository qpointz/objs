import { useCallback, useEffect, useState } from 'react'
import { Box, Group, ScrollArea, Stack, Text } from '@mantine/core'
import {
  edgeVersionStats,
  entityVersionStats,
  getEdgeVersion,
  getEntityVersion,
  getGraph,
  getGraphVersion,
  listEdgeVersions,
  listEntityGraphs,
  listEntityVersions,
  listGraphVersions,
} from './api'
import { useGraphContext } from './GraphContextProvider'
import { ObjectGraphBrowser } from './ObjectGraphBrowser'
import { ObjectViewer } from './ObjectViewer'
import { ObjectVersionBrowser } from './ObjectVersionBrowser'
import {
  formatInstanceVersionLabel,
  OBJECT_VERSION_PREVIEW_N,
  objectDisplayTitle,
} from './objectViewerTitle'
import {
  instanceToVersionRow,
  withLatestInBrowser,
  withLatestInRecent,
  type ObjectVersionRow,
} from './objectVersionRows'
import type {
  BoMEdge,
  BoMEntity,
  BoMGraphHeader,
  BoMGraphVersionSummary,
  GraphSelection,
  PayloadFieldKind,
} from './types'

type GraphContextInfo = {
  graphId: string
  graphVersion: number | null
  annotations: Record<string, string>
  entityCount: number
  edgeCount: number
}

type Props = {
  selection: GraphSelection | null
  /** Current canvas nodes — used to detect missing edge endpoints. */
  nodes: { id: string }[]
  /** When selection is null and graph context is open — show Graph inspect. */
  graphContext: GraphContextInfo | null
  fieldKindsByTypeVersion: Map<string, Record<string, PayloadFieldKind>>
  onSelectNode: (nodeId: string) => void
  onClearSelection: () => void
  endpointLabel: (nodeId: string) => string
}

function payloadName(payload: Record<string, unknown> | undefined): string | null {
  const n = payload?.name
  return typeof n === 'string' ? n : null
}

/**
 * Explorer inspect pane: Object viewer + optional inline version / graphs browser (Note 5 / U-8).
 */
export function ObjectInspectPane({
  selection,
  nodes,
  graphContext,
  fieldKindsByTypeVersion,
  onSelectNode,
  onClearSelection,
  endpointLabel,
}: Props) {
  const { setGraph, context } = useGraphContext()
  const [rightPane, setRightPane] = useState<'versions' | 'graphs' | null>(null)
  const [statsRecent, setStatsRecent] = useState<ObjectVersionRow[]>([])
  const [statsTotal, setStatsTotal] = useState(0)
  const [statsLoading, setStatsLoading] = useState(false)
  const [statsError, setStatsError] = useState<string | null>(null)
  const [browserRows, setBrowserRows] = useState<ObjectVersionRow[]>([])
  const [browserLoading, setBrowserLoading] = useState(false)
  const [browserError, setBrowserError] = useState<string | null>(null)
  const [graphsRows, setGraphsRows] = useState<BoMGraphHeader[]>([])
  const [graphsTotal, setGraphsTotal] = useState(0)
  const [graphsLoading, setGraphsLoading] = useState(false)
  const [graphsError, setGraphsError] = useState<string | null>(null)
  const [inspectEntity, setInspectEntity] = useState<BoMEntity | null>(null)
  const [inspectEdge, setInspectEdge] = useState<BoMEdge | null>(null)
  const [inspectGraph, setInspectGraph] = useState<{
    version: number | null
    annotations: Record<string, string>
  } | null>(null)

  const subjectKey =
    selection?.kind === 'node'
      ? `node:${selection.node.id}`
      : selection?.kind === 'edge'
        ? `edge:${selection.edge.id}`
        : graphContext
          ? `graph:${graphContext.graphId}`
          : 'empty'

  useEffect(() => {
    setRightPane(null)
    setInspectEntity(null)
    setInspectEdge(null)
    setInspectGraph(null)
    setStatsRecent([])
    setStatsTotal(0)
    setStatsError(null)
    setBrowserRows([])
    setBrowserError(null)
    setGraphsRows([])
    setGraphsTotal(0)
    setGraphsError(null)
  }, [subjectKey])

  useEffect(() => {
    if (selection?.kind === 'node') {
      let cancelled = false
      setStatsLoading(true)
      setStatsError(null)
      entityVersionStats(selection.node.id, OBJECT_VERSION_PREVIEW_N)
        .then((s) => {
          if (cancelled) return
          setStatsTotal(s.total)
          setStatsRecent(s.recent.map(instanceToVersionRow))
        })
        .catch((e) => {
          if (!cancelled) {
            setStatsTotal(0)
            setStatsRecent([])
            setStatsError(e instanceof Error ? e.message : String(e))
          }
        })
        .finally(() => {
          if (!cancelled) setStatsLoading(false)
        })
      return () => {
        cancelled = true
      }
    }
    if (selection?.kind === 'edge') {
      let cancelled = false
      setStatsLoading(true)
      setStatsError(null)
      edgeVersionStats(selection.edge.id, OBJECT_VERSION_PREVIEW_N)
        .then((s) => {
          if (cancelled) return
          setStatsTotal(s.total)
          setStatsRecent(s.recent.map(instanceToVersionRow))
        })
        .catch((e) => {
          if (!cancelled) {
            setStatsTotal(0)
            setStatsRecent([])
            setStatsError(e instanceof Error ? e.message : String(e))
          }
        })
        .finally(() => {
          if (!cancelled) setStatsLoading(false)
        })
      return () => {
        cancelled = true
      }
    }
    if (!selection && graphContext) {
      let cancelled = false
      setStatsLoading(true)
      setStatsError(null)
      const pinned = graphContext.graphVersion != null
      listGraphVersions(graphContext.graphId)
        .then((list) => {
          if (cancelled) return
          const deep = list.map(graphRowToInstance)
          setStatsTotal(list.length)
          setStatsRecent(
            withLatestInRecent(deep, {
              includeLatest: pinned,
              id: graphContext.graphId,
              annotations: graphContext.annotations,
              recentN: OBJECT_VERSION_PREVIEW_N,
            }),
          )
        })
        .catch((e) => {
          if (!cancelled) {
            setStatsTotal(0)
            setStatsRecent([])
            setStatsError(e instanceof Error ? e.message : String(e))
          }
        })
        .finally(() => {
          if (!cancelled) setStatsLoading(false)
        })
      return () => {
        cancelled = true
      }
    }
    setStatsRecent([])
    setStatsTotal(0)
    setStatsLoading(false)
  }, [selection, graphContext])

  useEffect(() => {
    if (selection?.kind !== 'node') {
      setGraphsRows([])
      setGraphsTotal(0)
      setGraphsLoading(false)
      setGraphsError(null)
      return
    }
    let cancelled = false
    setGraphsLoading(true)
    setGraphsError(null)
    listEntityGraphs(selection.node.id)
      .then((res) => {
        if (cancelled) return
        setGraphsTotal(res.total)
        setGraphsRows(res.items)
      })
      .catch((e) => {
        if (!cancelled) {
          setGraphsTotal(0)
          setGraphsRows([])
          setGraphsError(e instanceof Error ? e.message : String(e))
        }
      })
      .finally(() => {
        if (!cancelled) setGraphsLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [selection])

  useEffect(() => {
    if (rightPane !== 'versions') return
    let cancelled = false
    setBrowserLoading(true)
    setBrowserError(null)
    const load =
      selection?.kind === 'node'
        ? listEntityVersions(selection.node.id).then((list) => list.map(instanceToVersionRow))
        : selection?.kind === 'edge'
          ? listEdgeVersions(selection.edge.id).then((list) => list.map(instanceToVersionRow))
          : graphContext
            ? listGraphVersions(graphContext.graphId).then((list) =>
                withLatestInBrowser(list.map(graphRowToInstance), {
                  includeLatest: true,
                  id: graphContext.graphId,
                  annotations: graphContext.annotations,
                }),
              )
            : Promise.resolve([])
    load
      .then((rows) => {
        if (!cancelled) setBrowserRows(rows)
      })
      .catch((e) => {
        if (!cancelled) {
          setBrowserRows([])
          setBrowserError(e instanceof Error ? e.message : String(e))
        }
      })
      .finally(() => {
        if (!cancelled) setBrowserLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [rightPane, selection, graphContext])

  const openSharedGraph = useCallback(
    async (graphId: string) => {
      try {
        const resolved = await getGraph(graphId)
        setGraph(graphId, resolved.annotations ?? {}, {
          nodeCount: resolved.graph?.entities?.length ?? 0,
          edgeCount: resolved.graph?.edges?.length ?? 0,
        })
      } catch {
        // leave previous context
      }
    },
    [setGraph],
  )

  const openVersion = useCallback(
    async (version: number | null) => {
      setRightPane('versions')
      try {
        if (selection?.kind === 'node') {
          if (version == null) {
            setInspectEntity(null)
            setInspectEdge(null)
            setInspectGraph(null)
            return
          }
          const row = await getEntityVersion(selection.node.id, version)
          setInspectEntity(row)
          setInspectEdge(null)
          setInspectGraph(null)
          return
        }
        if (selection?.kind === 'edge') {
          if (version == null) {
            setInspectEntity(null)
            setInspectEdge(null)
            setInspectGraph(null)
            return
          }
          const row = await getEdgeVersion(selection.edge.id, version)
          setInspectEdge(row)
          setInspectEntity(null)
          setInspectGraph(null)
          return
        }
        if (graphContext) {
          if (version == null) {
            setInspectGraph(null)
            setInspectEntity(null)
            setInspectEdge(null)
            return
          }
          const pack = await getGraphVersion(graphContext.graphId, version)
          setInspectGraph({
            version,
            annotations: pack.annotations ?? {},
          })
          setInspectEntity(null)
          setInspectEdge(null)
        }
      } catch {
        // leave previous inspect
      }
    },
    [selection, graphContext],
  )

  if (!selection && !graphContext) {
    return (
      <Text c="dimmed" size="sm" p="md">
        Select a node or edge to inspect.
      </Text>
    )
  }

  const paneOpen = rightPane != null
  const graphsPreview = graphsRows.slice(0, OBJECT_VERSION_PREVIEW_N)
  const graphsProps =
    selection?.kind === 'node' && !paneOpen
      ? {
          loading: graphsLoading,
          total: graphsTotal,
          recent: graphsPreview,
          error: graphsError,
          onOpenBrowser: () => setRightPane('graphs'),
          onSelectGraph: (graphId: string) => void openSharedGraph(graphId),
        }
      : null

  const viewer = (() => {
    if (selection?.kind === 'node') {
      const live = selection.node
      const entity = inspectEntity
      const type = entity?.type ?? live.type
      const schemaVersion = entity?.schemaVersion ?? live.schemaVersion
      const id = entity?.id ?? live.id
      const payload = entity?.payload ?? live.payload ?? {}
      const annotations = entity?.annotations ?? live.annotations ?? {}
      const title = objectDisplayTitle(payloadName(payload), type, id)
      const fieldKinds =
        live.payloadFieldKinds ?? fieldKindsByTypeVersion.get(`${type}@${schemaVersion}`)
      return (
        <ObjectViewer
          title={title}
          identityLabel="Node"
          onClose={onClearSelection}
          inspectingVersionLabel={
            inspectEntity != null ? formatInstanceVersionLabel(inspectEntity.headVersion) : null
          }
          type={type}
          schemaVersion={schemaVersion}
          id={id}
          payload={payload}
          annotations={annotations}
          fieldKinds={fieldKinds}
          showAnnotations
          showVersions={!paneOpen}
          versions={
            !paneOpen
              ? {
                  headVersion: live.headVersion,
                  loading: statsLoading,
                  total: statsTotal,
                  recent: statsRecent,
                  error: statsError,
                  onOpenBrowser: () => setRightPane('versions'),
                  onSelectVersion: (v) => void openVersion(v),
                }
              : null
          }
          showGraphs={!paneOpen}
          graphs={graphsProps}
        />
      )
    }

    if (selection?.kind === 'edge') {
      const live = selection.edge
      const edge = inspectEdge
      const type = edge?.type ?? live.type
      const schemaVersion = edge?.schemaVersion ?? live.schemaVersion
      const id = edge?.id ?? live.id
      const payload = edge?.properties ?? live.properties ?? {}
      const title = objectDisplayTitle(live.role, type ?? 'edge', id)
      const fieldKinds = type
        ? fieldKindsByTypeVersion.get(`${type}@${schemaVersion ?? '1.0.0'}`)
        : undefined
      const source = edge?.source ?? live.source
      const target = edge?.target ?? live.target
      const sourcePresent = nodes.some((n) => n.id === source)
      const targetPresent = nodes.some((n) => n.id === target)
      return (
        <ObjectViewer
          title={title}
          identityLabel="Node"
          onClose={onClearSelection}
          inspectingVersionLabel={
            inspectEdge != null ? formatInstanceVersionLabel(inspectEdge.headVersion) : null
          }
          type={type}
          schemaVersion={schemaVersion}
          id={id}
          payload={payload}
          payloadLabel="properties"
          fieldKinds={fieldKinds}
          relation={{
            sourceLabel: endpointLabel(source),
            targetLabel: endpointLabel(target),
            sourcePresent,
            targetPresent,
            onSource: () => onSelectNode(source),
            onTarget: () => onSelectNode(target),
          }}
          showAnnotations={false}
          showVersions={!paneOpen}
          versions={
            !paneOpen
              ? {
                  headVersion: live.headVersion,
                  loading: statsLoading,
                  total: statsTotal,
                  recent: statsRecent,
                  error: statsError,
                  onOpenBrowser: () => setRightPane('versions'),
                  onSelectVersion: (v) => void openVersion(v),
                }
              : null
          }
        />
      )
    }

    const g = graphContext!
    const ann = inspectGraph?.annotations ?? g.annotations
    const versionLabel =
      inspectGraph != null
        ? formatInstanceVersionLabel(inspectGraph.version)
        : formatInstanceVersionLabel(g.graphVersion)
    const title = objectDisplayTitle(null, 'graph', g.graphId)
    return (
      <ObjectViewer
        title={title}
        identityLabel="Graph"
        inspectingVersionLabel={inspectGraph != null ? versionLabel : null}
        type={null}
        schemaVersion={null}
        id={g.graphId}
        payload={{
          entities: g.entityCount,
          edges: g.edgeCount,
          version: versionLabel,
        }}
        annotations={ann}
        showAnnotations
        showVersions={!paneOpen}
        versions={
          !paneOpen
            ? {
                headVersion: g.graphVersion,
                loading: statsLoading,
                total: statsTotal,
                recent: statsRecent,
                error: statsError,
                onOpenBrowser: () => setRightPane('versions'),
                onSelectVersion: (v) => void openVersion(v),
              }
            : null
        }
      />
    )
  })()

  const selectedVersion: number | null | undefined =
    inspectEntity != null
      ? (inspectEntity.headVersion ?? null)
      : inspectEdge != null
        ? (inspectEdge.headVersion ?? null)
        : inspectGraph != null
          ? inspectGraph.version
          : undefined

  if (rightPane === 'versions') {
    return (
      <Group align="stretch" wrap="nowrap" gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
        <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars type="auto">
          <Stack gap="xs">
            {selectedVersion === undefined && (
              <Text size="sm" c="dimmed" mih={20}>
                Select version
              </Text>
            )}
            {viewer}
          </Stack>
        </ScrollArea>
        <Box
          style={{
            width: '42%',
            minWidth: 160,
            maxWidth: 280,
            flexShrink: 0,
            borderLeft: '1px solid var(--mantine-color-default-border)',
            paddingLeft: 8,
            minHeight: 0,
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <ObjectVersionBrowser
            rows={browserRows}
            loading={browserLoading}
            error={browserError}
            selectedVersion={selectedVersion}
            onSelect={(v) => void openVersion(v)}
            onClose={() => {
              setRightPane(null)
              setInspectEntity(null)
              setInspectEdge(null)
              setInspectGraph(null)
            }}
          />
        </Box>
      </Group>
    )
  }

  if (rightPane === 'graphs') {
    return (
      <Group align="stretch" wrap="nowrap" gap="sm" style={{ flex: 1, minHeight: 0, height: '100%' }}>
        <ScrollArea style={{ flex: 1, minHeight: 0 }} offsetScrollbars type="auto">
          <Stack gap="xs">{viewer}</Stack>
        </ScrollArea>
        <Box
          style={{
            width: '42%',
            minWidth: 160,
            maxWidth: 280,
            flexShrink: 0,
            borderLeft: '1px solid var(--mantine-color-default-border)',
            paddingLeft: 8,
            minHeight: 0,
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <ObjectGraphBrowser
            rows={graphsRows}
            loading={graphsLoading}
            error={graphsError}
            selectedGraphId={context.kind === 'graph' ? context.graphId : null}
            onSelect={(graphId) => void openSharedGraph(graphId)}
            onClose={() => setRightPane(null)}
          />
        </Box>
      </Group>
    )
  }

  return (
    <ScrollArea style={{ flex: 1, minHeight: 0, height: '100%' }} offsetScrollbars type="auto">
      {viewer}
    </ScrollArea>
  )
}

function graphRowToInstance(row: BoMGraphVersionSummary): ObjectVersionRow {
  return {
    id: row.graphId,
    version: row.version,
    createdAt: row.createdAt,
    updatedAt: row.createdAt,
    annotations: row.annotations ?? {},
  }
}
