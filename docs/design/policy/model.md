# Policy model (S1 locked)

**Normative:** C-24 WI-001 · [`GAPS.md`](../../workitems/completed/20260904-policy-evaluate-core/GAPS.md)  
**Package (target):** `org.poc.objs.policy.api`

---

## Intent

A **Policy** is a foundation artefact: metadata + which engine runs it + opaque evaluation body (+ optional applicability fields). It is **not**:

- an SBOM ontology entity type `Policy` / `COMPLIES_WITH`
- a `GraphFragmentPolicy` (fragment **normalization**)
- persist-gate JSON Schema validation
- a product regulation text hard-coded in objs jars

```mermaid
flowchart TB
  subgraph doNotConflate [Do not conflate]
    GFP[GraphFragmentPolicy normalize]
    VAL[Persist validation]
    POL[objs-policy executable artefact]
    SBOM[SBOM Policy entity]
  end
  GFP -.->|not| POL
  VAL -.->|not| POL
  SBOM -.->|not| POL
```

---

## Identity and versioning (G-P3)

| Concept | Lock |
|---------|------|
| Logical identity | **name** (string) |
| Revision | **serial version** — allocated on every **create** and every **update** (immutable revision; object-like serial) |
| Enabled flag | **None** on Policy — enablement/required belongs to **suites** later (C-27) |
| Default resolve | **`latest`** serial for that name |
| Traceability | Every evaluation outcome **must** cite the **executed** policy version |
| Suite refs (later) | Membership may pin `latest` or a **specific** serial version |

```mermaid
sequenceDiagram
  participant App
  participant Repo as PolicyRepository
  App->>Repo: save(name=mongo-gate, body=...)
  Repo-->>App: version=1
  App->>Repo: save(same name, updated body)
  Repo-->>App: version=2
  App->>Repo: resolve(name, latest)
  Repo-->>App: version=2 revision
  Note over App: evaluate outcomes record version=2
```

**Contrast with free-form version strings:** callers do not invent the serial; the repository does on write.

---

## Fields (logical)

```mermaid
classDiagram
  class Policy {
    UUID id
    String name
    long version
    String engineKind
    String body
    String contentType
    String applicabilityKind
    String applicabilityBody
  }
  note for Policy "version is serial, immutable per revision\nno enabled flag"
```

| Field | Role |
|-------|------|
| `id` | Stable UUID for this revision row (implementation detail OK) |
| `name` | Logical policy name |
| `version` | Serial revision number |
| `engineKind` | **String** (no enum) — which adapter runs the body. S1 implements **`CUSTOM`** only |
| `body` | Opaque UTF-8 **String** — engine-agnostic payload |
| `contentType` | Optional **encoding** hint for body (not the engine name) |
| `applicabilityKind` | Optional String — gate kind; S1: blank or **`ALWAYS_APPLY`** |
| `applicabilityBody` | Optional opaque String — reserved for later kinds; ignored for ALWAYS_APPLY |

---

## engineKind vs contentType (G-P4, G-P5)

| Field | Answers | Example |
|-------|---------|---------|
| `engineKind` | **Which adapter?** | `CUSTOM`, later `DROOLS`, `OPA` |
| `contentType` | **How is body encoded?** | `text/plain`, DRL media type, … |
| `body` | **What to run?** | Opaque text |

Unknown / unimplemented `engineKind` at evaluate time → per-policy **ERROR** (continue others).

---

## Applicability fields (G-P6)

Forward-looking mini-gate on the same artefact:

- Missing/`ALWAYS_APPLY` → always in scope (body unused in S1)
- Other kinds → extensible later (research); unimplemented → **ERROR** unless a later lock softens this

Applicability is **not** a separate public pipeline stage — see [`pipeline.md`](pipeline.md).

---

## PolicyRef

Resolve inputs for evaluate:

- by **id**, or
- by **name** + **`latest`** | specific **version**

Suites/batch wrappers later map richer inputs onto lists of refs ([`modules.md`](modules.md)).
