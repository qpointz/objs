# WI-004 — Graph Apply-layout overlay (Note 4)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** Intake  
**Status:** done  
**Depends on:** WI-003  
**Examples:** **workbench**  
**Notes:** Intake Note removed at story closure
**Gaps:** [`GAPS.md`](GAPS.md) G-WU2-N4

## Goal

Move **Apply layout** off view action bars onto every Visual/Graph canvas: hover-reveal toolbar (top-right, `IconLayoutDashboard`) + duplicate on canvas context menu.

## Deliverables

- [x] Semi-transparent layout toolbar on graph canvases (hover to interact; apply + direction menu)
- [x] Apply layout on canvas context menu (pane / existing menus)
- [x] Remove Apply layout from Explorer / Policy / Composer view action bars
- [x] Schema catalog Visual: same overlay pattern
- [x] Tour + `ui.md`

## Acceptance

- [x] Apply layout is not on the page title action row for graph views
- [x] Toolbar sits top-right on the graph surface; quiet until hover
- [x] Right-click canvas offers Apply layout

## Implementation

- `GraphLayoutToolbar` / `GraphLayoutMenuItems` — shared overlay + menu items
- `GraphCanvas` — toolbar when `onLayoutChange` set; built-in pane menu when host has no `onPaneContextMenu`
- Hosts: Explorer, Query, Policy (`PolicyGraphOutputColumn`), Composer (`ObjectLinterVisualPanel` merges into canvas menu), Schema catalog overview
