/** Default Gremlin script for Query (graph-scoped Exec seeds from context). */
export const DEFAULT_QUERY_SCRIPT = 'g.V()'

/**
 * Bumped when the shipped default changed (old key held the Service/Policy demo script).
 * Unversioned `objs.ui.query.script` is ignored.
 */
export const QUERY_SCRIPT_STORAGE_KEY = 'objs.ui.query.script.v2'
