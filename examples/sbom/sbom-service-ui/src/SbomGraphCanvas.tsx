import {
  Background,
  Controls,
  Handle,
  MarkerType,
  MiniMap,
  Position,
  ReactFlow,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useReactFlow,
  type Edge,
  type Node,
  type NodeProps,
  type NodeTypes,
} from '@xyflow/react'
import dagre from '@dagrejs/dagre'
import '@xyflow/react/dist/style.css'
import { Menu } from '@mantine/core'
import { IconArrowNarrowDown, IconArrowNarrowUp, IconChevronRight } from '@tabler/icons-react'
import { forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState, type MouseEvent, type ReactNode, type Ref, Fragment } from 'react'
import type { AssetView, RelationView } from './api/types'
import type { DraftKind } from './bomDraft'
import { DraftStatusPill, draftStatusColor } from './DraftStatusPill'

const NODE_W = 188
const NODE_H = 76
const NODE_SEP = 88
const RANK_SEP = 110
const EDGE_SEP = 28
const PALETTE = [
  '#228be6',
  '#40c057',
  '#fab005',
  '#fa5252',
  '#7950f2',
  '#15aabf',
  '#e64980',
  '#fd7e14',
  '#12b886',
  '#4c6ef5',
  '#82c91e',
  '#be4bdb',
]
const DIMMED_OPACITY = 0.25
const EDGE_COLOR = '#495057'
const EDGE_SELECTED = '#228be6'

export const DEFAULT_TYPE_COLOR = 'var(--mantine-color-gray-6)'

