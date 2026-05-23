# Gramática de C99

**Fuente:** ISO/IEC 9899:1999 (C99), basado en el draft público N1256 / Annex A.
**Propósito:** referencia para el módulo `bbk-compiler` cuando hace lowering de BBK a C99.

---

## Convenciones de notación

- `<no-terminal>` — categoría sintáctica (regla de producción).
- `literal` — terminal exacto (token léxico que aparece tal cual en el código).
- `a | b` — alternativa: `a` o `b`.
- `a?` — opcional (0 o 1 ocurrencias).
- `{ a }` — repetición (0 o más ocurrencias).
- `<a>_opt` — equivalente a `<a>?`, notación del estándar.
- `one of: x y z` — atajo para `x | y | z` cuando son terminales sueltos.

---

## 1. Gramática léxica (tokens)

Los tokens de C99 son seis categorías. Lo que el preprocesador y el compilador consumen.

### 1.1 Token general

```
<token>
    : <keyword>
    | <identifier>
    | <constant>
    | <string-literal>
    | <punctuator>
```

### 1.2 Keywords

```
<keyword> one of:
    auto       break      case       char       const      continue
    default    do         double     else       enum       extern
    float      for        goto       if         inline     int
    long       register   restrict   return     short      signed
    sizeof     static     struct     switch     typedef    union
    unsigned   void       volatile   while      _Bool      _Complex
    _Imaginary
```

(`inline`, `restrict`, `_Bool`, `_Complex`, `_Imaginary` son C99-only.)

### 1.3 Identificadores

```
<identifier>
    : <identifier-nondigit>
    | <identifier> <identifier-nondigit>
    | <identifier> <digit>

<identifier-nondigit>
    : <nondigit>
    | <universal-character-name>

<nondigit> one of:
    _ a b c d e f g h i j k l m n o p q r s t u v w x y z
      A B C D E F G H I J K L M N O P Q R S T U V W X Y Z

<digit> one of:
    0 1 2 3 4 5 6 7 8 9
```

### 1.4 Universal character names (C99)

```
<universal-character-name>
    : \u <hex-quad>
    | \U <hex-quad> <hex-quad>

<hex-quad>
    : <hex-digit> <hex-digit> <hex-digit> <hex-digit>
```

### 1.5 Constantes

```
<constant>
    : <integer-constant>
    | <floating-constant>
    | <enumeration-constant>
    | <character-constant>
```

#### 1.5.1 Integer constants

```
<integer-constant>
    : <decimal-constant> <integer-suffix>?
    | <octal-constant>   <integer-suffix>?
    | <hexadecimal-constant> <integer-suffix>?

<decimal-constant>
    : <nonzero-digit>
    | <decimal-constant> <digit>

<octal-constant>
    : 0
    | <octal-constant> <octal-digit>

<hexadecimal-constant>
    : <hexadecimal-prefix> <hexadecimal-digit>
    | <hexadecimal-constant> <hexadecimal-digit>

<hexadecimal-prefix> one of:
    0x 0X

<nonzero-digit> one of:
    1 2 3 4 5 6 7 8 9

<octal-digit> one of:
    0 1 2 3 4 5 6 7

<hexadecimal-digit> one of:
    0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F

<integer-suffix>
    : <unsigned-suffix> <long-suffix>?
    | <unsigned-suffix> <long-long-suffix>
    | <long-suffix> <unsigned-suffix>?
    | <long-long-suffix> <unsigned-suffix>?

<unsigned-suffix> one of:    u U
<long-suffix>     one of:    l L
<long-long-suffix> one of:   ll LL
```

#### 1.5.2 Floating constants

