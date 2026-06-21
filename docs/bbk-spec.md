# BBK — Especificación del lenguaje

> Estado: documento vivo, **grounded en la implementación de `bbk-core`** (lexer, parser,
> AST, análisis semántico y los dos backends). Cuando este documento y el código difieran,
> el código manda — y este archivo es el bug. Gramática de referencia:
> `plugin-bbk/src/main/grammar/BBK.bnf`.

## 1. Qué es BBK

**BBK** (BoxBreaker) es un lenguaje intermedio moderno al que se traduce RPG (free-format).
Conserva el modelo de programación de RPG/IBM i (tipos decimales, estructuras de datos,
procedimientos, subrutinas, `monitor`) pero con una **sintaxis C-style** (llaves, `==` para
igualdad, `<<`/`>>`, operadores compuestos).

Un único AST de BBK alimenta **dos backends** (decisión de arquitectura: *el AST + su análisis
es el IR, no hay IR separado*):

- **JVM bytecode** — `JvmCompiler` genera una clase `bbk.Main` con ASM; corre in-process en la JVM.
- **C** — `CCompiler` emite C self-contained que compila con `gcc`/`clang` (AOT).

La superficie del **sistema operativo IBM i** (archivos/DB2, spool, jobs, library lists, llamadas
a `*PGM`) **no** la lowerean los backends: vive en el servicio `bbk-runtime` (Spring Boot, REST).

## 2. Estructura léxica

### 2.1 Sensibilidad a mayúsculas

BBK es **case-sensitive** (a diferencia de RPG). Por convención:

- **Declaraciones y tipos** en MAYÚSCULAS: `DCL-S`, `INT`, `PACKED`, `QUALIFIED`.
- **Control de flujo C-style** en minúsculas: `if`, `else`, `while`, `do`, `for`, `return`.
- **Operaciones de archivo / subrutinas** en su forma RPG: `read`, `chain` (minúscula),
  `BEGSR`, `EXSR` (mayúscula).

### 2.2 Comentarios

```
// comentario de línea
/* comentario
   de bloque */
```

### 2.3 Identificadores

- `IDENT` — `[A-Za-z_][A-Za-z0-9_]*`.
- `STAR_IDENT` — `*nombre` (valores especiales / figurativas: `*on`, `*blank`, `*inlr`).

### 2.4 Literales

| Categoría | Token | Ejemplos | Tipo |
|---|---|---|---|
| Entero decimal | `INT_LIT` | `123`, `0` | `INT` |
| Entero hexadecimal | `INT_LIT_HEX` | `0x1F` | `INT` |
| Entero octal | `INT_LIT_OCT` | `0o17` | `INT` |
| Punto flotante | `FLOAT_LIT` | `1.5e3` (con exponente) | `FLOAT` |
| Decimal | `DEC_LIT` | `199.95`, `0.21d` | `DECIMAL` |
| Cadena | `STR_LIT` | `"hola"`, `'hola'`, `"a\nb"` | `STRING` |
| Booleano | keyword | `true`, `false` | `BOOL` |
| Nulo | keyword | `null` | (cadena vacía en lowering) |

> **Importante:** un número con punto y **sin exponente** (`199.95`, `2.0`) es un literal
> **decimal** (`DECIMAL`), no flotante. Para flotante hace falta exponente (`2.0e0`) o una
> variable de tipo `FLOAT`. El sufijo `d` (`0.21d`) marca decimal explícitamente.

Escapes en cadenas: `\n`, `\t`, `\r`, `\\`, `\"`. Ambas comillas (`"` y `'`) son equivalentes.

### 2.5 Operadores y puntuadores

```
Aritméticos:   +  -  *  /  %  **
Comparación:   ==  !=  <  >  <=  >=
Lógicos:       &&  ||  !
Bitwise:       &  |  ^  ~  <<  >>
Asignación:    =  +=  -=  *=  /=  %=  &=  |=  ^=  <<=  >>=
Ternario:      ? :
Puntuadores:   ;  ,  .  ->  ( )  { }  [ ]  :  @
```

### 2.6 Modificadores de atributo

Redondeo decimal sobre una asignación: `@halfup`, `@halfdown`, `@trunc`.

```
total = subtotal / items @trunc;     // truncar al alcance declarado
tax   = base * RATE @halfup;          // redondeo half-up
```

## 3. Sistema de tipos

