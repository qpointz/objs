# WI-002 — Graph mapping + retrieval strategies

**Story:** [`STORY.md`](STORY.md)  
**Doc:** [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md)  
**Gaps:** G-F5, G-P2…G-P5, G-P12, G-F7/G-F8  
**Status:** complete  

## Goal

Iterate [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md) until each Journey 1–3 capability has a chosen objs mapping and a retrieval strategy (or an explicit foundation gap pointer).

## Focus (MI / portfolios)

```text
R21 portfolio level → apps → R22 latest version graph_ids
  → graph-id-set matcher (FB-5) → Gremlin (FB-4) → domain DTO
```

Portfolios stay **domain-only**. Core must not know “portfolio”.

## Deliverables

- [x] Domain→objs mapping table reviewed after WI-001 glossary  
- [x] Draft vs version entities + `graph_id` strategy locked (G-P2 / G-F5)  
- [x] R22 “latest version” rule: max `captured_at`, tie-break `id`  
- [x] Retrieval strategy R16–R22 with works-today vs gap (§4.6)  
- [x] Cross-links G-F7/G-F8 → FB-4/FB-5 in §4.5–4.6  

## Out of scope

- Implementing store APIs
- UI

## Acceptance

- [x] GRAPH-AND-RETRIEVAL is the engineer source of truth for Stage 2–3 WIs  
- [x] No user-facing doc exposes graph vocabulary (product doc is `docs/design/sbom/example.md`)  
