# Gramática sintáctica de BoxBreaker (BBK) — propuesta tentativa

**Estado:** borrador. Depende de las decisiones en [`tokens.md`](tokens.md) — si alguna cambia, la gramática se ajusta.

**Convenciones de notación:** las mismas que en [`../c99-grammar.md`](../c99-grammar.md):

- `<no-terminal>` — categoría sintáctica.
- `literal` — terminal exacto (token).
- `a | b` — alternativa.
- `a?` — opcional.
- `{ a }` — repetición (0 o más).
- `one of:` — alternativa simple entre terminales.

**Tokens referenciados:** definidos en [`tokens.md`](tokens.md). En esta gramática aparecen por su forma léxica (ej. `DCL-S`, `;`, `IDENT`).

---

## 1. Translation unit (nivel top)

Un archivo BBK es una secuencia de declaraciones top-level, opcionalmente precedidas por una directiva de módulo.

```
<translation-unit>
    : { <directive> }*
      <ctl-opt-statement>?
      { <directive> | <file-declaration> }*
      { <directive> | <module-data-declaration> }*
      { <directive> | <prototype-declaration> }*
      { <directive> | <procedure-declaration> }*

<module-data-declaration>
    : <constant-declaration>
    | <variable-declaration>
    | <data-structure-declaration>
```

**Notas:**
- **Orden forzado** de declaraciones top-level (decisión cerrada): primero `CTL-OPT`, después `DCL-F`, después `DCL-C`/`DCL-S`/`DCL-DS` (libres entre sí), después `DCL-PR`, después `DCL-PROC`. Equivalente a la convención RPG (H → F → D → P).
- Las directivas (`PRE-IF`, `PRE-INCLUDE`, etc.) pueden aparecer en cualquier punto sin romper el orden.

---

## 2. Module directive: CTL-OPT

```
<ctl-opt-statement>
    : CTL-OPT { <ctl-opt-keyword> }* ;

<ctl-opt-keyword>
    : IDENT                                     // ej. NOMAIN, DEBUG
    | IDENT ( <ctl-opt-arg-list> )              // ej. DFTACTGRP(*NO)

<ctl-opt-arg-list>
    : <ctl-opt-arg>
    | <ctl-opt-arg-list> : <ctl-opt-arg>

<ctl-opt-arg>
    : IDENT
    | <literal>
    | * IDENT                                   // figurative argument, ej. *NO, *NEW
```

Ejemplo:

```bbk
CTL-OPT NOMAIN DFTACTGRP(*NO) ACTGRP("BBKTEST") DEBUG;
```

---

## 3. Declaraciones de variables, constantes y tipos

### 3.1 Variable standalone (DCL-S)

```
<variable-declaration>
    : DCL-S IDENT <type-specification> { <var-modifier> }* ;

<type-specification>
    : <primitive-type>
    | <like-reference>

<primitive-type>
    : <type-keyword>
    | <type-keyword> ( <type-arg-list> )

<type-keyword> one of:
    CHAR  VARCHAR  PACKED  ZONED  BINDEC
    INT   UNS      FLOAT
    DATE  TIME     TIMESTAMP
    BOOL  POINTER  VOID

<type-arg-list>
    : <integer-literal>
    | <integer-literal> : <integer-literal>

<like-reference>
    : LIKE ( IDENT )
    | LIKEDS ( IDENT )
    | LIKEREC ( IDENT ( : <part>)? )

<part> one of:
    *ALL  *INPUT  *OUTPUT  *KEY

<var-modifier>
    : INZ ( <expression> )
    | INZ ( *LIKEDS )
    | BASED ( IDENT )
    | DIM ( <integer-literal> )
    | OVERLAY ( IDENT ( : <integer-literal> )? )
    | POS ( <integer-literal> )
    | STATIC
    | EXPORT
    | IMPORT
    | TEMPLATE
    | ALIGN
    | OPTIONS ( <options-list> )
    | CCSID ( <expression> )

<options-list>
    : <option>
    | <options-list> : <option>

<option> one of:
    *NOPASS  *OMIT  *VARSIZE  *STRING  *NULLIND
```

