# Consumers — store-text-search (C-20)

**Normative** with [`STORY.md`](STORY.md) after WI-001. Rewire in **WI-003** (same commit as that WI). Product name search stays domain.

## Stopgaps (today)

| Consumer | Today | After this story |
|----------|--------|------------------|
| Workbench Objects | Equality `obj-expr` only; full list until C-17 paging | Store text search on paged pool select |
| Workbench `GET /graphs/search?q=` | Header substring | **Unchanged** (not payload search) |
| SBOM `AssetInventoryService` asset list | Equality clauses / in-memory | Store `q` (or locked DSL) on searchable/identifier fields |
| SBOM application name | `sbom_application` `LIKE` | **Stays domain** |
| AR collection object search | Equality `obj-expr` / full graph | Store graph-scoped text search |
| AR collection name | `ar_collection` `LIKE` | **Stays domain** |

Exact method names and query params are locked in WI-001, then written into living `apps-vs-foundation` + product `example.md` in WI-002/WI-003/WI-004.
