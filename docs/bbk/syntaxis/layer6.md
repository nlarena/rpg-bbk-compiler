# BBK Grammar — Layer 6

**Estado:** implementada y verificada
**Fuente del parser:** [`plugin-bbk/src/main/grammar/BBK.bnf`](../../../plugin-bbk/src/main/grammar/BBK.bnf)
**Pre-requisitos:** [`layer1.md`](layer1.md) → [`layer5.md`](layer5.md)
**Archivos de prueba:** [`11-directives.bbk`](../../../tests/boxbreaker/examples/11-directives.bbk) (válido), [`bad-15-bad-directives.bbk`](../../../tests/boxbreaker/examples/bad/bad-15-bad-directives.bbk) (errores)

---

## Alcance

Layer 6 cierra el conjunto sintáctico principal con **directivas de preprocesador** (`PRE-*`). Todas se procesan idealmente en una fase pre-parser, pero por simplicidad las integramos como statements en la gramática principal.

| Directiva | Forma | Ejemplo |
|---|---|---|
| `PRE-IF` / `PRE-ELSEIF` / `PRE-ELSE` / `PRE-ENDIF` | Compilación condicional | `PRE-IF DEFINED(DEBUG) ... PRE-ENDIF` |
| `PRE-DEFINE` | Define macro (con o sin valor) | `PRE-DEFINE VERSION "1.0.0"` o `PRE-DEFINE DEBUG` |
| `PRE-UNDEFINE` | Quita una macro | `PRE-UNDEFINE DEBUG` |
| `PRE-INCLUDE` | Incluye otro archivo | `PRE-INCLUDE "common-types.bbki"` |
| `PRE-EOF` | Fin de fuente prematuro | `PRE-EOF` |

**Después de L6 — la sintaxis principal está completa.** Lo que sigue son refinamientos semánticos y mejoras del IDE, no nueva gramática.

---

## Estrategia: directivas como statements planos

A diferencia de muchos preprocesadores (C/C++) donde IF/ENDIF forman un bloque sintáctico anidado, en BBK **cada directiva es un statement plano e independiente**. El matching `PRE-IF`↔`PRE-ENDIF` se valida después (semánticamente).

**Trade-off:** el parser no reporta automáticamente "orphan PRE-ELSE" o "missing PRE-ENDIF". Eso lo hace un futuro analizador semántico.

**Beneficio:** la gramática es más simple, sin nesting recursivo de directivas, y las directivas pueden aparecer en cualquier lugar (top-level o dentro de bloques) sin gramáticas paralelas.

---

## Producciones BNF

### Categoría general

```bnf
directive ::= pre_if_directive
            | pre_elseif_directive
            | pre_else_directive
            | pre_endif_directive
            | pre_define_directive
            | pre_undefine_directive
            | pre_include_directive
            | pre_eof_directive
```

### Compilación condicional

```bnf
pre_if_directive     ::= KW_PRE_IF expression {pin=1}
pre_elseif_directive ::= KW_PRE_ELSEIF expression {pin=1}
pre_else_directive   ::= KW_PRE_ELSE
pre_endif_directive  ::= KW_PRE_ENDIF
```

La condición es una `expression` completa (de L4). Eso significa que `PRE-IF` acepta:
- IDENT solo: `PRE-IF DEBUG` (significa "si DEBUG está definido y es truthy")
- Llamadas a función: `PRE-IF DEFINED(DEBUG)` — `DEFINED` se parsea como IDENT y `(DEBUG)` como llamada
- Negación: `PRE-IF !DEFINED(DEBUG)`
- Booleanas: `PRE-IF DEFINED(DEBUG) && !DEFINED(PRODUCTION)`
- Constantes: `PRE-IF VERSION_MAJOR > 2`
- Literales: `PRE-IF true` (siempre incluye)

**Nota sobre `NOT`:** BBK usa `!` para negación. El estilo RPG `NOT DEFINED(X)` no es válido aquí; usar `!DEFINED(X)`.

### Definiciones de macro

```bnf
pre_define_directive   ::= KW_PRE_DEFINE IDENT pre_define_value? {pin=1}
pre_undefine_directive ::= KW_PRE_UNDEFINE IDENT {pin=1}

private pre_define_value ::= literal | STAR_IDENT
```

Dos formas de `PRE-DEFINE`:
- **Flag (sin valor):** `PRE-DEFINE DEBUG` — la macro está definida, sin valor de reemplazo
- **Con valor:** `PRE-DEFINE VERSION "1.0.0"` — la macro se reemplaza por el literal