Ejemplos:

```bbk
DCL-S counter INT(10);
DCL-S name CHAR(50) INZ("");
DCL-S price PACKED(9:2) INZ(0d);
DCL-S birthDate DATE;
DCL-S active BOOL INZ(false);
DCL-S nums INT(10) DIM(100);
DCL-S overlayField CHAR(20) OVERLAY(parentDS:1);
DCL-S sameAsPrice LIKE(price);
```

### 3.2 Constantes (DCL-C)

```
<constant-declaration>
    : DCL-C IDENT <constant-value> ;
    | DCL-C IDENT CONST ( <constant-value> ) ;

<constant-value>
    : <literal>
    | <figurative-constant>
    | ( <constant-expression> )

<figurative-constant> one of:
    true  false  null
```

(En BBK eliminamos las figurative constants legacy de RPG; `true`/`false`/`null` son los únicos.)

Ejemplos:

```bbk
DCL-C MAX_RETRIES 5;
DCL-C PI 3.14159;
DCL-C GREETING "Hello";
DCL-C IS_ENABLED CONST(true);
```

### 3.3 Data structures (DCL-DS)

```
<data-structure-declaration>
    : DCL-DS IDENT { <ds-modifier> }* { <ds-subfield> { <ds-subfield> }* }
    | DCL-DS IDENT { <ds-modifier> }* ;                  // forward / template

<ds-modifier>
    : QUALIFIED
    | TEMPLATE
    | EXTNAME ( <string-literal> )
    | LIKEDS ( IDENT )
    | LIKEREC ( IDENT )
    | INZ
    | BASED ( IDENT )
    | DIM ( <integer-literal> )
    | ALIGN
    | PSDS
    | INFDS ( IDENT )

<ds-subfield>
    : DCL-SUBF? IDENT <type-specification> { <var-modifier> }* ;
```

(`DCL-SUBF` es opcional; los subfields pueden declararse simplemente como `<nombre> <tipo>;`.)

Ejemplos:

```bbk
DCL-DS employee QUALIFIED {
  id        INT(10);
  name      CHAR(50);
  hireDate  DATE;
  salary    PACKED(9:2);
}

// Template (definición sin storage; usable con LIKEDS):
DCL-DS addressTemplate TEMPLATE {
  street CHAR(100);
  city   CHAR(50);
  zip    CHAR(10);
}

DCL-DS shippingAddr LIKEDS(addressTemplate);
DCL-DS billingAddr  LIKEDS(addressTemplate);
```

### 3.4 Prototipos (DCL-PR)

Forward declaration de un procedure.

```
<prototype-declaration>
    : DCL-PR IDENT <inline-param-list>? <return-type>? { <pr-modifier> }* ;

<return-type>
    : -> <type-specification>

<inline-param-list>
    : ( )
    | ( <inline-param> { , <inline-param> }* )

<inline-param>
    : IDENT <type-specification> { <param-modifier> }*

<pr-modifier>
    : EXTPGM ( <string-literal> )
    | EXTPROC ( <string-literal> )
    | OPDESC
    | RTNPARM

<param-modifier>
    : VALUE
    | CONST
    | OPDESC
    | OPTIONS ( <options-list> )
```

**Notas:**
- **Solo forma inline** (decisión cerrada). La forma con `DCL-PI` explícita interna ya no se soporta.
- Si hay valor de retorno: `-> <type>` después de los parámetros.
- Sin valor de retorno: omitir `-> <type>`.

Ejemplos:

```bbk
// Inline:
DCL-PR sumar(a INT(10), b INT(10)) -> INT(10);

// Sin retorno:
DCL-PR saludar(nombre CHAR(50));

// Llamada a programa externo:
DCL-PR CUSTPROG EXTPGM("CUSTPROG") {
  custId INT(10) VALUE;
  status INT(10);
}
```

