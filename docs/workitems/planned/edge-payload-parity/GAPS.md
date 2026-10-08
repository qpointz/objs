# Gaps — edge-payload-parity (C-48)

Status: `open` | `open (both)` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

- `open` — edge is weaker than entity.
- `open (both)` — missing for entity **and** edge; WI-001 decides whether it is in scope.

**WI-001 must close every `open` row** (resolve or defer) before implementation. Evidence paths are
as of `origin/dev` at story creation; line numbers are approximate.

---

## Parity baseline (no gap)

- JSONB storage: `bom_graph_edge.properties` vs `bom_entity.payload`
- JSON Schema validation (networknt) of edge properties when rule policy is `SCHEMA`
- Seed parse / apply / serialize (`GraphSeedHandler`), edge-properties schemas in seeds
- Deep versioning: versions / stats / compact (`ObjsEdgesController`, `DeepGraphVersionService`)
- Bulk `GraphMutation` `edges.set` / `edges.unset`; delete
- Identifier immutability check (`validateEdgeIdentifierImmutability`)
- Catalog JSON Schema export (`includeEdgePropertySchemas`)
- Raw carry-through into Gremlin (`EnvelopeMaterializationStrategy`), JGraphT (`GenericGraphEdge`), Drools (`EdgeFact`)

---

## 1. Domain model (`objs-api`)

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G1 | Edge `type` / `schemaVersion` / `properties` nullable; entity equivalents required | open | `api/domain/GraphPrimitives.kt` 12-41 |
| G2 | Edges have no `annotations` | open | `GraphPrimitives.kt`; `docs/design/graph/annotations-and-matchers.md` ("provisional") |
| G3 | No latest edge-properties schema helper (only `latestEntitySchema(s)`) | open | `api/domain/CatalogSupport.kt` 24-35 |
| G4 | No `displayLabel` / `fieldHints` / `firstLevelScalarFields` for edges | open | `CatalogSupport.kt` 37-83 |
| G5 | Validator never checks `Schema.usage` (entity may reference `EDGE_PROPERTIES` schema and vice versa) | open (both) | `core/validation/Validator.kt` 51, 239 |

## 2. Validation and schema evolution

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G6 | Allow-list rule pins one `propertiesSchemaVersion`; edge `type@version` must equal it (`EDGE_SCHEMA_REF_MISMATCH`). Entities may use any registered version | open | `Validator.kt` 187-205 vs 51; `docs/design/graph/validation.md` |
| G7 | Consequence of G6: bumping a rule to v2 breaks every stored v1 edge on next rewrite (incl. `clone` / `copyGraph` / `mergeGraph` re-validation) | open | `core/persistence/NamedGraphStore.kt` (graph copy/merge paths) |
| G8 | Upgrade-on-read is entity-only: `PayloadUpgrade.hydrate(entity: Entity, …)`; `TypedGraphView` copies edges raw; `RelationEdgeView` has no `effectiveSchemaVersion` / `upgradeDiagnostics` | open | `api/typed/upgrade/PayloadUpgrade.kt` 17; `api/typed/TypedGraphView.kt` 64-73, 154, 200-212 |
| G9 | No edge binding registry (`TypedEntityBindingRegistry` entity-only) | open | `TypedGraphView.kt` 17-24 |
| G10 | Stored payload is never rewritten on upgrade; `objs-autoconfigure` registers no upgrade registry | open (both) | C-35 GAPS G-E5 / G-E6; `docs/design/graph/schema-evolution.md` ("edges unchanged"); `SbomSchemaUpgradeConfiguration.kt` |
| G11 | Policy `NONE`: stray edge `type` / `schemaVersion` not flagged | open | `Validator.kt` 157-170 |
| G12 | Edge identifier-immutability skipped when either side lacks `type` / `schemaVersion` | open | `Validator.kt` 361-364 |

## 3. Persistence (`objs-persistence`)

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G13 | No `(type, schema_version)` index on `bom_graph_edge` (entity has `idx_bom_entity_type_schema_version`) | open | `db/migration/{postgresql,h2}/V1__bom_schema.sql` |
| G14 | No GIN index on payload / properties (entity has one on `annotations` only) | open (both) | `postgresql/V1__bom_schema.sql` |
| G15 | Entity-only store helpers: `getEntity`, `listEntities`, `upsertEntities`, `countByType`, `findEntitiesByIdentity`, `findDuplicateGroups`, `identityOf` | open | `core/persistence/GraphStore.kt` 239-359 |
| G16 | `EdgeDao` has no lookup by type / schema version | open | `EntityDao.kt` 122-195 |
| G17 | `selectFromPool` always returns `edges = emptyList()`; graph-scoped select returns only edges induced by selected entities | open | `GraphStore.kt` 282, 447-449 |
| G18 | `VersioningStrategy.shouldCapture` not called in `applyGraphEdgeUpserts` (called for entities on same path; result ignored today) | open | `NamedGraphStore.kt` 902 vs 951-967 |

