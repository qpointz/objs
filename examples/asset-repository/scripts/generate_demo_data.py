#!/usr/bin/env python3
"""Generate asset-repository-demo-data.yaml (Collection + CollectionObjects seeds)."""
from __future__ import annotations

from pathlib import Path

import yaml

OUT = Path(__file__).resolve().parents[1] / "asset-repository-service/src/main/resources/seeds/asset-repository-demo-data.yaml"

PURPOSES = ["Training", "Validation", "Evaluation", "Knowledge", "Operational", "Other"]
FORMATS = ["CSV", "Parquet", "JSON", "Database Table", "Documents", "Images", "Audio", "Video"]
CLASSIF = ["Public", "Internal", "Confidential", "Restricted"]
SERVICES = ["Azure Blob Storage", "SharePoint", "PostgreSQL", "S3", "Snowflake", "BigQuery"]
CATALOGS = ["Enterprise Data Catalog", "Corporate Data Catalog", "AI Data Catalog"]
OWNERS_DS = [
    "Customer Service Analytics",
    "Product Management",
    "AI Quality Engineering",
    "Customer Platform",
    "Finance Analytics",
    "HR Analytics",
    "Risk Data Office",
]
MODEL_TYPES = ["General Purpose", "Reasoning", "Coding", "Multimodal", "Other"]
MODEL_STATUS = ["Candidate", "Approved", "Restricted", "Deprecated"]
VENDORS = [
    ("OpenAI", "GPT", "Azure OpenAI"),
    ("Anthropic", "Claude", "Anthropic"),
    ("Google", "Gemini", "Google Cloud"),
    ("Meta", "Llama", "Internal GPU Platform"),
    ("Mistral", "Mistral", "Azure AI"),
    ("Amazon", "Titan", "AWS Bedrock"),
]
CAPS = [
    "Question Answering",
    "Search",
    "Summarization",
    "Content Generation",
    "Data Analysis",
    "Classification",
    "Recommendation",
    "Task Automation",
    "Decision Support",
]
CATEGORIES = [
    "Customer Support",
    "Software Development",
    "Data Analytics",
    "Finance",
    "Human Resources",
    "Sales",
    "Operations",
    "Research",
    "Legal",
    "Risk",
]
AGENT_STATUS = ["Development", "Pilot", "Production", "Suspended", "Retired"]
INTERACT = ["Conversational", "API", "Background", "Embedded"]
AUTONOMY = ["Assistant", "Assisted", "Autonomous"]
ACCESS = ["Internal", "External", "Restricted", "Public"]
CRIT = ["Low", "Medium", "High", "Critical"]
PROMPT_TYPES = ["System", "Task", "Classification", "Extraction", "Generation", "Evaluation", "Other"]
RES_STATUS = ["Draft", "Approved", "Deprecated"]
TOOL_TYPES = ["API", "Function", "Database", "Search", "MCP", "Other"]
OPS = ["Read", "Write", "Read / Write"]
GR_TYPES = [
    "Data Protection",
    "Content Safety",
    "Prompt Injection",
    "Access Control",
    "Output Validation",
    "Compliance",
    "Other",
]
GR_ACTIONS = ["Detect", "Warn", "Block", "Redact", "Require Approval"]
KS_TYPES = ["Documents", "Structured Data", "Web Content", "Mixed"]
KS_ACCESS = ["Semantic Search", "Keyword Search", "Database Query", "API", "Direct Context"]
TPL_TYPES = ["Document", "Report", "Email", "Summary", "Response", "Structured Output", "Other"]
TPL_FMT = ["Text", "Markdown", "HTML", "JSON", "YAML", "Other"]
TRANSPORT = ["Streamable HTTP", "STDIO", "Other"]
AUTH = ["OAuth 2.0", "API Key", "Service Identity", "None", "Other"]
REGIONS = ["Switzerland North", "West Europe", "East US", "Frankfurt"]
IO_SETS = [
    (["Text"], ["Text"]),
    (["Text", "Image"], ["Text"]),
    (["Text", "Image", "Audio", "Video"], ["Text"]),
    (["Text"], ["Text", "Image"]),
]


def pick(seq, i):
    return seq[i % len(seq)]


def coll(name, description, owner, email, sla, types):
    return {
        "apiVersion": "objs.poc.org/v1",
        "kind": "Collection",
        "name": name,
        "description": description,
        "owner": owner,
        "ownerEmail": email,
        "sla": sla,
        "objectWriteMode": "UUID_OR_IDENTIFIER",
        "types": types,
    }


