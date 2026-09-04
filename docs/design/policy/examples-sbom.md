# Example Drools policies (SBOM schema)

**Audience:** Policy playground (`:objs-service-app` → `/workbench/policy`)  
**Ontology:** [`sbom-ontology.yaml`](../../../examples/sbom/sbom-service/src/main/resources/seeds/sbom-ontology.yaml)  
**Demo graphs:** [`sbom-demo-graph.yaml`](../../../examples/sbom/sbom-service/src/main/resources/seeds/sbom-demo-graph.yaml)  
**Engine facts:** [`drools.md`](drools.md) · scratch API: `DroolsEvaluationScratch`

Paste into the Policy editor, **Check**, then **Evaluate** against an open SBOM graph (e.g. payments demo or application-bom). These are **playground sketches**, not a compliance pack (G-P38).

---

## Fact & scratch cheat sheet

| Source | Drools fact | Typical match |
|--------|-------------|----------------|
| Entity | `EntityFact` | `type == "Component"`, `$e.get("version")`, `annotations["…"]` |
| Edge | `EdgeFact` | `role == "DEPENDS_ON"` |
| Wired bag | `ObjectFact` | (advanced) `name == "…"`, `values["…"]` |

```drl
global DroolsEvaluationScratch scratch;

// FAIL + finding (rule name auto-filled into extras.rule / message prefix)
scratch.fail("short reason");
scratch.finding("detail", "ERROR", "CODE", entityUuidOrNull, edgeUuidOrNull);

// PASS note (optional — otherwise PASS has no findings)
scratch.pass("ok");
```

Payload access is map-style: `$e.get("version")` (not JavaBeans).  
Entity / edge ids for bindings: `$e.getId().toString()` / `$edge.getId().toString()`.

---

## SBOM types used below (payload fields)

| Type | Fields (required★) | Useful edges |
|------|--------------------|--------------|
| **Component** | name★, version★, ecosystem★, kind★, coordinates, description | `DEPENDS_ON`, `LICENSED_UNDER`, `PROVIDED_BY`, `HAS_VULNERABILITY` |
| **Product** | name★, version★, supplier, lifecycle, homepage, description | `CONTAINS`, `OWNED_BY` |
| **License** | name★, spdxId★, url, description | ← `LICENSED_UNDER` |
| **Organization** | name★, domain, website, country, description | ← `OWNED_BY` / `PROVIDED_BY` |
| **Vulnerability** | name★, cve, severity, cvss, description | ← `HAS_VULNERABILITY` |
| **Artifact** | name★, artifactType★, checksum, size, description | ← `BUILDS` / `PACKAGES` |
| **Container Image** | name★, tag★, digest, registry, description | ← `BUILDS` / `DEPLOYS` |

Edge roles are uppercase in seeds (`DEPENDS_ON`, `CONTAINS`, …).

---

## 1. Component — version required

Fails when `payload.version` is null/blank. Demo seed Components already have versions — clear one in Composer to see a hit.

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "component-version-required"
when
    $e : EntityFact(
        type == "Component",
        eval(
            $e.get("version") == null ||
            String.valueOf($e.get("version")).isBlank()
        )
    )
then
    String id = $e.getId() != null ? $e.getId().toString() : null;
    scratch.fail("Component missing payload.version");
    scratch.finding(
        "Component missing required payload.version",
        "ERROR",
        "SBOM_COMPONENT_VERSION_REQUIRED",
        id,
        null
    );
end
```

---

## 2. Component — no SNAPSHOT / unknown version

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "component-version-not-snapshot"
when
    $e : EntityFact(
        type == "Component",
        eval(
            $e.get("version") instanceof String &&
            (
              ((String) $e.get("version")).contains("SNAPSHOT") ||
              ((String) $e.get("version")).equalsIgnoreCase("unknown")
            )
        )
    )
then
    String ver = String.valueOf($e.get("version"));
    String id = $e.getId() != null ? $e.getId().toString() : null;
    scratch.fail("Component version not release-ready: " + ver);
    scratch.finding(
        "Component version not release-ready: " + ver,
        "WARN",
        "SBOM_COMPONENT_VERSION_SNAPSHOT",
        id,
        null
    );
end
```

---

## 3. Component — ecosystem required

Ontology marks `ecosystem` required (Maven, npm, …).

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "component-ecosystem-required"
when
    $e : EntityFact(
        type == "Component",
        eval(
            $e.get("ecosystem") == null ||
            String.valueOf($e.get("ecosystem")).isBlank()
        )
    )
then
    String id = $e.getId() != null ? $e.getId().toString() : null;
    scratch.fail("Component missing payload.ecosystem");
    scratch.finding(
        "Component missing required payload.ecosystem",
        "ERROR",
        "SBOM_COMPONENT_ECOSYSTEM_REQUIRED",
        id,
        null
    );
end
```

---

## 4. Product — must be OWNED_BY an Organization

Uses a pattern: Product with no outgoing `OWNED_BY` edge. Payments demo includes `OWNED_BY` — remove the edge (or evaluate a Product-only fragment) to fail.

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.EdgeFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "product-owned-by-required"
when
    $p : EntityFact( type == "Product", $pid : id != null )
    not EdgeFact( role == "OWNED_BY", source == $pid )
then
    String id = $p.getId().toString();
    scratch.fail("Product has no OWNED_BY Organization");
    scratch.finding(
        "Product missing OWNED_BY edge to Organization",
        "ERROR",
        "SBOM_PRODUCT_OWNER_REQUIRED",
        id,
        null
    );
end
```

---

## 5. Component — should declare a license

