#!/usr/bin/env python3
"""Load qsynth CSVs into a running asset-repository over domain REST only."""
from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

DEFAULT_BASE = "http://localhost:8080"
API = "/api/v1/asset-repository/"
SCHEMA_VERSION = "1.0.0"
ID_FIELDS = {
    "datasetId",
    "modelId",
    "agentId",
    "promptId",
    "skillId",
    "toolId",
    "guardrailId",
    "knowledgeSourceId",
    "templateId",
    "mcpServerId",
}
NEST_PREFIXES = (
    "io_",
    "limits_",
    "hosting_",
    "runtime_",
    "integration_",
    "policy_",
    "output_",
    "connection_",
    "access_",
)
ARRAY_PATHS = {("io", "inputs"), ("io", "outputs"), ("capabilities",)}

COLLECTIONS: dict[str, dict[str, Any]] = {
    "datasets": {
        "description": "Dataset inventory",
        "owner": "data-eng",
        "ownerEmail": "data@example.com",
        "sla": "P3 — best effort",
        "types": ["Dataset"],
    },
    "models": {
        "description": "Approved LLM model inventory",
        "owner": "ai-platform",
        "ownerEmail": "ai@example.com",
        "sla": "P2 — next business day",
        "types": ["LlmModel"],
    },
    "agents": {
        "description": "AI agent inventory",
        "owner": "Customer Service",
        "ownerEmail": "cs@example.com",
        "sla": "P2 — next business day",
        "types": ["AiAgent"],
    },
    "composables": {
        "description": "Reusable prompts, skills, tools, guardrails, knowledge sources, and templates",
        "owner": "ai-platform",
        "ownerEmail": "ai@example.com",
        "sla": "P3 — best effort",
        "types": ["Prompt", "Skill", "Tool", "Guardrail", "KnowledgeSource", "Template"],
    },
    "mcp-servers": {
        "description": "MCP servers and the tools, prompts, and knowledge they expose",
        "owner": "Customer Platform",
        "ownerEmail": "platform@example.com",
        "sla": "P2 — next business day",
        "types": ["McpServer", "Tool", "Prompt", "KnowledgeSource"],
    },
    "customer-support": {
        "description": "Assembled customer support solution graph",
        "owner": "Customer Service",
        "ownerEmail": "cs@example.com",
        "sla": "P2 — next business day",
        "types": [
            "Dataset",
            "LlmModel",
            "AiAgent",
            "Prompt",
            "Skill",
            "Tool",
            "Guardrail",
            "KnowledgeSource",
            "Template",
            "McpServer",
        ],
    },
}

OBJECT_TABLES = [
    ("dataset", "datasets", "Dataset", "datasetId"),
    ("llm_model", "models", "LlmModel", "modelId"),
    ("ai_agent", "agents", "AiAgent", "agentId"),
    ("prompt", "composables", "Prompt", "promptId"),
    ("skill", "composables", "Skill", "skillId"),
    ("tool", "composables", "Tool", "toolId"),
    ("guardrail", "composables", "Guardrail", "guardrailId"),
    ("knowledge", "composables", "KnowledgeSource", "knowledgeSourceId"),
    ("template", "composables", "Template", "templateId"),
    ("mcp_server", "mcp-servers", "McpServer", "mcpServerId"),
    ("mcp_tool", "mcp-servers", "Tool", "toolId"),
    ("mcp_knowledge", "mcp-servers", "KnowledgeSource", "knowledgeSourceId"),
    ("mcp_prompt", "mcp-servers", "Prompt", "promptId"),
    ("cs_dataset", "customer-support", "Dataset", "datasetId"),
    ("cs_model", "customer-support", "LlmModel", "modelId"),
    ("cs_agent", "customer-support", "AiAgent", "agentId"),
    ("cs_skill", "customer-support", "Skill", "skillId"),
    ("cs_prompt", "customer-support", "Prompt", "promptId"),
    ("cs_template", "customer-support", "Template", "templateId"),
    ("cs_tool", "customer-support", "Tool", "toolId"),
    ("cs_guardrail", "customer-support", "Guardrail", "guardrailId"),
    ("cs_knowledge", "customer-support", "KnowledgeSource", "knowledgeSourceId"),
    ("cs_mcp", "customer-support", "McpServer", "mcpServerId"),
]