El valor puede ser cualquier `literal` (string, número, hex, etc.) o un `STAR_IDENT` (`*NO`, `*YES`). **No** se permite IDENT como valor para evitar ambigüedad con la siguiente directiva.

### Inclusión

```bnf
pre_include_directive ::= KW_PRE_INCLUDE (STR_LIT | IDENT) {pin=1}
```

Acepta dos formas:
- `PRE-INCLUDE "common-types.bbki"` — string literal (más común, soporta paths con caracteres especiales)
- `PRE-INCLUDE common_types` — identificador (estilo RPG `/COPY libname`)

Decisión cerrada en `tokens.md`: `/COPY` y `/INCLUDE` se unificaron en `PRE-INCLUDE`.

### EOF

```bnf
pre_eof_directive ::= KW_PRE_EOF
```

Marca fin prematuro del fuente. Todo lo que sigue después de `PRE-EOF` se ignora (el preprocesador no lo emite al parser principal).

### Integración con top-level y bloques

```bnf
top_level_item ::= variable_declaration
                 | constant_declaration
                 | ctl_opt_statement
                 | data_structure_declaration
                 | file_declaration
                 | prototype_declaration
                 | procedure_declaration
                 | directive             // L6
                 | unknown_item

block_item ::= variable_declaration
             | constant_declaration
             | data_structure_declaration
             | subroutine_definition
             | directive                 // L6
             | statement
             | unknown_block_item
```

Las directivas son válidas **tanto a nivel módulo como dentro de procedure bodies** (y por extensión, dentro de cualquier bloque). Esto permite:

```bbk
PRE-DEFINE DEBUG

DCL-PROC main {
  DCL-S x INT(10);

  PRE-IF DEFINED(DEBUG)
    print("debug build");
  PRE-ELSE
    print("production build");
  PRE-ENDIF

  return;
}
```

---

## Ejemplos

### Válido — `11-directives.bbk`

```bbk
PRE-DEFINE VERSION "1.0.0"
PRE-DEFINE DEBUG

PRE-INCLUDE "common-types.bbki"

CTL-OPT MAIN(directivesDemo);

DCL-C MAX_BUFFER 1024;

DCL-PROC directivesDemo {
  DCL-S buffer CHAR(MAX_BUFFER);

  PRE-IF DEFINED(DEBUG)
    print("Debug build, version " + VERSION);
  PRE-ELSE
    print("Production build");
  PRE-ENDIF

  PRE-IF DEFINED(FEATURE_LOGGING)
    log("Demo started");
  PRE-ENDIF
}

// Conditional inclusion of additional source files
PRE-IF DEFINED(EXTENDED_FEATURES)
  PRE-INCLUDE "extended-procs.bbki"
PRE-ELSEIF DEFINED(LITE_MODE)
  PRE-INCLUDE "lite-procs.bbki"
PRE-ENDIF
```

Layer 6 reconoce todas las directivas + el contenido condicionalmente compilado entre ellas (que parsea con el resto de la gramática como si las directivas no estuvieran).

### Errores detectados — `bad-15-bad-directives.bbk`

| Categoría | Casos |
|---|---|
| PRE-DEFINE sin nombre | `PRE-DEFINE` |
| PRE-DEFINE con nombre que no es IDENT | `PRE-DEFINE "STRING_NAME"`, `PRE-DEFINE 123` |
| PRE-UNDEFINE sin nombre o con tipo incorrecto | `PRE-UNDEFINE`, `PRE-UNDEFINE "S"` |
| PRE-INCLUDE sin archivo | `PRE-INCLUDE`, `PRE-INCLUDE 42` |
| PRE-IF / PRE-ELSEIF sin condición | `PRE-IF`, `PRE-ELSEIF` |
| Condición incompleta | `PRE-IF DEFINED(` |

**Casos que NO marcan errores** (intencionalmente):
- `PRE-ELSE` orphan (sin PRE-IF previo) — el parser no valida pairing
- `PRE-ENDIF` orphan — idem
- `PRE-EOF` en cualquier lugar — idem

Esos casos los reportaría un futuro analizador semántico.

---

## Notas de implementación

### Sin SEMI en directivas

Las directivas no terminan en `;`. Esto difiere del resto de BBK (donde todo statement termina en `;`). Es una decisión consciente para alinear con la convención RPG de directivas line-oriented.