```
<floating-constant>
    : <decimal-floating-constant>
    | <hexadecimal-floating-constant>     // C99-only

<decimal-floating-constant>
    : <fractional-constant> <exponent-part>? <floating-suffix>?
    | <digit-sequence> <exponent-part> <floating-suffix>?

<hexadecimal-floating-constant>
    : <hexadecimal-prefix> <hexadecimal-fractional-constant>
          <binary-exponent-part> <floating-suffix>?
    | <hexadecimal-prefix> <hexadecimal-digit-sequence>
          <binary-exponent-part> <floating-suffix>?

<fractional-constant>
    : <digit-sequence>? . <digit-sequence>
    | <digit-sequence> .

<exponent-part>
    : e <sign>? <digit-sequence>
    | E <sign>? <digit-sequence>

<sign> one of:    + -

<digit-sequence>
    : <digit>
    | <digit-sequence> <digit>

<hexadecimal-fractional-constant>
    : <hexadecimal-digit-sequence>? . <hexadecimal-digit-sequence>
    | <hexadecimal-digit-sequence> .

<binary-exponent-part>
    : p <sign>? <digit-sequence>
    | P <sign>? <digit-sequence>

<hexadecimal-digit-sequence>
    : <hexadecimal-digit>
    | <hexadecimal-digit-sequence> <hexadecimal-digit>

<floating-suffix> one of:    f l F L
```

#### 1.5.3 Enumeration constants

```
<enumeration-constant>
    : <identifier>
```

(El identificador debe estar declarado como miembro de un `enum`.)

#### 1.5.4 Character constants

```
<character-constant>
    : ' <c-char-sequence> '
    | L' <c-char-sequence> '

<c-char-sequence>
    : <c-char>
    | <c-char-sequence> <c-char>

<c-char>
    : (cualquier carácter del source set salvo: ' \ y newline)
    | <escape-sequence>

<escape-sequence>
    : <simple-escape-sequence>
    | <octal-escape-sequence>
    | <hexadecimal-escape-sequence>
    | <universal-character-name>

<simple-escape-sequence> one of:
    \' \" \? \\ \a \b \f \n \r \t \v

<octal-escape-sequence>
    : \ <octal-digit>
    | \ <octal-digit> <octal-digit>
    | \ <octal-digit> <octal-digit> <octal-digit>

<hexadecimal-escape-sequence>
    : \x <hexadecimal-digit>
    | <hexadecimal-escape-sequence> <hexadecimal-digit>
```

### 1.6 String literals

```
<string-literal>
    : " <s-char-sequence>? "
    | L" <s-char-sequence>? "

<s-char-sequence>
    : <s-char>
    | <s-char-sequence> <s-char>

<s-char>
    : (cualquier carácter del source set salvo: " \ y newline)
    | <escape-sequence>
```

### 1.7 Punctuators

```
<punctuator> one of:
    [   ]   (   )   {   }   .   ->
    ++  --  &   *   +   -   ~   !
    /   %   <<  >>  <   >   <=  >=  ==  !=  ^   |   &&  ||
    ?   :   ;   ...
    =   *=  /=  %=  +=  -=  <<=  >>=  &=  ^=  |=
    ,   #   ##
    <:  :>  <%  %>  %:  %:%:     // digraphs
```

### 1.8 Comentarios

C99 permite ambos estilos:

```
<comment>
    : /* (cualquier secuencia de caracteres salvo */) */
    | //  (cualquier secuencia hasta newline)        // C99-only
```

---

## 2. Gramática sintáctica (phrase structure)

### 2.1 Expresiones

#### 2.1.1 Primary expressions

```
<primary-expression>
    : <identifier>
    | <constant>
    | <string-literal>
    | ( <expression> )
```

#### 2.1.2 Postfix expressions

```
<postfix-expression>
    : <primary-expression>
    | <postfix-expression> [ <expression> ]
    | <postfix-expression> ( <argument-expression-list>? )
    | <postfix-expression> . <identifier>
    | <postfix-expression> -> <identifier>
    | <postfix-expression> ++
    | <postfix-expression> --
    | ( <type-name> ) { <initializer-list> }            // C99 compound literal
    | ( <type-name> ) { <initializer-list> , }          // C99 compound literal

<argument-expression-list>
    : <assignment-expression>
    | <argument-expression-list> , <assignment-expression>
```

#### 2.1.3 Unary expressions

```
<unary-expression>
    : <postfix-expression>
    | ++ <unary-expression>
    | -- <unary-expression>
    | <unary-operator> <cast-expression>
    | sizeof <unary-expression>
    | sizeof ( <type-name> )

<unary-operator> one of:
    & * + - ~ !
```

#### 2.1.4 Cast expressions

```
<cast-expression>
    : <unary-expression>
    | ( <type-name> ) <cast-expression>
```

#### 2.1.5 Multiplicative expressions

