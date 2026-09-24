# WI-004 — Java + Python client codegen smoke

**Status:** done  
**Examples:** docs / scripts  
**Depends on:** WI-003

## Goal

Manual harness + docs for openapi-generator-cli Java/Python generate + compile.

## Done

- `scripts/openapi-client-codegen-smoke.py` — fetch `/v3/api-docs/{group}`, generate, compile  
- `scripts/openapi-client-codegen-smoke.md` — operator recipe  
- Default base `http://localhost:8080`; `--base` for workbench `:8081`  
- Primary groups `graph` `registry` `policy`; `--readiness-only` for SBOM/AR  
- Generated output temp/gitignored (`.tmp/…`)

## Acceptance

- [x] Documented recipe + harness  
- [x] Compile bar for primary groups (manual against localhost)  
- [x] No generated sources in git  
- [ ] Operator runs harness once against local workbench before merge (recommended)
