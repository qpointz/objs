#!/usr/bin/env python3
"""Runnable example for tools/objs_seed.py — entity schemas + edge rule variants.

Demonstrates catalog metadata from catalog-schema-metadata (C-16):
  - ObjectSchema envelope ``tags`` / ``attributes`` (incl. ``color``)
  - Field-level ``tags`` / ``attributes``
  - Enum optional ``caption``
  - AllowedEdgeRule description / verbs / tags / attributes

Usage:
  python tools/objs_seed_example.py
  python tools/objs_seed_example.py --out ontology.seeds.yaml
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

# Allow running as ``python tools/objs_seed_example.py`` from repo root.
sys.path.insert(0, str(Path(__file__).resolve().parent))

from objs_seed import (  # noqa: E402
    EDGE_PROPERTIES,
    Catalog,
    EdgeRule,
    ObjectSchema,
    enum,
    string,
    string_node,
    array,
)


def build_catalog() -> Catalog:
    cat = Catalog()

    # --- ENTITY schemas ---
    cat.add(
        ObjectSchema(
            "Product",
            "1.0.0",
            title="Product",
            description="Product payload",
            tags=["catalog", "product"],
            attributes={"color": "#228be6"},
            fields=[
                string(
                    "name",
                    title="Name",
                    description="Product display name",
                    required=True,
                    identifier=True,
                    searchable=True,
                    tags=["identity"],
                    attributes={"uiGroup": "general"},
                ),
                string(
                    "version",
                    title="Version",
                    description="Product version string",
                    required=False,
                    tags=["versioning"],
                ),
            ],
        )
    )

    cat.add(
        ObjectSchema(
            "Component",
            "1.0.0",
            title="Component",
            description="Software component",
            tags=["sbom", "core"],
            attributes={"color": "#4c6ef5"},
            fields=[
                string(
                    "name",
                    title="Name",
                    description="Component display name",
                    required=True,
                    identifier=True,
                    searchable=True,
                    tags=["identity"],
                    attributes={"uiGroup": "general", "priority": "high"},
                ),
                enum(
                    "severity",
                    [
                        ("LOW", "Limited impact", "Low"),
                        ("HIGH", "Serious impact", "High"),
                    ],
                    title="Severity",
                    description="Optional severity label",
                    required=False,
                    tags=["classification"],
                    attributes={"uiGroup": "risk"},
                ),
                array(
                    "tags",
                    string_node(title="Tag", description="One tag"),
                    title="Tags",
                    description="Search labels",
                    required=False,
                    stereotype=["tags"],
                    tags=["search"],
                ),
            ],
        )
    )

    # Type name with a space (quoting exercised by emitter)
    cat.add(
        ObjectSchema(
            "Container Image",
            "1.0.0",
            title="Container Image",
            description="Container image artifact",
            tags=["artifact"],
            attributes={"color": "nocolor"},
            fields=[
                string(
                    "name",
                    title="Name",
                    description="Image name",
                    required=True,
                    identifier=True,
                ),
            ],
        )
    )

    # --- EDGE_PROPERTIES schema (shared edge property bag) ---
    cat.add(
        ObjectSchema(
            "CanonicalEdge",
            "1.0.0",
            usage=EDGE_PROPERTIES,
            title="Canonical edge",
            description="Shared edge property bag",
            tags=["edge-meta"],
            fields=[
                string(
                    "createdAt",
                    title="Created at",
                    description="Edge creation timestamp",
                    required=False,
                    format="date-time",
                ),
                string(
                    "source",
                    title="Source",
                    description="Provenance of the edge",
                    required=False,
                    searchable=True,
                    tags=["provenance"],
                ),
            ],
        )
    )

    # --- Edge rules ---
    # 1) Bare relation (no properties)
    cat.add(
        EdgeRule(
            "Product",
            "OWNS",
            "Component",
            properties_policy="NONE",
            cardinality="1:*",
            description="Product owns the component",
            source_verb="owns",
            target_verb="owned by",
            tags=["ownership"],
        )
    )

    # 2) Schema-governed relation with cardinality 1:*
    cat.add(
        EdgeRule(
            "Product",
            "CONTAINS",
            "Component",
            properties_policy="SCHEMA",
            properties_schema=("CanonicalEdge", "1.0.0"),
            empty_properties_allowed=True,
            cardinality="1:*",
            description="Product includes the component in its bill",
            source_verb="contains",
            target_verb="contained in",
            tags=["composition"],
            attributes={"weight": "primary"},
        )
    )

    # 3) 1:1 cardinality + SCHEMA
    cat.add(
        EdgeRule(
            "Component",
            "PRIMARY_IMAGE",
            "Container Image",
            properties_policy="SCHEMA",
            properties_schema=("CanonicalEdge", "1.0.0"),
            empty_properties_allowed=True,
            cardinality="1:1",
            description="Primary container image for the component",
            source_verb="has primary image",
            target_verb="is primary image of",
        )
    )

    # 4) Wildcard target (metadata allow-list pattern)
    cat.add(
        EdgeRule(
            "Product",
            "RELATED_TO",
            "*",
            properties_policy="NONE",
            cardinality="UNSPECIFIED",
            tags=["loose"],
        )
    )

    return cat


def main() -> int:
    parser = argparse.ArgumentParser(description="Emit sample objs catalog seeds YAML")
    parser.add_argument(
        "--out",
        metavar="PATH",
        help="Write YAML to PATH instead of stdout",
    )
    args = parser.parse_args()
    yaml_text = build_catalog().dumps()
    if not yaml_text.endswith("\n"):
        yaml_text += "\n"
    if args.out:
        Path(args.out).write_text(yaml_text, encoding="utf-8", newline="\n")
        print(f"Wrote {args.out}", file=sys.stderr)
    else:
        sys.stdout.write(yaml_text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
