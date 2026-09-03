# WI-004 — objs-autoconfigure module

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Autoconfigure  
**Status:** done — [`fb52223`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/fb52223)  
**Depends on:** WI-003 (done)  
**Examples:** **—**

## Goal

Finish `:objs-autoconfigure` Boot wiring. A compile scaffold landed in WI-003; this WI closes the remaining Boot contract.

## Scope

- [x] Confirm `settings.gradle.kts` includes `:objs-autoconfigure` (scaffold from WI-003)
- [x] Confirm `api(project(":objs-core"))` + Boot starter / data-jpa / flyway (scaffold from WI-003)
- [x] Confirm autoconfig classes live under `org.poc.objs.autoconfigure.*` (scaffold from WI-003)
- [x] Confirm `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (scaffold from WI-003)
- [x] Confirm DAOs, stores, validator, catalogs, seed loader registered as `@Bean`s (scaffold from WI-003)
- [x] Spring UoW adapter (`TransactionTemplate` → core internal UoW) — [`TransactionTemplateUnitOfWork`](../../../../objs-autoconfigure/src/main/kotlin/org/poc/objs/autoconfigure/TransactionTemplateUnitOfWork.kt)
- [x] Confirm `@EntityScan` for `org.poc.objs.core.persistence`; **no** `@EnableJpaRepositories` (scaffold from WI-003)
- [x] `@EnableTransactionManagement`
- [x] Confirm `@ConfigurationProperties` binding for catalog, flyway, seed settings (scaffold from WI-003)
- [x] Minimal autoconfigure test that beans wire (`ObjsAutoconfigureWiringTest`; expanded in WI-006)

## Out of scope

- objs-service / example Gradle (WI-005)
- Living docs (WI-007)

## Acceptance

- `./gradlew :objs-autoconfigure:compileKotlin`
- objs-core JAR has no `META-INF/spring/*` autoconfig entries
- Minimal autoconfigure test proves beans wire (expanded in WI-006)
