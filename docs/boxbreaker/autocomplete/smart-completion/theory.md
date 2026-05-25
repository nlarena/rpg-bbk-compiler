# Smart Completion (`Ctrl+Shift+Space`) — Theory

**Status:** not implemented
**Scope:** the IntelliJ Platform concepts behind feature #9 (Smart type-aware completion) of [`../functionalities.md`](../functionalities.md)
**Plugin module:** `plugin-bbk/`
**Prerequisites:** Basic Completion (#1), Identifier Completion (#2), References + Scope (#5–#7). Adds a new dependency: a type system.

---

## 1. What it is

Smart Completion is **autocomplete filtered by the type expected at the caret**. Where `Ctrl+Space` (Basic Completion) suggests "everything syntactically valid here", `Ctrl+Shift+Space` (Smart Completion) suggests **only what additionally matches the type the position requires**.

Example in BBK:

```bbk
DCL-S counter INT(10);
DCL-S name    CHAR(50);
DCL-S retries INT(10);
DCL-PROC isReady -> BOOL { ... }
DCL-PROC getName -> CHAR(50) { ... }

counter = <caret>
```

- `Ctrl+Space` suggests `counter`, `name`, `retries`, `isReady`, `getName`, every valid keyword, every builtin...
- `Ctrl+Shift+Space` suggests **only** `counter`, `retries`, plus any function/builtin returning `INT(10)` (or assignable to it). `name` disappears because it is `CHAR(50)`. `isReady` disappears because it is `BOOL`.

The user reaches for it when they already know they need "something of a certain type" but cannot remember the name.

---

## 2. The three theoretical pieces

### 2.1 Expected type at the caret

Before filtering candidates, the engine must know **what type is expected there**. This is inferred from the **syntactic context**, not from the candidate. Typical cases:

| Context | Expected type |
|---|---|
| `counter = <caret>` | type of `counter` (LHS of assignment) |
| `if <caret>` | `BOOL` |
| `for i = 1 to <caret>` | `INT(*)` |
| `helper(<caret>)` | type of parameter #0 of `helper` |
| `helper(x: <caret>)` | type of parameter #1 |
| `DCL-S x INT(10) INZ(<caret>)` | `INT(10)` |
| `return <caret>` (inside proc → INT) | `INT(*)` |

Each of these is a distinct syntactic pattern. **If none is recognised, smart completion has nothing to filter against** and must fall back gracefully.

Implementation-wise this becomes a collection of "expected-type providers": each one inspects the PSI around the caret and, if it recognises its pattern, returns a `BbkType`. If none recognises, Smart Completion does not apply and yields to Basic.

### 2.2 Type inference for each candidate

To decide if a candidate fits, we need to know **the type of each candidate**. This is the *type inference* engine:

- A variable `DCL-S x INT(10)` has type `INT(10)` — trivial, read from the declaration.
- A procedure `DCL-PROC f -> CHAR(50)` has type "function returning `CHAR(50)`"; when it appears as a call expression `f()`, the type of the expression is `CHAR(50)`.
- A field `employee.firstName` has the type of the subfield, resolved via `LIKEDS`/`LIKEREC`.
- A literal `42` has type `INT(*)` (generic integer literal).
- A compound expression `a + b` has a type derived by rules (in BBK typically decimal promotion: precision = max + 1, etc.).

This engine is the **same one** required for type-aware inspections (#10) and parameter info (#11). That is why the plan groups #9, #10, #11 behind "Block D — Type system".

### 2.3 Assignability

Given `expected: BbkType` and `candidate: BbkType`, is the candidate assignable to the expected? Identity is not enough because of promotion rules:

- `INT(5)` is assignable to `INT(10)` (smaller → larger precision)
- `INT(10)` is NOT assignable to `INT(5)` (would truncate)
- `CHAR(20)` perhaps assignable to `CHAR(50)` (depends on BBK semantics)
- A literal `INT(*)` assignable to any `INT(N)`
- Any type assignable to itself
- A DS subfield assignable to another DS if they share layout (BBK-specific rule to be decided)

The relation is modelled as a method `isAssignableTo(BbkType target)` on `BbkType`, or as an external comparator `BbkTypeChecker.areCompatible(a, b)`. It is **directional** (a → b ≠ b → a), **reflexive**, but not necessarily transitive across promotion rules.

---

## 3. How it integrates with IntelliJ

IntelliJ does not have a separate API for Smart Completion — the same `CompletionContributor` receives a `CompletionParameters` carrying `getCompletionType()`. Values:

- `BASIC` — `Ctrl+Space`
- `SMART` — `Ctrl+Shift+Space`
- `CLASS_NAME` — `Ctrl+Alt+Space`

The contributor decides what to do based on the type. Typical pattern:

```java
if (parameters.getCompletionType() == CompletionType.SMART) {
    BbkType expected = BbkExpectedTypeProvider.find(parameters.getPosition());
    if (expected == null) return;  // no typed context → contribute nothing
    for (PsiNamedElement decl : BbkScopeWalker.allVisible(position)) {
        BbkType declType = BbkTypeInferrer.typeOf(decl);
        if (declType != null && declType.isAssignableTo(expected)) {
            result.addElement(LookupElementBuilder.create(decl)...);
        }
    }
}
```

The **difference with Basic** lives in two places: the pre-filter (`isAssignableTo`), and the fact that **keywords are not offered** — only program elements (identifiers, builtins, suggested literals).

---

## 4. When it does not apply, fall back to Basic

If at the caret the engine cannot determine an expected type (because syntactically it sits where any expression is valid, or the PSI is incomplete and unresolvable), Smart Completion should **contribute nothing** and let the user reach for plain `Ctrl+Space`. Showing "everything" under `Ctrl+Shift+Space` breaks the promise of the feature: the user pressed it precisely because they wanted filtering.

---

## 5. The hard part is inference, not ranking

At first glance Smart Completion looks like "just rank by type". In practice the work lives elsewhere:

- **Inferring the expected type across many contexts** — every BBK syntactic construct (assignment, call, conditional, INZ, return, arithmetic expression) is a separate case.
- **Inferring the type of each candidate** — a variable is trivial; a compound expression `a + b * c` requires walking the tree applying promotion rules.
- **Deciding assignability rules** — these are language design decisions, not algorithmic ones. Is `INT(5)` → `PACKED(7:2)` legal? Implicit, or requires `%int()`? Every answer changes what gets suggested.

The type engine ends up being the most expensive piece for features 9–12; smart completion itself, once the engine exists, is ~50 lines of filtering.

---

## 6. What we have today

| Component | Status |
|---|---|
| `CompletionContributor` for BBK | ✅ (handles BASIC only) |
| Scope walker | ✅ |
| References | ✅ |
| `BbkType` representation | ❌ |
| Type inference for declarations | ❌ |
| Type inference for expressions | ❌ |
| `isAssignableTo` / assignability rules | ❌ |
| Expected-type providers per syntactic context | ❌ |
| `SMART` branch in the contributor | ❌ |

Smart Completion is **gated entirely on the type system**.

---

## 7. Class map (preview, full set lives in `./classes.md`)

| Class | Responsibility |
|---|---|
| `types/BbkType.java` | Sealed interface or abstract root for the type representation. |
| `types/BbkPrimitiveType.java` | `INT(n)`, `CHAR(n)`, `PACKED(p:d)`, `BOOL`, `DATE`, etc. |
| `types/BbkStructType.java` | DS types (with reference to declaration + subfield list). |
| `types/BbkProcedureType.java` | Signature: parameter types + return type. |
| `types/BbkTypeInferrer.java` | `typeOf(PsiNamedElement)`, `typeOf(BbkExpression)`. |
| `types/BbkAssignability.java` | `areCompatible(BbkType, BbkType)` with promotion rules. |
| `completion/expected/BbkExpectedTypeProvider.java` | Reads the PSI around the caret, returns expected `BbkType`. One implementation per syntactic context, or one provider with internal pattern dispatch. |
| `completion/providers/BbkSmartCompletionProvider.java` | The `SMART` branch of the contributor: combines expected-type, candidate-type, assignability. |
| `completion/BbkCompletionContributor.java` change | Add the `SMART` branch dispatcher. |

No `plugin.xml` change — the existing `completion.contributor` extension already covers all completion types.

---

## 8. Decisions deferred until type system is designed

| # | Topic | Question |
|---|---|---|
| 1 | Implicit numeric promotion | Allow `INT → PACKED` without `%dec()`? Reject? Suggest both as soft warnings? |
| 2 | `CHAR`/`VARCHAR` widening | Is `CHAR(20)` assignable to `CHAR(50)`? |
| 3 | DS field-by-field compatibility | Two DS with same subfields but different declared names — compatible? |
| 4 | `LIKE` vs `LIKEDS` chains | Resolve transitively (`LIKE(LIKE(x))`)? How many hops max? |
| 5 | Literals of generic type | Should `42` be `INT(*)` (assignable everywhere numeric) or a specific minimum-precision `INT(2)`? |
| 6 | Should Smart Completion offer literals | `if <caret>` suggesting `*TRUE` / `*FALSE`? |
| 7 | Should it offer builtins | `INT(10) = <caret>` suggesting `%len(x)`, `%size(x)` if they return INT? |

Each of these affects how aggressive Smart Completion looks. The conservative default is: identity match only, no promotion. The lax default is: every legal implicit conversion shows up.

---

## 9. Estimated effort

The type system is the heavy work — Smart Completion itself is a thin layer on top.

- Type representation (5 classes): **1-2 days**
- Type inference for declarations (variables, constants, DS subfields, proc returns): **1 day**
- Type inference for expressions (literals, binary, unary, call, member access): **2-3 days**
- Assignability rules + tests: **1 day**
- Expected-type providers (assignment, condition, call argument, INZ, return, for-bounds): **1-2 days**
- Smart branch in the contributor: **0.5 day**
- Integration tests (one per expected-type context): **1 day**

**Total: ~7-10 days** for a usable Smart Completion. Once paid, features #10 (inspections) and #11 (parameter info) are mostly "use the same engine, different consumer" — so the marginal cost is much smaller.

---

## 10. What is NOT covered

- Ranking by relevance within already-filtered candidates (proximity in scope, recency of use) — IntelliJ default ordering is good enough for V1
- "Second smart completion" (`Ctrl+Shift+Space` twice) — IntelliJ offers chained-call exploration; out of scope
- Auto-import of cross-file symbols — BBK does not have an `import`/`/COPY` mechanism in V1, so N/A
- Type-driven post-completion templates (e.g., after picking a DS variable, expand `.firstSubfield = `)

---

## 11. Related documents

- [`../functionalities.md`](../functionalities.md) — full feature map, all blocks
- [`../basic-autocomplete/theory.md`](../basic-autocomplete/theory.md) — the BASIC completion infrastructure this builds on
- [`../reference-scope/theory.md`](../reference-scope/theory.md) — scope walker (used to enumerate candidates)
- [`../stubs-index/theory.md`](../stubs-index/theory.md) — cross-file enumeration (also a candidate source under SMART)
- (future) `./classes.md` — concrete class set, expected-type provider catalogue, order of implementation
