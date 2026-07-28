# EFFORT.md — sbom-typed-example dry run

Rough notes after implementing the concrete SBOM app on the foundation.

| Area | Observation |
|------|-------------|
| Toolkit (`…typed`) | Small (~6 types); GraphBuilder + RegistryPack cover most needs |
| Annotation mapping | Straightforward; provenance validation belongs in the app layer |
| CanonicalEdge SCHEMA | Prefer shared edge schema early; bare NONE would have needed a follow-up |
| Wave A | Mechanical once Component path existed; pack registration is the bulk |
| REST façade | Thin over `SbomService`; MockMvc standaloneSetup stays easy |
| Friction | Jackson null fields in payloads; provisional ids for edges; catalog clear in tests vs singleton seed |

**Follow-ups:** Waves B–D; C-3 catalog persistence; prune-on-PUT; importer merge-by-identity.
