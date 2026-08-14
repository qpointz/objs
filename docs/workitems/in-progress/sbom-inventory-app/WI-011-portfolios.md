# WI-011 — Application portfolios (subject-area tree)

**Story:** [`STORY.md`](STORY.md)  
**Journey:** 3 — Portfolio owner (Portfolios tab)  
**Gaps:** G-P10  
**Consumers:** WI-013 MI (R21 application set)

## Goal

Let the **Portfolio owner** create **application portfolios**: an editable taxonomy tree of **subject areas** (folders), each with 0+ applications. A portfolio groups applications and (via those apps) their SBOMs / latest version graphs for MI. Keep v1 **clean and obvious**.

## Locks

- Domain tables only (no objs graph for portfolio structure)  
- Membership = **application** only (**no version**)  
- Application appears **at most once** in a given portfolio (may appear in other portfolios)  
- R21: given portfolio + node/root → distinct application ids (subtree)  
- UI for this WI is under **Portfolios** only (WI-009)

## Deliverables

- [ ] SBOM Flyway/JPA: portfolio, subject-area nodes, membership  
- [ ] `PortfolioService` + domain API (incl. R21 helper or clear query)  
- [ ] Unique-app-per-portfolio enforcement  
- [ ] Tests (subtree vs root application sets)  

## Acceptance

- Portfolio owner can create/edit portfolio tree and place applications without graph vocabulary  
- R21 returns the correct application set for node and root  
