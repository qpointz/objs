#!/usr/bin/env python3
"""
SBOM REST helper: create / retrieve / update / delete graph objects.

Uses the example SBOM API for app-scoped upserts and fetches, and the
foundation graph API for batch delete.

Defaults to http://localhost:8080 (running objs-app).

Examples:
  python random_sbom_crud.py status
  python random_sbom_crud.py apps
  python random_sbom_crud.py seed --app demo-app --version 1.0.0 --entities 24 --edges 18
  python random_sbom_crud.py bulk
  python random_sbom_crud.py bulk --apps 5000
  python random_sbom_crud.py bulk --apps 100 --max-versions 5 --min-entities 8 --max-entities 20
  python random_sbom_crud.py bulk --tiny --apps 20000
  python random_sbom_crud.py bulk --app-number 500 --max-versions-per-app 10 --workers 16
  python random_sbom_crud.py bulk --apps 5000 --no-create-graphs
  python random_sbom_crud.py get --app demo-app --version 1.0.0
  python random_sbom_crud.py update --app demo-app --version 1.0.0 --count 3
  python random_sbom_crud.py delete --app demo-app --version 1.0.0 --entities 2 --edges 1
  python random_sbom_crud.py demo --app demo-app --version 1.0.0
"""

from __future__ import annotations

import argparse
import json
import random
import string
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any

SCHEMA_VERSION = "1.0.0"
EDGE_TYPE = "CanonicalEdge"
DEFAULT_BASE = "http://localhost:8080"
DEFAULT_TIMEOUT_S = 120

# Realistic provenance values. Randomized per entity, so objects inside the same
# app@version carry a mix of sources and origins.
CAPTURE_SOURCES = ("manual", "detected", "enriched")
ORIGINS = ("ui", "api", "ci-pipeline", "dependency-scanner", "catalog-sync")
CAPTURED_BY = ("alice", "bob", "carol", "dave", "erin")
DETECTORS = ("syft", "trivy", "grype", "cyclonedx-maven-plugin", "npm-audit")
CATALOGS = ("deps-dev", "ossindex", "nvd", "internal-catalog")

# Canonical relationship allow-list (source_type, role, target_type)
EDGE_RULES: list[tuple[str, str, str]] = [
    ("Product", "CONTAINS", "Component"),
    ("Product", "CONTAINS", "Artifact"),
    ("Container Image", "CONTAINS", "Container Layer"),
    ("Database", "CONTAINS", "Dataset"),
    ("Source Repository", "CONTAINS", "Source Module"),
    ("Component", "DEPENDS_ON", "Component"),
    ("Source Module", "PRODUCES", "Artifact"),
    ("Build", "BUILDS", "Artifact"),
    ("Build", "BUILDS", "Container Image"),
    ("Build", "USES", "Component"),
    ("Container Image", "PACKAGES", "Artifact"),
    ("Container Image", "BASED_ON", "Operating System"),
    ("Product", "RUNS_ON", "Runtime"),
    ("Runtime", "RUNS_ON", "Operating System"),
    ("Deployment", "DEPLOYS", "Container Image"),
    ("Deployment", "TARGETS", "Environment"),
    ("Deployment", "RUNS_ON", "Host"),
    ("Host", "MEMBER_OF", "Kubernetes Cluster"),
    ("Deployment", "LOCATED_IN", "Namespace"),
    ("Service", "IMPLEMENTS", "API"),
    ("Product", "CALLS", "API"),
    ("Product", "CONNECTS_TO", "Database"),
    ("Component", "PROVIDED_BY", "Organization"),
    ("Product", "OWNED_BY", "Organization"),
    ("Component", "LICENSED_UNDER", "License"),
    ("Component", "HAS_VULNERABILITY", "Vulnerability"),
    ("Container Image", "HAS_VULNERABILITY", "Vulnerability"),
    ("Product", "COMPLIES_WITH", "Policy"),
]


