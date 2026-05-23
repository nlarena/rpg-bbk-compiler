# Gramática de RPG IV / ILE RPG (RPGLE)

**Lenguaje:** RPG IV, también llamado **ILE RPG** o **RPGLE** (por la extensión de archivo `.RPGLE` en IBM i / AS/400).
**Compilador de referencia:** IBM ILE RPG Compiler (parte del ILE — Integrated Language Environment).
**Plataforma:** IBM i (anteriormente OS/400, antes AS/400).

---

## Nota sobre la naturaleza de esta gramática

A diferencia de C99, **RPG no tiene un estándar ISO**. La especificación autoritativa del lenguaje es la documentación oficial de IBM:

- *ILE RPG Language Reference* (publicación SC09-2508 o sucesores)
- *ILE RPG Programmer's Guide* (SC09-2507 o sucesores)

Esa documentación describe la sintaxis en **prosa + diagramas de ferrocarril** (railroad diagrams), no en BNF. La gramática BNF que sigue es una **reconstrucción razonada** desde esa fuente, suficiente para implementar un parser pero no equivalente a una norma formal. Es lo que se puede obtener: no existe una BNF oficial publicada del lenguaje.

---

## Convenciones de notación

Mismo formato que en [`c99-grammar.md`](c99-grammar.md):

- `<no-terminal>` — categoría sintáctica.
- `literal` — terminal exacto.
- `a | b` — alternativa.
- `a?` — opcional.
- `{ a }` — repetición (0 o más).
- `one of: x y z` — alternativa simple entre terminales.

---

## 1. Estructura general de un programa RPG

Un módulo RPG está compuesto por una secuencia de **especificaciones** (specifications, o "specs"). Cada spec tiene un tipo identificado por una letra. Históricamente todas eran fixed-form (columnas fijas en líneas de 80 caracteres). Desde **RPG IV V5R1** existe free-form para cálculos (`/FREE` ... `/END-FREE`), y desde **7.1 TR7** existe **fully free-form** (con directiva `**FREE` al inicio del archivo) que elimina las columnas fijas para todas las specs.

### 1.1 Tipos de specifications

| Letra | Nombre | Rol |
|---|---|---|
| **H** | Header / Control | Opciones globales del programa (data types defaults, optimization, debug) |
| **F** | File description | Archivos usados (input, output, update, combined) |
| **D** | Definition | Variables, constantes, data structures, prototipos |
| **I** | Input | (Legacy) descripción de registros de entrada |
| **C** | Calculation | Lógica de cálculo (legacy fixed-form o free-form `/FREE`) |
| **O** | Output | (Legacy) descripción de registros de salida |
| **P** | Procedure | Delimita una sub-procedure |

### 1.2 Modos de sintaxis

```
<source-file>
    : <fully-free-source>             // **FREE en línea 1
    | <mixed-form-source>             // mezcla de fixed-form + bloques /FREE en C-specs
    | <fixed-form-source>             // todo fixed-form (legacy puro)
```

#### 1.2.1 Fully free-form (moderno, ≥7.1 TR7)

```
<fully-free-source>
    : **FREE <newline> <free-form-statement-list>

<free-form-statement-list>
    : <free-form-statement>
    | <free-form-statement-list> <free-form-statement>
```

(En este modo no hay columnas fijas; las statements terminan en `;`.)

#### 1.2.2 Mixed-form (legado vivo)

Combinación: H/F/D/P en fixed-form, lógica en bloques `/FREE`.

```
<mixed-form-source>
    : <fixed-spec-section>?
      { /FREE <free-form-statement-list> /END-FREE }*
      <fixed-spec-section>?
```

#### 1.2.3 Fixed-form (legacy puro)

Cada línea tiene 80 columnas, columna 6 indica tipo de spec, y las columnas restantes tienen significado posicional. Detallado en §3.

---

## 2. Léxico (común a todos los modos)

### 2.1 Token general

```
<token>
    : <reserved-word>
    | <opcode>
    | <built-in-function>
    | <identifier>
    | <literal>
    | <punctuator>
    | <operator>
```

### 2.2 Identificadores (names)

```
<identifier>
    : <letter> { <letter> | <digit> | _ | # | $ | @ }*

<letter> one of:
    A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
    a b c d e f g h i j k l m n o p q r s t u v w x y z

<digit> one of:
    0 1 2 3 4 5 6 7 8 9
```

**Notas:**
- RPG es **case-insensitive** para identificadores y palabras reservadas.
- Longitud máxima: 4096 caracteres (en práctica, identificadores legacy son de hasta 6 o 10 caracteres por compatibilidad con fixed-form).
- Los caracteres `#`, `$`, `@` se aceptan por compatibilidad histórica con codificaciones EBCDIC.

### 2.3 Palabras reservadas (declaration keywords, free-form)

