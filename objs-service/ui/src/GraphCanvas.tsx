import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
} from 'react'
import dagre from '@dagrejs/dagre'
import {
  Background,
  Controls,
  MarkerType,
  MiniMap,
  ReactFlow,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useReactFlow,
  type Edge,
  type Node,
  type XYPosition,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { EntityCardNode, type EntityCardData } from './EntityCardNode'
import type { GraphLink, GraphNode, GraphSelection } from './types'

export type GraphNodePositions = Record<string, { x: number; y: number }>

type Props = {
  nodes: GraphNode[]
  links: GraphLink[]
  selection: GraphSelection | null
  onSelect: (selection: GraphSelection | null, meta?: { additive?: boolean }) => void
  layout: GraphLayout
  /**
   * When true (default), re-run dagre whenever entities/links/layout change.
   * When false, keep node positions across data edits; only place new nodes and
   * re-layout on explicit [GraphCanvasHandle.applyLayout] or a full graph replace.
   */
  autoLayoutOnDataChange?: boolean
  /** Extra node ids to highlight (e.g. connect pair). Merged with selection. */
  highlightedNodeIds?: string[]
  /** Fired after drag or layout with current canvas coordinates (for session restore). */
  onPositionsChange?: (positions: GraphNodePositions) => void
}

export type GraphLayout = 'TB' | 'LR' | 'BT' | 'RL'

export type GraphCanvasHandle = {
  applyLayout: (layout?: GraphLayout) => void
  /** Pan/zoom so [nodeId] is centered in the viewport. */
  focusNode: (nodeId: string) => void
}

function entitiesHavePositions(entities: GraphNode[]): boolean {
  return (
    entities.length > 0 &&
    entities.every((e) => typeof e.x === 'number' && typeof e.y === 'number')
  )
}

function positionsFromNodes(rfNodes: Node<EntityCardData>[]): GraphNodePositions {
  return Object.fromEntries(rfNodes.map((n) => [n.id, { x: n.position.x, y: n.position.y }]))
}

const NODE_W = 180
const NODE_H = 110
const EDGE_COLOR = '#495057'
const EDGE_SELECTED = '#228be6'
const nodeTypes = { entityCard: EntityCardNode }

type EdgeData = { edge: GraphLink }

function layoutWithDagre(
  rfNodes: Node<EntityCardData>[],
  rfEdges: Edge[],
  layout: GraphLayout,
): Node<EntityCardData>[] {
  const g = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}))
  g.setGraph({
    rankdir: layout,
    nodesep: 48,
    ranksep: 72,
    marginx: 24,
    marginy: 24,
  })

  for (const n of rfNodes) {
    g.setNode(n.id, { width: NODE_W, height: NODE_H })
  }
  for (const e of rfEdges) {
    g.setEdge(e.source, e.target)
  }
  dagre.layout(g)

  return rfNodes.map((n) => {
    const pos = g.node(n.id)
    return {
      ...n,
      position: {
        x: (pos?.x ?? 0) - NODE_W / 2,
        y: (pos?.y ?? 0) - NODE_H / 2,
      },
    }
  })
}

function edgeStyle(selected: boolean): Edge['style'] {
  return {
    stroke: selected ? EDGE_SELECTED : EDGE_COLOR,
    strokeWidth: selected ? 4 : 2,
  }
}

function styleForDraftEdge(
  draftStatus: GraphLink['draftStatus'],
  selected: boolean,
  dimmed = false,
): Pick<Edge<EdgeData>, 'style' | 'labelStyle' | 'markerEnd' | 'animated'> {
  const deleted = draftStatus === 'deleted'
  const modified = draftStatus === 'modified'
  const isNew = draftStatus === 'new'
  const stroke = selected
    ? EDGE_SELECTED
    : deleted
      ? '#fa5252'
      : modified
        ? '#fd7e14'
        : isNew
          ? '#40c057'
          : EDGE_COLOR
  return {
    animated: selected,
    style: {
      ...edgeStyle(selected),
      stroke,
      strokeWidth: selected ? 4 : deleted || modified || isNew ? 3 : 2,
      strokeDasharray: deleted ? '6 4' : undefined,
      opacity: dimmed ? 0.25 : deleted ? 0.7 : 1,
    },
    labelStyle: {
      fontSize: 10,
      fill: selected ? EDGE_SELECTED : deleted ? '#fa5252' : '#212529',
      fontWeight: 700,
      textDecoration: deleted ? 'line-through' : undefined,
      opacity: dimmed ? 0.25 : 1,
    },
    markerEnd: {
      type: MarkerType.ArrowClosed,
      color: stroke,
      width: selected ? 18 : 16,
      height: selected ? 18 : 16,
    },
  }
}

