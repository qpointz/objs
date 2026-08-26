#!/usr/bin/env python3
"""Generate .dumper.yml for bom-poc source export into OUT_DIR."""
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

import yaml

SOURCE_PACKAGE = "org.poc.objs"
SOURCE_GROUP = "org.poc.objs"
SOURCE_ROOT_PROJECT = "objs"
SOURCE_API_VERSION = "objs.poc.org/v1"
SOURCE_API_VERSION_QUOTED = '"objs.poc.org/v1"'

REPLACE_EXTENSIONS = [
    ".kt",
    ".java",
    ".kts",
    ".ts",
    ".tsx",
    ".html",
    ".yml",
    ".yaml",
    ".json",
    ".imports",
    ".py",
    ".md",
    ".properties",
    ".gradle",
]

TOP_LEVEL_MODULES = [
    "objs-core",
    "objs-service",
    "objs-service-ui",
    "objs-gremlin-core",
    "objs-gremlin-service",
    "objs-service-app",
]

EXAMPLE_MODULES = [
    ("examples/sbom/sbom-service", "sbom-service"),
    ("examples/sbom/sbom-service-ui", "sbom-service-ui"),
    (
        "examples/asset-repository/asset-repository-service",
        "asset-repository-service",
    ),
    (
        "examples/asset-repository/asset-repository-service-ui",
        "asset-repository-service-ui",
    ),
]

GRADLE_PROJECTS = [
    ":asset-repository-service-ui",
    ":asset-repository-service",
    ":objs-gremlin-service",
    ":objs-gremlin-core",
    ":objs-service-app",
    ":objs-service-ui",
    ":objs-service",
    ":objs-core",
    ":sbom-service-ui",
    ":sbom-service",
]


def derive_module_prefix(target_package: str) -> str:
    return target_package.rsplit(".", 1)[-1]


def derive_api_version(target_package: str) -> str:
    parts = target_package.split(".")
    return f"{'.'.join(reversed(parts))}/v1"


def package_slug(target_package: str) -> str:
    return target_package.replace(".", "-")


def renamed_module(prefix: str, module: str) -> str:
    if module.startswith("objs-"):
        return f"{prefix}-{module[len('objs-'):]}"
    return f"{prefix}-{module}"


def package_path(package: str) -> str:
    return package.replace(".", "/")


def validate_target_package(value: str) -> None:
    if not re.fullmatch(r"[a-z][a-z0-9]*(?:\.[a-z][a-z0-9]*)+", value):
        raise ValueError(
            f"TARGET_PACKAGE must look like a Java package (got {value!r})"
        )


def load_cleanup_manifest(path: Path) -> tuple[list[str], list[str]]:
    if not path.is_file():
        return [], []
    with path.open("r", encoding="utf-8") as handle:
        data = yaml.safe_load(handle) or {}
    delete_dirs = list(data.get("delete_dirs") or [])
    delete_files = list(data.get("delete_files") or [])
    return delete_dirs, delete_files


def build_replace_rules(
    *,
    target_package: str,
    module_prefix: str,
    api_version: str,
    root_project_name: str,
) -> list[dict[str, str]]:
    rules: list[dict[str, str]] = []

    def add(old: str, new: str):
        if old != new:
            rules.append({"old": old, "new": new})

    add(SOURCE_API_VERSION_QUOTED, f'"{api_version}"')
    add(SOURCE_API_VERSION, api_version)
    add(package_path(SOURCE_PACKAGE), package_path(target_package))

    for project in sorted(GRADLE_PROJECTS, key=len, reverse=True):
        bare = project[1:]
        add(project, f":{renamed_module(module_prefix, bare)}")

    add(":objs-sbom-example", f":{renamed_module(module_prefix, 'sbom-service')}")
    add(":objs-app", f":{renamed_module(module_prefix, 'service-app')}")

    for rel_path, module in sorted(EXAMPLE_MODULES, key=lambda item: len(item[0]), reverse=True):
        add(rel_path, rel_path.rsplit("/", 1)[0] + "/" + renamed_module(module_prefix, module))

    for module in sorted(TOP_LEVEL_MODULES, key=len, reverse=True):
        add(module, renamed_module(module_prefix, module))

    add("objs-sbom-example", renamed_module(module_prefix, "sbom-service"))
    add("objs-app", renamed_module(module_prefix, "service-app"))

    add(f'group = "{SOURCE_GROUP}"', f'group = "{target_package}"')
    add(f'rootProject.name = "{SOURCE_ROOT_PROJECT}"', f'rootProject.name = "{root_project_name}"')
    add(SOURCE_PACKAGE, target_package)

    add(
        " — see [`docs/workitems/RULES.md`](docs/workitems/RULES.md)",
        "",
    )
    add(
        "Normative process: [`docs/workitems/RULES.md`](docs/workitems/RULES.md).",
        "",
    )
    add(
        "Stories under `docs/workitems/planned/<story-slug>/` or "
        "`docs/workitems/in-progress/<story-slug>/`.",
        "",
    )
    add("../../workitems/", "")
    add("docs/workitems/", "")

    return rules


