# BBK Grammar — Layer 4

**Estado:** implementada y verificada
**Fuente del parser:** [`plugin-bbk/src/main/grammar/BBK.bnf`](../../../plugin-bbk/src/main/grammar/BBK.bnf)
**Pre-requisitos:** [`layer1.md`](layer1.md), [`layer2.md`](layer2.md), [`layer3.md`](layer3.md)
**Archivos de prueba:** [`05-procedures.bbk`](../../../tests/boxbreaker/examples/05-procedures.bbk), [`04-control-flow.bbk`](../../../tests/boxbreaker/examples/04-control-flow.bbk) (válidos), [`bad-13-bad-statements.bbk`](../../../tests/boxbreaker/examples/bad/bad-13-bad-statements.bbk) (errores)

---

## Alcance

Layer 4 es la **capa más grande** hasta ahora. Trae los dos componentes que hacen al lenguaje "ejecutable":

### Statements
| Construcción | Forma |
|---|---|
| `if/else if/else` | `if (cond) { ... } else if (cond) { ... } else { ... }` |
| `select/when/other` | `select { when (cond) { ... } when (cond) { ... } other { ... } }` |
| `while` | `while (cond) { ... }` |
| `do/while` | `do { ... } while (cond);` |
| `for` (con DCL-S inline opcional) | `for (DCL-S i INT(10) = 0; i < 10; i += 1) { ... }` |
| `break`, `continue` | `break;` / `continue;` |
| `return` | `return;` o `return expr;` |
| `monitor`/`on-error`/`on-exit` | `monitor { ... } on-error (404, 405) { ... } on-exit { ... }` |
| Assignment | `lvalue = expr;` (también `+=`, `-=`, etc.) con `@halfup` opcional |
| Expression statement | `f(args);` o cualquier expresión seguida de `;` |

### Expressions
Jerarquía completa de precedencia (14 niveles, de menor a mayor):

```
ternary           ?: (right-assoc)
logical_or        ||
logical_and       &&
bitwise_or        |
bitwise_xor       ^
bitwise_and       &
equality          ==  !=
relational        <  >  <=  >=
shift             <<  >>
additive          +  -
multiplicative    *  /  %
power             **  (right-assoc)
unary             +  -  !  ~  (prefix)
postfix           ()  []  .  ->
primary           literal | IDENT | (expr) | true | false | null | *IDENT
```

**Cambio importante de patrón:** Layer 4 unifica el bloque `{ ... }`. La regla `procedure_body` de Layer 3 desaparece; ahora `procedure_declaration` usa directamente `block_statement` (la misma regla que usan `if`, `while`, `for`, etc.). Cualquier bloque permite mezclar declaraciones locales (`DCL-S`/`DCL-C`/`DCL-DS`) y statements.

**Lo que sigue sin cubrirse:**
- File ops (`read`, `chain`, `write`, `setll`, `exfmt`, etc.) — Layer 6
- Subroutines (`BEGSR`/`ENDSR`/`EXSR`) — Layer 6
- Directivas `PRE-*` — Layer 6

---

## Producciones BNF — clave

### Bloque unificado

```bnf
block_statement ::= LBRACE block_item* RBRACE {pin=1}

block_item ::= variable_declaration
             | constant_declaration
             | data_structure_declaration
             | statement
             | unknown_block_item

private unknown_block_item ::= !RBRACE !<<eof>> any_token
```

El `unknown_block_item` con `!RBRACE !<<eof>>` es el truco que permite que **construcciones aún no soportadas** (file ops, EXSR, etc.) **no generen errores** dentro de bloques. El parser las consume token por token sin reportar errores hasta que Layer 6 las parsee correctamente.

### Statement principal

```bnf
statement ::= if_statement
            | select_statement
            | while_statement
            | do_while_statement
            | for_statement
            | break_statement
            | continue_statement
            | return_statement
            | monitor_statement
            | expression_statement
```

Cada statement tiene `{pin=1}` después del keyword distintivo (`KW_IF`, `KW_WHILE`, etc.). Eso es lo que permite reportar errores específicos:
- `if x > 0 { }` → "LPAREN expected, got IDENT"
- `while () { }` → "expression expected, got RPAREN"
- `for ; ; ;` → "LPAREN expected, got SEMI"

### Assignment + expression statement (caso especial)

```bnf
expression_statement ::= expression assignment_tail? SEMI

private assignment_tail ::= assignment_op expression attribute_modifier? {pin=1}
```

Esto unifica dos cosas que en otros lenguajes son separadas:
- `f(args);` → expression seguido de `;`
- `total = a + b @halfup;` → expression (lvalue) + `=` + expression + modifier + `;`