```
<reserved-word> one of:
    CTL-OPT       DCL-S         DCL-C         DCL-DS
    DCL-PR        DCL-PI        DCL-F         DCL-PROC
    END-DS        END-PR        END-PI        END-PROC
    BEGSR         ENDSR         EXSR
    IF            ELSE          ELSEIF        ENDIF
    SELECT        WHEN          OTHER         ENDSL
    DOW           ENDDO         DOU           DOWEQ
    FOR           ENDFOR        TO            DOWNTO       BY
    ITER          LEAVE         LEAVESR       RETURN
    MONITOR       ON-ERROR      ON-EXIT       ENDMON
    INZ           BASED         POS           OVERLAY
    LIKE          LIKEDS        LIKEREC       TEMPLATE
    EXPORT        IMPORT        STATIC        AUTO
    OPDESC        OPTIONS       RTNPARM       CONST       VALUE
    GLOBAL        QUALIFIED     ALIGN         EXTPGM      EXTPROC
    PSDS          INFDS         INDARA
    USROPN        DISK          PRINTER       WORKSTN     SEQ
    USAGE         RENAME        PREFIX        EXTNAME     EXTFILE
```

(No exhaustivo; IBM agrega keywords entre releases.)

### 2.4 Opcodes (operation codes)

Operation codes del lenguaje. Los más usados están vivos en free-form; algunos solo existen en fixed-form C-spec.

```
<opcode> one of:
    // Aritméticos (legacy)
    ADD        SUB        MULT       DIV        SQRT       MVR
    Z-ADD      Z-SUB

    // Asignación
    EVAL       EVALR      EVAL-CORR  MOVE       MOVEL      MOVEA
    CLEAR      RESET

    // Control de flujo
    IF         ELSE       ELSEIF     ENDIF
    SELECT     WHEN       OTHER      ENDSL
    FOR        ENDFOR     DOW        DOU        ENDDO
    DO         END        ITER       LEAVE
    GOTO       TAG                                    // legacy
    CASxx                                             // legacy comparison branch
    RETURN

    // Subroutines y procedures
    BEGSR      ENDSR      EXSR       LEAVESR
    CALLP      CALL       CALLB                       // CALL/CALLB legacy
    PARM       PLIST                                  // legacy parameter list

    // Manejo de errores
    MONITOR    ON-ERROR   ENDMON
    ON-EXIT                                           // ≥7.5

    // Archivos
    READ       READE      READP      READPE     CHAIN
    WRITE      UPDATE     DELETE     UNLOCK
    OPEN       CLOSE      FEOD       SETLL      SETGT
    EXFMT      EXCEPT
    POST       NEXT       ACQ        REL

    // Strings (legacy; ahora se usan BIFs)
    CAT        SUBST      SCAN       XLATE      CHECK     CHECKR

    // Indicadores y bits
    BITON      BITOFF     TESTB

    // Date/Time (legacy)
    ADDDUR     SUBDUR     EXTRCT     TIME

    // Misceláneos
    DSPLY      DUMP       SHTDN      DEBUG     IN        OUT
    SORTA      LOOKUP     DEFINE     KFLD     KLIST
    OCCUR
```

(Lista representativa; la referencia de IBM mantiene la enumeración completa.)

### 2.5 Built-in Functions (BIFs)

Todas comienzan con `%`. Sustituyen a opcodes legacy en el código moderno.

```
<built-in-function>
    : % <identifier>

// Las más comunes, agrupadas por categoría:

// Numéricas
%ABS        %DIV        %REM        %INT        %INTH
%UNS        %UNSH       %FLOAT      %DEC        %DECH
%DECPOS     %INTH       %SIGNED     %SQRT

// Strings
%CHAR       %CHECK      %CHECKR     %EDITC      %EDITW
%LEN        %LOWER      %UPPER      %REPLACE    %SCAN
%SCANR      %SPLIT      %STR        %SUBST      %TRIM
%TRIML      %TRIMR      %XLATE      %CONCAT     %CONCATARR

// Date/Time
%DATE       %DAYS       %DIFF       %HOURS      %MINUTES
%MONTHS     %MSECONDS   %SECONDS    %SUBDT      %TIME
%TIMESTAMP  %YEARS

// Arrays / Tables
%ELEM       %LOOKUP     %LOOKUPLT   %LOOKUPGE   %LOOKUPGT
%LOOKUPLE   %TLOOKUP    %XFOOT      %SUBARR     %SORTA

// Archivos
%EOF        %EQUAL      %ERROR      %FOUND      %OPEN
%STATUS     %SIZE       %PARMS      %PARMNUM    %PARMSNUM

// Punteros y direcciones
%ADDR       %ALLOC      %REALLOC    %TYPEOF
%ELEM       %SIZE

// Conversión / formato
%BITAND     %BITOR      %BITXOR     %BITNOT
%CHAR       %DEC        %INT        %FLOAT      %BIN

// Otros
%NULL       %NULLIND    %HANDLER    %PROC       %THIS
%KDS        %TIMESTAMP
```

(IBM agrega BIFs en cada release; lista no exhaustiva.)

### 2.6 Literales

