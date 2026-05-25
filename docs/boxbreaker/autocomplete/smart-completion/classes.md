# Smart Completion — Classes

**Status:** not implemented — design only
**Scope:** every class to create and every method to modify so `Ctrl+Shift+Space` filters candidates by the expected type at the caret
**Companion:** [`theory.md`](theory.md)
**Prerequisites:** Basic Completion (#1–#3) and References + Scope (#5–#7) landed. Adds a new package `types/` that is the heart of the work.

---

## 1. New classes

All under `plugin-bbk/src/main/java/com/larena/boxbreaker/plugin/bbk/`.

### 1.1 Type system — the foundation (`types/`)

The dominant block. Reused by features #10 (type-aware inspections), #11 (parameter info) and partly #12 (quick docs). Investing here pays back four times.

| Class | Responsibility | Approx LOC |
|---|---|---|
| `types/BbkType.java` | Sealed root interface for every BBK type. Methods: `getDisplayName()`, `isAssignableTo(BbkType target)`. Single contract every other type implements. | ~30 |
| `types/BbkPrimitiveType.java` | `INT(n)`, `UNS(n)`, `CHAR(n)`, `VARCHAR(n)`, `PACKED(p:d)`, `ZONED(p:d)`, `FLOAT`, `BINDEC(p:d)`, `BOOL`, `DATE`, `TIME`, `TIMESTAMP`, `POINTER`, `VOID`. Carries precision / length as data. Implements `equals` so two `INT(10)` instances are equal. | ~120 |
| `types/BbkStructType.java` | DS qualified types. Holds a reference to the declaring `BbkDataStructureDeclaration` + the ordered list of subfields with their typed representation. Required for `LIKEDS(x)` and for member-access type inference (`employee.firstName`). | ~60 |
| `types/BbkProcedureType.java` | Procedure signature: ordered `List<BbkParameterType>` (each carries name, type, `VALUE`/`CONST` flags) + return `BbkType`. Used both when the procedure name appears alone (as a function reference) and when called (`f(...)` produces the return type). | ~50 |
| `types/BbkArrayType.java` | Wrapper for `DIM(n)`. Carries the element type and the dimension(s). Open decision: does `arr` in an expression resolve to the array or to the element? (See `theory.md` §8.) | ~40 |
| `types/BbkUnknownType.java` | Singleton sentinel returned by the inferrer when a sub-expression cannot be resolved. Lets the rest of the inference keep flowing instead of aborting. `isAssignableTo` returns `true` so it never blocks completion. | ~15 |
| `types/BbkTypeFromPsi.java` | Converts a `BbkTypeSpecification` PSI node into `BbkType`. Resolves `LIKE(x)` / `LIKEDS(x)` / `LIKEREC(x)` by following the existing `BbkTypeReference`. Cycle protection (a→b→a) needed for `LIKE` chains. | ~80 |
| `types/BbkTypeInferrer.java` | Public entry points: `typeOf(PsiNamedElement decl)` and `typeOf(BbkExpression expr)`. Walks the expression tree applying inference rules per node kind (literal, ident, member access, binary, unary, call). The core engine. | ~200 |
| `types/BbkAssignability.java` | Stateless table + rule engine: `areCompatible(BbkType source, BbkType target)`. Covers identity, decimal promotion (`INT(5) → INT(10)`), generic-literal widening (`INT(*) → INT(n)`), and any BBK-specific rules decided in `theory.md` §8. | ~100 |

**Subtotal:** 9 classes, ~700 LOC. Mostly mechanical once the design decisions in `theory.md` §8 are nailed down.

### 1.2 Expected-type providers (`completion/expected/`)

Two valid shapes — pick A for V1.

**Shape A (recommended, one class):**

| Class | Responsibility |
|---|---|
| `completion/expected/BbkExpectedTypeProvider.java` | Static `find(PsiElement caretLeaf) → @Nullable BbkType`. Internally walks the PSI upward and pattern-matches: are we the RHS of an assignment? An argument of a call? The body of an `if`? Returns the type the position requires, or `null` when the context is type-free. ~150 LOC. |

**Shape B (one class per context):** only refactor to B if A grows past ~6 cases. Skeleton in `theory.md` §7.

### 1.3 Smart completion provider (`completion/providers/`)

| Class | Responsibility |
|---|---|
| `completion/providers/BbkSmartCompletionProvider.java` | Activated when `parameters.getCompletionType() == SMART`. Calls `BbkExpectedTypeProvider.find` — if null, contributes nothing. Otherwise enumerates candidates via `BbkScopeWalker.allVisible(position)` plus the cross-file stub indexes already wired, types each candidate via `BbkTypeInferrer.typeOf`, filters via `BbkAssignability.areCompatible`, and emits the surviving `LookupElement`s. No keywords. ~80 LOC. |

---

## 2. Modified classes

### 2.1 `completion/BbkCompletionContributor`

Single new branch dispatching on `getCompletionType()`. The BASIC branch stays untouched:

```java
if (parameters.getCompletionType() == CompletionType.SMART) {
    new BbkSmartCompletionProvider().addCompletions(parameters, ctx, result);
    return;
}
// existing BASIC path unchanged
```

~10 lines added. No existing provider modified.

### 2.2 `scope/BbkScopeWalker` (optional optimisation)

Today `allVisible(position)` returns every declaration in scope unfiltered. Optional overload:

```java
public static List<PsiNamedElement> allVisible(PsiElement at, Predicate<PsiNamedElement> filter);
```

Lets `BbkSmartCompletionProvider` reject candidates by name/kind before the more expensive type inference runs. Not required for correctness — performance only. Defer unless completion latency becomes noticeable on large files.

---

## 3. Files NOT touched

| File | Why |
|---|---|
| `META-INF/plugin.xml` | `<completion.contributor>` already covers all `CompletionType` values (BASIC, SMART, CLASS_NAME) with the same contributor instance. No new extension. |
| `BBK.bnf`, `BBK.flex` | Inference operates on the existing PSI. No grammar change. |
| `stub/*`, `index/*` | Cross-file candidate enumeration already exists. Smart completion just consumes it. |
| `reference/*` | The inferrer follows existing references — no change to reference code. |
| All existing completion providers (`BbkKeywordCompletionProvider`, `BbkScopeCompletionProvider`, etc.) | BASIC behaviour is unchanged. SMART is a separate branch. |
| `BbkParserDefinition`, `BbkLanguage`, `BbkFileType` | Stable. |
| Live templates, brace matcher, smart typing | Unrelated. |

---

## 4. Headcount summary

| Category | Count |
|---|---|
| Type system (mandatory) | **9** new |
| Expected-type provider (mandatory) | **1** new (shape A) |
| Smart completion provider (mandatory) | **1** new |
| Modified classes | **1** (`BbkCompletionContributor`) |
| Optional optimisation | +1 overload on `BbkScopeWalker` |
| XML changes | **0** |

**Total mandatory:** 11 new classes, 1 method added to existing contributor, ~900 LOC.

**Estimated effort:** 7–10 days for Smart Completion (see `theory.md` §9). Most of it is the type system; once paid, features #10, #11, #12 reuse the same engine.

---

## 5. Order of implementation

Sequenced so every step yields a testable artefact and the type engine grows independently of the completion wiring.

1. **`BbkType` + `BbkPrimitiveType` + `BbkUnknownType`** — minimum representation. Test: `INT(10).isAssignableTo(INT(20)) == true`, reverse `false`.
2. **`BbkTypeFromPsi`** for primitives — given a `DCL-S x INT(10)`, produce `INT(10)`. Test on synthetic files.
3. **`BbkTypeInferrer.typeOf(PsiNamedElement)`** for variables and constants — read the declaration's type spec. Test: `typeOf(decl)` matches expected for every primitive.
4. **`BbkStructType` + `BbkTypeFromPsi` for `LIKEDS`** — resolve the reference, build the struct type, cycle-protect. Test on a chain `LIKEDS(a) → LIKEDS(b)`.
5. **`BbkProcedureType` + `BbkTypeInferrer.typeOf` for procedure decls and call expressions** — calling produces return type.
6. **`BbkTypeInferrer.typeOf(BbkExpression)` for literals, ident references, member access** — covers the cases Smart Completion needs most.
7. **`BbkTypeInferrer` for binary / unary expressions** — apply decimal promotion. Defer if not blocking.
8. **`BbkAssignability` rules table** — implement promotions decided in `theory.md` §8. Heavy test coverage.
9. **`BbkExpectedTypeProvider`** with the first 3 cases (assignment, condition, call argument). Test each.
10. **`BbkSmartCompletionProvider`** wired through the contributor — first end-to-end Smart Completion. Integration test: `DCL-S x INT(10); DCL-S y CHAR(10); x = <caret>` shows `x` but not `y`.
11. **Remaining expected-type cases** (INZ, return, for-bounds) — each is one new pattern + one test.
12. **`BbkScopeWalker` predicate overload** if profiling shows the need.

Each numbered step is an independent verifiable commit.

---

## 6. Open decisions (echo from `theory.md` §8)

These are language-design decisions. Smart Completion's perceived quality depends on them more than on any algorithm.

| # | Topic | Question |
|---|---|---|
| 1 | Implicit numeric promotion | Allow `INT → PACKED` without `%dec()`? Reject? Suggest both with soft warnings? |
| 2 | `CHAR` / `VARCHAR` widening | Is `CHAR(20)` assignable to `CHAR(50)`? |
| 3 | DS field-by-field compatibility | Two DS with same subfields but different declared names — compatible? |
| 4 | `LIKE` vs `LIKEDS` chains | Resolve transitively? Cycle policy? |
| 5 | Literal types | Should `42` be `INT(*)` (assignable anywhere numeric) or a specific minimum-precision `INT(2)`? |
| 6 | Offer literals in Smart Completion | `if <caret>` → suggest `*TRUE`, `*FALSE`? |
| 7 | Offer builtins | `INT(10) = <caret>` → suggest `%len(x)`, `%size(x)` if they return INT? |

Decide before coding `BbkAssignability` — every answer changes its rules table.

---

## 7. What is NOT covered

- Ranking by relevance among already-filtered candidates (proximity, recency) — IntelliJ default ordering is enough for V1
- "Second smart completion" (`Ctrl+Shift+Space` twice) for chained-call exploration
- Auto-import of cross-file symbols — BBK V1 has no `import`/`/COPY` mechanism
- Type-driven post-completion templates (auto-expand `.firstSubfield = ` after picking a DS variable)
- Anything that requires runtime values (constant folding, range narrowing). The inferrer is strictly syntactic.

---

## 8. Related documents

- [`theory.md`](theory.md) — theory, motivation, decisions
- [`../functionalities.md`](../functionalities.md) — full feature map
- [`../basic-autocomplete/theory.md`](../basic-autocomplete/theory.md) — BASIC completion infrastructure that Smart builds on
- [`../basic-autocomplete/classes.md`](../basic-autocomplete/classes.md) — existing completion provider catalogue
- [`../reference-scope/theory.md`](../reference-scope/theory.md) — scope walker (candidate enumeration)
- [`../stubs-index/theory.md`](../stubs-index/theory.md) — cross-file enumeration (candidate source under SMART)
- [`../rename/classes.md`](../rename/classes.md) — sibling document, same shape
