# Tokens de BoxBreaker (BBK) — propuesta tentativa

**Estado:** borrador. Las decisiones acá son punto de partida; varias quedan abiertas explícitamente al final del documento.

**Principio de diseño detrás de las elecciones:** vocabulario semántico de RPG (cuando es más rico que C), sintaxis pragmática de C (cuando es más limpia), nada de la deuda histórica de ninguno de los dos. Ver [`../../mapping/similarities.md`](../../mapping/similarities.md) y [`../../mapping/translatable.md`](../../mapping/translatable.md) para el razonamiento item por item.

**Reglas globales que afectan al léxico:**

- BBK es **siempre free-form**. No hay modo column-based.
- Statements terminan en `;`.
- Bloques delimitados con `{` y `}` (estilo C). Esto elimina las keywords `END-IF`, `ENDIF`, `ENDFOR`, `ENDDO`, `ENDSL`, `END-DS`, `END-PR`, `END-PI`, `END-PROC` del léxico — los `}` los reemplazan.
- **Case sensitivity:** **case-insensitive** en todo (keywords e identificadores), igual que RPG. `dcl-s`, `DCL-S`, `Dcl-S` son el mismo token; `myVar`, `MYVAR`, `MyVar` referencian la misma variable. El frontend normaliza a una forma canónica (a definir: lowercase) antes de emitir C (que sí es case-sensitive).
- Sin `%` prefix para BIFs: en BBK son funciones normales (`trim`, `substr`, etc.).
- Sin figurative constants RPG (`*ON`, `*OFF`, `*NULL`, `*ZERO`, `*BLANK`): se reemplazan por literales C (`true`, `false`, `null`, `0`, `" "`). El frontend traduce.
- Sin indicators globales (`*IN01`-`*IN99`): el frontend traduce a booleanos nombrados.

---

## 1. Whitespace y comentarios

Descartados por el lexer.

| Token | Descripción |
|---|---|
| `WHITESPACE` | espacios, tabs, newlines — separa tokens |
| `LINE_COMMENT` | `//` hasta fin de línea |
| `BLOCK_COMMENT` | `/* ... */` multilinea, no anidados |

---

## 2. Identificadores

```
<identifier>  ::=  <letter> { <letter> | <digit> | _ }*
<letter>      ::=  a..z | A..Z
<digit>       ::=  0..9
```

Sin caracteres especiales tipo `#`, `$`, `@` (compatibilidad EBCDIC de RPG, no relevante para BBK).

**Token:** `IDENT`

---

## 3. Literales

### 3.1 Numéricos

| Token | Forma | Ejemplos |
|---|---|---|
| `INT_LIT` | decimal: `[0-9]+` | `0`, `42`, `100` |
| `INT_LIT_HEX` | `0x[0-9A-Fa-f]+` | `0xFF`, `0x1A2B` |
| `INT_LIT_OCT` | `0o[0-7]+` | `0o755` |
| `FLOAT_LIT` | `[0-9]+ . [0-9]+ ( e [+-]? [0-9]+ )?` | `3.14`, `1.5e10`, `2.0e-3` |
| `DEC_LIT` | `[0-9]+ . [0-9]+ d?` (sufijo `d` opcional) | `19.95d`, `0.01`, `19.95` |