```
<literal>
    : <character-literal>
    | <numeric-literal>
    | <hex-literal>
    | <date-literal>
    | <time-literal>
    | <timestamp-literal>
    | <ucs2-literal>
    | <graphic-literal>
    | <indicator-literal>

<character-literal>
    : ' { <char-or-escape> }* '

<char-or-escape>
    : (cualquier carácter excepto ' sin escapar)
    | ''                              // comilla escapada

<numeric-literal>
    : <digit-sequence> ( . <digit-sequence> )?      // decimal, packed o zoned según contexto
    | -<digit-sequence> ( . <digit-sequence> )?
    | +<digit-sequence> ( . <digit-sequence> )?

<digit-sequence>
    : <digit>
    | <digit-sequence> <digit>

<hex-literal>
    : X' <hex-digit-pairs> '

<hex-digit-pairs>
    : <hex-digit> <hex-digit>
    | <hex-digit-pairs> <hex-digit> <hex-digit>

<hex-digit> one of:
    0 1 2 3 4 5 6 7 8 9 A B C D E F a b c d e f

<date-literal>
    : D' YYYY-MM-DD '                 // formato ISO; otros formatos según DATFMT

<time-literal>
    : T' HH.MM.SS '                   // formato ISO; otros según TIMFMT

<timestamp-literal>
    : Z' YYYY-MM-DD-HH.MM.SS.mmmmmm ' // ISO con microsegundos

<ucs2-literal>
    : U' <hex-digit-pairs> '

<graphic-literal>
    : G' <DBCS-bytes> '

<indicator-literal>                   // *ON / *OFF / *BLANKS / etc.
    : *ON | *OFF | *BLANK | *BLANKS | *ZERO | *ZEROS | *HIVAL | *LOVAL
    | *NULL | *OMIT | *START | *END | *ALL ' <char-sequence> '
```

**Figurative constants** (literales especiales): `*ON`, `*OFF`, `*BLANK(S)`, `*ZERO(S)`, `*HIVAL`, `*LOVAL`, `*NULL`, `*OMIT`, `*ALL'x'`, `*START`, `*END`, `*LOOPCOUNT`.

### 2.7 Operadores

```
<operator> one of:
    +    -    *    /    **                           // aritméticos
    =    <>   <    >    <=   >=                       // relacionales
    AND  OR   NOT                                     // lógicos (palabras)
    AND  OR   NOT  XOR                                // bit-level: vía BIFs %BITAND etc.
```

### 2.8 Punctuators

```
<punctuator> one of:
    ;    :    ,    (    )    *
```

(`;` termina statements en free-form. `:` separa parámetros en muchas BIFs y opcodes. `*` introduce figurative constants.)

### 2.9 Comentarios

```
<comment>
    : // (hasta fin de línea)                         // free-form
    | * (fixed-form: columna 7 = *, resto de la línea es comentario)
    | /* ... */                                       // no estándar; algunas variantes lo admiten
```

---

## 3. Fixed-form: layout posicional

En fixed-form **cada columna importa**. Líneas de 80 caracteres. Columna 6 (1-indexada, es el carácter en posición 6) define el tipo de spec.

### 3.1 H-spec (Control/Header) — columna 6 = `H`

Define opciones globales. Sintaxis libre después de la columna 7 con keywords como:

```
<H-spec>
    : H { <H-keyword> }*

<H-keyword> one of:
    DATFMT(<format>)        TIMFMT(<format>)
    DECEDIT('<char>')       ALTSEQ(<table>)
    DEBUG                   DFTACTGRP(*NO|*YES)
    ACTGRP('<name>'|*NEW|*CALLER)
    BNDDIR('<name>')        OPTION(*SRCSTMT:*NODEBUGIO)
    THREAD(*SERIALIZE|*CONCURRENT)
    FIXNBR(*ZONED:*INPUTPACKED)
    EXTBINX                 NOMAIN
```

### 3.2 F-spec (File description) — columna 6 = `F`

```
Columnas:
  6       = F
  7-16    = Nombre del archivo
  17      = Tipo: I (Input) | O (Output) | U (Update) | C (Combined) | D (Display)
  18      = Designación: P (primary) | S (secondary) | F (full procedural) | R (record)
  19      = End-of-file: E (end designado)
  20      = Sequence: A (ascending) | D (descending)
  21      = Acceso: F (sequential) | K (keyed)
  22-27   = Longitud del registro
  28-32   = Longitud de la clave
  33      = Tipo de la clave: A | P | B | I (alfanumérica/packed/binario/integer)
  34      = Acceso a I/O: D (disk) | T (table)
  35-41   = Reservado
  42      = Device: DISK | PRINTER | WORKSTN | SEQ | SPECIAL
  44-80   = Keywords adicionales: PREFIX, RENAME, EXTFILE, USROPN, etc.
```

### 3.3 D-spec (Definition) — columna 6 = `D`

```
Columnas:
  6       = D
  7-21    = Nombre (15 chars)
  22      = External description (E) o no
  23      = Tipo declaración: S (standalone) | DS (data structure) | C (constant) | PR (prototype) | PI (procedure interface)
  24-25   = Reservado
  26-32   = From position (posición inicial en estructuras)
  33-39   = To position / longitud
  40      = Data type: A | B | C | D | F | G | I | N | O | P | S | T | U | Z | * (puntero) | <objeto>
  41-42   = Decimales
  44-80   = Keywords: INZ, BASED, OVERLAY, LIKE, DIM, etc.
```

