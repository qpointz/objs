# SBOM applications inventory

Runnable example under `examples/sbom/`: **`:sbom-service`** (API) + **`:sbom-service-ui`** (browser UI).

Uses **`objs-core` + `objs-gremlin-core`** at compile time. Foundation workbench/REST
(`:objs-service`, `:objs-service-ui`, `:objs-gremlin-service`) are **`runtimeOnly`** so a demo
Workbench can run **side by side** on the same process. Inventory stays fully functional if those
jars are removed (rebuild required; `/workbench/` then 404s). Gradle still forbids
`implementation` of those modules.

## Run

```bash
./gradlew :sbom-service:run
```

| Surface | URL |
|---------|-----|
| Inventory UI (Mantine) | http://localhost:8080/sbom/ |
| Workbench (when runtime jars present) | http://localhost:8080/workbench/ |
| Domain OpenAPI | http://localhost:8080/swagger-ui.html |

The `demo` profile loads ontology seeds and a **70-application** Meridian Financial Group inventory (Java, Python, and web LOB apps with 1–5 versions, Maven/npm/PyPI pins, org/runtime/deploy graph, portfolio taxonomy).

`:objs-service-app` remains an optional **separate** workbench process (port **8081**) if you prefer not to use the in-process sidecar.

## What you see

| Tab | Who it’s for | What to do |
|-----|--------------|------------|
| **Applications** | Application owner | Search apps, edit a draft, create a version, browse **Assets** |
| **Portfolios** | Portfolio owner | Maintain taxonomy, then **Reports**: portfolio → level → report → **Run** |

There is **no** Reports entry under Applications.

### Applications

1. Create or open an application.  
2. Add assets and relations on the **edit draft**.  
3. **Create version** when ready.  
4. Under **Assets**, search by type (searchable fields only), inspect usage, set owner, find duplicates.

### Portfolios

1. **Taxonomy** — create a portfolio and subject areas; place applications (demo data includes *Retail platform*).  
2. **Reports** — pick level (root or subject area) → pick MI-1…MI-4 → **Run** (composition, dependency map, shared assets, duplicate/risk signals).

## Notes

- Product language only in the UI (application, asset, relation, portfolio, report).  
- Workbench / `/api/v1/objs/**` are a **demo sidecar** on the same JVM when foundation jars are on the runtime classpath. Domain UI and domain REST do not call them.  
- CycloneDX export is a **weak demo** (Applications tab → Export links). Not certified.  