(`DEC_LIT` distingue literales decimales exactos de floats binarios. **Sin sufijo**, el tipo se infiere por el contexto: si el destino es `PACKED`/`ZONED`/`BINDEC` se trata como decimal exacto; si el destino es `FLOAT` o el contexto es ambiguo, se trata como float binario. **Con sufijo `d`**, siempre se trata como decimal exacto. El sufijo es estilo Java/C# decimal literals para resolver ambigüedad cuando hace falta.)

### 3.2 Strings

| Token | Forma | Ejemplos |
|---|---|---|
| `STR_LIT_DOUBLE` | `"..."` con escapes `\n`, `\t`, `\\`, `\"`, `\xNN`, `\uNNNN` | `"hola"`, `"linea\n"` |
| `STR_LIT_SINGLE` | `'...'` con escapes `\n`, `\t`, `\\`, `\'`, `\xNN`, `\uNNNN` | `'hola'`, `'it\'s'` |

Ambas formas son equivalentes semánticamente — son el mismo tipo de string. Coexisten para que código portado desde RPG (que usa `'...'`) no necesite reescribir comillas, y código nuevo o portado desde C (que típicamente usa `"..."`) tampoco. El lexer emite ambos como `STR_LIT` para el parser; la distinción doble/simple no llega al AST.

### 3.3 Booleanos y null

| Token | Forma |
|---|---|
| `TRUE` | `true` |
| `FALSE` | `false` |
| `NULL` | `null` |

### 3.4 Fechas/horas/timestamps

**No tienen literal propio.** Se construyen con funciones:

```
date("2026-05-22")
time("14:30:00")
timestamp("2026-05-22T14:30:00.000000")
```

(Decisión a confirmar: alternativa sería literales prefijados estilo SQL `DATE '2026-05-22'`. La forma funcional es más uniforme.)

---

## 4. Punctuators

| Token | Símbolo | Uso |
|---|---|---|
| `SEMI` | `;` | fin de statement |
| `COMMA` | `,` | separador de argumentos, fields, etc. |
| `DOT` | `.` | acceso a miembro de DS qualified |
| `ARROW` | `->` | acceso a miembro vía puntero, **y** tipo de retorno de procedure |
| `LPAREN` | `(` | apertura de agrupación / lista de parámetros |
| `RPAREN` | `)` | cierre de agrupación |
| `LBRACE` | `{` | apertura de bloque |
| `RBRACE` | `}` | cierre de bloque |
| `LBRACKET` | `[` | apertura de subíndice de array |
| `RBRACKET` | `]` | cierre de subíndice de array |
| `COLON` | `:` | separador en `OVERLAY(parent:pos)`, `PACKED(n:d)`, atributos `@halfup` |
| `AT` | `@` | introduce attribute modifier (ej. `@halfup`) |

---

## 5. Operadores

### 5.1 Aritméticos

| Token | Símbolo |
|---|---|
| `PLUS` | `+` |
| `MINUS` | `-` |
| `STAR` | `*` |
| `SLASH` | `/` |
| `PERCENT` | `%` (módulo) |
| `STAR_STAR` | `**` (exponenciación) |

### 5.2 Comparación

| Token | Símbolo |
|---|---|
| `EQ_EQ` | `==` |
| `BANG_EQ` | `!=` |
| `LT` | `<` |
| `GT` | `>` |
| `LT_EQ` | `<=` |
| `GT_EQ` | `>=` |

### 5.3 Lógicos

| Token | Símbolo |
|---|---|
| `AMP_AMP` | `&&` |
| `PIPE_PIPE` | `\|\|` |
| `BANG` | `!` |

### 5.4 Bitwise

| Token | Símbolo |
|---|---|
| `AMP` | `&` |
| `PIPE` | `\|` |
| `CARET` | `^` |
| `TILDE` | `~` |
| `LT_LT` | `<<` |
| `GT_GT` | `>>` |

### 5.5 Asignación

| Token | Símbolo |
|---|---|
| `EQ` | `=` |
| `PLUS_EQ` | `+=` |
| `MINUS_EQ` | `-=` |
| `STAR_EQ` | `*=` |
| `SLASH_EQ` | `/=` |
| `PERCENT_EQ` | `%=` |
| `AMP_EQ` | `&=` |
| `PIPE_EQ` | `\|=` |
| `CARET_EQ` | `^=` |
| `LT_LT_EQ` | `<<=` |
| `GT_GT_EQ` | `>>=` |

### 5.6 Ternario

| Token | Símbolo |
|---|---|
| `QUESTION` | `?` |
| `COLON` | `:` (reuso del token punctuator) |

**No incluidos** (decisión consciente):

- `++` / `--` — riesgo de sequence point issues, no aportan sobre `+= 1`.
- `,` como operador de expresión (lo de C). Confuso, raramente útil.

---

## 6. Keywords — control de flujo (estilo C)

| Token | Keyword |
|---|---|
| `KW_IF` | `if` |
| `KW_ELSE` | `else` |
| `KW_WHILE` | `while` |
| `KW_DO` | `do` |
| `KW_FOR` | `for` |
| `KW_BREAK` | `break` |
| `KW_CONTINUE` | `continue` |
| `KW_RETURN` | `return` |

Forma típica:

```bbk
if (cond) {
  ...
} else if (cond) {
  ...
} else {
  ...
}

while (cond) {
  ...
}

do {
  ...
} while (cond);

for (i = 0; i < 10; i += 1) {
  ...
}
```

`DOW` y `DOU` de RPG mapean a `while` y `do...while` respectivamente.

---

## 7. Keywords — control de flujo (estilo RPG retenido)

```bbk
select {
  when (tipo == "A") { ... }
  when (tipo == "B" || tipo == "C") { ... }
  other { ... }
}
```

| Token | Keyword |
|---|---|
| `KW_SELECT` | `select` |
| `KW_WHEN` | `when` |
| `KW_OTHER` | `other` |

(No usamos `switch`/`case` de C porque `case` exige constantes en C; `when` toma expresión booleana arbitraria. La estructura es la de RPG, los delimitadores son los de C.)

---

## 8. Keywords — declaración (estilo RPG, decisión previa)

### 8.1 Tokens de declaración (cada uno propio)

| Token | Keyword | Uso |
|---|---|---|
| `KW_DCL_S` | `DCL-S` | variable standalone |
| `KW_DCL_C` | `DCL-C` | constante con nombre |
| `KW_DCL_DS` | `DCL-DS` | data structure |
| `KW_DCL_PR` | `DCL-PR` | prototipo (forward declaration) |
| `KW_DCL_PROC` | `DCL-PROC` | procedure |
| `KW_DCL_F` | `DCL-F` | declaración de archivo |
| `KW_DCL_PARM` | `DCL-PARM` | parámetro (cuando explícito) |
| `KW_DCL_SUBF` | `DCL-SUBF` | subfield de DS (cuando explícito) |

**Decisión:** los `END-*` (END-DS, END-PR, END-PI, END-PROC) **no son tokens**. Los `}` los reemplazan.

### 8.2 Tipos primitivos

| Token | Keyword |
|---|---|
| `KW_CHAR` | `CHAR` |
| `KW_VARCHAR` | `VARCHAR` |
| `KW_PACKED` | `PACKED` |
| `KW_ZONED` | `ZONED` |
| `KW_BINDEC` | `BINDEC` |
| `KW_INT` | `INT` |
| `KW_UNS` | `UNS` |
| `KW_FLOAT` | `FLOAT` |
| `KW_DATE` | `DATE` |
| `KW_TIME` | `TIME` |
| `KW_TIMESTAMP` | `TIMESTAMP` |
| `KW_BOOL` | `BOOL` |
| `KW_POINTER` | `POINTER` |
| `KW_VOID` | `VOID` |

**Notas:**
- `IND` de RPG (indicator = char `'1'`/`'0'`) se reemplaza por `BOOL` (booleano real).
- `OBJECT` (Java object de RPG) no entra; BBK no apunta a JVM.
- Sintaxis de longitud: `CHAR(50)`, `VARCHAR(100)`, `PACKED(9:2)`, `INT(10)`, etc.

### 8.3 Modifiers de declaración

| Token | Keyword |
|---|---|
| `KW_INZ` | `INZ` (initializer) |
| `KW_BASED` | `BASED` |
| `KW_DIM` | `DIM` |
| `KW_OVERLAY` | `OVERLAY` |
| `KW_POS` | `POS` |
| `KW_LIKE` | `LIKE` |
| `KW_LIKEDS` | `LIKEDS` |
| `KW_LIKEREC` | `LIKEREC` |
| `KW_TEMPLATE` | `TEMPLATE` |
| `KW_QUALIFIED` | `QUALIFIED` |
| `KW_ALIGN` | `ALIGN` |
| `KW_VALUE` | `VALUE` |
| `KW_CONST` | `CONST` |
| `KW_OPTIONS` | `OPTIONS` |
| `KW_RTNPARM` | `RTNPARM` |
| `KW_OPDESC` | `OPDESC` |
| `KW_STATIC` | `STATIC` |
| `KW_EXPORT` | `EXPORT` |
| `KW_IMPORT` | `IMPORT` |
| `KW_EXTPGM` | `EXTPGM` |
| `KW_EXTPROC` | `EXTPROC` |

### 8.4 Keywords específicas de `DCL-F`

| Token | Keyword |
|---|---|
| `KW_USAGE` | `USAGE` |
| `KW_KEYED` | `KEYED` |
| `KW_EXTNAME` | `EXTNAME` |
| `KW_EXTFILE` | `EXTFILE` |
| `KW_PREFIX` | `PREFIX` |
| `KW_RENAME` | `RENAME` |
| `KW_DISK` | `DISK` |
| `KW_PRINTER` | `PRINTER` |
| `KW_WORKSTN` | `WORKSTN` |
| `KW_SEQ` | `SEQ` |
| `KW_USROPN` | `USROPN` |
| `KW_INFDS` | `INFDS` |
| `KW_INDDS` | `INDDS` |

### 8.5 Argumentos de USAGE

Argumentos de `USAGE(...)` no son keywords propios sino constantes especiales:

| Forma | Significado |
|---|---|
| `*INPUT` | archivo de entrada |
| `*OUTPUT` | salida |
| `*UPDATE` | actualización |
| `*DELETE` | borrado |

Se reconocen como **identificadores con prefijo `*`** vía el token genérico `STAR_IDENT`. Esto evita tener que listar un token dedicado por cada constante figurativa, y deja la puerta abierta a agregar nuevas (`*NEW`, `*CALLER`, `*ALL`, etc.) sin tocar el lexer.

---

## 9. Directivas — cada una su token

### 9.1 Directiva de módulo

| Token | Símbolo |
|---|---|
| `KW_CTL_OPT` | `CTL-OPT` |

**Sobre el prefijo de las directivas:** RPG usa `/IF`, `/INCLUDE`, etc. con `/` al frente. En BBK reemplazamos el `/` por el prefijo compound `PRE-` (consistente con `DCL-S`, `DCL-DS`, etc.). Sirve para distinguir las directivas de las keywords de control de flujo (`if`, `else`) sin necesidad de un símbolo "molesto" al frente.

### 9.2 Directivas de compilación condicional

| Token | Keyword |
|---|---|
| `KW_PRE_IF` | `PRE-IF` |
| `KW_PRE_ELSEIF` | `PRE-ELSEIF` |
| `KW_PRE_ELSE` | `PRE-ELSE` |
| `KW_PRE_ENDIF` | `PRE-ENDIF` |
| `KW_PRE_DEFINE` | `PRE-DEFINE` |
| `KW_PRE_UNDEFINE` | `PRE-UNDEFINE` |

(`PRE-ENDIF` se mantiene porque las directivas no usan bloques `{ }` — son line-oriented como en RPG/C.)

### 9.3 Directiva de inclusión

| Token | Keyword |
|---|---|
| `KW_PRE_INCLUDE` | `PRE-INCLUDE` |

Unificada — `/COPY` y `/INCLUDE` de RPG colapsan en un solo token. La semántica de "incluir un archivo de fuente en este punto" es la misma.

### 9.4 Directiva de fin de fuente

| Token | Keyword |
|---|---|
| `KW_PRE_EOF` | `PRE-EOF` |

**Descartadas** (no aportan en un compilador moderno):
- `/EJECT`, `/TITLE`, `/SPACE` (legacy de listing de compilador IBM)

---

## 10. Attribute modifiers (sintaxis intermedia novel)

Tokens introducidos por `@` para modificar el comportamiento de una statement, sin necesidad de keywords envolventes tipo `EVAL(H)`.

| Token | Símbolo | Significado |
|---|---|---|
| `ATTR_HALFUP` | `@halfup` | redondeo half-up (equivalente a `EVAL(H)`) |
| `ATTR_HALFDOWN` | `@halfdown` | redondeo half-down |
| `ATTR_TRUNC` | `@trunc` | truncar (default; mayormente para explicitar) |

Uso:

```bbk
total = precio / cantidad @halfup;
```

(Decisión: lista cerrada hardcoded, o `@<ident>` libre con validación en el parser. Recomiendo lista cerrada por simplicidad inicial.)

---

## 11. Resumen — categorías de tokens

```
Whitespace / Comments         (3)    — WHITESPACE, LINE_COMMENT, BLOCK_COMMENT (descartados por el lexer)
Identifiers                   (2)    — IDENT, STAR_IDENT
Literals                      (~7)   — INT_LIT, INT_LIT_HEX, INT_LIT_OCT, FLOAT_LIT, DEC_LIT, STR_LIT, TRUE, FALSE, NULL
Punctuators                   (12)   — ; , . -> ( ) { } [ ] : @
Operators                     (~30)  — aritméticos, comparación, lógicos, bitwise, asignación, ternario
Control flow C-style          (8)    — if, else, while, do, for, break, continue, return
Control flow RPG-style        (3)    — select, when, other
Declaration keywords          (9)    — DCL-S, DCL-C, DCL-DS, DCL-PR, DCL-PI, DCL-PROC, DCL-F, DCL-PARM, DCL-SUBF
Primitive types               (~14)  — CHAR, VARCHAR, PACKED, ZONED, BINDEC, INT, UNS, FLOAT, DATE, TIME, TIMESTAMP, BOOL, POINTER, VOID
Declaration modifiers         (~20)  — INZ, BASED, DIM, OVERLAY, POS, LIKE, LIKEDS, LIKEREC, TEMPLATE, QUALIFIED, ALIGN, VALUE, CONST, OPTIONS, RTNPARM, OPDESC, STATIC, EXPORT, IMPORT, EXTPGM, EXTPROC
File-spec keywords            (~13)  — USAGE, KEYED, EXTNAME, EXTFILE, PREFIX, RENAME, DISK, PRINTER, WORKSTN, SEQ, USROPN, INFDS, INDDS
Module directive              (1)    — CTL-OPT
Compilation directives        (8)    — PRE-IF, PRE-ELSEIF, PRE-ELSE, PRE-ENDIF, PRE-DEFINE, PRE-UNDEFINE, PRE-INCLUDE, PRE-EOF
Attribute modifiers           (3)    — @halfup, @halfdown, @trunc

TOTAL APROX                   ~130 tokens
```

(Note: la línea de strings literales se compone de `STR_LIT_DOUBLE` y `STR_LIT_SINGLE`, ambos colapsando a `STR_LIT` en el AST.)

Comparación: C99 tiene ~50 tokens, RPG IV tiene ~200+ (contando opcodes y BIFs). BBK queda en el medio, lo cual es consistente con su rol de IR intermedio.

---

## 12. Decisiones cerradas (resueltas)

Items que en la primera versión del documento estaban abiertos, ahora resueltos:

| # | Decisión | Resolución |
|---|---|---|
| 1 | Case-sensitivity | **Case-insensitive en todo.** Keywords e identificadores. El frontend normaliza antes de emitir C. |
| 2 | Comentarios multilinea | **Incluidos.** `/* ... */`, no anidados. |
| 3 | Sufijo `d` para literales decimales | **Opcional.** Sin sufijo se infiere por contexto del destino; con sufijo `d` siempre es decimal exacto. |
| 4 | Comillas para strings | **Ambas.** `"..."` y `'...'` equivalentes. |
| 5 | Date/time como literales o funciones | **Funciones.** `date("2026-05-22")`, no prefijos `D"..."`. |
| 6 | `/COPY` vs `/INCLUDE` | **Unificadas** en `PRE-INCLUDE`. |
| 7 | `*INPUT`/`*OUTPUT` tokens o identificadores con prefijo `*` | **Identificadores con prefijo `*`** (token `STAR_IDENT`). |
| 8 | Attribute modifiers lista cerrada o abierta | **Cerrada.** Solo `@halfup`, `@halfdown`, `@trunc`. |
| 9 | `LIKEREC` en V1 | **Sí, incluido.** |
| 10 | Case de keywords | **No aplica** — todo case-insensitive (ver #1). |

**Cambio adicional respecto a la versión anterior:** prefijo de directivas `/X` → `PRE-X` (ver §9.1).

---

## 13. Próximos documentos sugeridos en `boxbreaker/`

Una vez cerradas las decisiones abiertas:

- `lexical.md` — gramática léxica formal (regex/EBNF de cada categoría)
- `grammar.md` — gramática sintáctica (productions)
- `type-system.md` — sistema de tipos, conversiones, promociones
- `semantics.md` — semántica de ejecución
- `examples.md` — programas BBK de ejemplo, con su origen RPG y su salida C
