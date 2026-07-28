import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
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
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { EntityCardNode, type EntityCardData } from './EntityCardNode'
import type { GraphLink, GraphNode, GraphSelection } from './types'

type Props = {
  nodes: GraphNode[]
  links: GraphLink[]
  selection: GraphSelection | null
  onSelect: (selection: GraphSelection | null) => void
}

export type GraphCanvasHandle = {
  applyLayout: () => void
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
): Node<EntityCardData>[] {
  const g = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}))
  g.setGraph({
    rankdir: 'TB',
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
    position: { x: (i % 4) * (NODE_W + 40), y: Math.floor(i / 4) * (NODE_H + 40) },
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
        animated: selected,
        style: edgeStyle(selected),
        labelStyle: {
          fontSize: 10,
          fill: selected ? EDGE_SELECTED : '#212529',
          fontWeight: 700,
        },
        labelBgStyle: { fill: '#fff', fillOpacity: 0.95 },
        labelBgPadding: [4, 2] as [number, number],
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: selected ? EDGE_SELECTED : EDGE_COLOR,
          width: selected ? 18 : 16,
          height: selected ? 18 : 16,
        },
      }
    })

  return { nodes, edges }
}

function GraphCanvasInner(
  { nodes: entities, links, selection, onSelect }: Props,
  ref: React.Ref<GraphCanvasHandle>,
) {
  const { fitView } = useReactFlow()
  const initial = useMemo(
    () => toFlowElements(entities, links, selection),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [entities, links],
  )
  const [nodes, setNodes, onNodesChange] = useNodesState(initial.nodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initial.edges)

  useEffect(() => {
    const next = toFlowElements(entities, links, selection)
    const laidOut = layoutWithDagre(next.nodes, next.edges)
    setNodes(laidOut)
    setEdges(next.edges)
    requestAnimationFrame(() => fitView({ padding: 0.15, duration: 300 }))
    // selection applied in separate effect; avoid re-layout on every click
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entities, links, setNodes, setEdges, fitView])

  useEffect(() => {
    const selectedNodeId = selection?.kind === 'node' ? selection.node.id : null
    const selectedEdgeId = selection?.kind === 'edge' ? selection.edge.id : null

    setNodes((curr) =>
      curr.map((n) => ({
        ...n,
        selected: n.id === selectedNodeId,
        data: { ...n.data, selected: n.id === selectedNodeId },
      })),
    )
    setEdges((curr) =>
      curr.map((e) => {
        const selected = e.id === selectedEdgeId
        return {
          ...e,
          selected,
          animated: selected,
          style: edgeStyle(selected),
          labelStyle: {
            fontSize: 10,
            fill: selected ? EDGE_SELECTED : '#212529',
            fontWeight: 700,
          },
          markerEnd: {
            type: MarkerType.ArrowClosed,
            color: selected ? EDGE_SELECTED : EDGE_COLOR,
            width: selected ? 18 : 16,
            height: selected ? 18 : 16,
          },
        }
      }),
    )
  }, [selection, setNodes, setEdges])

  const applyLayout = useCallback(() => {
    setNodes((curr) => layoutWithDagre(curr, edges))
    requestAnimationFrame(() => fitView({ padding: 0.15, duration: 400 }))
  }, [edges, fitView, setNodes])

  useImperativeHandle(ref, () => ({ applyLayout }), [applyLayout])

  return (
    <ReactFlow
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
      onNodeClick={(_, node) =>
        onSelect({ kind: 'node', node: (node.data as EntityCardData).entity })
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
    <ReactFlowProvider>
      <GraphCanvasForward {...props} ref={ref} />
    </ReactFlowProvider>
  )
})
