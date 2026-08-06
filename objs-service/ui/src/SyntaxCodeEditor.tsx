import { useMemo } from 'react'
import { Box, useMantineColorScheme } from '@mantine/core'
import CodeMirror from '@uiw/react-codemirror'
import { json, jsonParseLinter } from '@codemirror/lang-json'
import { yaml } from '@codemirror/lang-yaml'
import { StreamLanguage } from '@codemirror/language'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { linter, lintGutter } from '@codemirror/lint'

export interface SyntaxCodeEditorProps {
  value: string
  onChange?: (value: string) => void
  language: 'json' | 'yaml' | 'groovy'
  minHeight?: number
  fillHeight?: boolean
  readOnly?: boolean
}

const basicSetup = {
  lineNumbers: true,
  foldGutter: true,
  dropCursor: false,
  allowMultipleSelections: false,
  indentOnInput: true,
  bracketMatching: true,
  closeBrackets: true,
  autocompletion: false,
  highlightSelectionMatches: true,
} as const

export function SyntaxCodeEditor({
  value,
  onChange,
  language,
  minHeight = 360,
  fillHeight = false,
  readOnly = false,
}: SyntaxCodeEditorProps) {
  const { colorScheme } = useMantineColorScheme()

  const extensions = useMemo(() => {
    const lang =
      language === 'json'
        ? json()
        : language === 'yaml'
          ? yaml()
          : StreamLanguage.define(groovy)
    if (readOnly || language !== 'json') {
      return [lang]
    }
    return [lang, linter(jsonParseLinter()), lintGutter()]
  }, [language, readOnly])

  const minH = `${minHeight}px`
  const editorHeight = fillHeight ? '100%' : minH

  return (
    <Box
      style={{
        border: '1px solid var(--mantine-color-default-border)',
        borderRadius: 'var(--mantine-radius-sm)',
        overflow: 'hidden',
        flex: fillHeight ? 1 : undefined,
        minHeight: fillHeight ? minHeight : undefined,
        display: fillHeight ? 'flex' : undefined,
        flexDirection: fillHeight ? 'column' : undefined,
      }}
    >
      <CodeMirror
        value={value}
        height={editorHeight}
        theme={colorScheme === 'dark' ? 'dark' : 'light'}
        extensions={extensions}
        onChange={readOnly ? undefined : onChange}
        editable={!readOnly}
        readOnly={readOnly}
        basicSetup={basicSetup}
        style={{ fontSize: 13, flex: fillHeight ? 1 : undefined, minHeight: fillHeight ? minHeight : undefined }}
      />
    </Box>
  )
}
