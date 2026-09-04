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
    body: 'Read-only canvas for the shared graph context. Row 1: title + graph-context bar. Row 2: type pills (dim non-matching nodes) and Open in Composer / Apply layout.',
    selector: '[data-tour="nav-explorer"]',
    route: '/explorer',
  },
  {
    id: 'graph-context',
    title: 'Graph context',
    body: 'Shared across Explorer, Objects, and Query. Graph mode shows id + annotations + version pin; Matcher mode shows the expression; All shows every graph.',
    selector: '[data-tour="graph-context"]',
    route: '/explorer',
  },
  {
    id: 'graph-context-open',
    title: 'Open context',
    body: 'Open ▾ picks Graph (search dialog), Matcher (modal), or All (`{ all: true }`). Stats on the right show nodes and edges in the current context.',
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
    body: 'Type pills filter the canvas; clear with ×. When the algorithm service is present: Analyze cycles highlights directed SCC regions (violet). Graph mode: Open in Composer. Matcher/All: New graph from selection. Apply layout ▾ when the canvas has nodes. Graph view disables above ~300 nodes.',
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
    body: 'Stats on the left; shelf actions on the right: Add/Remove selected, Clear shelf, New graph from shelf (disabled when shelf is empty).',
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
    body: 'Exec stats on the left; Open in Composer (when the last result has graph contents under the node cap), Exec, and Options (timeout cog) on the right.',
    selector: '[data-tour="query-view-actions"]',
    route: '/query',
  },
  {
    id: 'query-options',
    title: 'Query options',
    body: 'Timeout (traversalOptions.timeoutSeconds) lives in the Options popover. Matcher and a right Options pane were removed — scope is script + shared context only.',
    selector: '[data-tour="query-options"]',
    route: '/query',
  },
  {
    id: 'policy',
    title: 'Policy playground',
    body: 'Replaceable DROOLS playground: policy list + editor, shared graph context, Check/Evaluate, severity pills, Object/Tasks inspect, and bottom Policy/Evaluations tasks. Soft-fails if the policy service module is absent.',
    selector: '[data-tour="nav-policy"]',
    route: '/policy',
  },
  {
    id: 'composer',
    title: 'Composer',
    body: 'Write surface. ComposerGraphBar matches shared context chrome visually but never binds to it — draft graph id is local to Composer.',
    selector: '[data-tour="nav-composer"]',
    route: '/composer',
  },
  {
    id: 'composer-graph-bar',
    title: 'Composer graph bar',
    body: 'New ▾ Blank or Matcher seeds the draft; Open loads an existing graph into the draft only. Read-only Latest when a graph id is set.',
    selector: '[data-tour="composer-graph-bar"]',
    route: '/composer',
  },
  {
    id: 'composer-version',
    title: 'Create version and Clone',
    body: 'On the actions row below: Create version freezes the current graph (same id). Clone deep-copies to a new id. Both require a saved, clean draft.',
    selector: '[data-tour="composer-version"]',
    route: '/composer',
  },
  {
    id: 'schema',
    title: 'Schema',
    body: 'Global catalog — not graph-scoped. Row 1: title + SchemaContextBar. Row 2: view actions. Row 3: resizable type list + catalog or type editor.',
    selector: '[data-tour="nav-schema"]',
    route: '/model',
  },
  {
    id: 'schema-context-bar',
    title: 'Schema context',
    body: 'Catalog stats, or open type with Version ▾ (same chrome as graph version pin), kind pill, and tags. Unsaved shows as dimmed hint.',
    selector: '[data-tour="schema-context-bar"]',
    route: '/model',
  },
  {
    id: 'schema-context-version',
    title: 'Schema version',
    body: 'On a type detail: Version ▾ lists registered versions (latest / current badges). Create-version drafts show as (draft) and stay pinned until Save.',
    selector: '[data-tour="schema-context-version"]',
    route: '/model',
  },
  {
    id: 'schema-view-actions',
    body: 'Catalog: Apply layout, Export, Import, Create ▾. Detail: Create version, Save, Delete ▾, Rollback when dirty. Always: Create ▾ Object | Edge.',
    title: 'Schema actions',
    selector: '[data-tour="schema-view-actions"]',
    route: '/model',
  },
]
