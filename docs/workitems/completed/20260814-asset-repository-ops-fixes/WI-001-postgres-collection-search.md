# WI-001 — PostgreSQL collection search casts

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete

## Goal

`GET` collections with omitted name/owner filters must work on PostgreSQL (`postgres` profile).

## Deliverables

- [x] `CollectionRepository.search` casts `:nameContains` and `:owner` as string before `IS NULL` / `LOWER`
- [x] `CollectionServiceTest.shouldListCollections_whenNameAndOwnerFiltersOmitted`
- [x] `demo` + `postgres` (and `demo-empty`) profiles on the asset-repository service

## Acceptance

- Listing collections with null filters does not raise `function lower(bytea) does not exist`
