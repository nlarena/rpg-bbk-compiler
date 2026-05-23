# Similitudes entre RPG y C

**Propósito:** identificar constructos que se traducen casi 1:1 entre RPG y C99. El `bbk-compiler` los puede emitir de forma directa en el lowering BBK → C, sin necesidad de biblioteca de runtime adicional ni cambios semánticos.

Documentos relacionados:
- [`translatable.md`](translatable.md) — distinto pero traducible con mapeo explícito
- [`runtime-required.md`](runtime-required.md) — no traducible solo; requiere runtime

---

## 1. Operadores aritméticos

| RPG | C | Notas |
|---|---|---|
| `+` | `+` | Suma |
| `-` | `-` | Resta (binaria y unaria) |
| `*` | `*` | Multiplicación |
| `/` | `/` | División (semántica difiere según tipo — ver translatable) |
| `**` | `pow(a, b)` | Exponenciación. C no tiene operador, requiere `<math.h>`. Mapeo trivial. |
| `+` unario | `+` unario | Idéntico |
| `-` unario | `-` unario | Idéntico |

**Asociatividad y precedencia:** equivalentes para tipos numéricos básicos. `**` en RPG es right-associative igual que `pow()` anidado en C.

---

## 2. Operadores relacionales

| RPG | C |
|---|---|
| `=` | `==` |
| `<>` | `!=` |
| `<` | `<` |
| `>` | `>` |
| `<=` | `<=` |
| `>=` | `>=` |

**Único cambio:** `=` en RPG es comparación; en C es asignación. El frontend tiene que distinguir por contexto (en RPG el contexto sintáctico lo deja claro porque la asignación es por `EVAL` o por statement `<lvalue> = <expr>`).

---

## 3. Operadores lógicos

| RPG | C |
|---|---|
| `AND` | `&&` |
| `OR` | `\|\|` |
| `NOT` | `!` |

**Short-circuit evaluation:** ambos lenguajes la implementan igual para `AND`/`&&` y `OR`/`||`.

---

## 4. Estructuras de control

### 4.1 If / Else

```rpg
IF <cond>;
  <statements>;
ELSEIF <cond>;
  <statements>;
ELSE;
  <statements>;
ENDIF;
```

```c
if (<cond>) {
    <statements>;
} else if (<cond>) {
    <statements>;
} else {
    <statements>;
}
```

Mapeo 1:1. `ELSEIF` se vuelve `else if`.

### 4.2 FOR

```rpg
FOR i = 1 TO 10;
  <statements>;
ENDFOR;
```

```c
for (int i = 1; i <= 10; i++) {
    <statements>;
}
```

Con `BY n`:

```rpg
FOR i = 0 TO 100 BY 5;
```

```c
for (int i = 0; i <= 100; i += 5) {
```

Con `DOWNTO`:

```rpg
FOR i = 10 DOWNTO 1;
```

```c
for (int i = 10; i >= 1; i--) {
```

**Caveat:** RPG indexa desde 1 por convención cultural pero el `FOR` no impone esto. Los arrays sí lo hacen (ver translatable).

### 4.3 DOW (do while — pre-test)

```rpg
DOW <cond>;
  <statements>;
ENDDO;
```

```c
while (<cond>) {
    <statements>;
}
```

### 4.4 DOU (do until — post-test)

```rpg
DOU <cond>;
  <statements>;
ENDDO;
```

```c
do {
    <statements>;
} while (!(<cond>));
```

Atención: `DOU` continúa **hasta** que la condición sea verdadera (termina cuando es true). `do { } while` continúa **mientras** la condición sea verdadera. Por eso el `!` en la traducción.

### 4.5 LEAVE / ITER

| RPG | C |
|---|---|
| `LEAVE;` | `break;` |
| `ITER;` | `continue;` |

### 4.6 RETURN

```rpg
RETURN;             // void return
RETURN expr;        // return value
```

```c
return;
return expr;
```

---

## 5. Tipos numéricos básicos

Algunos tipos numéricos de RPG mapean directo a tipos C estándar:

| RPG | C |
|---|---|
| `INT(3)` | `int8_t` |
| `INT(5)` | `int16_t` |
| `INT(10)` | `int32_t` |
| `INT(20)` | `int64_t` |
| `UNS(3)` | `uint8_t` |
| `UNS(5)` | `uint16_t` |
| `UNS(10)` | `uint32_t` |
| `UNS(20)` | `uint64_t` |
| `FLOAT(4)` | `float` |
| `FLOAT(8)` | `double` |

**Disponibles vía `<stdint.h>`** (C99). El lowering puede emitir estos tipos directamente.

**Caveat:** los tipos `PACKED`, `ZONED`, `BINDEC` **NO** mapean directo — son decimales exactos. Van en [`translatable.md`](translatable.md).

---

## 6. Procedures y funciones

### 6.1 Sub-procedure con retorno

```rpg
DCL-PROC sumar EXPORT;
  DCL-PI *N INT(10);
    a INT(10) CONST;
    b INT(10) CONST;
  END-PI;

  RETURN a + b;
END-PROC;
```

```c
int32_t sumar(int32_t a, int32_t b) {
    return a + b;
}
```

### 6.2 Sub-procedure sin retorno

```rpg
DCL-PROC saludar;
  DCL-PI *N;
  END-PI;
  // ...
END-PROC;
```

