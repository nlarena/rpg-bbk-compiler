# Constructos traducibles RPG → C

**Propósito:** identificar constructos donde RPG y C **difieren** pero el `bbk-compiler` puede resolver la diferencia mediante traducción explícita en el lowering, eventualmente apoyándose en funciones helper del `bbk-runtime` que son de uso general (no emulación de IBM i).

La distinción con [`runtime-required.md`](runtime-required.md): los items de este archivo se traducen con código C estándar más, a lo sumo, helpers numéricos/string. No necesitan modelar conceptos exóticos del sistema operativo IBM i.

Documentos relacionados:
- [`similarities.md`](similarities.md) — mapeo directo 1:1
- [`runtime-required.md`](runtime-required.md) — no resoluble por traducción

---

## 1. Tipos decimales exactos: PACKED, ZONED, BINDEC

**El problema:** C no tiene un tipo decimal exacto en el lenguaje estándar. `float`/`double` son binarios y pierden precisión en operaciones decimales (clásico: `0.1 + 0.2 != 0.3`). RPG usa packed/zoned decimal de hasta 63 dígitos con escala fija.

**La traducción:** implementar `bbk_decimal_t` en una biblioteca C dentro del runtime. Operaciones como suma, resta, multiplicación, división, comparación expuestas como funciones.

```rpg
DCL-S precio PACKED(7:2);
DCL-S iva   PACKED(7:2) INZ(1.21);
DCL-S total PACKED(9:2);

total = precio * iva;
```

```c
bbk_decimal_t precio = bbk_dec_zero(7, 2);
bbk_decimal_t iva   = bbk_dec_from_str("1.21", 7, 2);
bbk_decimal_t total = bbk_dec_zero(9, 2);

bbk_dec_mul(&total, &precio, &iva);
```

**Alternativas conocidas para la biblioteca:** libdecimal, libgmp, o implementación propia (paquete BCD).

**No es ejecución de IBM i, es matemática.** Por eso va acá y no en runtime-required.

---

## 2. EVAL(H) — half-adjust

```rpg
EVAL(H) precio = total / cantidad;
```

```c
bbk_dec_div_round(&precio, &total, &cantidad, BBK_ROUND_HALF_UP);
```

Mismo patrón: la regla de redondeo va como flag al helper de la biblioteca decimal.

---

## 3. VARCHAR (strings de longitud variable)

**El problema:** RPG `VARCHAR(n)` almacena longitud actual + datos. C tiene `char[]` con terminador `\0`.

**La traducción:** struct con prefijo de longitud:

```c
typedef struct {
    uint16_t length;
    uint16_t capacity;
    char data[];        // flexible array member, C99
} bbk_varchar_t;
```

```rpg
DCL-S nombre VARCHAR(50);
nombre = 'Nicolas';
```

```c
bbk_varchar_t *nombre = bbk_varchar_alloc(50);
bbk_varchar_set(nombre, "Nicolas");
```

**Alternativamente** (más simple, peor performance):

```c
typedef struct {
    uint16_t length;
    char data[51];      // capacity + 1
} bbk_varchar_50_t;     // un tipo por capacidad declarada
```

---

## 4. CHAR (strings de longitud fija)

**El problema:** RPG `CHAR(n)` rellena con blancos a la derecha hasta longitud `n`. C `char[n]` no tiene esa convención.

**La traducción:** asignaciones a CHAR pad explícitamente:

```rpg
DCL-S codigo CHAR(5);
codigo = 'AB';
// codigo ahora es 'AB   ' (con 3 blancos)
```

```c
char codigo[5];
bbk_char_assign(codigo, 5, "AB");
// internamente: strncpy + relleno con ' ' hasta 5
```

---

## 5. Date / Time / Timestamp

**El problema:** C tiene `time_t` (segundos desde epoch) y `struct tm`, pero no soporta directamente fechas anteriores a 1970 ni operaciones tipo "agregar 3 meses".

**La traducción:** struct propio con helpers:

