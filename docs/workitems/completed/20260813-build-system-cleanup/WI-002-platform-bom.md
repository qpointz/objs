# WI-002 — Drop Spring DM; adopt `platform()`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Platform + prune  
**Status:** done  
**Depends on:** WI-001

## Goal

Remove `io.spring.dependency-management` and `dependencyManagement { mavenBom(...) }`
from every module. Import Boot BOM via Gradle `platform(libs.boot.dependencies)`.
Pin any formerly versionless coords in the catalog (e.g. `postgresql`).

## Acceptance

- [x] No `spring.dependency.management` plugin or `dependencyManagement` blocks  
- [x] Modules that need Boot-managed alignment use `platform(libs.boot.dependencies)`  
- [x] Catalog / build still resolves (compile at least one leaf)  