#### Data types codes

```
A    Alphanumeric (character)
B    Binary numeric (legacy)
C    UCS-2 character
D    Date
F    Float
G    Graphic
I    Integer (signed)
N    Indicator
O    Object (Java)
P    Packed decimal
S    Zoned decimal
T    Time
U    Unsigned integer
Z    Timestamp
*    Pointer (basing pointer / procedure pointer)
```

### 3.4 C-spec (Calculation) — columna 6 = `C`

Históricamente el corazón de RPG. Estructura:

```
Columnas:
  6       = C
  7-8     = Indicador condicional (N01: si NO indicador 01)
  9-11    = Indicador condicional 2
  12-25   = Factor 1 (operando izquierdo)
  26-35   = Operation code (opcode)
  36-49   = Factor 2 (operando derecho)
  50-63   = Result field
  64-68   = Length
  69-70   = Decimal positions
  71-72   = Indicator hi (resultado > Factor 2)
  73-74   = Indicator lo (resultado < Factor 2)
  75-76   = Indicator eq (resultado = Factor 2)
```

Ejemplo:
```
     C                   EVAL      X = Y + Z
     C                   IF        A > B
     C                   READ      MYFILE
     C                   EXCEPT    MYEXC
```

#### 3.4.1 Free-form en C-spec (`/FREE` ... `/END-FREE`)

Permite escribir el contenido del C-spec con statements:

```
     C/FREE
     X = Y + Z;
     IF A > B;
       READ MYFILE;
     ENDIF;
     C/END-FREE
```

(Histórico: introducido en V5R1. Reemplazado por fully-free-form en 7.1 TR7.)

### 3.5 P-spec (Procedure) — columna 6 = `P`

Delimita una sub-procedure dentro del módulo:

```
Columnas:
  6       = P
  7-21    = Nombre del procedure
  24      = B (begin) | E (end)
  44-80   = Keywords: EXPORT, IMPORT
```

Ejemplo:
```
     P MyProc          B
     D MyProc          PI
     D   parm1                       10A
     C                   ...
     P MyProc          E
```

### 3.6 I-spec y O-spec (legacy)

Específicos para programas tipo "RPG cycle" con archivos descriptos en el programa. En código moderno casi no se usan — los archivos se describen externamente y se accede vía F-spec con keyword `EXTNAME`. No incluyo la gramática completa aquí; referencia IBM.

### 3.7 Indicators (mecanismo de control de flujo legacy)

Los **indicators** son banderas booleanas globales. Existen 99 indicators numéricos (`*IN01` ... `*IN99`), más indicators especiales (`*INLR` = Last Record, `*INRT`, `*INH1`-`*INH9`, etc.).

Las opcodes legacy ponen indicators en sus columnas 71-76 según el resultado de la operación. Ejemplo:

```
     C     KEY1          CHAIN     MYFILE                            50
```

Si CHAIN no encuentra el registro, indicator `*IN50` se prende. Después se prueba:

```
     C                   IF        *IN50 = *OFF
       (registro encontrado)
     C                   ENDIF
```

En código moderno se reemplaza por BIFs `%FOUND`, `%EOF`, `%ERROR`, etc.

---

## 4. Free-form: sintaxis moderna

Aplica tanto al modo `**FREE` (fully free-form) como dentro de bloques `/FREE`. Las statements terminan en `;` y no hay columnas significativas.

### 4.1 Control specification (free-form)

```
<ctl-opt-statement>
    : CTL-OPT { <ctl-opt-keyword> }* ;
```

Equivalente a H-spec. Mismos keywords.

### 4.2 Declaraciones

```
<declaration>
    : <dcl-s>
    | <dcl-c>
    | <dcl-ds>
    | <dcl-pr>
    | <dcl-pi>
    | <dcl-f>
    | <dcl-proc>
    | <dcl-subf>
    | <dcl-parm>
```

#### 4.2.1 Standalone variable

```
<dcl-s>
    : DCL-S <identifier> <type-spec> { <var-keyword> }* ;

<type-spec>
    : <type-name>                                    // ej. INT, CHAR, PACKED, ZONED
    | <type-name> ( <length-spec> )                  // CHAR(10), PACKED(7:2)
    | LIKE ( <identifier> )                          // misma forma que otra variable
    | LIKEDS ( <identifier> )                        // misma forma que un DS
    | LIKEREC ( <record-format> { : <part> } )

<type-name> one of:
    CHAR    VARCHAR  UCS2    VARUCS2  GRAPH   VARGRAPH
    PACKED  ZONED    BINDEC  INT      UNS     FLOAT
    DATE    TIME     TIMESTAMP
    IND     POINTER  OBJECT

<var-keyword> one of:
    INZ ( <expr> )      INZ ( *LIKEDS )
    BASED ( <pointer> )
    EXPORT              IMPORT             STATIC          TEMPLATE
    DIM ( <const> )     OVERLAY ( <var> { : <pos> } )
    POS ( <const> )     CCSID ( <id> )
    CONST                                           // solo en parámetros
    VALUE
    OPTIONS ( <opt> { : <opt> }* )
```