def _token(n: int = 6) -> str:
    return "".join(random.choices(string.ascii_lowercase + string.digits, k=n))


def _name(prefix: str) -> str:
    return f"{prefix}-{_token()}"


def _payload(type_name: str) -> dict[str, Any]:
    """Minimal valid payload for each canonical entity type."""
    n = _name(type_name.replace(" ", "-").lower())
    makers: dict[str, Any] = {
        "Product": lambda: {
            "name": n,
            "version": f"{random.randint(1, 9)}.{random.randint(0, 9)}.{random.randint(0, 9)}",
            "lifecycle": random.choice(["ga", "beta", "eol"]),
        },
        "Component": lambda: {
            "name": n,
            "version": f"{random.randint(1, 5)}.{random.randint(0, 20)}.{random.randint(0, 9)}",
            "ecosystem": random.choice(["Maven", "npm", "PyPI", "Go"]),
            "kind": random.choice(["library", "framework", "application"]),
        },
        "Organization": lambda: {"name": n, "domain": f"{_token()}.example"},
        "License": lambda: {
            "name": n,
            "spdxId": random.choice(["Apache-2.0", "MIT", "GPL-3.0-only", "BSD-3-Clause"]),
        },
        "Vulnerability": lambda: {
            "name": n,
            "cve": f"CVE-2026-{random.randint(1000, 9999)}",
            "severity": random.choice(["LOW", "MEDIUM", "HIGH", "CRITICAL"]),
            "cvss": round(random.uniform(1.0, 10.0), 1),
        },
        "Build": lambda: {
            "name": n,
            "buildNumber": str(random.randint(1, 9999)),
            "status": random.choice(["success", "failed", "running"]),
            "builder": random.choice(["jenkins", "github-actions", "gitlab-ci"]),
        },
        "Source Repository": lambda: {
            "name": n,
            "url": f"https://git.example/{_token()}",
            "branch": random.choice(["main", "dev", "release"]),
        },
        "Source Module": lambda: {
            "name": n,
            "path": f"modules/{_token()}",
            "language": random.choice(["Kotlin", "Java", "TypeScript", "Python"]),
        },
        "Artifact": lambda: {
            "name": f"{n}.jar",
            "artifactType": random.choice(["jar", "war", "zip", "wheel"]),
            "checksum": f"sha256:{_token(16)}",
        },
        "Container Image": lambda: {
            "name": n,
            "tag": f"{random.randint(1, 3)}.{random.randint(0, 9)}.{random.randint(0, 9)}",
            "registry": "registry.example",
        },
        "Container Layer": lambda: {
            "name": n,
            "digest": f"sha256:{_token(24)}",
            "size": random.randint(1_000_000, 80_000_000),
        },
        "Runtime": lambda: {
            "name": n,
            "runtimeType": random.choice(["jvm", "nodejs", "python", "dotnet"]),
            "version": f"{random.randint(11, 22)}",
        },
        "Operating System": lambda: {
            "name": n,
            "distribution": random.choice(["alpine", "debian", "ubuntu", "rhel"]),
            "version": f"{random.randint(10, 24)}.04",
            "architecture": random.choice(["amd64", "arm64"]),
        },
        "Deployment": lambda: {
            "name": n,
            "status": random.choice(["running", "pending", "failed"]),
            "replicas": random.randint(1, 5),
        },
        "Environment": lambda: {
            "name": n,
            "environment": random.choice(["dev", "staging", "production"]),
        },
        "Host": lambda: {
            "name": n,
            "hostname": f"{_token()}.local",
            "ip": f"10.{random.randint(0, 255)}.{random.randint(0, 255)}.{random.randint(1, 254)}",
        },
        "Kubernetes Cluster": lambda: {
            "name": n,
            "version": f"1.{random.randint(25, 31)}.0",
        },
        "Namespace": lambda: {"name": n, "namespace": _token()},
        "Service": lambda: {
            "name": n,
            "protocol": random.choice(["http", "grpc", "tcp"]),
            "endpoint": f"https://{_token()}.example/api",
        },
        "API": lambda: {
            "name": n,
            "protocol": random.choice(["https", "grpc", "graphql"]),
            "version": f"v{random.randint(1, 3)}",
        },
        "Database": lambda: {
            "name": n,
            "engine": random.choice(["postgres", "mysql", "mongodb", "redis"]),
            "version": f"{random.randint(12, 16)}",
        },
        "Dataset": lambda: {
            "name": n,
            "datasetType": random.choice(["table", "collection", "topic"]),
            "classification": random.choice(["public", "internal", "confidential"]),
        },
        "Policy": lambda: {
            "name": n,
            "policyType": random.choice(["Security", "Compliance"]),
            "version": f"{random.randint(1, 5)}.0",
        },
    }
    if type_name not in makers:
        raise KeyError(f"Unknown type: {type_name}")
    payload = makers[type_name]()
    payload["description"] = f"random {_token()}"
    return payload


