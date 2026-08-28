# WI-001 — Dumper script

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** G-5, G-6, G-11  
**Status:** completed

## Goal

Add [`scripts/export/dumper.py`](../../../scripts/export/dumper.py) adapted from qpointz `dumper.py`.

## Tasks

- [x] Copy action runner: `move_package`, `move_dir`, `replace_in_files`, `delete_dir`, `delete_empty_folder`
- [x] UTF-8 read/write in `replace_in_files`
- [x] Skip `build/`, `node_modules/`, `.git/`, `.gradle/` during walks
- [x] CLI `--root` overrides yaml `root_dir`; refuse `--root` inside bom-poc source tree
- [x] Small fixture tree + unit test or script self-check for package move + replace

## Acceptance

- `python scripts/export/dumper.py --root /tmp/fixture --config /tmp/fixture/.dumper.yml` transforms fixture without touching repo root
- G-6 mitigations documented in script header or README stub
