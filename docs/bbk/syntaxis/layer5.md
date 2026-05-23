# BBK Grammar — Layer 5

**Estado:** implementada y verificada
**Fuente del parser:** [`plugin-bbk/src/main/grammar/BBK.bnf`](../../../plugin-bbk/src/main/grammar/BBK.bnf)
**Fuente del lexer:** [`plugin-bbk/src/main/grammar/BBK.flex`](../../../plugin-bbk/src/main/grammar/BBK.flex) (19 tokens nuevos agregados en esta capa)
**Pre-requisitos:** [`layer1.md`](layer1.md), [`layer2.md`](layer2.md), [`layer3.md`](layer3.md), [`layer4.md`](layer4.md)
**Archivos de prueba:** [`10-files.bbk`](../../../tests/boxbreaker/examples/10-files.bbk) (válido — usa file ops y monitor), [`bad-14-bad-file-ops-and-subroutines.bbk`](../../../tests/boxbreaker/examples/bad/bad-14-bad-file-ops-and-subroutines.bbk) (errores)

---

## Reframing de scope

En `layer4.md` había dejado L5 vaga ("refinamientos") porque ya había metido casi todo en L4. **Layer 5 se reorientó** para completar lo que todavía caía al fallback dentro de procedure bodies: file ops, subroutines y CALLP. Las directivas top-level (`PRE-*`) se mueven a Layer 6.

## Alcance

| Construcción | Forma | Ejemplo |
|---|---|---|
| File ops (acceso) | `read file [ds];` `chain key file [ds];` `setll key file;` `reade key file [ds];` etc. | `chain custId customers customerRec;` |
| File ops (escritura) | `write target [ds];` `update target [ds];` `delete [key] file;` | `write orders orderRec;` |
| File ops (ciclo de vida) | `open file;` `close file;` `unlock file;` `exfmt format [ds];` | `open report;` |
| Subroutine definition | `BEGSR name; ... ENDSR [name];` | Ver abajo |
| Subroutine call | `EXSR name;` | `EXSR validate;` |
| Salir de subroutine | `LEAVESR;` | `LEAVESR;` |
| CALLP explícito | `CALLP procName(args);` | `CALLP CUSTPROG(custId, status);` |

**Lo que sigue sin cubrirse:**
- Directivas `PRE-*` — **Layer 6**
- BIFs `%X(...)` con su sintaxis especial — Layer 6 o queda como mejora futura del lexer

---

## Tokens nuevos (19)

Layer 5 requiere agregar nuevos tokens al lexer JFlex porque antes no existían:

```
// File operations (14)
KW_READ      "read"
KW_READE     "reade"
KW_READP     "readp"
KW_READPE    "readpe"
KW_CHAIN     "chain"
KW_SETLL     "setll"
KW_SETGT     "setgt"
KW_WRITE     "write"
KW_UPDATE    "update"
KW_DELETE    "delete"
KW_UNLOCK    "unlock"
KW_OPEN      "open"
KW_CLOSE     "close"
KW_EXFMT     "exfmt"

// Subroutines (4)
KW_BEGSR     "BEGSR"
KW_ENDSR     "ENDSR"
KW_EXSR      "EXSR"
KW_LEAVESR   "LEAVESR"

// Call as statement (1)
KW_CALLP     "CALLP"
```

**Trade-off:** los nombres son ahora keywords reservados. Si un usuario tenía una variable llamada `read`, `chain`, `write`, etc., ya no puede. Aceptable porque son nombres semánticamente cargados en RPG.

Convención: file ops en minúsculas (estilo C: `read`, `write`), subroutines en mayúsculas (estilo RPG: `BEGSR`, `EXSR`). Recordá que **BBK es case-insensitive** (`READ` = `read` = `Read`), pero la convención visual ayuda a leer.

---

## Producciones BNF

### File operation statements

