# WI-003 — HTTP read surface

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002  

## Cold start

- Write already: `POST /evaluations/suite` in [`ObjsPolicyController.kt`](../../../../objs-policy-service/src/main/kotlin/org/poc/objs/policy/service/web/ObjsPolicyController.kt).  
- DTOs: [`ArchiveHttpDtos.kt`](../../../../objs-policy-service/src/main/kotlin/org/poc/objs/policy/service/web/ArchiveHttpDtos.kt).  
- Service: [`PolicyPlayService.kt`](../../../../objs-policy-service/src/main/kotlin/org/poc/objs/policy/service/PolicyPlayService.kt).  
- Mirror soft-fail: 503 when archive bean absent (same as persist).  
- Tests: [`ObjsPolicyControllerTest.kt`](../../../../objs-policy-service/src/test/kotlin/org/poc/objs/policy/service/web/ObjsPolicyControllerTest.kt).

## Goal

Expose archive list/load/delete on `:objs-policy-service` (`/api/v1/objs/policy/evaluations/**`).

## Scope

- DTOs for summary + full document responses
- `GET /evaluations`, `GET /evaluations/{id}` (+ `?view=standard`), `DELETE /evaluations/{id}`
- Wire via `PolicyPlayService`; 404 / 503
- Controller tests

## Acceptance

- [x] Soft-fail when archive bean absent (503)
- [x] Full document includes input when stored; standard view omits it
- [x] Tests cover list + get + delete happy paths and missing id