El análisis semántico (`semantic.Type`) colapsa los primitivos de BBK a cinco **clases
escalares**, más arrays y estructuras. Cada backend mapea esto a su representación física.

| Tipo BBK | Clase | JVM | C |
|---|---|---|---|
| `INT(n)`, `UNS(n)` | `INT` | `long` | `long long` |
| `FLOAT(n)` | `FLOAT` | `double` | `double` |
| `PACKED(p:s)`, `ZONED(p:s)`, `BINDEC(p:s)` | `DECIMAL` (escala `s`) | `BigDecimal` | `long double` |
| `CHAR(n)`, `VARCHAR(n)` | `STRING` | `String` | `const char*` |
| `BOOL`, `IND` | `BOOL` | `boolean` | `int` |
| `DATE`, `TIME`, `TIMESTAMP` | `DATE`/`TIME`/`TIMESTAMP` | `long` (epoch) | `long long` (epoch) |
| `POINTER` | — | **no lowered** | **no lowered** |

### 3.1 Torre numérica y coerción

Orden de promoción: **`INT < FLOAT < DECIMAL`**. En una operación binaria numérica el resultado
es el más ancho de los operandos; un resultado `DECIMAL` lleva la mayor de las escalas. La
coerción a lo ancho es implícita; el estrechamiento ocurre en asignaciones (con `@attr` para el
redondeo).

- `JVM`: `BigDecimal` exacto; la escala se aplica con `setScale(s, modo)` en cada store.
- `C`: `long double`; exacto en pantalla porque se redondea al alcance declarado al guardar
  (con un *snap* anti-ruido binario en el truncado). El BCD exacto pleno es dominio de
  `bbk-runtime` (decisión de arquitectura).

### 3.2 Arrays

`DIM(n)` declara un array de tamaño fijo. **Subíndices 0-based** (coherente con la sintaxis
C-style): `a[0]` … `a[n-1]`.

```
DCL-S nums INT(10) DIM(5);
nums[0] = 10;
print(nums[0] + nums[1]);
```

### 3.3 Estructuras de datos

```
DCL-DS person QUALIFIED {
  firstName VARCHAR(50);
  lastName  VARCHAR(50);
  age       INT(10);
}
person.firstName = "Nicolas";        // acceso calificado con .
```

- `QUALIFIED` — los subcampos se acceden vía `ds.campo`.
- `TEMPLATE` — solo define un layout reutilizable; no asigna almacenamiento.
- `LIKEDS(x)` — clona el layout de otra DS/template.
- `DIM(n)` sobre la DS → **array de estructuras**: `emp[0].id`.
- `OVERLAY` (aliasing de memoria) **no** está soportado por los backends.

### 3.4 Fechas y horas

`DATE`, `TIME` y `TIMESTAMP` son tipos de primera clase. Internamente se bajan a un **entero
epoch** (día-epoch / segundos del día / segundos-epoch), así la comparación y la diferencia son
aritmética nativa. La construcción, el formateo y la aritmética de calendario van por funciones
(§6.6): **JVM** usa `java.time` por debajo; **C** un prelude de calendario civil que da los
**mismos resultados** (ISO-8601, años bisiestos, recorte de día). Sin `INZ`, el valor por
defecto es `1970-01-01` / `00:00:00`.

```
DCL-S d DATE;
d = date("2024-01-31");
d = addmonths(d, 1);          // 2024-02-29 (recorta + bisiesto)
print(char(d));               // "2024-02-29"
if (d < today()) { ... }      // comparación entre el mismo tipo
```

Comparar o mezclar fechas con números crudos es error: la aritmética de fechas va por las
funciones de §6.6.

## 4. Declaraciones

### 4.1 Variable — `DCL-S`

```
DCL-S counter INT(10) INZ(0);
DCL-S price   PACKED(9:2) INZ(199.95d);
DCL-S name    VARCHAR(50);
DCL-S arr     INT(10) DIM(5);
```

Modificadores comunes: `INZ(expr)` (valor inicial), `DIM(n)`, `LIKE(x)`, `LIKEDS(x)`.

### 4.2 Constante — `DCL-C`

```
DCL-C MAX_RETRIES 5;
DCL-C PI          3.14159d;
DCL-C VERSION     "v1.0.0";
DCL-C IS_DEBUG    true;
```

Las constantes se resuelven en tiempo de compilación (inline). El backend no les reserva
almacenamiento.

### 4.3 Estructura — `DCL-DS`

Ver §3.3. Subcampos: `nombre TIPO modificadores*;`.