```
<multiplicative-expression>
    : <cast-expression>
    | <multiplicative-expression> * <cast-expression>
    | <multiplicative-expression> / <cast-expression>
    | <multiplicative-expression> % <cast-expression>
```

#### 2.1.6 Additive expressions

```
<additive-expression>
    : <multiplicative-expression>
    | <additive-expression> + <multiplicative-expression>
    | <additive-expression> - <multiplicative-expression>
```

#### 2.1.7 Shift expressions

```
<shift-expression>
    : <additive-expression>
    | <shift-expression> << <additive-expression>
    | <shift-expression> >> <additive-expression>
```

#### 2.1.8 Relational expressions

```
<relational-expression>
    : <shift-expression>
    | <relational-expression> <  <shift-expression>
    | <relational-expression> >  <shift-expression>
    | <relational-expression> <= <shift-expression>
    | <relational-expression> >= <shift-expression>
```

#### 2.1.9 Equality expressions

```
<equality-expression>
    : <relational-expression>
    | <equality-expression> == <relational-expression>
    | <equality-expression> != <relational-expression>
```

#### 2.1.10 Bitwise AND / XOR / OR

```
<AND-expression>
    : <equality-expression>
    | <AND-expression> & <equality-expression>

<exclusive-OR-expression>
    : <AND-expression>
    | <exclusive-OR-expression> ^ <AND-expression>

<inclusive-OR-expression>
    : <exclusive-OR-expression>
    | <inclusive-OR-expression> | <exclusive-OR-expression>
```

#### 2.1.11 Logical AND / OR

```
<logical-AND-expression>
    : <inclusive-OR-expression>
    | <logical-AND-expression> && <inclusive-OR-expression>

<logical-OR-expression>
    : <logical-AND-expression>
    | <logical-OR-expression> || <logical-AND-expression>
```

#### 2.1.12 Conditional expression

```
<conditional-expression>
    : <logical-OR-expression>
    | <logical-OR-expression> ? <expression> : <conditional-expression>
```

#### 2.1.13 Assignment expression

```
<assignment-expression>
    : <conditional-expression>
    | <unary-expression> <assignment-operator> <assignment-expression>

<assignment-operator> one of:
    =  *=  /=  %=  +=  -=  <<=  >>=  &=  ^=  |=
```

#### 2.1.14 Expression (comma operator)

```
<expression>
    : <assignment-expression>
    | <expression> , <assignment-expression>
```

#### 2.1.15 Constant expression

```
<constant-expression>
    : <conditional-expression>
```

---

### 2.2 Declaraciones

```
<declaration>
    : <declaration-specifiers> <init-declarator-list>? ;

<declaration-specifiers>
    : <storage-class-specifier> <declaration-specifiers>?
    | <type-specifier>          <declaration-specifiers>?
    | <type-qualifier>          <declaration-specifiers>?
    | <function-specifier>      <declaration-specifiers>?

<init-declarator-list>
    : <init-declarator>
    | <init-declarator-list> , <init-declarator>

<init-declarator>
    : <declarator>
    | <declarator> = <initializer>
```

#### 2.2.1 Storage class specifiers

```
<storage-class-specifier> one of:
    typedef  extern  static  auto  register
```

#### 2.2.2 Type specifiers

```
<type-specifier>
    : void
    | char
    | short
    | int
    | long
    | float
    | double
    | signed
    | unsigned
    | _Bool                                 // C99
    | _Complex                              // C99
    | _Imaginary                            // C99
    | <struct-or-union-specifier>
    | <enum-specifier>
    | <typedef-name>
```

#### 2.2.3 Struct / union specifiers

```
<struct-or-union-specifier>
    : <struct-or-union> <identifier>? { <struct-declaration-list> }
    | <struct-or-union> <identifier>

<struct-or-union> one of:
    struct  union

<struct-declaration-list>
    : <struct-declaration>
    | <struct-declaration-list> <struct-declaration>

<struct-declaration>
    : <specifier-qualifier-list> <struct-declarator-list> ;

<specifier-qualifier-list>
    : <type-specifier> <specifier-qualifier-list>?
    | <type-qualifier> <specifier-qualifier-list>?

<struct-declarator-list>
    : <struct-declarator>
    | <struct-declarator-list> , <struct-declarator>

<struct-declarator>
    : <declarator>
    | <declarator>? : <constant-expression>
```

