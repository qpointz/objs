import { Alert, SegmentedControl, Stack } from '@mantine/core'
import { useEffect, useRef, useState } from 'react'
import { parse as parseYaml, stringify as stringifyYaml } from 'yaml'
import { SyntaxCodeEditor } from './SyntaxCodeEditor'

export type EditorFormat = 'json' | 'yaml'

type Props = {
  value: unknown
  onChange: (next: unknown) => void
  format: EditorFormat
  onFormatChange: (format: EditorFormat) => void
  onParseError?: (error: string | null) => void
  minHeight?: number
  readOnly?: boolean
  showFormatToggle?: boolean
}

function serialize(value: unknown, format: EditorFormat): string {
  return format === 'json' ? JSON.stringify(value, null, 2) : stringifyYaml(value)
}

function parseText(
  text: string,
  format: EditorFormat,
): { ok: true; value: unknown } | { ok: false; error: string } {
  try {
    const parsed = format === 'json' ? JSON.parse(text) : parseYaml(text)
    return { ok: true, value: parsed }
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) }
  }
}

export function isCompositionDocument(value: unknown): value is {
  objects: unknown[]
  relations?: unknown[]
} {
  return (
    value != null &&
    typeof value === 'object' &&
    !Array.isArray(value) &&
    Array.isArray((value as { objects?: unknown }).objects)
  )
}

export function JsonYamlEditor({
  value,
  onChange,
  format,
  onFormatChange,
  onParseError,
  minHeight = 420,
  readOnly = false,
  showFormatToggle = true,
}: Props) {
  const [text, setText] = useState(() => serialize(value, format))
  const [localError, setLocalError] = useState<string | null>(null)
  const lastExternal = useRef(serialize(value, format))

  useEffect(() => {
    const next = serialize(value, format)
    if (next !== lastExternal.current) {
      lastExternal.current = next
      setText(next)
      setLocalError(null)
      onParseError?.(null)
    }
  }, [value, format, onParseError])

  function commitText(nextText: string) {
    setText(nextText)
    const parsed = parseText(nextText, format)
    if (!parsed.ok) {
      setLocalError(parsed.error)
      onParseError?.(parsed.error)
      return
    }
    if (parsed.value == null || typeof parsed.value !== 'object') {
      const err = 'Document must be a JSON/YAML object'
      setLocalError(err)
      onParseError?.(err)
      return
    }
    setLocalError(null)
    onParseError?.(null)
    lastExternal.current = serialize(parsed.value, format)
    onChange(parsed.value)
  }

  function switchFormat(next: EditorFormat) {
    if (next === format) return
    const parsed = parseText(text, format)
    if (!parsed.ok) {
      setLocalError(`Fix parse errors before switching format: ${parsed.error}`)
      onParseError?.(parsed.error)
      return
    }
    const serialized = serialize(parsed.value, next)
    lastExternal.current = serialized
    setText(serialized)
    setLocalError(null)
    onParseError?.(null)
    onFormatChange(next)
    if (parsed.value != null && typeof parsed.value === 'object') {
      onChange(parsed.value)
    }
  }

  return (
    <Stack gap="xs">
      {showFormatToggle && (
        <SegmentedControl
          size="xs"
          value={format}
          onChange={(v) => switchFormat(v as EditorFormat)}
          data={[
            { value: 'json', label: 'JSON' },
            { value: 'yaml', label: 'YAML' },
          ]}
          w={160}
        />
      )}
      <SyntaxCodeEditor
        value={text}
        onChange={commitText}
        language={format}
        minHeight={minHeight}
        readOnly={readOnly}
      />
      {localError && (
        <Alert color="red" title="Parse error">
          {localError}
        </Alert>
      )}
    </Stack>
  )
}