function toFlowElements(
  entities: GraphNode[],
  links: GraphLink[],
  selection: GraphSelection | null,
): { nodes: Node<EntityCardData>[]; edges: Edge<EdgeData>[] } {
  const selectedNodeId = selection?.kind === 'node' ? selection.node.id : null
  const selectedEdgeId = selection?.kind === 'edge' ? selection.edge.id : null

  const nodes: Node<EntityCardData>[] = entities.map((entity, i) => ({
    id: entity.id,
    type: 'entityCard',
    position:
      typeof entity.x === 'number' && typeof entity.y === 'number'
        ? { x: entity.x, y: entity.y }
        : { x: (i % 4) * (NODE_W + 40), y: Math.floor(i / 4) * (NODE_H + 40) },
    selected: entity.id === selectedNodeId,
    data: { entity, selected: entity.id === selectedNodeId },
    style: { width: NODE_W },
  }))

  const idSet = new Set(entities.map((e) => e.id))
  const edges: Edge<EdgeData>[] = links
    .filter((l) => idSet.has(l.source) && idSet.has(l.target))
    .map((l) => {
      const selected = l.id === selectedEdgeId
      return {
        id: l.id,
        source: l.source,
        target: l.target,
        label: l.role,
        type: 'smoothstep',
        selected,
        data: { edge: l },
        labelBgStyle: { fill: '#fff', fillOpacity: 0.95 },
        labelBgPadding: [4, 2] as [number, number],
        ...styleForDraftEdge(l.draftStatus, selected, l.dimmed === true),
      }
    })

  return { nodes, edges }
}

function positionForNewNode(
  nodeId: string,
  index: number,
  links: GraphLink[],
  posById: Map<string, XYPosition>,
): XYPosition {
  const fromSource = links.find((l) => l.target === nodeId && posById.has(l.source))
  if (fromSource) {
    const src = posById.get(fromSource.source)!
    return { x: src.x + NODE_W + 48, y: src.y }
  }
  const toTarget = links.find((l) => l.source === nodeId && posById.has(l.target))
  if (toTarget) {
    const tgt = posById.get(toTarget.target)!
    return { x: tgt.x - NODE_W - 48, y: tgt.y }
  }
  if (posById.size > 0) {
    let maxX = 0
    let y = 0
    for (const p of posById.values()) {
      if (p.x >= maxX) {
        maxX = p.x
        y = p.y
      }
    }
    return { x: maxX + NODE_W + 48, y }
  }
  return { x: (index % 4) * (NODE_W + 40), y: Math.floor(index / 4) * (NODE_H + 40) }
}

function mergePreservingPositions(
  nextNodes: Node<EntityCardData>[],
  links: GraphLink[],
  prevNodes: Node<EntityCardData>[],
): Node<EntityCardData>[] {
  const posById = new Map(prevNodes.map((n) => [n.id, n.position]))
  const overlap = nextNodes.some((n) => posById.has(n.id))
  if (!overlap && nextNodes.length > 0) {
    return nextNodes
  }
  return nextNodes.map((n, index) => {
    const prev = posById.get(n.id)
    if (prev) {
      return { ...n, position: prev }
    }
    return { ...n, position: positionForNewNode(n.id, index, links, posById) }
  })
}