```bnf
file_op_statement ::= read_op | reade_op | readp_op | readpe_op
                    | chain_op | setll_op | setgt_op
                    | write_op | update_op | delete_op | unlock_op
                    | open_op | close_op | exfmt_op

// Sequential / random access reads
read_op    ::= KW_READ    IDENT IDENT? SEMI {pin=1}                // read file [ds]
readp_op   ::= KW_READP   IDENT IDENT? SEMI {pin=1}                // read prior
reade_op   ::= KW_READE   expression IDENT IDENT? SEMI {pin=1}     // reade key file [ds]
readpe_op  ::= KW_READPE  expression IDENT IDENT? SEMI {pin=1}     // read equal prior

// Random access by key
chain_op   ::= KW_CHAIN   expression IDENT IDENT? SEMI {pin=1}     // chain key file [ds]
setll_op   ::= KW_SETLL   expression IDENT SEMI {pin=1}            // setll key file
setgt_op   ::= KW_SETGT   expression IDENT SEMI {pin=1}            // setgt key file

// Updates / inserts
write_op   ::= KW_WRITE   IDENT IDENT? SEMI {pin=1}                // write format_or_file [ds]
update_op  ::= KW_UPDATE  IDENT IDENT? SEMI {pin=1}                // update format_or_file [ds]
delete_op  ::= KW_DELETE  expression? IDENT SEMI {pin=1}           // delete [key] file

// File lifecycle
unlock_op  ::= KW_UNLOCK  IDENT SEMI {pin=1}
open_op    ::= KW_OPEN    IDENT SEMI {pin=1}
close_op   ::= KW_CLOSE   IDENT SEMI {pin=1}
exfmt_op   ::= KW_EXFMT   IDENT IDENT? SEMI {pin=1}                // exfmt format [ds]
```

**Notas de diseño:**
- **`expression` para keys** en lugar de `IDENT`: permite usar literales, BIFs futuros (`%KDS(...)`), expresiones aritméticas, etc. como key. `IDENT IDENT?` para nombres de archivo / DS de resultado.
- **`IDENT?` para result DS opcional**: para `chain key file;` (lee a variables globales del programa) o `chain key file ds;` (lee a la DS dada).
- **`delete expression? IDENT`**: el key es opcional porque `DELETE` también puede usarse en el record actual (sin key) o con key explícito.
- **Pin después del keyword** en todas las ops: una vez visto `read`/`chain`/etc., el parser se compromete y reporta errores específicos.

### Subroutines

```bnf
subroutine_definition ::= KW_BEGSR IDENT SEMI sr_item* KW_ENDSR IDENT? SEMI {pin=1}

sr_item ::= variable_declaration
          | constant_declaration
          | data_structure_declaration
          | statement
          | unknown_sr_item

// SR body fallback: stop at ENDSR (closes this SR), RBRACE (parent block), or EOF.
private unknown_sr_item ::= !KW_ENDSR !RBRACE !<<eof>> any_token

exsr_statement    ::= KW_EXSR IDENT SEMI {pin=1}
leavesr_statement ::= KW_LEAVESR SEMI {pin=1}
```

**Diferencias con DCL-PROC:**
- **No usa llaves** `{ }`. Delimitado por `BEGSR ... ENDSR`. Esto es la sintaxis RPG histórica preservada.
- El nombre repetido después de ENDSR es opcional: `BEGSR mySR; ... ENDSR mySR;` o solo `ENDSR;`. Mejora claridad pero no es obligatorio.
- Una subroutine es un `block_item` (vive dentro de un procedure body), no un `top_level_item`.

**El truco de `unknown_sr_item`:**
- `!KW_ENDSR` — para detener antes de que ENDSR cierre la SR
- `!RBRACE` — para que si falta ENDSR no consuma el `}` del procedure padre
- `!<<eof>>` — para detenerse en EOF

Esta triple negación asegura que un BEGSR mal cerrado dé un error claro ("ENDSR expected") y no se coma el resto del archivo.

### CALLP (call-as-statement explícito)

```bnf
callp_statement ::= KW_CALLP postfix_expression SEMI {pin=1}
```

**Forma:** `CALLP procName(args);`

