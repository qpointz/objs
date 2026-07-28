# objs-app

**Module:** `:objs-app`  
**Package:** `org.poc.objs.app`

Thin runnable assembly that depends on `:objs-service` (and transitively `:objs-core`).

| Type / resource | Purpose |
|-----------------|---------|
| `ObjsApplication` | `@SpringBootApplication` — `./gradlew :objs-app:run` |
| `application.yml` | Local defaults: in-memory H2 (`MODE=PostgreSQL`), Flyway, port 8080 |

```bash
./gradlew :objs-app:run
curl http://localhost:8080/api/v1/objs/status
# OpenAPI: http://localhost:8080/swagger-ui.html  ·  /v3/api-docs
```

SpringDoc **3.0.3** is on the classpath (also on `:objs-service`). Endpoint catalogue: [`../service/rest-api.md`](../service/rest-api.md).
