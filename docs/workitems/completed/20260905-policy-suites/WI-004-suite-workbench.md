# WI-004 — Workbench Policy > Suites (basic)

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002, WI-003  

## Goal

Add a **basic** workbench UI to create/manage policy suites and run suite evaluation against the shared graph context.

**Look & feel + interaction parity:** Suites must **match the current Policy Evaluate view** (same workbench chrome, density, Mantine controls, pane layout patterns, confirmations, and shared graph-context behavior). Prefer reuse/extract of shared Policy shell rather than a divergent Suites-only layout. Still tactical / rework-OK, but not a different visual language.

## Navigation

Top-level **Policy** gains **subnavigation** (same Policy route / nav item):

| Item | Role |
|------|------|
| **Evaluate** | Current Policy play — unchanged |
| **Suites** | Suite authoring + selection examine + suite evaluate |

Subnav should feel like part of the Policy page (tabs or segmented control in the Policy header area), not a second top-level Workbench entry.

## Match Evaluate (normative for this WI)

Reuse / mirror from `PolicyPlayPage` (and shared workbench pieces):

| Aspect | Expectation |
|--------|-------------|
| Layout | Same overall Policy chrome and resize patterns, but **bias space toward editing**. Suite authoring (tree, folders, matchers, selection examine, evaluate controls/results) is **primary**; graph / data / canvas inspect is **secondary** — give it less default width/height than on Evaluate, or collapse/minimize until needed. Prefer a larger center editor over a large live graph when both compete. |
| Left pane | Tree navigation (suite → folders → policies-as-leaves or selection preview), expand/collapse, selection drives center pane |
| Toolbar | Split **Add** menu (e.g. Suite / Folder / … as needed), **Delete** bound to selection (suite vs folder), same button sizes/placement language |
| Dialogs | Mantine modals/confirms only (no `window.confirm`) — same as Evaluate |
| Graph context | Same shared graph / matcher context as Evaluate; refuse evaluate without context with the same class of message |
| Evaluate action | Primary evaluate control analogous to Policy Evaluate; results in a tasks/results area consistent with Evaluate (status, severity, findings drill where feasible) |
| Forms | Mantine `TextInput` / `Select` / `Textarea` density (`size="xs"` etc.) consistent with Policy editor |
| Empty states | Dimmed helper text when nothing selected / empty suite — same tone as Evaluate |

## Scope

- [x] Policy shell with subnav **Evaluate** \| **Suites** (shared chrome); Suites layout **prioritizes edit space** over graph/data pane
- [x] **Suites** CRUD + folder tree + matchers (STATIC_LIST + METADATA); participation / roll-up strategy / severityConfig
- [x] **Examine policy selection** — effective policies for folder/suite before run (center or side list, Evaluate-like compactness)
- [x] **Evaluate suite** vs current graph context; scope **full** + **subfolder** at minimum
- [x] Results: `evaluationId` / tree status·severity / outcomes — presented in Evaluate-like results/tasks UX
- [x] Thin HTTP on `:objs-policy-service` for CRUD + `evaluateSuite` + selection preview (landed in WI-003)

## Out of scope

- Divergent design system or “new product” Suites layout
- Polished SBOM matrix / cross-suite reporting
- Input persist / snapshot freeze
- Drools-as-roll-up-strategy
- Full C-30 consumer REST beyond play service

## Acceptance

- [x] **Policy > Suites** is recognizable as the same Policy workbench family as **Evaluate**, with **more room for suite editing** (graph/data secondary)
- [x] User can create/manage suites, examine selection, run full or subfolder evaluate against current context
- [x] **Policy > Evaluate** unchanged in behavior
- [x] Still acceptable to rework later; must not ship as a one-off visual experiment