### 3.5 Procedures (DCL-PROC)

**Solo forma inline** (decisión cerrada). `DCL-PI` como declaración separada se eliminó del lenguaje; los parámetros van en la firma de `DCL-PROC` directamente.

```
<procedure-declaration>
    : DCL-PROC IDENT <inline-param-list>? <return-type>? { <proc-modifier> }* { <procedure-body> }

<procedure-body>
    : { <statement-or-declaration> }*

<statement-or-declaration>
    : <statement>
    | <variable-declaration>
    | <constant-declaration>
    | <data-structure-declaration>

<proc-modifier>
    : EXPORT
    | EXTPROC ( <string-literal> )
```

Ejemplos:

```bbk
DCL-PROC sumar(a INT(10), b INT(10)) -> INT(10) EXPORT {
  return a + b;
}

// Sin retorno, sin parámetros:
DCL-PROC saludar {
  // body
}

// Con variables locales:
DCL-PROC procesarOrden(orderId INT(10)) -> BOOL {
  DCL-S total PACKED(11:2);
  DCL-S valid BOOL;
  
  // ...
  return valid;
}
```

### 3.6 File declarations (DCL-F)

```
<file-declaration>
    : DCL-F IDENT { <f-keyword> }* ;

<f-keyword>
    : USAGE ( <usage-list> )
    | KEYED
    | RECNO ( IDENT )
    | PREFIX ( IDENT ( : <integer-literal> )? )
    | RENAME ( IDENT : IDENT )
    | EXTNAME ( <string-literal> )
    | EXTFILE ( <string-literal> )
    | EXTMBR ( <string-literal> )
    | EXTDESC ( <string-literal> )
    | USROPN
    | DISK | PRINTER | WORKSTN | SEQ
    | BLOCK ( * IDENT )
    | INDDS ( IDENT )
    | INFDS ( IDENT )

<usage-list>
    : <usage-arg>
    | <usage-list> : <usage-arg>

<usage-arg> one of:
    *INPUT  *OUTPUT  *UPDATE  *DELETE
```

Ejemplos:

```bbk
DCL-F customers DISK USAGE(*INPUT) KEYED EXTNAME("CUSTOMER");
DCL-F orders   DISK USAGE(*INPUT:*OUTPUT:*UPDATE) KEYED EXTNAME("ORDER");
DCL-F report   PRINTER USAGE(*OUTPUT) USROPN;
```

---

## 4. Statements

```
<statement>
    : <expression-statement>
    | <assignment-statement>
    | <if-statement>
    | <select-statement>
    | <while-statement>
    | <do-while-statement>
    | <for-statement>
    | <break-statement>
    | <continue-statement>
    | <return-statement>
    | <block-statement>
    | <call-statement>
    | <file-operation-statement>
    | <monitor-statement>
```

### 4.1 Expression statement

```
<expression-statement>
    : <expression> ;
    | ;                                          // null statement
```

Ejemplos:

```bbk
procesar(orderId);              // llamada como statement
incrementar();
;                               // statement vacío
```

### 4.2 Assignment statement

```
<assignment-statement>
    : <lvalue> <assignment-operator> <expression> <attribute-modifier>? ;

<lvalue>
    : IDENT
    | <lvalue> . IDENT                          // member access en DS qualified
    | <lvalue> [ <expression> ]                 // subscript
    | <lvalue> -> IDENT                         // pointer member access

<assignment-operator> one of:
    =  +=  -=  *=  /=  %=  &=  |=  ^=  <<=  >>=

<attribute-modifier>
    : @ IDENT                                   // ej. @halfup, @halfdown, @trunc
```

Ejemplos:

```bbk
counter = 0;
counter += 1;
total = precio * iva @halfup;
employee.id = 100;
nums[0] = 42;
employees[i].salary = baseSalary;
```

### 4.3 If statement

```
<if-statement>
    : if ( <expression> ) <block-or-statement>
        { else if ( <expression> ) <block-or-statement> }*
        ( else <block-or-statement> )?

<block-or-statement>
    : <block-statement>
    | <statement>
```