#### 4.2.2 Constants

```
<dcl-c>
    : DCL-C <identifier> <const-value> ;
    | DCL-C <identifier> CONST ( <const-value> ) ;

<const-value>
    : <literal>
    | <figurative-constant>
    | ( <expression> )                              // expresión constante
```

#### 4.2.3 Data structure

```
<dcl-ds>
    : DCL-DS <identifier> { <ds-keyword> }* ;
        { <ds-subfield> }*
      END-DS ;
    | DCL-DS <identifier> { <ds-keyword> }* END-DS ;     // sin subfields

<ds-keyword> one of:
    QUALIFIED    TEMPLATE    EXTNAME ( <file> )
    LIKEDS ( <ds> )          LIKEREC ( <rec> )
    INZ          BASED ( <ptr> )
    DIM ( <const> )          OCCURS ( <const> )           // legacy
    ALIGN
    PSDS                                                    // program status DS
    INFDS ( <file> )                                        // file info DS

<ds-subfield>
    : <identifier> <type-spec> { <var-keyword> }* ;
```

#### 4.2.4 Prototype y Procedure Interface

```
<dcl-pr>
    : DCL-PR <identifier> <return-type-spec>? { <pr-keyword> }* ;
        { <dcl-parm> }*
      END-PR ;
    | DCL-PR <identifier> <return-type-spec>? { <pr-keyword> }* END-PR ;

<dcl-pi>
    : DCL-PI <identifier> <return-type-spec>? { <pi-keyword> }* ;
        { <dcl-parm> }*
      END-PI ;
    | DCL-PI <identifier> <return-type-spec>? { <pi-keyword> }* END-PI ;

<return-type-spec>
    : <type-spec>

<pr-keyword>
    : EXTPGM ( '<program-name>' )                   // call to *PGM object
    | EXTPROC ( '<procedure-name>' )                // explicit external name
    | OPDESC                                         // pass operational descriptors

<pi-keyword>
    : (mismos que dcl-s; ej. STATIC, EXPORT, etc.)

<dcl-parm>
    : DCL-PARM <identifier> <type-spec> { <parm-keyword> }* ;

<parm-keyword> one of:
    CONST     VALUE     OPDESC     OPTIONS ( *NOPASS | *OMIT | *VARSIZE | *STRING )
    RTNPARM
```

#### 4.2.5 File declaration

```
<dcl-f>
    : DCL-F <file-name> { <f-keyword> }* ;

<f-keyword> one of:
    USAGE ( *INPUT | *OUTPUT | *UPDATE | *DELETE )
    KEYED     RECNO ( <var> )       PREFIX ( <pfx> { : <n> } )
    RENAME ( <old> : <new> )        IGNORE ( <fmt> )       INCLUDE ( <fmt> )
    EXTFILE ( '<file>' )            EXTMBR ( '<mbr>' )     EXTDESC ( '<file>' )
    DEVID ( <var> )                 USROPN
    DISK      PRINTER     WORKSTN     SEQ     SPECIAL
    BLOCK ( *YES | *NO )
    INDDS ( <ds-name> )             INFDS ( <ds-name> )    INFSR ( <sr> )
```

#### 4.2.6 Procedure declaration

```
<dcl-proc>
    : DCL-PROC <identifier> { <proc-keyword> }* ;
        <dcl-pi>?
        { <declaration> | <statement> }*
      END-PROC ;
    | DCL-PROC <identifier> { <proc-keyword> }* ;
        <dcl-pi>?
        { <declaration> | <statement> }*
      END-PROC <identifier> ;

<proc-keyword> one of:
    EXPORT
    EXTPROC ( '<name>' )
```

### 4.3 Statements

```
<statement>
    : <assignment-statement>
    | <if-statement>
    | <select-statement>
    | <dow-statement>
    | <dou-statement>
    | <for-statement>
    | <monitor-statement>
    | <subroutine-statement>
    | <return-statement>
    | <leave-statement>
    | <iter-statement>
    | <leavesr-statement>
    | <call-statement>
    | <file-op-statement>
    | <opcode-statement>
    | <bif-statement>
    | <expression-statement>
```

#### 4.3.1 Assignment (EVAL)

```
<assignment-statement>
    : <lvalue> = <expression> ;
    | EVAL <lvalue> = <expression> ;
    | EVAL ( H )   <lvalue> = <expression> ;        // half-adjust (round)
    | EVAL ( R )   <lvalue> = <expression> ;        // truncate
    | EVAL-CORR <ds-lvalue> = <ds-rvalue> ;          // matching subfields

<lvalue>
    : <identifier>
    | <identifier> ( <subscript-list> )              // array element
    | <identifier> . <identifier>                    // qualified DS subfield
    | %SUBST ( <var> : <start> : <length>? )
```

#### 4.3.2 IF / ELSE

