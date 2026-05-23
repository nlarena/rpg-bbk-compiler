# Arquitectura — BoxBreaker

**Repo:** [github.com/NicLarUniversidad/rpg-bbk-compiler](https://github.com/NicLarUniversidad/rpg-bbk-compiler)
**Tagline:** RPG → C
**Fecha del documento:** 2026-05-22
**Estado:** diseño inicial, sin código todavía

Este documento describe la organización de módulos del sistema y las responsabilidades de cada uno. Para el lenguaje intermedio BBK, ver [`bbk-spec.md`](bbk-spec.md) (pendiente).

---

## Resumen

El sistema compila y ejecuta programas RPG (IBM i / AS/400) en Windows y Linux mediante un lenguaje intermedio propio llamado **BBK (BoxBreaker)**. Soporta dos modos de ejecución: **intérprete** (ciclo rápido de desarrollo) y **AOT a binario nativo** vía `C` + `gcc` (producción).

El tooling de editor se distribuye como **plugins de IntelliJ** que pueden instalarse en IntelliJ Community estándar, y opcionalmente se empaqueta un **IDE customizado** (BoxBreaker IDE) basado en IntelliJ Platform SDK que bundlea los plugins junto con el runtime de ejecución.

---

## Pipeline de compilación

```
                    ┌─────────────┐
                    │   RPG src   │
                    └──────┬──────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │  rpg-frontend   │  lexer + parser + traducción
                  │  (lib headless) │  RPG → BBK
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │   BBK (IR)      │  AST/IR neutro, tipos, semántica
                  │   bbk-core      │
                  └────────┬────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
    ┌──────────────────┐      ┌──────────────────┐
    │ bbk-interpreter  │      │  bbk-compiler    │
    │  (modo dev)      │      │  (modo AOT)      │
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
        Ejecución                  Binario nativo
        interpretada               (.exe, ELF, Mach-O)
```

---

## Módulos

El repo es un monorepo Gradle multi-project con 9 subproyectos. Tres capas:

1. **Núcleo headless** (no depende de IntelliJ): `bbk-core`, `rpg-frontend`, `bbk-interpreter`, `bbk-compiler`, `bbk-runtime`.
2. **Plugins de IntelliJ** (dependen de IntelliJ Platform): `plugin-bbk`, `plugin-rpg`.
3. **Distribución de IDE**: `boxbreaker-ide`.

### `bbk-core/` — librería núcleo

Tipos y estructuras de datos del lenguaje BBK:

- AST de BBK
- Modelo de tipos
- Análisis semántico
- Utilidades comunes (visitors, transformers, pretty-printer)

**Sin dependencias a IntelliJ Platform.** Es la pieza compartida por todos los demás módulos.

**Lenguaje:** Java o Kotlin (decidir).

### `rpg-frontend/` — librería de traducción RPG

Lexer y parser de RPG, más la lógica de traducción RPG → BBK:

- Lexer RPG (manejo de columnas fijas, indicadores, opcodes)
- Parser RPG → AST RPG
- Lowering: AST RPG → AST BBK

**Sin dependencias a IntelliJ Platform.** Diseñado para ser usado headless desde CLI, CI, o invocado por el `plugin-rpg`.

**Lenguaje:** Java + ANTLR4.

### `bbk-interpreter/` — intérprete BBK (modo desarrollo)

Ejecuta BBK directamente sin compilación a nativo. Pensado para iteración rápida y debugging:

- Evalúa el AST BBK
- Integra `bbk-runtime` para resolver primitivas IBM i
- Expone API para integración con debugger

**Lenguaje:** Java o Kotlin.

### `bbk-compiler/` — compilador AOT BBK → C → nativo (modo producción)

Toma BBK y genera un binario nativo:

- Lowering BBK → C99
- Invoca `gcc` para producir el binario
- Linkea contra `bbk-runtime`
- CLI principal del compilador

**Lenguaje:** Java o Kotlin para el lowering; emite C99.

### `bbk-runtime/` — runtime de emulación IBM i

Implementación de primitivas IBM i ausentes en Windows/Linux:

- Job queues, activation groups, library lists
- Acceso a datos estilo DDS
- Spool files
- I/O específico de IBM i

Usado por:
- `bbk-interpreter` (linkeo directo)
- Binarios producidos por `bbk-compiler` (linkeo nativo)

**Lenguaje:** decidir entre Java (más simple, requiere JVM en runtime) o C (linkeo directo al binario, sin JVM).

### `plugin-bbk/` — plugin IntelliJ para BBK

Soporte de lenguaje BBK en IntelliJ:

- Syntax highlighting
- Parser para el editor (errores inline)
- Autocomplete
- Navegación (go-to-definition, find usages)
- Refactors básicos

**Depende de** `bbk-core` + IntelliJ Platform SDK. **No contiene el intérprete ni el compilador AOT** — esos viven afuera y son invocados por el IDE.

**Lenguaje:** Kotlin.

### `plugin-rpg/` — plugin IntelliJ para RPG

Soporte de lenguaje RPG en IntelliJ + integración con traducción a BBK:

- Syntax highlighting RPG
- Parser RPG en el editor (vía `rpg-frontend`)
- Comando "traducir a BBK"
- Navegación entre RPG y BBK generado

**Depende de** `rpg-frontend` + `plugin-bbk` + IntelliJ Platform SDK.

**Lenguaje:** Kotlin.

### `boxbreaker-ide/` — módulo de integración (IDE custom sobre IntelliJ Platform SDK)

Es el **punto de integración continua** del sistema. Existe desde la iteración 1 como cáscara mínima de IntelliJ Platform que bundlea el primer plugin, y se va engordando a medida que el resto del sistema aparece. No es un módulo "del final" — se toca a lo largo de todo el desarrollo.

Lo que crece en este módulo iteración por iteración:

- **Inicio:** configuración mínima de IntelliJ Platform SDK (tipo IDE custom, versión de plataforma, plugin code/version interno), sin branding ni assets cosméticos. Bundlea `plugin-bbk`.
- **Cuando aparece `plugin-rpg`:** se agrega al bundling.
- **Cuando aparece `bbk-interpreter`:** se cablea una acción "Run interpreted" que invoca al intérprete.
- **Cuando aparece `bbk-runtime`:** se incluye en el classpath para que el intérprete resuelva primitivas.
- **Cuando aparece `bbk-compiler`:** se cablea una acción "Compile to native".
- **A lo largo del desarrollo:** asociaciones de file type (`.rpg`, `.bbk`), tool windows (consola de runtime, log de compilación), run configurations, settings UI, integración debugger.
- **Mucho más tarde (fase de distribución):** branding (nombre comercial, splash, icono), build/sign/installer.

Es lo último que cobra branding pero lo primero que existe como cáscara runnable. El harness de desarrollo de todo el proyecto es `./gradlew :boxbreaker-ide:runIde`.

**Depende de** todos los plugins y módulos de runtime. Usa `gradle-intellij-plugin` con configuración de IDE custom.

**Lenguaje:** Kotlin para configuración; en su mayoría es Gradle + recursos.

---

## Propiedades del diseño

**Headless-first.** El núcleo del compilador (`bbk-core`, `rpg-frontend`, `bbk-compiler`, `bbk-interpreter`, `bbk-runtime`) no depende de IntelliJ. Se puede compilar y ejecutar RPG desde CLI, CI/CD, o scripts batch sin instalar ningún IDE.

**Plugins reutilizables en IntelliJ Community.** Los plugins `plugin-bbk` y `plugin-rpg` no dependen del IDE custom. Se pueden publicar en JetBrains Marketplace e instalar en IntelliJ Community estándar, donde proveen tooling de editor (pero no ejecución, eso requiere BoxBreaker IDE o la CLI).

**El IDE custom es el punto de integración continua.** `boxbreaker-ide` existe desde la iteración 1 como cáscara mínima de IntelliJ Platform y bundlea el primer plugin. A medida que aparecen plugins, módulos de runtime y entry points de ejecución, se cosen dentro de este módulo. El loop de desarrollo del proyecto entero es `runIde` sobre `boxbreaker-ide`, lo que garantiza que el contrato de bundling se valida desde el primer día (no aparecen sorpresas de integración tarde). El branding/distribución es la última capa que se agrega encima, no la primera.

**Dos modos de ejecución compartiendo IR.** Intérprete y compilador AOT consumen el mismo `bbk-core` y el mismo `bbk-runtime`. La diferencia está en cómo recorren el AST/IR — interpretándolo o lowereándolo a C.

---

## Dependencias entre módulos

```
boxbreaker-ide
    ├── plugin-bbk
    │   └── bbk-core
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

Reglas:
- `bbk-core` no depende de nadie.
- `bbk-runtime` no depende de nadie (o solo de `bbk-core` para tipos).
- Los plugins dependen de `bbk-core` / `rpg-frontend` pero **no** de los módulos de ejecución.
- `boxbreaker-ide` es el único módulo que conoce a todos.

---

## Decisiones pendientes

- **Nombre del repo** (en `compilador-rpg-bbk-setup.md` quedó pendiente).
- **Lenguaje de `bbk-runtime`**: Java (más simple, depende de JVM) vs C (linkeo nativo, sin JVM en runtime).
- **Lenguaje de `bbk-core`**: Java o Kotlin. Si todos los plugins son Kotlin y la mayoría del compilador también, Kotlin gana coherencia.
- **Estrategia de tests E2E**: cómo se valida que el intérprete y el binario AOT producen el mismo output para un mismo RPG.