Ejemplos:

```bbk
if (cond) {
  ...
}

if (cond) {
  ...
} else if (otherCond) {
  ...
} else {
  ...
}

if (cond) doSomething();          // sin llaves, single statement
```

(Decisión a confirmar: ¿permitimos statements sin llaves como en C, o forzamos llaves? Forzar llaves elimina el clásico bug del `if` sin braces. Recomiendo permitir ambos por ergonomía.)

### 4.4 Select / when / other statement

```
<select-statement>
    : select { <when-clause> { <when-clause> }* <other-clause>? }

<when-clause>
    : when ( <expression> ) <block-or-statement>

<other-clause>
    : other <block-or-statement>
```

Ejemplos:

```bbk
select {
  when (tipo == "A") {
    procesarA();
  }
  when (tipo == "B" || tipo == "C") {
    procesarBC();
  }
  other {
    procesarDefault();
  }
}
```

(`select` sin condición discriminante — el chequeo lo hace cada `when`. A diferencia de C `switch`, las expresiones de `when` no necesitan ser constantes.)

### 4.5 While statement

```
<while-statement>
    : while ( <expression> ) <block-or-statement>
```

Ejemplo:

```bbk
while (i < 10) {
  process(i);
  i += 1;
}
```

### 4.6 Do-while statement

```
<do-while-statement>
    : do <block-or-statement> while ( <expression> ) ;
```

Ejemplo:

```bbk
do {
  result = compute();
} while (result == 0);
```

### 4.7 For statement

```
<for-statement>
    : for ( <for-init>? ; <expression>? ; <for-update>? ) <block-or-statement>

<for-init>
    : <expression>
    | <variable-declaration-inline>

<variable-declaration-inline>
    : DCL-S IDENT <type-specification> = <expression>

<for-update>
    : <expression>
```

(C99 permite declaración en el init del `for`. BBK acepta la sintaxis equivalente con `DCL-S` inline.)

Ejemplos:

```bbk
for (i = 0; i < 10; i += 1) {
  process(i);
}

for (DCL-S j INT(10) = 0; j < 100; j += 1) {
  // j es local al for
}
```

### 4.8 Break / continue / return

```
<break-statement>     : break ;
<continue-statement>  : continue ;
<return-statement>    : return <expression>? ;
```

### 4.9 Block statement

```
<block-statement>
    : { <statement-or-declaration>* }
```

Permite mezcla libre de declaraciones y statements (al estilo C99).

### 4.10 Call statement

Llamada a procedure como statement (sin asignar resultado).

```
<call-statement>
    : <call-expression> ;

<call-expression>
    : <primary> ( <argument-list>? )

<argument-list>
    : <expression>
    | <argument-list> , <expression>
```

Ejemplos:

```bbk
saludar();
saludar("mundo");
procesarOrden(orderId, status);
```

### 4.11 File operation statement

Operaciones de archivo. Cada opcode es un keyword opcional al frente; la sintaxis general es función-like.

```
<file-operation-statement>
    : <file-op-keyword> <file-op-args> ;
    | <file-op-function-call> ;                    // sintaxis alternativa función-like

<file-op-keyword> one of:
    read  reade  readp  readpe  chain
    write  update  delete  unlock
    open  close  setll  setgt  exfmt

<file-op-args>
    : IDENT                                        // archivo
    | <expression> IDENT                           // key + archivo
    | IDENT IDENT                                  // archivo + DS result
    | <expression> IDENT IDENT                     // key + archivo + DS result
```

Ejemplos:

```bbk
read customers;
read customers customerDS;
chain custId customers customerDS;
write orders orderDS;
update orders orderDS;
setll *START customers;
```

(Decisión a confirmar: ¿`read customers` con identificadores sin paréntesis, o `read(customers)` función-like? La forma sin paréntesis es más legible para statements simples; la función-like es más uniforme con el resto del lenguaje. Recomendaría la función-like para consistencia.)

