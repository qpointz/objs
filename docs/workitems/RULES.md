# Work Item Rules

These rules apply to **every** work item and story implementation.

## Stories

A **story** is a coherent unit of delivery that maps to a single Git branch merged into `dev`.
Stories group related work items and are the primary unit of planning.

### `STORY.md`: WI tracker and per-WI commits (normative)

These rules apply to every story while it lives under **`planned/`**, **`in-progress/`**, and after
archive under **`completed/`**.

1. **WI tracker required** — Every **`STORY.md`** **must** include a **work-item tracker**: an ordered
   checklist of **all** WIs in that story (conventionally under a heading such as **`## Work Items`**),
   each line tied to a **`WI-NNN-<title>.md`** file in the same folder.
2. **Update after each WI** — When a work item is **complete**, set its tracker line to **`[x]`**
   **before** starting the next WI. The checklist must always match what is actually done on the
   branch. This is **mandatory** after every finished WI — not only at story closure.
3. **One commit = full working copy for that WI** — Completing a WI requires **one** commit that
   stages **every** intentional change for that WI **across the codebase** — production code, tests,
   config, and docs — **including** `STORY.md` (tracker update), the WI file when applicable, and any
   other trackers the WI obliges you to touch (**`MILESTONE.md`**, **`BACKLOG.md`**, design docs,
   etc.). Do not split a finished WI across multiple commits or leave related edits uncommitted. See
   **Commits** → **Per-WI cadence** and **Complete working copy per WI**.
4. **Commit and push after each WI** — After the per-WI commit, **push** the story branch to
   `origin`. This is mandatory after every completed WI so the remote branch, CI, and MR review stay
   current. See **Per-WI GitLab CI (when MCP available)** below.

### Story stages

If a story becomes logically large, split it into explicit **stages** inside `STORY.md` or a
story-local planning document. Stages should reflect execution order and dependency boundaries, not
discussion order. Keep WI numbering, tracker order, and stage order aligned so a reviewer can read
the story from top to bottom and understand what should be implemented first.

Each stage should identify:

- the WIs it contains
- whether it is ready to execute
- blocking decisions or prerequisite WIs
- which later WIs depend on it

Do this before implementation starts when the story is already large, or immediately when planning
reveals that the original WI list no longer matches the natural execution order.

### GitLab story issue (optional when MCP is available)

When GitLab MCP tools are available, a story may have exactly **one GitLab issue** tracking the
whole story. This is optional unless the user or team process requires it.

- Create the issue near story creation or before the first implementation WI.
- Link the issue from `STORY.md` when created.
- Use the issue for story-level scope, decisions, and MR linkage; do not create one issue per WI
  unless explicitly requested.
- Link merge requests for the story branch to the issue.
- Close the issue only after the story MR is merged. Do not close it merely because all local WIs
  are complete, tests pass, or an MR has been opened.

### Story folder layout

Stories live under **`planned/`**, **`in-progress/`**, or **`completed/`** (never as loose folders
at the `docs/workitems/` root). The **root** of `docs/workitems/` is only for shared artefacts:
`RULES.md`, `BACKLOG.md`, `MILESTONE.md` (draft of the **next** `releases/RELEASE-x.y.z.md`), and the
**`releases/`** directory.

```
docs/workitems/
  RULES.md
  BACKLOG.md
  MILESTONE.md
  releases/
    RELEASE-x.y.z.md
    ...
  planned/
    <story-slug>/
      STORY.md
      WI-NNN-<title>.md
      ...
  in-progress/
    <story-slug>/
      STORY.md
      WI-NNN-<title>.md
      ...
  completed/
    YYYYMMDD-<story-slug>/
      STORY.md
      WI-NNN-<title>.md
      ...
```

#### Placement rule (checkbox-based)

- **`planned/<story-slug>/`** — No WI is done yet: **every** task item in `STORY.md` that tracks a
  work item stays **`[ ]`** (unchecked). **Create new stories here.**
- **`in-progress/<story-slug>/`** — At least one WI is done: **`STORY.md` has at least one `[x]`**
  (checked) among those items. On the commit that marks the **first** WI complete, **move** the
  whole folder from `planned/<story-slug>/` → `in-progress/<story-slug>/` (same commit or same PR).
