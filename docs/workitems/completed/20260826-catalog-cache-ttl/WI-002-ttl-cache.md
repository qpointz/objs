# WI-002 — Hybrid Caffeine TTL catalogs + tests

**Status:** done  
**Examples:** —

## Goal

Replace restart-only catalog caches with write-through + Caffeine `expireAfterWrite` snapshots for
`JpaBoMSchemaCatalog` and `JpaBoMAllowedEdgeCatalog`. Configurable TTL; skip TTL reload mid-TX.

## Acceptance

- [x] `objs.catalogs.cache-ttl` wired via `@ConfigurationProperties`
- [x] Reads rehydrate from DB after TTL when no TX active
- [x] Writes update DB + snapshot and reset TTL
- [x] Rollback still rehydrates
- [x] Tests cover TTL expiry (FakeTicker) and write-through
