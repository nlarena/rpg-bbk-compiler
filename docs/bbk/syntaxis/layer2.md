# BBK Grammar — Layer 2

**Estado:** implementada y verificada
**Fuente del parser:** [`plugin-bbk/src/main/grammar/BBK.bnf`](../../../plugin-bbk/src/main/grammar/BBK.bnf)
**Pre-requisito:** [`layer1.md`](layer1.md)
**Archivos de prueba:** [`tests/boxbreaker/examples/`](../../../tests/boxbreaker/examples/) (válidos) y [`bad-09`](../../../tests/boxbreaker/examples/bad/bad-09-bad-ctl-opt.bbk), [`bad-10`](../../../tests/boxbreaker/examples/bad/bad-10-bad-file-decl.bbk), [`bad-11`](../../../tests/boxbreaker/examples/bad/bad-11-bad-data-structures.bbk) (errores intencionales)

---

## Alcance

Layer 2 completa el conjunto de **declaraciones a nivel módulo** sin entrar todavía a procedures, statements ni expresiones. Agrega tres construcciones:

| Construcción | Forma | Ejemplo |
|---|---|---|
| `CTL-OPT` | Directiva de módulo | `CTL-OPT MAIN(helloMain) NOMAIN DEBUG;` |
| `DCL-F` | Declaración de archivo | `DCL-F customers DISK USAGE(*INPUT) KEYED EXTNAME("CUSTOMER");` |
| `DCL-DS` | Data structure | `DCL-DS person QUALIFIED { id INT(10); name CHAR(50); }` |

**Novedad clave:** introduce la sintaxis de **bloques con `{ }`** vía `DCL-DS { subfield; subfield; }`. Este patrón se reusará en Layers posteriores (`DCL-PROC { body }`, `if (cond) { ... }`, etc.).

**Lo que sigue sin cubrirse:**
- `DCL-PR`, `DCL-PROC` — Layer 3
- Statements, expressions, file ops, directivas `PRE-*` — Layers 4-6

---

## Top-level actualizado

```bnf
top_level_item ::= variable_declaration       // L1
                 | constant_declaration        // L1
                 | ctl_opt_statement           // L2
                 | data_structure_declaration  // L2
                 | file_declaration            // L2
                 | unknown_item
```

---

## Producciones BNF

### CTL-OPT statement

```bnf
ctl_opt_statement ::= KW_CTL_OPT ctl_opt_keyword* SEMI {pin=1}

ctl_opt_keyword ::= IDENT ctl_opt_args?

ctl_opt_args ::= LPAREN ctl_opt_arg (COLON ctl_opt_arg)* RPAREN {pin=1}

private ctl_opt_arg ::= literal | KW_TRUE | KW_FALSE | KW_NULL | STAR_IDENT | IDENT
```

**Forma:** `CTL-OPT keyword keyword(arg) keyword(a:b:c) ... ;`

Los keywords (`MAIN`, `NOMAIN`, `DFTACTGRP`, `DEBUG`, etc.) **no son tokens dedicados** — se parsean como `IDENT` y la semántica se interpreta a nivel posterior. Esto evita explotar la lista de tokens con cada opción de IBM.

**Argumentos aceptados:**
- Literales (`INT_LIT`, `STR_LIT`, etc.)
- `true` / `false` / `null`
- `STAR_IDENT` (ej. `*NO`, `*NEW`, `*CALLER`)
- `IDENT` (ej. nombres de procedures, librerías)

Multi-args van separados por `:` (no por `,`): `OPTION(*SRCSTMT:*NODEBUGIO)`.

### File declaration

```bnf
file_declaration ::= KW_DCL_F IDENT f_keyword+ SEMI {pin=1}

f_keyword ::= simple_f_keyword
            | usage_f_keyword
            | extname_f_keyword
            | extfile_f_keyword
            | prefix_f_keyword
            | rename_f_keyword
            | indds_f_keyword
            | infds_f_keyword

simple_f_keyword  ::= KW_KEYED | KW_USROPN | KW_DISK | KW_PRINTER | KW_WORKSTN | KW_SEQ
usage_f_keyword   ::= KW_USAGE   LPAREN STAR_IDENT (COLON STAR_IDENT)* RPAREN {pin=1}
extname_f_keyword ::= KW_EXTNAME LPAREN STR_LIT RPAREN {pin=1}
extfile_f_keyword ::= KW_EXTFILE LPAREN STR_LIT RPAREN {pin=1}
prefix_f_keyword  ::= KW_PREFIX  LPAREN IDENT (COLON INT_LIT)? RPAREN {pin=1}
rename_f_keyword  ::= KW_RENAME  LPAREN IDENT COLON IDENT RPAREN {pin=1}
indds_f_keyword   ::= KW_INDDS   LPAREN IDENT RPAREN {pin=1}
infds_f_keyword   ::= KW_INFDS   LPAREN IDENT RPAREN {pin=1}
```

