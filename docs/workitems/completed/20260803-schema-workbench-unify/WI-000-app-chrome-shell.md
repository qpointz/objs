# WI-000 — App chrome + shell merge

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** —

## Goal

Move primary navigation into the top header, remove the app left navbar, drop Schema linter from
nav, and establish the Schemas page as the single workbench shell with route redirects.

## Scope

- [`AppLayout.tsx`](../../../../objs-sbom-example/ui/src/AppLayout.tsx): horizontal links for Graph explorer, Schemas, Object linter; remove `AppShell.Navbar` / burger navbar
- [`App.tsx`](../../../../objs-sbom-example/ui/src/App.tsx): redirect `/linter` and `/schemas/:type/:version/lint` into Schemas create/edit paths
- Keep Schemas page mountable; scaffold for later WIs (existing explorer still loads)

## Acceptance

- [x] Top bar shows Graph / Schemas / Object linter; no persistent left app pane
- [x] Schema linter is not a separate nav item
- [x] Old linter URLs redirect without 404
