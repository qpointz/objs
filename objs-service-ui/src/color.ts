const PALETTE = [
  '#228be6',
  '#40c057',
  '#fab005',
  '#fa5252',
  '#7950f2',
  '#15aabf',
  '#e64980',
  '#fd7e14',
  '#12b886',
  '#4c6ef5',
  '#82c91e',
  '#be4bdb',
]

const HEX_COLOR = /^#[0-9a-fA-F]{6}$/

/** Theme-aware mid gray for `attributes.color: nocolor` (readable on light and dark). */
export const DEFAULT_TYPE_COLOR = 'var(--mantine-color-gray-6)'

export function colorFromAttributes(
  attributes?: Record<string, string> | null,
): string | undefined {
  const raw = attributes?.color?.trim()
  if (!raw) return undefined
  if (raw.toLowerCase() === 'nocolor') return DEFAULT_TYPE_COLOR
  if (!HEX_COLOR.test(raw)) return undefined
  return raw.toLowerCase()
}

export function colorForType(
  type: string,
  attributes?: Record<string, string> | null,
): string {
  const fromSchema = colorFromAttributes(attributes)
  if (fromSchema) return fromSchema
  let h = 0
  for (let i = 0; i < type.length; i++) {
    h = (h * 31 + type.charCodeAt(i)) >>> 0
  }
  return PALETTE[h % PALETTE.length]
}

export function nodeLabel(payload: Record<string, unknown> | undefined, id: string): string {
  const name = payload?.name
  if (typeof name === 'string' && name.trim()) {
    return name.length > 28 ? `${name.slice(0, 26)}…` : name
  }
  return id.length > 8 ? `${id.slice(0, 8)}…` : id
}
