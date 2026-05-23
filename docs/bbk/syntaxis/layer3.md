# BBK Grammar — Layer 3

**Estado:** implementada y verificada
**Fuente del parser:** [`plugin-bbk/src/main/grammar/BBK.bnf`](../../../plugin-bbk/src/main/grammar/BBK.bnf)
**Pre-requisitos:** [`layer1.md`](layer1.md), [`layer2.md`](layer2.md)
**Archivos de prueba:** [`05-procedures.bbk`](../../../tests/boxbreaker/examples/05-procedures.bbk) (válido), [`bad-12-bad-procedures.bbk`](../../../tests/boxbreaker/examples/bad/bad-12-bad-procedures.bbk) (errores intencionales)

---

## Alcance

Layer 3 trae **procedures** al lenguaje:

| Construcción | Forma | Ejemplo |
|---|---|---|
| `DCL-PR` | Prototipo (forward declaration) | `DCL-PR sum(a INT(10) VALUE, b INT(10) VALUE) -> INT(10);` |
| `DCL-PROC` | Definición con body | `DCL-PROC sum(a INT(10), b INT(10)) -> INT(10) EXPORT { return a + b; }` |

**Decisión de scope para acotar L3:** el **body** del procedure soporta declaraciones anidadas (`DCL-S`/`DCL-C`/`DCL-DS`) y cualquier otra cosa cae al fallback `unknown_proc_item`. Esto significa que statements y expressions reales **todavía no se parsean** — vienen en Layer 4. Pero la estructura general del procedure (signature, params, return, body) sí se parsea y valida.

**Lo que sigue sin cubrirse:**
- Statements (`if`, `while`, `for`, `select`, `return`, etc.) — Layer 4
- Expressions (`a + b`, `f(x)`, etc.) — Layer 5
- File ops (`read`, `chain`, `write`, etc.) — Layer 6
- `monitor`/`on-error`/`on-exit` — Layer 6
- Directivas `PRE-*` — Layer 6

---

## Top-level actualizado

```bnf
top_level_item ::= variable_declaration       // L1
                 | constant_declaration        // L1
                 | ctl_opt_statement           // L2
                 | data_structure_declaration  // L2
                 | file_declaration            // L2
                 | prototype_declaration       // L3
                 | procedure_declaration       // L3
                 | unknown_item
```

---

## Producciones BNF

### Prototype declaration

```bnf
prototype_declaration ::= KW_DCL_PR IDENT inline_param_list? return_type? pr_modifier* SEMI {pin=1}
```

**Forma:** `DCL-PR name (params)? (-> RetType)? modifiers* ;`

Ejemplos:
```bbk
DCL-PR sum(a INT(10) VALUE, b INT(10) VALUE) -> INT(10);
DCL-PR greet(name VARCHAR(50) CONST);                   // sin return
DCL-PR factorial(n INT(10) VALUE) -> INT(10);
DCL-PR CUSTPROG(custId INT(10) VALUE, status INT(10)) EXTPGM("CUSTPROG");
```

### Procedure definition

```bnf
procedure_declaration ::= KW_DCL_PROC IDENT inline_param_list? return_type? proc_modifier* procedure_body {pin=1}

procedure_body ::= LBRACE proc_body_item* RBRACE {pin=1}

proc_body_item ::= variable_declaration
                 | constant_declaration
                 | data_structure_declaration
                 | unknown_proc_item

// Body fallback: consume any token but stop at the closing RBRACE.
private unknown_proc_item ::= !RBRACE !<<eof>> any_token
```

**Forma:** `DCL-PROC name (params)? (-> RetType)? modifiers* { body }`

Ejemplos:
```bbk
DCL-PROC main {
  print("hello");
}

DCL-PROC sum(a INT(10) VALUE, b INT(10) VALUE) -> INT(10) EXPORT {
  return a + b;
}

DCL-PROC processOrder(rec LIKEDS(orderRec) CONST) {
  print("processing");
}
```

El cuerpo soporta `DCL-S`/`DCL-C`/`DCL-DS` locales con análisis completo, y el resto (statements, expressions, function calls) pasa por `unknown_proc_item`. La clave del fallback es **`!RBRACE`**: consume cualquier token excepto `}`, así el `RBRACE` final cierra correctamente el body.

### Inline parameter list

```bnf
inline_param_list ::= LPAREN inline_params? RPAREN {pin=1}

private inline_params ::= inline_param (COMMA inline_param)*

inline_param ::= IDENT type_specification param_modifier* {pin=2}
```