```c
typedef struct { int16_t year; int8_t month; int8_t day; } bbk_date_t;
typedef struct { int8_t hour; int8_t min; int8_t sec; int32_t usec; } bbk_time_t;
typedef struct { bbk_date_t date; bbk_time_t time; } bbk_timestamp_t;

bbk_date_t bbk_date_add_days(bbk_date_t d, int32_t days);
int32_t    bbk_date_diff_days(bbk_date_t a, bbk_date_t b);
// etc.
```

Mapeo de BIFs:

| BIF RPG | Helper C |
|---|---|
| `%DATE(s:fmt)` | `bbk_date_parse(s, fmt)` |
| `%DAYS(n)` | (devuelve una duración, internamente un int32_t) |
| `%DIFF(a:b:fmt)` | `bbk_date_diff_X(a, b)` |

---

## 6. Indicators (estilo legacy *IN01-*IN99)

**El problema:** los 99 indicators numéricos de RPG son booleanos globales `'1'`/`'0'` (carácter, no bit). Las opcodes legacy los setean implícitamente. Los expresiones legacy hacen `*IN50` para leer indicator 50.

**La traducción:**

```c
// En bbk-runtime, declarado en bbk-runtime.h:
extern bbk_indicator_t bbk_indicators[100];   // *IN00 a *IN99
extern bbk_indicator_t bbk_inlr;              // *INLR
// etc.

#define _IN(n) (bbk_indicators[n])
```

```rpg
IF *IN50 = *ON;
  // ...
ENDIF;
```

```c
if (_IN(50) == BBK_ON) {
    // ...
}
```

Los opcodes que setean indicators (`CHAIN`, `READ`, etc.) llaman a helpers que actualizan `bbk_indicators[]` como side effect.

**En código moderno (con BIFs `%FOUND`, `%EOF`, etc.):** la traducción es más limpia — esas BIFs devuelven `bool` y el lowering emite `if (bbk_found(...))` sin tocar el array de indicators.

---

## 7. SELECT / WHEN / OTHER → cadena if/else

```rpg
SELECT;
WHEN tipo = 'A';
  procesarA();
WHEN tipo = 'B' OR tipo = 'C';
  procesarBC();
OTHER;
  procesarDefault();
ENDSL;
```

```c
if (bbk_char_eq(tipo, "A")) {
    procesarA();
} else if (bbk_char_eq(tipo, "B") || bbk_char_eq(tipo, "C")) {
    procesarBC();
} else {
    procesarDefault();
}
```

**No se puede mapear a `switch`** porque RPG `WHEN` toma una expresión booleana arbitraria, no un valor que se compara contra el discriminante. La traducción a if/else en cadena es directa.

---

## 8. Arrays 1-indexados → arrays C 0-indexados

```rpg
DCL-S nums INT(10) DIM(10);
nums(1) = 100;
nums(10) = 200;
```

```c
int32_t nums[10];
nums[0] = 100;     // nums(1) en RPG
nums[9] = 200;     // nums(10) en RPG
```

**Decisión de diseño del frontend:** restar 1 al subíndice durante el lowering. Si el subíndice es una variable, emitir `nums[i - 1]`. Optimización opcional: cuando es literal, computar en tiempo de compilación.

**Alternativa más fea pero a veces necesaria:** usar arrays C de tamaño N+1 y desperdiciar el índice 0. Más fácil para subíndices variables; peor uso de memoria.

---

## 9. Data structures con OVERLAY

```rpg
DCL-DS persona QUALIFIED;
  nombre   CHAR(50);
  apellido CHAR(50);
  fullName CHAR(101) POS(1) OVERLAY(persona);
END-DS;
```

```c
typedef struct {
    union {
        struct {
            char nombre[50];
            char apellido[50];
            char filler[1];     // para alinear a 101 total si hace falta
        };
        char fullName[101];
    };
} persona_ds_t;
```

C99 soporta **anonymous unions** dentro de structs (extensión común desde C11, pero gcc lo acepta como extensión en C99). Permite mantener el acceso `persona.fullName` sin un prefijo extra.

---

## 10. DCL-DS QUALIFIED y acceso `.`

```rpg
DCL-DS emp QUALIFIED;
  id   INT(10);
  nombre CHAR(50);
END-DS;

emp.id = 100;
```

