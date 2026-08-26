#!/usr/bin/env python3
"""Ensure OUT_DIR lies outside the bom-poc source tree."""
from __future__ import annotations

import argparse
import os
import sys


def is_inside_codebase(path: str, source_repo: str) -> bool:
    path_abs = os.path.abspath(path)
    repo_abs = os.path.abspath(source_repo)
    try:
        return os.path.commonpath([path_abs, repo_abs]) == repo_abs
    except ValueError:
        return False


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate OUT_DIR is outside source repo")
    parser.add_argument("--out-dir", required=True)
    parser.add_argument("--source-repo", required=True, help="bom-poc repository root")
    parser.add_argument(
        "--print",
        action="store_true",
        help="Print absolute OUT_DIR path on success",
    )
    args = parser.parse_args(argv)

    out_abs = os.path.abspath(args.out_dir)
    repo_abs = os.path.abspath(args.source_repo)

    if is_inside_codebase(out_abs, repo_abs):
        print(
            f"OUT_DIR must be outside codebase: {out_abs} is under {repo_abs}",
            file=sys.stderr,
        )
        return 1

    if args.print:
        print(out_abs)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