**Forma:** `(name TYPE modifier*, name TYPE modifier*, ...)`

Detalles:
- **Comma-separated** (no colon como CTL-OPT/USAGE).
- Paréntesis pueden estar vacíos: `()` para procedures sin parámetros.
- `inline_param {pin=2}` se compromete después de ver `IDENT type_specification`. Si el tipo falla, hace backtrack (útil para tolerar IDENT colgados en otros contextos sin generar errores espurios).

### Return type

```bnf
return_type ::= ARROW type_specification {pin=1}
```

**Forma:** `-> TYPE`

Sin return type, el procedure es `void` (no retorna nada). Con return type, se debe usar `return expression;` en el body.

### Procedure-level modifiers

```bnf
proc_modifier ::= KW_EXPORT | extproc_modifier

extproc_modifier ::= KW_EXTPROC LPAREN STR_LIT RPAREN {pin=1}
```

- `EXPORT` — el procedure es visible fuera del módulo
- `EXTPROC("name")` — usa este nombre externo en vez del nombre BBK (para naming customizado o llamadas a procs en otros lenguajes)

### Prototype-level modifiers

```bnf
pr_modifier ::= extpgm_modifier
              | extproc_modifier
              | KW_OPDESC
              | KW_RTNPARM

extpgm_modifier ::= KW_EXTPGM LPAREN STR_LIT RPAREN {pin=1}
```

- `EXTPGM("PGMNAME")` — el prototipo es para un programa IBM i `*PGM` externo (no un sub-procedure). Se llama con `CALL` en RPG legacy, en BBK es transparente.
- `EXTPROC("name")` — idem que en proc_modifier
- `OPDESC` — pasa operational descriptors (metadata de tipos) junto con args
- `RTNPARM` — convención especial donde el "return" se devuelve por un parámetro implícito

### Parameter modifiers

```bnf
param_modifier ::= KW_VALUE
                 | KW_CONST
                 | KW_OPDESC
                 | options_modifier

options_modifier ::= KW_OPTIONS LPAREN STAR_IDENT (COLON STAR_IDENT)* RPAREN {pin=1}
```

- `VALUE` — pass by value (default en BBK es by-reference)
- `CONST` — by-reference inmutable: permite pasar literales/expresiones que el callee no puede modificar
- `OPDESC` — pasa operational descriptor de este parámetro
- `OPTIONS(*NOPASS:*OMIT:*VARSIZE:*STRING:*NULLIND)` — flags adicionales (parámetro opcional, omitible, tamaño variable, conversión string, soporte null)

---

## Ejemplos completos

### Válido — `05-procedures.bbk`

```bbk
CTL-OPT MAIN(proceduresDemo);

// Forward prototypes
DCL-PR sum(a INT(10) VALUE, b INT(10) VALUE) -> INT(10);
DCL-PR greet(name VARCHAR(50) CONST);
DCL-PR factorial(n INT(10) VALUE) -> INT(10);
DCL-PR CUSTPROG(customerId INT(10) VALUE, status INT(10)) EXTPGM("CUSTPROG");

DCL-PROC proceduresDemo {
  DCL-S total      INT(10);
  DCL-S factResult INT(10);
  DCL-S custStatus INT(10);

  total = sum(5, 7);                       // statement: unknown_proc_item
  print("5 + 7 = " + char(total));         // statement: unknown_proc_item
  greet("World");                          // statement: unknown_proc_item
  factResult = factorial(5);               // statement: unknown_proc_item
  CUSTPROG(12345, custStatus);             // statement: unknown_proc_item
}

DCL-PROC sum(a INT(10) VALUE, b INT(10) VALUE) -> INT(10) EXPORT {
  return a + b;                            // statement: unknown_proc_item
}

DCL-PROC factorial(n INT(10) VALUE) -> INT(10) {
  if (n <= 1) {                            // statement: unknown_proc_item
    return 1;
  }
  return n * factorial(n - 1);
}
```

Layer 3 reconoce toda la estructura externa (CTL-OPT, prototipos, definiciones, params, return types, modifiers, body delimiters, declaraciones internas). Las líneas marcadas como `// statement` se consumen como tokens individuales vía `unknown_proc_item` sin generar errores ni PSI estructurado — esa es la responsabilidad de Layer 4.

### Errores detectados — `bad-12-bad-procedures.bbk`

