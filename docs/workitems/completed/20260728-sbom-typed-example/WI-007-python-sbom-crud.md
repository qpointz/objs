# WI-007 — Python SBOM REST CRUD / random graphs

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-004, full ontology (G-S39)

## Acceptance

- [x] Script under `objs-sbom-example/scripts/` talks to SBOM + foundation graph REST
- [x] Create / retrieve / update / delete objects (entities + edges)
- [x] Random graph generator using canonical types + allow-listed roles
- [x] Default base URL `http://localhost:8080`; documented in [`example.md`](../../../design/sbom/example.md)

## Notes

- **Create / update:** `PUT /api/v1/example/sbom/apps/{app}/versions/{version}` (upsert `BoMGraph`)
- **Retrieve:** `GET` same SBOM paths
- **List:** `GET /api/v1/example/sbom/apps` (WI-008)
- **Delete:** `DELETE /api/v1/objs/graph` with `{entityIds, edgeIds}` (foundation batch delete)
