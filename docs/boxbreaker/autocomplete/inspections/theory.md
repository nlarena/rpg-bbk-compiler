# Type-Aware Inspections — Theory

**Status:** not implemented
**Scope:** the IntelliJ Platform concepts behind feature #10 (Type-aware inspections) of [`../functionalities.md`](../functionalities.md)
**Plugin module:** `plugin-bbk/`
**Prerequisites:** References + Scope (#5–#7), Smart Completion type system (#9). The motor de tipos (`types/BbkTypeInferrer` + `types/BbkAssignability`) is the foundation — this feature is its visual consumer.

---

## 1. What it is

An **inspection** in IntelliJ is code that walks each file's PSI tree and reports problems — those red/yellow/grey wavy underlines in the margin, with explanatory tooltip and optional quick-fix. They do not alter program behaviour, only inform.

**Type-aware** means the inspection uses the type engine (the one built for Smart Completion) to detect semantic errors, not syntactic ones. The parser already rejects `DCL-S 1foo INT(10);` (invalid syntax). An inspection rejects `counter = 'hola';` (valid syntax, invalid semantics).

Example:

```bbk
DCL-S counter INT(10);
DCL-S name CHAR(50);

counter = name;
//        ~~~~  ← INT(10) cannot be assigned from CHAR(50)
```

---

## 2. The syntactic → semantic gradient

There are four levels of checks. Each one needs more infrastructure than the last:

| Level | Check | Who does it in BBK |
|---|---|---|
| **Lexical** | `123abc` is not a valid token | JFlex lexer |
| **Syntactic** | `DCL-S INT(10);` (missing name) | Grammar-Kit parser |
| **Resolution** | `counter = unknownVar;` (unknownVar not declared) | `BbkIdentReference.resolve()` + annotator / inspection |
| **Typed** | `counter = 'hola';` (assigning CHAR to INT) | Inspection that calls `BbkTypeInferrer` + `BbkAssignability` |

Smart Completion already built the last two layers. Type-aware inspections are **the visual consumer** of that work.

---

## 3. Inspection vs Annotator — two APIs

IntelliJ has two ways to paint squigglies; the difference matters for choosing:

### `Annotator`
- Runs **synchronously** during highlighting, per file.
- For each `PsiElement`, decides whether it has a problem and adds an annotation.
- Cannot be configured from Settings → Editor → Inspections.
- No native quick-fix UI (can be added manually).
- Ideal for mandatory language rules (resolution, post-parse syntax).

### `LocalInspectionTool`
- Runs through the inspections infrastructure.
- **Configurable**: appears in Settings → Inspections, the user can enable/disable, change severity (Error / Warning / Weak Warning / Info), suppress with a comment.
- Supports quick-fixes (`LocalQuickFix`) with a consistent UI.
- Ideal for opinionated or quality rules.

**For type checking, `LocalInspectionTool` is the natural fit** — the user may want to relax it during prototyping, or raise it to Error in CI.

---

## 4. Anatomy of an inspection

A typical `LocalInspectionTool` looks like:

```java
public class BbkTypeMismatchInspection extends LocalInspectionTool {

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new BbkVisitor() {
            @Override
            public void visitExpressionStatement(BbkExpressionStatement s) {
                if (isAssignment(s)) {
                    BbkType lhs = typeOf(s.lhs());
                    BbkType rhs = typeOf(s.rhs());
                    if (!rhs.isAssignableTo(lhs)) {
                        holder.registerProblem(
                            s.rhs(),                                      // where to paint
                            "Type mismatch: " + rhs + " not assignable to " + lhs,
                            ProblemHighlightType.GENERIC_ERROR,
                            new BbkChangeTypeFix(lhs)                     // optional quick-fix
                        );
                    }
                }
            }
        };
    }
}
```

Three pieces:

1. **`PsiElementVisitor`** — IntelliJ walks the file and hands each PSI node to the visitor. The visitor decides which nodes are interesting (assignments, calls, returns, INZ, conditions).
2. **The check** — `BbkTypeInferrer.typeOf(lhs)`, `BbkTypeInferrer.typeOf(rhs)`, `BbkAssignability.areCompatible(rhs, lhs)`.
3. **`ProblemsHolder.registerProblem(...)`** — registers the problem with text, severity, range (which PsiElement to underline), and optionally one or more `LocalQuickFix`.

---

## 5. Checks worth implementing for BBK

Each one is a separate inspection (better coverage, better configurability, better error reporting). The obvious ones:

| Inspection | Detects | Suggested severity |
|---|---|---|
| **Assignment type mismatch** | `counter = 'hola'` | Error |
| **Return type mismatch** | inside `DCL-PROC -> INT(10)`, `return 'x';` | Error |
| **Call argument type mismatch** | `f('hola')` where `f(n INT(10))` | Error |
| **Call argument count mismatch** | `f(a, b)` where `f` expects 1 | Error |
| **Condition not BOOL** | `if (counter)` where `counter` is INT | Warning (depends on whether implicit conversion is allowed) |
| **INZ value type mismatch** | `DCL-S x INT(10) INZ('a')` | Error |
| **Unresolved reference** | `counter = unknown;` (unknown not declared) | Error |
| **Unused variable / parameter** | `DCL-S x INT(10);` never read | Weak Warning |
| **Shadowed variable** | local with same name as module-level | Weak Warning |
| **Reserved-word identifier** | `DCL-S IF INT(10);` (syntactically valid only if not blocked elsewhere — the initial declaration does not go through `BbkRenameInputValidator`) | Warning |
| **LIKE chain cycle** | `a LIKE(b); b LIKE(a);` | Error |

The **first 7 are the pillars** — the rest is quality-of-life.

---

## 6. On-the-fly vs Batch

IntelliJ runs inspections in two modes:

- **On-the-fly**: while typing. Only the current file. Must be **fast** (< a few ms per file). The `buildVisitor` method receives the `isOnTheFly` parameter so the inspection can skip expensive analysis in this mode.
- **Batch** (Code → Inspect Code): the user triggers it explicitly over the project. Can take minutes. More expensive, more thorough.

For BBK V1 everything fits in on-the-fly — inference is local to the file (with one resolve per cross-file reference, already cached via `ResolveCache`).

---

## 7. Severities and philosophy

IntelliJ has 5 default severities:

| Severity | Colour | When |
|---|---|---|
| `ERROR` | red | Program does not compile / runtime failure guaranteed |
| `WARNING` | yellow | Probable bug, but the code runs |
| `WEAK_WARNING` | faint green | Code smell, improvable |
| `INFO` | grey | Suggestion |
| `TYPO` | green corrugated | Spelling |

Key decision: **is type mismatch in an assignment an ERROR or a WARNING?** Arguments:

- **ERROR**: in serious compilers (Java, Rust, TypeScript) a type mismatch is a compilation error; the program would not compile → coherent for IntelliJ to mark it red.
- **WARNING**: BBK does not yet have a production-ready compiler; during development everything in red is heavy. Better yellow, and let the user raise it to Error in CI.

Recommendation for V1: **WARNING** for everything type-related, leaving the user to escalate in Settings → Inspections. The exception is Unresolved Reference, which should be ERROR (the code is broken).

---

## 8. Quick-fixes

An inspection without a quick-fix is just a message. With a quick-fix it becomes a real productivity gain.

For type mismatch, the natural quick-fixes are:

1. **Change variable type to match RHS** — `counter = 'hola'` → "Change `counter` to CHAR(*)"
2. **Wrap RHS in conversion BIF** — `counter = name` → "Wrap with `%int(name)`"
3. **Cast literal** — `counter = '5'` → "Change `'5'` to `5`"

Each quick-fix is a `LocalQuickFix` that receives the offending `PsiElement` and mutates it (using the `BbkElementFactory` already in place from Rename).

For V1 it makes sense to **start without quick-fixes** — just the report. Add quick-fixes later, one at a time, as users ask.

---

## 9. Suppression

IntelliJ provides a standard mechanism for the user to suppress an inspection on a specific node, with a comment:

```bbk
// noinspection BbkTypeMismatch
counter = name;  // no warning here
```

Implementing it is trivial: `LocalInspectionTool` has a `getSuppressId()` method that returns the string that goes in the comment. IntelliJ handles the rest.

---

## 10. plugin.xml — registration

Each inspection registers as:

```xml
<localInspection
    language="BBK"
    implementationClass="com.larena.boxbreaker.plugin.bbk.inspection.BbkTypeMismatchInspection"
    displayName="Type mismatch"
    groupName="BBK"
    enabledByDefault="true"
    level="WARNING"/>
```

Plus a resource at `inspectionDescriptions/BbkTypeMismatch.html` with the long description that appears in Settings → Inspections when the user clicks the inspection.

---

## 11. What it is NOT

- **Not whole-program type inference** — still local. Detects "X is not assignable to Y", does not infer generic types nor resolve complex overloading (BBK has none).
- **Not a type checker for CI** — IntelliJ does not run in CI. For CI a standalone tool reusing the engine would be needed. Possible but out of plugin scope.
- **Not runtime checking** — badly typed code still gets generated (if it passes the parser). Inspections only warn in the editor.

---

## 12. Implementation cost

Once the type engine is in place (already done):

- **Type system reuse**: 0 (already exists)
- **Visitor infrastructure**: ~30 LOC shared by all inspections
- **Each inspection**: ~50-80 LOC
- **`inspectionDescriptions/*.html` per inspection**: 5-10 lines
- **plugin.xml registration**: 5 lines per inspection
- **Test per inspection**: ~30-50 LOC using `myFixture.enableInspections(...)` + `myFixture.checkHighlighting()`

For the 7 pillar inspections: **~3-4 days total**. For all 11 inspections: ~5-7 days.

---

## 13. The hard part is not the code

Inspections are mechanically simple once the type engine exists. The hard parts are:

1. **Choosing severity for each one** (Error vs Warning vs WeakWarning).
2. **Avoiding false positives** — `BbkUnknownType` must inhibit the report (if you cannot infer, you cannot claim it is wrong). This is already in `BbkAssignability.areCompatible`, which returns true when either side is Unknown.
3. **Useful error messages** — "Type mismatch" is useless. "Cannot assign `CHAR(50)` to `INT(10)`" is informative. "Cannot assign `name (CHAR(50))` to `counter (INT(10))`" is excellent.
4. **Not nagging in WIP code** — while the user types, the file is broken temporarily. Suppress reports when the AST is obviously incomplete (count of `BbkUnknownType` on either side already covers most of this).

---

## 14. Related documents

- [`../functionalities.md`](../functionalities.md) — full feature map, all blocks
- [`../smart-completion/theory.md`](../smart-completion/theory.md) — the type engine, foundation of every inspection here
- [`../reference-scope/theory.md`](../reference-scope/theory.md) — `BbkIdentReference.resolve()` is what powers the Unresolved Reference inspection
- [`../rename/theory.md`](../rename/theory.md) — `BbkElementFactory` is what quick-fixes will use to mutate PSI
- (future) `./classes.md` — concrete class set, per-inspection details, order of implementation