LINKS = [
    ("composables", "skill_uses_prompt.csv", "USES_PROMPT", "skill", "skillId", "prompt", "promptId"),
    ("composables", "skill_uses_tool.csv", "USES_TOOL", "skill", "skillId", "tool", "toolId"),
    ("composables", "skill_protected_by.csv", "PROTECTED_BY", "skill", "skillId", "guardrail", "guardrailId"),
    ("composables", "skill_uses_knowledge.csv", "USES_KNOWLEDGE", "skill", "skillId", "knowledge", "knowledgeSourceId"),
    ("composables", "skill_uses_template.csv", "USES_TEMPLATE", "skill", "skillId", "template", "templateId"),
    ("composables", "skill_depends_on.csv", "DEPENDS_ON", "skill", "skillId", "skill", "dependsOnSkillId"),
    ("composables", "prompt_uses_template.csv", "USES_TEMPLATE", "prompt", "promptId", "template", "templateId"),
    ("mcp-servers", "mcp_provides_tool.csv", "PROVIDES_TOOL", "mcp_server", "mcpServerId", "mcp_tool", "toolId"),
    ("mcp-servers", "mcp_provides_knowledge.csv", "PROVIDES_KNOWLEDGE", "mcp_server", "mcpServerId", "mcp_knowledge", "knowledgeSourceId"),
    ("mcp-servers", "mcp_provides_prompt.csv", "PROVIDES_PROMPT", "mcp_server", "mcpServerId", "mcp_prompt", "promptId"),
    ("customer-support", "cs_agent_uses_model.csv", "USES_MODEL", "cs_agent", "agentId", "cs_model", "modelId"),
    ("customer-support", "cs_agent_protected_by.csv", "PROTECTED_BY", "cs_agent", "agentId", "cs_guardrail", "guardrailId"),
    ("customer-support", "cs_agent_uses_mcp.csv", "USES_MCP_SERVER", "cs_agent", "agentId", "cs_mcp", "mcpServerId"),
    ("customer-support", "cs_agent_uses_data.csv", "USES_DATA", "cs_agent", "agentId", "cs_dataset", "datasetId"),
    ("customer-support", "cs_agent_uses_template.csv", "USES_TEMPLATE", "cs_agent", "agentId", "cs_template", "templateId"),
    ("customer-support", "cs_agent_uses_skill.csv", "USES_SKILL", "cs_agent", "agentId", "cs_skill", "skillId"),
    ("customer-support", "cs_skill_uses_prompt.csv", "USES_PROMPT", "cs_skill", "skillId", "cs_prompt", "promptId"),
    ("customer-support", "cs_skill_uses_template.csv", "USES_TEMPLATE", "cs_skill", "skillId", "cs_template", "templateId"),
    ("customer-support", "cs_skill_protected_by.csv", "PROTECTED_BY", "cs_skill", "skillId", "cs_guardrail", "guardrailId"),
    ("customer-support", "cs_skill_uses_tool.csv", "USES_TOOL", "cs_skill", "skillId", "cs_tool", "toolId"),
    ("customer-support", "cs_skill_uses_knowledge.csv", "USES_KNOWLEDGE", "cs_skill", "skillId", "cs_knowledge", "knowledgeSourceId"),
    ("customer-support", "cs_skill_uses_model.csv", "USES_MODEL", "cs_skill", "skillId", "cs_model", "modelId"),
    ("customer-support", "cs_skill_depends_on.csv", "DEPENDS_ON", "cs_skill", "skillId", "cs_skill", "dependsOnSkillId"),
    ("customer-support", "cs_prompt_designed_for.csv", "DESIGNED_FOR", "cs_prompt", "promptId", "cs_model", "modelId"),
    ("customer-support", "cs_prompt_uses_template.csv", "USES_TEMPLATE", "cs_prompt", "promptId", "cs_template", "templateId"),
    ("customer-support", "cs_agent_uses_prompt.csv", "USES_PROMPT", "cs_agent", "agentId", "cs_prompt", "promptId"),
    ("customer-support", "cs_agent_uses_tool.csv", "USES_TOOL", "cs_agent", "agentId", "cs_tool", "toolId"),
    ("customer-support", "cs_tool_accesses.csv", "ACCESSES", "cs_tool", "toolId", "cs_dataset", "datasetId"),
    ("customer-support", "cs_agent_uses_knowledge.csv", "USES_KNOWLEDGE", "cs_agent", "agentId", "cs_knowledge", "knowledgeSourceId"),
    ("customer-support", "cs_ks_sourced_from.csv", "SOURCED_FROM", "cs_knowledge", "knowledgeSourceId", "cs_dataset", "datasetId"),
    ("customer-support", "cs_ks_uses_model.csv", "USES_MODEL", "cs_knowledge", "knowledgeSourceId", "cs_model", "modelId"),
    ("customer-support", "cs_gr_applies_to_model.csv", "APPLIES_TO", "cs_guardrail", "guardrailId", "cs_model", "modelId"),
    ("customer-support", "cs_gr_applies_to_dataset.csv", "APPLIES_TO", "cs_guardrail", "guardrailId", "cs_dataset", "datasetId"),
    ("customer-support", "cs_mcp_provides_tool.csv", "PROVIDES_TOOL", "cs_mcp", "mcpServerId", "cs_tool", "toolId"),
    ("customer-support", "cs_mcp_provides_knowledge.csv", "PROVIDES_KNOWLEDGE", "cs_mcp", "mcpServerId", "cs_knowledge", "knowledgeSourceId"),
    ("customer-support", "cs_mcp_provides_prompt.csv", "PROVIDES_PROMPT", "cs_mcp", "mcpServerId", "cs_prompt", "promptId"),
    ("customer-support", "cs_ds_evaluates.csv", "EVALUATES", "cs_dataset", "datasetId", "cs_model", "modelId"),
    ("customer-support", "cs_ds_validates.csv", "VALIDATES", "cs_dataset", "datasetId", "cs_model", "modelId"),
    ("customer-support", "cs_ds_trains.csv", "TRAINS", "cs_dataset", "datasetId", "cs_model", "modelId"),
]