### 4.4 Prototipo y procedimiento — `DCL-PR` / `DCL-PROC`

```
DCL-PR sum(a INT(10) VALUE, b INT(10) VALUE) -> INT(10);   // prototipo (forward)

DCL-PROC sum(a INT(10) VALUE, b INT(10) VALUE) -> INT(10) EXPORT {
  return a + b;
}
```

- Parámetros inline: `nombre TIPO modificadores*`. Modificadores: `VALUE` (por valor),
  `CONST`, `DIM(n)` (array).
- `-> TIPO` declara el tipo de retorno; sin él, el procedimiento es `void`.
- Los backends bajan cada `DCL-PROC` a un método/función estática; las llamadas son
  `INVOKESTATIC` (JVM) / llamada a función (C). **Parámetros por valor.**
- Un parámetro puede ser una **estructura de datos** (`LIKEDS(t)`). Se pasa **por valor**,
  bajada a sus subcampos escalares (JVM: parámetros `c.id`, `c.name`; C: `c_id`, `c_name`); el
  que llama empuja los subcampos del DS argumento. *Retornar* una DS aún no está soportado.
- `EXTPGM(...)` / `EXTPROC(...)` designan programas/procedimientos externos del IBM i:
  son **llamadas al SO**, no lowered por los backends.

### 4.5 Opciones de control — `CTL-OPT`

```
CTL-OPT MAIN(proceduresDemo);
```

`MAIN(p)` designa el procedimiento de entrada: el `main` generado inicializa las globales y
llama a `p`. Sin `CTL-OPT MAIN`, las sentencias de nivel módulo forman el mainline.

### 4.6 Archivo — `DCL-F`

`DCL-F` y las operaciones de archivo se **parsean** (están en la gramática) pero **no** las
lowerean los backends: son I/O de DB2/archivos nativos del IBM i → `bbk-runtime`.

## 5. Sentencias

### 5.1 Asignación

```
x = expr;                  // simple
x += expr;                 // compuesta: += -= *= /= %= &= |= ^= <<= >>=
x = a / b @halfup;         // con modificador de redondeo decimal
arr[i] = expr;             // a elemento de array
person.age = expr;         // a subcampo de DS
```

Una asignación simple `=` a un nombre no declarado lo **auto-declara** (toma el tipo del RHS).

### 5.2 Condicional

```
if (cond) { ... } else if (cond2) { ... } else { ... }

select {
  when (cond1) { ... }
  when (cond2) { ... }
  other        { ... }
}
```

### 5.3 Bucles

```
while (cond) { ... }
do { ... } while (cond);
for (i = 1; i <= 10; i += 1) { ... }
break;     continue;
```

### 5.4 Retorno

```
return;          // de un procedimiento void
return expr;     // con valor
```

### 5.5 Manejo de errores — `monitor`

```
monitor {
  r = 10 / 0;
} on-error (statuses?) {
  r = -1;
} on-exit {
  cleanup();
}
```

- JVM: `try/catch` (atrapa cualquier `Throwable`).
- C: `setjmp/longjmp`; atrapa **división entera por cero** (vía un helper); C no puede atrapar
  portablemente otros traps aritméticos.

### 5.6 Subrutinas

```
EXSR init;                 // invoca (puede ser forward)
...
BEGSR init;
  x = 0;
  if (done) { LEAVESR; }   // sale temprano de la subrutina
  y = 1;
ENDSR;
```

Las subrutinas comparten las variables del procedimiento contenedor; los backends las
**inline-an** en cada `EXSR` (`LEAVESR` salta al fin del inlining). No recursivas.

### 5.7 Otras

- `CALLP expr;` — llama a un procedimiento descartando el resultado.
- Operaciones de archivo (`read`, `chain`, `write`, `update`, `delete`, `setll`, ...) — OS, no lowered.
- Directivas: `CTL-OPT`, `PRE-IF`/`PRE-ELSE`/`PRE-ENDIF`/`PRE-DEFINE`/`PRE-INCLUDE`/... — sin efecto en runtime (preprocesador).

## 6. Expresiones

### 6.1 Precedencia (de menor a mayor)

