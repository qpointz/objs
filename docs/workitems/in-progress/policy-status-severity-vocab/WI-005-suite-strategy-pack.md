# WI-005 — SuiteStrategy pack SPI

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  

## Done

- [x] `SuiteStrategy` SPI: execution select + leaf reported severity + folder roll-up + overall
- [x] `BuiltinSuiteStrategy` preserves matrices + bare `EXEC_ERROR`→`HIGH`
- [x] `DefaultSuiteEvaluator` orchestrates via pack only (no hard-coded leaf/overall formulas)
- [x] `suiteStrategyKind` on suite (+ `resolvedSuiteStrategyKind` legacy `rollUpStrategyKind` fallback)
- [x] Registry `SuiteStrategies.suite(kind)`