def objects_doc(collection, objects, relations=None):
    doc = {
        "apiVersion": "objs.poc.org/v1",
        "kind": "CollectionObjects",
        "collection": collection,
        "objects": objects,
    }
    if relations:
        doc["relations"] = relations
    return doc


def dataset(i, named=None):
    n = i + 1
    name = named or f"Enterprise Dataset {n:03d}"
    did = named and named.upper().replace(" ", "-")[:40] or f"DS-{n:03d}"
    if named:
        did = {
            "Customer Support Conversations": "DS-CS-CONVERSATIONS",
            "Product Documentation": "DS-PROD-DOCS",
            "AI Quality Golden Set": "DS-AIQ-GOLDEN",
            "Customer Master": "DS-CUSTOMER-MASTER",
        }.get(named, did)
    return {
        "key": f"ds-{n:03d}",
        "type": "Dataset",
        "schemaVersion": "1.0.0",
        "payload": {
            "datasetId": did if named else f"DS-{n:03d}",
            "name": name,
            "description": f"{name} used across AI solutions",
            "purpose": pick(PURPOSES, i),
            "catalog": pick(CATALOGS, i),
            "format": pick(FORMATS, i),
            "service": pick(SERVICES, i),
            "location": f"ai-data/domain-{n:03d}/v1",
            "owner": pick(OWNERS_DS, i),
            "classification": pick(CLASSIF, i),
            "size": f"{(i % 90) + 1} GB",
            "lastUpdated": f"2026-{(i % 8) + 1:02d}-{(i % 27) + 1:02d}",
        },
    }


def model(i):
    vendor, family, platform = pick(VENDORS, i)
    ins, outs = pick(IO_SETS, i)
    mid = f"{family.lower()}-demo-{i + 1:02d}"
    return {
        "key": f"model-{i + 1:02d}",
        "type": "LlmModel",
        "schemaVersion": "1.0.0",
        "payload": {
            "modelId": mid,
            "name": f"{family} {i + 1:02d}",
            "description": f"{vendor} {family} catalog entry",
            "vendor": vendor,
            "family": family,
            "version": f"2025-{(i % 12) + 1:02d}-01",
            "catalog": "Approved Model Catalog" if i % 5 else "Enterprise Model Catalog",
            "modelType": pick(MODEL_TYPES, i),
            "io": {"inputs": ins, "outputs": outs},
            "limits": {"contextWindow": pick(["128k tokens", "200k tokens", "1M tokens"], i)},
            "hosting": {"platform": platform, "region": pick(REGIONS, i)},
            "status": pick(MODEL_STATUS, i),
            "dataClassificationAllowed": pick(CLASSIF, i),
            "owner": "Enterprise AI Platform",
        },
    }


def agent(i):
    cat = pick(CATEGORIES, i)
    return {
        "key": f"agent-{i + 1:03d}",
        "type": "AiAgent",
        "schemaVersion": "1.0.0",
        "payload": {
            "agentId": f"agent-{cat.lower().replace(' ', '-')}-{i + 1:03d}",
            "name": f"{cat} Agent {i + 1:03d}",
            "description": f"Assists {cat.lower()} with AI-supported tasks",
            "catalog": "Enterprise AI Catalog",
            "category": cat,
            "capabilities": [pick(CAPS, i), pick(CAPS, i + 1), pick(CAPS, i + 2)],
            "runtime": {
                "platform": "Enterprise AI Platform",
                "interactionMode": pick(INTERACT, i),
                "autonomyLevel": pick(AUTONOMY, i),
            },
            "status": pick(AGENT_STATUS, i),
            "owner": cat,
            "access": pick(ACCESS, i),
            "businessCriticality": pick(CRIT, i),
        },
    }


def prompt(i, prefix="prompt"):
    return {
        "key": f"{prefix}-{i + 1:03d}",
        "type": "Prompt",
        "schemaVersion": "1.0.0",
        "payload": {
            "promptId": f"prompt-{i + 1:03d}",
            "name": f"Prompt {i + 1:03d}",
            "description": f"Reusable {pick(PROMPT_TYPES, i).lower()} prompt",
            "catalog": "Enterprise AI Catalog",
            "promptType": pick(PROMPT_TYPES, i),
            "version": f"{1 + (i % 4)}.{(i % 8) + 1}.0",
            "language": pick(["English", "German", "French"], i),
            "owner": pick(CATEGORIES, i) + " AI",
            "status": pick(RES_STATUS, i),
            "body": f"You are a helpful assistant for task {i + 1}.",
        },
    }


