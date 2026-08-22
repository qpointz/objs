# Matcher pushdown — FB-3 remainder (C-19)

**Status:** shipped (C-19)  
**Parent:** [annotations-and-matchers.md](annotations-and-matchers.md)  
**Sibling:** [C-20 store text search](../../workitems/planned/store-text-search/STORY.md) (contains / `q`)

## Split

| Slice | Story | Operators |
|-------|-------|-----------|
| Equality / inequality | C-17 | `==`, `!=`, `&&`, `\|\|` on `id`, `type`, `schemaVersion`, `a.*`, `p.*` |
| **Remainder (this doc)** | **C-19** | Scalar payload `>`, `>=`, `<`, `<=`; prefix via `=~ '^prefix'` |
| Text search | C-20 | Substring / `q` over identifier + searchable scalars |

`tsvector` / linguistic FTS is out for both C-19 and C-20.

## `obj-expr` pushdown (locked)

**Paths:** first-level payload keys only — `p.field` or `p['field']` (same as equality pushdown).
Nested paths (`p.a.b`) stay local eval.

**Comparisons** (string literal on RHS):

```text
p.version > '2.0'
p.severity >= 'HIGH'
p.count < '100'
```

Lowered to SQL `(payload ->> 'key') <op> ?` inside DNF AND-groups. Text ordering (lexicographic).

**Prefix** (anchored regex only):

```text
p.name =~ '^Apache'
p.ecosystem =~ '^Maven$'   // optional trailing $
```

Pushdown when the pattern is `^` + literal with **no regex metacharacters** (+ optional `$`).
SQL: `(payload ->> 'key') LIKE 'literal%' ESCAPE '\'` (Postgres + H2).

General regex / substring `=~` without `^` prefix anchor stays **local eval** (same as before).

**Not pushed:** `graph-expr` remainder (header search uses equality pushdown + free-text `q` separately).

## Catalog filter map (locked)

`BoMCatalogSupport.filterMapToObjExpr` extensions for schema-driven search forms:

| Filter value | Generated clause |
|--------------|------------------|
| `exact` | `p['field'] == 'exact'` (unchanged) |
| `prefix*` | `p['field'] =~ '^prefix'` (strip trailing `*`) |
| `>value`, `>=value`, `<value`, `<=value` | same operator on `p['field']` |

Keys remain searchable paths or `type` / `id` / `schemaVersion`. Unknown operators fall back to equality.

## Backend

- **PostgreSQL:** full pushdown in `BoMPoolEntityReader`
- **H2:** same `->>` / `LIKE` predicates for unit smoke; production bar is Postgres IT

## Consumers

| App | Usage |
|-----|-------|
| **SBOM** | Asset advanced search via `filterMapToObjExpr` |
| **AR** | Collection object list when filters use new operators |
| **Workbench** | Objects / matcher `obj-expr` benefits without UI change |

Contains / `q` API remains **C-20** — do not add substring pushdown here.
