# WI-007 — Graph context bar Open layout (Note1)

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-000  
**Gaps:** G-U1 (**resolved**), G-U2  

## Goal

Graph context selector must not overlap **Open**. Right edge of selector stays left of Open; selector may grow/shrink on the left.

## Deliverables

- [x] Close G-U1
- [x] Metadata group: `flex: 1; minWidth: 0; overflow: hidden`
- [x] Trailing **Open + N/E**: `flexShrink: 0`
- [x] Same layout in Composer bar + host `Box` allows shrink
- [x] Tour unchanged (`data-tour` targets unchanged)

## Out of scope

- Schema header chrome (G-U2)
- Changing Open menu behaviour
