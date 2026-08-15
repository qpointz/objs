# objs-app — foundation side service

**Module:** `:objs-app`  
**Package:** `org.poc.objs.app`

Thin **foundation side service** that wires `:objs-service` + `:objs-service-ui` (workbench SPA)
and `:objs-gremlin-service` (and transitively `:objs-core` / `:objs-gremlin-core`).

**Must not be used by the SBOM inventory app.** `:sbom-service` depends only on
`objs-core` + `objs-gremlin-core` (Gradle rejects `objs-service` / `objs-service-ui` /
`objs-gremlin-service` on that product path).

| Type / resource | Purpose |
|-----------------|---------|
| `ObjsApplication` | `@SpringBootApplication` — `./gradlew :objs-app:run` |
| `application.yml` | Local defaults: in-memory H2 (`MODE=PostgreSQL`), Flyway, **port 8081** |

```bash
./gradlew :objs-app:run
curl http://localhost:8081/api/v1/objs/status
# Foundation OpenAPI: http://localhost:8081/swagger-ui.html  ·  /v3/api-docs
# Traverse (gremlin): OpenAPI tag "traverse" · POST /api/v1/objs/graph/traverse/gremlin
# Workbench Query: http://localhost:8081/workbench/query
```

SBOM inventory (separate process, **port 8080** — does not call this side service):

```bash
./gradlew :sbom-service:run
# UI: http://localhost:8080/ui/
# Domain API: /api/v1/example/sbom/** — see [`../sbom/example.md`](../sbom/example.md)
```

| Surface | Path |
|---------|------|
| Foundation graph / registry / seeds | `/api/v1/objs/**` — see [`../service/rest-api.md`](../service/rest-api.md) |
| Gremlin traverse | `POST /api/v1/objs/graph/traverse/gremlin` — see [`../graph/gremlin.md`](../graph/gremlin.md) |
| Workbench | `/workbench/**` — see [`../ui.md`](../ui.md) |

SpringDoc **3.0.3** is on the classpath. Groups include `graph`, `registry`, `seeds`, and Gremlin **`traverse`**.
