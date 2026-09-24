# OpenAPI client codegen smoke (C-42)

Manual harness: generate **Java** and **Python** clients from live springdoc group docs
and verify they **compile**. Generated sources are **not** committed.

## Prerequisites

- A running app that exposes `/v3/api-docs/{group}`  
  - Workbench: `./gradlew :objs-service-app:run` → **http://localhost:8081**  
  - SBOM / AR examples: port **8080** by default  
- Java 17+  
- Maven (`mvn`) — used only to compile the generated Java client  
- Python 3.10+  

The harness downloads pinned **openapi-generator-cli 7.25.0** into `.tmp/openapi-generator/` (gitignored).

## Primary groups (full generate + compile)

From repo root, with workbench up:

```bash
python scripts/openapi-client-codegen-smoke.py --base http://localhost:8081
```

Default `--base` is `http://localhost:8080`. Override with env `OBJS_OPENAPI_BASE`.

Groups default to: `graph`, `registry`, `policy`.

Keep output for inspection:

```bash
python scripts/openapi-client-codegen-smoke.py --base http://localhost:8081 --keep .tmp/openapi-codegen-out
```

## SBOM / AR readiness (generate only)

```bash
python scripts/openapi-client-codegen-smoke.py --base http://localhost:8080 \
  --groups inventory asset-repository --readiness-only
```

## Success bar

- Specs download without error  
- `openapi-generator-cli` finishes for Java + Python  
- Java: `mvn -DskipTests compile` in the generated project  
- Python: `python -m compileall` on the generated tree  

No generated code belongs in git.
