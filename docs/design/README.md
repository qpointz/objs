# Design docs

Architecture and design notes for **objs** — an entity/graph PoC (**entity store**).

Organised by logical component (not by story). Story process lives under
[`docs/workitems/`](../workitems/RULES.md).

**Target Java package root:** `org.poc.objs` (scaffold code may still use `io.qpointz.poc.objs` until renamed).

| Area | Path | Contents |
|------|------|----------|
| Platform overview | [`platform/overview.md`](platform/overview.md) | Product intent, stack, module map, open questions |
| Build system | [`platform/build-system.md`](platform/build-system.md) | Gradle layout, versioning, conventions |
| GitLab CI | [`platform/ci-pipeline.md`](platform/ci-pipeline.md) | Orchestrator, unit/IT children, reserved Docker publish |
| Kotlin | [`platform/kotlin.md`](platform/kotlin.md) | Kotlin toolchain and implementation notes |
| App | [`platform/app.md`](platform/app.md) | `objs-service-app`: workbench-only runnable |
| Core | [`core/README.md`](core/README.md), [`core/spring-split.md`](core/spring-split.md), [`core/persistence-backends.md`](core/persistence-backends.md) | `objs-api` / Spring-free `objs-persistence` / `objs-autoconfigure` (C-25) |
| Service | [`service/README.md`](service/README.md) | `objs-service`: REST + autoconfiguration |
| Graph domain | [`graph/README.md`](graph/README.md), [`graph/persist-sketch.md`](graph/persist-sketch.md) | Entity store model, annotations, validation, write-path sketch |
| Foundation vs examples | [`graph/apps-vs-foundation.md`](graph/apps-vs-foundation.md) | Object/graph APIs: what apps reimplemented; what to lift into objs-persistence |
| Gremlin traversal | [`graph/gremlin.md`](graph/gremlin.md) | Matcher → read-only TinkerGraph → gremlin-lang → result |
| Gremlin examples | [`graph/gremlin-examples.md`](graph/gremlin-examples.md) | Sample scripts (vertices, tables, SBOM edge roles) |
| Typed domain | [`graph/typed-domain.md`](graph/typed-domain.md) | Reusable typed façades / GraphBuilder on the foundation |
| UI manual | [`ui.md`](ui.md) | User guide for graph exploration, schema browsing, and schema authoring |
| SBOM example | [`sbom/example.md`](sbom/example.md) | Concrete SBOM app on the entity store |
| SBOM user guide | [`sbom/user.md`](sbom/user.md) | Inventory SPA: applications, multi-BOM versions, drafts, graph |
| Canonical software graph | [`sbom/canonical-spec.md`](sbom/canonical-spec.md) | Ontology draft (types + relationships) |
| Asset repository example | [`asset-repository/example.md`](asset-repository/example.md) | Collections-as-graphs object store example |
| Policy evaluation | [`policy/README.md`](policy/README.md) | C-24 + C-26 shipped; C-31 playground on branch; suites/seeds/batch later |
