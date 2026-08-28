#!/usr/bin/env python3
"""
YAML-driven export transformer (qpointz dumper pattern).

Operates only on a disposable copy (OUT_DIR). Refuses --root inside the source repo.
Package moves use copy-then-delete (deepest paths first) to avoid fragile walk-order moves.
"""
from __future__ import annotations

import argparse
import os
import shutil
import sys
from pathlib import Path

import yaml

SKIP_DIR_NAMES = frozenset({".git", "build", "node_modules", ".gradle", ".gradle-home"})
SKIP_PATH_MARKERS = tuple(os.path.join(name, "") for name in SKIP_DIR_NAMES)


class Dumper:
    def __init__(self, root_dir: str):
        self.root_dir = os.path.abspath(root_dir)

    def _should_skip(self, path: str) -> bool:
        normalized = path.replace("\\", "/")
        for marker in SKIP_PATH_MARKERS:
            if marker.replace("\\", "/") in normalized:
                return True
        return False

    def move_package(self, *, config=None, src_pkg: str | None = None, dst_pkg: str | None = None):
        if config:
            src_pkg = config["from"]
            dst_pkg = config["to"]
        assert src_pkg and dst_pkg

        old_sp = src_pkg.replace(".", os.sep)
        new_sp = dst_pkg.replace(".", os.sep)
        old_parts = tuple(old_sp.split(os.sep))
        print(f"==== Moving package {src_pkg} to {dst_pkg}")

        matches: list[Path] = []
        for dirpath, dirnames, _files in os.walk(self.root_dir):
            if self._should_skip(dirpath):
                dirnames[:] = []
                continue
            parts = Path(dirpath).parts
            if len(parts) >= len(old_parts) and parts[-len(old_parts) :] == old_parts:
                matches.append(Path(dirpath))

        for old_path in sorted(matches, key=lambda p: len(p.parts), reverse=True):
            new_path = Path(str(old_path).replace(old_sp, new_sp, 1))
            print(f"Copy: {old_path} -> {new_path}")
            if os.path.normcase(os.path.abspath(old_path)) == os.path.normcase(
                os.path.abspath(new_path)
            ):
                print(f"==== Skipping same-path package move: {old_path}")
                continue
            if new_path.exists():
                shutil.rmtree(new_path)
            new_path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copytree(old_path, new_path)
            shutil.rmtree(old_path)

    def move_dir(self, *, config=None, from_path: str | None = None, to_path: str | None = None):
        if config:
            from_path = config["from"]
            to_path = config["to"]
        assert from_path and to_path

        from_sp = os.path.join(self.root_dir, from_path)
        to_sp = os.path.join(self.root_dir, to_path)
        print(f"==== Moving directory {from_sp} to {to_sp}")
        if os.path.normcase(os.path.abspath(from_sp)) == os.path.normcase(os.path.abspath(to_sp)):
            print(f"==== Skipping same-path directory move: {from_sp}")
            return
        if os.path.exists(from_sp):
            parent = os.path.dirname(to_sp)
            if parent:
                os.makedirs(parent, exist_ok=True)
            if os.path.exists(to_sp):
                shutil.rmtree(to_sp)
            shutil.move(from_sp, to_sp)

    def delete_dir(self, *, config=None, dirs: list[str] | None = None):
        if config:
            dirs = config["paths"]
        if not dirs:
            return

        def delete(subdir: str):
            fp = os.path.join(self.root_dir, subdir)
            if os.path.exists(fp):
                print(f"==== Deleting {fp}")
                shutil.rmtree(fp)

        for subdir in dirs:
            delete(subdir)

    def delete_empty_folder(self):
        print("==== Deleting empty folders")
        deleted: set[str] = set()
        for current_dir, subdirs, files in os.walk(self.root_dir, topdown=False):
            if self._should_skip(current_dir):
                continue
            still_has_subdirs = any(
                os.path.join(current_dir, subdir) not in deleted for subdir in subdirs
            )
            if not any(files) and not still_has_subdirs:
                os.rmdir(current_dir)
                print(f"Deleting {current_dir}")
                deleted.add(current_dir)
        return deleted

    def replace_in_files(self, *, config=None, old: str | None = None, new: str | None = None, exts: set | None = None):
        def replace_one(old_text: str, new_text: str, extensions: set):
            print(f"==== Replacing in files {old_text!r} with {new_text!r}, {extensions}")
            for current_dir, subdirs, files in os.walk(self.root_dir):
                if self._should_skip(current_dir):
                    subdirs[:] = []
                    continue
                for filename in files:
                    full_path = os.path.join(current_dir, filename)
                    ext = Path(full_path).suffix
                    if ext not in extensions:
                        continue
                    with open(full_path, "r", encoding="utf-8") as handle:
                        filedata = handle.read()
                    if old_text not in filedata:
                        continue
                    print(f"Replacing in: {full_path}")
                    filedata = filedata.replace(old_text, new_text)
                    with open(full_path, "w", encoding="utf-8") as handle:
                        handle.write(filedata)

        if not config:
            assert old is not None and new is not None and exts is not None
            replace_one(old, new, exts)
            return

        extensions = set(config["exts"])
        for rule in config["replace"]:
            replace_one(rule["old"], rule["new"], extensions)

    def delete_files(self, *, config=None, paths: list[str] | None = None):
        if config:
            paths = config["paths"]
        if not paths:
            return

        for path in paths:
            full_path = os.path.join(self.root_dir, path)
            print(f"==== Deleting {full_path}")
            if os.path.exists(full_path):
                os.remove(full_path)

    def run_action(self, action: dict):
        action_key = next(iter(action))
        config = action[action_key]
        match action_key:
            case "move_package":
                self.move_package(config=config)
            case "delete_dir":
                self.delete_dir(config=config)
            case "delete_file":
                self.delete_files(config=config)
            case "delete_empty_folder":
                self.delete_empty_folder()
            case "replace_in_files":
                self.replace_in_files(config=config)
            case "move_dir":
                self.move_dir(config=config)
            case _:
                raise ValueError(f"Invalid action: {action_key}")


def run_actions(config: dict, *, root_dir: str | None = None):
    dump = config["dump"]
    resolved_root = os.path.abspath(root_dir or dump["in"]["root_dir"])
    dump["in"]["root_dir"] = resolved_root
    dumper = Dumper(resolved_root)
    for action in dump["actions"]:
        dumper.run_action(action)


def _is_under(path: str, parent: str) -> bool:
    path_abs = os.path.abspath(path)
    parent_abs = os.path.abspath(parent)
    try:
        return os.path.commonpath([path_abs, parent_abs]) == parent_abs
    except ValueError:
        return False


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Run export transform actions on OUT_DIR copy")
    parser.add_argument("--root", required=True, help="Absolute path to export tree (OUT_DIR)")
    parser.add_argument("--config", default=".dumper.yml", help="Path to dumper YAML config")
    parser.add_argument(
        "--source-repo",
        default=None,
        help="Refuse --root if it lies inside this directory (bom-poc root)",
    )
    args = parser.parse_args(argv)

    root = os.path.abspath(args.root)
    if args.source_repo and _is_under(root, os.path.abspath(args.source_repo)):
        print(
            f"Refusing to transform inside source repo: {root} is under {args.source_repo}",
            file=sys.stderr,
        )
        return 1

    config_path = args.config
    if not os.path.isabs(config_path):
        config_path = os.path.join(root, config_path)

    with open(config_path, "r", encoding="utf-8") as stream:
        config = yaml.safe_load(stream)

    run_actions(config, root_dir=root)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