```
<if-statement>
    : IF <expression> ;
        { <statement> }*
      { ELSEIF <expression> ;
        { <statement> }* }*
      { ELSE ;
        { <statement> }* }?
      ENDIF ;
```

#### 4.3.3 SELECT / WHEN / OTHER

```
<select-statement>
    : SELECT ;
        { WHEN <expression> ;
          { <statement> }* }*
        { OTHER ;
          { <statement> }* }?
      ENDSL ;
```

#### 4.3.4 DOW (do while)

```
<dow-statement>
    : DOW <expression> ;
        { <statement> }*
      ENDDO ;
```

#### 4.3.5 DOU (do until)

```
<dou-statement>
    : DOU <expression> ;
        { <statement> }*
      ENDDO ;
```

(DOU evalúa la condición al final del bloque; DOW al principio.)

#### 4.3.6 FOR

```
<for-statement>
    : FOR <identifier> = <expression> ( TO | DOWNTO ) <expression> ( BY <expression> )? ;
        { <statement> }*
      ENDFOR ;
```

Ejemplo:
```rpg
FOR i = 1 TO 10;
  count = count + i;
ENDFOR;
```

#### 4.3.7 MONITOR (manejo de errores)

```
<monitor-statement>
    : MONITOR ;
        { <statement> }*
      { ON-ERROR <status-list>? ;
        { <statement> }* }*
      { ON-EXIT ;                                    // ≥7.5
        { <statement> }* }?
      ENDMON ;

<status-list>
    : <expression>
    | <status-list> : <expression>
```

#### 4.3.8 Subroutine

```
<subroutine-statement>
    : BEGSR <identifier> ;
        { <statement> }*
      ENDSR ( <identifier> )? ;

<exsr-statement>
    : EXSR <identifier> ;
```

#### 4.3.9 Return / Leave / Iter / Leavesr

```
<return-statement>
    : RETURN <expression>? ;

<leave-statement>     : LEAVE ;          // sale del DO/FOR
<iter-statement>      : ITER ;           // siguiente iteración
<leavesr-statement>   : LEAVESR ;        // sale de la subroutine
```

#### 4.3.10 Procedure call

```
<call-statement>
    : CALLP <procedure-name> ( <arg-list>? ) ;
    | <procedure-name> ( <arg-list>? ) ;             // forma corta (free-form)

<arg-list>
    : <expression>
    | <arg-list> : <expression>
```

(`CALLP` opcional cuando la llamada se usa como statement; obligatorio si hay ambigüedad.)

#### 4.3.11 File operations

```
<file-op-statement>
    : READ   <file>  <ds>? ;
    | READE  <key>   <file>  <ds>? ;
    | READP  <file>  <ds>? ;
    | READPE <key>   <file>  <ds>? ;
    | CHAIN  <key>   <file>  <ds>? ;
    | SETLL  <key>   <file> ;
    | SETGT  <key>   <file> ;
    | WRITE  <format-or-file>  <ds>? ;
    | UPDATE <format-or-file>  <ds>? ;
    | DELETE <key>?  <file-or-format> ;
    | UNLOCK <file> ;
    | OPEN   <file> ;
    | CLOSE  <file> ;
    | EXFMT  <format>  <ds>? ;
```

(Las claves multipart se construyen con `%KDS(<ds>)` o con literal de array de claves.)

### 4.4 Expresiones

```
<expression>
    : <or-expression>

<or-expression>
    : <and-expression>
    | <or-expression> OR <and-expression>

<and-expression>
    : <not-expression>
    | <and-expression> AND <not-expression>

<not-expression>
    : <comparison-expression>
    | NOT <not-expression>

<comparison-expression>
    : <additive-expression>
    | <additive-expression> <comparison-op> <additive-expression>

<comparison-op> one of:
    =   <>   <   >   <=   >=

<additive-expression>
    : <multiplicative-expression>
    | <additive-expression> + <multiplicative-expression>
    | <additive-expression> - <multiplicative-expression>

<multiplicative-expression>
    : <power-expression>
    | <multiplicative-expression> * <power-expression>
    | <multiplicative-expression> / <power-expression>

<power-expression>
    : <unary-expression>
    | <unary-expression> ** <power-expression>           // right-associative

<unary-expression>
    : <postfix-expression>
    | + <unary-expression>
    | - <unary-expression>

<postfix-expression>
    : <primary>
    | <postfix-expression> ( <argument-list>? )          // BIF or proc call
    | <postfix-expression> ( <subscript-list> )          // array indexing
    | <postfix-expression> . <identifier>                // qualified DS access

<primary>
    : <identifier>
    | <literal>
    | <figurative-constant>
    | <indicator-ref>                                    // *IN01, *INLR, etc.
    | <built-in-function-call>
    | ( <expression> )

<built-in-function-call>
    : % <identifier> ( <argument-list>? )

<argument-list>
    : <expression>
    | <argument-list> : <expression>

<subscript-list>
    : <expression>
    | <subscript-list> : <expression>                    // multi-dim
```

#### Precedencia (de mayor a menor)

```
1. ( )  llamadas BIF/proc  indexing
2. ** (right-assoc)
3. unary + / -
4. * /
5. + -
6. = <> < > <= >=
7. NOT
8. AND
9. OR
```