- **`completed/YYYYMMDD-<story-slug>/`** — Story closed per **Story closure** below; move from
  `planned/` or `in-progress/` to here (do not add new work under `completed/` except by archiving).

If `STORY.md` uses **numbered** checklist lines (`1. [ ]` / `1. [x]`), apply the same rule:
all unchecked → `planned/`; any checked → `in-progress/`.

- **Folder name**: lowercase, hyphen-separated slug of the story topic (same slug as you move
  between `planned/`, `in-progress/`, and `completed/YYYYMMDD-...`).
- **`STORY.md`**: required at story creation (**normative** detail: **`STORY.md`: WI tracker and per-WI
  commits** above). Must contain:
  - A short description of the story's goal.
  - **Mandatory WI tracker** — an ordered checklist of **all** WIs in the story under a heading such
    as **`## Work Items`** (same tracking list while **planned**, **in-progress**, and in the
    **completed/** archive):
    ```markdown
    ## Work Items
    - [ ] WI-NNN — Short title (`WI-NNN-<title>.md`)
    - [ ] WI-NNN — Short title (`WI-NNN-<title>.md`)
    ```
  - **While implementing:** after **each** WI is finished, set that line to **`[x]`** **before**
    starting the next WI — the tracker must always reflect what is actually done.
  - **As soon as a WI is finished:** update the tracking list (`[x]`), update the WI’s
    `WI-NNN-<title>.md` if the story expects it (notes, acceptance, status), then **commit** that
    WI’s **entire** intentional working copy — **all** related source, tests, and docs in one commit
    (see **Commits** below). Do not leave completed WIs unchecked or uncommitted while starting the
    next WI. If this is the **first** `[x]` for the story, **move** the folder from
    `planned/<story-slug>/` to `in-progress/<story-slug>/` (same commit or PR).
- **WI files**: all WI markdown files for the story live under that story’s folder under
  `planned/` or `in-progress/` (and eventually under `completed/`), never at the top level of
  `docs/workitems/`.

### Story closure

#### Explicit closure only (agents)

**Do not close a story** — and do **not** perform any **Story closure** step below — until the
**user explicitly asks** to close it (e.g. “close the story”, “story is merge-ready”, “archive the
story”). Finishing the last WI, passing tests, or opening a PR is **not** sufficient on its own.

Until that explicit request:

- Leave the story folder under **`in-progress/<story-slug>/`** (even when every WI is `[x]`).
- Do **not** move the folder to **`completed/`**.
- Do **not** update **`MILESTONE.md`**, set **`BACKLOG.md`** rows to **`done`**, or run the
  **Completion (Story level)** history rewrite for closure purposes.
- Continue to follow **per-WI** tracking and commits; optional follow-up WIs or polish on the same
  branch remain valid while the story stays open.

When the user **does** ask to close the story, perform the steps below **in order** in one
coordinated pass unless they specify otherwise.

**Clean working tree (required at closure):** before starting step **0**, and again before declaring
the story closed, **`git status` must be clean** — every intentional change **committed** (no
modified or untracked story files left behind). Commit any in-progress edits (including closure
doc updates from steps **1–5**) before history rewrite if they would be lost, and commit the final
closure doc/archive commit(s) before push or hand-off. Do not archive with a dirty working tree.

When all WIs in a story are complete **and** the user has requested closure:

0. **Rewrite branch history (required)** — per **Completion (Story level)** below: rebase onto the
   merge target (usually **`origin/dev`**), then **logically combine and squash** per-WI commits
   into a small, reviewable set (~10 commits or fewer when practical). **Do not** archive the story
   or update **`MILESTONE.md`** / **`BACKLOG.md`** until this step is done and the branch is
   **MR-ready**.
1. **Update `MILESTONE.md`** — record the story's completed WIs in the **next release** section
   only (see **Milestone ledger (`MILESTONE.md`)** below); use the same compact bullet format as
   existing entries. Do **not** add sections for already **tagged** releases.
2. **Update `BACKLOG.md`** — set any related rows to **`done`**, and add or adjust deferred follow-ups
   as **`backlog`** / **`planned`** / **`in-progress`**. **Do not delete** completed rows here; they
   stay until **Release housekeeping** (see **Release (version) process** below).
3. **Update or create design docs** under the relevant `docs/design/<component>/` section
   (e.g. `agentic/`, `metadata/`, `platform/`, `security/`) — capture decisions, architecture
   notes, and anything a future agent or developer needs to understand the system. Design docs
   are organised by logical component, not by story.
4. **Update or create user documentation** under `docs/public/src/` — ensure the public-facing
   docs reflect any new features or changed behaviour.
5. **Archive the story folder** — do **not** delete it. Move the entire folder from
   **`docs/workitems/planned/<story-slug>/`** or **`docs/workitems/in-progress/<story-slug>/`**
   to:
   `docs/workitems/completed/YYYYMMDD-<story-slug>/`, where:
   - **`YYYYMMDD`** is the **story closure date** (UTC unless the team agrees otherwise —
     the calendar day the work is accepted as merge-ready or merged).
   - **`<story-slug>`** is the original active folder name (lowercase hyphen-separated slug).
   - Example: `docs/workitems/completed/20260330-metadata-persistence/`

   The archived folder preserves `STORY.md`, all WI files, and any other story-local artefacts
   as the historical record. Durable summaries remain in **`MILESTONE.md`** (next release block
   only), **`releases/`**, and **design / public docs**. **`BACKLOG.md`** may still list **`done`**
   rows until the next **release** (see **Release (version) process**).

   **Ordering:** With a `YYYYMMDD-` prefix, **ascending** name sort lists **oldest** closures first.
   To see **most recent closures first**, sort folder names **descending** (reverse alphabetical)
   in your file browser, or maintain the index in `docs/workitems/completed/README.md`.

6. **Verify clean tree and push rewritten branch** — `git status` shows nothing to commit; all
   closure commits are on the story branch. Because closure always rewrites history, push the
   rewritten story branch per **Completion (Story level)** → **Push after rewrite**.
7. **Create or update merge request** — if an MR for the story branch does not already exist, create
   one after the rewritten branch is pushed. If an MR already exists, update its description with the
   final story scope, WI list, and verification evidence.

Merging into `dev` is done manually by the user; the agent prepares everything above first.

## Milestone ledger (`MILESTONE.md`)

- **`MILESTONE.md` is the draft of `RELEASE-x.y.z.md`:** for the **next** version only, the
  **`## x.y.z`** block is the **working pre-release** — the same content you will **promote** (after
  any editorial pass: Highlights, compare link, polish) into
  **`docs/workitems/releases/RELEASE-x.y.z.md`** when git tag **`vx.y.z`** is cut. Until then, keep
  building the draft here; **`RELEASE-x.y.z.md`** may exist as a short stub pointing at this file.
- **Forward-looking:** one milestone block at a time — work merged **after** the latest shipped tag
  **`v*.*.*`** and **before** the next tag. **`MILESTONE.md`** is **not** a permanent archive of past
  releases.
- **Already shipped** versions are documented only in **`releases/RELEASE-x.y.z.md`**. Do **not** keep
  a **`## x.y.z`** section in **`MILESTONE.md`** once **`vx.y.z`** exists.
- **When version `x.y.z` is released** (git tag **`vx.y.z`**): fold **`MILESTONE.md`** § **`x.y.z`**
  into **`releases/RELEASE-x.y.z.md`**, then **remove** that section from **`MILESTONE.md`** and open
  a draft for the **following** milestone only. See **Release (version) process** below.

## Release (version) process

**Between releases** — for example after **`0.7.0`** is recorded and development continues toward
**`0.8.0`** on `dev`:

- **`BACKLOG.md` may include `done` rows.** When a backlog item ships (or a story you track there
  closes), set **Status** to **`done`** and tighten **Source** (e.g. pointer to `MILESTONE.md` / WI).
- Those rows **remain** in the file until **release housekeeping** for the **next** version. They are
  a convenient “since last tag / last release notes” ledger inside the repo.

**Release housekeeping** — when you **cut** version **`x.y.z`** (documentation and tracker pass,
typically same moment as or immediately after you treat the milestone as **released**):

1. **`releases/RELEASE-x.y.z.md`** — **promote** the draft from **`MILESTONE.md`** § **`x.y.z`** into
   this file (merge with any stub). Add compare link, **Highlights**, and polish so it matches the
   shape of existing **`RELEASE-*.md`** files. This is the **canonical** shipped record for
   **`vx.y.z`**.
2. **`MILESTONE.md`** — **delete** the **`## x.y.z`** draft block. The file must contain **only** the
   **next** milestone (e.g. **`## 0.9.0`**) as a fresh **draft** of the **next**
   **`RELEASE-*.md`** — **§ Completed** / **§ In Progress** / **§ Planned** as appropriate, or an
   empty **§ Completed** until new work lands. Roll any still relevant **Planned** items into that
   next block. See **Milestone ledger (`MILESTONE.md`)** above.
3. **`BACKLOG.md` — prune** — **delete every table row** whose **Status** is **`done`**. Shipped work
   is recorded under **`releases/`** (and story archives), **not** under past blocks of
   **`MILESTONE.md`**. In the same pass, you may remove **`cancelled`** / **`superseded`** rows if
   you want a minimal open tracker.
4. **`BACKLOG.md` — Summary** — if the file includes per-category totals, **recalculate** them over
   **open** rows only (`backlog` \| `planned` \| `in-progress`).

**Relation to story closure:** story closure **marks** backlog rows **`done`**; **release** **removes**
those rows. One-off cleanups (e.g. bulk-delete historical `done` rows when adopting this policy) are
allowed; thereafter follow the cycle above.

---

## Branching

- Each story is implemented on a **dedicated branch**. **Usually** it is created from `origin/dev`;
  it may legitimately branch from another integration point when a story depends on unmerged work —
  at **closure**, use the branch’s **actual merge target** (normally `origin/dev`) when rebasing and
  when deciding what to squash.
- Preferred when starting from mainline: `git fetch origin && git checkout -b <story-slug> origin/dev`.
- Individual WIs within a story share the same branch; they are **not** separated into sub-branches
  unless a WI explicitly depends on another that is not yet merged.
- Before the branch is ready for review, **rebase** against `origin/dev`:
  `git fetch origin && git rebase origin/dev`.
- Never commit directly to `dev`. Never reuse a previous story branch.

## Merge request review

When an agent is asked to **review a merge request** (or MR feedback is left on the story branch):

1. **Propose changes** — summarise findings and concrete refactors; do not implement until the user confirms or agrees.
2. **Implement when confirmed** — once the user accepts the proposed changes, apply them on the story branch, run relevant tests, and **finish with commit and push** to the MR source branch unless the user says otherwise.
3. **Resolve threads** — mark or reply on MR discussion threads when the feedback is addressed (user may do this in GitLab UI).
4. **Do not** treat “review only” as complete if the user subsequently agrees to the proposal — agreed review feedback is delivery work on the same branch.

## Commits

- At the end of each work item, squash its changes into **one logical commit**.
- Keep commits minimal — ideally **one commit per WI**, labelled with the WI identifier.
- **Never** add `Co-Authored-By` or similar trailers to commit messages.
- Follow the bracketed prefix style: `[feat]`, `[fix]`, `[change]`, `[refactor]`, `[docs]`, `[wip]`.

### Per-WI cadence (story implementation)

Normative summary: **`STORY.md`: WI tracker and per-WI commits** above. While implementing a story on
its branch, treat each WI as a closed loop:

1. **Finish the WI** — code, tests, and docs required by that WI.
2. **Update tracking** — set the matching item to `[x]` in `STORY.md` **immediately** when the WI is
   done; update `WI-NNN-<title>.md` when the story or WI template calls for it. **Do not** start the
   next WI with the previous WI still unchecked.
3. **Commit** — stage everything that belongs to that WI (including tracking/doc edits) and create one
   commit so `git status` is clean before the next WI.
4. **Push** — push the story branch to `origin` (`git push -u origin HEAD` on first push, then
   `git push`).
5. **CI (when GitLab MCP is available)** — after push, confirm the pipeline for the branch commit is
   **green** (or wait until it succeeds) before starting the next WI. See **Per-WI GitLab CI**.

If you complete a WI but skip updating `STORY.md`, skip the commit, skip push, or skip CI verification
when MCP is available, the branch no longer matches the story’s tracking list and delivery is harder
to review.

### Per-WI GitLab CI (when MCP available)

When **GitLab MCP** tools are configured in the agent environment, use them during story
implementation on the feature branch:

#### After every WI (after commit + push)

1. **Push** the story branch if not already on `origin`.
2. **Trigger or observe CI** — a push to the feature branch should start the GitLab pipeline for that
   ref. Use GitLab MCP (pipeline / commit-status APIs) to confirm the relevant jobs **succeed**.
3. **Do not start the next WI** while the pipeline for the current WI’s commit is **failed**, **stuck**,
   or **unknown** — fix failures, amend with a new commit if needed, push again, and re-check until
   green (or report a blocker to the user).
4. **Local tests** — run module-appropriate tests locally as part of the WI (e.g. `./gradlew test`,
   `./gradlew :objs-core:test :objs-service:test`) **before** push; CI is the remote
   confirmation, not a substitute for local verification when the WI touches that code.

Which pipeline jobs apply depends on the change (Gradle modules, etc.); at minimum expect the
**default / build / test** stage jobs for the project to pass.

#### Last WI in the story (before MR)

When completing the **final** WI in `STORY.md` (all other items already `[x]`):

1. Complete the usual per-WI loop (track, commit, push).
2. **Run or confirm the integration-test pipeline** — ensure integration-stage jobs pass for the branch.
   In this repo that typically includes downstream integration jobs (e.g. **`integration:downstream`**
   from [`.gitlab-ci.yml`](../../.gitlab-ci.yml) / `.gitlab/pipelines/integration.yml`), subject to
   project CI variables (`RUN_INTEGRATION`, MR rules, etc.). Use GitLab MCP to verify integration jobs
   **success** on the latest commit; fix and re-push if they fail.
3. **Optional early merge request** — when GitLab MCP is available, an MR may be opened from the
   story branch into the merge target (usually **`dev`**) with a summary of the story, WI list, test
   evidence, and link to the green pipeline. If an MR already exists for the branch, update its
   description instead of opening a duplicate. If no MR is opened here, story closure must create it.

**Story closure** (archive, MILESTONE, history squash) remains **explicit user request only** — opening
an MR after the last WI does **not** close the story by itself.

#### When GitLab MCP is not available

Fall back to local test commands documented in the story or `STORY.md` / WI files; still commit and
push after each completed WI; note in hand-off that CI was not verified remotely.

### Complete working copy per WI (story branches)

On a **story branch**, when a WI is **complete** (implementation + tests/docs as required by that WI):

1. **Commit every file** that is part of that WI’s delivery — source, tests, story docs (`STORY.md` tracking list / checkbox, WI file updates if any), and any other intentional edits. Do not stop with a **partial** commit while leaving related changes unstaged for the same WI.
2. **Leave a clean working tree** for that slice of work: after the commit, `git status` should show no remaining modified/untracked files **for completed work** (aside from deliberate local-only files such as IDE noise, if those are gitignored).
3. **Same commit** should normally include marking the WI as done in **`STORY.md`** (`[x]`) so the branch always reflects completed WIs and the tracking list stays accurate.
4. **Push** the commit to `origin` on the story branch — see **Per-WI cadence** step 4.

This keeps each WI a **reviewable, reproducible checkpoint** on the story branch. Do not accumulate multiple finished WIs worth of changes without committing. Do not commit build outputs, secrets, or unrelated work from outside the story.

## Completion (WI level)

- When the WI is implemented, update the **tracking list** in `STORY.md` (checkbox `[x]`) **in the same
  commit** as the WI’s code/docs — see **Per-WI cadence** and **Complete working copy per WI** above.
- **Push** that commit to `origin` and, when GitLab MCP is available, confirm pipeline success before
  the next WI — see **Per-WI GitLab CI (when MCP available)**.
- Do **not** remove or relocate WI files until **story closure** — they stay with the story folder
  through its move from `planned/` or `in-progress/` to `docs/workitems/completed/`.

## Module Reference

- `objs-core` — Entity SDK, domain types, JPA / PostgreSQL persistence, typed-domain toolkit
- `objs-service` — Spring REST API and Boot autoconfiguration (library); foundation side service (with UI)
- `objs-service-app` — Workbench-only runnable (`./gradlew :objs-service-app:run`, port 8081); no example apps
- `examples/sbom/sbom-service` (`:sbom-service`) — Concrete SBOM inventory app (canonical ontology, domain API; **must not** use objs-service)
- `examples/sbom/sbom-service-ui` (`:sbom-service-ui`) — Inventory SPA (node-gradle, like `:objs-service-ui`)

---

## Flyway (library + derived apps)

objs is a **library**. Schema for `bom_*` is owned by **objs-core** and applied by **objs autoconfig
Flyway**, not by the embedding app’s Boot Flyway.

### Two independent lines

Same DataSource, two Flyway beans, two history tables. Version numbers are unique **per history
table**, not globally. Both lines may ship `V1`.

| Owner | Locations | History table | Versions |
|-------|-----------|---------------|----------|
| objs-core | `classpath:org/poc/objs/core/db/migration/{vendor}` | `flyway_schema_history_objs` | objs `V1…VN` |
| Derived app (including `examples/*`) | Boot `classpath:db/migration` or `classpath:db/migration/{vendor}` (**app SQL only**) | `flyway_schema_history` | app `V1…VN` |

`{vendor}` is the Spring Boot Flyway placeholder: `DatabaseDriver` id from the JDBC URL
(`postgresql`, `h2`, …). Do **not** use JDBC `databaseProductName` (`"PostgreSQL"`). Unknown driver
**fails fast**.

Startup **order is Spring**, not version comparison: objs Flyway first, then Boot Flyway, then JPA
`ddl-auto: validate`. App `V1` never races objs `V2`. Autoconfig sets Boot Flyway
`baselineOnMigrate` with `baselineVersion` `0` so the app history can apply its own `V1`
after objs has already created `bom_*`.

### What a story must change

- **`bom_*` schema** — add objs SQL for **both** `postgresql` and `h2` in objs-core in the **same
  WI**. Do **not** add a version on SBOM, asset-repository, or any other app line.
- **App-owned tables** — next version on **that app’s** Boot Flyway line only.
- Derived apps **must not** add objs locations to `spring.flyway.locations`. Inclusion is the second
  Flyway bean (autoconfig), not extra locations. Do **not** copy objs SQL into app `db/migration`.
- Examples are derived apps. Workbench runner with no app DDL (`:objs-service-app`):
  `spring.flyway.enabled=false`; objs autoconfig still runs.
- Until existing databases are gone: **greenfield only** — recreate the database or drop both history
  tables plus domain tables. Do not repair a split of one merged `flyway_schema_history`.

### Do not

- One `flyway_schema_history` + `classpath:db/migration` seeing both JARs (merged sequence).
- Reserved version ranges (`V100`, skip objs numbers) to avoid collisions.
- Putting objs folders on `spring.flyway.locations` “to include them”.

---

## Concrete example integration (`examples/sbom`)

`:sbom-service` (under `examples/sbom/`) is the **living consumer** of the graph foundation and the **canonical software
graph** ontology. New product features must not land in `objs-core` / `objs-service` alone while the
example stays stale.

### When integration is required

For every story/WI that changes **consumer-visible** foundation or domain capability — including but
not limited to entity/edge model, catalogs, validation, `BoMGraphStore`, generic REST
(`/api/v1/objs/**`), typed toolkit (`org.poc.objs.core.typed`), annotations/subgraphs, OpenAPI shapes,
or the **canonical ontology** — the **same WI** (or an explicit companion WI in the same story)
**must** update `:sbom-service` (and its UI when user-facing) so the feature is exercised and documented end-to-end.

### What “integrated” means

Update whichever of the following the change implies (all that apply, in the **same** per-WI commit):

| Area | Typical touchpoints |
|------|---------------------|
| Registry / allow-list | `SbomRegistry.pack()` schemas and edge rules |
| Typed domain | payloads / `EntityTypeMeta` under `org.poc.objs.sbom.model`; builders/helpers |
| App behaviour | `SbomService`, `SbomGraphBuilder`, controller, auto-config, demo seed |
| API docs | OpenAPI customizer / example group when request/response shapes change |
| Tests | module tests proving the new capability via the example path |
| Design | [`docs/design/sbom/`](../design/sbom/) — especially `example.md` and, for ontology edits, **`canonical-spec.md`** |
| Persistence / Flyway | objs `bom_*` → objs-core `{vendor}` SQL; app tables → that example’s `db/migration` only. See **Flyway (library + derived apps)** |

Ontology additions or relationship-table changes are incomplete until **`canonical-spec.md`**, typed
models, and `SbomRegistry` agree.

### Planning

- Call out example integration in `STORY.md` / WI acceptance (or a dedicated WI) when the story
  touches foundation or ontology.
- Prefer integrating in the **feature WI** itself; use a follow-up WI in the **same story** only when
  the feature WI would otherwise become unreviewably large — never defer to a later story by default.
- `:objs-service-app` does **not** depend on example modules (workbench-only runner).

### Exceptions

Integration may be skipped **only** when the change is clearly invisible to domain apps (pure
internal refactor, test-only harness, CI/docs process with no runtime API) **or** the story’s
`GAPS.md` / user explicitly marks the example out of scope for that WI. When skipping, record the
reason in the WI or `GAPS.md`.

---

## Completion (Story level)

**When:** only after the user **explicitly requests story closure** (see **Story closure** →
**Explicit closure only (agents)**). This section is **step 0** of **Story closure** — run it
**before** MILESTONE / BACKLOG / archive. Do not archive or mark backlog **`done`** on a branch that
still has raw per-WI commit noise.

**Purpose:** per-WI commits are a convenience during implementation; **at closure** the story branch
must always be **rebased and squashed from the original branch base** (usually `origin/dev`) into
logical review units so reviewers see a deliberate history, not one commit per WI by default.

### Prerequisites

- **Clean working tree** — commit or stash nothing that belongs on the story; see **Story closure**
  → **Clean working tree**. Rebase/squash must not leave uncommitted intentional edits.

### Merge target and rebase

1. **Fetch and choose merge base** — default MR target is **`origin/dev`**:
   `git fetch origin`
   Use `git merge-base HEAD origin/dev` (or the branch the user named as MR target) as the rewrite
   base. If the story branched from another integration branch, use that branch consistently.
2. **Rebase onto latest target** — before squashing:
   `git rebase origin/dev`
   Resolve conflicts; re-run tests if the rebase touched substantial code.

### Squash and logical grouping

3. **Combine commits above the merge base** — always squash/recompose the commits since the original
   branch base into logical units grouped by **substance of change** (feature area, module, risk
   boundary), not by WI number alone. Acceptable approaches:
   - Interactive rebase: `git rebase -i <merge-base>` — `squash` / `fixup` related commits,
     `reword` messages, split commits that mix unrelated concerns.
   - Soft reset + new commits: `git reset --soft <merge-base>` then create a fresh logical commit
     sequence (same bar as below).
4. **Ordering** — commits should read top-to-bottom as a coherent story (foundation → wiring →
   migration → tests → docs is a common pattern; match what the change actually needs).
5. **MR-ready bar** — after rewrite, `git log <merge-base>..HEAD` should show:
   - Clear messages (bracket prefix, imperative, under 72 chars).
   - No `[wip]` or fixup-only noise unless intentionally kept.
   - **Guideline:** **~10 commits or fewer** above the merge base; more is OK when splitting reduces
     review risk (large refactors, unrelated modules).

### Then documentation closure

6. With history rewritten and tests still green, continue **Story closure** steps **1–5**
   (MILESTONE, BACKLOG `done`, design docs, public docs, archive to
   `docs/workitems/completed/YYYYMMDD-<story-slug>/`). Commit those doc/tracker changes; working
   tree must be clean before step **6** (verify) in **Story closure**. **BACKLOG row deletion** is
   **not** part of story closure — it happens in **Release (version) process** above.

### Push after rewrite

Story closure always rewrites history, so after closure commits are complete, **force-push the
story feature branch** with lease:

```bash
git push --force-with-lease origin <feature-branch>
```

- **Allowed:** force-push (prefer **`--force-with-lease`**) to the **story / feature branch** only.
- **Forbidden:** never force-push protected integration branches — **`main`**, **`dev`**, **`rc`**
  (and any other branch the team treats as shared integration).
- The force-push is mandatory at story closure so the remote branch and MR reflect the squashed,
  review-ready history.

### Merge request at closure

At story closure, create an MR if one does not already exist. If an MR already exists, update its
description instead of opening a duplicate.

- Target the story's merge branch, usually `dev`.
- Include story scope, WI list, closure verification, and relevant CI/test evidence.
- Link the GitLab story issue when one exists.
- The MR is created or updated after the rewritten branch is force-pushed.
