# WI-007 — Python producer / consumer client

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  
**Depends on:** WI-004 (domain REST); preferably WI-006 demo seeds  

## Goal

Ship a Python 3 client that simulates an external **producer** and **consumer** against the domain REST API (G-P6).

## Deliverables

- [x] Script under `examples/asset-repository/scripts/` (`ar_client.py`; `requirements.txt` notes stdlib-only)  
- [x] **Producer:** create/patch collections; write objects with and without UUID (G-P3); write Database+Dataset composition; optional delete  
- [x] **Consumer:** list/filter collections; list/get objects; **search** within a collection; print summaries  
- [x] Uses domain OpenAPI paths only — not `/api/v1/objs/**`  
- [x] Default base URL `http://localhost:8080`; documented in README / design doc  

## Out of scope

- Calling foundation workbench APIs  
- Real message-bus event consumption  

## Acceptance

- Script runs against local `bootRun` and exercises producer + consumer paths including search  