## 4. Query / matcher

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G19 | `obj-expr` binds `id` / `type` / `schemaVersion` / `a` / `p` for `EntityMatchCandidate` only | open | `api/match/ObjExprMatcher.kt` 43, 62, 160-176 |
| G20 | `Matcher.matchesEdge` checks endpoints only; never overridden (except `ChainedMatcher` delegate); never called from main code | open | `api/match/Matcher.kt` 14-15 |
| G21 | Unused / unimplemented: `EdgeMatchCandidate.properties`, `EdgeCandidateStrategy`, `CandidateSourceWithEdges`, `LazyJsonMap.properties()` | open | `MatchCandidate.kt` 37-55; `CandidateSource.kt` 23-38; `LazyJsonMap.kt` 90-91 |
| G22 | Matcher DSL has no edge-expression key | open | `MatcherDsl.kt` 25-28 |
| G23 | SQL pushdown covers `p.*` only (`ObjExprLowerer`, `PoolEntityReader`) | open | `ObjExprLowerer.kt`; `PoolEntityReader.kt` 168-230 |
| G24 | Net effect: no component can filter by edge `role` / `type` / `properties` (gremlin-service, jgrapht-service, policy-service, workbench, examples) | open | consequence of G19-G23 |

## 5. Mutation / rewriter / merge

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G25 | `MutationRewriter`: `EntityDecision` + `replaceByPayload` for entities; edges only `edge(edge, ctx): Edge?` | open | `GraphMutationRewriter.kt` 8-32, 132 |
| G26 | Rewriter dedupes edges by `(source, target, role)` ignoring properties — parallel edges with different properties collapse silently | open | `GraphMutationRewriter.kt` 92 |
| G27 | `FirstSeenGraphMergePolicy.edgeKey` same triple; property maps never merged | open | `GraphMergePolicy.kt` 20-31 |
| G28 | `TypedEdge` only `toEdge`; no `fromEdge` / `syncFrom`; `EdgeCatalog` has no `toTyped` | open | `TypedEdge.kt` 14-33; `docs/design/graph/typed-conversion-recipes.md` |
| G29 | Writes are full-document replace; no field-level patch | open (both) | `GraphStore.kt` 216, 373; `docs/design/graph/object-schema-dsl.md` |

## 6. REST (`objs-service`)

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G30 | `POST /graphs/{id}/edges` takes raw domain `Edge` (OpenAPI shows `graphId` / `createdAt` / `headVersion` writable); entity has `EntityWriteBody` | open | `ObjsGraphsController.kt` 478-510; `ObjsEntitiesController.kt` 132-153 |
| G31 | No GET for a single live edge (`/edges/{id}` or `/graphs/{id}/edges/{edgeId}`) | open | `ObjsGraphsController.kt`; `ObjsEdgesController.kt` |
| G32 | `PUT /graphs/{id}/edges/{edgeId}` requires full edge (`source` / `target` / `role` non-null) for a properties-only change | open | `ObjsGraphsController.kt` 512-556 |
| G33 | No edge list / search; `POST /entities/query` documents "Edges are not returned" | open | `ObjsEntitiesController.kt` 92, 124 |
| G34 | `GET /registry/types/{type}/edges` works for entity types only | open | `ObjsRegistryController.kt` 704-720 |
| G35 | `next-major` exists for edge schemas but is unusable in practice because of G6 | open | `ObjsRegistryController.kt` 554-603 |
| G36 | No edge-to-graphs membership endpoint (edges are graph-local; may be fine) | open | `ObjsEntitiesController.kt` `/entities/{id}/graphs` |

## 7. Gremlin (`objs-gremlin-core`, `objs-gremlin-service`)

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G37 | Vertex label = entity `type`; edge label = `role`, edge `type` is a separate property — `hasLabel` semantics differ | open | `materialize/EnvelopeMaterializationStrategy.kt` 31 vs 48-60 |
| G38 | Null edge `properties` → TinkerGraph property absent → response `"properties": null`; vertices always `payload: {}` | open | `EnvelopeMaterializationStrategy.kt` 56-59; `GremlinResultProjector.kt` 139 vs 157 |
| G39 | `mapToEdge` drops `graphId` / `createdAt` / `updatedAt` / `headVersion` on edge hits (entities lose the same minus `graphId`) | open | `GremlinResultProjector.kt` 70-93, 191-228 |
| G40 | Payload nested, never flattened; `flatten` strategy documented, not implemented | open (both) | `GremlinMaterializationStrategy.kt` 9-10 |
| G41 | Tests missing: nested/list edge properties, projector round-trip, REST response edge properties, null-properties edges | open | `EnvelopeMaterializationStrategyTest.kt` 76-78; `GremlinEngineTest.kt` 41-46; `ObjsGremlinControllerTest.kt` |