**Cómo el parser sabe dónde termina una directiva:** mediante greedy matching de su contenido fijo. Por ejemplo, `PRE-DEFINE VERSION "1.0.0"` consume `KW_PRE_DEFINE`, `IDENT (VERSION)`, y un `pre_define_value` (el string literal). Después de eso, el siguiente token inicia un nuevo `top_level_item` (o `block_item`).

Si después de `PRE-DEFINE VERSION` hay otro IDENT en vez de un literal:
```
PRE-DEFINE VERSION
otherIdent
```

`pre_define_value` no matchea IDENT por diseño (solo `literal | STAR_IDENT`). Así que el `otherIdent` queda como siguiente item para parsear. Esto evita la ambigüedad de "es este IDENT el valor de la define previa o un statement nuevo?".

### Por qué `expression` para la condición de PRE-IF

```bnf
pre_if_directive ::= KW_PRE_IF expression {pin=1}
```

En lugar de inventar una `pre_condition` ad-hoc, reusamos `expression` (de L4). Eso le da a `PRE-IF` toda la potencia de expresiones:

```bbk
PRE-IF DEFINED(DEBUG)
PRE-IF !DEFINED(PRODUCTION)
PRE-IF VERSION_MAJOR > 2
PRE-IF DEFINED(DEBUG) && !DEFINED(LITE_MODE)
PRE-IF (FEATURES & 0x1) != 0
```

Todo eso parsea con la misma regla. La semántica (qué significa "DEFINED", cómo se evalúa al preprocesar) es responsabilidad de la fase de preproceso, no del parser.

### `DEFINED(X)` parsea como función

`DEFINED` no es un keyword en BBK. Se parsea como IDENT. `DEFINED(X)` es entonces un `postfix_expression`:
- `DEFINED` (primary, IDENT)
- `(X)` (postfix_suffix, function call)

El preprocesador semántico debe reconocer "esta llamada en contexto de PRE-IF significa: ¿está X definido?".

Mismo patrón que con BIFs como `trim(x)`, `len(s)`, etc. — el parser no sabe nada especial, los reconoce como llamadas genéricas.

### Match de IF/ENDIF es semántico

El parser **no** valida que un `PRE-IF` tenga su `PRE-ENDIF` correspondiente. Cada directiva es un item plano. El validador semántico (futuro) recorrerá el árbol PSI verificando:
- Todo `PRE-IF` tiene un `PRE-ENDIF` posterior antes de EOF
- `PRE-ELSE` y `PRE-ELSEIF` están dentro de un PRE-IF abierto
- `PRE-ELSE` aparece a lo sumo una vez por bloque
- Etc.

Si la complejidad de mantener esto separado se vuelve molesta, podemos refactorizar a un `pre_if_block` que envuelva los items con anidación real. Por ahora, simpler es mejor.

---

## Estado del proyecto después de Layer 6

**La sintaxis de BBK está completa.** Las 6 capas cubren toda la superficie del lenguaje:

| Capa | Foco | Estado |
|---|---|---|
| L1 | Variables, constantes | ✅ |
| L2 | Módulo (CTL-OPT, archivos, data structures) | ✅ |
| L3 | Procedures | ✅ |
| L4 | Statements + expressions | ✅ |
| L5 | File ops, subroutines, CALLP | ✅ |
| L6 | Directivas | ✅ |

**Lo que sigue (fuera del scope de "sintaxis"):**

1. **Mejoras del IDE plugin:**
   - Actualizar `BbkSyntaxHighlighter` con los keywords de L5 y L6 (file ops, subroutines, directives) — sin esto se ven sin color especial
   - Structure view de la PSI
   - Code folding por bloque y por subroutine
   - Brace matching
   - Comment toggle (`//` y `/* */`)
   - Tabla de keywords para autocomplete básico

2. **Análisis semántico:**
   - Type checking (`DCL-S x INT(10); x = "string"` debería fallar)
   - Scope resolution (referencias a variables no declaradas)
   - Matching de IF/ENDIF, BEGSR/ENDSR
   - Resolución de prototipos vs definiciones
   - Validación de argumentos en llamadas (cuenta y tipo)

3. **Generación de código** (lowering BBK → C):
   - El backend en `bbk-compiler`
   - El runtime en `bbk-runtime` (decimal BCD propio, emulación IBM i)

4. **Frontend RPG → BBK** (`rpg-frontend`):
   - Parser de RPG (fixed + free form)
   - Traducción RPG → AST → BBK
