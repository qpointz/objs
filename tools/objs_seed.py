#!/usr/bin/env python3
"""Objs catalog seed nano-framework (stdlib only).

Paste this single file into an export project. Define ontology seeds as Python
objects; emit multi-document YAML for ``POST /api/v1/objs/registry/import?format=seeds``.

Design docs (in the objs repo):
  - docs/design/graph/json-schema-to-seeds.md  (how-to)
  - docs/design/graph/seeds.md                (seed format)

v1 supports ObjectSchema + AllowedEdgeRule only (no Graph).
No Excel I/O — map source data in your project, then build Catalog entries here.

Example::

    from objs_seed import Catalog, ObjectSchema, string, EdgeRule, EDGE_PROPERTIES

    cat = Catalog()
    cat.add(ObjectSchema(
        "Component", "1.0.0",
        title="Component",
        description="Software component",
        fields=[
            string("name", title="Name", description="Display name",
                   identifier=True, searchable=True),
        ],
    ))
    cat.write("ontology.seeds.yaml")
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Iterable, List, Optional, Sequence, Tuple, Union

SEED_API_VERSION = "objs.poc.org/v1"
ENTITY = "ENTITY"
EDGE_PROPERTIES = "EDGE_PROPERTIES"

_SCALAR_TYPES = frozenset({"STRING", "NUMBER", "INTEGER", "BOOLEAN", "ENUM"})


# ---------------------------------------------------------------------------
# Schema nodes / fields
# ---------------------------------------------------------------------------


@dataclass
class SchemaNode:
    type: str
    title: str
    description: str
    fields: Optional[List["Field"]] = None
    items: Optional["SchemaNode"] = None
    values: Optional[List[Tuple[str, str]]] = None  # (value, description)
    format: Optional[str] = None
    default: Any = None

    def to_dict(self) -> dict:
        t = self.type.upper()
        if not self.title or not str(self.title).strip():
            raise ValueError(f"SchemaNode type={t} requires nonblank title")
        if not self.description or not str(self.description).strip():
            raise ValueError(f"SchemaNode type={t} requires nonblank description")
        d: dict = {
            "type": t,
            "title": self.title,
            "description": self.description,
        }
        if t == "OBJECT":
            if self.fields is None:
                raise ValueError("OBJECT node requires fields (use [] if empty)")
            d["fields"] = [f.to_dict() for f in self.fields]
        elif t == "ARRAY":
            if self.items is None:
                raise ValueError("ARRAY node requires items")
            d["items"] = self.items.to_dict()
        elif t == "ENUM":
            if not self.values:
                raise ValueError("ENUM node requires nonempty values")
            d["values"] = [
                {"value": v, "description": desc} for v, desc in self.values
            ]
        elif t == "STRING" and self.format:
            d["format"] = self.format
        if self.default is not None:
            d["default"] = self.default
        return d


@dataclass
class Field:
    name: str
    schema: SchemaNode
    required: bool = True
    identifier: bool = False
    searchable: bool = False
    stereotype: Optional[Sequence[str]] = None

    def to_dict(self) -> dict:
        if not self.name or not str(self.name).strip():
            raise ValueError("Field requires nonblank name")
        if self.identifier or self.searchable:
            if self.schema.type.upper() not in _SCALAR_TYPES:
                raise ValueError(
                    f"identifier/searchable only allowed on scalar fields "
                    f"(got {self.schema.type} for '{self.name}')"
                )
        d: dict = {
            "name": self.name,
            "schema": self.schema.to_dict(),
            "required": bool(self.required),
        }
        if self.identifier:
            d["identifier"] = True
        if self.searchable:
            d["searchable"] = True
        if self.stereotype:
            d["stereotype"] = list(self.stereotype)
        return d


# ---------------------------------------------------------------------------
# Field helpers
# ---------------------------------------------------------------------------


def _scalar_field(
    name: str,
    node_type: str,
    *,
    title: str,
    description: str,
    required: bool = True,
    identifier: bool = False,
    searchable: bool = False,
    format: Optional[str] = None,
    default: Any = None,
    stereotype: Optional[Sequence[str]] = None,
) -> Field:
    return Field(
        name=name,
        schema=SchemaNode(
            type=node_type,
            title=title,
            description=description,
            format=format,
            default=default,
        ),
        required=required,
        identifier=identifier,
        searchable=searchable,
        stereotype=stereotype,
    )


def string(
    name: str,
    *,
    title: str,
    description: str,
    required: bool = True,
    identifier: bool = False,
    searchable: bool = False,
    format: Optional[str] = None,
    default: Any = None,
    stereotype: Optional[Sequence[str]] = None,
) -> Field:
    return _scalar_field(
        name,
        "STRING",
        title=title,
        description=description,
        required=required,
        identifier=identifier,
        searchable=searchable,
        format=format,
        default=default,
        stereotype=stereotype,
    )


def integer(
    name: str,
    *,
    title: str,
    description: str,
    required: bool = True,
    identifier: bool = False,
    searchable: bool = False,
    default: Any = None,
    stereotype: Optional[Sequence[str]] = None,
) -> Field:
    return _scalar_field(
        name,
        "INTEGER",
        title=title,
        description=description,
        required=required,
        identifier=identifier,
        searchable=searchable,
        default=default,
        stereotype=stereotype,
    )


def number(
    name: str,
    *,
    title: str,
    description: str,
    required: bool = True,
    identifier: bool = False,
    searchable: bool = False,
    default: Any = None,
    stereotype: Optional[Sequence[str]] = None,
) -> Field:
    return _scalar_field(
        name,
        "NUMBER",
        title=title,
        description=description,
        required=required,
        identifier=identifier,
        searchable=searchable,
        default=default,
        stereotype=stereotype,
    )


def boolean(
    name: str,
    *,
    title: str,
    description: str,
    required: bool = True,
    identifier: bool = False,
    searchable: bool = False,
    default: Any = None,
    stereotype: Optional[Sequence[str]] = None,
) -> Field:
    return _scalar_field(
        name,
        "BOOLEAN",
        title=title,
        description=description,
        required=required,
        identifier=identifier,
        searchable=searchable,
        default=default,
        stereotype=stereotype,
    )


def enum(
    name: str,
    values: Sequence[Union[str, Tuple[str, str]]],
    *,
    title: str,
    description: str,
    required: bool = True,
    identifier: bool = False,
    searchable: bool = False,
    default: Any = None,
    stereotype: Optional[Sequence[str]] = None,
) -> Field:
    parsed: List[Tuple[str, str]] = []
    for item in values:
        if isinstance(item, tuple):
            parsed.append((item[0], item[1]))
        else:
            parsed.append((item, item))
    return Field(
        name=name,
        schema=SchemaNode(
            type="ENUM",
            title=title,
            description=description,
            values=parsed,
            default=default,
        ),
        required=required,
        identifier=identifier,
        searchable=searchable,
        stereotype=stereotype,
    )


def obj(
    name: str,
    fields: Sequence[Field],
    *,
    title: str,
    description: str,
    required: bool = True,
    stereotype: Optional[Sequence[str]] = None,
) -> Field:
    """Nested OBJECT field. Do not pass identifier/searchable here — mark nested scalars."""
    return Field(
        name=name,
        schema=SchemaNode(
            type="OBJECT",
            title=title,
            description=description,
            fields=list(fields),
        ),
        required=required,
        stereotype=stereotype,
    )


def array(
    name: str,
    items: SchemaNode,
    *,
    title: str,
    description: str,
    required: bool = True,
    stereotype: Optional[Sequence[str]] = None,
) -> Field:
    """ARRAY field. Do not pass identifier/searchable here — forbidden under ARRAY paths."""
    return Field(
        name=name,
        schema=SchemaNode(
            type="ARRAY",
            title=title,
            description=description,
            items=items,
        ),
        required=required,
        stereotype=stereotype,
    )


def string_node(
    *,
    title: str,
    description: str,
    format: Optional[str] = None,
    default: Any = None,
) -> SchemaNode:
    """Leaf STRING node for ARRAY items."""
    return SchemaNode(
        type="STRING",
        title=title,
        description=description,
        format=format,
        default=default,
    )


# ---------------------------------------------------------------------------
# Catalog documents
# ---------------------------------------------------------------------------


@dataclass
class ObjectSchema:
    type: str
    version: str
    title: str
    description: str
    fields: Sequence[Field] = field(default_factory=list)
    usage: str = ENTITY

    def to_document(self) -> dict:
        if not self.type or not str(self.type).strip():
            raise ValueError("ObjectSchema requires type")
        if not self.version or not str(self.version).strip():
            raise ValueError("ObjectSchema requires version")
        usage = (self.usage or ENTITY).upper()
        if usage not in (ENTITY, EDGE_PROPERTIES):
            raise ValueError(f"usage must be ENTITY or EDGE_PROPERTIES, got {usage}")
        doc: dict = {
            "apiVersion": SEED_API_VERSION,
            "kind": "ObjectSchema",
            "type": self.type,
            "version": str(self.version),
        }
        if usage != ENTITY:
            doc["usage"] = usage
        doc["contentSchema"] = SchemaNode(
            type="OBJECT",
            title=self.title,
            description=self.description,
            fields=list(self.fields),
        ).to_dict()
        return doc


@dataclass
class EdgeRule:
    """AllowedEdgeRule — directed allow-list triple (not a payload field)."""

    source_type: str
    role: str
    target_type: str
    properties_policy: str = "NONE"
    empty_properties_allowed: bool = True
    properties_schema: Optional[Tuple[str, str]] = None  # (type, version)
    cardinality: str = "UNSPECIFIED"

    def to_document(self) -> dict:
        for label, val in (
            ("sourceType", self.source_type),
            ("role", self.role),
            ("targetType", self.target_type),
        ):
            if not val or not str(val).strip():
                raise ValueError(f"EdgeRule requires {label}")
        policy = (self.properties_policy or "NONE").upper()
        if policy not in ("NONE", "SCHEMA"):
            raise ValueError(f"properties_policy must be NONE or SCHEMA, got {policy}")
        card = (self.cardinality or "UNSPECIFIED").strip()
        if card not in ("UNSPECIFIED", "1:1", "1:*"):
            raise ValueError(f"cardinality must be UNSPECIFIED|1:1|1:*, got {card}")
        doc: dict = {
            "apiVersion": SEED_API_VERSION,
            "kind": "AllowedEdgeRule",
            "sourceType": self.source_type,
            "role": self.role,
            "targetType": self.target_type,
            "propertiesPolicy": policy,
            "emptyPropertiesAllowed": bool(self.empty_properties_allowed),
            "cardinality": card,
        }
        if policy == "SCHEMA":
            if not self.properties_schema or len(self.properties_schema) != 2:
                raise ValueError(
                    "SCHEMA properties_policy requires properties_schema=(type, version)"
                )
            doc["propertiesSchemaType"] = self.properties_schema[0]
            doc["propertiesSchemaVersion"] = str(self.properties_schema[1])
        return doc


class Catalog:
    """Ordered collection of ObjectSchema and EdgeRule documents."""

    def __init__(self) -> None:
        self._docs: List[Any] = []

    def add(self, item: Union[ObjectSchema, EdgeRule]) -> "Catalog":
        self._docs.append(item)
        return self

    def extend(self, items: Iterable[Union[ObjectSchema, EdgeRule]]) -> "Catalog":
        for item in items:
            self.add(item)
        return self

    def documents(self) -> List[dict]:
        return [d.to_document() for d in self._docs]

    def dumps(self) -> str:
        docs = self.documents()
        if not docs:
            return ""
        parts = [_emit_yaml(doc) for doc in docs]
        return "---\n" + "\n---\n".join(parts)

    def write(self, path: str, *, encoding: str = "utf-8") -> None:
        text = self.dumps()
        if not text.endswith("\n"):
            text += "\n"
        with open(path, "w", encoding=encoding, newline="\n") as fh:
            fh.write(text)


# ---------------------------------------------------------------------------
# Minimal YAML emitter (subset needed for seeds)
# ---------------------------------------------------------------------------


def _needs_quotes(s: str) -> bool:
    if s == "" or s.strip() != s:
        return True
    if s in ("true", "false", "null", "True", "False", "None", "~"):
        return True
    if s[0] in "-?:,[]{}#&*!|>'\"%@`":
        return True
    if any(c in s for c in ":#[]{},\n\t"):
        return True
    # quote numeric-looking strings (incl. versions like 1.0.0) for YAML stability
    bare = s[1:] if s.startswith("-") else s
    if bare.replace(".", "").isdigit():
        return True
    return False


def _quote(s: str) -> str:
    escaped = s.replace("\\", "\\\\").replace('"', '\\"')
    return f'"{escaped}"'


def _emit_scalar(value: Any) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, int) and not isinstance(value, bool):
        return str(value)
    if isinstance(value, float):
        return repr(value)
    s = str(value)
    return _quote(s) if _needs_quotes(s) else s


def _emit_yaml(data: Any, indent: int = 0) -> str:
    sp = "  " * indent
    if isinstance(data, dict):
        if not data:
            return "{}"
        lines: List[str] = []
        for key, val in data.items():
            k = _quote(str(key)) if _needs_quotes(str(key)) else str(key)
            if isinstance(val, dict):
                if not val:
                    lines.append(f"{sp}{k}: {{}}")
                else:
                    lines.append(f"{sp}{k}:")
                    lines.append(_emit_yaml(val, indent + 1))
            elif isinstance(val, list):
                if not val:
                    lines.append(f"{sp}{k}: []")
                else:
                    lines.append(f"{sp}{k}:")
                    lines.append(_emit_yaml(val, indent + 1))
            else:
                lines.append(f"{sp}{k}: {_emit_scalar(val)}")
        return "\n".join(lines)
    if isinstance(data, list):
        if not data:
            return f"{sp}[]"
        lines = []
        for item in data:
            if isinstance(item, (dict, list)):
                nested = _emit_yaml(item, indent + 1)
                nested_lines = nested.split("\n")
                first = nested_lines[0].lstrip()
                lines.append(f"{sp}- {first}")
                for more in nested_lines[1:]:
                    lines.append(more)
            else:
                lines.append(f"{sp}- {_emit_scalar(item)}")
        return "\n".join(lines)
    return f"{sp}{_emit_scalar(data)}"