export function colorForType(type: string, attributes?: Record<string, string> | null): string {
  const raw = attributes?.color?.trim()
  if (raw && raw.toLowerCase() === 'nocolor') return DEFAULT_TYPE_COLOR
  if (raw && /^#[0-9a-fA-F]{6}$/.test(raw)) return raw.toLowerCase()
  let h = 0
  for (let i = 0; i < type.length; i++) h = (h * 31 + type.charCodeAt(i)) >>> 0
  return PALETTE[h % PALETTE.length]
}

export type GraphViewMode = 'minimal' | 'details'

export type GraphLayoutDir = 'TB' | 'LR' | 'BT' | 'RL'

export type SbomGraphHandle = {
  focusNode: (nodeId: string) => void
  applyLayout: (layout?: GraphLayoutDir) => void
}

type Props = {
  assets: AssetView[]
  relations: RelationView[]
  selectedAssetId: string | null
  onSelectAsset: (assetId: string | null) => void
  visible?: boolean
  viewMode?: GraphViewMode
  highlightedTypes?: ReadonlySet<string>
  assetStatus?: ReadonlyMap<string, DraftKind>
  relationStatus?: ReadonlyMap<string, DraftKind>
  changesOnly?: boolean
}

type PayloadRow = { key: string; value: string }

type CardData = {
  label: string
  type: string
  color: string
  emphasized: boolean
  dimmed: boolean
  viewMode: GraphViewMode
  payloadRows: PayloadRow[]
  draftStatus: DraftKind
}

const HANDLE_STYLE = {
  opacity: 0,
  width: 8,
  height: 8,
  border: 'none',
  background: 'transparent',
} as const

function payloadRows(payload: Record<string, unknown>): PayloadRow[] {
  const rows: PayloadRow[] = []
  for (const [key, value] of Object.entries(payload ?? {})) {
    if (value == null || value === '') continue
    if (typeof value === 'object') {
      rows.push({ key, value: Array.isArray(value) ? `[${value.length}]` : '{…}' })
    } else {
      const text = String(value)
      rows.push({ key, value: text.length > 28 ? `${text.slice(0, 27)}…` : text })
    }
    if (rows.length >= 8) break
  }
  return rows
}

function nodeSize(data: CardData): { width: number; height: number } {
  if (data.viewMode !== 'details') return { width: NODE_W, height: NODE_H }
  const width = 240
  const height = 44 + Math.max(1, data.payloadRows.length) * 16 + 10
  return { width, height }
}

function statusColor(status: DraftKind): string | null {
  return draftStatusColor(status)
}

function StatusPill({ status }: { status: DraftKind }) {
  return <DraftStatusPill status={status} ml={6} />
}

type AssetNodeType = Node<CardData, 'asset'>

function AssetNode({ data, selected }: NodeProps<AssetNodeType>) {
  const { width } = nodeSize(data)
  const ring = selected || data.emphasized
  const details = data.viewMode === 'details'
  const [hovered, setHovered] = useState(false)
  const draftColor = statusColor(data.draftStatus)
  const borderColor = selected ? EDGE_SELECTED : draftColor ?? data.color
  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        width,
        opacity: data.dimmed && !hovered ? DIMMED_OPACITY : 1,
        border: `2px solid ${borderColor}`,
        boxShadow: ring ? `0 0 0 3px rgba(34, 139, 230, 0.28)` : 'none',
        borderRadius: 8,
        background: selected ? 'var(--mantine-color-blue-0)' : '#fff',
        overflow: 'hidden',
        padding: details ? 0 : 8,
        textDecoration: data.draftStatus === 'deleted' ? 'line-through' : undefined,
      }}
    >
      <Handle type="target" id="top-target" position={Position.Top} style={HANDLE_STYLE} />
      <Handle type="source" id="top-source" position={Position.Top} style={HANDLE_STYLE} />
      <Handle type="target" id="left-target" position={Position.Left} style={HANDLE_STYLE} />
      <Handle type="source" id="left-source" position={Position.Left} style={HANDLE_STYLE} />
      <Handle type="target" id="right-target" position={Position.Right} style={HANDLE_STYLE} />
      <Handle type="source" id="right-source" position={Position.Right} style={HANDLE_STYLE} />
      {details ? (
        <>
          <div
            style={{
              padding: '6px 8px',
              background: data.color,
              color: '#fff',
              fontSize: 11,
              fontWeight: 700,
            }}
          >
            {data.type}
            <StatusPill status={data.draftStatus} />
          </div>
          <div style={{ padding: '6px 8px 8px' }}>
            <div style={{ fontSize: 12, fontWeight: 700 }}>{data.label}</div>
            <div
              style={{
                marginTop: 6,
                borderTop: '1px solid #dee2e6',
                paddingTop: 6,
                display: 'grid',
                gridTemplateColumns: 'minmax(56px, 38%) 1fr',
                gap: 2,
              }}
            >
              {data.payloadRows.length === 0 ? (
                <div style={{ gridColumn: '1 / -1', fontSize: 10, color: '#adb5bd' }}>No properties</div>
              ) : (
                data.payloadRows.map((row) => (
                  <div key={row.key} style={{ display: 'contents' }}>
                    <div style={{ fontSize: 10, color: '#868e96', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {row.key}
                    </div>
                    <div style={{ fontSize: 10, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {row.value}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      ) : (
        <>
          <div style={{ fontSize: 10, color: data.color, fontWeight: 700 }}>
            {data.type}
            <StatusPill status={data.draftStatus} />
          </div>
          <div style={{ fontSize: 12, fontWeight: 600 }}>{data.label}</div>
        </>
      )}
      <Handle type="target" id="bottom-target" position={Position.Bottom} style={HANDLE_STYLE} />
      <Handle type="source" id="bottom-source" position={Position.Bottom} style={HANDLE_STYLE} />
    </div>
  )
}

const nodeTypes: NodeTypes = { asset: AssetNode }

type GraphCtxMenu =
  | { kind: 'node'; nodeId: string; x: number; y: number }
  | { kind: 'edge'; edgeId: string; sourceId: string; targetId: string; x: number; y: number }

type NeighborRef = { id: string; type: string; label: string }

type RelationGroup = {
  key: string
  title: string
  direction: 'IN' | 'OUT'
  connected: NeighborRef[]
}

function graphAssetCaption(type: string, label: string) {
  return `${type} ${label}`
}

function neighborCompare(a: NeighborRef, b: NeighborRef) {
  const byType = a.type.localeCompare(b.type, undefined, { sensitivity: 'base' })
  if (byType !== 0) return byType
  return a.label.localeCompare(b.label, undefined, { sensitivity: 'base' })
}

function groupNeighborsByType(refs: NeighborRef[]): { type: string; items: NeighborRef[] }[] {
  const byType = new Map<string, NeighborRef[]>()
  for (const ref of refs) {
    const list = byType.get(ref.type) ?? []
    list.push(ref)
    byType.set(ref.type, list)
  }
  return [...byType.entries()]
    .sort(([a], [b]) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
    .map(([type, items]) => ({
      type,
      items: [...items].sort((a, b) => a.label.localeCompare(b.label, undefined, { sensitivity: 'base' })),
    }))
}

/** Hover submenu — Mantine 7 has no Menu.Sub. */
function GraphMenuSub({
  label,
  leftSection,
  children,
}: {
  label: string
  leftSection?: ReactNode
  children: ReactNode
}) {
  return (
    <Menu trigger="hover" openDelay={0} closeDelay={120} position="right-start" offset={4} withinPortal={false}>
      <Menu.Target>
        <Menu.Item
          closeMenuOnClick={false}
          leftSection={leftSection}
          rightSection={<IconChevronRight size={14} />}
        >
          {label}
        </Menu.Item>
      </Menu.Target>
      <Menu.Dropdown>{children}</Menu.Dropdown>
    </Menu>
  )
}

function GraphNeighborItems({
  refs,
  onGoTo,
}: {
  refs: NeighborRef[]
  onGoTo: (id: string) => void
}) {
  return (
    <>
      {groupNeighborsByType(refs).map((group) => (
        <Fragment key={group.type}>
          <Menu.Label c="dimmed">{group.type}</Menu.Label>
          {group.items.map((item) => (
            <Menu.Item key={item.id} onClick={() => onGoTo(item.id)}>
              {item.label}
            </Menu.Item>
          ))}
        </Fragment>
      ))}
    </>
  )
}

type NodeBox = { id: string; position: { x: number; y: number }; width: number; height: number }

function closestHandles(source: NodeBox, target: NodeBox): { sourceHandle: string; targetHandle: string } {
  const sc = { x: source.position.x + source.width / 2, y: source.position.y + source.height / 2 }
  const tc = { x: target.position.x + target.width / 2, y: target.position.y + target.height / 2 }
  const dx = tc.x - sc.x
  const dy = tc.y - sc.y
  if (Math.abs(dx) >= Math.abs(dy)) {
    return dx >= 0
      ? { sourceHandle: 'right-source', targetHandle: 'left-target' }
      : { sourceHandle: 'left-source', targetHandle: 'right-target' }
  }
  return dy >= 0
    ? { sourceHandle: 'bottom-source', targetHandle: 'top-target' }
    : { sourceHandle: 'top-source', targetHandle: 'bottom-target' }
}

function withClosestHandles(edges: Edge[], nodes: Node<CardData>[]): Edge[] {
  const byId = new Map(nodes.map((n) => [n.id, n]))
  return edges.map((e) => {
    const source = byId.get(e.source)
    const target = byId.get(e.target)
    if (!source || !target) return e
    const handles = closestHandles(
      {
        id: source.id,
        position: source.position,
        width: nodeSize(source.data).width,
        height: nodeSize(source.data).height,
      },
      {
        id: target.id,
        position: target.position,
        width: nodeSize(target.data).width,
        height: nodeSize(target.data).height,
      },
    )
    return { ...e, ...handles }
  })
}

function layoutWithDagre(rfNodes: Node<CardData>[], rfEdges: Edge[], rankdir: GraphLayoutDir): Node<CardData>[] {
  const g = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}))
  g.setGraph({
    rankdir,
    nodesep: NODE_SEP,
    ranksep: RANK_SEP,
    edgesep: EDGE_SEP,
    marginx: 40,
    marginy: 40,
    acyclicer: 'greedy',
    ranker: 'network-simplex',
  })
  for (const n of rfNodes) {
    const { width, height } = nodeSize(n.data)
    g.setNode(n.id, { width, height })
  }
  for (const e of rfEdges) g.setEdge(e.source, e.target)
  dagre.layout(g)
  return rfNodes.map((n) => {
    const pos = g.node(n.id)
    const { width, height } = nodeSize(n.data)
    return {
      ...n,
      position: { x: (pos?.x ?? 0) - width / 2, y: (pos?.y ?? 0) - height / 2 },
    }
  })
}

function CanvasInner(
  {
    assets,
    relations,
    selectedAssetId,
    onSelectAsset,
    visible = true,
    viewMode = 'details',
    highlightedTypes,
    assetStatus,
    relationStatus,
    changesOnly = false,
  }: Props,
  ref: Ref<SbomGraphHandle>,
) {
  const { fitView, setCenter, getZoom } = useReactFlow()
  const layoutDir = useRef<GraphLayoutDir>('TB')
  const nodesRef = useRef<Node<CardData>[]>([])
  const laidOut = useRef(false)
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null)
  const [ctxMenu, setCtxMenu] = useState<GraphCtxMenu | null>(null)

  const skeletonCache = useRef<{ nodes: Node<CardData>[]; edges: Edge[] }>({ nodes: [], edges: [] })
  const visibleRef = useRef(visible)
  visibleRef.current = visible

  const assetById = useMemo(() => {
    const map = new Map<string, AssetView>()
    for (const a of assets) map.set(a.id, a)
    return map
  }, [assets])

  const neighborsByNode = useMemo(() => {
    const sources = new Map<string, NeighborRef[]>()
    const targets = new Map<string, NeighborRef[]>()
    const relationGroups = new Map<string, RelationGroup[]>()
    const idSet = new Set(assets.map((a) => a.id))

    const pushUnique = (list: NeighborRef[], ref: NeighborRef) => {
      if (!list.some((x) => x.id === ref.id)) list.push(ref)
    }

    type GroupAcc = { title: string; direction: 'IN' | 'OUT'; byId: Map<string, NeighborRef> }
    const groupsByNode = new Map<string, Map<string, GroupAcc>>()

    const touchGroup = (
      nodeId: string,
      role: string,
      title: string,
      direction: 'IN' | 'OUT',
      other: NeighborRef,
    ) => {
      let nodeGroups = groupsByNode.get(nodeId)
      if (!nodeGroups) {
        nodeGroups = new Map()
        groupsByNode.set(nodeId, nodeGroups)
      }
      const key = `${direction}:${role}`
      let acc = nodeGroups.get(key)
      if (!acc) {
        acc = { title, direction, byId: new Map() }
        nodeGroups.set(key, acc)
      }
      acc.byId.set(other.id, other)
    }

    for (const r of relations) {
      if (!idSet.has(r.fromAssetId) || !idSet.has(r.toAssetId)) continue
      const from = assetById.get(r.fromAssetId)
      const to = assetById.get(r.toAssetId)
      if (!from || !to) continue
      const fromRef: NeighborRef = { id: from.id, type: from.type, label: from.label }
      const toRef: NeighborRef = { id: to.id, type: to.type, label: to.label }
      const relTitle = r.label || r.role

      const srcList = sources.get(r.toAssetId) ?? []
      pushUnique(srcList, fromRef)
      sources.set(r.toAssetId, srcList)

      const tgtList = targets.get(r.fromAssetId) ?? []
      pushUnique(tgtList, toRef)
      targets.set(r.fromAssetId, tgtList)

      touchGroup(r.toAssetId, r.role, relTitle, 'IN', fromRef)
      touchGroup(r.fromAssetId, r.role, relTitle, 'OUT', toRef)
    }

    for (const [nodeId, list] of sources) {
      sources.set(nodeId, [...list].sort(neighborCompare))
    }
    for (const [nodeId, list] of targets) {
      targets.set(nodeId, [...list].sort(neighborCompare))
    }
    for (const [nodeId, nodeGroups] of groupsByNode) {
      const groups: RelationGroup[] = [...nodeGroups.entries()]
        .map(([key, acc]) => ({
          key,
          title: acc.title,
          direction: acc.direction,
          connected: [...acc.byId.values()].sort(neighborCompare),
        }))
        .sort((a, b) => {
          const byTitle = a.title.localeCompare(b.title, undefined, { sensitivity: 'base' })
          if (byTitle !== 0) return byTitle
          return a.direction.localeCompare(b.direction)
        })
      relationGroups.set(nodeId, groups)
    }

    return { sources, targets, relationGroups }
  }, [assets, relations, assetById])

  const skeleton = useMemo(() => {
    if (!visible) return skeletonCache.current
    const filtering = (highlightedTypes?.size ?? 0) > 0
    const typeById = new Map(assets.map((a) => [a.id, a.type]))
    const nodes: Node<CardData>[] = assets.map((a) => {
      const status = assetStatus?.get(a.id) ?? 'unchanged'
      const changed = status !== 'unchanged'
      const dimmed =
        (changesOnly && !changed) || (filtering && !highlightedTypes!.has(a.type) && !changed)
      return {
        id: a.id,
        type: 'asset',
        position: { x: 0, y: 0 },
        selected: false,
        data: {
          label: a.label,
          type: a.type,
          color: colorForType(a.type),
          emphasized: filtering && highlightedTypes!.has(a.type),
          dimmed,
          viewMode,
          payloadRows: payloadRows(a.payload),
          draftStatus: status,
        },
      }
    })
    const idSet = new Set(assets.map((a) => a.id))
    const edges: Edge[] = relations
      .filter((r) => idSet.has(r.fromAssetId) && idSet.has(r.toAssetId))
      .map((r) => {
        const status = relationStatus?.get(r.id) ?? 'unchanged'
        const changed = status !== 'unchanged'
        const dimmed =
          (changesOnly && !changed) ||
          (filtering &&
            (!highlightedTypes!.has(typeById.get(r.fromAssetId) ?? '') ||
              !highlightedTypes!.has(typeById.get(r.toAssetId) ?? '')) &&
            !changed)
        const stroke = draftStatusColor(status) ?? EDGE_COLOR
        return {
          id: r.id,
          source: r.fromAssetId,
          target: r.toAssetId,
          label: r.label,
          type: 'smoothstep',
          selectable: true,
          data: { dimmed, draftStatus: status },
          markerEnd: { type: MarkerType.ArrowClosed, color: stroke, width: 16, height: 16 },
          style: {
            stroke,
            strokeWidth: changed ? 3 : 2,
            strokeDasharray: status === 'deleted' ? '6 4' : undefined,
            opacity: dimmed ? DIMMED_OPACITY : 1,
          },
          labelStyle: {
            fontSize: 10,
            fontWeight: 700,
            fill: status === 'deleted' ? (draftStatusColor('deleted') ?? EDGE_COLOR) : '#212529',
            textDecoration: status === 'deleted' ? 'line-through' : undefined,
          },
          labelBgStyle: { fill: '#fff', fillOpacity: 0.95 },
        }
      })
    const next = { nodes, edges }
    skeletonCache.current = next
    return next
  }, [assets, relations, viewMode, highlightedTypes, visible, assetStatus, relationStatus, changesOnly])

  const [nodes, setNodes, onNodesChange] = useNodesState(skeleton.nodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(skeleton.edges)
  nodesRef.current = nodes

  const applyLayout = useCallback(
    (nextLayout: GraphLayoutDir = layoutDir.current) => {
      if (!visibleRef.current) return
      layoutDir.current = nextLayout
      const curr = nodesRef.current.length > 0 ? nodesRef.current : skeletonCache.current.nodes
      const eds = edges.length > 0 ? edges : skeletonCache.current.edges
      if (curr.length === 0) return
      const laid = layoutWithDagre(curr, eds, nextLayout)
      laidOut.current = true
      setNodes(laid)
      setEdges(withClosestHandles(eds, laid))
      requestAnimationFrame(() => {
        void fitView({ padding: 0.18, duration: 250 })
      })
    },
    [edges, fitView, setEdges, setNodes],
  )

  const focusNode = useCallback(
    (nodeId: string) => {
      if (!visibleRef.current) return
      const node = nodesRef.current.find((n) => n.id === nodeId)
      if (!node) return
      const { width, height } = nodeSize(node.data)
      const x = node.position.x + width / 2
      const y = node.position.y + height / 2
      const zoom = Math.max(getZoom(), 0.9)
      setCenter(x, y, { zoom, duration: 350 })
    },
    [getZoom, setCenter],
  )

  const goToNode = useCallback(
    (nodeId: string) => {
      setSelectedEdgeId(null)
      setCtxMenu(null)
      onSelectAsset(nodeId)
      requestAnimationFrame(() => focusNode(nodeId))
    },
    [focusNode, onSelectAsset],
  )

  useImperativeHandle(ref, () => ({ applyLayout, focusNode }), [applyLayout, focusNode])

  useEffect(() => {
    if (!visible) {
      laidOut.current = false
      return
    }
    const prevById = new Map(nodesRef.current.map((p) => [p.id, p]))
    const nextNodes = skeleton.nodes.map((n) => {
      const prev = prevById.get(n.id)
      const piled = !prev || (prev.position.x === 0 && prev.position.y === 0)
      return !piled && prev ? { ...n, position: prev.position } : n
    })
    const nextEdges = skeleton.edges
    const prevIds = new Set(prevById.keys())
    const nextIds = new Set(skeleton.nodes.map((n) => n.id))
    const membershipChanged =
      prevIds.size !== nextIds.size ||
      [...nextIds].some((id) => !prevIds.has(id)) ||
      [...prevIds].some((id) => !nextIds.has(id))
    const piled = nextNodes.every((n) => n.position.x === 0 && n.position.y === 0)
    if (!laidOut.current || piled || membershipChanged) {
      const laid = layoutWithDagre(nextNodes, nextEdges, layoutDir.current)
      setNodes(laid)
      setEdges(withClosestHandles(nextEdges, laid))
      laidOut.current = true
      requestAnimationFrame(() => {
        void fitView({ padding: 0.18, duration: 250 })
      })
    } else {
      setNodes(nextNodes)
      setEdges(withClosestHandles(nextEdges, nextNodes))
    }
  }, [visible, skeleton.nodes, skeleton.edges, fitView, setEdges, setNodes])

  const prevViewMode = useRef(viewMode)
  useEffect(() => {
    if (!visible) return
    if (prevViewMode.current === viewMode) return
    prevViewMode.current = viewMode
    laidOut.current = false
    applyLayout()
  }, [visible, viewMode, applyLayout])

  const wasVisible = useRef(false)
  useEffect(() => {
    if (!visible) {
      wasVisible.current = false
      return
    }
    if (wasVisible.current) return
    wasVisible.current = true
    const id = requestAnimationFrame(() => {
      requestAnimationFrame(() => applyLayout())
    })
    return () => cancelAnimationFrame(id)
  }, [visible, applyLayout])

  useEffect(() => {
    if (!visible) return
    if (selectedEdgeId && !skeleton.edges.some((e) => e.id === selectedEdgeId)) {
      setSelectedEdgeId(null)
    }
  }, [visible, selectedEdgeId, skeleton.edges])

  useEffect(() => {
    if (!visible) return
    setNodes((curr) =>
      curr.map((n) => ({
        ...n,
        selected: n.id === selectedAssetId,
      })),
    )
    setEdges((curr) =>
      curr.map((e) => {
        const edgeSelected = e.id === selectedEdgeId
        const incident = e.source === selectedAssetId || e.target === selectedAssetId
        const highlight = edgeSelected || incident
        const dimmed = Boolean((e.data as { dimmed?: boolean } | undefined)?.dimmed)
        const status = (e.data as { draftStatus?: DraftKind } | undefined)?.draftStatus ?? 'unchanged'
        const stroke = edgeSelected
          ? EDGE_SELECTED
          : incident
            ? EDGE_SELECTED
            : draftStatusColor(status) ?? EDGE_COLOR
        const opacity = highlight ? 1 : dimmed ? DIMMED_OPACITY : 1
        return {
          ...e,
          selected: edgeSelected,
          animated: highlight,
          style: {
            ...e.style,
            stroke,
            strokeWidth: edgeSelected ? 4 : highlight || status !== 'unchanged' ? 3 : 2,
            strokeDasharray: status === 'deleted' ? '6 4' : undefined,
            opacity,
          },
          markerEnd: {
            type: MarkerType.ArrowClosed,
            color: stroke,
            width: 16,
            height: 16,
          },
        }
      }),
    )
  }, [visible, selectedAssetId, selectedEdgeId, setEdges, setNodes])

  const closeCtx = useCallback(() => setCtxMenu(null), [])

  const onNodeClick = useCallback(
    (_: MouseEvent, node: Node) => {
      setSelectedEdgeId(null)
      setCtxMenu(null)
      onSelectAsset(node.id)
    },
    [onSelectAsset],
  )

  const onEdgeClick = useCallback((_: MouseEvent, edge: Edge) => {
    setCtxMenu(null)
    setSelectedEdgeId(edge.id)
  }, [])

  const onPaneClick = useCallback(() => {
    setSelectedEdgeId(null)
    setCtxMenu(null)
    onSelectAsset(null)
  }, [onSelectAsset])

  const onNodeContextMenu = useCallback(
    (event: MouseEvent, node: Node) => {
      event.preventDefault()
      event.stopPropagation()
      const hasSources = (neighborsByNode.sources.get(node.id)?.length ?? 0) > 0
      const hasTargets = (neighborsByNode.targets.get(node.id)?.length ?? 0) > 0
      if (!hasSources && !hasTargets) return
      setCtxMenu({ kind: 'node', nodeId: node.id, x: event.clientX, y: event.clientY })
    },
    [neighborsByNode],
  )

  const onEdgeContextMenu = useCallback((event: MouseEvent, edge: Edge) => {
    event.preventDefault()
    event.stopPropagation()
    setSelectedEdgeId(edge.id)
    setCtxMenu({
      kind: 'edge',
      edgeId: edge.id,
      sourceId: edge.source,
      targetId: edge.target,
      x: event.clientX,
      y: event.clientY,
    })
  }, [])

  const nodeSources =
    ctxMenu?.kind === 'node' ? (neighborsByNode.sources.get(ctxMenu.nodeId) ?? []) : []
  const nodeTargets =
    ctxMenu?.kind === 'node' ? (neighborsByNode.targets.get(ctxMenu.nodeId) ?? []) : []
  const nodeRelationGroups =
    ctxMenu?.kind === 'node' ? (neighborsByNode.relationGroups.get(ctxMenu.nodeId) ?? []) : []
  const showGoTo = nodeSources.length > 0 || nodeTargets.length > 0
  const showRelations = nodeRelationGroups.length > 0

  return (
    <>
      <ReactFlow
        style={{ width: '100%', height: '100%' }}
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={onNodeClick}
        onEdgeClick={onEdgeClick}
        onPaneClick={onPaneClick}
        onNodeContextMenu={onNodeContextMenu}
        onEdgeContextMenu={onEdgeContextMenu}
        onInit={() => applyLayout()}
        nodesDraggable={false}
        nodesConnectable={false}
        edgesFocusable
        elementsSelectable
        minZoom={0.2}
        proOptions={{ hideAttribution: true }}
        onMoveStart={closeCtx}
      >
        <Background />
        <Controls />
        <MiniMap />
      </ReactFlow>
      <Menu
        opened={ctxMenu != null}
        onChange={(opened) => {
          if (!opened) closeCtx()
        }}
        position="bottom-start"
        offset={0}
        withinPortal
      >
        <Menu.Target>
          <div
            style={{
              position: 'fixed',
              left: ctxMenu?.x ?? 0,
              top: ctxMenu?.y ?? 0,
              width: 1,
              height: 1,
              pointerEvents: 'none',
            }}
          />
        </Menu.Target>
        <Menu.Dropdown>
          {ctxMenu?.kind === 'edge' && (
            <GraphMenuSub label="Go to…">
              {(() => {
                const source = assetById.get(ctxMenu.sourceId)
                const target = assetById.get(ctxMenu.targetId)
                return (
                  <>
                    <Menu.Item onClick={() => goToNode(ctxMenu.sourceId)}>
                      {source
                        ? `Source ${graphAssetCaption(source.type, source.label)}`
                        : `Source ${ctxMenu.sourceId}`}
                    </Menu.Item>
                    <Menu.Item onClick={() => goToNode(ctxMenu.targetId)}>
                      {target
                        ? `Target ${graphAssetCaption(target.type, target.label)}`
                        : `Target ${ctxMenu.targetId}`}
                    </Menu.Item>
                  </>
                )
              })()}
            </GraphMenuSub>
          )}
          {ctxMenu?.kind === 'node' && (
            <>
              {nodeRelationGroups.map((group) => (
                <GraphMenuSub
                  key={group.key}
                  label={group.title}
                  leftSection={
                    group.direction === 'IN' ? (
                      <IconArrowNarrowDown size={14} />
                    ) : (
                      <IconArrowNarrowUp size={14} />
                    )
                  }
                >
                  <GraphNeighborItems refs={group.connected} onGoTo={goToNode} />
                </GraphMenuSub>
              ))}
              {showGoTo && showRelations && <Menu.Divider />}
              {nodeSources.length > 0 && (
                <GraphMenuSub label="Go to source…">
                  <GraphNeighborItems refs={nodeSources} onGoTo={goToNode} />
                </GraphMenuSub>
              )}
              {nodeTargets.length > 0 && (
                <GraphMenuSub label="Go to target…">
                  <GraphNeighborItems refs={nodeTargets} onGoTo={goToNode} />
                </GraphMenuSub>
              )}
            </>
          )}
        </Menu.Dropdown>
      </Menu>
    </>
  )
}

const Inner = forwardRef(CanvasInner)

export const SbomGraphCanvas = forwardRef<SbomGraphHandle, Props>(function SbomGraphCanvas(props, ref) {
  return (
    <div style={{ width: '100%', height: '100%' }}>
      <ReactFlowProvider>
        <Inner {...props} ref={ref} />
      </ReactFlowProvider>
    </div>
  )
})
