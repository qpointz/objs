# Consumers — codegen-aggregate-materializer (C-44)

**Normative** after WI-001. Proof in **WI-003**.

## Today (gap)

| Consumer | Today | After this story |
|----------|--------|------------------|
| `:examples/codegen/jsonschema` | Nested `Product.setContainsComponent(List)` may exist on POJO; tests use **explicit** `mutations.containsComponent(a,b)` | Smoke: `materialize(productWithNests)` → mutation with 2+ entities + edge(s) |
| `:examples/codegen/jsonschema-draft07` | Same | Dialect parity if WI-001 locks both |
| SBOM / AR | Domain services build mutations / compositions by hand | **Optional** one call-site only if WI-001 locks |

## Not consumers

| Surface | Why |
|---------|-----|
| Workbench Composer | Uses store REST / draft model; not nested POJO aggregates |
| Silent Jackson serialize of object graphs as entity payload | Explicitly out (G-M10) |

Exact type/method names locked in WI-001 → living docs in WI-004.
