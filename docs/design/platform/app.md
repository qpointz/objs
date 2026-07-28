# objs-app

**Module:** `:objs-app`  
**Package:** `org.poc.objs.app`

Thin runnable assembly that depends on `:objs-service`, `:objs-sbom-example` (and transitively `:objs-core`).

| Type / resource | Purpose |
|-----------------|---------|
| `ObjsApplication` | `@SpringBootApplication` — `./gradlew :objs-app:run` |
| `application.yml` | Local defaults: in-memory H2 (`MODE=PostgreSQL`), Flyway, port 8080; `objs.sbom.demo-seed: true` |

```bash
./gradlew :objs-app:run
curl http://localhost:8080/api/v1/objs/status
# Foundation OpenAPI: http://localhost:8080/swagger-ui.html  ·  /v3/api-docs
# Example SBOM group: select "example-sbom" in Swagger UI

# Demo seed (when objs.sbom.demo-seed=true):
curl "http://localhost:8080/api/v1/example/sbom/apps/payments-api/versions/2.3.1"
curl "http://localhost:8080/api/v1/example/sbom/apps/billing-api"
```

| Surface | Path |
|---------|------|
| Foundation graph / registry | `/api/v1/objs/**` — see [`../service/rest-api.md`](../service/rest-api.md) |
| SBOM example app | `/api/v1/example/sbom/**` — see [`../sbom/example.md`](../sbom/example.md) |

On startup the SBOM module registers the full canonical ontology schemas/edge rules and (if `objs.sbom.demo-seed=true`) loads a small sample graph for `payments-api@2.3.1` and `billing-api@1.0.0`.

SpringDoc **3.0.3** is on the classpath. Groups: `graph`, `registry`, `example-sbom`.

The **example-sbom** group publishes domain payload schemas dynamically from `BoMSchemaCatalog`
(names like `Component.1.0.0`) into OpenAPI **Schemas**, plus an allow-list summary in the API description.