| Nivel | Operadores | Asociatividad |
|---|---|---|
| 1 | `\|\|` | izquierda |
| 2 | `&&` | izquierda |
| 3 | `\|` | izquierda |
| 4 | `^` | izquierda |
| 5 | `&` | izquierda |
| 6 | `==` `!=` | izquierda |
| 7 | `<` `>` `<=` `>=` | izquierda |
| 8 | `<<` `>>` | izquierda |
| 9 | `+` `-` | izquierda |
| 10 | `*` `/` `%` | izquierda |
| 11 | `**` | **derecha** |
| 12 | unario `+` `-` `!` `~` | — |
| 13 | postfijo: llamada `()`, índice `[]`, miembro `.` / `->` | izquierda |
| 14 | primario: literal, identificador, `( )` | — |

(Idéntica a C; por eso ambos backends emiten paréntesis mínimos correctos.)

### 6.2 Semántica de operadores

- `+` sobre operandos donde alguno es `STRING` → **concatenación**; si no, suma numérica.
- `**` (potencia) → canónicamente **`FLOAT`** en ambos backends (`Math.pow` / `pow`).
- Comparación (`== != < > <= >=`) → `BOOL`. Strings: solo `==`/`!=` (igualdad).
- Lógicos `&& || !` → `BOOL` (no short-circuit garantizado en el lowering actual).
- Bitwise/shift `& | ^ ~ << >>` → operan sobre enteros (`INT`).
- Ternario `cond ? a : b` — ambas ramas deben tener el mismo tipo.

### 6.3 Llamadas, índice, miembro

```
add(5, 7)            // llamada a procedimiento o builtin
arr[i]               // índice de array (0-based)
emp[i].field         // subcampo de un elemento de array de DS
ds.field             // subcampo de DS
ptr->field           // acceso vía puntero (mismo que . en el lowering)
```

### 6.4 Builtins puros

Implementados por ambos backends (no son llamadas al SO):

| Builtin | Resultado | Notas |
|---|---|---|
| `len(s)` | `INT` | longitud de la cadena |
| `substr(s, start [, len])` | `STRING` | **1-based** (semántica RPG `%SUBST`) |
| `scan(needle, haystack)` | `INT` | posición 1-based, 0 si no está |
| `trim(s)` / `triml(s)` / `trimr(s)` | `STRING` | recorta ambos / izquierda / derecha |
| `lower(s)` / `upper(s)` | `STRING` | caja |
| `replace(s, from, to)` | `STRING` | reemplazo de subcadena |
| `char(x)` | `STRING` | número/bool/fecha → texto (las fechas en ISO) |
| `int(x)` | `INT` | string/double/decimal → entero |
| `float(x)` | `FLOAT` | a punto flotante |
| `dec(x)` | `DECIMAL` | a decimal |
| `sqrt(x)` | `FLOAT` | raíz cuadrada |
| `abs(x)` | tipo de `x` | valor absoluto |
| `print(x)` | — | salida estándar (solo como sentencia) |

### 6.5 Valores especiales (`STAR_IDENT`)

| Valor | Tipo | Significado |
|---|---|---|
| `*on` / `*off` | `BOOL` | `true` / `false` |
| `*zero` / `*zeros` | `INT` | `0` |
| `*blank` / `*blanks` | `STRING` | `""` |
| `*null` | (cadena vacía) | nulo |

Otros (`*inlr`, `*hival`, ...) no están soportados.

### 6.6 Funciones de fecha/hora

Builtins puros (no son llamadas al SO). **JVM** las baja con `java.time`; **C** con un prelude de
calendario civil — **mismos resultados**, verificado en paridad. Restar = `n` negativo.

| Función | Resultado | Notas |
|---|---|---|
| `date(s)` / `time(s)` / `timestamp(s)` | `DATE` / `TIME` / `TIMESTAMP` | parsea ISO (`"2024-01-15"`, `"13:45:30"`, `"2024-01-15T13:45:30"`) |
| `today()` / `now()` | `DATE` / `TIMESTAMP` | fecha / fecha-hora actual |
| `year(d)` / `month(d)` / `day(d)` | `INT` | componentes de `DATE`/`TIMESTAMP` |
| `hour(t)` / `minute(t)` / `second(t)` | `INT` | componentes de `TIME`/`TIMESTAMP` |
| `adddays` / `addmonths` / `addyears(d, n)` | igual que `d` | sobre `DATE`/`TIMESTAMP`; `addmonths`/`addyears` recortan el día (31-ene +1 mes = 28/29-feb) |
| `addhours` / `addminutes` / `addseconds(t, n)` | igual que `t` | sobre `TIME`/`TIMESTAMP` (`TIME` envuelve dentro del día) |
| `diffdays(a, b)` / `diffseconds(a, b)` | `INT` | diferencia entre dos valores del mismo tipo |

