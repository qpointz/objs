# Synthetic load-data

qsynth model + committed CSV extract + REST loader for the asset-repository demo ontology.

- Domain API only: `/api/v1/asset-repository/**`
- Base `rows` in [`asset-repository.yaml`](asset-repository.yaml) match the classpath demo mix
- Scale volume with **`rows_multiply` only** (do not retune per-table `rows`)
- Committed [`generated/`](generated/) is `rows_multiply: 1.0` — load it without Docker or qsynth

| Slice | Base objects (`rows_multiply: 1.0`) |
|-------|-------------------------------------|
| datasets | 50 |
| models | 20 |
| agents | 100 |
| composables | 200 |
| mcp-servers | 142 |
| customer-support | 142 |

Identifiers are prefixed `syn-` on load so they do not collide with demo seed objects. Collections are found by name or created. `load.py` posts objects **per collection** and edges in `--batch-size` composition requests (default 200); do not post one relation per HTTP call.

## Load default extract (no Docker)

Start the app (`./gradlew :asset-repository-service:run`), then from the repo root:

```bash
python examples/asset-repository/demo/load-data/load.py --base-url http://localhost:8080
```

Dry-run (counts only; still needs a reachable API for collection list):

```bash
python examples/asset-repository/demo/load-data/load.py --dry-run
```

## Regenerate CSVs with Docker

Use **`qpointz/qsynth:latest`**. Mount this folder at `/data` (image `WORKDIR` is `/data`). Writes overwrite [`generated/*.csv`](generated/).

### Bash / Git Bash

```bash
cd examples/asset-repository/demo/load-data
docker pull qpointz/qsynth:latest
docker run --rm -v "$(pwd):/data" qpointz/qsynth:latest run --input-file /data/asset-repository.yaml --experiment write_csv
```

### PowerShell

```powershell
cd examples/asset-repository/demo/load-data
docker pull qpointz/qsynth:latest
docker run --rm -v "${PWD}:/data" qpointz/qsynth:latest run --input-file /data/asset-repository.yaml --experiment write_csv
```

If `${PWD}` does not mount correctly, use an absolute path:

```powershell
docker run --rm -v "C:\Users\vm\wip\objs-agent\examples\asset-repository\demo\load-data:/data" qpointz/qsynth:latest run --input-file /data/asset-repository.yaml --experiment write_csv
```

### Scale volume

1. Edit `rows_multiply` at the top of `asset-repository.yaml` (for example `0.5`, `2`, or `10`).
2. Re-run the `docker run` command above.
3. Load again with `load.py`.

Optional: preview without writing files:

```bash
docker run --rm -v "$(pwd):/data" qpointz/qsynth:latest preview /data/asset-repository.yaml --rows 5
```
