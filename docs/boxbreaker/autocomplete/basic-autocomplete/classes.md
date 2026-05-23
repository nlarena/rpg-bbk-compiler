# Basic Autocomplete — Classes

**Status:** design / not implemented
**Scope:** every class to create and every file to modify for Block A (basic autocomplete + live templates)
**Companion:** [`theory.md`](theory.md)

---

## 1. New classes

All under `plugin-bbk/src/main/java/com/larena/boxbreaker/plugin/bbk/`.

### 1.1 Lexer config

| Class | Responsibility |
|---|---|
| `lexer/BbkWordsScanner.java` | Wraps `BbkLexerAdapter` into a `DefaultWordsScanner`, classifying which token types count as identifiers, keywords, literals, comments. Without this, hyphenated keywords like `DCL-S` are seen as two words and word-aware features (find usages by text, extend selection, default word completion) break. |

### 1.2 Completion entry point

| Class | Responsibility |
|---|---|
| `completion/BbkCompletionContributor.java` | Single entry point. In its constructor calls `extend(...)` once per `(context pattern, provider)` pair, wiring each provider to the syntactic positions where it should fire. Holds no completion logic itself — it's a registration table. |
| `completion/BbkCompletionPatterns.java` | Static helpers that build the `PlatformPatterns` used by the contributor. One method per logical context (`afterDclS()`, `insideUsageArgs()`, `procedureBody()`, etc.). Centralizing patterns avoids duplication and makes them testable. |

### 1.3 Prefix matcher

| Class | Responsibility |
|---|---|
| `completion/matcher/BbkHyphenAwarePrefixMatcher.java` | Custom `PrefixMatcher` that (a) is case-insensitive (BBK is case-insensitive), and (b) treats `-` as part of a keyword token. Wraps the `result` inside providers so that typing `dcl` matches `DCL-S`, `DCL-PR`, etc. |

### 1.4 Completion providers (one per syntactic context)

All extend `CompletionProvider<CompletionParameters>` and live under `completion/providers/`.

| Class | What it suggests | Context |
|---|---|---|
| `BbkTopLevelKeywordProvider.java` | `DCL-S`, `DCL-C`, `DCL-DS`, `DCL-F`, `DCL-PR`, `DCL-PROC`, `CTL-OPT` | Start of a top-level item |
| `BbkTypeProvider.java` | `INT`, `UNS`, `CHAR`, `VARCHAR`, `PACKED`, `ZONED`, `BINDEC`, `FLOAT`, `DATE`, `TIME`, `TIMESTAMP`, `BOOL`, `POINTER`, `VOID`, plus `LIKE`, `LIKEDS`, `LIKEREC` | After an IDENT inside a declaration that expects a type |
| `BbkVarModifierProvider.java` | `INZ`, `DIM`, `BASED`, `STATIC`, `EXPORT`, `IMPORT`, `OVERLAY`, `POS`, `QUALIFIED`, `TEMPLATE`, `ALIGN`, `OPTIONS` | After a complete type spec inside `DCL-S` |
| `BbkDsModifierProvider.java` | `QUALIFIED`, `TEMPLATE`, `EXTNAME`, `LIKEDS`, `LIKEREC`, `ALIGN`, `INZ`, `BASED`, `DIM` | After IDENT in `DCL-DS` and before `{` |
| `BbkParamModifierProvider.java` | `VALUE`, `CONST`, `OPDESC`, `OPTIONS` | After a type inside an inline param list |
| `BbkFileKeywordProvider.java` | `USAGE`, `KEYED`, `DISK`, `PRINTER`, `WORKSTN`, `SEQ`, `EXTNAME`, `EXTFILE`, `USROPN`, `PREFIX`, `RENAME`, `INFDS`, `INDDS` | After IDENT in `DCL-F` |
| `BbkProcModifierProvider.java` | `EXPORT`, `EXTPROC` | After the signature in `DCL-PROC` and before `{` |
| `BbkPrModifierProvider.java` | `EXTPGM`, `EXTPROC`, `OPDESC`, `RTNPARM` | After the signature in `DCL-PR` and before `;` |
| `BbkStatementKeywordProvider.java` | `if`, `else`, `while`, `do`, `for`, `select`, `when`, `other`, `return`, `break`, `continue`, `monitor`, `on-error`, `on-exit`, `exsr`, `leavesr`, `begsr`, `endsr` | Statement position inside a procedure body |
| `BbkFileOpProvider.java` | `read`, `reade`, `readp`, `readpe`, `chain`, `write`, `update`, `delete`, `setll`, `setgt`, `open`, `close`, `exfmt`, `unlock`, `callp` | Statement position inside a procedure body |
| `BbkDirectiveProvider.java` | `PRE-IF`, `PRE-ELSEIF`, `PRE-ELSE`, `PRE-ENDIF`, `PRE-DEFINE`, `PRE-UNDEFINE`, `PRE-INCLUDE`, `PRE-EOF` | Start of a top-level or block item |
| `BbkStarIdentProvider.java` | `*INPUT`, `*OUTPUT`, `*UPDATE`, `*DELETE` (USAGE), `*NOPASS`, `*OMIT`, `*VARSIZE`, `*STRING`, `*NULLIND` (OPTIONS), `*NO`, `*YES` (CTL-OPT args) | Inside `USAGE(`, `OPTIONS(`, `CTL-OPT` arg positions |

