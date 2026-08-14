Groovy sketches for Workbench Query (`gremlin-lang` on the wire). Envelope: entity type → vertex label; edge role → edge label; domain fields under `payload`.

Matchers:

```json
{ "all": true }
```

```json
{ "graph-expr": "a.collection == 'agents'" }
```

```json
{ "graph-expr": "a.collection == 'customer-support'" }
```

The `agents` library has no wiring. Use `customer-support` (or `all`) for relation columns.

---

## MCP servers — one row per server

```groovy
g.V()
  .hasLabel('McpServer')
  .order().by(values('payload').select('name'))
  .project('mcpServerId','name','owner','status','access','catalog','version','endpoint','transport','authentication','protocolVersion','toolCount','promptCount','knowledgeCount','tools','prompts','knowledge')
    .by(values('payload').select('mcpServerId'))
    .by(values('payload').select('name'))
    .by(coalesce(values('payload').select('owner'),  constant('')))
    .by(coalesce(values('payload').select('status'), constant('')))
    .by(coalesce(values('payload').select('access'),  constant('')))
    .by(coalesce(values('payload').select('catalog'), constant('')))
    .by(coalesce(values('payload').select('version'), constant('')))
    .by(coalesce(values('payload').select('connection').select('endpoint'),  constant('')))
    .by(coalesce(values('payload').select('connection').select('transport'),  constant('')))
    .by(coalesce(values('payload').select('connection').select('authentication'),  constant('')))
    .by(coalesce(values('payload').select('connection').select('protocolVersion'), constant('')))
    .by(out('PROVIDES_TOOL').hasLabel('Tool').count())
    .by(out('PROVIDES_PROMPT').hasLabel('Prompt').count())
    .by(out('PROVIDES_KNOWLEDGE').hasLabel('KnowledgeSource').count())
    .by(out('PROVIDES_TOOL').hasLabel('Tool').values('payload').select('name').fold())
    .by(out('PROVIDES_PROMPT').hasLabel('Prompt').values('payload').select('name').fold())
    .by(out('PROVIDES_KNOWLEDGE').hasLabel('KnowledgeSource').values('payload').select('name').fold())
```

---

## AI agents — one row per agent

```groovy
g.V()
  .hasLabel('AiAgent')
  .order().by(values('payload').select('name'))
  .project(
      'agentId','name','owner','status','access','catalog','category','criticality',
      'platform','interactionMode','autonomyLevel','capabilities',
      'modelCount','skillCount','promptCount','toolCount','knowledgeCount',
      'templateCount','guardrailCount','mcpCount','datasetCount',
      'models','skills','prompts','tools','knowledge','templates','guardrails','mcpServers','datasets'
    )
    .by(values('payload').select('agentId'))
    .by(values('payload').select('name'))
    .by(coalesce(values('payload').select('owner'), constant('')))
    .by(coalesce(values('payload').select('status'), constant('')))
    .by(coalesce(values('payload').select('access'), constant('')))
    .by(coalesce(values('payload').select('catalog'), constant('')))
    .by(coalesce(values('payload').select('category'), constant('')))
    .by(coalesce(values('payload').select('businessCriticality'), constant('')))
    .by(coalesce(values('payload').select('runtime').select('platform'), constant('')))
    .by(coalesce(values('payload').select('runtime').select('interactionMode'), constant('')))
    .by(coalesce(values('payload').select('runtime').select('autonomyLevel'), constant('')))
    .by(coalesce(values('payload').select('capabilities'), constant([])))
    .by(out('USES_MODEL').hasLabel('LlmModel').count())
    .by(out('USES_SKILL').hasLabel('Skill').count())
    .by(out('USES_PROMPT').hasLabel('Prompt').count())
    .by(out('USES_TOOL').hasLabel('Tool').count())
    .by(out('USES_KNOWLEDGE').hasLabel('KnowledgeSource').count())
    .by(out('USES_TEMPLATE').hasLabel('Template').count())
    .by(out('PROTECTED_BY').hasLabel('Guardrail').count())
    .by(out('USES_MCP_SERVER').hasLabel('McpServer').count())
    .by(out('USES_DATA').hasLabel('Dataset').count())
    .by(out('USES_MODEL').hasLabel('LlmModel').values('payload').select('name').fold())
    .by(out('USES_SKILL').hasLabel('Skill').values('payload').select('name').fold())
    .by(out('USES_PROMPT').hasLabel('Prompt').values('payload').select('name').fold())
    .by(out('USES_TOOL').hasLabel('Tool').values('payload').select('name').fold())
    .by(out('USES_KNOWLEDGE').hasLabel('KnowledgeSource').values('payload').select('name').fold())
    .by(out('USES_TEMPLATE').hasLabel('Template').values('payload').select('name').fold())
    .by(out('PROTECTED_BY').hasLabel('Guardrail').values('payload').select('name').fold())
    .by(out('USES_MCP_SERVER').hasLabel('McpServer').values('payload').select('name').fold())
    .by(out('USES_DATA').hasLabel('Dataset').values('payload').select('name').fold())
```

---

## AI agents — one row per used resource

Agents with no outgoing resource edges are omitted.

```groovy
g.V()
  .hasLabel('AiAgent').as('a')
  .outE(
      'USES_MODEL','USES_SKILL','USES_PROMPT','USES_TOOL','USES_KNOWLEDGE',
      'USES_TEMPLATE','PROTECTED_BY','USES_MCP_SERVER','USES_DATA'
    ).as('e')
  .inV().as('r')
  .project('agentId','agentName','owner','status','category','relation','resourceType','resourceId','resourceName')
    .by(select('a').values('payload').select('agentId'))
    .by(select('a').values('payload').select('name'))
    .by(select('a').values('payload').select('owner'))
    .by(select('a').values('payload').select('status'))
    .by(select('a').values('payload').select('category'))
    .by(select('e').label())
    .by(select('r').label())
    .by(coalesce(
        select('r').values('payload').select('modelId'),
        select('r').values('payload').select('skillId'),
        select('r').values('payload').select('promptId'),
        select('r').values('payload').select('toolId'),
        select('r').values('payload').select('knowledgeSourceId'),
        select('r').values('payload').select('templateId'),
        select('r').values('payload').select('guardrailId'),
        select('r').values('payload').select('mcpServerId'),
        select('r').values('payload').select('datasetId'),
        constant('')
      ))
    .by(select('r').values('payload').select('name'))
```
