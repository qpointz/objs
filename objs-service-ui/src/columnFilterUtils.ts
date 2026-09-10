/** Empty selection = no filter (pass all). Em dash / blank values never match an active filter. */
export function passesColumnFilter(
  value: string | null | undefined,
  selected: ReadonlySet<string>,
): boolean {
  if (selected.size === 0) return true
  if (value == null || value === '' || value === '—') return false
  return selected.has(value)
}

export function uniqueSortedOptions(
  values: Iterable<string | null | undefined>,
): { value: string; label: string }[] {
  const set = new Set<string>()
  for (const v of values) {
    if (v != null && v !== '' && v !== '—') set.add(v)
  }
  return [...set].sort((a, b) => a.localeCompare(b)).map((value) => ({ value, label: value }))
}

export function toggleInSet(prev: ReadonlySet<string>, value: string): Set<string> {
  const next = new Set(prev)
  if (next.has(value)) next.delete(value)
  else next.add(value)
  return next
}
