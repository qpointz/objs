export const WORKBENCH_TOUR_STORAGE_KEY = 'objs.ui.workbench.tour.v1'

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
    body: 'This header switches the five product views. Nothing here writes to the store except Composer (and Schema edits).',
    selector: '[data-tour="nav"]',
  },
  {
    id: 'explorer',
    title: 'Explorer',
    body: 'Read-only graph explore. Open one graph, or run a matcher as a Selection. Use Open in Composer / Query to hand off.',
    selector: '[data-tour="nav-explorer"]',
    route: '/explorer',
  },
  {
    id: 'explorer-scope',
    title: 'Graph or Selection',
    body: 'Graph mode shows members of one opened graph. Selection mode is a matcher result set (may span graphs).',
    selector: '[data-tour="explorer-scope"]',
    route: '/explorer',
  },
  {
    id: 'explorer-open',
    title: 'Open a graph',
    body: 'Search graphs (incremental, not a full catalog). After you open one, a Versions list appears on the left if that graph has freezes.',
    selector: '[data-tour="explorer-open-graph"]',
    route: '/explorer',
  },
  {
    id: 'explorer-versions',
    title: 'Versions',
    body: 'Click a version to reconstruct it on the canvas (layout stays put). Latest returns to live HEAD. This pane is hidden when the graph has no versions.',
    selector: '[data-tour="explorer-versions"]',
    route: '/explorer',
  },
  {
    id: 'objects',
    title: 'Objects',
    body: 'Search the entity pool (orphans included). Add hits to the shelf, then New graph from shelf opens Composer with those objects as a draft.',
    selector: '[data-tour="nav-objects"]',
    route: '/objects',
  },
  {
    id: 'composer',
    title: 'Composer',
    body: 'This is the only graph write surface. First Save creates a graph when none is selected. Validate before Save if you want a dry run.',
    selector: '[data-tour="nav-composer"]',
    route: '/composer',
  },
  {
    id: 'composer-version',
    title: 'Create version and Clone',
    body: 'Create version freezes the current graph (same id). Clone makes a new graph with new object ids and an empty history. Both need a saved, clean draft.',
    selector: '[data-tour="composer-version"]',
    route: '/composer',
  },
  {
    id: 'query',
    title: 'Query',
    body: 'Run a Gremlin script or matcher against the current graph (or a handed-off canvas). Exec never writes.',
    selector: '[data-tour="nav-query"]',
    route: '/query',
  },
  {
    id: 'schema',
    title: 'Schema',
    body: 'Browse and edit object and edge schemas. Types here drive validation in Composer.',
    selector: '[data-tour="nav-schema"]',
    route: '/model',
  },
]