## 8. JGraphT (`objs-jgrapht-core`, `objs-jgrapht-service`)

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G42 | Edge properties carried but unused; unweighted `DirectedPseudograph`, no weight-from-property | open | `DefaultJGraphTGraphFactory.kt` 9-12; `GenericGraphElements.kt` 14-18 |
| G43 | Request has no edge role / property filter and no weight-key parameter | open | `GraphCycleAnalysisRequest.kt` 21-35 |
| G44 | Analyzer purely topological; result DTOs carry ids only | open (both) | `DirectedCycleRegionAnalyzer.kt`; `GraphAlgorithmDtos.kt` 30-40 |
| G45 | No DOT / GraphML / JSON export | open (both) | `objs-jgrapht-*` |
| G46 | No test that edge properties are preserved | open | `GenericJGraphTMaterializerTest.kt` |

## 9. Policies (`objs-policy-*`)

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G47 | `EdgeFact`: `annotations` always `emptyMap()`; `type` / `schema` / `schemaVersion` nullable (DRL null-safety); `graphId` not projected | open | `objs-policy-drools/.../PolicyFacts.kt` 41-67 |
| G48 | No `EdgeFact.from` test; no DRL fixture using `properties[...]` | open | `DroolsPolicyEngineTest.kt` |
| G49 | Policy-service input selection uses entity-only matcher (see G24) | open | `PolicyHttpDtos.kt` 18-33; `SuiteHttpDtos.kt` 94-99 |
| G50 | No policy targeting/applicability by entity or edge type; CUSTOM engine never reads context; no archive filter by subject; no payload snapshot in findings | open (both) | `Policy.kt`; `Applicability.kt`; `CustomPolicyEngine.kt`; `EvaluationArchive.kt`; `Finding.kt` |

## 10. Workbench UI (`objs-service-ui`)

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G51 | TS `BoMEdge` lacks `graphId` / `createdAt` / `updatedAt`; `EdgeRelationRequest` lacks `description` / `sourceVerb` / `targetVerb` / `tags` / `attributes` | open | `src/types.ts` 13-22, 217-223 |
| G52 | Composer: no schema-version migrate menu for edges (entities use `migratePayloadByKey`) | open | `ObjectLinterVisualPanel.tsx` 914-956, 1482-1501 |
| G53 | Composer: no annotations tab, no field delete for edges | open | `ObjectLinterVisualPanel.tsx` 1598-1631 vs 1751-1765 |
| G54 | New edges start `properties: {}` without schema defaults; invalid under `emptyPropertiesAllowed=false` | open | `ObjectLinterVisualPanel.tsx` 900-1009 |
| G55 | Canvas labels edges with `role` only; properties never rendered; `GraphLink` has no field kinds | open | `GraphCanvas.tsx` 259; `types.ts` 122-138 |
| G56 | Edge filter is role-only; no type / property filter, no edge search page, no properties column in Structured Edges grid | open | `GraphFilterToolbar.tsx` 416-524; `queryStructuredModel.ts` 59-94 |
| G57 | `SchemaExplorerPage`: no relations tab for edge-properties schemas; catalog overview skips them; `getSchemaEdges` / `replaceSchemaEdges` / `EdgeRelationsEditor.tsx` unused | open | `SchemaExplorerPage.tsx` 434-436, 1239-1241; `SchemaCatalogOverview.tsx` 820; `api.ts` 729-750 |
| G58 | Bug: edge viewer passes `identityLabel="Node"` | open | `ObjectInspectPane.tsx` 468 |
| G59 | No version diff view | open (both) | `objs-service-ui/src` |

## 11. Examples

| # | Topic | Status | Evidence |
|---|-------|--------|----------|
| G60 | SBOM: `CanonicalEdge@1.0.0` declared but always written `{}`; no domain edge fields (dependency scope, optional, dev, …) | open | `sbom-ontology.yaml` 140-168; `ApplicationVersionService.kt` 375-382, 621-629; `sbom-demo-graph.yaml` |
| G61 | SBOM DTOs drop edge `type` / `properties`: `RelationView`, `AssetUsageRelation`, `DraftRelationWrite`, UI TS types | open | `ApplicationModels.kt` 46-71; `AssetInventoryModels.kt` 53-59; `sbom-service-ui/src/api/types.ts` 32-38 |
| G62 | SBOM: no relation update endpoint; CycloneDX export ignores edge properties | open | `ApplicationInventoryController.kt` 329-343; `CycloneDxExportService.kt` 68-84 |
| G63 | SBOM: only entity upgrade wired (`Component_1_0_0_to_2_0_0`); "Used in" and schema pages hide edge schemas | open | `SbomSchemaUpgradeConfiguration.kt` 27-38; `SchemaBrowseService.kt` 103-117; `SchemaPortalPage.tsx`; `SchemaViewPage.tsx` |
| G64 | AR: all 29 rules `propertiesPolicy: NONE`, no edge schemas — exercises no edge-property path | open | `asset-repository-ontology.yaml` |
| G65 | AR: `ObjectRelationDto` and UI relation types have no properties; no relation update / delete | open | `ApiDtos.java` 97-113; `ObjectWriteService.java` 81-159; `asset-repository-service-ui/src/api.ts` 177-200 |
