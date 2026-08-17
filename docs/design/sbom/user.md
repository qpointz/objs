# SBOM inventory — user guide

**Audience:** application owners and anyone using the inventory SPA  
**Product design:** [`example.md`](example.md)  
**Run:** `./gradlew :sbom-service:run` → **http://localhost:8080/sbom/**  
**Workbench** (graphs/schemas, different app): [`../ui.md`](../ui.md)

This guide uses **product language only**: application, version, BOM, Combined SBOM, fingerprint, asset, relation. It does not use graph / entity / edge / matcher terms.

---

## What a version is

A **version** of an application is made of one or more **BOMs** (incomplete bills). Together they are the **Combined SBOM** — the complete bill for that version.

- **One BOM** looks like a single bill. The constituent is labeled **BOM**; extra multi-select chrome is hidden.
- **Two or more BOMs** (for example Build, Runtime, Image) show Combined SBOM (all of them) plus the ability to look at one BOM or a subset.
- Combined SBOM is **always read-only**. You never “edit Combined.” You edit **one BOM** at a time on a **DRAFT**, then Save.

**RELEASED** versions are read-only. To change assets or BOMs, create a **New draft**, edit, then **Promote**.

---

## Start here — Applications portal

**Applications** lists inventory apps (search by name). Each card lazy-loads:

| On the card | Meaning |
|-------------|---------|
| Latest version string, or “No released version” | Highest **RELEASED** SemVer. Drafts never count as latest. |
| **Multi-BOM** badge | That **latest RELEASED** has two or more BOMs. |
| Footer counts | **All** BOMs across every version · **all** versions (drafts included). Independent of the badge. |

**New application** asks for name, description, **target version** (required), and optional tags. You land on a **DRAFT** with one empty BOM.

---

## Application page layout

Three regions:

1. **Header** — application name, combined tags (view mode), Edit / Save / Discard, New draft, Promote, Fingerprint.  
2. **Left pane** — nested tree (top) and asset types (bottom), with a drag splitter.  
3. **Right pane** — **Assets** (metadata or selected asset) and **Graph**.

### Left tree

```text
Application name
  └── v. 1.2.0          or   1.3.0 DRAFT
        ├── Build
        ├── Runtime
        └── Image
```

With a **single** BOM the child is labeled **BOM** (the stored name stays `BOM` but is not emphasized).

| Click | Does |
|-------|------|
| Application name | Open **application** metadata (versions + fingerprints tables). Does **not** change which BOMs are checked. |
| Version label | Open **version** metadata. Does **not** change checks. |
| A BOM name | Open **that BOM** (metadata + its assets/graph when it is the Open constituent). |

**View mode, two or more BOMs:**

- Checkboxes on BOM rows choose which graphs are **unioned** on the canvas (Assets list + Graph).  
- Checkbox on the **version** row = select all = **Combined SBOM**. Indeterminate = some but not all.  
- Clicking a title and ticking a checkbox are independent: you can inspect version metadata while the canvas still shows Combined.

**Edit mode, two or more BOMs:**

- **Radios** on BOM rows pick the **one** BOM you are mutating.  
- No check/radio on the application or version rows.

Fingerprint view has **no** BOM list.

### Right pane focus

When no asset is selected, the Assets tab title follows focus:

| Focus | Tab title | Contents |
|-------|-----------|----------|
| Application | Application | Name, description, tags. **Versions** table (status, based-on, **BOM** column Single/Multi). **Fingerprints** table. |
| Version | `v. x` / `x DRAFT` | Version string/tags. **Create BOM** / **Delete BOM** while editing a DRAFT. BOM list if count ≥ 2. |
| BOM | BOM name | That BOM’s name, description, tags. Graph and related assets are writable here when editing. |

Selecting an **asset** (left list or graph) switches the Assets tab to that asset (payload, related assets).

---

## View vs Edit

| | View | Edit |
|--|------|------|
| Who | Any version | **DRAFT** only |
| Combined / subset canvas | Read-only | Read-only (still). Mutation is the radio-selected BOM. |
| Switching BOMs / focus | Free | Free inside the draft (changeset). Stay/Leave only if you **leave this application/version** with unsaved work. |
| Save / Discard | Hidden | Persist or revert the **whole** draft session (app + version + every touched BOM, including create/delete). |