def skill(i, prefix="skill"):
    return {
        "key": f"{prefix}-{i + 1:03d}",
        "type": "Skill",
        "schemaVersion": "1.0.0",
        "payload": {
            "skillId": f"skill-{i + 1:03d}",
            "name": f"Skill {i + 1:03d}",
            "description": f"Reusable skill for {pick(CATEGORIES, i).lower()}",
            "catalog": "Enterprise AI Catalog",
            "category": pick(CATEGORIES, i),
            "version": f"{1 + (i % 3)}.{(i % 8) + 1}.0",
            "owner": pick(CATEGORIES, i) + " AI",
            "status": pick(RES_STATUS, i),
        },
    }


def tool(i, prefix="tool", tool_id=None):
    return {
        "key": f"{prefix}-{i + 1:03d}",
        "type": "Tool",
        "schemaVersion": "1.0.0",
        "payload": {
            "toolId": tool_id or f"tool-{i + 1:03d}",
            "name": f"Tool {i + 1:03d}",
            "description": f"Invokes {pick(SERVICES, i)}",
            "catalog": "Enterprise AI Catalog",
            "integration": {
                "toolType": pick(TOOL_TYPES, i),
                "operationType": pick(OPS, i),
                "service": pick(SERVICES, i),
            },
            "version": f"1.{(i % 8) + 1}.0",
            "owner": pick(["Customer Platform", "AI Infrastructure", "Data Platform"], i),
            "status": pick(RES_STATUS, i),
        },
    }


def guardrail(i, prefix="gr"):
    return {
        "key": f"{prefix}-{i + 1:03d}",
        "type": "Guardrail",
        "schemaVersion": "1.0.0",
        "payload": {
            "guardrailId": f"guardrail-{i + 1:03d}",
            "name": f"Guardrail {i + 1:03d}",
            "description": f"{pick(GR_TYPES, i)} control",
            "catalog": "Enterprise AI Catalog",
            "policy": {"guardrailType": pick(GR_TYPES, i), "action": pick(GR_ACTIONS, i)},
            "version": f"{1 + (i % 5)}.1.0",
            "owner": "AI Governance",
            "status": pick(RES_STATUS, i),
        },
    }


def knowledge(i, prefix="ks", kid=None):
    return {
        "key": f"{prefix}-{i + 1:03d}",
        "type": "KnowledgeSource",
        "schemaVersion": "1.0.0",
        "payload": {
            "knowledgeSourceId": kid or f"knowledge-{i + 1:03d}",
            "name": f"Knowledge Source {i + 1:03d}",
            "description": f"Exposes {pick(KS_TYPES, i).lower()} to agents",
            "catalog": "Enterprise AI Catalog",
            "access": {
                "knowledgeType": pick(KS_TYPES, i),
                "accessMethod": pick(KS_ACCESS, i),
                "refreshFrequency": pick(["Hourly", "Daily", "Weekly"], i),
            },
            "owner": pick(CATEGORIES, i),
            "status": pick(AGENT_STATUS, i),
        },
    }


def template(i, prefix="tpl"):
    return {
        "key": f"{prefix}-{i + 1:03d}",
        "type": "Template",
        "schemaVersion": "1.0.0",
        "payload": {
            "templateId": f"template-{i + 1:03d}",
            "name": f"Template {i + 1:03d}",
            "description": f"{pick(TPL_TYPES, i)} output structure",
            "catalog": "Enterprise AI Catalog",
            "output": {"templateType": pick(TPL_TYPES, i), "format": pick(TPL_FMT, i)},
            "version": f"1.{(i % 6) + 1}.0",
            "owner": "Collaboration Platform",
            "status": pick(RES_STATUS, i),
        },
    }


def mcp_server(i, prefix="mcp", sid=None):
    return {
        "key": f"{prefix}-{i + 1:03d}",
        "type": "McpServer",
        "schemaVersion": "1.0.0",
        "payload": {
            "mcpServerId": sid or f"mcp-{i + 1:03d}",
            "name": f"MCP Server {i + 1:03d}",
            "description": f"Exposes tools and knowledge for domain {i + 1:03d}",
            "catalog": "Enterprise AI Catalog",
            "version": f"2.{(i % 8) + 1}.0",
            "connection": {
                "endpoint": f"https://mcp.example.com/s{i + 1:03d}",
                "transport": pick(TRANSPORT, i),
                "authentication": pick(AUTH, i),
                "protocolVersion": "2025-06-18",
            },
            "access": pick(ACCESS, i),
            "owner": pick(["Customer Platform", "AI Infrastructure", "Data Platform"], i),
            "status": pick(AGENT_STATUS, i),
        },
    }


