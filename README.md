# BoxBreaker

> **RPG → C.** Toolchain to compile and run RPG programs (IBM i / AS/400) on Windows and Linux through a custom intermediate language called **BBK (BoxBreaker)**.

**Repo:** [github.com/nlarena/rpg-bbk-compiler](https://github.com/nlarena/rpg-bbk-compiler)
**Status:** alpha — initial design, active development.

---

## Why

RPG codebases are tied to IBM i hardware for development, build, and testing. This toolchain decouples the development cycle from IBM hardware, allowing local iteration on Windows or Linux while preserving RPG semantics through BBK and a runtime layer that emulates the missing primitives.

---

## Pipeline

```
RPG  ──▶  rpg-frontend  ──▶  BBK (IR)  ──┬──▶  interpreter  ──▶  execution
                                         │
                                         └──▶  C  ──▶  gcc  ──▶  native binary
```

Two execution modes over the same IR:

- **BBK interpreter** — fast cycle for development and debugging.
- **AOT to native** — BBK → C → gcc → binary for production.

Both modes share the IBM i emulation runtime.

---

## Modules

The repo is a Gradle multi-project monorepo. See [`docs/architecture.md`](docs/architecture.md) for full detail.

| Module | Type | Role |
|---|---|---|
| `bbk-core` | lib | BBK AST, IR, and semantics |
| `rpg-frontend` | lib | RPG lexer + parser, RPG → BBK translation |
| `bbk-interpreter` | app | BBK interpreter (dev mode) |
| `bbk-compiler` | app | BBK → C lowering + gcc invocation (AOT mode) |
| `bbk-runtime` | lib | IBM i primitives emulation (job queues, DDS, library lists, etc.) |
| `plugin-bbk` | plugin | BBK language support in IntelliJ |
| `plugin-rpg` | plugin | RPG language support + "translate to BBK" command |
| `boxbreaker-ide` | IDE | Customized IDE distribution on top of IntelliJ Platform SDK |

**Layers:**

- **Headless core** (`bbk-core`, `rpg-frontend`, `bbk-interpreter`, `bbk-compiler`, `bbk-runtime`) — no IntelliJ dependency. Compile and run from CLI or CI without installing any IDE.
- **IntelliJ plugins** (`plugin-bbk`, `plugin-rpg`) — installable on stock IntelliJ Community. Provide editor tooling (highlighting, autocomplete, navigation) without the runtime.
- **BoxBreaker IDE** (`boxbreaker-ide`) — full distribution: plugins + runtime + branding. The product's "shrinkwrap".

---

## Quick start

> The build is not yet implemented. This section is a projection of the final UX.

```bash
# Build the entire monorepo
./gradlew build

# Run an RPG in interpreter mode (fast)
./gradlew :bbk-interpreter:run --args="examples/hello-world/hello.rpg"

# Compile an RPG to a native binary (AOT)
./gradlew :bbk-compiler:run --args="examples/hello-world/hello.rpg --output dist/hello.exe"
./dist/hello.exe

# Launch BoxBreaker IDE in development mode
./gradlew :boxbreaker-ide:runIde
```

---

## Architecture

Main document: [`docs/architecture.md`](docs/architecture.md).

BBK language spec: `docs/bbk-spec.md` (pending).

---

## Stack

- **Java + Kotlin** — compiler core and plugins
- **Gradle (Kotlin DSL) multi-project** — build
- **ANTLR4** — RPG grammar
- **IntelliJ Platform SDK** + `gradle-intellij-plugin` — plugins and IDE
- **C99 + gcc** — AOT compiler backend
- **JUnit 5** — tests

---

## Status and roadmap

Work in progress, pending items, and long-term roadmap in [`TODO.md`](TODO.md).

---

## License

MIT — see [`LICENSE`](LICENSE).