### 4.12 Monitor statement (manejo de errores)

```
<monitor-statement>
    : monitor { { <statement> }* } { <on-error-clause> }*  <on-exit-clause>?

<on-error-clause>
    : on-error <status-list>? <block-statement>

<status-list>
    : ( <expression> { , <expression> }* )

<on-exit-clause>
    : on-exit <block-statement>
```

Ejemplos:

```bbk
monitor {
  open customers;
  read customers customerDS;
} on-error (00404, 00405) {
  log("file not found");
} on-error {
  log("unknown error");
} on-exit {
  close customers;
}
```

---

## 5. Expresiones

Precedencia de menor a mayor (top-down):

```
<expression>
    : <ternary-expression>

<ternary-expression>
    : <logical-or-expression>
    | <logical-or-expression> ? <expression> : <ternary-expression>

<logical-or-expression>
    : <logical-and-expression>
    | <logical-or-expression> || <logical-and-expression>

<logical-and-expression>
    : <bitwise-or-expression>
    | <logical-and-expression> && <bitwise-or-expression>

<bitwise-or-expression>
    : <bitwise-xor-expression>
    | <bitwise-or-expression> | <bitwise-xor-expression>

<bitwise-xor-expression>
    : <bitwise-and-expression>
    | <bitwise-xor-expression> ^ <bitwise-and-expression>

<bitwise-and-expression>
    : <equality-expression>
    | <bitwise-and-expression> & <equality-expression>

<equality-expression>
    : <relational-expression>
    | <equality-expression> == <relational-expression>
    | <equality-expression> != <relational-expression>

<relational-expression>
    : <shift-expression>
    | <relational-expression> < <shift-expression>
    | <relational-expression> > <shift-expression>
    | <relational-expression> <= <shift-expression>
    | <relational-expression> >= <shift-expression>

<shift-expression>
    : <additive-expression>
    | <shift-expression> << <additive-expression>
    | <shift-expression> >> <additive-expression>

<additive-expression>
    : <multiplicative-expression>
    | <additive-expression> + <multiplicative-expression>
    | <additive-expression> - <multiplicative-expression>

<multiplicative-expression>
    : <power-expression>
    | <multiplicative-expression> * <power-expression>
    | <multiplicative-expression> / <power-expression>
    | <multiplicative-expression> % <power-expression>

<power-expression>
    : <unary-expression>
    | <unary-expression> ** <power-expression>        // right-associative

<unary-expression>
    : <postfix-expression>
    | + <unary-expression>
    | - <unary-expression>
    | ! <unary-expression>
    | ~ <unary-expression>

<postfix-expression>
    : <primary>
    | <postfix-expression> ( <argument-list>? )      // function call
    | <postfix-expression> [ <expression> ]          // subscript
    | <postfix-expression> . IDENT                   // member access
    | <postfix-expression> -> IDENT                  // pointer member access

<primary>
    : IDENT
    | <literal>
    | true | false | null
    | ( <expression> )
```

### 5.1 Tabla de precedencia (alta → baja)

| Nivel | Operadores | Asociatividad |
|---|---|---|
| 1 | `(...)` `[...]` `.` `->` (postfix) | left |
| 2 | unario `+` `-` `!` `~` | right |
| 3 | `**` | right |
| 4 | `*` `/` `%` | left |
| 5 | `+` `-` | left |
| 6 | `<<` `>>` | left |
| 7 | `<` `>` `<=` `>=` | left |
| 8 | `==` `!=` | left |
| 9 | `&` | left |
| 10 | `^` | left |
| 11 | `\|` | left |
| 12 | `&&` | left |
| 13 | `\|\|` | left |
| 14 | `?:` ternario | right |

### 5.2 Literales

```
<literal>
    : INT_LIT
    | INT_LIT_HEX
    | INT_LIT_OCT
    | FLOAT_LIT
    | DEC_LIT
    | STR_LIT
```