def rel(source, role, target):
    return {"source": source, "role": role, "target": target}


class QuotedStrDumper(yaml.SafeDumper):
    def ignore_aliases(self, data):
        return True


def _represent_str(dumper, data):
    # Keep dotted versions / numeric-looking ids as YAML strings (not floats).
    if data.replace(".", "", 1).isdigit():
        return dumper.represent_scalar("tag:yaml.org,2002:str", data, style='"')
    return dumper.represent_scalar("tag:yaml.org,2002:str", data)


QuotedStrDumper.add_representer(str, _represent_str)


def dump_docs(docs):
    chunks = []
    for d in docs:
        chunks.append(yaml.dump(d, Dumper=QuotedStrDumper, sort_keys=False, allow_unicode=True).strip())
    return "---\n" + "\n---\n---\n".join(chunks) + "\n"


def main():
    docs = []

    docs.append(coll("datasets", "Dataset inventory", "data-eng", "data@example.com", "P3 — best effort", ["Dataset"]))
    named = ["Customer Support Conversations", "Product Documentation", "AI Quality Golden Set", "Customer Master"]
    ds = [dataset(i, named[i] if i < 4 else None) for i in range(50)]
    docs.append(objects_doc("datasets", ds))

    docs.append(coll("models", "Approved LLM model inventory", "ai-platform", "ai@example.com", "P2 — next business day", ["LlmModel"]))
    models = [model(i) for i in range(20)]
    docs.append(objects_doc("models", models))

    docs.append(coll("agents", "AI agent inventory", "Customer Service", "cs@example.com", "P2 — next business day", ["AiAgent"]))
    agents = [agent(i) for i in range(100)]
    docs.append(objects_doc("agents", agents))

    docs.append(coll(
        "composables",
        "Reusable prompts, skills, tools, guardrails, knowledge sources, and templates",
        "ai-platform",
        "ai@example.com",
        "P3 — best effort",
        ["Prompt", "Skill", "Tool", "Guardrail", "KnowledgeSource", "Template"],
    ))
    prompts = [prompt(i) for i in range(50)]
    skills = [skill(i) for i in range(40)]
    tools = [tool(i) for i in range(40)]
    grs = [guardrail(i) for i in range(25)]
    kss = [knowledge(i) for i in range(20)]
    tpls = [template(i) for i in range(25)]
    composables = prompts + skills + tools + grs + kss + tpls
    assert len(composables) == 200
    crels = []
    for i, sk in enumerate(skills):
        crels.append(rel(sk["key"], "USES_PROMPT", prompts[i % 50]["key"]))
        crels.append(rel(sk["key"], "USES_TOOL", tools[i % 40]["key"]))
        crels.append(rel(sk["key"], "PROTECTED_BY", grs[i % 25]["key"]))
        if i % 2 == 0:
            crels.append(rel(sk["key"], "USES_KNOWLEDGE", kss[i % 20]["key"]))
        if i % 3 == 0:
            crels.append(rel(sk["key"], "USES_TEMPLATE", tpls[i % 25]["key"]))
        if i > 0 and i % 7 == 0:
            crels.append(rel(sk["key"], "DEPENDS_ON", skills[i - 1]["key"]))
    for i, pr in enumerate(prompts):
        if i % 2 == 0:
            crels.append(rel(pr["key"], "USES_TEMPLATE", tpls[i % 25]["key"]))
    docs.append(objects_doc("composables", composables, crels))

    docs.append(coll(
        "mcp-servers",
        "MCP servers and the tools, prompts, and knowledge they expose",
        "Customer Platform",
        "platform@example.com",
        "P2 — next business day",
        ["McpServer", "Tool", "Prompt", "KnowledgeSource"],
    ))
    mcp_objs = []
    mcp_rels = []
    for i in range(50):
        srv = mcp_server(i)
        t = tool(i, prefix=f"mcp-tool", tool_id=f"tool-mcp-{i + 1:03d}")
        t["payload"]["integration"]["toolType"] = "MCP"
        mcp_objs.extend([srv, t])
        mcp_rels.append(rel(srv["key"], "PROVIDES_TOOL", t["key"]))
        if i % 2 == 0:
            ks = knowledge(i, prefix=f"mcp-ks", kid=f"knowledge-mcp-{i + 1:03d}")
            mcp_objs.append(ks)
            mcp_rels.append(rel(srv["key"], "PROVIDES_KNOWLEDGE", ks["key"]))
        if i % 3 == 0:
            pr = prompt(i + 200, prefix=f"mcp-pr")
            pr["payload"]["promptId"] = f"prompt-mcp-{i + 1:03d}"
            mcp_objs.append(pr)
            mcp_rels.append(rel(srv["key"], "PROVIDES_PROMPT", pr["key"]))
    docs.append(objects_doc("mcp-servers", mcp_objs, mcp_rels))

    all_types = [
        "Dataset", "LlmModel", "AiAgent", "Prompt", "Skill", "Tool",
        "Guardrail", "KnowledgeSource", "Template", "McpServer",
    ]
    docs.append(coll(
        "customer-support",
        "Assembled customer support solution graph",
        "Customer Service",
        "cs@example.com",
        "P2 — next business day",
        all_types,
    ))
    cs_rels = []
    cs_agent_names = [
        "Customer Support Agent",
        "Support QA Agent",
        "Escalation Triage Agent",
        "Knowledge Authoring Agent",
        "Ticket Summarizer Agent",
        "Voice Assist Agent",
        "Retention Offer Agent",
        "Policy Advisor Agent",
        "Complaint Classifier Agent",
        "Field Technician Copilot",
    ]
    cs_agents = []
    for i, name in enumerate(cs_agent_names):
        a = agent(i)
        a["key"] = f"cs-agent-{i + 1:03d}"
        a["payload"]["agentId"] = f"agent-cs-sol-{i + 1:03d}"
        a["payload"]["name"] = name
        a["payload"]["category"] = "Customer Support"
        a["payload"]["status"] = "Production" if i < 6 else "Pilot"
        cs_agents.append(a)
    cs_models = [model(i) for i in range(8)]
    for i, m in enumerate(cs_models):
        m["payload"]["modelId"] = m["payload"]["modelId"] + "-sol"
        m["key"] = f"cs-model-{i + 1:02d}"
    cs_skills = [skill(i, prefix="cs-skill") for i in range(24)]
    for s in cs_skills:
        s["payload"]["skillId"] = s["payload"]["skillId"] + "-sol"
        s["payload"]["category"] = "Customer Support"
    cs_prompts = [prompt(i, prefix="cs-prompt") for i in range(24)]
    for p in cs_prompts:
        p["payload"]["promptId"] = p["payload"]["promptId"] + "-sol"
    cs_tpls = [template(i, prefix="cs-tpl") for i in range(12)]
    for t in cs_tpls:
        t["payload"]["templateId"] = t["payload"]["templateId"] + "-sol"
    cs_tools = [tool(i, prefix="cs-tool") for i in range(16)]
    for t in cs_tools:
        t["payload"]["toolId"] = t["payload"]["toolId"] + "-sol"
    cs_grs = [guardrail(i, prefix="cs-gr") for i in range(12)]
    for g in cs_grs:
        g["payload"]["guardrailId"] = g["payload"]["guardrailId"] + "-sol"
    cs_ks = [knowledge(i, prefix="cs-ks") for i in range(12)]
    for k in cs_ks:
        k["payload"]["knowledgeSourceId"] = k["payload"]["knowledgeSourceId"] + "-sol"
    cs_mcp = [mcp_server(i, prefix="cs-mcp") for i in range(8)]
    for m in cs_mcp:
        m["payload"]["mcpServerId"] = m["payload"]["mcpServerId"] + "-sol"
    cs_ds = [dataset(i) for i in range(16)]
    for i, d in enumerate(cs_ds):
        d["payload"]["datasetId"] = d["payload"]["datasetId"] + "-SOL"
        d["key"] = f"cs-ds-{i + 1:03d}"

    cs_objs = cs_agents + cs_models + cs_skills + cs_prompts + cs_tpls + cs_tools + cs_grs + cs_ks + cs_mcp + cs_ds

    for i, ag in enumerate(cs_agents):
        cs_rels.append(rel(ag["key"], "USES_MODEL", cs_models[i % 8]["key"]))
        cs_rels.append(rel(ag["key"], "PROTECTED_BY", cs_grs[i % 12]["key"]))
        cs_rels.append(rel(ag["key"], "USES_MCP_SERVER", cs_mcp[i % 8]["key"]))
        cs_rels.append(rel(ag["key"], "USES_DATA", cs_ds[i % 16]["key"]))
        cs_rels.append(rel(ag["key"], "USES_TEMPLATE", cs_tpls[i % 12]["key"]))
    cs_rels.append(rel(cs_agents[0]["key"], "USES_MODEL", cs_models[1]["key"]))
    for i, sk in enumerate(cs_skills):
        cs_rels.append(rel(cs_agents[i % 10]["key"], "USES_SKILL", sk["key"]))
        cs_rels.append(rel(sk["key"], "USES_PROMPT", cs_prompts[i]["key"]))
        cs_rels.append(rel(sk["key"], "USES_TEMPLATE", cs_tpls[i % 12]["key"]))
        cs_rels.append(rel(sk["key"], "PROTECTED_BY", cs_grs[i % 12]["key"]))
        cs_rels.append(rel(sk["key"], "USES_TOOL", cs_tools[i % 16]["key"]))
        cs_rels.append(rel(sk["key"], "USES_KNOWLEDGE", cs_ks[i % 12]["key"]))
        cs_rels.append(rel(sk["key"], "USES_MODEL", cs_models[i % 8]["key"]))
        if i > 0:
            cs_rels.append(rel(sk["key"], "DEPENDS_ON", cs_skills[i - 1]["key"]))
    for i, pr in enumerate(cs_prompts):
        cs_rels.append(rel(pr["key"], "DESIGNED_FOR", cs_models[i % 8]["key"]))
        cs_rels.append(rel(pr["key"], "USES_TEMPLATE", cs_tpls[i % 12]["key"]))
        cs_rels.append(rel(cs_agents[i % 10]["key"], "USES_PROMPT", pr["key"]))
    for i, t in enumerate(cs_tools):
        cs_rels.append(rel(cs_agents[i % 10]["key"], "USES_TOOL", t["key"]))
        cs_rels.append(rel(t["key"], "ACCESSES", cs_ds[i % 16]["key"]))
    for i, k in enumerate(cs_ks):
        cs_rels.append(rel(cs_agents[i % 10]["key"], "USES_KNOWLEDGE", k["key"]))
        cs_rels.append(rel(k["key"], "SOURCED_FROM", cs_ds[(i + 2) % 16]["key"]))
        cs_rels.append(rel(k["key"], "USES_MODEL", cs_models[i % 8]["key"]))
    for i, g in enumerate(cs_grs):
        cs_rels.append(rel(g["key"], "APPLIES_TO", cs_models[i % 8]["key"]))
        cs_rels.append(rel(g["key"], "APPLIES_TO", cs_ds[i % 16]["key"]))
        cs_rels.append(rel(cs_agents[i % 10]["key"], "PROTECTED_BY", g["key"]))
    for i, mcp in enumerate(cs_mcp):
        cs_rels.append(rel(cs_agents[i % 10]["key"], "USES_MCP_SERVER", mcp["key"]))
        cs_rels.append(rel(mcp["key"], "PROVIDES_TOOL", cs_tools[i % 16]["key"]))
        cs_rels.append(rel(mcp["key"], "PROVIDES_KNOWLEDGE", cs_ks[i % 12]["key"]))
        cs_rels.append(rel(mcp["key"], "PROVIDES_PROMPT", cs_prompts[i % 24]["key"]))
    cs_rels.append(rel(cs_ds[3]["key"], "EVALUATES", cs_models[0]["key"]))
    cs_rels.append(rel(cs_ds[4]["key"], "VALIDATES", cs_models[1]["key"]))
    cs_rels.append(rel(cs_ds[5]["key"], "TRAINS", cs_models[3]["key"]))
    for i in range(6, 16):
        cs_rels.append(rel(cs_ds[i]["key"], "EVALUATES" if i % 2 == 0 else "VALIDATES", cs_models[i % 8]["key"]))

    docs.append(objects_doc("customer-support", cs_objs, cs_rels))

    OUT.write_text(dump_docs(docs), encoding="utf-8")
    print(
        f"Wrote {OUT} collections={len([d for d in docs if d['kind']=='Collection'])} "
        f"cs_objects={len(cs_objs)} cs_rels={len(cs_rels)} mcp_objects={len(mcp_objs)}"
    )


if __name__ == "__main__":
    main()
