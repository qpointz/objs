import { forwardRef, useEffect, useImperativeHandle, useMemo, useState } from 'react'
import { useDebouncedValue } from '@mantine/hooks'
import { Button, Group, SegmentedControl, Stack, Text } from '@mantine/core'
import { parse as parseYaml, stringify as stringifyYaml } from 'yaml'
import { SyntaxCodeEditor } from './SyntaxCodeEditor'
import type { EditorFormat } from './schemaDsl'

function normalizeParsedForWire(parsed: unknown, sourceFormat: EditorFormat): unknown {
  if (sourceFormat !== 'yaml') return parsed
  return JSON.parse(JSON.stringify(parsed))
}

function serializeForFormat(value: unknown, format: EditorFormat): string {
  return format === 'json' ? JSON.stringify(value, null, 2) : stringifyYaml(value)
}

export type JsonYamlEditorHandle = {
  getParsedForSubmit: () => { ok: true; value: unknown } | { ok: false; error: string }
}

interface JsonYamlEditorProps {
  value: unknown
  rollbackValue?: unknown
  onDraftParsed?: (draft: { valid: boolean; value?: unknown; error?: string }) => void
  minHeight?: number
  fillHeight?: boolean
}

export const JsonYamlEditor = forwardRef<JsonYamlEditorHandle, JsonYamlEditorProps>(
  function JsonYamlEditor({ value, rollbackValue, onDraftParsed, minHeight = 360, fillHeight = false }, ref) {
    const [format, setFormat] = useState<EditorFormat>('yaml')
    const [text, setText] = useState('')
    const [parseError, setParseError] = useState<string | null>(null)

    const jsonText = useMemo(() => JSON.stringify(value, null, 2), [value])
    const yamlText = useMemo(() => stringifyYaml(value), [value])
    const rollbackSource = rollbackValue ?? value

    useEffect(() => {
      setText(format === 'json' ? jsonText : yamlText)
    }, [format, jsonText, yamlText])

    useImperativeHandle(
      ref,
      () => ({
        getParsedForSubmit: () => {
          try {
            const parsed = format === 'json' ? JSON.parse(text) : parseYaml(text)
            const wire = normalizeParsedForWire(parsed, format)
            return { ok: true, value: wire }
          } catch (e) {
            return {
              ok: false,
              error: e instanceof Error ? e.message : 'Invalid document',
            }
          }
        },
      }),
      [format, text],
    )

    const [debouncedText] = useDebouncedValue(text, 120)

    useEffect(() => {
      if (!onDraftParsed) return
      try {
        const parsed = format === 'json' ? JSON.parse(debouncedText) : parseYaml(debouncedText)
        const wire = normalizeParsedForWire(parsed, format)
        setParseError(null)
        onDraftParsed({ valid: true, value: wire })
      } catch (e) {
        const error = e instanceof Error ? e.message : 'Invalid document'
        setParseError(error)
        onDraftParsed({ valid: false, error })
      }
    }, [format, onDraftParsed, debouncedText])

    const formatDocument = () => {
      try {
        if (format === 'json') {
          setText(JSON.stringify(JSON.parse(text), null, 2))
        } else {
          setText(stringifyYaml(parseYaml(text), { lineWidth: 100 }))
        }
      } catch {
        // keep parseError from live validation
      }
    }

    const rollback = () => {
      setText(serializeForFormat(rollbackSource, format))
      setParseError(null)
      onDraftParsed?.({ valid: true, value: rollbackSource })
    }

    return (
      <Stack gap="xs" style={{ flex: fillHeight ? 1 : undefined, minHeight: fillHeight ? minHeight : undefined }}>
        <Group justify="space-between">
          <SegmentedControl
            size="xs"
            value={format}
            onChange={(v) => setFormat(v as EditorFormat)}
            data={[
              { label: 'YAML', value: 'yaml' },
              { label: 'JSON', value: 'json' },
            ]}
          />
          <Group gap="xs">
            <Button size="xs" variant="light" onClick={formatDocument}>
              Format
            </Button>
            <Button size="xs" variant="subtle" onClick={rollback}>
              Rollback
            </Button>
          </Group>
        </Group>
        {parseError && (
          <Text size="xs" c="red">
            {parseError}
          </Text>
        )}
        <SyntaxCodeEditor
          value={text}
          onChange={setText}
          language={format}
          minHeight={minHeight}
          fillHeight={fillHeight}
        />
      </Stack>
    )
  },
)