ALL_TYPES = list(
    {
        "Product",
        "Component",
        "Organization",
        "License",
        "Vulnerability",
        "Build",
        "Source Repository",
        "Source Module",
        "Artifact",
        "Container Image",
        "Container Layer",
        "Runtime",
        "Operating System",
        "Deployment",
        "Environment",
        "Host",
        "Kubernetes Cluster",
        "Namespace",
        "Service",
        "API",
        "Database",
        "Dataset",
        "Policy",
    }
)


class ObjsClient:
    def __init__(self, base_url: str, timeout_s: float = DEFAULT_TIMEOUT_S) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout_s = timeout_s

    def _request(
        self,
        method: str,
        path: str,
        *,
        query: dict[str, str] | None = None,
        body: Any = None,
        expect_json: bool = True,
        raise_http: bool = True,
    ) -> Any:
        url = f"{self.base_url}{path}"
        if query:
            url = f"{url}?{urllib.parse.urlencode(query)}"
        data = None
        headers = {"Accept": "application/json"}
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        req = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=self.timeout_s) as resp:
                raw = resp.read()
                if not expect_json or resp.status == 204 or not raw:
                    return {"status": resp.status}
                return json.loads(raw.decode("utf-8"))
        except urllib.error.HTTPError as e:
            err_body = e.read().decode("utf-8", errors="replace")
            try:
                parsed = json.loads(err_body) if err_body else None
            except json.JSONDecodeError:
                parsed = err_body
            if raise_http:
                raise SystemExit(f"{method} {url} -> HTTP {e.code}: {parsed}") from e
            return {"_error": True, "status": e.code, "body": parsed, "url": url}
        except urllib.error.URLError as e:
            if raise_http:
                raise SystemExit(f"Cannot reach {url}: {e.reason}") from e
            return {"_error": True, "status": 0, "body": str(e.reason), "url": url}

    def status(self) -> Any:
        return self._request("GET", "/api/v1/objs/status")

    def sbom_list_apps(self) -> Any:
        return self._request("GET", "/api/v1/example/sbom/apps")

    def sbom_put(
        self,
        app: str,
        version: str,
        graph: dict[str, Any],
        *,
        origin: str | None = None,
        source: str | None = None,
        raise_http: bool = True,
    ) -> Any:
        app_q = urllib.parse.quote(app, safe="")
        ver_q = urllib.parse.quote(version, safe="")
        selected_origin = origin or random.choice(ORIGINS)
        selected_source = source or random.choice(CAPTURE_SOURCES)
        return self._request(
            "PUT",
            f"/api/v1/example/sbom/apps/{app_q}/versions/{ver_q}",
            query={"origin": selected_origin, "source": selected_source},
            body=graph,
            raise_http=raise_http,
        )

    def sbom_get(self, app: str, version: str | None = None) -> Any:
        app_q = urllib.parse.quote(app, safe="")
        if version is None:
            return self._request("GET", f"/api/v1/example/sbom/apps/{app_q}")
        ver_q = urllib.parse.quote(version, safe="")
        return self._request("GET", f"/api/v1/example/sbom/apps/{app_q}/versions/{ver_q}")

    def entity_create(self, entity: dict[str, Any], *, raise_http: bool = True) -> Any:
        """POST /api/v1/objs/entities — pool only (no bom_graph membership)."""
        body = {
            "id": entity.get("id"),
            "type": entity["type"],
            "schemaVersion": entity.get("schemaVersion") or SCHEMA_VERSION,
            "payload": entity.get("payload") or {},
            "annotations": entity.get("annotations") or {},
        }
        return self._request("POST", "/api/v1/objs/entities", body=body, raise_http=raise_http)

    def graph_delete(
        self,
        *,
        entity_ids: list[str] | None = None,
        edge_ids: list[str] | None = None,
    ) -> Any:
        return self._request(
            "DELETE",
            "/api/v1/objs/graph",
            body={"entityIds": entity_ids or [], "edgeIds": edge_ids or []},
            expect_json=False,
        )


