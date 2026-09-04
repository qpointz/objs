# WI-002 — Policy HTTP + opt-in wiring

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  
**Depends on:** WI-001  

## Goal

Expose thin HTTP for playground: **list / create / delete / check (compile) / evaluate**, plus capability discovery. Opt-in on `:objs-service-app` only.

## Delivered

- [x] `:objs-policy-service` — REST under `/api/v1/objs/policy/**`, OpenAPI tag `policy`
- [x] Capability `GET …/capabilities`
- [x] CRUD + `POST …/check` + `POST …/evaluate`
- [x] `PolicyRepository.update` / `delete`; Drools `tryCompile`
- [x] Wire into `:objs-service-app`
- [x] Controller tests

## Acceptance

- [x] Workbench runner can call list/create/delete/check/evaluate when module present
- [x] Absent module → capability 404 / soft degrade
