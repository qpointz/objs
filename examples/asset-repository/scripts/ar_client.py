#!/usr/bin/env python3
"""Asset repository producer / consumer against domain REST only.

Uses /api/v1/asset-repository/** — never /api/v1/objs/**.

Examples:
  python ar_client.py consumer
  python ar_client.py producer
  python ar_client.py all --base-url http://localhost:8080
"""

from __future__ import annotations

import argparse
import json
import sys
import uuid
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urljoin
from urllib.request import Request, urlopen

DEFAULT_BASE = "http://localhost:8080"
API = "/api/v1/asset-repository/"


class ArClient:
    def __init__(self, base_url: str) -> None:
        self.base = base_url.rstrip("/") + "/"

    def _url(self, path: str, query: dict[str, str] | None = None) -> str:
        url = urljoin(self.base, API.lstrip("/") + path.lstrip("/"))
        if query:
            url = f"{url}?{urlencode({k: v for k, v in query.items() if v is not None})}"
        return url

    def request(
        self,
        method: str,
        path: str,
        *,
        body: Any | None = None,
        query: dict[str, str] | None = None,
    ) -> Any:
        data = None
        headers = {"Accept": "application/json"}
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        req = Request(self._url(path, query), data=data, headers=headers, method=method)
        try:
            with urlopen(req, timeout=30) as resp:
                raw = resp.read()
                if not raw or resp.status == 204:
                    return None
                return json.loads(raw.decode("utf-8"))
        except HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise SystemExit(f"{method} {path} -> HTTP {exc.code}: {detail}") from exc
        except URLError as exc:
            raise SystemExit(f"Cannot reach {self.base}: {exc.reason}") from exc


def pp(label: str, value: Any) -> None:
    print(f"\n=== {label} ===")
    print(json.dumps(value, indent=2, default=str))


def run_consumer(client: ArClient) -> None:
    collections = client.request("GET", "collections")
    pp("Collections", collections)

    filtered = client.request("GET", "collections", query={"acceptedType": "Database"})
    pp("Collections accepting Database", filtered)

    if not collections:
        print("No collections — start the service with the demo profile.")
        return

    target = next((c for c in collections if c.get("name") == "dp-customers"), collections[0])
    cid = target["id"]
    objects = client.request("GET", f"collections/{cid}/objects")
    pp(f"Objects in {target['name']}", [{"id": o["id"], "type": o["type"], "payload": o["payload"]} for o in objects])

    if objects:
        one = client.request("GET", f"collections/{cid}/objects/{objects[0]['id']}")
        pp("Get object", one)

    hits = client.request(
        "POST",
        f"collections/{cid}/objects/search",
        body={"filters": {"name": objects[0]["payload"].get("name", "")}} if objects else {},
    )
    pp("Search by name", hits)


def run_producer(client: ArClient, *, do_delete: bool) -> None:
    stamp = uuid.uuid4().hex[:8]
    created = client.request(
        "POST",
        "collections",
        body={
            "name": f"py-prompts-{stamp}",
            "description": "Created by ar_client.py producer (prompt/tool library)",
            "owner": "python-client",
            "ownerEmail": "python@example.com",
            "supportEmail": None,
            "sla": "best-effort",
            "objectWriteMode": "UUID_OR_IDENTIFIER",
            "types": [
                {"objectType": "Prompt", "metadata": None},
                {"objectType": "Tool", "metadata": None},
            ],
        },
    )
    pp("Created collection", created)
    cid = created["id"]

    patched = client.request(
        "PATCH",
        f"collections/{cid}",
        body={"description": "Patched by producer", "sla": "P3"},
    )
    pp("Patched collection", patched)

    without_uuid = client.request(
        "POST",
        f"collections/{cid}/objects",
        body={
            "type": "Prompt",
            "schemaVersion": "1.0.0",
            "payload": {
                "name": f"prompt-{stamp}",
                "template": f"Hello from producer {stamp}",
                "description": "Producer write without UUID",
            },
        },
    )
    pp("Write object (no UUID)", without_uuid)

    fixed_id = str(uuid.uuid4())
    with_uuid = client.request(
        "POST",
        f"collections/{cid}/objects",
        body={
            "id": fixed_id,
            "type": "Tool",
            "schemaVersion": "1.0.0",
            "payload": {
                "name": f"tool-{stamp}",
                "kind": "function",
                "description": "Producer write with UUID",
            },
        },
    )
    pp("Write object (with UUID)", with_uuid)

    related = client.request(
        "POST",
        "collections",
        body={
            "name": f"py-data-{stamp}",
            "description": "Data product composition demo",
            "owner": "python-client",
            "objectWriteMode": "UUID_OR_IDENTIFIER",
            "types": [
                {"objectType": "Database", "metadata": None},
                {"objectType": "Dataset", "metadata": None},
            ],
        },
    )
    rid = related["id"]
    composition = client.request(
        "POST",
        f"collections/{rid}/compositions",
        body={
            "objects": [
                {
                    "type": "Database",
                    "schemaVersion": "1.0.0",
                    "payload": {"name": f"db-{stamp}", "engine": "postgresql", "version": "16"},
                },
                {
                    "type": "Dataset",
                    "schemaVersion": "1.0.0",
                    "payload": {
                        "name": f"table-{stamp}",
                        "datasetType": "table",
                        "classification": "internal",
                    },
                },
            ],
            "relations": [{"sourceKey": "obj-0", "role": "CONTAINS", "targetKey": "obj-1"}],
        },
    )
    pp("Composition write", composition)

    if do_delete:
        client.request("DELETE", f"collections/{cid}/objects/{with_uuid['id']}")
        print(f"\nDeleted object {with_uuid['id']}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Asset repository domain REST producer/consumer")
    parser.add_argument(
        "mode",
        choices=("consumer", "producer", "all"),
        help="consumer = read/search; producer = write paths; all = both",
    )
    parser.add_argument("--base-url", default=DEFAULT_BASE, help=f"Service base URL (default {DEFAULT_BASE})")
    parser.add_argument(
        "--delete",
        action="store_true",
        help="Producer: delete the UUID-written object after create",
    )
    args = parser.parse_args(argv)

    client = ArClient(args.base_url)
    if args.mode in ("consumer", "all"):
        run_consumer(client)
    if args.mode in ("producer", "all"):
        run_producer(client, do_delete=args.delete)
    return 0


if __name__ == "__main__":
    sys.exit(main())
