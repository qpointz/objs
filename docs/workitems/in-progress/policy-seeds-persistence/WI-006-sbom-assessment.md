# WI-006 — SBOM Assessment demo

**Story:** [STORY.md](STORY.md)  
**Status:** done

## Goal

Demo-ready Assessment for the SBOM inventory example: seed three Dimension→Measure policy suites, per-application Assessment overlays, and a Portfolio Assessment matrix tab. Results are **session-only** (no persistence). UI talks **only** to sbom-service (`/api/v1/inventory/**`); policy evaluation runs in-process on the server.

## Deliverables

1. Classpath policy catalog seeds (Category / Policy / PolicySuite REPLACE) under `examples/sbom/sbom-service/src/main/resources/seeds/policy/`, loaded on `demo` profile.
2. Demo inventory bias so clean vs dirty apps produce clear PASS vs FAIL/WARN.
3. SBOM Assessment façade:
   - `GET /api/v1/inventory/assessment/suites`
   - `POST /api/v1/inventory/applications/{id}/assessment/run`
   - `POST /api/v1/inventory/portfolios/{id}/assessment/run`
4. Application detail: Assessment split button + severity overlays + Clear.
5. Portfolio workspace: Assessment tab with two-level Dimension/Measure matrix, progress, Clear.
6. `PortfolioAssessmentRunner` SPI with sequential MVP impl (replaceable by C-29 batch later).

## Out of scope

- Persisting evaluation results (G-P11r)
- Real batch evaluate (C-29)
- Calling `/api/v1/objs/**` from sbom-service-ui
