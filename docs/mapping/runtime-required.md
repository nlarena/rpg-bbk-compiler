# Constructos no resolubles por traducción

**Propósito:** identificar los aspectos de RPG / IBM i donde la diferencia con C + Windows/Linux es **estructural**, no de sintaxis. No hay manera de cerrar la brecha emitiendo código C. Hace falta uno (o más) de:

1. **Una biblioteca de emulación substantial** dentro del `bbk-runtime` que modele primitivas del sistema operativo IBM i.
2. **Componentes externos** (motor de DB embebido, terminal emulator, scheduler).
3. **Decisiones de diseño** que asumen una semántica reducida o alternativa cuando la original no es replicable.

La distinción con [`translatable.md`](translatable.md): los items de este archivo no se resuelven con un helper de matemática o de strings. Son cosas del **modelo de cómputo** y del **sistema operativo** que RPG asume y que C + Windows/Linux no proveen.

Documentos relacionados:
- [`similarities.md`](similarities.md) — mapeo directo
- [`translatable.md`](translatable.md) — distinto pero traducible

---

## 1. RPG Cycle (ciclo principal implícito)

**Lo que es:** programas RPG con archivo "primary" (P en F-spec) ejecutan un **ciclo implícito** del runtime:

1. Lee siguiente registro del archivo primario
2. Procesa breaks de control (level breaks `L1`-`L9`)
3. Ejecuta cálculos detail (C-specs sin restricción)
4. Procesa output
5. Si `*INLR = *ON` → termina; sino vuelve al paso 1

**Por qué no es traducible:** el flujo de control no está en el código fuente. **Lo provee el runtime.** El programador escribe cálculos asumiendo que "alguien" se ocupa de leer, processar breaks, hacer output, etc. Un mapeo a C no puede ignorar esto porque cambiaría qué se ejecuta y cuándo.

**Lo que requiere:**

- El `bbk-runtime` debe proveer un **dispatcher de ciclo** que el código generado invoque.
- El `bbk-compiler` tiene que detectar si el programa fuente usa el ciclo (archivos primarios declarados, breaks de nivel, indicators `*INLR`) y emitir una llamada al dispatcher en vez de un `main` lineal.
- Programas marcados `NOMAIN` o procedures puros no necesitan esto — son funciones C normales.

**Implicación de diseño:** dos modos de generación de código:
- Modo "main lineal" — para programas modernos sin ciclo
- Modo "main con ciclo" — emite `int main()` que llama a `bbk_cycle_run(<config>)`, donde `config` describe los archivos primarios, los breaks, etc.

---

## 2. Activation Groups

**Lo que es:** contexto de ejecución de IBM i que aísla recursos entre programas. Cada activation group tiene su propio:

- Conjunto de archivos abiertos
- Storage estático (variables `STATIC` por activation group)
- Commitment control boundary
- Overrides de jobs activos
- Error handlers

Programas pueden correr en `*DFTACTGRP`, `*CALLER`, o en un activation group nombrado.

**Por qué no es traducible:** no existe nada equivalente en POSIX ni en Windows. Procesos comparten un único contexto global de archivos abiertos por proceso. Los `static` de C son globales del proceso, no de un sub-ámbito.

**Lo que requiere:**

- El `bbk-runtime` necesita un **gestor de activation groups** propio: un nivel de indirección entre el código RPG y las APIs del SO.
- Cada activation group es una struct con su tabla de archivos abiertos, su pool de storage estático, etc.
- Las operaciones de archivo y los accesos a `STATIC` pasan por este gestor en vez de llamar directo al SO.

**Trade-off realista:** soportar activation groups con fidelidad completa es trabajo significativo. Una versión mínima podría tener un único activation group implícito (`*DFTACTGRP`) y dejar la fidelidad multi-actgrp para una iteración futura. Programas que no dependen explícitamente de multi-actgrp funcionarían igual.

---

## 3. Library list

**Lo que es:** una lista ordenada de "librerías" (namespaces de objetos en IBM i) que el runtime consulta para resolver referencias a objetos por nombre no calificado. Tiene varias secciones:

- System libraries (`QSYS`, etc.)
- Product libraries
- Current library
- User libraries (los que el job agrega vía `ADDLIBLE`)