def random_provenance() -> dict[str, str]:
    """Provenance annotations for a single object: source, origin, and matching detail."""
    source = random.choice(CAPTURE_SOURCES)
    provenance = {"source": source, "origin": random.choice(ORIGINS)}
    if source == "manual":
        provenance["capturedBy"] = random.choice(CAPTURED_BY)
    elif source == "detected":
        provenance["sourceDetail"] = random.choice(DETECTORS)
    else:
        provenance["sourceDetail"] = random.choice(CATALOGS)
    return provenance


def make_entity(type_name: str, annotations: dict[str, str] | None = None) -> dict[str, Any]:
    return {
        "id": str(uuid.uuid4()),
        "type": type_name,
        "schemaVersion": SCHEMA_VERSION,
        "payload": _payload(type_name),
        # Per-entity provenance; overrides the request-level defaults server-side.
        "annotations": {**random_provenance(), **(annotations or {})},
    }


def make_edge(source_id: str, target_id: str, role: str) -> dict[str, Any]:
    return {
        "id": str(uuid.uuid4()),
        "source": source_id,
        "target": target_id,
        "role": role,
        "type": EDGE_TYPE,
        "schemaVersion": SCHEMA_VERSION,
        "properties": {},
    }


def build_random_graph(entity_count: int, edge_count: int) -> dict[str, Any]:
    """Build a random but allow-list-valid BoMGraph."""
    # Bias toward types that participate in many relationships
    weighted = ALL_TYPES + ["Component", "Product", "Artifact", "Container Image"] * 2
    entities: list[dict[str, Any]] = []
    by_type: dict[str, list[dict[str, Any]]] = {t: [] for t in ALL_TYPES}

    # Ensure at least one of each type when count is large enough
    types_cycle = ALL_TYPES[:]
    random.shuffle(types_cycle)
    for i in range(entity_count):
        t = types_cycle[i] if i < len(types_cycle) else random.choice(weighted)
        ent = make_entity(t)
        entities.append(ent)
        by_type[t].append(ent)

    edges: list[dict[str, Any]] = []
    usable_rules = [
        (s, r, t)
        for s, r, t in EDGE_RULES
        if by_type[s] and by_type[t] and (s != t or len(by_type[s]) >= 2)
    ]
    if not usable_rules:
        return {"entities": entities, "edges": edges}

    attempts = 0
    seen: set[tuple[str, str, str]] = set()
    while len(edges) < edge_count and attempts < edge_count * 20:
        attempts += 1
        src_type, role, tgt_type = random.choice(usable_rules)
        src = random.choice(by_type[src_type])
        tgt = random.choice(by_type[tgt_type])
        if src["id"] == tgt["id"]:
            continue
        key = (src["id"], role, tgt["id"])
        if key in seen:
            continue
        seen.add(key)
        edges.append(make_edge(src["id"], tgt["id"], role))

    return {"entities": entities, "edges": edges}


