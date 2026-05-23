# BBK Grammar — Layer 1

**Estado:** implementada y verificada
**Fuente del parser:** [`plugin-bbk/src/main/grammar/BBK.bnf`](../../../plugin-bbk/src/main/grammar/BBK.bnf)
**Archivos de prueba:** [`tests/boxbreaker/examples/`](../../../tests/boxbreaker/examples/) (válidos) y [`tests/boxbreaker/examples/bad/`](../../../tests/boxbreaker/examples/bad/) (con errores intencionales)

---

## Alcance

Layer 1 es la primera capa funcional del parser BBK. Reconoce las dos construcciones de declaración más simples a nivel módulo:

| Construcción | Forma | Ejemplo |
|---|---|---|
| `DCL-S` | Variable standalone | `DCL-S counter INT(10) INZ(0);` |
| `DCL-C` | Constante con nombre | `DCL-C MAX_RETRIES 5;` |

**Lo que NO cubre Layer 1** (cae al fallback `unknown_item` sin reportar errores):
- `CTL-OPT`, `DCL-F`, `DCL-DS` — cubiertos en Layer 2
- `DCL-PR`, `DCL-PROC` — Layer 3
- Statements, expressions, file ops, directivas `PRE-*` — Layers 4-6

---

## Producciones BNF

### Top-level

```bnf
translation_unit ::= top_level_item*

top_level_item ::= variable_declaration
                 | constant_declaration
                 | unknown_item
                 // (Layer 2+ agrega más alternativas)
```

### Variable declaration

```bnf
variable_declaration ::= KW_DCL_S IDENT type_specification var_modifier* SEMI {pin=1}
```

El `{pin=1}` significa que una vez consumido `DCL-S`, el parser se compromete con la regla. Cualquier fallo posterior se reporta como error en vez de hacer backtrack.

### Constant declaration

```bnf
constant_declaration ::= KW_DCL_C IDENT constant_value SEMI {pin=1}

constant_value ::= const_wrapper | literal | KW_TRUE | KW_FALSE | KW_NULL

const_wrapper ::= KW_CONST LPAREN (literal | IDENT) RPAREN {pin=1}
```

`DCL-C` acepta:
- Literales directos: `DCL-C PI 3.14159d;`
- Constantes figurativas: `DCL-C IS_DEBUG true;`
- Expresión envuelta en `CONST(...)`: `DCL-C DOUBLE CONST(MAX * 2);` (Layer 1 solo permite literal o IDENT adentro; expresiones reales vienen con Layer 5)

### Type specification

```bnf
type_specification ::= like_reference | primitive_type_spec

primitive_type_spec ::= primitive_type type_args?

primitive_type ::= KW_CHAR | KW_VARCHAR
                 | KW_PACKED | KW_ZONED | KW_BINDEC
                 | KW_INT | KW_UNS | KW_FLOAT
                 | KW_DATE | KW_TIME | KW_TIMESTAMP
                 | KW_BOOL | KW_POINTER | KW_VOID

type_args ::= LPAREN INT_LIT type_args_tail? RPAREN {pin=1}
private type_args_tail ::= COLON INT_LIT {pin=1}

like_reference ::= (KW_LIKE | KW_LIKEDS | KW_LIKEREC) LPAREN IDENT RPAREN {pin=1}
```

**Notas:**
- `type_args` tiene `{pin=1}` para que `CHAR()` o `INT(;` reporten error en vez de hacer backtrack silencioso.
- `like_reference` reusa los tokens `KW_LIKE`/`KW_LIKEDS`/`KW_LIKEREC` (los tres mismos que para el modifier de DCL-DS, pero aquí se usan como type-spec replacement).

### Modifiers

```bnf
var_modifier ::= inz_modifier
               | static_modifier
               | export_modifier
               | dim_modifier
               | based_modifier
               | qualified_modifier
               | overlay_modifier
               | pos_modifier

inz_modifier        ::= KW_INZ LPAREN modifier_value RPAREN {pin=1}
static_modifier     ::= KW_STATIC
export_modifier     ::= KW_EXPORT
dim_modifier        ::= KW_DIM LPAREN INT_LIT (COLON INT_LIT)? RPAREN {pin=1}
based_modifier      ::= KW_BASED LPAREN IDENT RPAREN {pin=1}
qualified_modifier  ::= KW_QUALIFIED
overlay_modifier    ::= KW_OVERLAY LPAREN IDENT (COLON INT_LIT)? RPAREN {pin=1}
pos_modifier        ::= KW_POS LPAREN INT_LIT RPAREN {pin=1}
```

