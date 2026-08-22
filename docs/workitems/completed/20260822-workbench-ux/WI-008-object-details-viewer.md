# WI-008 — Object details viewer (Note 5)

**Status:** done  
**Examples:** workbench (`:objs-service-ui` + thin stats API in `:objs-core` / `:objs-service`)  
**Gaps:** `G-UX-odetail`, `G-UX-over` (**resolved**; Note 1 `G-UX-objver` superseded)  
**Source:** [`UX-NOTES/Note5/Object details view.md`](UX-NOTES/Note5/Object%20details%20view.md)

## Shipped

1. Reusable **ObjectViewer** + SBOM-style **ObjectViewerSection** dividers.
2. **ObjectInspectPane** — Node / Edge / Graph modes; inline **ObjectVersionBrowser**.
3. Versions: `GET …/versions/stats?recent=5` (entity + edge); preview N=5; browser default 10.
4. Tour step for object inspect; Explorer wired; old badge/boxed inspect removed.

## Acceptance

- [x] `G-UX-odetail` / `G-UX-over` resolved in `GAPS.md`
- [x] Explorer inspect matches Note 5 sections
- [x] Versions UX per locked `G-UX-over` (inline)
- [x] `./gradlew :objs-service-ui:test`
