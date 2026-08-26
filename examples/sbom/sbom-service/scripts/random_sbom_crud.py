#!/usr/bin/env python3
"""Retired with the legacy `/api/v1/example/sbom` façade.

Use the inventory product API instead:

  ./gradlew :sbom-service:run
  OpenAPI group `inventory` → /api/v1/inventory/**
  UI → http://localhost:8080/sbom/
"""

from __future__ import annotations

import sys


def main() -> None:
    print(
        "random_sbom_crud.py was removed with /api/v1/example/sbom.\n"
        "Use /api/v1/inventory/** (OpenAPI group `inventory`) or the /sbom/ UI.",
        file=sys.stderr,
    )
    raise SystemExit(2)


if __name__ == "__main__":
    main()
