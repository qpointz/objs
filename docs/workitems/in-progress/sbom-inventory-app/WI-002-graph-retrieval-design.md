# WI-002 — Graph mapping + retrieval strategies

**Story:** [`STORY.md`](STORY.md)  
**Doc:** [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md)  
**Gaps:** G-F5, G-P2…G-P5, G-P12, G-F7/G-F8  

## Goal

Iterate [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md) until each Journey 1–3 capability has a chosen objs mapping and a retrieval strategy (or an explicit foundation gap pointer).

## Focus (MI / portfolios)

Document end-to-end:

```text
R21 portfolio level → apps → R22 latest version graph_ids
  → graph-id-set matcher (FB-5) → Gremlin (FB-4) → domain DTO
```

Portfolios stay **domain-only** (no portfolio graph). Core must not know “portfolio”.

## Deliverables

- [ ] Domain→objs mapping table reviewed and adjusted after WI-001 glossary
- [ ] Draft vs version entities + `graph_id` strategy locked (G-P2 / G-F5)
- [ ] R22 “latest version” rule defined (ordering field)
- [ ] Retrieval strategy per capability including R16–R22 (CDX + portfolio MI) with “works today” vs “gap”
- [ ] Cross-links from `GAPS.md` foundation rows (G-F7/G-F8 → FB-4/FB-5) to concrete sections

## Out of scope

- Implementing store APIs
- UI

## Acceptance

- GRAPH-AND-RETRIEVAL is the engineer source of truth for Stage 2–3 WIs
- No user-facing doc exposes graph vocabulary
