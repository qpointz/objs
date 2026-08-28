#!/usr/bin/env python3
"""Self-check for scripts/export/dumper.py on a tiny fixture tree."""
from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
FIXTURE_SRC = Path(__file__).resolve().parent / "test" / "fixture"
DUMPER = Path(__file__).resolve().parent / "dumper.py"
GENERATE = Path(__file__).resolve().parent / "generate-config.py"
CHECK_OUT_DIR = Path(__file__).resolve().parent / "check_out_dir.py"


def run(cmd: list[str], *, cwd: Path | None = None) -> None:
    result = subprocess.run(cmd, cwd=cwd, check=False, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr, file=sys.stderr)
        raise SystemExit(result.returncode)


def main() -> int:
    outside = subprocess.run(
        [
            sys.executable,
            str(CHECK_OUT_DIR),
            "--out-dir",
            "/tmp/bom-export-outside-check",
            "--source-repo",
            str(REPO_ROOT),
        ],
        check=False,
    )
    if outside.returncode != 0:
        print("check_out_dir rejected valid outside path", file=sys.stderr)
        return 1

    inside = subprocess.run(
        [
            sys.executable,
            str(CHECK_OUT_DIR),
            "--out-dir",
            str(REPO_ROOT / "build" / "export-staging"),
            "--source-repo",
            str(REPO_ROOT),
        ],
        check=False,
    )
    if inside.returncode == 0:
        print("check_out_dir should reject in-repo path", file=sys.stderr)
        return 1

    with tempfile.TemporaryDirectory(prefix="bom-export-fixture-") as tmp:
        out_dir = Path(tmp) / "out"
        shutil.copytree(FIXTURE_SRC, out_dir)

        run(
            [
                sys.executable,
                str(GENERATE),
                "--out-dir",
                str(out_dir),
                "--target-package",
                "com.example.demo",
            ]
        )
        run(
            [
                sys.executable,
                str(DUMPER),
                "--root",
                str(out_dir),
                "--config",
                str(out_dir / ".dumper.yml"),
                "--source-repo",
                str(REPO_ROOT),
            ]
        )

        moved = (
            out_dir
            / "demo-core"
            / "src"
            / "main"
            / "kotlin"
            / "com"
            / "example"
            / "demo"
            / "core"
            / "Sample.kt"
        )
        if not moved.is_file():
            print(f"Expected transformed file missing: {moved}", file=sys.stderr)
            return 1

        content = moved.read_text(encoding="utf-8")
        if "com.example.demo" not in content:
            print("Package replace failed in fixture", file=sys.stderr)
            return 1
        if "org.poc.objs" in content:
            print("Leftover source package in fixture", file=sys.stderr)
            return 1
        if "demo.example.com/v1" not in content:
            print("apiVersion replace failed in fixture", file=sys.stderr)
            return 1

        imports = out_dir / "demo-core" / "src" / "main" / "resources" / "META-INF" / "spring" / "org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        imports_text = imports.read_text(encoding="utf-8")
        if "com.example.demo.core.DemoAutoConfiguration" not in imports_text:
            print("SPI .imports replace failed in fixture", file=sys.stderr)
            return 1

    with tempfile.TemporaryDirectory(prefix="bom-export-source-package-fixture-") as tmp:
        out_dir = Path(tmp) / "out"
        shutil.copytree(FIXTURE_SRC, out_dir)
        run(
            [
                sys.executable,
                str(GENERATE),
                "--out-dir",
                str(out_dir),
                "--target-package",
                "org.poc.objs",
            ]
        )
        run(
            [
                sys.executable,
                str(DUMPER),
                "--root",
                str(out_dir),
                "--config",
                str(out_dir / ".dumper.yml"),
                "--source-repo",
                str(REPO_ROOT),
            ]
        )

        retained = (
            out_dir
            / "objs-core"
            / "src"
            / "main"
            / "kotlin"
            / "org"
            / "poc"
            / "objs"
            / "core"
            / "Sample.kt"
        )
        if not retained.is_file():
            print(f"Source package was removed during no-op export: {retained}", file=sys.stderr)
            return 1

    with tempfile.TemporaryDirectory(prefix="bom-export-prefix-fixture-") as tmp:
        out_dir = Path(tmp) / "out"
        shutil.copytree(FIXTURE_SRC, out_dir)
        run(
            [
                sys.executable,
                str(GENERATE),
                "--out-dir",
                str(out_dir),
                "--target-package",
                "com.example.demo",
                "--module-prefix",
                "objs",
            ]
        )
        run(
            [
                sys.executable,
                str(DUMPER),
                "--root",
                str(out_dir),
                "--config",
                str(out_dir / ".dumper.yml"),
                "--source-repo",
                str(REPO_ROOT),
            ]
        )

        retained = (
            out_dir
            / "objs-core"
            / "src"
            / "main"
            / "kotlin"
            / "com"
            / "example"
            / "demo"
            / "core"
            / "Sample.kt"
        )
        if not retained.is_file():
            print(f"Explicit objs module prefix removed module: {retained}", file=sys.stderr)
            return 1

    with tempfile.TemporaryDirectory(prefix="bom-export-hierarchy-fixture-") as tmp:
        out_dir = Path(tmp) / "out"
        shutil.copytree(FIXTURE_SRC, out_dir)
        run(
            [
                sys.executable,
                str(GENERATE),
                "--out-dir",
                str(out_dir),
                "--target-package",
                "com.example.demo",
                "--module-prefix",
                "demo",
                "--module-hierarchy",
                ":platform:objs",
            ]
        )
        run(
            [
                sys.executable,
                str(DUMPER),
                "--root",
                str(out_dir),
                "--config",
                str(out_dir / ".dumper.yml"),
                "--source-repo",
                str(REPO_ROOT),
            ]
        )

        settings = (out_dir / "settings.gradle.kts").read_text(encoding="utf-8")
        expected_core_path = '":platform:objs:demo-core"'
        if f"include({expected_core_path})" not in settings:
            print("Module hierarchy missing from generated include", file=sys.stderr)
            return 1
        if f"project({expected_core_path}).projectDir = file(\"demo-core\")" not in settings:
            print("Flat module directory mapping missing from generated settings", file=sys.stderr)
            return 1

        service_build = out_dir / "demo-service" / "build.gradle.kts"
        service_text = service_build.read_text(encoding="utf-8")
        if 'project.parent!!.project("demo-core")' not in service_text:
            print("Parent-relative dependency was not renamed", file=sys.stderr)
            return 1
        if "objs-core" in service_text:
            print("Source module name remained in parent-relative dependency", file=sys.stderr)
            return 1

        print("Fixture self-check passed")
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