class ArClient:
    def __init__(self, base_url: str, timeout: int = 120) -> None:
        self.base = base_url.rstrip("/") + "/"
        self.timeout = timeout

    def request(self, method: str, path: str, body: Any | None = None) -> Any:
        url = self.base + API.lstrip("/") + path.lstrip("/")
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


def syn_id(value: str) -> str:
    value = (value or "").strip()
    if not value:
        raise ValueError("blank identifier")
    return value if value.startswith("syn-") else f"syn-{value}"


def nest_payload(row: dict[str, str], id_field: str) -> dict[str, Any]:
    payload: dict[str, Any] = {}
    for key, raw in row.items():
        if raw is None:
            continue
        value: Any = raw.strip() if isinstance(raw, str) else raw
        if value == "":
            continue
        if key in ID_FIELDS or key == id_field:
            value = syn_id(str(value))
        nested_key = None
        for prefix in NEST_PREFIXES:
            if key.startswith(prefix):
                nested_key = (prefix[:-1], key[len(prefix) :])
                break
        if nested_key:
            parent, child = nested_key
            obj = payload.setdefault(parent, {})
            if not isinstance(obj, dict):
                obj = {}
                payload[parent] = obj
            if (parent, child) in ARRAY_PATHS:
                obj[child] = [p for p in str(value).split("|") if p]
            else:
                obj[child] = value
        elif ("capabilities",) in ARRAY_PATHS and key == "capabilities":
            payload[key] = [p for p in str(value).split("|") if p]
        else:
            payload[key] = value
    return payload


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as fh:
        return list(csv.DictReader(fh))