**Sutil pero importante:** `expression_statement` **no tiene pin**. El `assignment_tail` **sí tiene pin** internamente.

¿Por qué? Compromiso entre dos objetivos:
- **Detectar `total = ;`** (assignment sin valor): cuando el `=` se consume y la expresión derecha falla, el pin de `assignment_tail` engaja y reporta error. ✓
- **No marcar falsos errores en file ops como `read customers;`**: el parser intenta expression_statement, matchea `read` como expresión, no encuentra `=` (no entra el optional), espera `;`, obtiene IDENT, falla sin pin, hace backtrack limpio, y el fallback `unknown_block_item` consume todo silenciosamente. ✓

Cuando Layer 6 agregue file ops como statements propios, se matchearán antes de expression_statement y este trade-off se vuelve innecesario.

### Expression hierarchy

```bnf
expression ::= ternary_expression

ternary_expression ::= logical_or_expression (QUESTION expression COLON ternary_expression)?

logical_or_expression  ::= logical_and_expression (PIPE_PIPE logical_and_expression)*
logical_and_expression ::= bitwise_or_expression (AMP_AMP bitwise_or_expression)*
// ... (8 más en el medio)
power_expression       ::= unary_expression (STAR_STAR power_expression)?  // right-assoc
unary_expression       ::= (PLUS | MINUS | BANG | TILDE) unary_expression
                         | postfix_expression
postfix_expression     ::= primary postfix_suffix*

primary ::= literal | KW_TRUE | KW_FALSE | KW_NULL | STAR_IDENT | IDENT | LPAREN expression RPAREN

postfix_suffix ::= LPAREN argument_list? RPAREN
                 | LBRACKET subscript_list RBRACKET
                 | DOT IDENT
                 | ARROW IDENT
```

**Notas:**
- **Right-associative**: ternary (`a ? b : c ? d : e` = `a ? b : (c ? d : e)`) y power (`2 ** 3 ** 4` = `2 ** (3 ** 4)`)
- **Left-associative**: todo lo demás (sigue el patrón estándar `lhs (OP rhs)*`)
- **Subscript con coma**: `arr[i, j]` para multi-dim (no `arr[i][j]`), siguiendo decisión #8 de `grammar.md`
- **Calls como lvalue**: sintácticamente `f().field = x` es válido. El type checker semántico debe verificar que `f()` retorne una referencia (futuro)

### `for` con declaración inline

```bnf
for_statement ::= KW_FOR LPAREN for_init? SEMI expression? SEMI for_update? RPAREN block_statement {pin=1}

for_init ::= for_inline_decl | for_assignment | expression
for_update ::= for_assignment | expression
for_inline_decl ::= KW_DCL_S IDENT type_specification EQ expression {pin=1}
for_assignment ::= lvalue assignment_op expression {pin=2}
```

Ejemplos válidos:
```bbk
for (i = 0; i < 10; i += 1) { ... }                        // assignment-based
for (DCL-S j INT(10) = 0; j < 100; j += 1) { ... }         // inline declaration
```

### `select/when/other`

```bnf
select_statement ::= KW_SELECT LBRACE when_clause+ other_clause? RBRACE {pin=1}
when_clause      ::= KW_WHEN LPAREN expression RPAREN block_statement {pin=1}
other_clause     ::= KW_OTHER block_statement {pin=1}
```

`when_clause+` (uno o más) — un `select` vacío `select { }` falla. Esto difiere de C `switch` que sí acepta switch vacío.

### `monitor/on-error/on-exit`

```bnf
monitor_statement ::= KW_MONITOR block_statement on_error_clause* on_exit_clause? {pin=1}
on_error_clause   ::= KW_ON_ERROR status_list? block_statement {pin=1}
on_exit_clause    ::= KW_ON_EXIT block_statement {pin=1}
status_list       ::= LPAREN expression (COMMA expression)* RPAREN {pin=1}
```

Cero o más `on-error` clauses (cada una con lista opcional de status codes), y cero o un `on-exit` final.

---

## Ejemplos completos

### Válido — `04-control-flow.bbk`

```bbk
DCL-PROC controlFlowDemo {
  DCL-S counter INT(10) INZ(0);
  DCL-S status  CHAR(1);
  DCL-S sum     INT(10) INZ(0);

  if (counter == 0) {
    print("Zero");
  } else if (counter < 10) {
    print("Small");
  } else {
    print("Large");
  }

  status = "A";
  select {
    when (status == "A") {
      print("Active");
    }
    when (status == "I" || status == "P") {
      print("Inactive or Pending");
    }
    other {
      print("Unknown status");
    }
  }

  while (counter < 5) {
    counter += 1;
  }

  do {
    counter -= 1;
  } while (counter > 0);

  for (DCL-S i INT(10) = 0; i < 10; i += 1) {
    sum += i;
  }

  for (DCL-S j INT(10) = 0; j < 100; j += 1) {
    if (j == 50) {
      break;
    }
    if (j % 2 == 0) {
      continue;
    }
    sum += j;
  }
}
```

