# WI-005 — Composer Merge vs Overwrite Save

**Status:** done  
**Examples:** workbench  
**Depends on:** WI-003

## Goal

Composer (and Object Linter if shared Save) exposes:

| UI | HTTP | Mode | Body |
|----|------|------|------|
| **Merge** (default Save) | `PATCH /graphs/{id}` | MERGE | `*.set` + `*.unset` (kind-first) |
| **Overwrite…** | `PUT /graphs/{id}` | REPLACE | set-only (`unset` cleared); Mantine confirm modal |

Client: `patchGraphMutation` / `putGraphMutation`. Validate uses matching verb when graph id known.
`ui.md` updated.

## Acceptance

- [x] Default Save = Merge (PATCH)
- [x] Overwrite = PUT + confirm; set-only
- [x] Validate dry-run verb matches Save mode when offered
- [x] `ui.md` documents both options