---

## 5. Reglas semánticas

Igual que con C99, no son BNFeables. Resumen:

### 5.1 Tipos y conversión

- **Numéricos:** `PACKED`, `ZONED`, `BINDEC`, `INT`, `UNS`, `FLOAT`. La conversión entre numéricos preserva valor pero puede truncar decimales (modificable con `EVAL(H)` para redondear).
- **Strings:** `CHAR` (fijo), `VARCHAR` (variable), `UCS2`, `GRAPH`. Concatenación con `+` o `%CONCAT`.
- **Date/time:** `DATE`, `TIME`, `TIMESTAMP`. Aritmética con `%DIFF`, `%DATE`, `%TIME`, `+ <duration>`.
- **Indicators:** `IND` (booleano 1-char `'1'`/`'0'`). Convertibles con figurative constants `*ON`/`*OFF`.
- **Punteros:** `POINTER` (basing pointer), `*` para puntero a procedure. Aritmética limitada.

### 5.2 Scope y visibilidad

- **Module-level (global):** declaraciones fuera de cualquier `DCL-PROC`. Visibles en todo el módulo.
- **Procedure-level (local):** declaraciones dentro de un `DCL-PROC`. Solo visibles dentro de ese procedure.
- **Export/Import:** los símbolos marcados `EXPORT` en un módulo pueden ser referenciados con `IMPORT` desde otro módulo del mismo programa (después del binding).
- **Static / Automatic storage:** local variables son **automatic** por default. Con `STATIC` mantienen valor entre llamadas.

### 5.3 Indicators (mecanismo legacy)

- 99 indicators numéricos `*IN01`-`*IN99`, todos globales.
- Indicators especiales: `*INLR` (Last Record, marca fin de programa), `*INRT`, `*INH1`-`*INH9`, `*INU1`-`*INU8`.
- En código moderno: reemplazar por BIFs (`%FOUND`, `%EOF`, `%ERROR`, `%EQUAL`, `%STATUS`) o por una indicator data structure (`INDDS`) con nombres descriptivos.

### 5.4 RPG Cycle (ciclo principal)

Programa RPG legacy con archivo "primary" (P en F-spec): el runtime ejecuta un ciclo implícito:

1. Lee siguiente registro del primary.
2. Procesa breaks de control.
3. Ejecuta los cálculos detail (C-specs sin condición de break).
4. Procesa output.
5. Si `*INLR = *ON` → termina, sino vuelve al paso 1.

**Importancia para el lowering:** programas con cycle requieren generar un loop principal en C, no un main lineal. Programas `NOMAIN` o procedures puros no tienen cycle.

### 5.5 Sub-procedures vs. main procedure

- **Main procedure:** punto de entrada del programa (`*PGM`). Es el código sin envolver en `DCL-PROC`. En modo `NOMAIN` (en H-spec) no hay main.
- **Sub-procedure:** funciones/procedures internas, declaradas con `DCL-PROC ... END-PROC`. Pueden retornar valor o no, recibir parámetros con `CONST`/`VALUE`/by-reference.
- **By-reference vs by-value:** default es by-reference. `VALUE` lo pasa por valor; `CONST` permite pasar literales y expresiones constantes pero el callee no puede modificar.

### 5.6 Activation groups y binding

- RPG ILE corre dentro de **activation groups** — contexto de ejecución que aísla recursos (archivos abiertos, override de jobs, storage estático).
- El binding une módulos compilados (`*MODULE`) en un programa (`*PGM`) o service program (`*SRVPGM`).
- Relevante para la **emulación en Windows/Linux**: hay que decidir cómo modelar activation groups y service programs en el runtime.

### 5.7 Result Data Structure (RDS) para file ops

Operaciones de archivo aceptan una "result data structure" opcional como destino del registro leído:

```rpg
DCL-DS empRec EXTNAME('EMPLOYEE') QUALIFIED;
END-DS;

CHAIN keyValue EMPLOYEE empRec;
```

El registro se copia en `empRec` en vez de en variables globales con nombres heredados.

### 5.8 PSDS e INFDS

- **PSDS (Program Status Data Structure):** estructura especial declarada con `PSDS`. Recibe información del estado del programa (status code, librería actual, job name, etc.) en posiciones fijas.
- **INFDS (File Information Data Structure):** estructura asociada a un archivo con `INFDS(<ds>)`. Recibe información del último I/O del archivo (status, RRN, formato, etc.).

### 5.9 Initialization

- Variables `STATIC` y globales sin `INZ` → inicializadas a default por tipo (numéricos a 0, alfanuméricos a blanks, fechas a 0001-01-01).
- Variables `AUTO` (default local) sin `INZ` → también inicializadas a default (esto difiere de C; en RPG nunca son indeterminadas).

### 5.10 Half-adjust y precisión decimal

- La aritmética packed/zoned es **decimal exacta** (no floating point), con precisión configurable.
- `EVAL(H)` indica "half-adjust" — redondear al decimal más cercano. Default es truncar.
- `EVAL(R)` indica truncar explícitamente.
- Reglas de promoción definidas por el lenguaje: el resultado se computa con suficiente precisión y se ajusta al destino.

