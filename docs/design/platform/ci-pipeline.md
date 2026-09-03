# GitLab CI pipeline

**Status:** skeleton (unit + integration). Packaging and Docker publish are reserved, not implemented.  
**Pattern:** qpointz root orchestrator + child pipelines under `.gitlab/pipelines/`.  
**Code:** this doc describes CI YAML only. Gradle tasks are invoked as they already exist (`test`, `testIT`).

## Stages

```text
build → integration → publish → release
```

| Stage | Now | Later |
|-------|-----|--------|
| `build` | Child `test.yml`: `./gradlew test` + compile `:objs-persistence:testITClasses` | Per-module change filters |
| `integration` | Child `integration.yml`: `./gradlew testIT` | Extra IT jobs |
| `publish` | Stub child `publish-docker.yml` (not triggered) | Official + example images |
| `release` | Stub child `release.yml` (not triggered) | Tags: Hub + GitLab registry, Maven |

## File map

| Path | Role |
|------|------|
| `.gitlab-ci.yml` | Orchestrator: workflow, variables, child triggers |
| `.gitlab/common/common.yml` | Includes jobs + vars |
| `.gitlab/common/jobs.yml` | Includes templates |
| `.gitlab/common/vars.yml` | Git fetch defaults |
| `.gitlab/templates/gradle.yml` | `.gradle-job` (JDK 21, Gradle cache) |
| `.gitlab/templates/docker.yml` | `.docker-build-job` / `.docker-build-job-release` (unused) |
| `.gitlab/pipelines/test.yml` | Unit tests |
| `.gitlab/pipelines/integration.yml` | `testIT` |
| `.gitlab/pipelines/publish-docker.yml` | Reserved Docker child |
| `.gitlab/pipelines/release.yml` | Reserved tag child |

Root stays orchestration-only. Add new cross-cutting work as another file under `.gitlab/pipelines/` and a `trigger` in the root.

## Workflow (branch vs MR)

Same order as qpointz. First match wins:

1. Protected tag → pipeline
2. Unprotected tag → no pipeline
3. Merge-request event → pipeline
4. Protected ref (`dev`) → pipeline
5. Feature-branch **push** while an MR is open → **no** branch pipeline (MR pipeline only)
6. Other branches → pipeline

**GitLab setting:** Settings → CI/CD → General pipelines → **Merge request pipelines** must be on.

## Variables

| Variable | Default | Effect |
|----------|---------|--------|
| `RUN_INTEGRATION` | `false` | Force the integration child (also used from web/manual pipelines) |
| `RUN_FULL_TEST` | `false` | `true` forces the unit-test child; MR and protected branches still run it |
| `GRADLE_CONTINUE` | `false` | Pass `--continue` to Gradle |
| `GRADLE_FORCE_CLEAN` | `false` | Run `clean` before the Gradle job |

### When children run

**`test:downstream`**

- Never on a protected tag
- Always if `RUN_INTEGRATION == "true"` or protected **branch**
- Always if `RUN_FULL_TEST == "true"` or MR pipeline or protected branch
- Always on a feature-branch pipeline (no open MR — workflow already dropped the duplicate push)

**`integration:downstream`** (`needs: []`, does not wait for unit tests)

- Never on a protected tag
- Always if `RUN_INTEGRATION == "true"` **or** protected **branch**
- Not on MRs unless `RUN_INTEGRATION` is true

**`publish:docker:downstream` / `release:downstream`:** `when: never` until implemented.

## Gradle invocations

No Gradle files are owned by this pipeline.

| Job | Command |
|-----|---------|
| Unit | `./gradlew test :objs-persistence:testITClasses` |
| Integration | `./gradlew testIT` |

`testIT` against PostgreSQL:

| Environment | How Postgres is provided |
|-------------|--------------------------|
| CI (`integration.yml`) | GitLab service `postgres:17-alpine`; JDBC via `OBJS_IT_JDBC_URL` / `USER` / `PASSWORD` |
| Local | Testcontainers `postgres:17-alpine` when those env vars are unset |

The fixture (`ObjsPostgresPersistenceFixture`) chooses env JDBC when set; otherwise starts a container. No Docker-in-Docker on the integration job.

## Reserved Docker publish (not implemented)

Match qpointz: GitLab registry on feature branches; GitLab registry **and** Docker Hub `qpointz/<name>:<version>` on protected tags.

| Kind | Image name (planned) | Source |
|------|----------------------|--------|
| Official | `objs-service-app` | `:objs-service-app` workbench |
| Example | `sbom-service` | `:sbom-service` |
| Example | `asset-repository-service` | `:asset-repository-service` |

Templates live in `.gitlab/templates/docker.yml`. Child pipeline file exists; root trigger stays off.

## How to extend

1. New child: add `.gitlab/pipelines/<name>.yml` with `include: /.gitlab/common/common.yml` and `workflow: { rules: [{ when: always }] }`.
2. Root: `trigger: { include: { local: .gitlab/pipelines/<name>.yml }, strategy: depend }` plus `forward: { pipeline_variables: true }`.
3. Gate with a `RUN_*` variable and/or `CI_COMMIT_REF_PROTECTED` / `merge_request_event`, same style as `test:downstream` / `integration:downstream`.
4. Do not put large job scripts in `.gitlab-ci.yml`.
