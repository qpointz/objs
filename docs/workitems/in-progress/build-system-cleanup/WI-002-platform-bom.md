# WI-002 — Drop Spring DM; adopt `platform()`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Platform + prune  
**Status:** pending  
**Depends on:** WI-001

## Goal

Remove `io.spring.dependency-management` and `dependencyManagement { mavenBom(...) }`
from every module. Import Boot BOM via Gradle `platform(libs.boot.dependencies)`.
Pin any formerly versionless coords in the catalog (e.g. `postgresql`).

## Acceptance

- [ ] No `spring.dependency.management` plugin or `dependencyManagement` blocks  
- [ ] Modules that need Boot-managed alignment use `platform(libs.boot.dependencies)`  
- [ ] Catalog / build still resolves (compile at least one leaf)  
