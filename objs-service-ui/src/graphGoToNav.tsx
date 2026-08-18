import { Fragment, type ReactNode } from 'react'
import { Menu } from '@mantine/core'
import { IconArrowNarrowDown, IconArrowNarrowUp, IconChevronRight } from '@tabler/icons-react'
import type { GraphLink, GraphNode } from './types'

export type GraphNeighborRef = { id: string; type: string; label: string }

export type GraphRelationGroup = {
  key: string
  title: string
  direction: 'IN' | 'OUT'
  connected: GraphNeighborRef[]
}

export type GraphNeighborIndex = {
  sources: Map<string, GraphNeighborRef[]>
  targets: Map<string, GraphNeighborRef[]>
  relationGroups: Map<string, GraphRelationGroup[]>
}

function neighborCompare(a: GraphNeighborRef, b: GraphNeighborRef): number {
  const byLabel = a.label.localeCompare(b.label, undefined, { sensitivity: 'base' })
  if (byLabel !== 0) return byLabel
  return a.id.localeCompare(b.id)
}

function pushUnique(list: GraphNeighborRef[], ref: GraphNeighborRef) {
  if (!list.some((x) => x.id === ref.id)) list.push(ref)
}

/** Incoming/outgoing neighbors and role groups for instance graphs (Explorer, Composer, Query). */
export function buildGraphNeighborIndex(
  nodes: GraphNode[],
  links: GraphLink[],
): GraphNeighborIndex {
  const sources = new Map<string, GraphNeighborRef[]>()
  const targets = new Map<string, GraphNeighborRef[]>()
  const idSet = new Set(nodes.map((n) => n.id))
  const byId = new Map(nodes.map((n) => [n.id, n]))

  type GroupAcc = { title: string; direction: 'IN' | 'OUT'; byId: Map<string, GraphNeighborRef> }
  const groupsByNode = new Map<string, Map<string, GroupAcc>>()

  const toRef = (node: GraphNode): GraphNeighborRef => ({
    id: node.id,
    type: node.type,
    label: node.name,
  })

  const touchGroup = (
    nodeId: string,
    role: string,
    direction: 'IN' | 'OUT',
    other: GraphNeighborRef,
  ) => {
    let nodeGroups = groupsByNode.get(nodeId)
    if (!nodeGroups) {
      nodeGroups = new Map()
      groupsByNode.set(nodeId, nodeGroups)
    }
    const key = `${direction}:${role}`
    let acc = nodeGroups.get(key)
    if (!acc) {
      acc = { title: role, direction, byId: new Map() }
      nodeGroups.set(key, acc)
    }
    acc.byId.set(other.id, other)
  }

  for (const link of links) {
    if (!idSet.has(link.source) || !idSet.has(link.target)) continue
    const from = byId.get(link.source)
    const to = byId.get(link.target)
    if (!from || !to) continue
    const fromRef = toRef(from)
    const toRefNode = toRef(to)
    const role = link.role || 'related'

    const srcList = sources.get(link.target) ?? []
    pushUnique(srcList, fromRef)
    sources.set(link.target, srcList)

    const tgtList = targets.get(link.source) ?? []
    pushUnique(tgtList, toRefNode)
    targets.set(link.source, tgtList)

    touchGroup(link.target, role, 'IN', fromRef)
    touchGroup(link.source, role, 'OUT', toRefNode)
  }

  for (const [nodeId, list] of sources) {
    sources.set(nodeId, [...list].sort(neighborCompare))
  }
  for (const [nodeId, list] of targets) {
    targets.set(nodeId, [...list].sort(neighborCompare))
  }

  const relationGroups = new Map<string, GraphRelationGroup[]>()
  for (const [nodeId, nodeGroups] of groupsByNode) {
    const groups: GraphRelationGroup[] = [...nodeGroups.entries()]
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
}

function groupNeighborsByType(refs: GraphNeighborRef[]): { type: string; items: GraphNeighborRef[] }[] {
  const byType = new Map<string, GraphNeighborRef[]>()
  for (const ref of refs) {
    const list = byType.get(ref.type) ?? []
    list.push(ref)
    byType.set(ref.type, list)
  }
  return [...byType.entries()]
    .sort(([a], [b]) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
    .map(([type, items]) => ({ type, items }))
}

/** Hover submenu — Mantine 7 has no Menu.Sub. */
export function GraphMenuSub({
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
  refs: GraphNeighborRef[]
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

export type GraphGoToTarget =
  | { kind: 'node'; nodeId: string }
  | { kind: 'edge'; sourceId: string; targetId: string }

function nodeCaption(nodes: GraphNode[], id: string): string {
  const node = nodes.find((n) => n.id === id)
  return node ? `${node.name} (${node.type})` : id
}

export function graphGoToAvailable(index: GraphNeighborIndex, target: GraphGoToTarget): boolean {
  if (target.kind === 'edge') return true
  const sources = index.sources.get(target.nodeId) ?? []
  const targets = index.targets.get(target.nodeId) ?? []
  return sources.length > 0 || targets.length > 0
}

/**
 * SBOM-style graph navigation. When [wrap] is true, nest under a last **Go to…** item
 * (for menus that already have Actions). When false, emit the same items at the top level.
 */
export function GraphGoToMenuItems({
  target,
  nodes,
  index,
  wrap = true,
  onGoTo,
}: {
  target: GraphGoToTarget
  nodes: GraphNode[]
  index: GraphNeighborIndex
  wrap?: boolean
  onGoTo: (id: string) => void
}) {
  const inner =
    target.kind === 'edge' ? (
      <>
        <Menu.Item onClick={() => onGoTo(target.sourceId)}>
          Source {nodeCaption(nodes, target.sourceId)}
        </Menu.Item>
        <Menu.Item onClick={() => onGoTo(target.targetId)}>
          Target {nodeCaption(nodes, target.targetId)}
        </Menu.Item>
      </>
    ) : (
      <NodeGoToItems nodeId={target.nodeId} index={index} onGoTo={onGoTo} />
    )

  if (!wrap) return inner
  return <GraphMenuSub label="Go to…">{inner}</GraphMenuSub>
}

/** Standalone context menu when the graph has no existing Actions menu. */
export function GraphGoToContextMenu({
  opened,
  x,
  y,
  onClose,
  target,
  nodes,
  index,
  onGoTo,
}: {
  opened: boolean
  x: number
  y: number
  onClose: () => void
  target: GraphGoToTarget | null
  nodes: GraphNode[]
  index: GraphNeighborIndex
  onGoTo: (id: string) => void
}) {
  const show = opened && target != null && graphGoToAvailable(index, target)
  return (
    <Menu
      opened={show}
      onChange={(next) => {
        if (!next) onClose()
      }}
      position="bottom-start"
      offset={0}
      withinPortal
      shadow="md"
    >
      <Menu.Target>
        <div
          style={{
            position: 'fixed',
            left: x,
            top: y,
            width: 1,
            height: 1,
            pointerEvents: 'none',
          }}
        />
      </Menu.Target>
      <Menu.Dropdown>
        {target && (
          <GraphGoToMenuItems
            target={target}
            nodes={nodes}
            index={index}
            wrap={false}
            onGoTo={(id) => {
              onClose()
              onGoTo(id)
            }}
          />
        )}
      </Menu.Dropdown>
    </Menu>
  )
}

function NodeGoToItems({
  nodeId,
  index,
  onGoTo,
}: {
  nodeId: string
  index: GraphNeighborIndex
  onGoTo: (id: string) => void
}) {
  const nodeSources = index.sources.get(nodeId) ?? []
  const nodeTargets = index.targets.get(nodeId) ?? []
  const nodeRelationGroups = index.relationGroups.get(nodeId) ?? []
  const showGoTo = nodeSources.length > 0 || nodeTargets.length > 0
  const showRelations = nodeRelationGroups.length > 0
  return (
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
          <GraphNeighborItems refs={group.connected} onGoTo={onGoTo} />
        </GraphMenuSub>
      ))}
      {showGoTo && showRelations && <Menu.Divider />}
      {nodeSources.length > 0 && (
        <GraphMenuSub label="Go to source…">
          <GraphNeighborItems refs={nodeSources} onGoTo={onGoTo} />
        </GraphMenuSub>
      )}
      {nodeTargets.length > 0 && (
        <GraphMenuSub label="Go to target…">
          <GraphNeighborItems refs={nodeTargets} onGoTo={onGoTo} />
        </GraphMenuSub>
      )}
    </>
  )
}
