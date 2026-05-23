# Reference + Scope — Classes

**Status:** design / not implemented
**Scope:** every class to create and every file to modify for Block B (reference + scope)
**Companion:** [`theory.md`](theory.md)
**Prerequisite:** Block A landed and verified

---

## 1. New classes

All under `plugin-bbk/src/main/java/com/larena/boxbreaker/plugin/bbk/`.

### 1.1 PSI naming utility

| Class | Responsibility |
|---|---|
| `psi/util/BbkPsiImplUtil.java` | Static methods Grammar-Kit will weave into the auto-generated PSI classes via the BNF `psiImplUtilClass` header. One overload of `getName(...)`, `setName(...)`, `getNameIdentifier(...)` per named declaration type: `BbkVariableDeclaration`, `BbkConstantDeclaration`, `BbkDataStructureDeclaration`, `BbkDsSubfield`, `BbkInlineParam`, `BbkPrototypeDeclaration`, `BbkProcedureDeclaration`, `BbkFileDeclaration`, `BbkSubroutineDefinition`. This is the wiring that makes every declaration a `PsiNamedElement`. |

### 1.2 References

All extend `PsiReferenceBase<PsiElement>` (or `PsiPolyVariantReferenceBase` if multi-target ever needed; not required for BBK). All four route their actual resolution logic through `ResolveCache.getInstance(...).resolveWithCaching(...)`.

| Class | Responsibility |
|---|---|
| `reference/BbkReferenceContributor.java` | Registers the patterns that turn an IDENT in a use-site into a `PsiReference`. One `registerReferenceProvider(...)` call per kind of use-site (generic identifier, member access RHS, exsr argument, LIKE/LIKEDS argument). |
| `reference/BbkIdentReference.java` | The generic case. Resolves an IDENT to the first matching `PsiNamedElement` found by walking the scope chain returned by `BbkScopeWalker.scopeAt(position)`. Implements `getVariants()` by flattening the scope chain. |
| `reference/BbkMemberReference.java` | Handles the IDENT after a dot. Calls `BbkTypeResolver.typeOf(lhs)` to get the declared type, then looks up the field name among that DS's subfields. |
| `reference/BbkSubroutineReference.java` | Resolves the IDENT inside `exsr <name>` to a `BbkSubroutineDefinition` whose `begsr <name>` matches. Scope is the enclosing procedure body. |
| `reference/BbkTypeReference.java` | Resolves the IDENT inside `LIKE(...)`, `LIKEDS(...)`, `LIKEREC(...)` to its referent declaration (DCL-S, DCL-DS, file format). Same scope walker; filters candidates by declaration kind. |

### 1.3 Scope

