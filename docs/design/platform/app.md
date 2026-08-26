# objs-service-app — workbench runner

**Module:** `:objs-service-app`  
**Path:** `objs-service-app/`  
**Package:** `org.poc.objs.app`

Thin **workbench-only** runnable that wires `:objs-service` + `:objs-service-ui` (workbench SPA)
and `:objs-gremlin-service` (and transitively `:objs-core` / `:objs-gremlin-core`).

It **must not** depend on `examples/` or any other concrete product. Example apps are separate
launchables and do not use this module.

| Type / resource | Purpose |
|-----------------|---------|
| `ObjsApplication` | `@SpringBootApplication` — `./gradlew :objs-service-app:run` |
| `application.yml` | Local defaults: in-memory H2 (`MODE=PostgreSQL`), **objs Flyway** (`bom_*`), Boot Flyway **off**, **port 8081** |

```bash
./gradlew :objs-service-app:run
curl http://localhost:8081/api/v1/objs/status
# Foundation OpenAPI: http://localhost:8081/swagger-ui.html  ·  /v3/api-docs
# Traverse (gremlin): OpenAPI tag "traverse" · POST /api/v1/objs/graph/traverse/gremlin
# Workbench Query: http://localhost:8081/workbench/query
```

SBOM inventory (separate process, **port 8080** — does not call this runner):

```bash
./gradlew :sbom-service:run
# UI: http://localhost:8080/sbom/
# Domain API: /api/v1/inventory/** — see [`../sbom/example.md`](../sbom/example.md)
```

| Surface | Path |
|---------|------|
| Foundation graph / registry / seeds | `/api/v1/objs/**` — see [`../service/rest-api.md`](../service/rest-api.md) |
| Gremlin traverse | `POST /api/v1/objs/graph/traverse/gremlin` — see [`../graph/gremlin.md`](../graph/gremlin.md) |
| Workbench | `/workbench/**` — see [`../ui.md`](../ui.md) |

SpringDoc **3.0.3** is on the classpath. Groups include `graph`, `registry`, `seeds`, and Gremlin **`traverse`**.
