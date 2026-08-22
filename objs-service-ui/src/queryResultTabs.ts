/** Query result pane tabs (display order). */
export type QueryResultTab = 'graph' | 'structured' | 'raw'

export type QueryResultTabPresence = {
  hasGraph: boolean
  hasStructured: boolean
  hasRaw: boolean
}

const TAB_ORDER: QueryResultTab[] = ['graph', 'structured', 'raw']

export function queryResultTabHasContent(
  tab: string | null,
  presence: QueryResultTabPresence,
): boolean {
  if (tab === 'graph') return presence.hasGraph
  if (tab === 'structured') return presence.hasStructured
  if (tab === 'raw') return presence.hasRaw
  return false
}

/**
 * After Exec: keep the current tab when it has content; otherwise pick the first
 * non-empty tab in Graph → Structured → Raw order.
 */
export function resolveQueryResultTab(
  current: string | null,
  presence: QueryResultTabPresence,
): QueryResultTab {
  if (
    (current === 'graph' || current === 'structured' || current === 'raw') &&
    queryResultTabHasContent(current, presence)
  ) {
    return current
  }
  for (const tab of TAB_ORDER) {
    if (queryResultTabHasContent(tab, presence)) return tab
  }
  return current === 'structured' || current === 'raw' ? current : 'graph'
}
