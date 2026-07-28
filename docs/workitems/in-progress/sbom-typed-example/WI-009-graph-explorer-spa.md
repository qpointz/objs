# WI-009 — Graph explorer SPA

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** foundation graph API (`GET /api/v1/objs/graph`)

## Acceptance

- [x] React + Mantine SPA under `objs-sbom-example/ui/`
- [x] Annotation JSON + Exec → foundation graph query
- [x] Force-directed graph; nodes colored by type; payload + annotations on select
- [x] Packaged into module static resources; served at `/ui/`
- [x] Gradle build with `-PskipUi=true` escape hatch
