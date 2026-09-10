/**
 * Title-row (`*-view-actions`) button chrome (Note 5 / G-WU2-N5).
 *
 * - **Primary** (omit `variant` → filled blue): the view’s affirmative CTAs
 *   create / save / run / main handoff.
 * - **Secondary** (`VIEW_ACTION_VARIANT`): everything else on the title row
 *   (export, check/validate, delete, open-as-secondary, reset, …).
 * - Destructive still uses `color="red"` on secondary.
 * - Split buttons: both halves match the main half’s role.
 *
 * Per view primaries:
 * - Explorer — Open in Composer | New graph from selection
 * - Objects — New graph from shelf
 * - Query — Exec ▾
 * - Policy / Suites — Add ▾, Save, Evaluate
 * - Composer — New ▾, Save ▾
 * - Schema — Import, Create ▾, Save, Create schema
 */
export const VIEW_ACTION_BUTTON_SIZE = 'xs' as const

/** Non-primary title-row actions — bordered, not `light` / filled. */
export const VIEW_ACTION_VARIANT = 'default' as const

/** Quiet page title — duplicates L0 nav (Note 3). */
export const VIEW_TITLE_PROPS = {
  order: 4 as const,
  c: 'dimmed' as const,
  fw: 500 as const,
  style: { flexShrink: 0 } as const,
}