Changed BOMs (and a dirty version row) highlight **blue** until Save or Discard.

There is **no Edit** on RELEASED. Use **New draft**.

---

## Working with one BOM vs many

### Single BOM

Treat it as “the bill” for the version. Create BOM (on the **version** pane, while editing) when you need a second slice — that turns multi-BOM chrome on.

### Combined SBOM (all BOMs)

Open the **version** row and/or select all checkboxes. Canvas is the **union**: each asset once; duplicate relations (same source, target, role) collapsed. You can browse assets and the graph. You cannot add/remove assets or relations here.

### One BOM among many

Check only that BOM (view) or set its radio (edit) and open it. That graph is what you mutate.

### Subset (two BOMs, not all)

Check some but not all. Canvas is a **read-only union**, **not** labeled Combined SBOM. Useful to compare slices without opening Combined.

### Delete a BOM

Version pane, DRAFT, not Combined, **not the last** BOM. Confirm. After 2→1 the tree immediately looks like a single-BOM version.

### Delete a DRAFT

Versions table (application pane). Confirm. If other drafts are based on this draft or its fingerprints, you see the list and must confirm **cascade**.

---

## Assets and relations (inside a BOM)

On a writable BOM (Edit + Open BOM):

- **Add** an existing pool asset or **Create** a new one (by type).  
- Select an asset: payload editor; **Related to** (outgoing) and **Related from** (incoming).  
- Tables use **Source · Type · Target**. The current asset is plain text; the other end is a link.  
- If the type has **no** outgoing (or no incoming) relationships, that section stays visible but **grayed out** (same empty table, no Create/Add).  
- **Create / Add** on a direction only when that direction has relationship types.  
- Removing a relation may offer to keep or delete assets that would be left with no relations.

Replace / Delete / Show on graph sit on the asset actions row.

---

## Graph tab

Same membership as the current union (Combined, subset, or one BOM).

| Action | Result |
|--------|--------|
| Click a **node** | Selects that asset (syncs the left asset list and Assets tab). |
| Click an **edge** | Selects the relation **on the graph only** (does not change the left pane). |
| Right-click an **edge** | **Go to…** → Source / Target (`Type Name@version`). Pans like **Show on graph**. |
| Right-click a **node** | Relation-role submenus first (**↓** inbound, **↑** outgoing), then **Go to source…** / **Go to target…** if neighbors exist. Submenus group by **asset type** (dimmed header) then `Name@version`. Isolated nodes have no menu. |
| Layout / view mode | Details vs Minimal; direction (TB/LR/…). **Navigate to selected** pans when you pick an asset. |

---

## Drafts, promote, fingerprints

### New draft

Header **New draft**. Choose **based on**:

- a **RELEASED** or **DRAFT** version, or  
- a **fingerprint**.

Set the **target version**. If the source **version** has more than one BOM, choose whether to **keep BOMs split** or **combine into a single BOM**. Fingerprint sources always become one BOM.

### Promote

Header **Promote** on a DRAFT. Re-type the version string to confirm (you may override the stored target if it stays unique). The version becomes RELEASED; BOMs stay as they were.

### Fingerprint

Header **Fingerprint** (button). Required **name** and **category** (`approval` / `history` / `unknown`). Always snapshots the **full Combined SBOM**, never a subset. Opening a fingerprint is a read-only snapshot (no BOM switch).

---

## Assets inventory and Portfolios

Under **Applications** chrome, **Assets** searches the global pool (type + searchable schema fields), shows usage, owner, and find-only duplicates.

**Portfolios** is a separate tab: taxonomy of applications (not versions) and **MI reports** (MI-1…MI-4). Reports always use each in-scope app’s **latest RELEASED Combined SBOM**. Drafts are never used for MI.

---

## Related

- Product locks and API sketch: [`example.md`](example.md)  
- Storage and union: [`GRAPH-AND-RETRIEVAL.md`](../../workitems/completed/20260817-multi-bom-app-versions/GRAPH-AND-RETRIEVAL.md)  
- Example README: [`../../../examples/sbom/README.md`](../../../examples/sbom/README.md)  
