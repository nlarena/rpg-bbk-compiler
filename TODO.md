# TODO — BoxBreaker

In-progress and pending work list. Live document, updated as development advances.

For the architectural context of each item see [`docs/architecture.md`](docs/architecture.md).

---

## Done

- [x] Initial architecture documented (`docs/architecture.md`)
- [x] Monorepo module structure (9 subprojects)
- [x] README + `.gitignore` + `LICENSE`
- [x] Public repo created on GitHub: `NicLarUniversidad/rpg-bbk-compiler`
- [x] **`plugin-bbk` — complete (12/12 IDE features, 72 tests green)**
  - [x] Syntax highlighting + lexer/parser (BBK.bnf via Grammar-Kit)
  - [x] Basic + identifier + member autocomplete
  - [x] Live templates (3 contexts)
  - [x] Go to declaration, find usages (intra + cross-file)
  - [x] Rename refactor (+ reserved-word validator)
  - [x] Cross-file symbol search (stubs + 7 indexes)
  - [x] Smart completion (type-aware, `Ctrl+Shift+Space`)
  - [x] Type-aware inspections (7 inspections)
  - [x] Parameter info hints (`Ctrl+P`)
  - [x] Quick documentation (`Ctrl+Q`) + builtins catalog
  - [x] Type system (`types/`: inference + assignability)

## In progress

- [ ] **`rpg-frontend` — RPG (free-format RPGLE) → BBK text** (see [`docs/rpg-frontend/overview.md`](docs/rpg-frontend/overview.md))
  - [x] Module setup (Gradle, registered in settings)
  - [x] RPG lexer — complete free-form lexical surface (grounded in `rpgle-grammar.md §2`)
  - [x] RPG AST — complete (sealed interfaces + records: §4.2/4.3/4.4)
  - [x] RPG parser — complete free-form grammar (all declarations + statements + expressions; `=` disambiguation)
  - [x] BBK emitter + `RpgToBbk` facade — translates the full surface to BBK text
  - [x] 63 tests green
  - [ ] Validate the loop: confirm generated BBK parses + type-checks via `plugin-bbk`
  - [ ] (deferred, not in grammar doc) compound assignment `+= -= *= /=`

## Next

- [ ] Initial BBK spec (`docs/bbk-spec.md`) — define grammar, types, minimal opcodes
- [ ] Decide language for `bbk-core` (Java vs Kotlin)
- [ ] Decide language for `bbk-runtime` (Java vs C)

## Backlog — compiler core

- [ ] BBK AST/IR in `bbk-core`
- [ ] BBK semantic analysis
- [ ] BBK → C lowering (core opcodes)
- [ ] `gcc` invocation from `bbk-compiler`
- [ ] BBK interpreter (dev mode)

## Backlog — runtime

- [ ] Job queues emulation
- [ ] Activation groups emulation
- [ ] Library lists emulation
- [ ] DDS-style data access
- [ ] Spool files

## Backlog — IntelliJ tooling

- [ ] `plugin-rpg`: syntax highlighting
- [ ] `plugin-rpg`: editor parser
- [ ] `plugin-rpg`: "translate to BBK" command (invokes `rpg-frontend`)

## Backlog — IDE

- [ ] `boxbreaker-ide`: initial bundle on IntelliJ Platform SDK
- [ ] `boxbreaker-ide`: entry points integration for interpreter and AOT
- [ ] `boxbreaker-ide`: branding (splash, icon, name)
- [ ] `boxbreaker-ide`: distribution build (installer)

## Backlog — testing

- [ ] E2E suite: compile RPG → run → verify output
- [ ] Interpreter vs AOT equivalence (same RPG, same output)

## Backlog — hygiene / tech debt

- [ ] Remove diagnostic logs from `plugin-bbk` (`BBK-REF` / `BBK-RESOLVE` / `BBK-SCOPE`)
- [ ] Unify builtins: `BbkBifProvider` still has a hardcoded list parallel to `BbkBuiltinRegistry`
- [ ] Manual GUI verification of smart completion / inspections / `Ctrl+P` / `Ctrl+Q`
- [ ] `plugin-bbk` inspections V1.5 (unused / shadowed / reserved-word / LIKE-cycle) + quick-fixes

## Distribution

- [ ] Publish BBK plugin on JetBrains Marketplace
- [ ] Publish RPG plugin on JetBrains Marketplace
- [ ] Distribute BoxBreaker IDE (Windows/Linux installers)

---

## Roadmap (long-term)

Ideas beyond the MVP, no timeline commitment:

- Cross-compile to ARM (Raspberry Pi, Apple Silicon)
- COBOL front-end (reusing the BBK back-end)
- Web playground (RPG → BBK → C → WASM in the browser)
- Integration with LLM code generation (link with thesis)
