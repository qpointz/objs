# Gremlin traversal examples

**Language:** `gremlin-lang` (Query editor may use Groovy highlighting)  
**Materialization:** `envelope` — entity `type` → vertex label; edge `role` → edge label; domain fields under `payload` / `annotations`  
**API:** `POST /api/v1/objs/graph/traverse/gremlin` — see [`gremlin.md`](gremlin.md)  
**Ontology roles:** SBOM examples use [`../sbom/canonical-spec.md`](../sbom/canonical-spec.md)

Scripts run only on the **matcher-selected** subgraph (`subgraph1`). Adjust the matcher (e.g. `{ "anno": { "app": "app-00001" } }`) so the needed types are in scope.

## Mental model

| Want | Pattern | Typical `primary` |
|------|---------|-------------------|
| Objects / Explorer-like | Return **vertices** (and/or paths) | `graph` → `subgraph` |
| Rows | `project(...).by(...)` maps | `table` |
| Count / aggregate | `count()`, `groupCount()` | `scalar` / `table` |

Payload fields:

```text
values('payload').select('name')
```

not `values('name')`.

---

## Vertices (graph result)

### All of one type

```
g.V().hasLabel('Service')
```

```
g.V().hasLabel('Service', 'Policy')
```

### Filter by payload

```
g.V().hasLabel('Service').where(values('payload').select('name').is('payments-svc'))
```

### Product that connects to a Database

Canonical edge: **Product** —`CONNECTS_TO`→ **Database**.

Products only (vertices):

```
g.V().hasLabel('Product').where(out('CONNECTS_TO').hasLabel('Database'))
```

Product **and** linked Database vertices:

```
g.V().hasLabel('Product').where(out('CONNECTS_TO').hasLabel('Database'))
  .union(identity(), out('CONNECTS_TO').hasLabel('Database'))
  .dedup()
```

### Outgoing / incoming walks

Service → APIs:

```
g.V().hasLabel('Service').out('IMPLEMENTS').hasLabel('API')
```

Product → Policies:

```
g.V().hasLabel('Product').out('COMPLIES_WITH').hasLabel('Policy')
```

Databases that a Product connects to (start from Product):

```
g.V().hasLabel('Product').out('CONNECTS_TO').hasLabel('Database')
```

### Paths

```
g.V().hasLabel('Product').outE('CONNECTS_TO').inV().hasLabel('Database').path()
```

```
g.V().hasLabel('Service').outE('IMPLEMENTS').inV().hasLabel('API').path()
```

---

## Tables

### Type + payload columns

```
g.V().hasLabel('Service', 'Policy').project('type', 'name', 'protocol')
  .by(label)
  .by(values('payload').select('name'))
  .by(coalesce(values('payload').select('protocol'), constant('')))
```

### Product → Database names

```
g.V().hasLabel('Product').where(out('CONNECTS_TO').hasLabel('Database'))
  .project('product', 'database')
  .by(values('payload').select('name'))
  .by(out('CONNECTS_TO').hasLabel('Database').values('payload').select('name'))
```

### Counts by label

```
g.V().groupCount().by(label)
```

### Full vertex maps (nested payload OK)

```
g.V().hasLabel('Service').elementMap()
```

---

## Scalars

```
g.V().hasLabel('Component').count()
```

```
g.E().hasLabel('DEPENDS_ON').count()
```

---

## REST envelope

```json
{
  "matcher": { "anno": { "app": "app-00001" } },
  "script": "g.V().hasLabel('Product').where(out('CONNECTS_TO').hasLabel('Database'))",
  "traversalOptions": { "timeoutSeconds": 60, "language": "gremlin-lang" }
}
```

Workbench: `/workbench/query` — same matcher + script; **Structured** for graph/table, **Raw** for full JSON.

## Common SBOM edge roles (subset)

| Role | From → To |
|------|-----------|
| `CONNECTS_TO` | Product → Database |
| `IMPLEMENTS` | Service → API |
| `COMPLIES_WITH` | Product → Policy |
| `CONTAINS` | Product → Component / Artifact |
| `DEPENDS_ON` | Component → Component |

Full allow-list: [`../sbom/canonical-spec.md`](../sbom/canonical-spec.md).
