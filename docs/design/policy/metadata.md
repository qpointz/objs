# Policy metadata — categories, tags, annotations, semver (C-32)

**Status:** **shipped** (api/core + `:objs-policy-service` + workbench)  
**Normative:** [policy-metadata/GAPS.md](../../workitems/completed/20260905-policy-metadata/GAPS.md)  
**Story:** [policy-metadata/STORY.md](../../workitems/completed/20260905-policy-metadata/STORY.md)  
**Boundary:** Pre-suite **catalog navigation** — not suite run configuration (C-27).

## Intent

Extend Policy artefacts so the workbench Policy list can **navigate** larger inventories: group/filter by category and tags, edit annotations, and show a user-managed version (major.minor) with timestamp serial.

## Category (user-managed vocabulary)

| Field | Lock |
|-------|------|
| `id` | **UUID** primary |
| `displayName` | UI label |
| `slug` | `[a-z]+` only (no spaces, digits, punctuation); unique in registry |

- Create + rename allowed.
- **Delete refused** while any policy references the category; delete only when unreferenced.
- **Split** `CategoryRepository` (not folded into `PolicyRepository`).

## Policy fields (additive)

| Field | Lock |
|-------|------|
| `categoryId` | UUID — **always required**; must exist in category registry |
| `tags` | Non-empty `List<String>`; trim + lowercase; dedupe; no max count |
| `description` | Human-readable text; empty OK |
| `annotations` | Objs-shaped `Map<String,String>`; empty OK; no reserved keys |
| `version` | User-managed major.minor string (e.g. `1.2`) |
| `serial` | Timestamp long — same rule as object head versions: `max(nowMillis, previous + 1)` on create/update |

**Display:** `version · serial`. PolicyRef pin / `latest` keyed by **serial**.

## List / query

Filter by category, tag(s), annotation containment/equals; **name** search (case-insensitive substring). **No paging.**

## HTTP (`:objs-policy-service`)

Category CRUD + policy list query params + metadata on create/update. Same opt-in wiring as C-31 (`:objs-service-app`).

## Workbench

- Toolbar filters: **category** + **tags** (no annotation list filter); name search on the left tree.
- Left nav: **category / policy tree**; selecting a category lists its policies in the content pane (**Delete** targets the category); selecting a policy opens the editor (**Delete** targets the policy).
- Editor tabs: **General** (category, name, description, version major.minor, tags, annotations) | **Code** (body).
- Confirmations: Mantine `Modal` dialogs (not `window.confirm`).
- Add: split menu **Policy** | **Category**.

See also [`workbench.md`](workbench.md) (C-31 base chrome).