#### 2.2.4 Enum specifiers

```
<enum-specifier>
    : enum <identifier>? { <enumerator-list> }
    | enum <identifier>? { <enumerator-list> , }       // C99 trailing comma
    | enum <identifier>

<enumerator-list>
    : <enumerator>
    | <enumerator-list> , <enumerator>

<enumerator>
    : <enumeration-constant>
    | <enumeration-constant> = <constant-expression>
```

#### 2.2.5 Type qualifiers

```
<type-qualifier> one of:
    const  restrict  volatile
```

(`restrict` es C99-only.)

#### 2.2.6 Function specifiers

```
<function-specifier>
    : inline                                // C99
```

#### 2.2.7 Declarators

```
<declarator>
    : <pointer>? <direct-declarator>

<direct-declarator>
    : <identifier>
    | ( <declarator> )
    | <direct-declarator> [ <type-qualifier-list>? <assignment-expression>? ]
    | <direct-declarator> [ static <type-qualifier-list>? <assignment-expression> ]
    | <direct-declarator> [ <type-qualifier-list> static <assignment-expression> ]
    | <direct-declarator> [ <type-qualifier-list>? * ]      // C99 VLA
    | <direct-declarator> ( <parameter-type-list> )
    | <direct-declarator> ( <identifier-list>? )

<pointer>
    : * <type-qualifier-list>?
    | * <type-qualifier-list>? <pointer>

<type-qualifier-list>
    : <type-qualifier>
    | <type-qualifier-list> <type-qualifier>

<parameter-type-list>
    : <parameter-list>
    | <parameter-list> , ...

<parameter-list>
    : <parameter-declaration>
    | <parameter-list> , <parameter-declaration>

<parameter-declaration>
    : <declaration-specifiers> <declarator>
    | <declaration-specifiers> <abstract-declarator>?

<identifier-list>
    : <identifier>
    | <identifier-list> , <identifier>
```

#### 2.2.8 Type names y abstract declarators

```
<type-name>
    : <specifier-qualifier-list> <abstract-declarator>?

<abstract-declarator>
    : <pointer>
    | <pointer>? <direct-abstract-declarator>

<direct-abstract-declarator>
    : ( <abstract-declarator> )
    | <direct-abstract-declarator>? [ <type-qualifier-list>? <assignment-expression>? ]
    | <direct-abstract-declarator>? [ static <type-qualifier-list>? <assignment-expression> ]
    | <direct-abstract-declarator>? [ <type-qualifier-list> static <assignment-expression> ]
    | <direct-abstract-declarator>? [ * ]                   // C99 VLA
    | <direct-abstract-declarator>? ( <parameter-type-list>? )

<typedef-name>
    : <identifier>
```

#### 2.2.9 Initializers

```
<initializer>
    : <assignment-expression>
    | { <initializer-list> }
    | { <initializer-list> , }

<initializer-list>
    : <designation>? <initializer>                          // C99 designated init
    | <initializer-list> , <designation>? <initializer>

<designation>                                               // C99-only
    : <designator-list> =

<designator-list>
    : <designator>
    | <designator-list> <designator>

<designator>
    : [ <constant-expression> ]
    | . <identifier>
```

---

### 2.3 Statements

```
<statement>
    : <labeled-statement>
    | <compound-statement>
    | <expression-statement>
    | <selection-statement>
    | <iteration-statement>
    | <jump-statement>
```

#### 2.3.1 Labeled statements

```
<labeled-statement>
    : <identifier> : <statement>
    | case <constant-expression> : <statement>
    | default : <statement>
```

#### 2.3.2 Compound statement

```
<compound-statement>
    : { <block-item-list>? }

<block-item-list>
    : <block-item>
    | <block-item-list> <block-item>

<block-item>                                                // C99: mezcla libre
    : <declaration>
    | <statement>
```

(En C89 las declaraciones tenían que ir al principio del bloque. En C99 pueden intercalarse con statements.)

#### 2.3.3 Expression statement

```
<expression-statement>
    : <expression>? ;
```

#### 2.3.4 Selection statements

```
<selection-statement>
    : if ( <expression> ) <statement>
    | if ( <expression> ) <statement> else <statement>
    | switch ( <expression> ) <statement>
```