### 1.5 Live template contexts

| Class | Responsibility |
|---|---|
| `templates/BbkLiveTemplateContext.java` | Base context: returns `true` for any element in a BBK file. Parent of the two more specific contexts below. |
| `templates/BbkTopLevelTemplateContext.java` | Restricts templates to module-level positions. Used for `dcls`, `dclc`, `dclds`, `dclf`, `dclpr`, `proc`, `ctlopt`. |
| `templates/BbkProcedureBodyTemplateContext.java` | Restricts templates to procedure-body positions. Used for `ifb`, `whileb`, `forb`, `selectb`, `monitorb`, `dowb`, `preif`. |

### 1.6 Icons helper

| Class | Responsibility |
|---|---|
| `icons/BbkIcons.java` | Static fields with cached `Icon` instances (`KEYWORD`, `TYPE`, `MODIFIER`, `DIRECTIVE`, `FILE_OP`). Loaded once via `IconLoader.getIcon(...)`. Used by `LookupElementBuilder.withIcon(...)` in every provider to give visual differentiation in the popup. If reusing the existing `bbk.svg` for all categories is acceptable, this class can be a single field for now and expanded later. |

---

## 2. New resources

| File | Responsibility |
|---|---|
| `src/main/resources/liveTemplates/Bbk.xml` | Declares every live template (abbreviation, expansion, placeholders, context). Examples: `dcls` → `DCL-S $NAME$ $TYPE$;`, `proc` → `DCL-PROC $NAME$ {\n  $END$\n}`, `ifb` → `if ($COND$) {\n  $END$\n}`, `preif` → `PRE-IF $COND$\n  $END$\nPRE-ENDIF`. |
| `src/main/resources/icons/keyword.svg` *(optional)* | Distinct icon for keyword suggestions. Skip if reusing `bbk.svg`. |
| `src/main/resources/icons/type.svg` *(optional)* | Distinct icon for type suggestions. Skip if reusing `bbk.svg`. |
| `src/main/resources/icons/modifier.svg` *(optional)* | Distinct icon for modifier suggestions. Skip if reusing `bbk.svg`. |

---

## 3. Modified files

### 3.1 `src/main/resources/META-INF/plugin.xml` (required)

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<lang.wordScanner
    language="BBK"
    implementationClass="com.larena.boxbreaker.plugin.bbk.lexer.BbkWordsScanner"/>

<completion.contributor
    language="BBK"
    implementationClass="com.larena.boxbreaker.plugin.bbk.completion.BbkCompletionContributor"/>

<defaultLiveTemplates
    file="/liveTemplates/Bbk.xml"/>

<liveTemplateContext
    contextId="BBK"
    implementation="com.larena.boxbreaker.plugin.bbk.templates.BbkLiveTemplateContext"/>

<liveTemplateContext
    contextId="BBK_TOPLEVEL"
    baseContextId="BBK"
    implementation="com.larena.boxbreaker.plugin.bbk.templates.BbkTopLevelTemplateContext"/>

<liveTemplateContext
    contextId="BBK_PROC_BODY"
    baseContextId="BBK"
    implementation="com.larena.boxbreaker.plugin.bbk.templates.BbkProcedureBodyTemplateContext"/>
