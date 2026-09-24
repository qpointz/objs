# WI-002 — Apply tags and springdoc groups

**Status:** done  
**Examples:** objs + SBOM + AR  
**Depends on:** WI-001

## Goal

Apply the locked tag/group matrix from WI-001.

## Done

- `ObjsOpenApiConfiguration`: `graph` group includes entities + edges; registry unchanged
- `ObjsPolicyOpenApiConfiguration`: separate `policy` group
- Topic `@Operation(tags=…)` on graphs, policy, SBOM apps, AR controllers
- `graph-seeds` tag on seed I/O controller; descriptions on entities/edges/traverse/algorithms

## Acceptance

- [x] Tags/groups match WI-001 matrix
- [x] Relevant module tests green (`:objs-service:test`, `:objs-policy-service:test` filtered)
