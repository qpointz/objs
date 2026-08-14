# WI-002 — Ontology, demo volumes, Gremlin sketches

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete

## Goal

Lock the 10-type AI catalog ontology and seed demo collections at agreed volumes.

## Deliverables

- [x] Ontology YAML: Dataset, LlmModel, AiAgent, Prompt, Skill, Tool, Guardrail, KnowledgeSource, Template, McpServer + AllowedEdgeRules
- [x] `generate_demo_data.py` + `asset-repository-demo-data.yaml` (50 / 20 / 100 / 200 / ~142 MCP / ~142 CS)
- [x] `examples/asset-repository/demo/traversals.md` Groovy sketches (MCP + AI agents)

## Acceptance

- Collection seed tests assert those sizes
- Demo profile starts with six collections