CALLP es opcional: una llamada como `myProc(a, b);` también funciona vía `expression_statement` (la expresión es la llamada, y el `;` la termina). CALLP existe para casos donde:
- Querés ser explícito de que es una llamada (legibilidad)
- Hay ambigüedad sintáctica (raros en BBK porque las llamadas tienen `()`)
- Compatibilidad con RPG donde CALLP era requerido

### Integración con statement y block_item

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
            | file_op_statement       // L5
            | exsr_statement          // L5
            | leavesr_statement       // L5
            | callp_statement         // L5
            | expression_statement

block_item ::= variable_declaration
             | constant_declaration
             | data_structure_declaration
             | subroutine_definition  // L5
             | statement
             | unknown_block_item
```

**Orden de alternativas en `statement`:** las file ops y EXSR/LEAVESR/CALLP van **antes** de expression_statement. Esto es importante porque `read customers;` antes de L5 (cuando no había KW_READ) caía como expression_statement con falso error "SEMI expected". Ahora matchea como `read_op` primero. ✓

---

## Ejemplos

### Válido — `10-files.bbk`

```bbk
DCL-F customers DISK USAGE(*INPUT) KEYED EXTNAME("CUSTOMER");
DCL-F orders    DISK USAGE(*INPUT:*OUTPUT:*UPDATE) KEYED EXTNAME("ORDER");
DCL-F report    PRINTER USAGE(*OUTPUT) USROPN;

DCL-DS customerRec EXTNAME("CUSTOMER") QUALIFIED;

DCL-DS orderRec QUALIFIED {
  orderId    INT(10);
  customerId INT(10);
  amount     PACKED(11:2);
  orderDate  DATE;
}

DCL-PR processOrder(rec LIKEDS(orderRec) CONST);

