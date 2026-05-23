# TODO — BoxBreaker

Lista de trabajo en curso y pendiente. Estado vivo, se actualiza a medida que avanza el desarrollo.

Para el contexto arquitectónico de cada item ver [`docs/architecture.md`](docs/architecture.md).

---

## Hecho

- [x] Arquitectura inicial documentada (`docs/architecture.md`)
- [x] Estructura de módulos del monorepo (9 subproyectos)
- [x] README + `.gitignore` + `LICENSE`
- [x] Repo público creado en GitHub: `NicLarUniversidad/rpg-bbk-compiler`

## En curso

(vacío por ahora)

## Próximo

- [ ] Spec inicial de BBK (`docs/bbk-spec.md`) — definir gramática, tipos, opcodes mínimos
- [ ] Decidir lenguaje de `bbk-core` (Java vs Kotlin)
- [ ] Decidir lenguaje de `bbk-runtime` (Java vs C)

## Backlog — núcleo del compilador

- [ ] Lexer RPG
- [ ] Parser RPG (subset básico — columnas fijas, opcodes core)
- [ ] AST RPG
- [ ] Traducción RPG → BBK (opcodes core)
- [ ] AST/IR BBK en `bbk-core`
- [ ] Análisis semántico BBK
- [ ] Lowering BBK → C (opcodes core)
- [ ] Invocación a `gcc` desde `bbk-compiler`
- [ ] Intérprete BBK (modo dev)

## Backlog — runtime

- [ ] Emulación de job queues
- [ ] Emulación de activation groups
- [ ] Emulación de library lists
- [ ] Acceso a datos estilo DDS
- [ ] Spool files

## Backlog — tooling IntelliJ

- [ ] `plugin-bbk`: syntax highlighting
- [ ] `plugin-bbk`: parser de editor (errores inline)
- [ ] `plugin-bbk`: autocomplete básico
- [ ] `plugin-rpg`: syntax highlighting
- [ ] `plugin-rpg`: parser de editor
- [ ] `plugin-rpg`: comando "traducir a BBK"

## Backlog — IDE

- [ ] `boxbreaker-ide`: bundle inicial sobre IntelliJ Platform SDK
- [ ] `boxbreaker-ide`: integración entry points para intérprete y AOT
- [ ] `boxbreaker-ide`: branding (splash, icono, nombre)
- [ ] `boxbreaker-ide`: build de distribución (installer)

## Backlog — testing

- [ ] Suite E2E: compilar RPG → ejecutar → verificar output
- [ ] Equivalencia intérprete vs AOT (mismo RPG, mismo output)

## Distribución

- [ ] Publicación del plugin BBK en JetBrains Marketplace
- [ ] Publicación del plugin RPG en JetBrains Marketplace
- [ ] Distribución de BoxBreaker IDE (installers Windows/Linux)

---

## Roadmap (largo plazo)

Ideas más allá del MVP, sin compromiso de timeline:

- Cross-compile a ARM (Raspberry Pi, Apple Silicon)
- Front-end para COBOL (reusando el back-end BBK)
- Playground web (RPG → BBK → C → WASM en el navegador)
- Integración con generación de código vía LLMs (link con tesis)