def chunks(items: list[Any], size: int) -> list[list[Any]]:
    return [items[i : i + size] for i in range(0, len(items), size)]


def ensure_collections(client: ArClient, dry_run: bool) -> dict[str, str]:
    existing = {c["name"]: c["id"] for c in (client.request("GET", "collections") or [])}
    ids: dict[str, str] = {}
    for name, spec in COLLECTIONS.items():
        if name in existing:
            ids[name] = existing[name]
            print(f"collection {name}: reuse {ids[name]}")
            continue
        body = {
            "name": name,
            "description": spec["description"],
            "owner": spec["owner"],
            "ownerEmail": spec["ownerEmail"],
            "sla": spec["sla"],
            "objectWriteMode": "UUID_OR_IDENTIFIER",
            "types": [{"objectType": t, "metadata": None} for t in spec["types"]],
        }
        if dry_run:
            print(f"collection {name}: would create")
            ids[name] = "dry-run"
            continue
        created = client.request("POST", "collections", body)
        ids[name] = created["id"]
        print(f"collection {name}: created {ids[name]}")
    return ids


def main() -> None:
    parser = argparse.ArgumentParser(description="Load synthetic asset-repository CSVs over domain REST")
    parser.add_argument("--base-url", default=DEFAULT_BASE)
    parser.add_argument("--data-dir", default=str(Path(__file__).resolve().parent / "generated"))
    parser.add_argument("--batch-size", type=int, default=50)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    data_dir = Path(args.data_dir)
    if not data_dir.is_dir():
        raise SystemExit(f"data dir not found: {data_dir}")

    client = ArClient(args.base_url)
    collection_ids = ensure_collections(client, args.dry_run)

    objects: dict[tuple[str, str], dict[str, Any]] = {}
    for table, collection, type_name, id_field in OBJECT_TABLES:
        path = data_dir / f"{table}.csv"
        if not path.exists():
            raise SystemExit(f"missing {path}")
        rows = read_csv(path)
        writes = []
        for row in rows:
            payload = nest_payload(row, id_field)
            ident = str(payload[id_field])
            writes.append({"type": type_name, "schemaVersion": SCHEMA_VERSION, "payload": payload})
            objects[(table, ident)] = {"type": type_name, "schemaVersion": SCHEMA_VERSION, "payload": payload}
        print(f"{table}: {len(writes)} {type_name} -> {collection}")
        if args.dry_run:
            continue
        cid = collection_ids[collection]
        for batch in chunks(writes, args.batch_size):
            client.request("POST", f"collections/{cid}/compositions", {"objects": batch, "relations": []})

    rel_count = 0
    for collection, csv_name, role, src_table, src_col, tgt_table, tgt_col in LINKS:
        path = data_dir / csv_name
        if not path.exists():
            raise SystemExit(f"missing {path}")
        for row in read_csv(path):
            src_id = syn_id(row[src_col])
            tgt_raw = row[tgt_col]
            tgt_id = syn_id(tgt_raw)
            if src_id == tgt_id and role == "DEPENDS_ON":
                continue
            src = objects.get((src_table, src_id))
            tgt = objects.get((tgt_table, tgt_id))
            if src is None or tgt is None:
                print(f"skip {csv_name}: missing endpoint {src_table}/{src_id} or {tgt_table}/{tgt_id}", file=sys.stderr)
                continue
            rel_count += 1
            if args.dry_run:
                continue
            cid = collection_ids[collection]
            client.request(
                "POST",
                f"collections/{cid}/compositions",
                {
                    "objects": [src, tgt],
                    "relations": [{"sourceKey": "obj-0", "role": role, "targetKey": "obj-1"}],
                },
            )
    print(f"relations posted: {rel_count}")


if __name__ == "__main__":
    main()