#### 2.3.5 Iteration statements

```
<iteration-statement>
    : while ( <expression> ) <statement>
    | do <statement> while ( <expression> ) ;
    | for ( <expression>? ; <expression>? ; <expression>? ) <statement>
    | for ( <declaration> <expression>? ; <expression>? ) <statement>    // C99
```

(C99 permite declarar la variable en la cláusula init del `for`.)

#### 2.3.6 Jump statements

```
<jump-statement>
    : goto <identifier> ;
    | continue ;
    | break ;
    | return <expression>? ;
```

---

### 2.4 External definitions (top-level)

```
<translation-unit>
    : <external-declaration>
    | <translation-unit> <external-declaration>

<external-declaration>
    : <function-definition>
    | <declaration>

<function-definition>
    : <declaration-specifiers> <declarator> <declaration-list>? <compound-statement>

<declaration-list>
    : <declaration>
    | <declaration-list> <declaration>
```

(`<declaration-list>` antes del cuerpo es solo para el estilo viejo de declaración de parámetros K&R.)

---

## 3. Gramática del preprocesador

Conceptualmente separada del compilador. El preprocesador opera sobre tokens y produce tokens.

```
<preprocessing-file>
    : <group>?

<group>
    : <group-part>
    | <group> <group-part>

<group-part>
    : <if-section>
    | <control-line>
    | <text-line>
    | # <non-directive>

<if-section>
    : <if-group> <elif-groups>? <else-group>? <endif-line>

<if-group>
    : # if      <constant-expression> <new-line> <group>?
    | # ifdef   <identifier> <new-line> <group>?
    | # ifndef  <identifier> <new-line> <group>?

<elif-groups>
    : <elif-group>
    | <elif-groups> <elif-group>

<elif-group>
    : # elif <constant-expression> <new-line> <group>?

<else-group>
    : # else <new-line> <group>?

<endif-line>
    : # endif <new-line>

<control-line>
    : # include <pp-tokens> <new-line>
    | # define  <identifier> <replacement-list> <new-line>
    | # define  <identifier> ( <identifier-list>? ) <replacement-list> <new-line>
    | # define  <identifier> ( ... ) <replacement-list> <new-line>           // C99 variadic
    | # define  <identifier> ( <identifier-list> , ... ) <replacement-list> <new-line>
    | # undef   <identifier> <new-line>
    | # line    <pp-tokens> <new-line>
    | # error   <pp-tokens>? <new-line>
    | # pragma  <pp-tokens>? <new-line>
    | #         <new-line>

<text-line>
    : <pp-tokens>? <new-line>

<non-directive>
    : <pp-tokens> <new-line>

<replacement-list>
    : <pp-tokens>?

<pp-tokens>
    : <preprocessing-token>
    | <pp-tokens> <preprocessing-token>

<preprocessing-token>
    : <header-name>
    | <identifier>
    | <pp-number>
    | <character-constant>
    | <string-literal>
    | <punctuator>
    | (cualquier carácter no-blanco que no sea ninguno de los anteriores)
```

---

## 4. Reglas semánticas — síntesis

Las reglas semánticas no son BNFeables. Resumen de las categorías más relevantes del estándar C99 para el lowering de BBK:

### 4.1 Tipos (§6.2.5)

- **Tipos básicos:** `char`, `signed/unsigned char`, `short`, `int`, `long`, `long long` (C99), `_Bool` (C99), `float`, `double`, `long double`, `_Complex`/`_Imaginary` (C99).
- **Tipos derivados:** punteros, arrays, structs, unions, funciones.
- **Tipos enumerados:** compatibles con un tipo entero subyacente (implementation-defined).
- **Tipos calificados:** `const`, `volatile`, `restrict` (C99). El calificador altera el comportamiento pero no la representación.

### 4.2 Conversiones (§6.3)

- **Promociones enteras:** `char`, `short` y bitfields se promueven a `int` (o `unsigned int`) en contextos aritméticos.
- **Conversiones aritméticas usuales:** dado un operador binario aritmético, ambos operandos se convierten a un tipo común (jerarquía: `long double` > `double` > `float` > integer types con ranking).
- **Punteros:** `NULL` es un puntero constante de valor cero. Cualquier puntero a objeto puede convertirse a `void *` y volver sin pérdida.