Cuando un programa hace `CHAIN ... CUSTOMER` (sin calificar librería), el runtime busca `CUSTOMER` en cada librería de la lista hasta encontrarlo.

**Por qué no es traducible:** ni Windows ni Linux tienen un mecanismo equivalente a "buscar este símbolo en una lista ordenada de namespaces a runtime". El filesystem es plano (con paths absolutos o relativos al cwd), no hay search path para tablas de DB ni para procedimientos.

**Lo que requiere:**

- Una **implementación de library list dentro del `bbk-runtime`** que mapea "nombre de objeto" → "ruta a archivo / nombre de tabla SQL / referencia a procedimiento".
- APIs equivalentes a `ADDLIBLE`, `RMVLIBLE`, `CHGLIBL` para que el código pueda manipularla.
- Persistencia: la library list es state del job, no del proceso del SO.

**Decisión de diseño:** modelar las "librerías" como directorios en disco + tablas en una DB embebida. Para resolver `CUSTOMER`, buscar primero un archivo `CUSTOMER.dat` en cada directorio de la lista, o una tabla `CUSTOMER` en la DB con scope correspondiente.

---

## 4. Acceso a archivos record-level (DB2/400)

**Lo que es:** RPG accede a archivos físicos (PF) y lógicos (LF) de DB2/400 con primitivas de bajo nivel:

- `READ` — siguiente registro secuencial
- `READE key` — siguiente con clave igual
- `CHAIN key` — random access por clave
- `WRITE` — agrega registro
- `UPDATE` — actualiza el último leído
- `DELETE` — borra el último leído
- `SETLL` / `SETGT` — posiciona el cursor

Los archivos tienen descripción externa (DDS) que define los campos.

