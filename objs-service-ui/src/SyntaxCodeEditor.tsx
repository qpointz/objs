import { forwardRef, useImperativeHandle, useMemo, useRef } from 'react'
import { Box, useMantineColorScheme } from '@mantine/core'
import CodeMirror, { type ReactCodeMirrorRef } from '@uiw/react-codemirror'
import { json, jsonParseLinter } from '@codemirror/lang-json'
import { yaml } from '@codemirror/lang-yaml'
import { StreamLanguage } from '@codemirror/language'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { linter, lintGutter } from '@codemirror/lint'
import { search, searchKeymap, openSearchPanel } from '@codemirror/search'
import { EditorSelection, Prec } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'

export interface SyntaxCodeEditorProps {
  value: string
  onChange?: (value: string) => void
  language: 'json' | 'yaml' | 'groovy'
  minHeight?: number
  fillHeight?: boolean
  readOnly?: boolean
  /** Ctrl/Cmd+Enter (e.g. Query Exec). */
  onModEnter?: () => void
}

export type SyntaxCodeEditorHandle = {
  /** Open the find panel (same as Ctrl/Cmd+F). */
  openSearch: () => void
  /**
   * Scroll to and select the first occurrence of `query`.
   * Returns false if not found.
   */
  revealText: (query: string) => boolean
  /** Current document text (editor is source of truth for Exec). */
  getValue: () => string
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
  searchKeymap: true,
} as const

export const SyntaxCodeEditor = forwardRef<SyntaxCodeEditorHandle, SyntaxCodeEditorProps>(
  function SyntaxCodeEditor(
    {
      value,
      onChange,
      language,
      minHeight = 360,
      fillHeight = false,
      readOnly = false,
      onModEnter,
    },
    ref,
  ) {
    const { colorScheme } = useMantineColorScheme()
    const cmRef = useRef<ReactCodeMirrorRef>(null)

    useImperativeHandle(ref, () => ({
      openSearch: () => {
        const view = cmRef.current?.view
        if (view) openSearchPanel(view)
      },
      revealText: (query: string) => {
        const view = cmRef.current?.view
        if (!view || !query) return false
        const doc = view.state.doc.toString()
        const idx = doc.indexOf(query)
        if (idx < 0) return false
        const anchor = idx
        const head = idx + query.length
        view.dispatch({
          selection: EditorSelection.range(anchor, head),
          effects: EditorView.scrollIntoView(anchor, { y: 'center' }),
        })
        view.focus()
        return true
      },
      getValue: () => cmRef.current?.view?.state.doc.toString() ?? value,
    }))

    const extensions = useMemo(() => {
      const lang =
        language === 'json'
          ? json()
          : language === 'yaml'
            ? yaml()
            : StreamLanguage.define(groovy)
      const base = [lang, search({ top: true }), keymap.of(searchKeymap)]
      const withModEnter =
        onModEnter != null && !readOnly
          ? [
              ...base,
              Prec.highest(
                keymap.of([
                  {
                    key: 'Mod-Enter',
                    run: () => {
                      onModEnter()
                      return true
                    },
                  },
                ]),
              ),
            ]
          : base
      if (readOnly || language !== 'json') {
        return withModEnter
      }
      return [...withModEnter, linter(jsonParseLinter()), lintGutter()]
    }, [language, readOnly, onModEnter])

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
          ref={cmRef}
          value={value}
          height={editorHeight}
          theme={colorScheme === 'dark' ? 'dark' : 'light'}
          extensions={extensions}
          onChange={readOnly ? undefined : onChange}
          editable={!readOnly}
          readOnly={readOnly}
          basicSetup={basicSetup}
          style={{
            fontSize: 13,
            flex: fillHeight ? 1 : undefined,
            minHeight: fillHeight ? minHeight : undefined,
          }}
        />
      </Box>
    )
  },
)