Layer 4 reconoce y estructura todo esto: cada bloque, cada condición, cada llamada a `print()`, cada assignment con operadores compuestos.

### Errores detectados — `bad-13-bad-statements.bbk`

| Categoría | Casos |
|---|---|
| **if/else** | `if x > 0 { }` (faltan parens), `if (x > 0) doSomething();` (faltan llaves), `if () { }` (condición vacía), `else` sin body |
| **while** | Faltan parens, condición vacía, sin body |
| **do/while** | Faltan parens en while, sin llaves, sin `;` final |
| **for** | Sin parens, sin `;` separadores, inline decl sin valor inicial |
| **select** | `select { }` (requiere `when+`), `when` fuera de `select` |
| **return/break/continue** | Sin `;` final |
| **assignments** | `x = ;` (sin rhs), `= 5;` (sin lvalue), `x +=;` (compound op sin rhs) |
| **expressions** | `5 +;` (additive incompleto), `(5 + 7;` (parens sin cerrar), `foo(;` (call args), `arr[;` (subscript), `a ? b;` (ternary sin `:` y else) |
| **monitor** | Sin body, on-error sin body, trailing comma en status list |

---

## Notas de implementación

### El truco del bloque unificado

Antes de L4, `procedure_body`, `ds_body` y cualquier otro `{ ... }` se definían como reglas separadas. Layer 4 introduce `block_statement` que todos comparten:

- `procedure_declaration ::= ... block_statement {pin=1}`
- `if_statement ::= KW_IF LPAREN expression RPAREN block_statement ...`
- `while_statement ::= KW_WHILE LPAREN expression RPAREN block_statement {pin=1}`
- `monitor_statement ::= KW_MONITOR block_statement ...`
- etc.

Beneficio: cualquier mejora a `block_statement` (ej. agregar nuevos tipos de declaration que se pueden hacer adentro) se propaga automáticamente a todos los contextos. `ds_body` se mantiene separado porque sus items son subfields, no statements.

### Por qué `expression_statement` no tiene pin pero `assignment_tail` sí

Discutido arriba en la sección de producciones. Resumen:
- Pin en `expression_statement` → file ops como `read customers;` generan falsos errores ("SEMI expected, got IDENT")
- Sin pin → assignments malformados como `total = ;` no se detectan
- Solución: pin solo en `assignment_tail` (la parte `= expression`)

Cuando Layer 6 agregue file ops como reglas propias, podríamos agregar el pin a `expression_statement` sin problemas.

### Llaves obligatorias

Decisión cerrada (de `grammar.md` #4): **todos los bloques requieren `{ }`**, incluso para un solo statement. No se acepta `if (cond) doStuff();` — debe ser `if (cond) { doStuff(); }`.

Esto evita el clásico bug del C donde:
```c
if (cond)
    statement1();
    statement2();  // siempre ejecuta, NO está en el if
```

### `lvalue` permissivo

```bnf
lvalue ::= postfix_expression
```

Sintácticamente, cualquier postfix_expression puede aparecer del lado izquierdo de `=`. Eso incluye:
- `x = ...` (simple IDENT)
- `ds.field = ...` (member access)
- `arr[i] = ...` (subscript)
- `arr[i, j] = ...` (multi-dim)
- `ptr->field = ...` (pointer deref)
- `f(x).field = ...` (call result, decisión #9)

La validación de "es realmente assignable" la haría el type checker en una fase semántica futura.

### Conflictos potenciales con file ops

Con Layer 4 instalado pero **sin Layer 6 todavía**, expresiones como:

```bbk
read customers customerRec;
chain key file rec;
exfmt screenFormat;
```

caen en `unknown_block_item` token-por-token. No generan errores ni structure-view items. Layer 6 los reemplazará por `file_op_statement` con error reporting completo.

Mientras tanto, los archivos como `10-files.bbk` que usan file ops dentro de monitor blocks **parsean sin errores** (todo lo que no es declaration o statement reconocido cae al fallback silente). Eso es lo deseado para mantener la experiencia limpia entre Layers.

---

## Próxima capa

`layer5.md` — refinamientos de expressions si los hay, y posiblemente attribute modifiers extendidos.

`layer6.md` — file operations (`read`, `chain`, `write`, etc.), subroutines (`BEGSR`/`EXSR`), directivas (`PRE-*`).
