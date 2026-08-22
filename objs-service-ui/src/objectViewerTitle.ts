/** Display title for object viewer (Note 5): name, else `typename-shortId`. */
export function objectDisplayTitle(
  displayName: string | null | undefined,
  type: string,
  id: string,
): string {
  const name = displayName?.trim()
  if (name) return name
  const short = id.replace(/-/g, '').slice(0, 5)
  return `${type.toLowerCase()}-${short}`
}

export function formatInstanceVersionLabel(headVersion: number | null | undefined): string {
  return headVersion == null ? 'LATEST' : String(headVersion)
}

export const OBJECT_VERSION_PREVIEW_N = 5
export const OBJECT_VERSION_BROWSER_DEFAULT = 10
