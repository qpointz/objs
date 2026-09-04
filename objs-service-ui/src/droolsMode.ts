import { clike } from '@codemirror/legacy-modes/mode/clike'
import type { StreamParser } from '@codemirror/language'

/** Build a keyword lookup map for clike. */
function words(str: string): Record<string, true> {
  const obj: Record<string, true> = {}
  for (const w of str.split(/\s+/)) {
    if (w) obj[w] = true
  }
  return obj
}

/**
 * Basic Drools DRL highlighting (Java-like + DRL keywords).
 * Not a full grammar — enough for playground readability.
 */
export const drools: StreamParser<unknown> = clike({
  name: 'drools',
  // Include `-` so attributes like no-loop / agenda-group tokenize as one word.
  isIdentifierChar: /[\w$_\-]/,
  keywords: words(
    [
      // DRL structure
      'package',
      'import',
      'global',
      'function',
      'rule',
      'end',
      'when',
      'then',
      'query',
      'declare',
      'extends',
      'unit',
      'dialect',
      // Attributes / traits
      'salience',
      'no-loop',
      'agenda-group',
      'activation-group',
      'date-effective',
      'date-expires',
      'enabled',
      'duration',
      'timer',
      'calendars',
      'auto-focus',
      'lock-on-active',
      // LHS CE
      'not',
      'exists',
      'forall',
      'accumulate',
      'acc',
      'collect',
      'from',
      'eval',
      'entry-point',
      'over',
      'in',
      'matches',
      'contains',
      'memberof',
      'soundslike',
      'str',
      'and',
      'or',
      // RHS helpers
      'modify',
      'update',
      'insert',
      'insertLogical',
      'retract',
      'delete',
      // Java-ish (consequence / functions)
      'abstract',
      'assert',
      'break',
      'case',
      'catch',
      'class',
      'const',
      'continue',
      'default',
      'do',
      'else',
      'enum',
      'final',
      'finally',
      'for',
      'if',
      'implements',
      'instanceof',
      'interface',
      'new',
      'private',
      'protected',
      'public',
      'return',
      'static',
      'super',
      'switch',
      'this',
      'throw',
      'throws',
      'try',
      'while',
      'void',
    ].join(' '),
  ),
  types: words(
    'byte short int long float double boolean char Boolean Byte Character Double Float Integer Long Number Object Short String StringBuffer StringBuilder Void List Map Set Collection Optional',
  ),
  blockKeywords: words('rule when then end query declare catch do else finally for if switch try while'),
  atoms: words('true false null'),
  hooks: {
    '@': (stream: { eatWhile: (re: RegExp) => boolean }) => {
      stream.eatWhile(/[\w$_]/)
      return 'meta'
    },
  },
})
