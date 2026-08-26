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

        print("Fixture self-check passed")
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
