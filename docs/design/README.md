# Design docs

Architecture and design notes for **objs** — an entity/graph PoC (**entity store**).

Organised by logical component (not by story). Story process lives under
[`docs/workitems/`](../workitems/RULES.md).

**Target Java package root:** `org.poc.objs` (scaffold code may still use `io.qpointz.poc.objs` until renamed).

| Area | Path | Contents |
|------|------|----------|
| Platform overview | [`platform/overview.md`](platform/overview.md) | Product intent, stack, module map, open questions |
| Build system | [`platform/build-system.md`](platform/build-system.md) | Gradle layout, versioning, conventions |
| Kotlin | [`platform/kotlin.md`](platform/kotlin.md) | Kotlin toolchain and implementation notes |
| Core | [`core/README.md`](core/README.md) | `objs-core`: entity SDK + validation + JPA |
| Service | [`service/README.md`](service/README.md) | `objs-service`: REST + autoconfiguration |
| Graph domain | [`graph/README.md`](graph/README.md) | Entity store model, annotations, validation, persistence |
