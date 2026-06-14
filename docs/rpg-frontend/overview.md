# rpg-frontend — Traductor RPG → BBK

**Módulo:** `rpg-frontend/`
**Qué hace:** traduce RPG (free-format RPGLE) a **texto BBK** (`.bbk`), validable abriéndolo con `plugin-bbk`.
**Estado:** traduce programas free-form completos. 63 tests.

---

## 1. Decisiones de arquitectura

| Decisión | Elección | Por qué |
|---|---|---|
| Dialecto de entrada | **free-format RPGLE** | Ya tiene `dcl-s`, `dcl-proc`, `if/endif`; cerca de BBK. Lexer estándar. |
| Lenguaje | **Java** (21) | Consistente con `plugin-bbk`. Reusable desde `plugin-rpg`. Sin nueva toolchain. |
| Salida | **texto `.bbk`** | Legible, se verifica con el plugin, desacopla de `bbk-core` (aún vacío). |
| Parser | **recursive descent a mano** | Control total del `=` (asignación vs igualdad), buenos errores, sin dependencias. (CUP y Grammar-Kit descartados; Grammar-Kit se reserva para `plugin-rpg`.) |
| AST | **sealed interfaces + records** | El `switch` exhaustivo del emitter obliga a cubrir cada nodo: red de seguridad en compilación. |

Todo grounded en `docs/theory/rpgle-grammar.md` (gramática de entrada) y la gramática de BBK (salida).

---

## 2. El pipeline

```
RPG source ──▶ RpgLexer ──▶ RpgParser ──▶ RpgProgram ──▶ BbkEmitter ──▶ texto .bbk
   (String)     (tokens)      (AST)         (AST raíz)     (traductor)
```

- **RpgLexer** (`lexer/`) — texto crudo → lista plana de `RpgToken`.
- **RpgParser** (`parser/`) — tokens → árbol `RpgProgram`. Aquí vive la desambiguación del `=`.
- **AST** (`ast/`) — estructura inmutable que describe *qué dijo el RPG*, fielmente.
- **BbkEmitter** (`translate/`) — recorre el AST y emite BBK como string.
- **RpgToBbk** — fachada: `RpgToBbk.translate(rpgSource)` → BBK text.

---

## 3. Qué cubre (toda la superficie free-form)

### Léxico (`rpgle-grammar.md §2`)
Identificadores, keywords (reserved words + opcodes free-form + `and/or/not/xor`), `%`BIFs; los 9 literales (int/dec/string/hex/date/time/timestamp/ucs2/graphic) + figurativas/indicadores; operadores `+ - * / **`, `= <> < > <= >=`; puntuadores `; : , ( ) .`; directivas `**FREE` y `/COPY //IF /DEFINE ...`.

El `*` se desambigua por **contexto**: en posición de valor (tras `(`, `=`, `:`, inicio) es star-name (`*ON`, `*NO`, `*INPUT`, `*INLR`); tras un operando es multiplicación.

### Declaraciones (§4.2)
`ctl-opt`, `dcl-s`, `dcl-c`, `dcl-ds` (+subfields + `end-ds`), `dcl-pr` (+ `end-pr`), `dcl-pi` (+ `end-pi`), `dcl-f`, `dcl-proc` (+ `end-proc`). Tipos: escalar con tamaño, `LIKE`/`LIKEDS`/`LIKEREC`.

### Statements (§4.3)
asignación (incl. `eval`/`eval(h)`/`eval(r)`), `if/elseif/else`, `select/when/other`, `dow`, `dou`, `for` (`to`/`downto`/`by`), `monitor/on-error/on-exit`, `begsr/endsr`, `return`, `leave`, `iter`, `leavesr`, file ops (`read`/`chain`/`write`/...), `exsr`, `callp`, directivas.

### Expresiones (§4.4)
Cadena de precedencia completa (or/and/not/comparación/aditiva/multiplicativa/potencia/unaria/postfix/primary), llamadas, BIFs, member access (`.`), figurativas, indicadores, todos los literales.

---

## 4. Traducciones RPG → BBK (las que hacen "transpilar")

| RPG | BBK |
|---|---|
| `int(10)`, `ind` | `INT(10)`, `BOOL` |
| `=` (comparación), `<>` | `==`, `!=` |
| `and`/`or`/`not` | `&&`/`\|\|`/`!` |
| `'it''s'`, `199.95` | `"it's"`, `199.95d` |
| `*on`/`*off`/`*null` | `true`/`false`/`null` |
| `f(a : b)` | `f(a, b)` |
| `if c; … endif;` | `if (c) { … }` |
| `dow c; … enddo;` | `while (c) { … }` |
| `dou c; … enddo;` | `do { … } while (!(c));` |
| `for i = 1 to 10 by 2;` | `for (i = 1; i <= 10; i += 2) { … }` |
| `select; when c; …` | `select { when (c) { … } }` |
| `monitor; on-error; …` | `monitor { … } on-error { … }` |
| `leave;` / `iter;` | `break;` / `continue;` |
| `dcl-ds x qualified; … end-ds;` | `DCL-DS x QUALIFIED { … }` |
| `dcl-proc` + `dcl-pi` params | `DCL-PROC name(p TYPE VALUE) -> RET { … }` (params inline) |

El emitter pone **paréntesis mínimos** según precedencia y asociatividad. Las directivas (`/COPY`) se emiten como comentario BBK (preservadas, sin traducción inventada).

---

## 5. Gap consciente

**Asignación compuesta** (`+= -= *= /=`): el RPGLE real ≥7.1 la tiene, pero `rpgle-grammar.md §4.3.1` no la lista, así que no se inventa. Cuando se decida agregarla: token nuevo + manejo en parser + emisión.

---

## 6. Layout del módulo

```
rpg-frontend/src/main/java/com/larena/boxbreaker/rpg/
├── RpgToBbk.java                 (fachada: RPG text → BBK text)
├── lexer/
│   ├── RpgTokenType.java  RpgToken.java  RpgKeywords.java
│   ├── RpgLexer.java      RpgLexException.java
├── ast/
│   ├── RpgProgram.java  RpgItem.java  RpgDeclaration.java
│   ├── RpgStatement.java  RpgExpr.java  RpgType.java  RpgKeyword.java
├── parser/
│   ├── RpgParser.java  RpgParseException.java
└── translate/
    └── BbkEmitter.java
```

---

## 7. Próximo paso

Cerrar el loop: confirmar que el BBK generado **parsea y tipa sin errores** abriéndolo con `plugin-bbk` (validación end-to-end frontend ↔ plugin).

---

## 8. Documentos relacionados

- [`../theory/rpgle-grammar.md`](../theory/rpgle-grammar.md) — gramática de RPGLE (entrada)
- [`../theory/boxbreaker/grammar.md`](../theory/boxbreaker/grammar.md) — gramática de BBK (salida)
- [`../mapping/translatable.md`](../mapping/translatable.md) — construcciones RPG resolubles por traducción
- [`../mapping/runtime-required.md`](../mapping/runtime-required.md) — construcciones que requieren emulación de IBM i
- [`../architecture.md`](../architecture.md) — arquitectura general
