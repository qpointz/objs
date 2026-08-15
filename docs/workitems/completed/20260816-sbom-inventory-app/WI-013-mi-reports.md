# WI-013 — MI reports (portfolio-scoped MI-1…MI-4)

**Story:** [`STORY.md`](STORY.md)  
**Journey:** 3 — Portfolio owner (Portfolios tab only)  
**Gaps:** G-P12, G-A5; foundation [`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md) (FB-1 / FB-2 parked; FB-4 / FB-5 preferred path)  
**Retrieval:** GRAPH-AND-RETRIEVAL R17–R22  
**UI:** WI-009 linear flow — clean and obvious  

## Goal

Ship Journey 3 management reports for the **Portfolio owner** chrome only. Prefer **objs-core** + **Gremlin** over a multi-graph selection. Use implementation to **identify foundation deficits** and feed WI-003 / backlog — do not use `objs-service` REST.

## Run UX (normative)

```text
Select portfolio → select level (node or root) → select report → Run → results
```

## Scope + graphs (every report)

1. **Application set** = apps under the selected subject-area node (subtree); **root** = all apps in the portfolio (R21).  
2. **Graphs** = each in-scope app’s **latest version** `graph_id` only (R22). Apps with no version → omitted from graph selection (may still appear as “no version” in MI-1).  
3. **Draft graphs are never used for MI.**

## v1 report set (extensible after user testing)

| ID | Report | Answers |
|----|--------|---------|
| MI-1 | Portfolio composition | Apps in scope; asset counts by type; relation / DEPENDS_ON density across latest versions |
| MI-2 | Application dependency map | Inferred app→app deps **within the selected set** (shared objects; G-P4) |
| MI-3 | Shared asset hotspots | Assets in **multiple** in-scope apps; which apps |
| MI-4 | Duplicate & risk signals | Identifier duplicate groups + lightweight risk signals across the set |

## Preferred execution path

```text
R21 apps → R22 latest graph_ids → graph-id-set matcher (+ optional obj-expr)
  → materialize union → gremlin-lang (report script) → domain DTO
```

Stopgaps (loop `selectInGraph` / in-memory) only if WI-003 leaves FB-4/FB-5 open; record debt.

## Deliverables

- [x] `MiReportService` + domain API for MI-1…MI-4 (inputs: portfolio id + node/root + report id)  
- [x] R21 helper: portfolio node/root → application ids (or call PortfolioService)  
- [x] R22 helper: application ids → latest version `graph_id`s  
- [x] Prefer graph-id-set matcher + Gremlin (`objs-gremlin-core`); join SBOM tables for labels only  
- [x] Document foundation gaps discovered (update GAPS / GRAPH-AND-RETRIEVAL / FOUNDATION-BACKLOG)  
- [x] Tests per report with seeded data (subtree vs root; apps without versions)  
- [x] Portfolios UI Run → `POST …/portfolios/{id}/reports` + render MI sections  

## Implementation notes

- **MI-1:** `BoMGremlinEngine.selectAndEval` + `BoMGraphIdsMatcher` — `label().groupCount()`, edge counts. Counts are **unique pool entities** in the union (shared memberships counted once).  
- **MI-2 / MI-3:** domain fold over named-graph memberships (FB-1 parked; no reverse index).  
- **MI-4:** identity projection + in-memory groups over selection (FB-2 parked); vulnerability / duplicate-group risk cues.  
- Endpoint: `POST /api/v1/example/sbom/portfolios/{id}/reports` — Portfolios chrome only.

## Acceptance

- [x] All four reports return useful MI in product language  
- [x] Every report respects portfolio → level → latest-version graph scope  
- [x] No MI entry points under Application owner chrome  
- [x] Any missing foundation capability is recorded (not silently bypassed via forbidden APIs)  