**Por qué no es traducible (directamente):** SQL estándar no expone primitivas de cursor con esa granularidad. La filosofía "read next, update what you just read" es más cercana a ISAM (un modelo de los '70s) que a SQL moderno.

**Lo que requiere:**

- Un **motor de DB embebido** dentro del runtime que provea acceso record-level. Opciones:
  - **SQLite** — embebido, sin servidor. Cursor-based access se puede emular sobre cursores SQL.
  - **Implementación propia tipo ISAM** — más fiel pero mucho trabajo.
  - **Hybrid** — SQLite para storage, una capa propia que expone primitivas tipo `CHAIN`/`READ`/`UPDATE` sobre cursores SQLite.
- **Schemas externos** (DDS) deben mapearse a definiciones de tabla SQL + metadata del runtime para que el compilador sepa los nombres y tipos de campos.

**Implicación masiva:** este punto solo es un proyecto en sí mismo. La elección del motor de DB y el modelado de keyed access es probablemente la decisión técnica más grande del proyecto después del IR.

---

## 5. DDS (Data Description Specifications)

**Lo que es:** un lenguaje aparte de RPG que describe schemas de archivos. Define campos, claves, joins, validaciones, formatos de presentación. Compilado a objetos `*FILE` que RPG referencia con `EXTNAME`.

**Por qué no es traducible:** DDS es un lenguaje propio, no parte de RPG. RPG solo lo consume.

**Lo que requiere:**

- O un **compilador DDS embebido** en el proyecto que convierta DDS a:
  - Definiciones de tabla SQL para el motor de DB elegido
  - Metadata para que el `rpg-frontend` resuelva `EXTNAME` y conozca los campos
- O **decidir que el proyecto solo soporta schemas SQL nativos**, no DDS. Más simple, menos fiel.

**Decisión de scope:** soportar DDS completo es un sub-proyecto enorme. Para MVP, soportar **archivos físicos básicos definidos en SQL** (CREATE TABLE) que el código RPG accede vía `EXTNAME`. DDS queda como roadmap.

---

## 6. Display files (pantallas 5250)

**Lo que es:** archivos de tipo "display file" (DSPF) que definen pantallas para terminales 5250 (el terminal estándar de IBM i). RPG accede con `EXFMT`, `WRITE`/`READ` a un WORKSTN.

Características de 5250:
- Diseño basado en campos en posiciones de la pantalla (filas × columnas)
- Edición por campo, validación local
- Función keys (F1-F24) y attention keys
- Subfiles (listas paginadas con cursor)

**Por qué no es traducible:** Windows/Linux no tienen un equivalente nativo a 5250. El paradigma "el programa lee/escribe una pantalla entera" no se parece a stdin/stdout ni a un GUI moderno.

**Lo que requiere:**

- Una **biblioteca de emulación 5250** (o adaptador a un emulador existente como `tn5250`).
- O renderizar a una **pseudo-pantalla en consola** (ncurses-like) para versión mínima.
- O reescribir display files a una tecnología moderna (HTML + servidor, o GUI nativo) — pero eso se aleja de la promesa de "RPG corre sin cambios".

**Decisión de scope realista para MVP:** soporte solo de programas batch (sin WORKSTN). Display files quedan para una fase posterior, posiblemente con un emulador externo.

---

## 7. Printer files y spool

**Lo que es:** salida a impresora va a un *spool file* en una *output queue* (OUTQ). El programa hace `WRITE` a un printer file (PRTF), el spool file queda en cola, el operador lo libera para imprimir, lo reimprime, o lo borra.

**Por qué no es traducible:** Windows tiene printer queues pero el modelo es distinto. Linux no tiene un equivalente nativo (CUPS lo cubre pero requiere setup).

**Lo que requiere:**

- Una **emulación de spool** dentro del `bbk-runtime` — directorio donde se guardan los outputs como archivos, con metadata (status, fecha, usuario, etc.).
- APIs equivalentes a `WRKSPLF`, `DLTSPLF`, etc., o decidir que solo soportamos "imprimir = generar archivo en /spool/<usuario>/<timestamp>.txt".

**Decisión de scope:** versión mínima — `WRITE` a printer file genera un `.txt` en un directorio configurable. Sin metadata, sin queue, sin OUTQs. Suficiente para programas batch que generan reportes.

---

## 8. Objetos IBM i (*PGM, *MODULE, *SRVPGM, *FILE, *DTAARA, *MSGF, *USRPRF, *JOBD, etc.)

**Lo que es:** IBM i tiene un namespace **unificado** de objetos. Cada objeto tiene tipo, nombre, librería de pertenencia, autoridad, atributos. Filesystem y "objetos" son la misma cosa desde la perspectiva del SO.

Tipos relevantes:

- `*PGM` — programa ejecutable
- `*MODULE` — módulo compilado pero no bindeado
- `*SRVPGM` — service program (shared library)
- `*FILE` — archivo de datos (PF, LF, DSPF, PRTF, etc.)
- `*DTAARA` — data area (ver §10)
- `*MSGF` — message file
- `*USRPRF` — user profile
- `*JOBD` — job description
- ... y ~100 más

**Por qué no es traducible:** Windows/Linux no tienen un namespace unificado tipado. Tienen un filesystem (archivos y directorios) y namespaces separados para usuarios, servicios, etc.

**Lo que requiere:**

- Decidir un **modelo de mapping**: cómo representamos cada tipo de objeto.
  - `*PGM`, `*SRVPGM` → ejecutables y bibliotecas dinámicas (mapeo natural)
  - `*FILE` (data) → tablas SQL + metadata (relacionado con §4)
  - `*DTAARA` → archivos en disco (relacionado con §10)
  - `*MSGF` → archivos con messages (relacionado con §13)
  - `*USRPRF` → ¿usuarios del SO? ¿Tabla propia? — decisión pendiente
- Una **convención de nombres y rutas** dentro del `bbk-runtime` que respete la "librería" como sub-directorio o sub-schema.

**Decisión de scope:** mapping completo es ambicioso. MVP: soportar `*PGM`, `*SRVPGM`, `*MODULE` y `*FILE` (data). El resto se posterga.

---

## 9. Single-level Store (SLS)

**Lo que es:** modelo de memoria único de IBM i. Toda la memoria — RAM y disco — está en un mismo espacio direccionado. Las "variables" en disco se acceden con la misma semántica que las en memoria. Conceptualmente, no hay distinción entre "leer un archivo" y "leer una variable".

**Por qué no es traducible:** no hay equivalente en ningún SO moderno. Es una característica única de IBM i (heredada de System/38).

**Lo que requiere:** **no lo emulamos.** Este es un caso donde la decisión sana es no intentar replicar la semántica. Los programas RPG que dependen de SLS de forma sutil (uso de punteros que cruzan boundaries memoria/disco, persistencia automática de variables) van a requerir refactor manual o no van a funcionar.

**Implicación:** documentar este límite explícitamente. Programas con código "exotic" que depende de SLS no son targets soportados.

---

## 10. Data areas (*DTAARA)

**Lo que es:** storage persistente con nombre. Tiene tipo (CHAR, DEC, LGL) y longitud declarada. Vive entre invocaciones de programas. APIs: `*LOCK DTAARA`, `IN`/`OUT` para leer/escribir, `UNLOCK`.

**Por qué no es traducible:** C tiene variables `static` que persisten en el proceso, pero los data areas **persisten entre procesos**. Cuando un proceso termina, sus `static` se pierden. Los data areas no.

**Lo que requiere:**

- Implementar data areas como **archivos en disco** dentro del `bbk-runtime`, con:
  - Locking de filesystem para `*LOCK`
  - Serialización del valor según el tipo declarado
  - APIs `bbk_dtaara_in()`, `bbk_dtaara_out()`, `bbk_dtaara_lock()`, etc.
- Decidir dónde se guardan (un directorio configurable `~/.boxbreaker/dtaara/`).

**Razonablemente simple** comparado con otros items de este archivo. Lo metí acá y no en translatable porque requiere persistencia entre procesos, que es una primitiva del runtime, no una función de matemática.

---

## 11. Authority / seguridad por objeto

**Lo que es:** IBM i tiene un sistema de seguridad **integrado al SO** que asigna autoridad por objeto (READ, ADD, UPDATE, DELETE, etc.) a usuarios y grupos. RPG asume que el SO valida acceso al ejecutar `CHAIN`, `UPDATE`, etc., y si no tiene autoridad, falla.

**Por qué no es traducible:** Windows/Linux tienen permisos de filesystem (rwx) pero no a nivel de tabla de DB ni de operaciones específicas (UPDATE vs DELETE, por ejemplo, son lo mismo a nivel de "write" en POSIX).

**Lo que requiere:**

- **Decisión:** ignorar autoridad (el código corre con permisos full) en una versión inicial.
- Versión completa: emular tabla de autoridades en el `bbk-runtime`, validar antes de cada operación. Mucho trabajo.

**MVP realista:** ignorar autoridad. Documentar que el modelo de seguridad no se preserva.

---

## 12. Job queues y subsistemas

**Lo que es:** IBM i tiene **subsistemas** (work managers) que toman trabajos de **job queues** y los ejecutan según prioridades, perfiles, etc. `SBMJOB` envía un job a una queue.

**Por qué no es traducible:** ningún SO moderno tiene un equivalente nativo. Existen schedulers (cron, systemd timers, Windows Task Scheduler) y queues (RabbitMQ, etc.) pero ninguno coincide con la semántica de "subsistema toma jobs y los ejecuta".

**Lo que requiere:**

- Decidir si se soporta `SBMJOB`. Si sí: implementar un mini-scheduler en el runtime (probablemente cola en disco + worker process).
- Si no: documentar como no soportado.

**MVP realista:** no soportar. La mayoría del código RPG que se quiere modernizar es batch o interactivo síncrono, no orquestación de jobs.

---

## 13. Message handling (CL-style messages)

**Lo que es:** programas RPG envían y reciben **mensajes** del sistema de messaging de IBM i (`SNDPGMMSG`, `RCVMSG`). Los mensajes están en un **message file** con identificadores y textos parametrizables. Tienen severidad, type (info, inquiry, escape, etc.), y se ponen en la **message queue** del job.

**Por qué no es traducible:** stderr/stdout no es equivalente. Linux tiene `syslog` pero el modelo de severidad y de queue es distinto.

**Lo que requiere:**

- Una **biblioteca de messaging** en el runtime — emulación de message queues por job (proceso), serialización a stdout/stderr o a un archivo de log.
- Si se necesita fidelidad: implementación de message files como archivos JSON/YAML con identificadores y textos.

**MVP realista:** las funciones de mensajes se vuelven calls a `fprintf(stderr, ...)` con prefijo según severidad. Suficiente para la mayoría de los casos de uso.

---

## 14. *PSSR y program exception handling

**Lo que es:** subroutine especial `*PSSR` que se ejecuta si el programa recibe una excepción no manejada (división por cero, error de archivo, etc.). Es a nivel programa, no por bloque.

**Por qué no es directamente traducible:** C no tiene excepciones. Tiene `setjmp`/`longjmp` (rudimentario) y `signal` (para signals del SO).

**Lo que requiere:**

- Implementar `*PSSR` con `setjmp`/`longjmp` + signal handlers en el runtime.
- Las opcodes que pueden fallar (división, file ops) chequean errores y hacen `longjmp` al handler `*PSSR` registrado si está activo.

**Razonable de implementar** pero requiere infrastructura cross-cutting en el código generado.

---

## 15. Commitment control

**Lo que es:** transacciones a nivel de activation group. `COMMIT` y `ROLBK` (rollback). Define qué updates se hacen atómicos en grupo.

**Por qué no es traducible:** depende del motor de DB elegido. SQLite tiene transacciones pero el modelo de "transacción por activation group" es propio de IBM i.

**Lo que requiere:** el motor de DB del runtime (§4) debe soportar transacciones. El gestor de activation groups (§2) debe abrir una transacción al entrar a un commit-controlled actgrp y commitearla/rolbackearla según corresponda.

---

## 16. Time-of-day / job attributes

**Lo que es:** RPG accede a atributos del job (usuario, fecha del sistema, librería actual, etc.) vía PSDS, BIFs como `%TIMESTAMP`, etc. Muchos asumen un modelo "este programa corre dentro de un job de IBM i".

**Por qué requiere runtime:** la mayoría son traducibles (fecha → `time()`, usuario → `getenv("USER")` o equivalente), pero algunos no tienen equivalente directo (número de job, sub-system, library list activa) o tienen valores artificiales.

**Lo que requiere:**

- El runtime debe simular un "job context" con valores razonables: nombre de job sintético, número, librería actual, library list, usuario, etc.
- Cuando un programa lee PSDS o llama a BIFs de job info, el runtime devuelve valores del contexto simulado.

---

## Resumen — el mapa del runtime

Para que el lowering RPG → C funcione en serio, el `bbk-runtime` debe implementar (en orden de prioridad razonable):

| Componente | Complejidad | Necesario para |
|---|---|---|
| Cycle dispatcher | Media | Programas RPG con `*INLR` y archivos primarios |
| Decimal arithmetic (de translatable) | Media | Casi cualquier programa RPG |
| File access record-level (DB engine) | **Alta** | Cualquier programa que toca DB |
| Library list | Media | Resolución de nombres no calificados |
| `*PSSR` y exception handling | Media | Programas con manejo de errores legacy |
| Data areas | Baja | Programas que usan persistencia simple |
| Spool / printer files | Baja-Media | Programas de reportería |
| Activation groups | **Alta** | Programas que dependen de aislamiento de recursos |
| Display files (5250) | **Muy alta** | Programas interactivos (mucho código legacy) |
| Job queues / scheduler | Alta | Programas que envían jobs |
| Authority | Media | Fidelidad de seguridad |
| Commitment control | Media | Programas transaccionales |
| Single-level store | Imposible | (no se soporta — limitación documentada) |

**Lo que queda de "no soportado documentado":**
- Single-level store (semantically incompatible)
- Authority/security model (defer al SO o ignorar)
- Algunos atributos de job context exotic

**Decisión de scope para el MVP del proyecto BoxBreaker:**

Implementar primero los items de complejidad media-baja:
1. Cycle dispatcher
2. Decimal arithmetic (translatable)
3. Data areas
4. `*PSSR`
5. Spool / printer (versión mínima)

Dejar para fases posteriores:
- DB engine (paralelo a lo anterior porque es bloqueante para casi todo)
- Activation groups (versión mínima: uno solo)
- Display files (puede ser fase 2 o 3)

Esto define qué subset de RPG el sistema soporta en cada milestone.
