import { describe, expect, it } from 'vitest'
import { closestHandleIds, closestHandleSides } from './graphEdgeHandles'

describe('closestHandleSides', () => {
  it('shouldAttachRightToLeft_whenTargetIsToTheRight', () => {
    const source = { id: 'a', position: { x: 0, y: 0 }, width: 180, height: 110 }
    const target = { id: 'b', position: { x: 300, y: 10 }, width: 180, height: 110 }
    expect(closestHandleSides(source, target)).toEqual({ source: 'right', target: 'left' })
    expect(closestHandleIds(source, target)).toEqual({
      sourceHandle: 'right-source',
      targetHandle: 'left-target',
    })
  })

  it('shouldAttachBottomToTop_whenTargetIsBelow', () => {
    const source = { id: 'a', position: { x: 0, y: 0 }, width: 180, height: 110 }
    const target = { id: 'b', position: { x: 20, y: 250 }, width: 180, height: 110 }
    expect(closestHandleSides(source, target)).toEqual({ source: 'bottom', target: 'top' })
  })

  it('shouldAttachTopToBottom_whenTargetIsAbove', () => {
    const source = { id: 'a', position: { x: 0, y: 250 }, width: 180, height: 110 }
    const target = { id: 'b', position: { x: 20, y: 0 }, width: 180, height: 110 }
    expect(closestHandleSides(source, target)).toEqual({ source: 'top', target: 'bottom' })
  })

  it('shouldUseSides_whenSelfLoop', () => {
    const node = { id: 'a', position: { x: 0, y: 0 }, width: 180, height: 110 }
    expect(closestHandleSides(node, node)).toEqual({ source: 'right', target: 'left' })
  })
})
