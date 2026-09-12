# WI-001 — Export rewrite for `.drl`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Export DRL  
**Status:** planned  
**Depends on:** WI-000  
**Examples:** **SBOM policy seeds** (`examples/sbom/.../seeds/policy/drl/*.drl`)

## Goal

Include Drools `.drl` in export text rewrite so `package` / `import` lines follow `TARGET_PACKAGE`.
Document that the repo scan found no other missing package-sensitive non-code extensions.

## Deliverables

- [ ] Add `.drl` to `REPLACE_EXTENSIONS` in [`scripts/export/generate-config.py`](../../../../scripts/export/generate-config.py)
- [ ] Fixture DRL + assertion in [`scripts/export/test_fixture.py`](../../../../scripts/export/test_fixture.py)
- [ ] Note in [`scripts/export/README.md`](../../../../scripts/export/README.md)
- [ ] `make test-export-fixture` passes

## Acceptance

- [ ] Exported sample `.drl` has no leftover `org.poc.objs`
- [ ] G-1 decision already locked; ship the `.drl` rewrite

## Out of scope

- Codegen path guard (WI-002)
