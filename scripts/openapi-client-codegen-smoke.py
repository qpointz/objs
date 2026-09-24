#!/usr/bin/env python3
"""Manual OpenAPI client codegen smoke (C-42 / G-8…G-10).

Fetches springdoc group specs from a running server, generates Java + Python
clients with openapi-generator-cli, and verifies they compile.

Generated sources are written under a temp/gitignored directory — never commit them.

Usage:
  # workbench (port 8081):
  python scripts/openapi-client-codegen-smoke.py --base http://localhost:8081

  # default base is http://localhost:8080
  python scripts/openapi-client-codegen-smoke.py

  # primary groups only (default): graph registry policy
  # readiness for SBOM/AR:
  python scripts/openapi-client-codegen-smoke.py --base http://localhost:8080 \\
      --groups inventory asset-repository --readiness-only

Requires: Java 17+, python3, curl/urllib, Maven (for Java compile).
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.request
from pathlib import Path

GENERATOR_VERSION = "7.25.0"
JAR_NAME = f"openapi-generator-cli-{GENERATOR_VERSION}.jar"
JAR_URL = (
    "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/"
    f"{GENERATOR_VERSION}/{JAR_NAME}"
)

PRIMARY_GROUPS = ("graph", "registry", "policy")
READINESS_GROUPS = ("inventory", "asset-repository")


def log(msg: str) -> None:
    print(msg, flush=True)


def run(cmd: list[str], *, cwd: Path | None = None) -> None:
    log(f"+ {' '.join(cmd)}")
    subprocess.run(cmd, cwd=cwd, check=True)


def ensure_jar(cache_dir: Path) -> Path:
    cache_dir.mkdir(parents=True, exist_ok=True)
    jar = cache_dir / JAR_NAME
    if jar.is_file():
        return jar
    log(f"Downloading {JAR_URL}")
    urllib.request.urlretrieve(JAR_URL, jar)
    return jar


def fetch_spec(base: str, group: str, dest: Path) -> None:
    url = f"{base.rstrip('/')}/v3/api-docs/{group}"
    log(f"GET {url}")
    with urllib.request.urlopen(url, timeout=60) as resp:
        dest.write_bytes(resp.read())


def generate(jar: Path, spec: Path, lang: str, out: Path) -> None:
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    run(
        [
            "java",
            "-jar",
            str(jar),
            "generate",
            "-g",
            lang,
            "-i",
            str(spec),
            "-o",
            str(out),
            "--skip-validate-spec",
        ]
    )


def compile_java(out: Path) -> None:
    pom = out / "pom.xml"
    if not pom.is_file():
        raise SystemExit(f"Java generate missing pom.xml under {out}")
    mvn = shutil.which("mvn") or shutil.which("mvn.cmd")
    if not mvn:
        raise SystemExit("Maven (mvn) required to compile generated Java client")
    run([mvn, "-q", "-DskipTests", "compile"], cwd=out)


def compile_python(out: Path) -> None:
    # Syntax/bytecode compile only — no pip install of generated package.
    run([sys.executable, "-m", "compileall", "-q", str(out)])


def smoke_group(
    jar: Path,
    work: Path,
    base: str,
    group: str,
    *,
    compile_clients: bool,
) -> None:
    spec = work / f"{group}.json"
    fetch_spec(base, group, spec)
    java_out = work / group / "java"
    py_out = work / group / "python"
    generate(jar, spec, "java", java_out)
    generate(jar, spec, "python", py_out)
    if compile_clients:
        compile_java(java_out)
        compile_python(py_out)
        log(f"OK {group}: generate + compile")
    else:
        log(f"OK {group}: generate (readiness-only, skip compile)")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--base",
        default=os.environ.get("OBJS_OPENAPI_BASE", "http://localhost:8080"),
        help="Server base URL (default http://localhost:8080; workbench often :8081)",
    )
    parser.add_argument(
        "--groups",
        nargs="+",
        default=list(PRIMARY_GROUPS),
        help=f"springdoc groups (default: {' '.join(PRIMARY_GROUPS)})",
    )
    parser.add_argument(
        "--readiness-only",
        action="store_true",
        help="Generate only; skip Java/Python compile (for SBOM/AR readiness)",
    )
    parser.add_argument(
        "--keep",
        type=Path,
        help="Keep generated tree at this path instead of a temp dir",
    )
    args = parser.parse_args()

    repo = Path(__file__).resolve().parents[1]
    cache = repo / ".tmp" / "openapi-generator"
    jar = ensure_jar(cache)

    if args.keep:
        work = args.keep
        work.mkdir(parents=True, exist_ok=True)
        cleanup = False
    else:
        work = Path(tempfile.mkdtemp(prefix="objs-openapi-codegen-"))
        cleanup = True

    log(f"Work dir: {work}")
    try:
        for group in args.groups:
            smoke_group(
                jar,
                work,
                args.base,
                group,
                compile_clients=not args.readiness_only,
            )
        log("All groups OK")
        return 0
    finally:
        if cleanup:
            shutil.rmtree(work, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
