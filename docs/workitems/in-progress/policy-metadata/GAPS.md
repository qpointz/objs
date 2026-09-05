# Gaps — policy-metadata (C-32)

Close in this story’s WI-001 only. Pre-suite catalog metadata for Policy list navigation.

| #      | Topic                                  | Status       | Notes                                                                                                                                                                                                                                                                                                                                                                               |
| ------ | -------------------------------------- | ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| G-P50m | Category identity                      | **resolved** | **UUID** primary id. **displayName** for UI. **slug** key: lowercase letters only (`[a-z]+`), no spaces, no digits/punctuation. Policy stores category by **UUID** (stable ref). Slug unique in the category registry. |
| G-P51m | Category lifecycle                     | **resolved** | Create and rename allowed. **Do not delete** while any policy references the category; delete only when unreferenced (refuse otherwise)                                                                                                                                                                                                                                             |
| G-P52m | Category required on Policy            | **resolved** | Category **always required** on every Policy (create/update). Existing policies without one must be migrated/assigned before they validate.                                                                                                                                                                                                                                         |
| G-P53m | Tags shape                             | **resolved** | Non-empty list required (empty not OK). Normalize: trim + lowercase; dedupe. No max count.                                                                                                                                                                                                                                                                                          |
| G-P54m | Annotations on Policy                  | **resolved** | Objs-shaped `Map<String,String>` (same as entity/graph headers). Empty map OK. No platform-reserved keys.                                                                                                                                                                                                                                                                           |
| G-P55m | List / query API                       | **resolved** | Filter by category, tag(s), annotation containment/equals; **search by name** (case-insensitive substring). **No paging.**                                                                                                                                                                                                                                                          |
| G-P56m | CategoryRepository vs PolicyRepository | **resolved** | **Split** SPIs: `CategoryRepository` and `PolicyRepository` remain separate (no facade).                                                                                                                                                                                                                                                                                            |
| G-P57m | Workbench navigate UX                  | **resolved** | Left **category/policy tree** + tag filter. **No** annotation list filtering. Editor: **General** (category before name, description, semantic version string, tags, annotations) \| **Code**. Confirmations via Mantine Modal. Add split: Policy \| Category. **Visual styles:** existing workbench. |
| G-P58m | HTTP surface                           | **resolved** | Extend **`:objs-policy-service`**: category CRUD + policy list query params (per G-P55m); create/update carry metadata. Same opt-in wiring as C-31 (service-app).                                                                                                                                                                                                                   |
| G-P59m | Serial version + metadata              | **resolved** | User-managed **`version` string** = major.minor (e.g. `1.2`). Auto **`serial`** Long uses object head-version rule (`max(nowMillis, previous+1)`) on create/update. Full display `version · serial`. PolicyRef pin/`latest` keyed by serial (`PolicyRef.ByName.serial`). Outcomes cite `policySerial`. |

## Philosophy (locked intent)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P60m | Pre-suite, not suite | **resolved** (intent) | Metadata = group/find/navigate catalog; suites = run config + result interpretation |
| G-P61m | Categories app-managed | **resolved** (intent) | No foundation enum; users/apps CRUD vocabulary |
| G-P62m | All three fields | **resolved** (intent) | Tags + categories + annotations |

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| 1 | G-P51m Category lifecycle | 2026-09-05 | Create + rename OK. Refuse delete while any policy still references the category; delete only when unreferenced. |
| 2 | G-P52m Category required | 2026-09-05 | Category always required on every Policy. |
| 3 | G-P53m Tags shape | 2026-09-05 | Tags required non-empty; trim + lowercase; dedupe; no max count. |
| 4 | G-P54m Annotations | 2026-09-05 | Objs-shaped MapStringString; empty OK; no reserved keys. |
| 5 | G-P55m List / query API | 2026-09-05 | Category, tags, annotation filters + name search; no paging. |
| 6 | G-P56m Repositories | 2026-09-05 | Split CategoryRepository and PolicyRepository SPIs. |
| 7 | G-P57m Workbench UX | 2026-09-05 | Tree nav; General (category first, semver string) + Code; Mantine confirms; Add Policy/Category. |
| 8 | G-P58m HTTP surface | 2026-09-05 | Extend objs-policy-service: category CRUD + policy list query + metadata on write. |
| 9 | G-P59m Serial + version | 2026-09-05 | `version` = major.minor string; `serial` = timestamp; outcomes use policySerial. |
| 10 | G-P50m Category identity | 2026-09-05 | UUID primary; displayName; slug [a-z]+ only; Policy refs category by UUID. |
| 11 | G-P59m naming | 2026-09-05 | Drop semanticVersion name; version=major.minor, serial=timestamp. |