`overlay_modifier` y `pos_modifier` se agregaron pensando en subfields de `DCL-DS` (Layer 2), pero también aplican a `DCL-S BASED(ptr)` para overlays sobre memoria basada.

### Literal

```bnf
literal ::= INT_LIT | INT_LIT_HEX | INT_LIT_OCT | FLOAT_LIT | DEC_LIT | STR_LIT
```

### Fallback

```bnf
private unknown_item ::= !<<eof>> any_token
external any_token ::= consumeAnyToken
```

`any_token` está implementado en Java (`BbkParserUtil.consumeAnyToken`) y avanza el lexer un token a la vez.

---

## Ejemplos

### Código válido (no debería marcar errores)

```bbk
DCL-S counter   INT(10) INZ(0);
DCL-S name      VARCHAR(100) INZ("");
DCL-S price     PACKED(9:2) INZ(19.95d);
DCL-S birthDate DATE;
DCL-S isActive  BOOL INZ(true);
DCL-S nums      INT(10) DIM(100);
DCL-S matrix    INT(10) DIM(10:10);
DCL-S basedInt  INT(10) BASED(ptr);
DCL-S priceCopy LIKE(price);
DCL-S sharedCnt INT(10) STATIC;

DCL-C MAX_RETRIES 5;
DCL-C PI          3.14159d;
DCL-C COMPANY     "Acme Corporation";
DCL-C IS_DEBUG    true;
DCL-C DOUBLED     CONST(MAX_RETRIES * 2);
```

Ver [`02-variables.bbk`](../../../tests/boxbreaker/examples/02-variables.bbk) y [`03-constants.bbk`](../../../tests/boxbreaker/examples/03-constants.bbk).

### Errores detectados

Los archivos en [`bad/`](../../../tests/boxbreaker/examples/bad/) cubren los siguientes casos:

| Archivo | Tipo de error |
|---|---|
| `bad-01-missing-type.bbk` | DCL-S sin tipo (típico: poner directo un modifier como tipo) |
| `bad-02-missing-semicolon.bbk` | Falta `;` al final |
| `bad-03-missing-name.bbk` | Falta IDENT del nombre |
| `bad-04-bad-type-args.bbk` | Args de tipo malformados: `CHAR()`, `INT(;`, `PACKED(9:)`, etc. |
| `bad-05-bad-modifiers.bbk` | INZ/DIM/BASED con args inválidos |
| `bad-06-missing-constant-value.bbk` | DCL-C sin valor o CONST mal |
| `bad-07-bad-like-reference.bbk` | LIKE/LIKEDS/LIKEREC con args inválidos |
| `bad-08-mixed-errors.bbk` | Múltiples errores mezclados con código válido (testea recovery) |

---

## Notas de implementación

### Pins

Cada regla principal usa `{pin=1}` para comprometerse después de consumir el primer token. Esto es lo que permite reportar errores específicos en vez de fallar silenciosamente con backtrack.

Ejemplo: `variable_declaration ::= KW_DCL_S IDENT type_specification var_modifier* SEMI {pin=1}`:
- Si vemos `DCL-S`, ya estamos parseando una variable declaration.
- Si falta `IDENT` después → "IDENT expected, got ...".
- Si falta `type_specification` → "type specification expected, got ...".
- Si falta `;` al final → "; expected, got ...".

### El truco del `type_args` con pin

Sin pin en `type_args`, casos como `CHAR()` pasaban silenciosos: el parser intentaba `type_args`, fallaba al ver `)` donde esperaba `INT_LIT`, hacía backtrack, y dejaba `()` colgando como `unknown_item`. Resultado: la `DCL-S` terminaba sin error pero también sin consumir los paréntesis.

Con `{pin=1}` en `type_args`, una vez visto `(`, el parser se compromete. Si lo que sigue no es `INT_LIT`, ERROR.

### IDs del lenguaje

- `BbkLanguage.INSTANCE.getID() = "BBK"`
- Element types se generan con `new BbkElementType("...")` bound a `BbkLanguage`
- Token types se generan con `new BbkTokenType("...")` también bound

Si los element types se generaran con `null` como lenguaje (como pasó en una versión temprana del scaffold), IntelliJ NO reporta los `PsiErrorElement` como squigglies aunque la PSI tree los contenga.

---

## Próxima capa

[`layer2.md`](layer2.md) — agrega `CTL-OPT`, `DCL-F`, `DCL-DS` (con bloques `{ }` para subfields).
