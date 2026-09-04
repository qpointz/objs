import { forwardRef, useCallback, useImperativeHandle, useMemo, useRef } from 'react'
import { Box, useMantineColorScheme } from '@mantine/core'
import CodeMirror, { type ReactCodeMirrorRef } from '@uiw/react-codemirror'
import { json, jsonParseLinter } from '@codemirror/lang-json'
import { yaml } from '@codemirror/lang-yaml'
import { StreamLanguage } from '@codemirror/language'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { linter, lintGutter } from '@codemirror/lint'
import { drools } from './droolsMode'
import { search, searchKeymap, openSearchPanel } from '@codemirror/search'
import { EditorSelection, Prec } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'

export interface SyntaxCodeEditorProps {
  value: string
  onChange?: (value: string) => void
  language: 'json' | 'yaml' | 'groovy' | 'drools'
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
  /** Scroll to 1-based line (and optional 1-based column). Returns false if out of range. */
  revealLine: (line: number, column?: number) => boolean
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
      revealLine: (line: number, column?: number) => {
        const view = cmRef.current?.view
        if (!view || line < 1) return false
        const doc = view.state.doc
        if (line > doc.lines) return false
        const lineObj = doc.line(line)
        const col = column != null && column > 0 ? Math.min(column - 1, lineObj.length) : 0
        const pos = lineObj.from + col
        view.dispatch({
          selection: EditorSelection.cursor(pos),
          effects: EditorView.scrollIntoView(pos, { y: 'center' }),
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
            : language === 'drools'
              ? StreamLanguage.define(drools)
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

    const handleChange = useCallback(
      (next: string) => {
        onChange?.(next)
      },
      [onChange],
    )

    const minH = `${minHeight}px`
    const editorHeight = fillHeight ? '100%' : minH

    const fillTheme = useMemo(
      () =>
        fillHeight
          ? EditorView.theme({
              '&': { height: '100%', maxHeight: '100%' },
              '.cm-scroller': { overflow: 'auto' },
            })
          : null,
      [fillHeight],
    )

    const allExtensions = useMemo(
      () => (fillTheme ? [...extensions, fillTheme] : extensions),
      [extensions, fillTheme],
    )

    return (
      <Box
        style={{
          border: '1px solid var(--mantine-color-default-border)',
          borderRadius: 'var(--mantine-radius-sm)',
          overflow: 'hidden',
          flex: fillHeight ? 1 : undefined,
          // height:0 + flex:1 forces a definite box so CM scrolls instead of growing.
          height: fillHeight ? 0 : undefined,
          minHeight: fillHeight ? minHeight : undefined,
          maxHeight: fillHeight ? '100%' : undefined,
          display: fillHeight ? 'flex' : undefined,
          flexDirection: fillHeight ? 'column' : undefined,
          alignSelf: fillHeight ? 'stretch' : undefined,
        }}
      >
        <CodeMirror
          ref={cmRef}
          value={value}
          height={editorHeight}
          theme={colorScheme === 'dark' ? 'dark' : 'light'}
          extensions={allExtensions}
          onChange={readOnly ? undefined : handleChange}
          editable={!readOnly}
          readOnly={readOnly}
          basicSetup={basicSetup}
          style={{
            fontSize: 13,
            height: fillHeight ? '100%' : undefined,
            maxHeight: fillHeight ? '100%' : undefined,
            flex: fillHeight ? 1 : undefined,
            minHeight: fillHeight ? 0 : undefined,
            overflow: fillHeight ? 'hidden' : undefined,
          }}
        />
      </Box>
    )
  },
)
