# Gaps — export-package-sensitive-assets (P-5)

All decisions below are **locked**. Implementation is WI-001 / WI-002; G-3 stays out of this story.

| ID | Topic | Decision | Status |
|----|-------|----------|--------|
| G-1 | Non-code package rewrite | Add `.drl` to `REPLACE_EXTENSIONS`; no other missing extensions | **locked** → WI-001 |
| G-2 | Codegen output path guard | Reject only exact foundation module dir segments; allow any nesting / incidental `objs-*` ancestors / app modules | **locked** → WI-002 |
| G-3 | Export module move list incomplete | Do not expand `TOP_LEVEL_MODULES` in this story | **locked deferred** |

## G-2 (detail)

**Bug:** `startsWith("objs-")` on any path segment rejects valid app output under nested monorepos.

**Lock:** exact foundation module directory names only (`objs-api`, `objs-codegen-java`, `objs-persistence`, …).
