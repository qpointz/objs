import dagre from '@dagrejs/dagre'
import type { Edge, Node } from '@xyflow/react'
import { MarkerType, Position } from '@xyflow/react'
import { parseAllDocuments } from 'yaml'
import { allowedEdgeKey } from './allowedEdgeRef'
import { allowedEdgeLabel } from './SchemaRelationshipGraph'
import type { BoMAllowedEdgeRule } from './types'

export type CatalogTypeNode = {
  type: string
  version: string
}

export type CatalogNodeData = {
  kind: 'entity' | 'wildcard'
  type: string
  version?: string
}

const NODE_WIDTH = 168
const NODE_HEIGHT = 64

/** Build React Flow elements for the full-catalog ontology graph (dagre LR, orthogonal edges). */
export function schemaCatalogElements(
  entityTypes: CatalogTypeNode[],
  rules: BoMAllowedEdgeRule[],
): { nodes: Node[]; edges: Edge[] } {
  const needsWildcard = rules.some((r) => r.sourceType === '*' || r.targetType === '*')
  const sorted = [...entityTypes].sort((a, b) => a.type.localeCompare(b.type))
  const typesForLayout = needsWildcard
    ? [...sorted, { type: '*', version: '' }]
    : sorted

  const baseNodes: Node[] = typesForLayout.map((entry) => {
    const isWild = entry.type === '*'
    return {
      id: `type:${entry.type}`,
      type: 'catalogType',
      position: { x: 0, y: 0 },
      sourcePosition: Position.Right,
      targetPosition: Position.Left,
      data: {
        kind: isWild ? 'wildcard' : 'entity',
        type: entry.type,
        version: isWild ? undefined : entry.version,
      } satisfies CatalogNodeData,
      style: { width: NODE_WIDTH },
    }
  })

  const typeIds = new Set(baseNodes.map((node) => node.id))
  const edges: Edge[] = []
  rules.forEach((rule, index) => {
    const sourceId = `type:${rule.sourceType}`
    const targetId = `type:${rule.targetType}`
    if (!typeIds.has(sourceId) || !typeIds.has(targetId)) return
    edges.push({
      id: `rule:${allowedEdgeKey(rule)}:${index}`,
      source: sourceId,
      target: targetId,
      label: allowedEdgeLabel(rule.role, rule.cardinality),
      type: 'step',
      markerEnd: { type: MarkerType.ArrowClosed, color: '#5c7cfa', width: 14, height: 14 },
      style: { stroke: '#5c7cfa', strokeWidth: 1.5 },
      labelStyle: { fontSize: 10, fontWeight: 600, fill: '#364fc7' },
      labelBgStyle: { fill: '#fff', fillOpacity: 0.9 },
      data: rule,
    })
  })

  const g = new dagre.graphlib.Graph({ multigraph: true }).setDefaultEdgeLabel(() => ({}))
  g.setGraph({
    rankdir: 'LR',
    nodesep: 56,
    ranksep: 100,
    edgesep: 28,
    marginx: 32,
    marginy: 32,
  })

  for (const node of baseNodes) {
    g.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT })
  }
  for (const edge of edges) {
    g.setEdge(edge.source, edge.target, {}, edge.id)
  }
  dagre.layout(g)

  const nodes = baseNodes.map((node) => {
    const pos = g.node(node.id)
    return {
      ...node,
      position: {
        x: (pos?.x ?? 0) - NODE_WIDTH / 2,
        y: (pos?.y ?? 0) - NODE_HEIGHT / 2,
      },
    }
  })

  return { nodes, edges }
}

/** True when multi-document YAML includes any Graph seed kind. */
export function catalogSeedContainsGraph(yamlText: string): boolean {
  try {
    const docs = parseAllDocuments(yamlText)
    for (const doc of docs) {
      if (doc.errors.length > 0) continue
      const value = doc.toJSON()
      if (value && typeof value === 'object' && !Array.isArray(value)) {
        const kind = (value as Record<string, unknown>).kind
        if (kind === 'Graph') return true
      }
    }
    return false
  } catch {
    return false
  }
}