Warn when a Component has no `LICENSED_UNDER` edge.

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.EdgeFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "component-license-edge"
when
    $c : EntityFact( type == "Component", $cid : id != null )
    not EdgeFact( role == "LICENSED_UNDER", source == $cid )
then
    String id = $c.getId().toString();
    scratch.fail("Component has no LICENSED_UNDER License");
    scratch.finding(
        "Component missing LICENSED_UNDER edge",
        "WARN",
        "SBOM_COMPONENT_LICENSE_MISSING",
        id,
        null
    );
end
```

---

## 6. License — SPDX id required

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "license-spdx-required"
when
    $e : EntityFact(
        type == "License",
        eval(
            $e.get("spdxId") == null ||
            String.valueOf($e.get("spdxId")).isBlank()
        )
    )
then
    String id = $e.getId() != null ? $e.getId().toString() : null;
    scratch.fail("License missing payload.spdxId");
    scratch.finding(
        "License missing required payload.spdxId",
        "ERROR",
        "SBOM_LICENSE_SPDX_REQUIRED",
        id,
        null
    );
end
```

---

## 7. DEPENDS_ON edges — inventory note

Does not fail; emits an OK finding per dependency edge (useful to verify evaluate + severity pills).

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EdgeFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "depends-on-inventory"
when
    $edge : EdgeFact( role == "DEPENDS_ON" )
then
    String eid = $edge.getId() != null ? $edge.getId().toString() : null;
    scratch.pass("DEPENDS_ON edge present");
    scratch.finding(
        "Dependency edge " + $edge.getSource() + " → " + $edge.getTarget(),
        "INFO",
        "SBOM_DEPENDS_ON",
        null,
        eid
    );
end
```

---

## 8. Vulnerability — CRITICAL severity

Matches `Vulnerability.payload.severity` (string). Seed a CRITICAL vuln or edit in Composer.

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "vulnerability-critical"
when
    $e : EntityFact(
        type == "Vulnerability",
        eval(
            $e.get("severity") != null &&
            String.valueOf($e.get("severity")).equalsIgnoreCase("CRITICAL")
        )
    )
then
    String id = $e.getId() != null ? $e.getId().toString() : null;
    String cve = $e.get("cve") != null ? String.valueOf($e.get("cve")) : "?";
    scratch.fail("CRITICAL vulnerability: " + cve);
    scratch.finding(
        "CRITICAL vulnerability " + cve,
        "ERROR",
        "SBOM_VULN_CRITICAL",
        id,
        null
    );
end
```

---

## 9. Artifact — checksum recommended

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "artifact-checksum-recommended"
when
    $e : EntityFact(
        type == "Artifact",
        eval(
            $e.get("checksum") == null ||
            String.valueOf($e.get("checksum")).isBlank()
        )
    )
then
    String id = $e.getId() != null ? $e.getId().toString() : null;
    scratch.fail("Artifact missing payload.checksum");
    scratch.finding(
        "Artifact should declare payload.checksum",
        "WARN",
        "SBOM_ARTIFACT_CHECKSUM",
        id,
        null
    );
end
```

---

## 10. Container Image — digest recommended

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "container-image-digest"
when
    $e : EntityFact(
        type == "Container Image",
        eval(
            $e.get("digest") == null ||
            String.valueOf($e.get("digest")).isBlank()
        )
    )
then
    String id = $e.getId() != null ? $e.getId().toString() : null;
    scratch.fail("Container Image missing payload.digest");
    scratch.finding(
        "Container Image should declare payload.digest",
        "WARN",
        "SBOM_IMAGE_DIGEST",
        id,
        null
    );
end
```

---

## 11. Combined starter (version + license)

One policy body with two rules — good first paste for the payments demo (`Component` + `LICENSED_UNDER`).

```drl
package org.poc.objs.policy.sbom.examples;

import org.poc.objs.policy.drools.EntityFact;
import org.poc.objs.policy.drools.EdgeFact;
import org.poc.objs.policy.drools.DroolsEvaluationScratch;

global DroolsEvaluationScratch scratch;

rule "component-version-required"
when
    $e : EntityFact(
        type == "Component",
        eval(
            $e.get("version") == null ||
            String.valueOf($e.get("version")).isBlank()
        )
    )
then
    String id = $e.getId() != null ? $e.getId().toString() : null;
    scratch.fail("Component missing payload.version");
    scratch.finding(
        "Component missing required payload.version",
        "ERROR",
        "SBOM_COMPONENT_VERSION_REQUIRED",
        id,
        null
    );
end

rule "component-license-edge"
when
    $c : EntityFact( type == "Component", $cid : id != null )
    not EdgeFact( role == "LICENSED_UNDER", source == $cid )
then
    String id = $c.getId().toString();
    scratch.fail("Component has no LICENSED_UNDER License");
    scratch.finding(
        "Component missing LICENSED_UNDER edge",
        "WARN",
        "SBOM_COMPONENT_LICENSE_MISSING",
        id,
        null
    );
end
```

On the stock payments graph, rule 1 should **PASS** (versions present); Jackson Component has no license edge in the seed — rule 2 should **WARN/FAIL** for that node.

---

## How to try

1. Run `:sbom-service` (or load SBOM seeds into the workbench DB) and open a demo graph in shared context.  
2. Workbench → **Policy** → **Add** → paste an example → **Save**.  
3. **Check** (compile) → **Evaluate**.  
4. Findings appear under **Evaluations**; severity pills on the graph; click a row to select the bound node/edge.

See also: [`workbench.md`](workbench.md) · [`drools.md`](drools.md).