```c
typedef struct {
    int32_t id;
    char nombre[50];
} emp_t;

emp_t emp;
emp.id = 100;
```

Mapeo directo. C ya tiene `.` para acceso a struct members.

---

## 11. LIKE y LIKEDS — type inheritance

```rpg
DCL-S total LIKE(precio);                 // mismo tipo que precio
DCL-DS empClone LIKEDS(emp);              // misma estructura que emp
```

```c
// Resuelto en compile time por el frontend:
bbk_decimal_t total;                      // copia el tipo decimal de precio
emp_t empClone;                           // alias del typedef de emp
```

**Característica del frontend:** mantener una tabla de símbolos con tipos; cuando aparece `LIKE`, sustituir por el tipo resuelto.

---

## 12. BIFs traducibles a funciones C

Casi todas las BIFs son traducciones directas a llamadas a helpers del runtime:

| BIF | Equivalente C |
|---|---|
| `%TRIM(s)` | `bbk_trim(s)` |
| `%TRIML(s)` | `bbk_triml(s)` |
| `%TRIMR(s)` | `bbk_trimr(s)` |
| `%SUBST(s:start:len)` | `bbk_substr(s, start, len)` |
| `%SCAN(needle:hay)` | `bbk_scan(needle, hay)` |
| `%LEN(s)` | `bbk_len(s)` (para VARCHAR; para CHAR es el tamaño fijo conocido) |
| `%CHAR(n)` | `bbk_to_char(n)` (varias sobrecargas por tipo) |
| `%DEC(s)` | `bbk_to_dec(s, prec, scale)` |
| `%INT(n)`, `%INTH(n)` | `bbk_to_int(n)`, `bbk_to_int_round(n)` |
| `%ABS(n)` | `bbk_abs(n)` o `abs()`/`llabs()` para enteros |
| `%ELEM(arr)` | constante en tiempo de compilación (`sizeof(arr)/sizeof(arr[0])`) |
| `%ADDR(v)` | `&v` |
| `%SIZE(v)` | `sizeof(v)` o constante computada |

**Patrón común:** una BIF → una función C en la biblioteca de runtime. La gramática de la BIF se preserva como llamada a función. Lo único distinto es el separador `:` de RPG que se traduce a `,` de C.

---

## 13. Initialization defaults

**RPG:** variables sin `INZ` se inicializan automáticamente:
- Numéricos → 0
- Alfanuméricos → blanks (`' '`)
- Date → `0001-01-01`
- Time → `00.00.00`

**C:** locales sin inicializar son indeterminados. Globales y `static` se inicializan a cero.

**La traducción:** el frontend tiene que emitir initializers explícitos para variables locales que en RPG son auto-inicializadas.

```rpg
DCL-S contador INT(10);            // 0 por default
DCL-S nombre   CHAR(50);           // blanks por default
```

```c
int32_t contador = 0;
char nombre[50];
memset(nombre, ' ', 50);           // o un helper bbk_char_clear(nombre, 50)
```

---

## 14. Numeric promotion / conversiones explícitas

RPG hace conversiones implícitas con reglas de promoción decimal definidas (el resultado se computa en precisión suficiente y se ajusta al destino). C tiene sus propias reglas pero distintas (promociones a `int`, usual arithmetic conversions, etc.).

**La traducción:** el frontend emite conversiones explícitas cuando hace falta para preservar la semántica RPG.

```rpg
DCL-S x INT(10);
DCL-S y FLOAT(8);

x = y;     // float a int — semántica RPG: truncar (o redondear con (H))
```

```c
int32_t x;
double y;

x = (int32_t)y;          // truncate, semántica equivalente
// o:
x = (int32_t)bbk_round(y);    // si fue EVAL(H)
```

---

## 15. Free-form vs fixed-form → AST unificado

**El problema:** la `rpg-frontend` recibe código en tres formas distintas (fully-free, mixed con `/FREE`, fixed-form puro). El lowering debería trabajar sobre **una sola representación**.