---

## 6. Built-in Functions — referencia rápida

| BIF | Descripción |
|---|---|
| `%ABS(n)` | Valor absoluto |
| `%ADDR(v)` | Dirección de memoria de v |
| `%ALLOC(n)` | Aloca n bytes y retorna puntero |
| `%CHAR(v {:fmt})` | Convierte a string |
| `%CHECK(set:str {:start})` | Posición del primer char de str que no está en set |
| `%CHECKR(set:str {:start})` | Como CHECK pero desde la derecha |
| `%DATE(v {:fmt})` | Convierte a DATE |
| `%DAYS(n)` | Crea una duración de n días |
| `%DEC(v {:prec:dec})` | Convierte a packed decimal |
| `%DECH(...)` | Como %DEC con half-adjust |
| `%DIFF(d1:d2:fmt)` | Diferencia entre dos fechas/timestamps |
| `%DIV(a:b)` | División entera |
| `%EDITC(n:'code')` | Formatea numérico con edit code |
| `%EDITW(n:'mask')` | Formatea numérico con edit word |
| `%ELEM(arr)` | Cantidad de elementos del array |
| `%EOF({file})` | Indica si último I/O dio fin-de-archivo |
| `%EQUAL({file})` | Indica si SETLL/SETGT encontró match exacto |
| `%ERROR()` | Indica si último opcode con (E) dio error |
| `%FLOAT(v)` | Convierte a float |
| `%FOUND({file})` | Indica si CHAIN/SETLL/SETGT encontró registro |
| `%INT(v)`, `%INTH(v)` | Convierte a integer (con/sin half-adjust) |
| `%LEN(v)` | Longitud actual de un VARCHAR o longitud declarada |
| `%LOOKUP(arg:arr {:start:nbr})` | Búsqueda en array |
| `%LOWER(s)`, `%UPPER(s)` | Conversión a minúsculas/mayúsculas |
| `%NULL`, `%NULLIND(f)` | Manipulación de nulls (DB) |
| `%OPEN(file)` | Indica si el archivo está abierto |
| `%PARMS()`, `%PARMNUM(n)` | Cantidad de parámetros pasados / número del parámetro |
| `%REM(a:b)` | Resto de división |
| `%REPLACE(src:tgt {:start:len})` | Reemplaza substring |
| `%SCAN(needle:haystack {:start})` | Posición de needle en haystack |
| `%SIZE(v {:*ALL})` | Tamaño en bytes |
| `%STATUS({file})` | Status code del último I/O |
| `%STR(ptr {:len})` | String C terminado en null |
| `%SUBST(s:start:len)` | Substring |
| `%TIME(v {:fmt})` | Convierte a TIME |
| `%TIMESTAMP(v {:fmt})` | Convierte a TIMESTAMP |
| `%TRIM(s)`, `%TRIML(s)`, `%TRIMR(s)` | Trim |
| `%XFOOT(arr)` | Suma de todos los elementos del array |
| `%XLATE(from:to:str)` | Traduce caracteres |

(Lista representativa. IBM publica la referencia completa en cada release.)

---

## 7. Diferencias entre dialectos de RPG

Si el `rpg-frontend` apunta a parsear código real, conviene tener en cuenta los dialectos:

| Dialecto | Características |
|---|---|
| **RPG II** | El original, '70s. Muy rara vez vivo. Fixed-form puro. |
| **RPG III** | OS/400 hasta los 90. Fixed-form. |
| **RPG IV (V3R1+)** | Moderno. Introduce D-spec, free-form C-spec, prototypes. |
| **RPG IV con `/FREE`** | V5R1+. Free-form dentro de C-specs. |
| **RPG IV fully-free** | 7.1 TR7+. `**FREE` al inicio del archivo. |
| **SQLRPGLE** | Embedded SQL con `EXEC SQL ... ;`. Pre-procesado antes de la compilación RPG. |

**Decisión de scope para `rpg-frontend`:**
- **Mínimo viable:** parsear fully-free-form (`**FREE`) — moderno, más limpio, sintaxis más cercana a BNF tradicional.
- **Realista:** soporte para `/FREE` dentro de fixed-form (most legacy code).
- **Ambicioso:** soporte completo de fixed-form puro (programas legacy de los '90 sin modernizar).

---

## 8. Referencias

- IBM, *ILE RPG Language Reference* (SC09-2508). Documentación oficial del lenguaje. Disponible en https://www.ibm.com/docs/en/i/<version>?topic=programming-rpg
- IBM, *ILE RPG Programmer's Guide* (SC09-2507). Guía de programación con ejemplos.
- IBM Redbooks sobre RPG IV modernization.
- Comunidades activas: `midrange.com`, `rpgpgm.com`, `iSeriesGuru`.

**Lo que NO existe:** un estándar formal tipo ISO con la gramática consolidada en BNF. Esta sección es lo más cercano a eso, reconstruido desde la documentación de IBM.
