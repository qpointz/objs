# WI-004 — Java generator scaffolding and typed bindings

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Generator scaffolding
**Status:** completed
**Depends on:** WI-003

## Goal

Add the reusable root `:objs-codegen-java` artifact and its non-behavioral typed binding foundation.
Its Gradle tooling reads the consuming application's exported schema and relation manifest after
`jsonschema2pojo`, then emits Java bindings against the stabilized `objs-api` into that consuming
application's generated source set. Mutation behavior and read navigation are separate later gates.

## Scope

- Add `:objs-codegen-java` to the root build and expose a reusable Java artifact/task contract
- Keep the generator implementation in the root foundation; examples are consumers and fixtures
- Provide repository-local project dependency wiring so examples can use the generator immediately
  after checkout
- Provide a CLI/task contract: application-owned schema input, application-owned output directory,
  and target package
- Parse both `$defs`/2020-12 and `definitions`/draft-07
- Read the root `x-objs-relations` manifest without requiring non-standard keywords inside
  dialect-native schema definitions
- Validate definition, relation, method, and package-name collisions
- Apply schema-level and relation-level `codegen.java.*` overrides before symbol validation
- Use the existing normalized names when overrides are absent; reject blank, invalid Java identifiers,
  reserved keywords, and collisions after overrides
- Apply schema-level `codegen.baseClass` and `codegen.interfaces` to generated Java entity/payload
  classes without hand-editing jsonschema2pojo output
- For wildcard relations, apply relation-level `codegen.baseClass` to the wildcard endpoint and
  generate a binding using that common type
- Omit a static binding when a wildcard endpoint has no base class, while retaining the relation in
  the generated runtime metadata
- Validate one superclass, normalize and de-duplicate interface names in declared order, and fail
  clearly for malformed or inaccessible inheritance metadata
- Resolve source/target and edge-property schema references
- Generate entity type bindings and typed references
- Generate a common node identity/payload handle plus independent read-navigation and write-
  mutation capability interfaces; permit an optional application-owned combined facade
- Report unresolved or non-`EDGE_PROPERTIES` edge-property references through explicit diagnostics
  and use a generic property representation instead of blocking generation
- Register generated output as a Gradle source directory
- Enforce task order: `generateJsonSchema2Pojo` → `generateObjsApi` → compilation
- Attach generation to the normal example compilation lifecycle so no manual generation command is
  required
- Reject output paths under any root `objs-*` module and keep all generated classes application-owned
- Make generation incremental and deterministic
- Keep Kotlin generation out of this artifact; a future `:objs-codegen-kotlin` is a separate design

## Stop / review gate

Do not start WI-005 until generated type/reference/capability scaffolding compiles against the
renamed API and all output ownership, naming, inheritance, wildcard, and diagnostics behavior has
been reviewed. This WI must not introduce mutation or persistence behavior.

## Out of scope

- Spring or HTTP client generation
- Runtime persistence or validation
- Automatic conversion of linked POJOs
- Strongly typed wildcard relation methods

## Implementation evidence

- Added the reusable root `:objs-codegen-java` Kotlin/JVM library with a public
  `JavaCodeGenerator` API and `JavaCodegenMain` CLI entry point.
- The generator consumes `$defs` or `definitions`, `x-objs-codegen`, and `x-objs-relations`;
  validates Java/package symbols, relation method collisions, definition references, inheritance
  metadata, wildcard static-binding flags, and application-owned output paths.
- Generated sources include a common typed node handle, independent capability markers, typed
  identity references, entity metadata factories, and deterministic relation metadata. Mutation
  behavior and read navigation remain out of scope for this stage.
- Generator tests cover deterministic output, inheritance/diagnostic preservation, malformed input,
  root-module output rejection, and Java compilation against `objs-api`.

## Acceptance

- [x] Generator fails clearly for malformed manifests and reports unresolved references without
  blocking later object-model construction
- [x] `:objs-codegen-java` can be consumed by an application outside the repository
- [x] A clean full checkout builds the example without publishing or manually invoking generation
- [x] Repeated generation produces stable source
- [x] Generated Java compiles against `:objs-api` and jsonschema2pojo DTOs
- [x] Generated sources are emitted only into the consuming application
- [x] No generated application class is added to a root `objs-*` module
- [x] `ProductType`-style entity bindings and typed references are generated
- [x] Schema and relation metadata can override generated Java names without changing source schemas
- [x] Missing overrides retain the current generated names
- [x] Generated entity/payload classes can inherit the configured base class and implement the
  configured interfaces
- [x] Wildcard relations generate only when their wildcard endpoint has a configured base class
- [x] Wildcard relations without a base class remain available through runtime metadata without
  producing an unsafe typed binding
- [x] Generated capability scaffolding compiles without persistence or Spring types
- [x] Draft-07 and 2020-12 inputs are supported
- [x] Generator consumes the codegen-only relation metadata while standard export documents remain
  unaffected
- [x] No Spring type appears in generated source
- [x] The artifact emits Java only and contains no application ontology or generated application
  classes
- [x] Generated output is application-owned and never written under a root `objs-*` module
- [x] The scaffolding checkpoint is explicitly accepted before WI-005 begins