**La traducción:** el frontend normaliza todas las formas a un mismo AST. El parser detecta el modo en el header del archivo y dispatcha al sub-parser apropiado, pero todos producen los mismos nodos de AST.

No es una traducción RPG → C propiamente dicha; es trabajo del frontend. Pero conviene mencionarlo acá porque elimina una dimensión de complejidad antes de que el lowering la vea.

---

## 16. Service program binding → linker

**RPG:** los `*SRVPGM` se bindean usando *binding directories* que el compilador resuelve durante la creación del `*PGM` o el `*MODULE`.

**C:** linker estándar (`gcc` linkea objetos `.o` y bibliotecas `.so`/`.dll`). El mapeo conceptual:

| RPG | C (gcc + ld) |
|---|---|
| `*MODULE` | `.o` object file |
| `*PGM` | `.exe` |
| `*SRVPGM` | `.so` / `.dll` shared library |
| Binding directory | linker flags (`-l`, `-L`) |
| `EXPORT` | símbolo visible (default sin `static`) |
| `IMPORT` | `extern` |

**No es exactamente lo mismo** (no hay activation groups, scope diferente), pero el modelo de archivos compilados sueltos que se linkean para producir un ejecutable/biblioteca es análogo.

---

## 17. Multi-occurrence Data Structure (legacy OCCURS)

```rpg
DCL-DS reg OCCURS(100);
  campo1 INT(10);
  campo2 CHAR(20);
END-DS;
```

```c
typedef struct {
    int32_t campo1;
    char    campo2[20];
} reg_t;

reg_t reg[100];
int32_t reg_occur = 0;        // emula el OCCUR pointer de RPG
```

El opcode legacy `OCCUR n reg` se traduce a setear el índice `reg_occur = n - 1`. Cualquier acceso a `reg.campo1` se traduce a `reg[reg_occur].campo1`.

(En código moderno se evita OCCURS y se usa `DIM` con acceso explícito por índice — ese caso ya está cubierto en §8.)

---

## 18. Embedded SQL (SQLRPGLE)

Las statements `EXEC SQL ... END-EXEC` son **pre-procesadas** por el SQL precompiler antes de la compilación RPG. Generan llamadas a APIs SQLI.

**La traducción equivalente en C:** usar una biblioteca SQL embebido (ESQL/C estándar o equivalente). Idealmente el frontend procesa las statements SQL en una fase separada y emite código C que use una API de DB.

**Decisión de diseño pendiente:** qué motor de DB usa el `bbk-runtime` para emular DB2/400. Opciones: SQLite (embebido, fácil), PostgreSQL (más capabilidades pero requiere servidor). Esto cae más en runtime-required, pero el aspecto puramente sintáctico de "EXEC SQL ... END-EXEC → llamada a API" es traducible.

---

## Resumen

Constructos que requieren mapeo pero son resolubles por el `bbk-compiler` + helpers C de uso general:

- **Decimales exactos** (packed/zoned) → biblioteca decimal
- **EVAL(H)** half-adjust → helper con flag de redondeo
- **VARCHAR / CHAR** con relleno → structs / helpers de string
- **Date / Time / Timestamp** → structs y helpers
- **Indicators legacy** → array global de booleanos
- **SELECT/WHEN/OTHER** → cadena if/else
- **Arrays 1-indexados** → offset en el frontend
- **DS con OVERLAY** → union dentro de struct
- **DCL-DS QUALIFIED** → struct
- **LIKE / LIKEDS** → resolución en el frontend
- **BIFs** → llamadas a helpers de runtime
- **Initialization defaults** → initializers explícitos emitidos por el frontend
- **Conversiones numéricas** → casts explícitos con reglas RPG
- **Free-form vs fixed-form** → unificación a AST en el frontend
- **Service program binding** → linker
- **Multi-occurrence DS** → array indexado con cursor
- **Embedded SQL** → llamadas a API de DB

Todo esto es trabajo "razonable" para un compilador. La biblioteca de runtime asociada (`bbk-runtime`) que da soporte a estos items es esencialmente una biblioteca matemática/de strings/de fechas — no requiere emular el sistema operativo de IBM i.