DCL-PROC filesDemo {
  DCL-S targetCustomerId INT(10) INZ(12345);
  DCL-S customerFound    BOOL;

  monitor {
    open report;

    chain targetCustomerId customers customerRec;
    customerFound = (status() == 0);

    if (customerFound) {
      print("Customer: " + trim(customerRec.name));

      setll targetCustomerId orders;
      reade targetCustomerId orders orderRec;
      while (status() == 0) {
        processOrder(orderRec);
        reade targetCustomerId orders orderRec;
      }
    } else {
      print("Customer not found");
    }

    orderRec.orderId    = 99999;
    orderRec.customerId = targetCustomerId;
    orderRec.amount     = 150.00d;
    orderRec.orderDate  = currentDate();
    write orders orderRec;

  } on-error (404, 405) {
    print("File access error: code " + char(status()));
  } on-exit {
    close report;
  }
}
```

Layer 5 reconoce y estructura completamente: file ops (`open`, `chain`, `setll`, `reade`, `write`, `close`), control flow (`if`, `while`), expressions (`status() == 0`), assignment con member access (`orderRec.orderId = 99999`), llamadas como statement (`print(...)`, `processOrder(orderRec)`).

### Ejemplo con subroutines

```bbk
DCL-PROC oldSchoolStyle {
  DCL-S counter INT(10) INZ(0);

  // Llamada a subroutine
  EXSR validate;
  EXSR process;

  return;

  // Definición de subroutines
  BEGSR validate;
    if (counter < 0) {
      LEAVESR;   // salir temprano de la SR
    }
    counter += 1;
  ENDSR validate;

  BEGSR process;
    DCL-S temp INT(10);  // local a la SR (en realidad escope del proc, pero idea)
    temp = counter * 2;
    print(char(temp));
  ENDSR;   // ENDSR sin nombre repetido también válido
}
```

### Errores detectados — `bad-14-bad-file-ops-and-subroutines.bbk`

| Categoría | Casos |
|---|---|
| File ops sin args mínimos | `read;`, `chain;`, `setll;`, `write;`, `open;` |
| File ops sin `;` | `read customers customerRec`, `chain key`, `delete` |
| File ops parciales | `setll *START;` (sin file), `reade ;` (sin key), `chain ( orders;` (parens) |
| Subroutine def sin nombre | `BEGSR;` |
| Subroutine sin `;` después del nombre | `BEGSR mySR` |
| Subroutine sin cierre | `BEGSR mySR; ... ` (sin ENDSR) |
| ENDSR sin `;` | `ENDSR otherSR` (falta `;`) |
| EXSR sin nombre / sin `;` | `EXSR;`, `EXSR mySR` |
| LEAVESR sin `;` | `LEAVESR` |
| CALLP sin call | `CALLP;` |
| CALLP sin parens o sin `;` | `CALLP myProc`, `CALLP myProc(a, b)` |

---

## Notas de implementación

### Convención de keywords case (recordatorio)

BBK es case-insensitive en el lexer (`%ignorecase` en BBK.flex). Por eso `READ` = `read` = `Read` = `rEaD` desde el punto de vista del parser. La convención visual sugerida:

- **File ops en minúsculas** (`read`, `chain`, `write`) — más cerca de C, más fluido al mezclar con expresiones.
- **Declarations en mayúsculas con guión** (`DCL-S`, `BEGSR`, `ENDSR`) — más cerca de RPG, más reconocible como "estructura del programa".

Pero técnicamente cualquier capitalización es válida.

### Por qué `expression` (no `IDENT`) para keys

```bnf
chain_op ::= KW_CHAIN expression IDENT IDENT? SEMI {pin=1}
```

El primer arg (el key) es `expression`, no `IDENT`. Permite:
- `chain custId customers ds;` (key = IDENT simple)
- `chain *START orders;` (key = STAR_IDENT figurativo)
- `chain 12345 customers;` (key = literal)
- Futuro: `chain %KDS(myKeysDS) customers;` cuando agreguemos BIFs

Pero `expression` ES greedy. Para `chain x customers;`, el parser:
- expression: `x` (primary IDENT). Postfix? No LPAREN/etc. después.
- Sigue por additive, etc. Nada matchea. Expression termina con solo `x`.
- IDENT `customers` ✓.
- SEMI ✓.

OK funciona. Pero para `chain x + y customers;` (key calculada):
- expression: `x` then `+` then `y` (additive expression) = `x + y`.
- IDENT `customers` ✓.

También funciona.

### Subroutines como sub-bloque dentro de procedures

Las subroutines son legacy RPG. En BBK las soportamos pero la convención moderna sería usar sub-procedures (DCL-PROC) en su lugar. La diferencia clave:

| Aspecto | Subroutine (BEGSR/ENDSR) | Sub-procedure (DCL-PROC) |
|---|---|---|
| Scope | Comparte variables con el proc padre | Tiene su propio scope |
| Parámetros | No tiene | Sí, tipados |
| Return value | No | Sí, opcionalmente |
| Llamada | `EXSR name;` | `name(args);` |
| Recomendación moderna | Evitar para código nuevo | Preferir |

BBK las soporta para compatibilidad con RPG legacy. Cuando el frontend lo traduzca, podría convertirlas a procedures auto-generadas con scope explícito.

### Edge case: BEGSR antes del statement que lo llama

En RPG (y BBK por extensión), el orden es libre:
```bbk
DCL-PROC main {
  EXSR helper;       // llamada antes de la definición
  return;

  BEGSR helper;
    print("hello");
  ENDSR;
}
```

El parser de L5 acepta esto sin problema — la SR puede definirse en cualquier parte del body. La validación de "EXSR refiere a una SR existente" es semántica, no sintáctica.

---

## Próxima capa

`layer6.md` — **directivas de preprocesador** (`PRE-IF`, `PRE-ELSEIF`, `PRE-ELSE`, `PRE-ENDIF`, `PRE-DEFINE`, `PRE-UNDEFINE`, `PRE-INCLUDE`, `PRE-EOF`). Tienen su propia sub-gramática y se procesan idealmente antes que el parser principal, pero por simplicidad las podemos integrar como statements top-level con sintaxis especial.

Eventualmente también: BIFs con sintaxis `%X(args)` que requieren un nuevo token para `%IDENT`.
