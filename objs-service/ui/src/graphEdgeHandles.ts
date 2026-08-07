/** Side of a node card used for edge attachment. */
export type NodeSide = 'top' | 'bottom' | 'left' | 'right'

export type NodeBox = {
  id: string
  position: { x: number; y: number }
  width: number
  height: number
}

export function sourceHandleId(side: NodeSide): string {
  return `${side}-source`
}

export function targetHandleId(side: NodeSide): string {
  return `${side}-target`
}

function centerOf(box: NodeBox): { x: number; y: number } {
  return {
    x: box.position.x + box.width / 2,
    y: box.position.y + box.height / 2,
  }
}

/**
 * Pick the source/target sides that face each other based on node centers.
 * Avoids fixed bottom→top attachments that look broken for LR/side placements.
 */
export function closestHandleSides(
  source: NodeBox,
  target: NodeBox,
): { source: NodeSide; target: NodeSide } {
  if (source.id === target.id) {
    return { source: 'right', target: 'left' }
  }
  const sc = centerOf(source)
  const tc = centerOf(target)
  const dx = tc.x - sc.x
  const dy = tc.y - sc.y

  if (Math.abs(dx) >= Math.abs(dy)) {
    return dx >= 0
      ? { source: 'right', target: 'left' }
      : { source: 'left', target: 'right' }
  }
  return dy >= 0
    ? { source: 'bottom', target: 'top' }
    : { source: 'top', target: 'bottom' }
}

export function closestHandleIds(
  source: NodeBox,
  target: NodeBox,
): { sourceHandle: string; targetHandle: string } {
  const sides = closestHandleSides(source, target)
  return {
    sourceHandle: sourceHandleId(sides.source),
    targetHandle: targetHandleId(sides.target),
  }
}