**Forma:** `DCL-F filename keyword keyword(args) ... ;`

A diferencia de `CTL-OPT`, los keywords de DCL-F **sí son tokens dedicados** (`KW_USAGE`, `KW_EXTNAME`, etc.) porque cada uno tiene gramática propia para sus argumentos. Esto da error reporting más preciso.

**Restricciones de tipo en argumentos:**
- `USAGE(*X:*Y:...)` — solo `STAR_IDENT`, no `IDENT`
- `EXTNAME("file")` / `EXTFILE("file")` — solo `STR_LIT`
- `PREFIX(prefix:n)` — `IDENT` opcionalmente seguido de `:INT_LIT`
- `RENAME(old:new)` — dos `IDENT` separados por `:`
- `INDDS(name)` / `INFDS(name)` — un `IDENT`

`f_keyword+` (uno o más) — un `DCL-F` siempre necesita al menos un keyword. `DCL-F customers;` da error.

### Data structure declaration

```bnf
data_structure_declaration ::= KW_DCL_DS IDENT ds_modifier* ds_tail {pin=1}

private ds_tail ::= ds_body | SEMI

ds_body ::= LBRACE ds_subfield* RBRACE {pin=1}

ds_subfield ::= IDENT type_specification var_modifier* SEMI {pin=1}
```

**Dos formas:**

1. **Inline con body** — declara la estructura con sus subfields:
   ```bbk
   DCL-DS person QUALIFIED {
     id     INT(10);
     name   CHAR(50);
     active BOOL;
   }
   ```

2. **Sin body, solo `;`** — usado con `LIKEDS` o `TEMPLATE` para reusar estructuras:
   ```bbk
   DCL-DS homeAddress LIKEDS(addressTemplate);
   DCL-DS customerRec EXTNAME("CUSTOMER") QUALIFIED;
   ```

### DS modifiers

```bnf
ds_modifier ::= qualified_modifier      // reusado de var_modifier
              | template_modifier        // L2
              | align_modifier           // L2
              | dim_modifier             // reusado
              | based_modifier           // reusado
              | inz_modifier             // reusado
              | extname_ds_modifier      // L2
              | likeds_ds_modifier       // L2
              | likerec_ds_modifier      // L2
              | infds_ds_modifier        // L2

template_modifier     ::= KW_TEMPLATE
align_modifier        ::= KW_ALIGN
extname_ds_modifier   ::= KW_EXTNAME LPAREN STR_LIT RPAREN {pin=1}
likeds_ds_modifier    ::= KW_LIKEDS  LPAREN IDENT RPAREN {pin=1}
likerec_ds_modifier   ::= KW_LIKEREC LPAREN IDENT (COLON IDENT)? RPAREN {pin=1}
infds_ds_modifier     ::= KW_INFDS   LPAREN IDENT RPAREN {pin=1}
```

**Modifiers DS-específicos:**
- `TEMPLATE` — la DS no aloca storage, solo define el layout para reusar con `LIKEDS`
- `ALIGN` — alinea subfields en boundaries naturales
- `EXTNAME("FILE")` — toma los subfields del schema externo del archivo
- `LIKEDS(other)` — copia el layout de otra DS
- `LIKEREC(rec)` — copia el layout del record format de un archivo
- `INFDS(name)` — declara que esta DS es la file info DS del archivo `name`

**Modifiers reusados de `var_modifier`** (Layer 1):
- `QUALIFIED` — fuerza acceso vía `ds.subfield` en vez de variables directas
- `DIM(n)` — array de DSs
- `BASED(ptr)` — DS basada en puntero
- `INZ` — inicializa subfields a default

### Subfields

```bnf
ds_subfield ::= IDENT type_specification var_modifier* SEMI {pin=1}
```

Idénticos a `variable_declaration` pero **sin** el prefix `DCL-S`. Reusan todos los modifiers de Layer 1 (`INZ`, `OVERLAY`, `POS`, etc.).

Ejemplo con `OVERLAY` (union de memoria):
```bbk
DCL-DS dateRecord QUALIFIED {
  fullDate CHAR(8);
  year     CHAR(4) OVERLAY(fullDate:1);
  month    CHAR(2) OVERLAY(fullDate:5);
  day      CHAR(2) OVERLAY(fullDate:7);
}
```

---