```

### 3.2 `BbkSyntaxHighlighter.java` (optional, recommended)

If the L5/L6 keywords (file ops `read`/`chain`/`write`/...; subroutine markers `begsr`/`endsr`/`exsr`/`leavesr`; directives `PRE-*`) are not yet covered in the highlighter, complete it before shipping Block A. Otherwise, suggestions land in the editor as plain text and the experience feels incoherent.

---

## 4. Files NOT touched

For the record — none of these need changes:

| File | Why |
|---|---|
| `BBK.bnf`, `BBK.flex` | Grammar/lexer stable from L6 |
| `gen/**/*.java` | Auto-generated PSI/parser — reused as-is |
| `BbkParserDefinition.java`, `BbkLexerAdapter.java` | Lexical/syntactic infra |
| `BbkElementType.java`, `BbkTokenType.java` | Token/element type infra |
| `BbkFileType.java`, `BbkLanguage.java` | Language registration |
| `BbkParserUtil.java` | Only used during parsing |
| `psi/BbkFile.java`, `psi/BbkPsiElement.java` | Providers read PSI; they don't need helpers added here |
| `icons/bbk.svg` | Can be reused if extra icons are skipped |

---

## 5. Headcount summary

| Category | Count |
|---|---|
| New Java classes | 18 |
| New XML resources | 1 (`Bbk.xml`) |
| Optional new SVG icons | 0–3 |
| Required modified files | 1 (`plugin.xml`) |
| Recommended modified files | 1 (`BbkSyntaxHighlighter.java`) |
| Touched but unmodified | 0 |

**Estimated effort:** 3–5 days. The 18 files are heavy on boilerplate but short; the real work is (a) getting the patterns right for the 12 contexts without overlap, and (b) deciding the exact catalog each provider suggests.

---

## 6. Order of implementation

Suggested sequence to minimize half-broken states:

1. `BbkWordsScanner` + register in `plugin.xml` → verify hyphenated keywords are recognized as one word.
2. `BbkIcons` + `BbkCompletionPatterns` + `BbkCompletionContributor` (empty) + register in `plugin.xml` → infrastructure compiles and loads without contributing anything.
3. `BbkHyphenAwarePrefixMatcher` → wired into the contributor.
4. Add providers one at a time, in this order (each one independently demoable):
   1. `BbkTopLevelKeywordProvider` — easiest to verify
   2. `BbkTypeProvider`
   3. `BbkVarModifierProvider`
   4. `BbkDsModifierProvider`
   5. `BbkParamModifierProvider`
   6. `BbkFileKeywordProvider`
   7. `BbkProcModifierProvider`, `BbkPrModifierProvider`
   8. `BbkStatementKeywordProvider`
   9. `BbkFileOpProvider`
   10. `BbkDirectiveProvider`
   11. `BbkStarIdentProvider`
5. `BbkLiveTemplateContext` + the two sub-contexts + `Bbk.xml` → live templates light up.
6. Optionally extend `BbkSyntaxHighlighter` to color the L5/L6 keywords if not done yet.

After step 4.i the autocomplete already feels real for top-level declarations — that's the first natural demo checkpoint.

---

## 7. Closed decisions

These resolve the open questions raised during design review. They apply across all providers and shape the implementation defaults.

### 7.1 UX / catalog

| # | Decision | Resolution |
|---|---|---|
| 1 | Icon strategy | **Deferred to TODO.** Start by reusing `bbk.svg` for every category; revisit later whether to add differentiated icons (keyword / type / modifier / directive / file-op / star-ident). Tracked in root `TODO.md` under `plugin-bbk: basic autocomplete`. |
| 2 | Tail text / type text format | **Option B (schematic).** `INT` with tail `(precision)` and type text `primitive type`; `PACKED` with tail `(precision:scale)`. Modifiers, keywords, file ops follow the same pattern: schematic placeholder in tail text when the keyword takes args, empty otherwise. |
| 3 | InsertHandler behavior | **Auto-insert parens and braces.** `INT` → `INT(\|)` with cursor inside the parens. `DCL-S` → `DCL-S ` (trailing space). `if` → `if (\|) {\n  \n}` with cursor at the condition (overlaps with the `ifb` template; the keyword provider wins when the user types `if<Ctrl+Space>`, the template wins when the user types `ifb<Tab>`). Same skeleton-completion for `while`, `for`, `select`, `monitor`, `do`. |
| 4 | Auto-popup trigger | **Open after 1 character typed.** Configured via `CompletionContributor.invokeAutoPopup` returning `true` for relevant prefixes. |

### 7.2 Non-trivial syntactic patterns

All confirmed as required. Each gets a dedicated helper in `BbkCompletionPatterns` and may require an ancestor walk:

| # | Context | Pattern requirement |
|---|---|---|
| 5 | `else` | Lookbehind: must follow a `BbkBlockStatement` whose parent is a `BbkIfStatement`. |
| 6 | `when` / `other` | Ancestor walk: cursor must be inside the `{ ... }` of a `BbkSelectStatement` (not nested inside another block). |
| 7 | `on-error` / `on-exit` | Lookbehind: must follow the `}` of a `monitor` block or another `on-error` clause. |
| 8 | `begsr` / `endsr` / `leavesr` | Stateful: `begsr` only at procedure-body top level (not nested inside another block); `endsr` only when there's an unclosed `begsr` ancestor; `leavesr` only inside a `BbkSubroutineDefinition`. |
| 9 | Comma in arg lists | After a `COMMA` token inside `BbkInlineParamList` or `BbkArgumentList`, fire the type/modifier providers again. |

### 7.3 Star identifiers, BIFs, CTL-OPT keywords

| # | Decision | Resolution |
|---|---|---|
| 10 | CTL-OPT keywords | **Catalog explicit.** Add to `BbkTopLevelKeywordProvider` (or new `BbkCtlOptKeywordProvider` if the list grows): `MAIN`, `NOMAIN`, `OPTION`, `BNDDIR`, `DFTACTGRP`, `ACTGRP`, `DEBUG`, `DECEDIT`, `DATFMT`, `TIMFMT`, `COPYRIGHT`, `EXTBININT`, `FIXNBR`, `THREAD`, `ALLOC`. |
| 13 | `STAR_IDENT` sub-contexts | **One provider, three internal sub-patterns:** `*INPUT`/`*OUTPUT`/`*UPDATE`/`*DELETE` only inside `USAGE(`; `*NOPASS`/`*OMIT`/`*VARSIZE`/`*STRING`/`*NULLIND` only inside `OPTIONS(`; `*NO`/`*YES`/`*NEW`/`*CALLER` only inside `CTL-OPT` arg lists. |
| 14 | BIFs in Block A | **Included.** Add `BbkBifProvider` (extra provider, brings new-class count to 19) with `trim`, `triml`, `trimr`, `substr`, `scan`, `len`, `char`, `dec`, `int`, `inth`, `abs`, `elem`, `addr`, `size`, `date`, `time`, `timestamp`, `days`, `diff`, `lookup`, `xlate`. Fires in expression context inside procedure bodies. Block E later upgrades these with signature docs and type-aware filtering. |

### 7.4 Prefix matcher

| # | Decision | Resolution |
|---|---|---|
| 11 | Matching algorithm | **Three modes accepted by `BbkHyphenAwarePrefixMatcher`:** (a) prefix without hyphen — typing `dcls` matches `DCL-S`; (b) literal prefix — typing `dcl-s` matches `DCL-S`; (c) CamelHumps-with-hyphen — typing `d-s` matches `DCL-S`, `d-c` matches `DCL-C`, etc. All case-insensitive. Implementation: strip `-` from both candidate and prefix for comparison in mode (a); first-letter-of-segment matching for mode (c). |

### 7.5 Validation risks

| # | Decision | Resolution |
|---|---|---|
| 15 | PSI shape with dummy identifier | **Verify per provider.** Before merging a provider, manually exercise its pattern in the sandbox IDE: open a `.bbk` file, position cursor in the intended context, trigger completion, and confirm that `parameters.getPosition()` returns a PSI element whose parent chain matches the pattern. Document any surprises in `pattern-notes.md` (to be created if surprises accumulate). |

### 7.6 Process

| # | Decision | Resolution |
|---|---|---|
| 16 | Tests | **Deferred.** No completion tests in Block A. To be added later as a dedicated milestone. |
| 17 | i18n | **Adopted from day one.** All user-visible strings (tail text, type text, lookup descriptions, template descriptions) go through a `BbkBundle` properties file at `src/main/resources/messages/BbkBundle.properties`. Code reads them via `BbkBundle.message("completion.type.int.tail")` etc. Adds 1 new file (the bundle) plus a `BbkBundle.java` helper class to the count (→ **20 new Java classes + 1 properties file**). |
| 18 | Sorting / priority | **Alphabetical (IntelliJ default).** No custom `LookupElement.priority` for now. Revisit only if popup ordering proves confusing in practice. |

---

## 8. Updated headcount

After applying the closed decisions:

| Category | Previous | After decisions |
|---|---|---|
| New Java classes | 18 | **20** (added `BbkBifProvider`, `BbkBundle`) |
| New XML resources | 1 | 1 |
| New properties files | 0 | **1** (`BbkBundle.properties`) |
| Optional new SVG icons | 0–3 | **0** (deferred to TODO) |
| Required modified files | 1 | 1 |
| Recommended modified files | 1 | 1 |

**Estimated effort:** 4–6 days (slightly above the prior 3–5 because of i18n routing and the BIF provider).

---

## 9. Post-Block A polish: smart typing handler

Added after the first sandbox session surfaced this papercut:

> Typing `MAIN(` (autocompleted) → `procName` → `;` produced `MAIN(procName;)` because the `;` landed at the caret instead of past the auto-inserted `)`.

Resolved by one new class and one `plugin.xml` extension:

| File | Responsibility |
|---|---|
| `editor/BbkSmartTypingHandler.java` | Extends `TypedHandlerDelegate`. Two rules: (a) typing `;` fast-forwards the caret past any trailing `)` characters before the platform inserts the `;`; (b) typing `)`, `}` or `]` while that exact char is at the caret just advances the caret (skip-over), mirroring Java/Kotlin behaviour. Active only inside `BbkFileType`. |
| `META-INF/plugin.xml` | Adds `<typedHandler implementation="...BbkSmartTypingHandler"/>`. |

This is not part of any of the planned blocks (B/C/D/E) — it's pure editor ergonomics that complements Block A's auto-paren `InsertHandler`s.
