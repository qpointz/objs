import { json, jsonParseLinter } from '@codemirror/lang-json'
import { yaml } from '@codemirror/lang-yaml'
import { linter, lintGutter } from '@codemirror/lint'
import { vscodeDark, vscodeLight } from '@uiw/codemirror-theme-vscode'
import CodeMirror from '@uiw/react-codemirror'
import { useMantineColorScheme } from '@mantine/core'
import { useMemo } from 'react'

type Props = {
  value: string
  onChange: (value: string) => void
  language: 'json' | 'yaml'
  minHeight?: number
  readOnly?: boolean
}

export function SyntaxCodeEditor({
  value,
  onChange,
  language,
  minHeight = 360,
  readOnly = false,
}: Props) {
  const { colorScheme } = useMantineColorScheme()
  const extensions = useMemo(() => {
    if (language === 'json') {
      return [json(), linter(jsonParseLinter()), lintGutter()]
    }
    return [yaml()]
  }, [language])

  return (
    <div className="syntax-editor">
      <CodeMirror
        value={value}
        height={`${minHeight}px`}
        theme={colorScheme === 'dark' ? vscodeDark : vscodeLight}
        extensions={extensions}
        editable={!readOnly}
        onChange={onChange}
        basicSetup={{
          lineNumbers: true,
          foldGutter: true,
          bracketMatching: true,
          closeBrackets: true,
          highlightSelectionMatches: true,
          autocompletion: false,
        }}
        style={{ fontSize: 13 }}
      />
    </div>
  )
}