(Definición léxica detallada en [`tokens.md`](tokens.md) §3.)

### 5.3 Constructores de fecha/hora/timestamp

Se construyen como llamadas a función — no son literales propios:

```bbk
DCL-S d DATE;
DCL-S t TIME;
DCL-S ts TIMESTAMP;

d = date("2026-05-22");
t = time("14:30:00");
ts = timestamp("2026-05-22T14:30:00.000000");
```

---

## 6. Directivas

Procesadas por una fase de pre-procesamiento antes del parsing principal. Sintaxis similar al preprocesador C pero con el formato `/KEYWORD` de RPG.

```
<directive>
    : <conditional-directive>
    | <define-directive>
    | <include-directive>
    | <eof-directive>
```

### 6.1 Conditional compilation

```
<conditional-directive>
    : <if-directive> { <elseif-directive> }* <else-directive>? <endif-directive>

<if-directive>
    : PRE-IF <condition-expression>

<elseif-directive>
    : PRE-ELSEIF <condition-expression>

<else-directive>
    : PRE-ELSE

<endif-directive>
    : PRE-ENDIF

<condition-expression>
    : DEFINED ( IDENT )
    | NOT DEFINED ( IDENT )
    | <constant-expression>
```

Ejemplo:

```bbk
PRE-IF DEFINED(DEBUG)
  log("debug mode active");
PRE-ELSEIF DEFINED(PRODUCTION)
  log("production mode");
PRE-ELSE
  log("default mode");
PRE-ENDIF
```

### 6.2 Define / undefine

```
<define-directive>
    : PRE-DEFINE IDENT
    | PRE-DEFINE IDENT <replacement-text>

<undefine-directive>
    : PRE-UNDEFINE IDENT
```

BBK acepta ambos modos: flag simple (`PRE-DEFINE DEBUG`) o con texto de reemplazo (`PRE-DEFINE MAX_RETRIES 5`). El procesador reemplaza ocurrencias del símbolo por el texto en el resto del archivo.

### 6.3 Include

```
<include-directive>
    : PRE-INCLUDE <string-literal>
    | PRE-INCLUDE IDENT
```

Unificada — no hay distinción entre `/COPY` y `/INCLUDE`. Una sola directiva con semántica "incluir un archivo fuente en este punto".

Ejemplo:

```bbk
PRE-INCLUDE "common-types.bbki"
PRE-INCLUDE "db-prototypes.bbki"
```

### 6.4 EOF

```
<eof-directive>
    : PRE-EOF
```

Marca fin del fuente prematuro; lo que sigue en el archivo se ignora.

---

## 7. Attribute modifiers

Tokens introducidos por `@`. Listado cerrado en V1; pueden extenderse en versiones futuras.

```
<attribute-modifier>
    : @ <attribute-name>

<attribute-name> one of:
    halfup    halfdown    trunc
```

**Contextos donde aplica:**

- Statement de asignación: modifica el redondeo del valor asignado.
- Posibles extensiones: cuerpo de procedure (`@inline`?), tipo de variable (`@volatile`?). Por ahora solo asignación.

Ejemplo:

```bbk
DCL-S total   PACKED(11:2);
DCL-S precio  PACKED(9:2);
DCL-S cantidad INT(10);

total = precio / cantidad @halfup;     // redondeo half-up
total = precio / cantidad @trunc;      // truncar (default)
```

---

## 8. Reglas semánticas — síntesis

(Como con [`../c99-grammar.md`](../c99-grammar.md) §4, las reglas semánticas no son BNFeables. Se documentarán en `semantics.md` cuando se escriba.)

Por ahora, los puntos críticos pendientes de definir formalmente:

- **Sistema de tipos.** Reglas de promoción entre tipos numéricos (INT ↔ PACKED ↔ FLOAT). Compatibilidad asignación CHAR ↔ VARCHAR. Conversiones implícitas vs explícitas.
- **Scope rules.** Variables module-level vs procedure-local. Visibility con EXPORT/IMPORT.
- **Storage durations.** STATIC, automatic, basado (BASED).
- **Initialization defaults.** ¿BBK auto-inicializa o exige `INZ` explícito? (Decisión ya tomada en [`../mapping/translatable.md`](../../mapping/translatable.md): siempre explícito.)
- **Sequence points.** Cuándo se garantizan efectos colaterales.
- **Aritmética decimal.** Reglas de precisión y redondeo en operaciones entre PACKED/ZONED de distinta escala.
- **Punteros y aliasing.** Qué se permite con BASED, OVERLAY, casts.
- **Manejo de errores.** Semántica de monitor/on-error/on-exit. Equivalente a try/catch/finally.
- **File ops.** Estado de cursor tras read/chain/setll. Comportamiento de write/update tras error.

---

## 9. Decisiones abiertas (specifics de la gramática)

Items donde la gramática propuesta es una elección pero hay alternativas:

| # | Decisión | Resolución |
|---|---|---|
| 1 | Ordenamiento de top-level declarations | **Orden forzado** CTL-OPT → DCL-F → (DCL-C/S/DS) → DCL-PR → DCL-PROC. Las directivas pueden ir en cualquier punto. |
| 2 | Procedure inline vs explícita | **Solo inline.** `DCL-PI` separado se elimina del lenguaje. |
| 3 | Subfield de DS con DCL-SUBF | **Opcional.** Ambas formas (con y sin `DCL-SUBF`) son válidas. |
| 4 | Llaves obligatorias en bloques | **Sí, llaves obligatorias** en `if`, `else`, `while`, `do/while`, `for`, `select`/`when`/`other`, `monitor`/`on-error`/`on-exit`. Sin single-statement sin llaves. |
| 5 | File operations sintaxis | **Keyword style** (`read customers customerDS;`), no función-like. |
| 6 | `PRE-DEFINE` con replacement text | **Estilo C** — admite `PRE-DEFINE NAME` (flag) y `PRE-DEFINE NAME value` (con texto de reemplazo). |
| 7 | `/INCLUDE` y `/COPY` | **Unificados** en `PRE-INCLUDE`. |
| 8 | Multi-dim arrays | **`arr[i, j]`** (comma-separated indices en un solo `[]`), no chained `[i][j]`. |
| 9 | Lvalue puede contener call result | **Sí.** `f().field = x` y similares son sintácticamente válidos. La semántica (función debe retornar referencia/lvalue) se valida en el type checker. |
| 10 | `LIKE(expression)` | **Solo IDENT** — `LIKE(otherVar)` válido; `LIKE(f())` o `LIKE(arr[i])` no. |
| 11 | Declaración inline en `for` | **Sí soportado** — `for (DCL-S i INT(10) = 0; i < 10; i += 1) { ... }`. |
| 12 | Sintaxis de attribute modifier | **`@`** — `@halfup`, `@halfdown`, `@trunc`. Lista cerrada. |

**Las 12 decisiones de gramática están cerradas.** Quedan por hacer:
- Reflejar en las producciones BNF los cambios de #4 (forzar llaves), #8 (subscript con comma), #9 (lvalue con calls) — son ediciones puntuales pendientes en este archivo.
- Definir reglas semánticas formales en `semantics.md` (sistema de tipos, conversiones, sequence points, etc.).

---

## 10. Documentos relacionados

- [`tokens.md`](tokens.md) — léxico (tokens individuales)
- [`../c99-grammar.md`](../c99-grammar.md) — gramática de C99 (target del lowering)
- [`../rpgle-grammar.md`](../rpgle-grammar.md) — gramática de RPG (source del frontend)
- [`../../mapping/similarities.md`](../../mapping/similarities.md) — qué mapea directo RPG ↔ C
- [`../../mapping/translatable.md`](../../mapping/translatable.md) — qué necesita traducción con runtime
- [`../../mapping/runtime-required.md`](../../mapping/runtime-required.md) — qué no se resuelve solo traduciendo