function GraphCanvasInner(
  {
    nodes: entities,
    links,
    selection,
    onSelect,
    layout,
    autoLayoutOnDataChange = true,
    highlightedNodeIds,
    onPositionsChange,
  }: Props,
  ref: React.Ref<GraphCanvasHandle>,
) {
  const { fitView, setCenter, getZoom } = useReactFlow()
  const initial = useMemo(
    () => toFlowElements(entities, links, selection),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [entities, links],
  )
  const restored = entitiesHavePositions(entities)
  const [nodes, setNodes, onNodesChange] = useNodesState(
    autoLayoutOnDataChange && !restored
      ? layoutWithDagre(initial.nodes, initial.edges, layout)
      : initial.nodes,
  )
  const [edges, setEdges, onEdgesChange] = useEdgesState(initial.edges)
  const laidOutOnce = useRef(autoLayoutOnDataChange || restored)
  const nodesRef = useRef(nodes)
  nodesRef.current = nodes
  const onPositionsChangeRef = useRef(onPositionsChange)
  onPositionsChangeRef.current = onPositionsChange

  const emitPositions = useCallback((rfNodes: Node<EntityCardData>[]) => {
    onPositionsChangeRef.current?.(positionsFromNodes(rfNodes))
  }, [])

  useEffect(() => {
    const next = toFlowElements(entities, links, selection)
    const shouldAutoLayout = autoLayoutOnDataChange
    const hasPositions = entitiesHavePositions(entities)
    let laidOut: Node<EntityCardData>[] | null = null
    setNodes((curr) => {
      if (hasPositions) {
        laidOutOnce.current = true
        // Prefer live canvas coordinates when the graph is already mounted so a
        // parent re-render (e.g. mid applyLayout) cannot wipe a fresher layout.
        if (curr.length > 0 && next.nodes.some((n) => curr.some((c) => c.id === n.id))) {
          return mergePreservingPositions(next.nodes, links, curr)
        }
        return next.nodes
      }
      if (shouldAutoLayout) {
        laidOutOnce.current = true
        laidOut = layoutWithDagre(next.nodes, next.edges, layout)
        return laidOut
      }
      const overlap = next.nodes.some((n) => curr.some((c) => c.id === n.id))
      if (!laidOutOnce.current || (!overlap && next.nodes.length > 0)) {
        laidOutOnce.current = true
        laidOut = layoutWithDagre(next.nodes, next.edges, layout)
        return laidOut
      }
      return mergePreservingPositions(next.nodes, links, curr)
    })
    setEdges(next.edges)
    if (laidOut || shouldAutoLayout) {
      const snapshot = laidOut
      requestAnimationFrame(() => {
        if (snapshot) emitPositions(snapshot)
        fitView({ padding: 0.15, duration: 300 })
      })
    }
    // selection applied in separate effect; avoid re-layout on every click
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entities, links, layout, autoLayoutOnDataChange, setNodes, setEdges, fitView, emitPositions])

  useEffect(() => {
    const selectedFromFocus = selection?.kind === 'node' ? selection.node.id : null
    const selectedIds = new Set<string>([
      ...(highlightedNodeIds ?? []),
      ...(selectedFromFocus ? [selectedFromFocus] : []),
    ])
    const selectedEdgeId = selection?.kind === 'edge' ? selection.edge.id : null

    setNodes((curr) =>
      curr.map((n) => {
        const selected = selectedIds.has(n.id)
        return {
          ...n,
          selected,
          data: { ...n.data, selected },
        }
      }),
    )
    setEdges((curr) =>
      curr.map((e) => {
        const selected = e.id === selectedEdgeId
        const draftStatus = (e.data as EdgeData | undefined)?.edge?.draftStatus
        const dimmed = (e.data as EdgeData | undefined)?.edge?.dimmed === true
        return {
          ...e,
          selected,
          ...styleForDraftEdge(draftStatus, selected, dimmed),
        }
      }),
    )
  }, [selection, highlightedNodeIds, setNodes, setEdges])

  const applyLayout = useCallback(
    (nextLayout: GraphLayout = layout) => {
      setNodes((curr) => {
        const next = layoutWithDagre(curr, edges, nextLayout)
        requestAnimationFrame(() => {
          emitPositions(next)
          fitView({ padding: 0.15, duration: 400 })
        })
        return next
      })
      laidOutOnce.current = true
    },
    [edges, emitPositions, fitView, layout, setNodes],
  )

  const focusNode = useCallback(
    (nodeId: string) => {
      const node = nodesRef.current.find((n) => n.id === nodeId)
      if (!node) return
      const width = node.measured?.width ?? NODE_W
      const height = node.measured?.height ?? NODE_H
      const x = node.position.x + width / 2
      const y = node.position.y + height / 2
      const zoom = Math.max(getZoom(), 0.85)
      setCenter(x, y, { zoom, duration: 400 })
    },
    [getZoom, setCenter],
  )

  useImperativeHandle(ref, () => ({ applyLayout, focusNode }), [applyLayout, focusNode])

  return (
    <ReactFlow
      style={{ width: '100%', height: '100%' }}
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      nodeTypes={nodeTypes}
      fitView
      fitViewOptions={{ padding: 0.15 }}
      minZoom={0.15}
      maxZoom={1.5}
      edgesFocusable
      elementsSelectable
      nodesDraggable
      onNodeDragStop={(_event, _node, dragged) => emitPositions(dragged)}
      onNodeClick={(event, node) =>
        onSelect(
          { kind: 'node', node: (node.data as EntityCardData).entity },
          { additive: event.ctrlKey || event.metaKey },
        )
      }
      onEdgeClick={(_, edge) => {
        const data = edge.data as EdgeData | undefined
        if (data?.edge) onSelect({ kind: 'edge', edge: data.edge })
      }}
      onPaneClick={() => onSelect(null)}
      proOptions={{ hideAttribution: true }}
    >
      <Background gap={16} size={1} />
      <Controls showInteractive={false} />
      <MiniMap zoomable pannable />
    </ReactFlow>
  )
}

const GraphCanvasForward = forwardRef(GraphCanvasInner)

export const GraphCanvas = forwardRef<GraphCanvasHandle, Props>(function GraphCanvas(props, ref) {
  return (
    <div style={{ width: '100%', height: '100%', minHeight: 0 }}>
      <ReactFlowProvider>
        <GraphCanvasForward {...props} ref={ref} />
      </ReactFlowProvider>
    </div>
  )
})