def build_tiny_graph(app: str, version: str) -> dict[str, Any]:
    """Minimal valid BOM (Product CONTAINS Component) for fast bulk load."""
    product = make_entity("Product")
    product["payload"]["name"] = app
    product["payload"]["version"] = version
    component = make_entity("Component")
    component["payload"]["name"] = f"{app}-lib"
    component["payload"]["version"] = version
    return {
        "entities": [product, component],
        "edges": [make_edge(product["id"], component["id"], "CONTAINS")],
    }


def build_bulk_graph(
    app: str,
    version: str,
    *,
    tiny: bool,
    min_entities: int,
    max_entities: int,
    min_edges: int,
    max_edges: int,
) -> dict[str, Any]:
    """Per-version graph: tiny 2-node stub or random ontology objects/edges (seed/demo style)."""
    if tiny:
        return build_tiny_graph(app, version)
    entity_count = random.randint(min_entities, max_entities)
    edge_count = random.randint(min_edges, max_edges)
    graph = build_random_graph(entity_count, edge_count)
    # Stamp Product payloads with this app@version when present
    for ent in graph["entities"]:
        if ent.get("type") == "Product":
            payload = ent.setdefault("payload", {})
            payload["name"] = app
            payload["version"] = version
    return graph


def _version_label(index: int) -> str:
    major = 1 + (index // 100)
    minor = (index // 10) % 10
    patch = index % 10
    return f"{major}.{minor}.{patch}"


def _stamp_app_version(graph: dict[str, Any], app: str, version: str) -> None:
    """Stamp BOM identity annotations on every entity (pool-only path; SBOM PUT also stamps server-side)."""
    for ent in graph.get("entities") or []:
        ann = dict(ent.get("annotations") or {})
        ann["app"] = app
        ann["appVersion"] = version
        ent["annotations"] = ann


def cmd_bulk(client: ObjsClient, args: argparse.Namespace) -> None:
    """Create many apps with 1..N versions each; each version is a random graph by default.

    By default each successful app@version write creates one ``bom_graph`` header
    (annotations ``app`` + ``appVersion``) via SBOM PUT → SbomService.ensureGraph.
    Pass ``--no-create-graphs`` to write entities into the pool only (no headers, no edges).
    """
    if args.entities is not None:
        args.min_entities = args.entities
        args.max_entities = args.entities
    if args.edges is not None:
        args.min_edges = args.edges
        args.max_edges = args.edges
    if args.min_versions < 1 or args.max_versions < args.min_versions:
        raise SystemExit("--min-versions/--max-versions invalid")
    if args.apps < 1:
        raise SystemExit("--apps must be >= 1")
    if args.min_entities < 1 or args.max_entities < args.min_entities:
        raise SystemExit("--min-entities/--max-entities invalid")
    if args.min_edges < 0 or args.max_edges < args.min_edges:
        raise SystemExit("--min-edges/--max-edges invalid")

    prefix = args.prefix
    create_graphs = bool(args.create_graphs)
    jobs: list[tuple[str, str]] = []
    for i in range(args.apps):
        app = f"{prefix}{i:05d}"
        n_versions = random.randint(args.min_versions, args.max_versions)
        for v in range(n_versions):
            jobs.append((app, _version_label(v)))

    total = len(jobs)
    mode = (
        "tiny (2 nodes)"
        if args.tiny
        else (
            f"random graphs "
            f"(entities {args.min_entities}-{args.max_entities}, "
            f"edges {args.min_edges}-{args.max_edges})"
        )
    )
    graph_mode = (
        "one bom_graph per app@version (SBOM PUT)"
        if create_graphs
        else "pool entities only (--no-create-graphs; no edges)"
    )
    print(
        f"bulk: {args.apps} apps, {total} app@version jobs "
        f"(versions {args.min_versions}-{args.max_versions}), "
        f"{mode}, {graph_mode}, workers={args.workers}",
        flush=True,
    )
    started = time.time()
    ok = 0
    failed = 0
    errors: list[str] = []

    def put_one(job: tuple[str, str]) -> tuple[bool, str]:
        app, version = job
        graph = build_bulk_graph(
            app,
            version,
            tiny=args.tiny,
            min_entities=args.min_entities,
            max_entities=args.max_entities,
            min_edges=args.min_edges,
            max_edges=args.max_edges,
        )
        if create_graphs:
            result = client.sbom_put(app, version, graph, raise_http=False)
            if isinstance(result, dict) and result.get("_error"):
                return False, f"{app}@{version}: {result.get('status')} {result.get('body')}"
            return True, f"{app}@{version}"

        _stamp_app_version(graph, app, version)
        for ent in graph.get("entities") or []:
            result = client.entity_create(ent, raise_http=False)
            if isinstance(result, dict) and result.get("_error"):
                return False, f"{app}@{version}: {result.get('status')} {result.get('body')}"
        return True, f"{app}@{version}"

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = [pool.submit(put_one, job) for job in jobs]
        done = 0
        report_every = max(1, total // 50)
        for fut in as_completed(futures):
            success, detail = fut.result()
            done += 1
            if success:
                ok += 1
            else:
                failed += 1
                if len(errors) < 20:
                    errors.append(detail)
            if done % report_every == 0 or done == total:
                elapsed = time.time() - started
                rate = done / elapsed if elapsed > 0 else 0
                print(
                    f"  progress {done}/{total} ok={ok} fail={failed} "
                    f"({rate:.1f} jobs/s, {elapsed:.0f}s)",
                    flush=True,
                )

    elapsed = time.time() - started
    summary = {
        "action": "bulk",
        "apps": args.apps,
        "jobs": total,
        "createGraphs": create_graphs,
        "mode": "tiny" if args.tiny else "random",
        "ok": ok,
        "failed": failed,
        "seconds": round(elapsed, 1),
        "jobsPerSecond": round(ok / elapsed, 2) if elapsed > 0 else 0,
        "errorsSample": errors,
    }
    print(json.dumps(summary, indent=2))
    if failed:
        raise SystemExit(1)


def cmd_apps(client: ObjsClient, _args: argparse.Namespace) -> None:
    print(json.dumps(client.sbom_list_apps(), indent=2))


def cmd_status(client: ObjsClient, _args: argparse.Namespace) -> None:
    print(json.dumps(client.status(), indent=2))


def cmd_seed(client: ObjsClient, args: argparse.Namespace) -> None:
    graph = build_random_graph(args.entities, args.edges)
    result = client.sbom_put(args.app, args.version, graph)
    print(
        json.dumps(
            {
                "action": "create",
                "app": args.app,
                "version": args.version,
                "entities": len(result.get("entities", [])),
                "edges": len(result.get("edges", [])),
                "entityIds": [e.get("id") for e in result.get("entities", [])],
            },
            indent=2,
        )
    )


def cmd_get(client: ObjsClient, args: argparse.Namespace) -> None:
    subgraph = client.sbom_get(args.app, None if args.all_versions else args.version)
    if args.summary:
        print(
            json.dumps(
                {
                    "entities": len(subgraph.get("entities", [])),
                    "edges": len(subgraph.get("edges", [])),
                    "types": sorted({e.get("type") for e in subgraph.get("entities", [])}),
                },
                indent=2,
            )
        )
    else:
        print(json.dumps(subgraph, indent=2))


def cmd_update(client: ObjsClient, args: argparse.Namespace) -> None:
    subgraph = client.sbom_get(args.app, args.version)
    entities = list(subgraph.get("entities") or [])
    if not entities:
        raise SystemExit(f"No entities for {args.app}@{args.version} — seed first")
    sample = random.sample(entities, k=min(args.count, len(entities)))
    updated: list[dict[str, Any]] = []
    for ent in sample:
        payload = dict(ent.get("payload") or {})
        payload["description"] = f"updated-{_token()}"
        if "name" in payload and isinstance(payload["name"], str):
            payload["name"] = f"{payload['name']}-u{_token(3)}"
        updated.append(
            {
                "id": ent["id"],
                "type": ent["type"],
                "schemaVersion": ent.get("schemaVersion", SCHEMA_VERSION),
                "payload": payload,
                "annotations": dict(ent.get("annotations") or {}),
            }
        )
    result = client.sbom_put(args.app, args.version, {"entities": updated, "edges": []})
    print(
        json.dumps(
            {
                "action": "update",
                "updated": len(result.get("entities", [])),
                "ids": [e.get("id") for e in result.get("entities", [])],
            },
            indent=2,
        )
    )


def cmd_delete(client: ObjsClient, args: argparse.Namespace) -> None:
    subgraph = client.sbom_get(args.app, args.version)
    entities = list(subgraph.get("entities") or [])
    edges = list(subgraph.get("edges") or [])
    if args.all:
        entity_ids = [e["id"] for e in entities if e.get("id")]
        edge_ids = [e["id"] for e in edges if e.get("id")]
    else:
        entity_ids = [e["id"] for e in random.sample(entities, k=min(args.entities, len(entities)))]
        edge_ids = [e["id"] for e in random.sample(edges, k=min(args.edges, len(edges)))]
    if not entity_ids and not edge_ids:
        raise SystemExit("Nothing to delete")
    client.graph_delete(entity_ids=entity_ids, edge_ids=edge_ids)
    remaining = client.sbom_get(args.app, args.version)
    print(
        json.dumps(
            {
                "action": "delete",
                "deletedEntities": len(entity_ids),
                "deletedEdges": len(edge_ids),
                "remainingEntities": len(remaining.get("entities", [])),
                "remainingEdges": len(remaining.get("edges", [])),
            },
            indent=2,
        )
    )


def cmd_demo(client: ObjsClient, args: argparse.Namespace) -> None:
    print("== status ==")
    print(json.dumps(client.status()))
    print("\n== create (seed) ==")
    graph = build_random_graph(args.entities, args.edges)
    created = client.sbom_put(args.app, args.version, graph)
    print(f"entities={len(created.get('entities', []))} edges={len(created.get('edges', []))}")

    print("\n== retrieve ==")
    got = client.sbom_get(args.app, args.version)
    print(f"entities={len(got.get('entities', []))} edges={len(got.get('edges', []))}")

    print("\n== update ==")
    args.count = min(3, len(got.get("entities", [])))
    cmd_update(client, args)

    print("\n== delete (sample) ==")
    args.entities = 2
    args.edges = 1
    args.all = False
    cmd_delete(client, args)

    print("\n== final retrieve ==")
    final = client.sbom_get(args.app, args.version)
    print(
        json.dumps(
            {
                "entities": len(final.get("entities", [])),
                "edges": len(final.get("edges", [])),
            },
            indent=2,
        )
    )


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--base-url", default=DEFAULT_BASE, help=f"API base (default {DEFAULT_BASE})")
    sub = p.add_subparsers(dest="command", required=True)

    s = sub.add_parser("status", help="GET /api/v1/objs/status")
    s.set_defaults(func=cmd_status)

    s = sub.add_parser("apps", help="LIST applications and versions (GET /api/v1/example/sbom/apps)")
    s.set_defaults(func=cmd_apps)

    s = sub.add_parser("seed", help="CREATE random graph via SBOM PUT")
    s.add_argument("--app", default="random-app")
    s.add_argument("--version", default="1.0.0")
    s.add_argument("--entities", type=int, default=24)
    s.add_argument("--edges", type=int, default=18)
    s.set_defaults(func=cmd_seed)

    s = sub.add_parser(
        "bulk",
        help=(
            "CREATE many apps/versions; each version is a random ontology graph (or --tiny). "
            "Default: one bom_graph per app@version (SBOM PUT)."
        ),
    )
    s.add_argument(
        "--apps",
        "--app-number",
        "-n",
        type=int,
        default=20_000,
        dest="apps",
        metavar="N",
        help="Number of applications to create (default: 20000). Alias: --app-number, -n",
    )
    graphs = s.add_mutually_exclusive_group()
    graphs.add_argument(
        "--create-graphs",
        dest="create_graphs",
        action="store_true",
        help=(
            "Create one bom_graph per app@version via SBOM PUT "
            "(header annotations app + appVersion). Default."
        ),
    )
    graphs.add_argument(
        "--no-create-graphs",
        dest="create_graphs",
        action="store_false",
        help=(
            "Write entities to the pool only (POST /entities); no bom_graph headers and no edges "
            "(edges require a graph)."
        ),
    )
    s.add_argument(
        "--min-versions",
        "--min-versions-per-app",
        type=int,
        default=1,
        dest="min_versions",
        metavar="N",
        help="Minimum versions per app (default: 1). Alias: --min-versions-per-app",
    )
    s.add_argument(
        "--max-versions",
        "--max-versions-per-app",
        type=int,
        default=30,
        dest="max_versions",
        metavar="N",
        help="Maximum versions per app (default: 30). Alias: --max-versions-per-app",
    )
    s.add_argument("--prefix", default="app-", help="App id prefix (app-00000, ...)")
    s.add_argument("--workers", type=int, default=16, help="Concurrent PUT workers")
    s.add_argument(
        "--tiny",
        action="store_true",
        help="Fast 2-node Product->Component graphs instead of random ontology graphs",
    )
    s.add_argument(
        "--min-entities",
        type=int,
        default=8,
        help="Min random entities per version (default: 8; ignored with --tiny)",
    )
    s.add_argument(
        "--max-entities",
        type=int,
        default=20,
        help="Max random entities per version (default: 20; ignored with --tiny)",
    )
    s.add_argument(
        "--min-edges",
        type=int,
        default=6,
        help="Min random edges per version (default: 6; ignored with --tiny)",
    )
    s.add_argument(
        "--max-edges",
        type=int,
        default=16,
        help="Max random edges per version (default: 16; ignored with --tiny)",
    )
    s.add_argument(
        "--entities",
        type=int,
        default=None,
        help="Fixed entity count per version (sets min=max; ignored with --tiny)",
    )
    s.add_argument(
        "--edges",
        type=int,
        default=None,
        help="Fixed edge count per version (sets min=max; ignored with --tiny)",
    )
    s.set_defaults(func=cmd_bulk, create_graphs=True)

    s = sub.add_parser("get", help="RETRIEVE SBOM subgraph")
    s.add_argument("--app", default="random-app")
    s.add_argument("--version", default="1.0.0")
    s.add_argument("--all-versions", action="store_true", help="GET by app only")
    s.add_argument("--summary", action="store_true")
    s.set_defaults(func=cmd_get)

    s = sub.add_parser("update", help="UPDATE random entities (SBOM PUT upsert)")
    s.add_argument("--app", default="random-app")
    s.add_argument("--version", default="1.0.0")
    s.add_argument("--count", type=int, default=3)
    s.set_defaults(func=cmd_update)

    s = sub.add_parser("delete", help="DELETE entities/edges via foundation graph API")
    s.add_argument("--app", default="random-app")
    s.add_argument("--version", default="1.0.0")
    s.add_argument("--entities", type=int, default=2)
    s.add_argument("--edges", type=int, default=1)
    s.add_argument("--all", action="store_true", help="Delete entire app@version subgraph")
    s.set_defaults(func=cmd_delete)

    s = sub.add_parser("demo", help="Run create → retrieve → update → delete against localhost")
    s.add_argument("--app", default="random-app")
    s.add_argument("--version", default="1.0.0")
    s.add_argument("--entities", type=int, default=20)
    s.add_argument("--edges", type=int, default=15)
    s.set_defaults(func=cmd_demo)

    return p


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    client = ObjsClient(args.base_url)
    args.func(client, args)
    return 0


if __name__ == "__main__":
    sys.exit(main())
