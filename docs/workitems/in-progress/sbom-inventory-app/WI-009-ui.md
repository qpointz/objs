# WI-009 — Non-technical UI (J1–J3)

**Story:** [`STORY.md`](STORY.md)  

## Goal

Simple UI with a **visual** split between Application owner and Portfolio owner. No auth/roles. No graph concepts in copy or chrome.

**UX lock — clean and obvious:**

- Two global tabs only at the top level: **Applications** | **Portfolios** (labels a non-technical user understands immediately).  
- One primary job per view; avoid dashboards, card clutter, and secondary panels that compete with the main task.  
- On Portfolios: keep **taxonomy** and **Reports** as clear modes/steps — do not crowd both into one busy canvas.  
- MI is a **short linear flow**: pick portfolio → pick level → pick report → **Run** → results. No buried menus.  
- Actions use plain product words (application, version, asset, portfolio, report). Never graph/entity/edge/matcher.  
- Empty states say what to do next in one short sentence.  

## Chrome (normative)

| Tab | Persona | Content |
|-----|---------|---------|
| **Applications** | Application owner | Journey 1 (apps, drafts, versions, CDX) + Journey 2 (assets) |
| **Portfolios** | Portfolio owner | Journey 3 — portfolio taxonomy + **MI only here** |

## Deliverables

- [ ] Applications tab: search / edit draft / version / CDX export  
- [ ] Applications tab: assets search / detail (usage, duplicates, owner)  
- [ ] Portfolios tab: portfolio tree UI (subject areas + place apps)  
- [ ] Portfolios tab: MI flow — **portfolio → level → report → Run → results** (MI-1…MI-4)  
- [ ] Dynamic schema search form (**searchable fields only**)  
- [ ] No Reports entry under Applications  

## Acceptance

- Walkthrough of Journeys 1–3 without foundation vocabulary  
- MI unreachable from Applications chrome without switching to Portfolios  
- First-time layout is obvious: tabs + linear MI steps; no scavenger-hunt navigation  
