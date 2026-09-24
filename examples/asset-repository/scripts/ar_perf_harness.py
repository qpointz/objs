#!/usr/bin/env python3
"""Timed observational harness against a filled asset-repository perf dataset.

Uses domain REST /api/v1/asset-repository/** for list/search/stats.
Optionally times foundation Gremlin (sidecar) when --gremlin is set.

When ar.perf.graphs>1, pass --collection perf-noise-0 (or another shard).

Examples:
  python ar_perf_harness.py --base-url http://localhost:8080
  python ar_perf_harness.py --collection perf-noise-0 --iterations 5 --warmup 1
  python ar_perf_harness.py --gremlin
"""

from __future__ import annotations

import argparse
import json
import statistics
import sys
import time
from typing import Any, Callable
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

DEFAULT_BASE = "http://localhost:8080"
DOMAIN_API = "/api/v1/asset-repository/"
GREMLIN_PATH = "/api/v1/objs/graph/traverse/gremlin"


class HttpClient:
    def __init__(self, base_url: str, timeout: int = 120) -> None:
        self.base = base_url.rstrip("/") + "/"
        self.timeout = timeout

    def request(self, method: str, path: str, body: Any | None = None) -> Any:
        url = self.base + path.lstrip("/")
        data = None
        headers = {"Accept": "application/json"}
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        req = Request(url, data=data, headers=headers, method=method)
        try:
            with urlopen(req, timeout=self.timeout) as resp:
                raw = resp.read()
                if not raw or resp.status == 204:
                    return None
                return json.loads(raw.decode("utf-8"))
        except HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise SystemExit(f"{method} {path} -> HTTP {exc.code}: {detail}") from exc
        except URLError as exc:
            raise SystemExit(f"Cannot reach {self.base}: {exc.reason}") from exc


def percentile(sorted_vals: list[float], p: float) -> float:
    if not sorted_vals:
        return 0.0
    if len(sorted_vals) == 1:
        return sorted_vals[0]
    k = (len(sorted_vals) - 1) * (p / 100.0)
    f = int(k)
    c = min(f + 1, len(sorted_vals) - 1)
    if f == c:
        return sorted_vals[f]
    return sorted_vals[f] + (sorted_vals[c] - sorted_vals[f]) * (k - f)


def time_op(name: str, iterations: int, warmup: int, fn: Callable[[], Any]) -> None:
    for _ in range(max(0, warmup)):
        fn()
    samples: list[float] = []
    last: Any = None
    for _ in range(iterations):
        t0 = time.perf_counter()
        last = fn()
        samples.append((time.perf_counter() - t0) * 1000.0)
    ordered = sorted(samples)
    size_hint = ""
    if isinstance(last, list):
        size_hint = f"  rows={len(last)}"
    elif isinstance(last, dict) and "objectCount" in last:
        size_hint = f"  objectCount={last.get('objectCount')}"
    print(
        f"{name:40s}  n={iterations}  "
        f"p50={percentile(ordered, 50):8.1f}ms  "
        f"p95={percentile(ordered, 95):8.1f}ms  "
        f"max={max(ordered):8.1f}ms  "
        f"mean={statistics.fmean(samples):8.1f}ms"
        f"{size_hint}"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Asset repository perf harness (observational)")
    parser.add_argument("--base-url", default=DEFAULT_BASE)
    parser.add_argument(
        "--collection",
        default="perf-noise",
        help="Collection name (use perf-noise-0 when ar.perf.graphs>1)",
    )
    parser.add_argument("--iterations", type=int, default=5)
    parser.add_argument("--warmup", type=int, default=1)
    parser.add_argument(
        "--gremlin",
        action="store_true",
        help="Also time POST /api/v1/objs/graph/traverse/gremlin (foundation sidecar)",
    )
    args = parser.parse_args()
    if args.iterations < 1:
        raise SystemExit("--iterations must be >= 1")

    client = HttpClient(args.base_url)
    collections = client.request("GET", DOMAIN_API + "collections") or []
    target = next((c for c in collections if c.get("name") == args.collection), None)
    if target is None:
        names = [c.get("name") for c in collections]
        raise SystemExit(
            f"Collection '{args.collection}' not found. Present: {names}. "
            "Start with --spring.profiles.active=perf and wait for fill."
        )
    cid = target["id"]
    graph_id = target.get("graphId")

    print(f"base={args.base_url.rstrip('/')}  collection={args.collection}  id={cid}")
    stats = client.request("GET", f"{DOMAIN_API}collections/{cid}/statistics") or {}
    print(
        f"statistics: objectCount={stats.get('objectCount')}  "
        f"edgeCount={stats.get('edgeCount')}"
    )
    if stats.get("edgeCount") is None:
        print("warning: statistics.edgeCount missing — rebuild/restart the service", file=sys.stderr)
    elif int(stats.get("edgeCount") or 0) == 0:
        print(
            "warning: edgeCount=0 — edges are written as in-collection relations; "
            "open an object detail (e.g. an AiAgent) or check fill logs for 'Perf fill edges'",
            file=sys.stderr,
        )
    print("---")

    time_op(
        "GET collections",
        args.iterations,
        args.warmup,
        lambda: client.request("GET", DOMAIN_API + "collections"),
    )
    time_op(
        "GET collection objects (full list)",
        args.iterations,
        args.warmup,
        lambda: client.request("GET", f"{DOMAIN_API}collections/{cid}/objects"),
    )
    time_op(
        "GET collection statistics",
        args.iterations,
        args.warmup,
        lambda: client.request("GET", f"{DOMAIN_API}collections/{cid}/statistics"),
    )
    time_op(
        "POST objects/search (type=AiAgent)",
        args.iterations,
        args.warmup,
        lambda: client.request(
            "POST",
            f"{DOMAIN_API}collections/{cid}/objects/search",
            {"filters": {"type": "AiAgent"}},
        ),
    )

    if args.gremlin:
        if not graph_id:
            print("Gremlin skipped: collection has no graphId", file=sys.stderr)
        else:
            body = {
                "graphId": graph_id,
                "matcher": {"obj-expr": "true"},
                "script": "g.V().count()",
            }
            time_op(
                "POST gremlin V().count()",
                args.iterations,
                args.warmup,
                lambda: client.request("POST", GREMLIN_PATH, body),
            )

    print("---")
    print("Done (observational only; no latency SLO asserted).")


if __name__ == "__main__":
    main()