| Class | Responsibility |
|---|---|
| `scope/BbkScope.java` | Interface. `List<PsiNamedElement> getDeclarations(String nameOrNull)` returns either all declarations or the ones matching a name (lets implementations short-circuit). `BbkScope getParent()` returns the enclosing scope, or `null` at the root. |
| `scope/BbkScopeWalker.java` | Static façade. `BbkScope scopeAt(PsiElement position)` builds the chain by walking ancestors. `PsiNamedElement resolve(PsiElement position, String name, Class<? extends PsiNamedElement> filter)` is the convenience used by every reference. |
| `scope/BbkModuleScope.java` | Top-level scope. Iterates the file's children for `BbkVariableDeclaration`, `BbkConstantDeclaration`, `BbkDataStructureDeclaration`, `BbkPrototypeDeclaration`, `BbkProcedureDeclaration`, `BbkFileDeclaration`. Also exposes subfields of non-`QUALIFIED` DSs at module level. |
| `scope/BbkProcedureScope.java` | Scope of the enclosing `BbkProcedureDeclaration`. Iterates inline parameters and the procedure's local declarations (DCL-S/C/DS). Excludes declarations after the cursor position to honour "declare before use" (configurable; BBK currently allows forward references for declarations, so this may be a permissive `true` for V1 — see open decision). |
| `scope/BbkBlockScope.java` | Scope of the smallest enclosing `BbkBlockStatement` that contains the cursor. Picks up local declarations inside nested blocks (e.g. `for (DCL-S i INT(10) = 0; ...)`. |

### 1.4 Shallow type resolver

| Class | Responsibility |
|---|---|
| `types/BbkTypeResolver.java` | Lightweight, intentionally not a real type system. Two methods: `typeOf(PsiElement expr)` returns a `BbkDataStructureDeclaration` for the common cases (`employee`, `employees[i]`, `getCustomer()`), or `null` if unknown. Walks `LIKEDS(otherDs)` chains transparently. Block D will later replace this with a full inference engine. |

### 1.5 Completion providers (added on top of Block A)

Both extend `BbkKeywordProviderBase` (existing) so they automatically pick up the prefix matcher, icons, and i18n plumbing.

| Class | What it suggests | Context |
|---|---|---|
| `completion/providers/BbkScopeCompletionProvider.java` | Every visible identifier (variables, constants, procedures, prototypes, subroutines, files, DS names) at the cursor position. Suggestions come from `BbkScopeWalker.scopeAt(position)`. | Any expression / statement position where an IDENT is grammatically valid. |
| `completion/providers/BbkMemberCompletionProvider.java` | The subfields of the DS that `BbkTypeResolver.typeOf(lhs)` returns. | Right after a `.` token whose LHS resolves to a DS. |

---

## 2. New resources

| File | Responsibility |
|---|---|
| (none) | Block B adds no new XML or properties files. All new strings (e.g. "unresolved reference", suggestion type texts for variables/procedures) are added as keys to the existing `messages/BbkBundle.properties`. |

---

## 3. Modified files

### 3.1 `src/main/grammar/BBK.bnf` (required)

Two changes:

- **Header:** add `psiImplUtilClass="com.larena.boxbreaker.plugin.bbk.psi.util.BbkPsiImplUtil"` to the top stanza so Grammar-Kit knows where to find the static methods.
- **Per named declaration rule:** add `mixin="com.larena.boxbreaker.plugin.bbk.psi.BbkNamedElementMixin"` and `methods=[getName setName getNameIdentifier]` attributes on the rules for `variable_declaration`, `constant_declaration`, `data_structure_declaration`, `ds_subfield`, `inline_param`, `prototype_declaration`, `procedure_declaration`, `file_declaration`, `subroutine_definition`. The mixin extends `ASTWrapperPsiElement implements PsiNamedElement`; Grammar-Kit weaves the `methods` list to call into `BbkPsiImplUtil`.

Requires a regen via `:plugin-bbk:generateBbkParser`.

### 3.2 `src/main/java/com/larena/boxbreaker/plugin/bbk/psi/BbkNamedElementMixin.java` (new helper for the BNF mixin)

| Class | Responsibility |
|---|---|
| `psi/BbkNamedElementMixin.java` | One-class scaffold extending `ASTWrapperPsiElement implements PsiNamedElement`. Grammar-Kit emits each named-declaration PSI impl as a subclass of this mixin. Empty body; `getName` / `setName` come via `BbkPsiImplUtil`. |

(Adds 1 to the headcount, but it's BNF plumbing not really logic.)

### 3.3 `src/main/resources/META-INF/plugin.xml` (required)

Add one extension:

```xml
<psi.referenceContributor
    language="BBK"
    implementation="com.larena.boxbreaker.plugin.bbk.reference.BbkReferenceContributor"/>
```

### 3.4 `completion/BbkCompletionContributor.java` (required)

Two new `extend(...)` calls:

```java
extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(),
       new BbkScopeCompletionProvider());
extend(CompletionType.BASIC, BbkCompletionPatterns.anyBbkElement(),
       new BbkMemberCompletionProvider());
```

### 3.5 `completion/providers/BbkStatementKeywordProvider.java` (recommended)

Refine the `else` gating: replace the unconditional `else` suggestion with one that only fires when the immediately preceding sibling is the closing brace of an `if` block. Doable now that PSI walking is on the table.

### 3.6 `messages/BbkBundle.properties` (required)

Add keys for the new categories:

```
completion.type.userVariable   = variable
completion.type.userConstant   = constant
completion.type.userProcedure  = procedure
completion.type.userPrototype  = prototype
completion.type.userDataStruct = data structure
completion.type.userSubfield   = field
completion.type.userSubroutine = subroutine
completion.type.userFile       = file
inspection.unresolvedReference = Cannot resolve symbol ''{0}''
```

---

## 4. Files NOT touched

| File | Why |
|---|---|
| `BBK.flex` | Lexer unchanged |
| `gen/**/*.java` | Auto-regenerated from BBK.bnf; no manual edits |
| `BbkLanguage.java`, `BbkFileType.java`, `BbkLexerAdapter.java`, `BbkParserDefinition.java` | Stable |
| `BbkElementType.java`, `BbkTokenType.java` | Token/element type infra |
| `BbkSyntaxHighlighter.java` | No new colours needed |
| `BbkWordsScanner.java` | Already in place from Block A |
| Existing 14 completion providers from Block A | Untouched (except the `else` refinement in §3.5) |
| Existing live templates | Untouched |

---

## 5. Headcount summary

| Category | Block A baseline | Added in B | After B |
|---|---|---|---|
| New Java classes | 22 | **14** | 36 |
| New XML resources | 1 | 0 | 1 |
| Properties files | 1 | 0 (extended) | 1 |
| Required modified files | 1 (`plugin.xml`) | 3 (`plugin.xml`, `BBK.bnf`, `BbkCompletionContributor`) | — |
| Recommended modified files | 1 (highlighter) | 1 (`BbkStatementKeywordProvider`) | — |

**Block B-only new class breakdown:** 1 PSI util + 1 named-element mixin + 5 references + 5 scope + 1 type resolver + 2 completion providers = **15 classes**. The classes.md is conservative and lists 14 (treating the mixin as a marker shipped with BbkPsiImplUtil). Round to 14–15 depending on how strict you count.

**Estimated effort:** 5–7 days.

---

## 6. Order of implementation

Suggested sequence to minimize half-broken states:

1. **PSI naming first.** Edit `BBK.bnf` to add `psiImplUtilClass` + `mixin` on every named declaration. Create `BbkNamedElementMixin` and `BbkPsiImplUtil`. Regenerate via `:plugin-bbk:generateBbkParser`. Verify the project compiles and `BbkVariableDeclaration` now implements `PsiNamedElement`.
2. **Scope infrastructure.** Create `BbkScope` interface + the three concrete scopes + `BbkScopeWalker`. Unit-test by hand: open a file in sandbox IDE, place a probe (a temporary action) that prints `scopeAt(cursor)` to the IDE log.
3. **Generic identifier reference.** Create `BbkReferenceContributor` registering just `BbkIdentReference` for IDENTs that are not part of declarations. Register in `plugin.xml`. Verify `Ctrl+B` works for the simplest case (a `DCL-S` referenced from an assignment in the same procedure).
4. **Subroutine and type references.** Add `BbkSubroutineReference` and `BbkTypeReference`. Verify `Ctrl+B` works on `exsr myRoutine` and `LIKEDS(myDs)`.
5. **Shallow type resolver + member reference.** Implement `BbkTypeResolver.typeOf` for the common cases. Add `BbkMemberReference`. Verify `Ctrl+B` works on `employee.firstName`.
6. **Caching.** Wrap each reference's `resolveInner` in `ResolveCache.getInstance(...).resolveWithCaching(...)`. Open a 300-line `.bbk` file and confirm typing stays responsive.
7. **Completion providers.** Add `BbkScopeCompletionProvider` and `BbkMemberCompletionProvider`. Wire into `BbkCompletionContributor`. Verify the popup now lists user variables and DS fields.
8. **Refine Block A gating.** Use the new PSI knowledge to fix `else` and any other gating imperfections surfaced in Block A sandbox testing.
9. **Bundle keys.** Add the new strings to `BbkBundle.properties` and route every new user-visible string through `BbkBundle.message(...)`.

After step 3 the plugin already feels "alive" — that's the first natural demo checkpoint.

---

## 7. Open decisions (to resolve before / during implementation)

These are the genuinely ambiguous points worth resolving up front. They mirror the structure of Block A's section 7 but are still open as of writing.

| # | Topic | Question |
|---|---|---|
| 1 | "Declare before use" enforcement | BBK currently allows forward references for top-level declarations. Should `BbkProcedureScope` enforce that a local declaration is visible only from its line onward, or accept forward references for V1 to match Java/C99-style flexibility? |
| 2 | Non-qualified DS subfield exposure | When a DS is declared without `QUALIFIED`, its subfields are visible by bare name in the surrounding scope. Should `BbkModuleScope` / `BbkProcedureScope` flatten those subfields into the scope, or require the user to use `LIKEDS` patterns? |
| 3 | LIKE / LIKEDS / LIKEREC chain depth | Should `BbkTypeResolver` follow chains of any depth (`X` → `LIKEDS(Y)` → `LIKEDS(Z)` → ...) or cap at N hops to avoid pathological cases? Recommend uncapped with cycle detection. |
| 4 | Subroutine visibility | `begsr` is procedure-scoped. Should `exsr` from one procedure resolve to a `begsr` in another procedure? In RPG, no. Default to "no" and report unresolved. |
| 5 | DCL-PR vs DCL-PROC when both exist | A prototype + a definition both name the same procedure. Should `resolve()` prefer the definition (more useful for go-to), or return both as a `PsiPolyVariantReference`? Recommend: definition wins; prototype is a "secondary" navigation target reachable through find-usages. |
| 6 | Caching strategy | Use IntelliJ's `ResolveCache` (recommended) or roll a custom `CachedValue`. The first is idiomatic and free. |
| 7 | Reference range inside the IDENT token | A `PsiReference` declares a `TextRange` inside its owning element. For a leaf IDENT, the range is the full leaf. Confirm that this is consistent for member access (where the IDENT after the `.` is the reference range, not the `.` itself). |
| 8 | Performance budget for `getVariants()` | Worst case: a 1000-line file with hundreds of variables. `getVariants()` should be O(scope size), not O(file size). Recommend lazy flattening of scope chains with early termination if IntelliJ truncates. |

These should be triaged similarly to how Block A's 18 open questions were triaged before implementation.

---

## 8. Closed decisions

(Empty for now. Populate after a design review the same way Block A's §7 was populated.)