## Ejemplos

### Código válido

```bbk
CTL-OPT MAIN(helloMain) NOMAIN DFTACTGRP(*NO) DEBUG;

DCL-F customers DISK USAGE(*INPUT) KEYED EXTNAME("CUSTOMER");
DCL-F orders    DISK USAGE(*INPUT:*OUTPUT:*UPDATE) KEYED EXTNAME("ORDER");
DCL-F report    PRINTER USAGE(*OUTPUT) USROPN;

DCL-DS person QUALIFIED {
  firstName VARCHAR(50);
  lastName  VARCHAR(50);
  age       INT(10);
  salary    PACKED(9:2);
}

DCL-DS addressTemplate TEMPLATE {
  street VARCHAR(100);
  city   VARCHAR(50);
  zip    CHAR(10);
}

DCL-DS homeAddress LIKEDS(addressTemplate);
DCL-DS workAddress LIKEDS(addressTemplate);

DCL-DS employees QUALIFIED DIM(1000) {
  id     INT(10);
  name   VARCHAR(50);
  active BOOL;
}

DCL-DS customerRec EXTNAME("CUSTOMER") QUALIFIED;
```

Ver [`01-hello.bbk`](../../../tests/boxbreaker/examples/01-hello.bbk), [`06-data-structures.bbk`](../../../tests/boxbreaker/examples/06-data-structures.bbk), [`10-files.bbk`](../../../tests/boxbreaker/examples/10-files.bbk).

### Errores detectados

| Archivo | Tipo de error |
|---|---|
| `bad-09-bad-ctl-opt.bbk` | CTL-OPT con args sin cerrar, vacíos, dos puntos sobrantes, falta `;` |
| `bad-10-bad-file-decl.bbk` | DCL-F sin nombre, sin keywords, USAGE/EXTNAME con tipo de arg incorrecto |
| `bad-11-bad-data-structures.bbk` | DCL-DS sin nombre/body, LIKEDS mal, subfields sin tipo/`;`, body sin cerrar |

---

## Notas de implementación

### El patrón de bloque (`LBRACE ... RBRACE`)

Layer 2 introduce el primer bloque del lenguaje en `ds_body`:

```bnf
ds_body ::= LBRACE ds_subfield* RBRACE {pin=1}
```

El `{pin=1}` después de `LBRACE` es clave: si el `{` aparece, el parser se compromete a cerrar con `}`. Si el `}` falta (body sin terminar), reporta error claramente.

Este mismo patrón se reusará para:
- `procedure_body` en Layer 3
- `if`/`else`/`while` blocks en Layer 4
- `select { when { ... } }` en Layer 4

### Reuso de modifiers entre `DCL-S` y `DCL-DS`

`var_modifier` (definido en Layer 1) se reusa para subfields de `DCL-DS`. Eso significa que un subfield puede llevar `INZ`, `OVERLAY`, `DIM`, etc. — las mismas opciones que una variable standalone.

Los modifiers exclusivos de DS (`TEMPLATE`, `EXTNAME`, `LIKEDS`, `LIKEREC`, `INFDS`) van en `ds_modifier`, separado.

Hay duplicación intencional en algunos casos:
- `KW_EXTNAME(...)` aparece como `extname_f_keyword` en DCL-F y como `extname_ds_modifier` en DCL-DS — misma sintaxis pero PSI elements distintos.
- `KW_INFDS(...)` idem como `infds_f_keyword` y `infds_ds_modifier`.

Esta separación facilita después el análisis semántico (saber si un EXTNAME está en contexto F o DS sin ambigüedad).

### CTL-OPT keywords son IDENT, no tokens

A diferencia de DCL-F donde cada keyword (`USAGE`, `EXTNAME`, etc.) es un token dedicado, los keywords de CTL-OPT (`MAIN`, `NOMAIN`, `DFTACTGRP`, `THREAD`, etc.) se parsean como `IDENT` genéricos.

**Razón:** IBM agrega nuevos keywords de CTL-OPT en cada release. Tenerlos como tokens dedicados forzaría a actualizar el lexer/parser con cada cambio. Mejor parsearlos como identificadores y validar en una fase semántica posterior contra una tabla actualizable.

**Trade-off:** errores de typo en keywords (ej. `DFTACTGROP` en vez de `DFTACTGRP`) no se detectan a nivel parser — son aceptados como IDENT válido. Se detectarán en validación semántica.

---

## Próxima capa

`layer3.md` (pendiente) — agregará `DCL-PR` y `DCL-PROC` con parámetros y bodies. Es el más grande porque trae **statements y expressions** dentro del body de procedures.