def build_actions(
    *,
    target_package: str,
    module_prefix: str,
    api_version: str,
    root_project_name: str,
    cleanup_dirs: list[str],
    cleanup_files: list[str],
) -> list[dict]:
    actions: list[dict] = []

    actions.append(
        {"move_package": {"from": SOURCE_PACKAGE, "to": target_package}}
    )

    for module in TOP_LEVEL_MODULES:
        actions.append(
            {
                "move_dir": {
                    "from": module,
                    "to": renamed_module(module_prefix, module),
                }
            }
        )

    for rel_path, module in EXAMPLE_MODULES:
        parent, _ = rel_path.rsplit("/", 1)
        actions.append(
            {
                "move_dir": {
                    "from": rel_path,
                    "to": f"{parent}/{renamed_module(module_prefix, module)}",
                }
            }
        )

    actions.append(
        {
            "replace_in_files": {
                "exts": REPLACE_EXTENSIONS,
                "replace": build_replace_rules(
                    target_package=target_package,
                    module_prefix=module_prefix,
                    api_version=api_version,
                    root_project_name=root_project_name,
                ),
            }
        }
    )

    orphan_dirs = TOP_LEVEL_MODULES + [path for path, _ in EXAMPLE_MODULES]
    actions.append({"delete_dir": {"paths": orphan_dirs}})

    if cleanup_dirs:
        actions.append({"delete_dir": {"paths": cleanup_dirs}})
    if cleanup_files:
        actions.append({"delete_file": {"paths": cleanup_files}})

    actions.append({"delete_empty_folder": None})
    return actions


def generate_config(
    *,
    out_dir: str,
    target_package: str,
    module_prefix: str | None = None,
    api_version: str | None = None,
    root_project_name: str | None = None,
    cleanup_config: Path | None = None,
) -> dict:
    validate_target_package(target_package)
    prefix = module_prefix or derive_module_prefix(target_package)
    version = api_version or derive_api_version(target_package)
    root_name = root_project_name or prefix
    cleanup_dirs, cleanup_files = load_cleanup_manifest(cleanup_config or Path())

    return {
        "dump": {
            "in": {"root_dir": os.path.abspath(out_dir)},
            "actions": build_actions(
                target_package=target_package,
                module_prefix=prefix,
                api_version=version,
                root_project_name=root_name,
                cleanup_dirs=cleanup_dirs,
                cleanup_files=cleanup_files,
            ),
        }
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Write .dumper.yml for bom-poc export")
    parser.add_argument("--out-dir", required=True)
    parser.add_argument("--target-package", required=True)
    parser.add_argument("--module-prefix")
    parser.add_argument("--api-version")
    parser.add_argument("--root-project-name")
    parser.add_argument("--cleanup-config")
    parser.add_argument(
        "--output",
        help="Config output path (default: OUT_DIR/.dumper.yml)",
    )
    args = parser.parse_args(argv)

    try:
        config = generate_config(
            out_dir=args.out_dir,
            target_package=args.target_package,
            module_prefix=args.module_prefix,
            api_version=args.api_version,
            root_project_name=args.root_project_name,
            cleanup_config=Path(args.cleanup_config) if args.cleanup_config else None,
        )
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        return 1

    output_path = Path(args.output or os.path.join(args.out_dir, ".dumper.yml"))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as handle:
        yaml.safe_dump(config, handle, default_flow_style=False, sort_keys=False)

    print(f"Wrote {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
