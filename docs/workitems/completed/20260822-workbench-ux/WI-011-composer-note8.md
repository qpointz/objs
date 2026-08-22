# WI-011 — Composer chrome (Note 8)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 11 — Composer Note 8  
**Status:** done  
**Depends on:** WI-010  
**Examples:** **workbench**  
**Source:** [`UX-NOTES/Note8/Note 8.md`](UX-NOTES/Note8/Note%208.md)  
**Gaps:** `G-UX-cgbar`, `G-UX-cnew`, `G-UX-copen` (**resolved**)

## Goal

Ship Note 8 Composer UX: graph bar visual match to shared context chrome; **New ▾** Blank/Matcher; single **Open**; never bind to shared graph context.

## Deliverables

- [x] `ComposerGraphBar` per **G-UX-cgbar** (replaces `CurrentGraphBar`)
- [x] **New ▾** Blank / Matcher per **G-UX-cnew** (`OpenMatcherModal` with `bindSharedContext={false}`)
- [x] **Open** per **G-UX-copen**
- [x] Tour steps updated for Composer

## Out of scope

- Open specific graph version in Composer (deferred)
- WI-005 full living-docs sweep (`ui.md` remains WI-005)
