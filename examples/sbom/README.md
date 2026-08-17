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

The `demo` profile loads ontology seeds and a **70-application** Meridian Financial Group inventory (Java, Python, and web LOB apps). About half of the apps have a single **BOM**; the rest have 2–3 (`Build` / `Runtime` / `Image`). A few apps have **parallel drafts**. Fingerprints use **name** + **category** (`approval` / `history` / `unknown`). Portal cards lazy-load latest RELEASED, a multi-BOM cue, and BOM/version totals.

`:objs-service-app` remains an optional **separate** workbench process (port **8081**) if you prefer not to use the in-process sidecar.

## What you see

| Tab | Who it’s for | What to do |
|-----|--------------|------------|
| **Applications** | Application owner | Search apps, compose **BOMs**, parallel **drafts**, fingerprints, browse **Assets** |
| **Portfolios** | Portfolio owner | Maintain taxonomy, then **Reports**: portfolio → level → report → **Run** |

There is **no** Reports entry under Applications.

### Applications

1. Create or open an application (required **target version**).  
2. Edit a **BOM** on a DRAFT (count = 1 looks like a single bill; **Create BOM** adds more).  
3. With ≥ 2 BOMs: **Combined SBOM** (select all, read-only) and left-pane multi-select.  
4. **New draft** from a released/draft version or fingerprint; **Promote** by re-typing the version; **Fingerprint** with name + category.  
5. Under **Assets**, search by type (searchable fields only), inspect usage, set owner, find duplicates.

### Portfolios

1. **Taxonomy** — create a portfolio and subject areas; place applications (demo data includes *Retail platform*).  
2. **Reports** — pick level (root or subject area) → pick MI-1…MI-4 → **Run** (composition, dependency map, shared assets, duplicate/risk signals).

## Notes

- Product language only in the UI (application, version, BOM, Combined SBOM, fingerprint, asset, relation, portfolio, report).  
- Workbench / `/api/v1/objs/**` are a **demo sidecar** on the same JVM when foundation jars are on the runtime classpath. Domain UI and domain REST do not call them.  
- CycloneDX **export API** remains; the application-detail download link is **hidden**. Not certified.  
