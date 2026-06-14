# Type-Aware Inspections — Classes

**Status:** not implemented — design only
**Scope:** every class to create and every plugin.xml entry to register so the BBK plugin reports type errors inline as the user types
**Companion:** [`theory.md`](theory.md)
**Prerequisites:** Smart Completion (#9) landed — the type system (`types/`) is the foundation reused here. Nothing else in the plugin changes.

---

## 1. Headcount summary

| Category | Count | Approx LOC |
|---|---|---|
| Shared new infrastructure | 2 | ~80 |
| Pillar inspections (V1) | 7 | ~400 |
| Extra inspections (V1.5) | 4 | ~250 |
| Quick-fixes (deferred to V2) | 3 | ~150 |
| HTML description per inspection | 11 files | ~10 each |
| `plugin.xml` `<localInspection>` entries | 11 | 5 each |
| Tests | 11 | ~50 each |

**Zero new engine** — everything reuses `BbkTypeInferrer`, `BbkAssignability`, `BbkScopeWalker`, and (for V2 quick-fixes) `BbkElementFactory`, all of which already exist.

---

## 2. Shared infrastructure (`inspection/`)

### 2.1 Abstract base

| Class | Responsibility |
|---|---|
| `inspection/BbkInspectionBase.java` | Abstract `LocalInspectionTool`. Overrides `getGroupDisplayName()` → `"BBK"`, provides a localized message helper, and a helper that returns true when the surrounding PSI looks too broken to bother (lots of `BbkUnknownType` in the chain) — used by concrete inspections to skip reports in WIP code. Each concrete inspection extends it. ~30 LOC. |

### 2.2 Visitor base

| Class | Responsibility |
|---|---|
| `inspection/BbkInspectionVisitor.java` | Abstract `PsiElementVisitor` exposing semantic hooks: `visitAssignment(BbkExpressionStatement)`, `visitCall(BbkPostfixSuffix, BbkPostfixExpression)`, `visitReturn(BbkReturnStatement)`, `visitCondition(PsiElement enclosing, BbkExpression cond)`, `visitInzModifier(BbkInzModifier)`. The dispatch from `visitElement(PsiElement)` does the syntactic recognition once (assignment_op presence, argument list presence, etc.). Each inspection overrides only the hooks it cares about. ~50 LOC. |

**Why a custom visitor base**: without it, every inspection would duplicate the same `instanceof BbkExpressionStatement && hasAssignmentOp(...)` pattern. Centralizing avoids 7× duplication and gives a stable semantic surface that survives BNF refactors.

---

## 3. Pillar inspections — V1 (7 classes)

Each one in `inspection/`, extends `BbkInspectionBase`, implements `buildVisitor` returning a `BbkInspectionVisitor` with the relevant hooks overridden. Each typically ~50–80 LOC because the pattern is: override hook → 2× `BbkTypeInferrer.typeOf(...)` → 1× `BbkAssignability.areCompatible(...)` → `registerProblem(...)` with a formatted message.

| Class | Detects | Severity |
|---|---|---|
| `BbkAssignmentTypeMismatchInspection` | `LHS = RHS` where `typeOf(RHS).isAssignableTo(typeOf(LHS))` is false | WARNING |
| `BbkReturnTypeMismatchInspection` | `return EXPR;` whose EXPR does not fit the enclosing proc's return type | WARNING |
| `BbkCallArgumentTypeMismatchInspection` | `f(arg)` where `arg` does not fit the positional parameter's type | WARNING |
| `BbkCallArgumentCountMismatchInspection` | `f(a, b)` where `f` expects 1 or 3 | WARNING |
| `BbkConditionNotBoolInspection` | `if / while / when / for` test whose type is not BOOL | WARNING |
| `BbkInzValueTypeMismatchInspection` | `DCL-S x T INZ(value)` where `value` does not fit `T` | WARNING |
| `BbkUnresolvedReferenceInspection` | IDENT use whose `reference.resolve()` returns null | ERROR |

---

## 4. Extra inspections — V1.5 (4 classes)

| Class | Detects | Severity |
|---|---|---|
| `BbkUnusedDeclarationInspection` | `DCL-S / DCL-C / inline param / DCL-PROC` whose `ReferencesSearch.search(decl)` returns 0 hits | WEAK_WARNING |
| `BbkShadowedDeclarationInspection` | Procedure-local with the same name as a module-level declaration | WEAK_WARNING |
| `BbkReservedWordIdentifierInspection` | `DCL-S` whose IDENT is a keyword (`IF`, `VALUE`, ...) — reuses the existing `refactoring/BbkReservedWords` | WARNING |
| `BbkLikeCycleInspection` | `LIKE` / `LIKEDS` chain that loops back (`a LIKE(b); b LIKE(a);`) | ERROR |

---

## 5. Quick-fixes — deferred to V2 (3 classes)

Each implements `LocalQuickFix`, receives the offending `PsiElement`, mutates it using `BbkElementFactory` (already in place from the Rename work).

| Class | Quick-fix | Offered by |
|---|---|---|
| `inspection/fixes/BbkChangeVariableTypeFix.java` | `counter = 'hola'` → "Change `counter` to CHAR(*)" | `BbkAssignmentTypeMismatchInspection` |
| `inspection/fixes/BbkWrapWithConversionBifFix.java` | `counter = name` → "Wrap with `%int(name)`" | `BbkAssignmentTypeMismatchInspection`, `BbkCallArgumentTypeMismatchInspection` |
| `inspection/fixes/BbkChangeLiteralKindFix.java` | `counter = '5'` → "Change `'5'` to `5`" | `BbkAssignmentTypeMismatchInspection` |

**Recommendation: skip quick-fixes for V1.** Inspections without a fix are still useful (they flag the problem); fixes are productivity sugar that add test surface. Add them after users ask.

---

## 6. HTML resources (`resources/inspectionDescriptions/`)

One per inspection, same name as the class without the `Inspection` suffix:

```
plugin-bbk/src/main/resources/inspectionDescriptions/
├── BbkAssignmentTypeMismatch.html
├── BbkReturnTypeMismatch.html
├── BbkCallArgumentTypeMismatch.html
├── BbkCallArgumentCountMismatch.html
├── BbkConditionNotBool.html
├── BbkInzValueTypeMismatch.html
├── BbkUnresolvedReference.html
├── BbkUnusedDeclaration.html
├── BbkShadowedDeclaration.html
├── BbkReservedWordIdentifier.html
└── BbkLikeCycle.html
```

Each file is 5–15 lines of HTML with:
- Short description of what it detects
- A "Bad" and "Good" example
- Notes on when to disable it

Shown in Settings → Inspections when the user clicks the inspection in the list.

---

## 7. Connection point with the IntelliJ Platform — `plugin.xml`

Single extension point: **`com.intellij.localInspection`**, registered once per inspection.

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- ... existing extensions ... -->

    <!-- Type-aware inspections (feature #10) -->
    <localInspection
        language="BBK"
        shortName="BbkAssignmentTypeMismatch"
        displayName="Type mismatch in assignment"
        groupName="BBK"
        enabledByDefault="true"
        level="WARNING"
        implementationClass="com.larena.boxbreaker.plugin.bbk.inspection.BbkAssignmentTypeMismatchInspection"/>

    <localInspection
        language="BBK"
        shortName="BbkReturnTypeMismatch"
        displayName="Type mismatch in return"
        groupName="BBK"
        enabledByDefault="true"
        level="WARNING"
        implementationClass="com.larena.boxbreaker.plugin.bbk.inspection.BbkReturnTypeMismatchInspection"/>

    <!-- ... 9 more, same pattern ... -->
</extensions>
```

**Attributes that matter:**

| Attribute | Why it matters |
|---|---|
| `language="BBK"` | IntelliJ only invokes this inspection inside `.bbk` files. Without it, it would run on every file in the project. |
| `shortName` | Stable ID used in `// noinspection BbkAssignmentTypeMismatch` to suppress, and in the HTML description filename. |
| `displayName` | Text the user sees in Settings → Inspections → BBK. |
| `groupName="BBK"` | Groups all plugin inspections under the same node in the Settings tree. |
| `enabledByDefault` | `true` for all 11 — users who want one off can disable it manually. |
| `level` | Default severity. User can override in Settings without touching plugin code. |
| `implementationClass` | FQN of the Java class. |

---

## 8. Runtime invocation flow

```
User types in main.bbk
    ↓
Editor change → DaemonCodeAnalyzer requests highlighting
    ↓
Highlighting pass splits into:
    ├── Syntax (parser)
    ├── Annotators
    └── LocalInspectionTool ← my inspections enter here
        ↓
    For each <localInspection language="BBK"> registered:
        1. IntelliJ instantiates the class
        2. Calls inspection.buildVisitor(holder, isOnTheFly)
        3. Walks the file's PSI, hands each node to the visitor
        4. Visitor calls holder.registerProblem(...) on offending nodes
        5. IntelliJ renders each ProblemDescriptor as squiggly + tooltip
```

The plugin does NOT control the loop — it just provides the `Visitor`. IntelliJ orchestrates everything.

---

## 9. Files NOT touched

| File | Why |
|---|---|
| `BBK.bnf`, `BBK.flex` | Parser unchanged. |
| `types/*` | Engine already complete, reused as-is. |
| `scope/*`, `reference/*` | Inspections consume these, do not extend them. |
| `completion/*` | Independent feature. |
| `psi/factory/BbkElementFactory` | Only used by V2 quick-fixes (deferred). |
| `stub/*`, `index/*` | Cross-file enumeration not required — inspections are per-file. |
| Live templates, brace matcher, smart typing, rename | Unrelated. |

---

## 10. Final package layout

```
plugin-bbk/src/main/java/com/larena/boxbreaker/plugin/bbk/inspection/
├── BbkInspectionBase.java                              (shared)
├── BbkInspectionVisitor.java                           (shared)
├── BbkAssignmentTypeMismatchInspection.java            (V1 pillar)
├── BbkReturnTypeMismatchInspection.java                (V1 pillar)
├── BbkCallArgumentTypeMismatchInspection.java          (V1 pillar)
├── BbkCallArgumentCountMismatchInspection.java         (V1 pillar)
├── BbkConditionNotBoolInspection.java                  (V1 pillar)
├── BbkInzValueTypeMismatchInspection.java              (V1 pillar)
├── BbkUnresolvedReferenceInspection.java               (V1 pillar)
├── BbkUnusedDeclarationInspection.java                 (V1.5)
├── BbkShadowedDeclarationInspection.java               (V1.5)
├── BbkReservedWordIdentifierInspection.java            (V1.5)
├── BbkLikeCycleInspection.java                         (V1.5)
└── fixes/                                              (V2 — deferred)
    ├── BbkChangeVariableTypeFix.java
    ├── BbkWrapWithConversionBifFix.java
    └── BbkChangeLiteralKindFix.java

plugin-bbk/src/main/resources/inspectionDescriptions/
├── BbkAssignmentTypeMismatch.html
├── BbkReturnTypeMismatch.html
├── ... (11 files total)
└── BbkLikeCycle.html
```

---

## 11. Order of implementation

Each step is an independent verifiable commit. Each step's tests stay green after later steps land (every inspection is independent → regression for free).

1. **`BbkInspectionBase` + `BbkInspectionVisitor`** — shared infrastructure + 1 test that validates the visitor dispatch on synthetic PSI.
2. **`BbkUnresolvedReferenceInspection`** — first concrete, the simplest (only calls `resolve()`), establishes the full pattern (class + HTML + plugin.xml + test).
3. **`BbkAssignmentTypeMismatchInspection`** — the most visible inspection; validates the type engine works end-to-end via inspection.
4. **`BbkConditionNotBoolInspection`** — second iteration of the pattern.
5. **`BbkReturnTypeMismatchInspection`** — uses `BbkProcedureType.getReturnType()`.
6. **`BbkCallArgumentTypeMismatchInspection` + `BbkCallArgumentCountMismatchInspection`** — share setup (locate callee, get param list).
7. **`BbkInzValueTypeMismatchInspection`** — completes the 7 pillars.
8. **V1.5**: Unused, Shadowed, ReservedWord, LikeCycle.
9. **V2**: quick-fixes one by one.

---

## 12. Testing pattern

IntelliJ provides specific fixtures for inspections. The standard form for every inspection test:

```java
public class BbkAssignmentTypeMismatchInspectionTest extends BasePlatformTestCase {

    public void testIntCannotAcceptChar() {
        myFixture.enableInspections(BbkAssignmentTypeMismatchInspection.class);
        myFixture.configureByText("main.bbk",
            "DCL-PROC main {\n" +
            "  DCL-S counter INT(10);\n" +
            "  DCL-S name CHAR(50);\n" +
            "  counter = <warning descr=\"Cannot assign CHAR(50) to INT(10)\">name</warning>;\n" +
            "}\n");
        myFixture.checkHighlighting();
    }
}
```

The `<warning descr="...">...</warning>` annotations in the source string are IntelliJ's standard way of saying "I expect this inspection to mark this text with this message." `checkHighlighting()` validates the actual result against the annotations.

Variants: `<error descr="...">` for ERROR-level, `<weak_warning descr="...">` for WEAK_WARNING.

---

## 13. Open decisions

| # | Topic | Question |
|---|---|---|
| 1 | Severity of "type mismatch" | ERROR (compilers reject) or WARNING (BBK has no production compiler yet)? `theory.md` §7 recommends WARNING for V1. |
| 2 | Implicit conversions in conditions | Treat `if (counter)` (INT) as error? Allow as warning? See theory §5. |
| 3 | Suppression scheme | Use IntelliJ default (`// noinspection BbkXxx`) or BBK-specific syntax (`/* @suppress */`)? Default is recommended. |
| 4 | Unused declaration scope | Only flag locals, or also module-level exported procs? Locals safe; module-level requires cross-file analysis (we have stub indexes, so feasible). |
| 5 | Quick-fix UX | Single offered fix or multiple alternatives? V1 recommendation: no fixes; V2 starts with single fix per inspection. |

---

## 14. What is NOT covered

- Whole-program type inference (still local).
- Standalone CI tool reusing the engine (out of plugin scope).
- Runtime type checking — the inspections only mark the editor.
- Cross-file flow analysis (e.g., "this proc is called with wrong type from another file") — possible via `ReferencesSearch.search(declaration)`, but expensive and deferred.

---

## 15. Related documents

- [`theory.md`](theory.md) — theory and motivation
- [`../functionalities.md`](../functionalities.md) — full feature map
- [`../smart-completion/theory.md`](../smart-completion/theory.md) — the type engine, foundation
- [`../smart-completion/classes.md`](../smart-completion/classes.md) — classes already in `types/` reused here
- [`../reference-scope/theory.md`](../reference-scope/theory.md) — `BbkIdentReference.resolve()` powers Unresolved Reference
- [`../rename/classes.md`](../rename/classes.md) — `BbkElementFactory` reused by V2 quick-fixes