```c
static void saludar(void) {
    // ...
}
```

(Sin `EXPORT` → `static` en C.)

### 6.3 Parámetros

| RPG | C |
|---|---|
| `VALUE` | parámetro por valor (default en C) |
| sin `VALUE` (por referencia) | puntero (`T *`) |
| `CONST` | `const T` o `const T *` |

```rpg
DCL-PI proc;
  x INT(10) VALUE;        // by value
  y INT(10);              // by reference
  z INT(10) CONST;        // const, by reference
END-PI;
```

```c
void proc(int32_t x, int32_t *y, const int32_t *z) {
```

---

## 7. Constantes

### 7.1 Constantes literales

```rpg
DCL-C MAX_LEN 100;
DCL-C PI 3.14159;
DCL-C SALUDO 'Hola';
```

```c
#define MAX_LEN 100
#define PI 3.14159
static const char SALUDO[] = "Hola";
```

O con `const` de C:

```c
static const int32_t MAX_LEN = 100;
static const double PI = 3.14159;
```

(Mapeo a `#define` vs `const` es decisión de estilo. `const` es más type-safe.)

### 7.2 Figurative constants traducibles directo

| RPG | C |
|---|---|
| `*ON` | `1` (o `true` con `<stdbool.h>`) |
| `*OFF` | `0` (o `false`) |
| `*ZERO` / `*ZEROS` | `0` |
| `*NULL` | `NULL` |

(Otras figurative constants como `*BLANK`, `*HIVAL`, `*LOVAL` dependen del tipo del destino — van en translatable.)

---

## 8. Storage classes

| RPG | C |
|---|---|
| `STATIC` (en variable local) | `static` |
| Variable global de módulo | `static` (file-scope) por default; `extern` si `EXPORT` |
| `EXPORT` | (sin `static`) — visible al linker |
| `IMPORT` | `extern` |
| Variable local (default `AUTOMATIC`) | variable local C |

---

## 9. Identificadores

Reglas léxicas similares: empiezan con letra, después letras/dígitos/`_`. RPG agrega `#`, `$`, `@` por compatibilidad EBCDIC. El frontend los puede transliterar a `_` para C.

**Única diferencia material:** RPG es **case-insensitive**. La normalización a un canon (típicamente lowercase o uppercase) se hace en el frontend. Después la emisión a C es trivial.

---

## 10. Comentarios

| RPG | C |
|---|---|
| `// comentario hasta fin de línea` | `// comentario hasta fin de línea` (C99) |
| `/* ... */` (no estándar en RPG, pero algunos editores aceptan) | `/* ... */` |

Mapeo directo.

---

## 11. Punteros (semántica básica)

```rpg
DCL-S p POINTER;
DCL-S x INT(10) BASED(p);

p = %ADDR(otroVar);
x = 42;                    // escribe en *p tratado como int32_t
```

```c
void *p;
int32_t *x_p;              // p tipado al uso

p = &otroVar;
x_p = (int32_t *)p;
*x_p = 42;
```

**Limitación del mapeo:** RPG con `BASED` permite re-aliasing dinámico que en C requiere casts explícitos. La aritmética de punteros básica (sumar offsets) es similar.

---

## 12. Sequence points en expresiones

C99 define sequence points en `&&`, `||`, `?:`, `,`, fin de full expression, y llamadas a función.

RPG no usa la terminología pero su comportamiento de evaluación de expresiones coincide en los puntos prácticos: `AND`/`OR` con short-circuit, evaluación left-to-right en general. Las expresiones simples sin efectos colaterales (RPG no tiene `++`/`--` ni asignaciones embebidas en expresiones) evitan los undefined behaviors típicos de C.

**Ventaja para el lowering:** las expresiones RPG son más restrictivas que las de C, así que generar C correcto desde ellas es más fácil que el camino inverso.

---

## 13. Modularidad

| RPG | C |
|---|---|
| Módulo (`*MODULE`) | Translation unit (`.c` + headers) |
| Programa (`*PGM`) — bindea uno o más módulos | Ejecutable (`.exe`) — linkea uno o más `.o` |
| Service program (`*SRVPGM`) — bindea módulos para uso compartido | Shared library (`.dll` / `.so`) — linkea módulos compartibles |
| Main procedure | `int main(void)` o `int main(int argc, char *argv[])` |

**Caveat:** la relación `*PGM` ↔ `*MODULE` ↔ `*SRVPGM` con activation groups tiene complejidades semánticas que no mapean limpiamente. La estructura **de archivos** sí.

---

## Resumen

Lo que mapea directo entre RPG y C99:

- **Sintaxis de expresiones:** operadores aritméticos, relacionales, lógicos.
- **Sintaxis de control:** `IF`/`FOR`/`DOW`/`DOU`/`LEAVE`/`ITER`/`RETURN`.
- **Tipos numéricos enteros y flotantes** (`INT`, `UNS`, `FLOAT`).
- **Procedures con parámetros y retorno.**
- **Storage classes** (`STATIC`, `EXPORT`).
- **Constantes literales** (`DCL-C`).
- **Identificadores** (modulo case y caracteres especiales).
- **Comentarios.**
- **Punteros básicos.**
- **Modularidad a nivel de archivos** (módulo, programa, srvpgm).

Para el `bbk-compiler` este conjunto es la parte fácil del lowering: emite código C estándar sin necesidad de la biblioteca `bbk-runtime`.
