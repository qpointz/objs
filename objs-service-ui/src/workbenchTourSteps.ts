export const WORKBENCH_TOUR_STORAGE_KEY = 'objs.ui.workbench.tour.v2'

export type WorkbenchTourStep = {
  id: string
  title: string
  body: string
  /** `document.querySelector` target. Missing targets are skipped. */
  selector?: string
  /** Navigate here before highlighting. */
  route?: string
}

export const WORKBENCH_TOUR_STEPS: WorkbenchTourStep[] = [
  {
    id: 'nav',
    title: 'Workbench',
    body: 'L0 nav: Explorer · Objects · Query · Policy · Composer · Schema. Explorer, Objects, Query, and Policy share one graph context. Composer and Schema stay separate.',
    selector: '[data-tour="nav"]',
  },
  {
    id: 'explorer',
    title: 'Explorer',
    body: 'Read-only canvas for the shared graph context. Header holds GraphContextBar with L0 nav. Page row: quiet title + right-aligned view actions. Canvas: filter toolbar (top-left) + Apply layout / Fit to view (top-right) + inspect.',
    selector: '[data-tour="nav-explorer"]',
    route: '/explorer',
  },
  {
    id: 'graph-context',
    title: 'Graph context',
    body: 'Shared across Explorer, Objects, Query, and Policy — lives on the AppShell header next to L0 nav. Graph mode: id, Annotations: N (hover), version pin, N/E; Matcher shows the expression; All shows every graph.',
    selector: '[data-tour="graph-context"]',
    route: '/explorer',
  },
  {
    id: 'graph-context-open',
    title: 'Open context',
    body: 'Open ▾ picks Graph (search dialog), Matcher (modal), or All (`{ all: true }`). Stats show N/E for the current context.',
    selector: '[data-tour="graph-context-open"]',
    route: '/explorer',
  },
  {
    id: 'graph-context-version',
    title: 'Graph version',
    body: 'Graph mode only: pin Latest or a saved version. The pin follows you on Explorer, Objects, and Query. From/to filters and paging live in the menu.',
    selector: '[data-tour="graph-context-version"]',
    route: '/explorer',
  },
  {
    id: 'explorer-view-actions',
    title: 'Explorer actions',
    body: 'Right-aligned on the title row: Analyze cycles (when present), Open in Composer / New graph from selection. Apply layout and Fit to view live on the canvas (top-right toolbar + right-click). Filters are on the top-left canvas toolbar.',
    selector: '[data-tour="explorer-view-actions"]',
    route: '/explorer',
  },
  {
    id: 'object-inspect',
    title: 'Object details',
    body: 'Select a node or edge (or empty canvas in graph mode for the graph header). Sectioned viewer: Node / Payload / Annotations / Versions. Drag the splitter to resize the inspect pane.',
    selector: '[data-tour="object-inspect"]',
    route: '/explorer',
  },
  {
    id: 'objects',
    title: 'Objects',
    body: 'Lists entities from the shared graph context (chained obj-expr). Grid matches Query Data chrome; click Id to inspect in the right pane.',
    selector: '[data-tour="nav-objects"]',
    route: '/objects',
  },
  {
    id: 'objects-actions',
    title: 'Objects actions',
    body: 'Right-aligned on the title row: stats (left within the actions strip), shelf actions (Add/Remove, Clear, New graph from shelf). Shared context is in the header.',
    selector: '[data-tour="objects-view-actions"]',
    route: '/objects',
  },
  {
    id: 'objects-side',
    title: 'Shelf and Matcher',
    body: 'When nothing is inspected, the right pane shows Shelf (bold + count when non-empty) and Matcher. Search lives in the Matcher tab only.',
    selector: '[data-tour="objects-side"]',
    route: '/objects',
  },
  {
    id: 'query',
    title: 'Query',
    body: 'Gremlin script against the shared graph context. Script editor on top (Ctrl/Cmd+Enter to Exec); Visual / Data / Raw results below. No Matcher tab — context comes from the bar.',
    selector: '[data-tour="nav-query"]',
    route: '/query',
  },
  {
    id: 'query-actions',
    title: 'Query actions',
    body: 'Right-aligned on the title row: Exec stats, Open in Composer, Exec ▾ (Options in the split menu). Shared context is in the header.',
    selector: '[data-tour="query-view-actions"]',
    route: '/query',
  },
  {
    id: 'query-options',
    title: 'Query options',
    body: 'Open from Exec ▾ → Options. Timeout (traversalOptions.timeoutSeconds) is in the Options modal. Matcher and a right Options pane were removed — scope is script + shared context only.',
    selector: '[data-tour="query-options"]',
    route: '/query',
  },
  {
    id: 'policy',
    title: 'Policy playground',
    body: 'Policy playground: left Policies | Suites mode tabs, full-height editor, Visual/Data, and Policy|Evaluations|Object (Suites: Selection|Evaluation|Object) under the graph — Object opens on selection; Evaluations filters findings to the selection. Shared graph context; soft-fails if the policy service module is absent.',
    selector: '[data-tour="nav-policy"]',
    route: '/policy',
  },
  {
    id: 'composer',
    title: 'Composer',
    body: 'Write surface. Quiet title + right-aligned view actions (including New ▾). ComposerGraphBar lives in the AppShell header (draft-local, not shared graph context). Visual L2: Changes only left of right-aligned draft actions.',
    selector: '[data-tour="nav-composer"]',
    route: '/composer',
  },
  {
    id: 'composer-graph-bar',
    title: 'Composer graph bar',
    body: 'In the header on Composer: Open loads an existing graph into the draft only. New ▾ (Blank | Matcher) lives on view actions. Read-only Latest when a graph id is set.',
    selector: '[data-tour="composer-graph-bar"]',
    route: '/composer',
  },
  {
    id: 'composer-version',
    title: 'Create version and Clone',
    body: 'On the title-row actions: Create version freezes the current graph (same id; optional backdated created-at). Clone deep-copies to a new id. Both require a saved, clean draft.',
    selector: '[data-tour="composer-version"]',
    route: '/composer',
  },
  {
    id: 'composer-lifecycle',
    title: 'Graph lifecycle',
    body: 'Graph ▾: Clear contents, versions (purge / travel back / apply membership), purge all, soft Delete, or Destroy. Confirms before destructive ops.',
    selector: '[data-tour="composer-lifecycle"]',
    route: '/composer',
  },
  {
    id: 'schema',
    title: 'Schema',
    body: 'Global catalog — not graph-scoped. No context bar. Row 1: quiet title + right-aligned view actions. Then resizable type list + catalog or type editor.',
    selector: '[data-tour="nav-schema"]',
    route: '/model',
  },
  {
    id: 'schema-view-actions',
    body: 'Catalog: Export, Import, Create ▾. Apply layout / Fit to view are on the catalog Visual canvas (top-right toolbar + right-click). Detail: Create version, Save, Delete ▾, Rollback when dirty. Always: Create ▾ Object | Edge.',
    title: 'Schema actions',
    selector: '[data-tour="schema-view-actions"]',
    route: '/model',
  },
]
