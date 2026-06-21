# Architecture — BoxBreaker

**Repo:** [github.com/nlarena/rpg-bbk-compiler](https://github.com/nlarena/rpg-bbk-compiler)
**Tagline:** RPG → C
**Document date:** 2026-05-22
**Status:** initial design, no code yet

This document describes the system's module organization and the responsibilities of each one. For the BBK intermediate language, see [`bbk-spec.md`](bbk-spec.md) (pending).

---

## Summary

The system compiles and runs RPG programs (IBM i / AS/400) on Windows and Linux through a custom intermediate language called **BBK (BoxBreaker)**. It supports two execution modes: **interpreter** (fast development cycle) and **AOT to native binary** via `C` + `gcc` (production).

Editor tooling is distributed as **IntelliJ plugins** that can be installed on stock IntelliJ Community, and optionally a **customized IDE** (BoxBreaker IDE) is packaged on top of the IntelliJ Platform SDK that bundles the plugins together with the execution runtime.

---

## Compilation pipeline

```
                    ┌─────────────┐
                    │   RPG src   │
                    └──────┬──────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │  rpg-frontend   │  lexer + parser + translation
                  │  (headless lib) │  RPG → BBK
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │   BBK (IR)      │  Neutral AST/IR, types, semantics
                  │   bbk-core      │
                  └────────┬────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
    ┌──────────────────┐      ┌──────────────────┐
    │ bbk-interpreter  │      │  bbk-compiler    │
    │   (dev mode)     │      │   (AOT mode)     │
    │                  │      │  BBK → C → gcc   │
    └────────┬─────────┘      └────────┬─────────┘
             │                         │
             ▼                         ▼
    ┌─────────────────┐       ┌──────────────────┐
    │  bbk-runtime    │       │  bbk-runtime     │
    │  (linked in)    │       │  (linked in)     │
    └─────────────────┘       └──────────────────┘
             │                         │
             ▼                         ▼
       Interpreted              Native binary
        execution               (.exe, ELF, Mach-O)
```

---

## Modules

The repo is a Gradle multi-project monorepo with 9 subprojects. Three layers:

1. **Headless core** (no IntelliJ dependency): `bbk-core`, `rpg-frontend`, `bbk-interpreter`, `bbk-compiler`, `bbk-runtime`.
2. **IntelliJ plugins** (depend on IntelliJ Platform): `plugin-bbk`, `plugin-rpg`.
3. **IDE distribution**: `boxbreaker-ide`.

### `bbk-core/` — core library

BBK language types and data structures:

- BBK AST
- Type model
- Semantic analysis
- Common utilities (visitors, transformers, pretty-printer)

**No IntelliJ Platform dependencies.** It's the piece shared by every other module.

**Language:** Java or Kotlin (TBD).

### `rpg-frontend/` — RPG translation library

RPG lexer and parser, plus the RPG → BBK translation logic:

- RPG lexer (handling fixed columns, indicators, opcodes)
- RPG parser → RPG AST
- Lowering: RPG AST → BBK AST

**No IntelliJ Platform dependencies.** Designed to be used headless from CLI, CI, or invoked by `plugin-rpg`.

**Language:** Java + ANTLR4.

### `bbk-interpreter/` — BBK interpreter (development mode)

Runs BBK directly without compiling to native. Intended for fast iteration and debugging:

- Evaluates the BBK AST
- Integrates `bbk-runtime` to resolve IBM i primitives
- Exposes an API for debugger integration

**Language:** Java or Kotlin.

> **Note:** in the current code the tree-walking interpreter lives in `bbk-debugger/` and now backs
> only the static **"Trace"** action. Interactive debugging moved to `bbk-jvm-debug/` (below).

### `bbk-jvm-debug/` — debugger over the real bytecode (JDI/JDWP)

UI-agnostic library that debugs the **real JVM bytecode** emitted by `bbk-core` — not a separate
execution engine. Uses the standard Java debug mechanism (the same JDI/JDWP that Java, Kotlin and
Scala use in IntelliJ):

- Compiles BBK to `bbk/Main.class` on disk **with debug info** (LineNumberTable + `SourceFile` +
  LocalVariableTable)
- Forks a JVM with the JDWP agent (`suspend=y`) and attaches via JDI (`com.sun.jdi`, no `--add-modules`)
- Maps JDI `Location` ↔ BBK line/file; installs breakpoints (with conditions), drives **native**
  step over/into/out, reads frames + variables, evaluates expressions
- Reads variables **lazily** (off the JDI event-loop thread, to avoid hanging the session)

The plugin's XDebugger (`plugin-bbk`) orchestrates it; the engine is headless and unit-tested.

**Depends on** `bbk-core` (+ `jdk.jdi` from the JDK). **Language:** Java.

### `bbk-compiler/` — BBK → C → native AOT compiler (production mode)

Takes BBK and generates a native binary:

- BBK → C99 lowering
- Invokes `gcc` to produce the binary
- Links against `bbk-runtime`
- Main compiler CLI

**Language:** Java or Kotlin for the lowering; emits C99.

### `bbk-runtime/` — IBM i emulation runtime

Implementation of IBM i primitives absent from Windows/Linux:

- Job queues, activation groups, library lists
- DDS-style data access
- Spool files
- IBM i-specific I/O

Used by:
- `bbk-interpreter` (direct linking)
- Binaries produced by `bbk-compiler` (native linking)

**Language:** TBD between Java (simpler, requires JVM at runtime) and C (direct binary linking, no JVM).

### `plugin-bbk/` — IntelliJ plugin for BBK

BBK language support in IntelliJ:

- Syntax highlighting
- Editor parser (inline errors)
- Autocomplete
- Navigation (go-to-definition, find usages)
- Basic refactors

**Depends on** `bbk-core` + IntelliJ Platform SDK. **Does not contain the interpreter or AOT compiler** — those live outside and are invoked by the IDE.

**Language:** Kotlin.

### `plugin-rpg/` — IntelliJ plugin for RPG

RPG language support in IntelliJ + integration with the BBK translation:

- RPG syntax highlighting
- RPG editor parser (via `rpg-frontend`)
- "Translate to BBK" command
- Navigation between RPG and generated BBK

**Depends on** `rpg-frontend` + `plugin-bbk` + IntelliJ Platform SDK.

**Language:** Kotlin.

### `boxbreaker-ide/` — integration module (custom IDE on top of IntelliJ Platform SDK)

This is the **continuous integration point** of the system. It exists from iteration 1 as a minimal IntelliJ Platform shell that bundles the first plugin, and grows as the rest of the system appears. It is not an "end-stage" module — it is touched throughout development.

What grows in this module iteration by iteration:

- **Start:** minimal IntelliJ Platform SDK configuration (custom IDE type, platform version, internal plugin code/version), no branding or cosmetic assets. Bundles `plugin-bbk`.
- **When `plugin-rpg` appears:** added to the bundling.
- **When `bbk-interpreter` appears:** a "Run interpreted" action is wired in to invoke the interpreter.
- **When `bbk-runtime` appears:** included in the classpath so the interpreter can resolve primitives.
- **When `bbk-compiler` appears:** a "Compile to native" action is wired in.
- **Throughout development:** file type associations (`.rpg`, `.bbk`), tool windows (runtime console, compilation log), run configurations, settings UI, debugger integration.
- **Much later (distribution phase):** branding (commercial name, splash, icon), build/sign/installer.

It is the last thing to receive branding but the first thing to exist as a runnable shell. The development harness for the whole project is `./gradlew :boxbreaker-ide:runIde`.

**Depends on** every plugin and runtime module. Uses `gradle-intellij-plugin` with custom IDE configuration.

**Language:** Kotlin for configuration; mostly Gradle + resources.

---

## Design properties

**Headless-first.** The compiler core (`bbk-core`, `rpg-frontend`, `bbk-compiler`, `bbk-interpreter`, `bbk-runtime`) does not depend on IntelliJ. RPG can be compiled and run from the CLI, CI/CD, or batch scripts without installing any IDE.

**Plugins reusable on IntelliJ Community.** The `plugin-bbk` and `plugin-rpg` plugins do not depend on the custom IDE. They can be published on the JetBrains Marketplace and installed on stock IntelliJ Community, where they provide editor tooling (but not execution — that requires BoxBreaker IDE or the CLI).

**The custom IDE is the continuous integration point.** `boxbreaker-ide` exists from iteration 1 as a minimal IntelliJ Platform shell bundling the first plugin. As plugins, runtime modules and execution entry points appear, they are sewn into this module. The development loop for the entire project is `runIde` over `boxbreaker-ide`, which guarantees that the bundling contract is validated from day one (no late integration surprises). Branding/distribution is the last layer added on top, not the first.

**Two execution modes sharing the IR.** Interpreter and AOT compiler consume the same `bbk-core` and the same `bbk-runtime`. The difference is how they walk the AST/IR — interpreting it or lowering it to C.

---

## Module dependencies

```
boxbreaker-ide
    ├── plugin-bbk
    │   ├── bbk-core
    │   ├── bbk-jvm-debug   (→ bbk-core)   # interactive debugger (JDI)
    │   └── bbk-debugger    (→ bbk-core)   # interpreter, only for "Trace"
    ├── plugin-rpg
    │   ├── rpg-frontend
    │   │   └── bbk-core
    │   └── plugin-bbk
    ├── bbk-interpreter
    │   ├── bbk-core
    │   └── bbk-runtime
    ├── bbk-compiler
    │   └── bbk-core
    └── bbk-runtime
```

Rules:
- `bbk-core` depends on no one.
- `bbk-runtime` depends on no one (or only on `bbk-core` for shared types).
- Plugins depend on `bbk-core` / `rpg-frontend` but **not** on the execution modules.
- `boxbreaker-ide` is the only module that knows them all.

---

## Pending decisions

- **Repo name** (still open in `compilador-rpg-bbk-setup.md`).
- **`bbk-runtime` language**: Java (simpler, JVM-dependent) vs C (native linking, no JVM at runtime).
- **`bbk-core` language**: Java or Kotlin. If all plugins are Kotlin and most of the compiler too, Kotlin wins on consistency.
- **E2E test strategy**: how to validate that the interpreter and the AOT binary produce the same output for the same RPG.
