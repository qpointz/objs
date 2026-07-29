# WI-003 — Matcher selection REST API

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — REST API  
**Status:** done  
**Depends on:** WI-000–WI-002; G-A1–G-A4 in [`GAPS.md`](GAPS.md)

## Goal

Replace annotation matching through GET with one explicit graph-query operation accepting
equivalent JSON and YAML matcher DSL bodies.

## Scope

- Add `POST /api/v1/objs/graph/query` as the sole graph-query endpoint.
- Remove matching via `GET /api/v1/objs/graph?key=value`.
- Use `/query`, not `/select` or matcher-specific endpoint paths.
- Consume:
  - `application/json`;
  - `application/yaml`;
  - `text/yaml`.
- Accept either one matcher object or an ordered matcher array.
- Decode the request to `BoMMatcher` before calling
  `BoMGraphStore.selectSubgraph(matcher: BoMMatcher)`.
- Return the existing `BoMSubgraph` response shape.
- Express simple annotation match-all requests through `{ "anno": { ... } }`.
- Validate body size, shape, matcher registration, expression safety, and execution limits before or
  during selection as appropriate.
- Map malformed DSL, expression compilation/evaluation, and 3-minute selection-budget failures to
  stable `BoMValidationResult` issue codes and appropriate HTTP statuses.
- Extend the graph OpenAPI group with:
  - matcher DSL request schema/discriminator documentation;
  - object and array examples;
  - equivalent JSON and YAML examples;
  - validation/error responses;
  - the existing `BoMSubgraph` response schema.
- Add MockMvc/controller tests for both media types and matcher forms.

## Query operation

- `POST /api/v1/objs/graph/query` is the only graph matching/query operation.
- It accepts both simple `anno` objects and expressive/chained matcher DSL bodies.
- Remove the current `GET /api/v1/objs/graph` controller operation.
- Seed graph export filters remain unchanged in this WI unless the implementation shares internal
  matcher construction without changing their API.

## Out of scope

- Keeping a compatibility GET alias for matching
- Adding `/select` or matcher-specific query paths
- GET bodies or encoding expressions into query parameters
- Authentication/authorization
- Streaming response formats
- A separate endpoint per matcher kind

## Acceptance

- [x] JSON and YAML bodies produce equivalent selections
- [x] Single-object and array forms decode to `BoMMatcher` and use one store method
- [x] `POST /api/v1/objs/graph/query` is the sole graph matching/query endpoint
- [x] Matching through `GET /api/v1/objs/graph?key=value` is removed
- [x] Simple annotation queries use the `anno` DSL body
- [x] Empty/unknown/multi-key/malformed matcher requests are rejected with stable errors
- [x] Unsafe, invalid, oversized, or over-budget (3-minute) JEXL requests produce documented responses
- [x] OpenAPI documents matcher variants, both encodings, and the existing subgraph response
- [x] Controller tests cover `anno`, `anno-expr`, chained requests, content types, and failures