| Caso | Línea aproximada |
|---|---|
| `DCL-PR;` — falta nombre | Prototype sin IDENT |
| `DCL-PR sum(;` — params sin cerrar | Unterminated args |
| `DCL-PR sum(a);` — param sin tipo | IDENT solo, sin type_specification |
| `DCL-PR sum() ->;` — return type vacío | ARROW sin TYPE |
| `DCL-PR foo EXTPGM(MyProgram);` — tipo de arg | EXTPGM expects STR_LIT, got IDENT |
| `DCL-PROC main;` (sin body) | Falta `{ ... }` |
| `DCL-PROC main(a INT(10),) {` | Comma colgante en params |
| `DCL-PROC body1 { ... ` sin `}` | Body sin cerrar |
| `DCL-S x;` adentro de body | Error de L1 (missing type) reportado correctamente dentro del body |

**Casos que NO marcan errores** (porque caen al fallback de body):
- `total = 5 + 7;` — statement no implementado
- `print("anything");` — call no implementado
- `if (x > 0) { doSomething(); }` — control flow no implementado

Eso es lo correcto para L3 — Layer 4 los marcará.

---

## Notas de implementación

### El fallback `unknown_proc_item`

```bnf
private unknown_proc_item ::= !RBRACE !<<eof>> any_token
```

La doble negación `!RBRACE !<<eof>>` es la clave del diseño:
- `!RBRACE` — lookahead negativo: solo procede si el próximo token NO es `}`
- `!<<eof>>` — lookahead negativo para EOF

Sin esto, el fallback consumiría el `}` final y el `RBRACE` requerido por `procedure_body` nunca se encontraría, dando un error confuso al final del archivo.

### Pin position en `inline_param`

```bnf
inline_param ::= IDENT type_specification param_modifier* {pin=2}
```

`pin=2` significa "pin después de los dos primeros elementos" (IDENT + type_specification). Esto es importante porque:

- **Si solo se ve un IDENT** (sin type): backtrack, no error. Esto permite que un IDENT colgado en un contexto raro (poco probable, pero defensive) no force errores.
- **Una vez vista la combinación `IDENT TYPE`**: commit. Si después falta `)` o aparece un token raro, se reporta error.

### `LIKEDS` en parámetros (uniforme con DCL-S)

Los parámetros usan la misma `type_specification` que `DCL-S`, así que `LIKEDS(...)`, `LIKEREC(...)`, `LIKE(...)` funcionan idénticamente:

```bbk
DCL-PR processOrder(rec LIKEDS(orderRec) CONST);
DCL-PR processArray(arr LIKE(myArray) VALUE);
```

### Diferencia EXTPGM vs EXTPROC

Ambos toman `(STR_LIT)`. Sólo se permiten en distintos contextos:

| Modifier | Donde se permite | Significado |
|---|---|---|
| `EXTPGM` | Solo en `DCL-PR` (no en DCL-PROC) | El prototipo es para un programa externo `*PGM` |
| `EXTPROC` | Tanto en `DCL-PR` como `DCL-PROC` | Nombre externo de la procedure (distinto del nombre BBK) |

Esto se refleja en la BNF: `pr_modifier` incluye ambos, `proc_modifier` solo `extproc_modifier`.

### Procedure body NO permite ciertos top-level

Dentro de un `procedure_body`, **no** se permiten:
- `DCL-F` (archivos son globales del módulo)
- `DCL-PR` (prototipos van al nivel módulo)
- `DCL-PROC` (RPG no soporta nested procedures)
- `CTL-OPT` (es directiva de módulo)

Esto está reflejado en `proc_body_item` que solo incluye `variable_declaration | constant_declaration | data_structure_declaration | unknown_proc_item`. Si alguien intenta `DCL-PROC nested { DCL-PROC inner { ... } }`, el `DCL-PROC` interno cae a `unknown_proc_item` y eventualmente puede confundir el cierre (el `{` del inner abre un block que se traga el `}` del outer). Layer 4 podría mejorar esto agregando un error explícito.

---

## Próxima capa

`layer4.md` (pendiente) — agregará **statements**: `if`/`else`, `while`, `do/while`, `for`, `select`/`when`/`other`, `return`, `break`, `continue`, asignaciones (`x = expr;`), y llamadas como statement (`f(args);`).

Layer 5 después agregará **expressions** con toda la jerarquía de precedencia (aritméticos, comparación, lógicos, bitwise, ternario, member access, subscript, function calls como expresión).
