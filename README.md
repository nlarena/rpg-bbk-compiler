# BoxBreaker

> **RPG → C.** Toolchain para compilar y ejecutar programas RPG (IBM i / AS/400) en Windows y Linux, mediante un lenguaje intermedio propio llamado **BBK (BoxBreaker)**.

**Repo:** [github.com/NicLarUniversidad/rpg-bbk-compiler](https://github.com/NicLarUniversidad/rpg-bbk-compiler)
**Estado:** alpha — diseño inicial, en desarrollo activo.

---

## Por qué

Las bases de código RPG están atadas al hardware IBM i para desarrollo, build y testing. Este toolchain desacopla el ciclo de desarrollo del hardware IBM, permitiendo iterar localmente en Windows o Linux preservando la semántica de RPG mediante BBK y una capa de runtime que emula las primitivas faltantes.

---

## Pipeline

```
RPG  ──▶  rpg-frontend  ──▶  BBK (IR)  ──┬──▶  intérprete  ──▶  ejecución
                                         │
                                         └──▶  C  ──▶  gcc  ──▶  binario nativo
```

Dos modos de ejecución sobre el mismo IR:

- **Intérprete BBK** — ciclo rápido para desarrollo y debugging.
- **AOT a nativo** — BBK → C → gcc → binario para producción.

Ambos modos comparten el runtime de emulación IBM i.

---

## Módulos

El repo es un monorepo Gradle multi-project. Ver [`docs/architecture.md`](docs/architecture.md) para el detalle completo.

| Módulo | Tipo | Rol |
|---|---|---|
| `bbk-core` | lib | AST, IR y semántica de BBK |
| `rpg-frontend` | lib | Lexer + parser RPG, traducción RPG → BBK |
| `bbk-interpreter` | app | Intérprete BBK (modo dev) |
| `bbk-compiler` | app | Lowering BBK → C + invoca gcc (modo AOT) |
| `bbk-runtime` | lib | Emulación de primitivas IBM i (job queues, DDS, library lists, etc.) |
| `plugin-bbk` | plugin | Soporte de lenguaje BBK en IntelliJ |
| `plugin-rpg` | plugin | Soporte de lenguaje RPG + comando "traducir a BBK" |
| `boxbreaker-ide` | IDE | Distribución de IDE customizado sobre IntelliJ Platform SDK |

**Capas:**

- **Núcleo headless** (`bbk-core`, `rpg-frontend`, `bbk-interpreter`, `bbk-compiler`, `bbk-runtime`) — no depende de IntelliJ. Compilá y ejecutá desde CLI o CI sin instalar ningún IDE.
- **Plugins IntelliJ** (`plugin-bbk`, `plugin-rpg`) — instalables en IntelliJ Community estándar. Proveen tooling de editor (highlighting, autocomplete, navegación) sin runtime.
- **BoxBreaker IDE** (`boxbreaker-ide`) — distribución completa: plugins + runtime + branding. Es el "shrinkwrap" del producto.

---

## Quick start

> El build todavía no está implementado. Esta sección es una proyección de la UX final.

```bash
# Build de todo el monorepo
./gradlew build

# Ejecutar un RPG en modo intérprete (rápido)
./gradlew :bbk-interpreter:run --args="examples/hello-world/hello.rpg"

# Compilar un RPG a binario nativo (AOT)
./gradlew :bbk-compiler:run --args="examples/hello-world/hello.rpg --output dist/hello.exe"
./dist/hello.exe

# Levantar BoxBreaker IDE en modo desarrollo
./gradlew :boxbreaker-ide:runIde
```

---

## Arquitectura

Documento principal: [`docs/architecture.md`](docs/architecture.md).

Spec del lenguaje BBK: `docs/bbk-spec.md` (pendiente).

---

## Stack

- **Java + Kotlin** — núcleo del compilador y plugins
- **Gradle (Kotlin DSL) multi-project** — build
- **ANTLR4** — gramática de RPG
- **IntelliJ Platform SDK** + `gradle-intellij-plugin` — plugins e IDE
- **C99 + gcc** — backend del compilador AOT
- **JUnit 5** — tests

---

## Estado y roadmap

Trabajo en curso, pendiente y roadmap a largo plazo en [`TODO.md`](TODO.md).

---

## Licencia

MIT — ver [`LICENSE`](LICENSE).
