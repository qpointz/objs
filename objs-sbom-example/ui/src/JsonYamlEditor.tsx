import { forwardRef, useEffect, useImperativeHandle, useMemo, useState, type ReactNode } from 'react'
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
  formatDocument: () => void
}

interface JsonYamlEditorProps {
  value: unknown
  rollbackValue?: unknown
  onDraftParsed?: (draft: { valid: boolean; value?: unknown; error?: string }) => void
  /** When set, Rollback delegates here instead of only resetting editor text. */
  onRollback?: () => void
  minHeight?: number
  fillHeight?: boolean
  /** Extra toolbar buttons rendered after Format / Rollback (e.g. Lint). */
  extraActions?: ReactNode
  /** Controlled format; omit to manage format internally. */
  format?: EditorFormat
  onFormatChange?: (format: EditorFormat) => void
  /** Hide the built-in YAML/JSON toggle when the parent owns that control. */
  hideFormatToggle?: boolean
  /** Hide the entire Format / Rollback toolbar (parent renders those actions). */
  hideToolbar?: boolean
}

export const JsonYamlEditor = forwardRef<JsonYamlEditorHandle, JsonYamlEditorProps>(
  function JsonYamlEditor(
    {
      value,
      rollbackValue,
      onDraftParsed,
      onRollback,
      minHeight = 360,
      fillHeight = false,
      extraActions,
      format: formatProp,
      onFormatChange,
      hideFormatToggle = false,
      hideToolbar = false,
    },
    ref,
  ) {
    const [uncontrolledFormat, setUncontrolledFormat] = useState<EditorFormat>('yaml')
    const format = formatProp ?? uncontrolledFormat
    const setFormat = (next: EditorFormat) => {
      if (formatProp === undefined) setUncontrolledFormat(next)
      onFormatChange?.(next)
    }
    const [text, setText] = useState('')
    const [parseError, setParseError] = useState<string | null>(null)

    const jsonText = useMemo(() => JSON.stringify(value, null, 2), [value])
    const yamlText = useMemo(() => stringifyYaml(value), [value])
    const rollbackSource = rollbackValue ?? value

    useEffect(() => {
      setText(format === 'json' ? jsonText : yamlText)
    }, [format, jsonText, yamlText])

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
        formatDocument,
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

    const rollback = () => {
      if (onRollback) {
        onRollback()
        return
      }
      setText(serializeForFormat(rollbackSource, format))
      setParseError(null)
      onDraftParsed?.({ valid: true, value: rollbackSource })
    }

    const showToolbar = !hideToolbar

    return (
      <Stack
        gap="xs"
        style={{
          flex: fillHeight ? 1 : undefined,
          minHeight: fillHeight ? 0 : undefined,
          height: fillHeight ? '100%' : undefined,
        }}
      >
        {showToolbar && (
          <Group
            justify={hideFormatToggle ? 'flex-end' : 'space-between'}
            style={{ flexShrink: 0 }}
          >
            {!hideFormatToggle && (
              <SegmentedControl
                size="xs"
                value={format}
                onChange={(v) => setFormat(v as EditorFormat)}
                data={[
                  { label: 'YAML', value: 'yaml' },
                  { label: 'JSON', value: 'json' },
                ]}
              />
            )}
            <Group gap="xs">
              <Button size="xs" variant="light" onClick={formatDocument}>
                Format
              </Button>
              <Button size="xs" variant="subtle" onClick={rollback}>
                Rollback
              </Button>
              {extraActions}
            </Group>
          </Group>
        )}
        {parseError && (
          <Text size="xs" c="red" style={{ flexShrink: 0 }}>
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