### 4.3 Lvalues, rvalues, arrays decay (§6.3.2)

- Un **lvalue** es una expresión con almacenamiento accesible (no necesariamente modificable; ej. `const int x` es lvalue pero no modificable).
- **Array decay:** salvo en contextos específicos (`sizeof`, `&`, inicialización), un array se convierte automáticamente a puntero al primer elemento.
- **Function decay:** un identificador de función se convierte automáticamente a puntero a función.

### 4.4 Sequence points (§6.5)

Puntos donde todos los side effects previos están completados:
- Después del primer operando de `&&`, `||`, `?:`, `,` (operador).
- Al final de cada expresión completa (statement).
- Al llamar a una función (después de evaluar todos los argumentos).
- Al retornar de una función.

Modificar el mismo objeto más de una vez entre dos sequence points es **undefined behavior** (ej. `i = i++`).

### 4.5 Inicialización (§6.7.8)

- Variables `static` y globales sin inicializador explícito → inicializadas a cero.
- Variables `auto` sin inicializador → indeterminadas.
- C99 permite inicializadores designados: `struct Foo f = { .x = 1, .y = 2 };` y `int a[10] = { [3] = 5 };`.
- C99 permite **compound literals**: `(struct Foo){ .x = 1 }` crea un objeto temporal.

### 4.6 Storage durations (§6.2.4)

- **Static:** vive toda la ejecución del programa. Variables globales y `static` locales.
- **Automatic:** vive desde la entrada al bloque hasta su salida. Variables locales `auto` (default) y `register`.
- **Allocated:** vive entre `malloc` y `free`.

### 4.7 Linkage (§6.2.2)

- **External:** visible entre translation units (`extern`, default para funciones y variables globales).
- **Internal:** visible solo en su translation unit (`static` en scope global).
- **No linkage:** variables locales y parámetros.

### 4.8 Function calls (§6.5.2.2)

- Argumentos evaluados en orden no especificado (no hay sequence point entre ellos, salvo lo introducido por `&&`/`||`/`?:`/`,`).
- Tipos de argumentos se convierten al tipo declarado del parámetro (default argument promotions si no hay prototipo).
- C99: la llamada a función con declarador implícito (sin prototipo previo) ya no es legal.

### 4.9 `inline` (C99, §6.7.4)

- Sugiere al compilador inlinear la función.
- Una función `inline` puede coexistir con una definición externa (con `extern inline`).
- Sin garantía de inlining; el estándar solo define la semántica.

### 4.10 Variable-length arrays (C99, §6.7.5.2)

- `int a[n]` donde `n` no es constante en tiempo de compilación.
- Solo permitidos en scope automático (locales).
- El tamaño se evalúa al entrar al bloque.

---

## 5. Diferencias clave C99 vs C89

Resumen de lo que C99 agregó (relevante para el lowering desde BBK):

| Feature | Referencia |
|---|---|
| `//` comentarios | §6.4.9 |
| Tipos `long long`, `_Bool`, `_Complex`, `_Imaginary` | §6.2.5 |
| Calificador `restrict` | §6.7.3 |
| Función `inline` | §6.7.4 |
| Mezcla libre de declaraciones y statements en bloques | §6.8.2 |
| Declaración en cláusula init de `for` | §6.8.5.3 |
| VLAs (variable-length arrays) | §6.7.5.2 |
| Designated initializers | §6.7.8 |
| Compound literals | §6.5.2.5 |
| Variadic macros (`...` en `#define`) | §6.10.3 |
| Hex floating constants (`0x1.fp3`) | §6.4.4.2 |
| `_Pragma` operator | §6.10.9 |
| Headers nuevos: `<stdbool.h>`, `<stdint.h>`, `<inttypes.h>`, `<tgmath.h>`, `<complex.h>`, `<fenv.h>` | §7 |

---

## Referencias

- ISO/IEC 9899:1999 — *Programming languages — C* (norma oficial, paga).
- Draft público N1256 — committee draft de C99 con TC1, TC2, TC3 incorporados. Es la referencia gratuita más cercana al estándar final: https://www.open-std.org/jtc1/sc22/wg14/www/docs/n1256.pdf
- Annex A del estándar contiene la gramática consolidada (lo que se replica aquí).