La comparación (`< > == …`) es entre dos valores del **mismo** tipo de fecha; `char(d)` los
formatea en ISO. Mezclar tipos o sumar una fecha con un número crudo es error de tipos.

## 7. Análisis semántico

`semantic.SemanticAnalyzer` recorre el programa una vez y produce un `SemanticModel`: el **tipo
de cada expresión** (por identidad de nodo), las **firmas de procedimientos**, las **constantes**
y una lista de **diagnósticos**. Es la única fuente de verdad semántica — los dos backends la
consultan en lugar de re-derivar tipos/resolución.

Diagnósticos que reporta (antes del codegen): nombre no declarado, función desconocida, aridad
incorrecta, campo de DS inexistente, índice sobre no-array, tipo no soportado, valor especial
desconocido, operación de archivo (OS).

## 8. Lowering: qué entra y qué no

**Cubierto por ambos backends (lenguaje no-SO completo):** escalares (incl. decimales exactos),
todos los operadores (incl. `**` y `+` concatenación), `if`/`select`/`while`/`do-while`/`for`,
`break`/`continue`/`return`, `monitor`, constantes, valores especiales, arrays (incl. de DS),
estructuras (`QUALIFIED`/`TEMPLATE`/`LIKEDS`), procedimientos + `CTL-OPT MAIN` (incluyendo
**parámetros DS por valor**), subrutinas (`BEGSR`/`EXSR`/`LEAVESR`), los builtins puros y los
**tipos y funciones de fecha/hora** (`DATE`/`TIME`/`TIMESTAMP`).

**No lowered (diagnóstico / a `bbk-runtime`):**

- **SO IBM i:** `DCL-F` y operaciones de archivo, `EXTPGM`/`EXTPROC` (programas externos).
- **`OVERLAY`** (aliasing de memoria de subcampos).
- **`POINTER`**.
- Retornar una **DS** desde un procedimiento (los parámetros DS sí; el retorno aún no).

## 9. Gramática (resumen EBNF)

```
program      ::= item*
item         ::= declaration | statement
declaration  ::= dcl-s | dcl-c | dcl-ds | dcl-pr | dcl-proc | dcl-f | ctl-opt
statement    ::= expr-stmt | assignment | if | select | while | do-while | for
               | break | continue | return | monitor | subroutine | exsr
               | leavesr | callp | file-op | directive

dcl-s        ::= 'DCL-S' IDENT type modifier* ';'
dcl-c        ::= 'DCL-C' IDENT expr ';'
dcl-ds       ::= 'DCL-DS' IDENT modifier* ( '{' subfield* '}' | ';' )
subfield     ::= IDENT type modifier* ';'
dcl-proc     ::= 'DCL-PROC' IDENT params? ('->' type)? modifier* '{' item* '}'
params       ::= '(' param (',' param)* ')'
param        ::= IDENT type modifier*

type         ::= IDENT ( '(' INT (':' INT)? ')' )?           // INT(10), PACKED(11:2)
               | ('LIKE'|'LIKEDS'|'LIKEREC') '(' IDENT ')'

assignment   ::= lvalue assign-op expr attr? ';'
if           ::= 'if' '(' expr ')' block ('else' (if | block))?
select       ::= 'select' '{' ('when' '(' expr ')' block)* ('other' block)? '}'
for          ::= 'for' '(' init? ';' expr? ';' update? ')' block
monitor      ::= 'monitor' block ('on-error' ('(' expr* ')')? block)* ('on-exit' block)?
subroutine   ::= 'BEGSR' IDENT ';' item* 'ENDSR' ';'

expr         ::= ternary
ternary      ::= binary ('?' expr ':' ternary)?
binary       ::= <precedence climbing, niveles de §6.1>
unary        ::= ('+'|'-'|'!'|'~') unary | postfix
postfix      ::= primary ( '(' args? ')' | '[' args ']' | ('.'|'->') IDENT )*
primary      ::= literal | IDENT | STAR_IDENT | 'true' | 'false' | 'null' | '(' expr ')'
block        ::= '{' item* '}'
```

---

*Generado y mantenido junto a `bbk-core`. Para los detalles ejecutables, ver el paquete
`com.larena.boxbreaker.core` (lexer, parser, `ast`, `semantic`, `backend.jvm`, `backend.c`) y los
ejemplos en `tests/boxbreaker/examples/`.*
