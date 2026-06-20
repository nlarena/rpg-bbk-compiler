# BBK Runtime — Jobs: concepto y modelo

> Documento de **concepto + intención de diseño** del runtime (`bbk-runtime`, servicio
> Spring Boot / REST). Captura qué es un *job* de IBM i, qué datos trae, y qué subconjunto
> vamos a modelar nosotros. No es un manual de IBM i ni la spec de la API — es el "por qué"
> detrás del modelo de job.

## 1. Qué es un job

En IBM i **todo lo que se ejecuta corre dentro de un job**. No existe ejecutar código "suelto":
un programa RPG, una consulta, un comando, una sesión de usuario — cada uno vive en un job. El
job no es solo "un programa corriendo": es **todo el contexto de ejecución** alrededor de ese
trabajo (quién sos, qué bibliotecas ves, qué archivos tenés abiertos, a dónde va tu salida).

Frase guía: **un programa RPG no *tiene* contexto; lo toma prestado del job en el que corre.**
Cuando resuelve un nombre de archivo, abre un cursor o llama a otro programa, lo hace
implícitamente *en el contexto del job*. Por eso el job es la columna vertebral del runtime.

## 2. Un job no es un hilo — es (casi) un proceso

La confusión natural es pensar el job como un hilo de SO. No lo es:

| | Unidad de… | Relación con el contexto |
|---|---|---|
| **Hilo** | ejecución (un flujo de control, stack + PC, despachable) | **usa** el contexto; lo comparte con sus hermanos |
| **Proceso** | propiedad y aislamiento (recursos, identidad, ciclo de vida) | **provee/posee** el contexto |

El job cae del lado del **proceso**: posee la library list, QTEMP, archivos abiertos y locks;
tiene identidad (el nombre calificado ≈ PID); está aislado (tu QTEMP no la ve otro job); tiene
ciclo de vida. Nada de eso es propio de un hilo.

Lo *thread-like* está **adentro** del job: el **flujo de control** (el call stack
`PGMA → PGMB → calc`). Un job clásico tenía un solo hilo de ejecución; los modernos pueden tener
varios, todos compartiendo el contexto del job. La analogía completa:

```
   proceso : hilo   ::   job : flujo-de-control
   (dueño)   (corre)      (dueño)   (corre)
```

## 3. Las tres facetas del "contexto"

"El job otorga un contexto" es correcto, siempre que entiendas que es un contexto **vivo, con
estado y dueño de recursos** — no una configuración pasiva. Mezcla tres cosas que conviene
distinguir:

1. **Contexto de resolución (ambiental).** La parte "configuración": library list, usuario,
   formatos. Responde *"¿en qué entorno interpreto nombres y datos?"*. Solo esto sería pasivo.
2. **Estado mutable que persiste (sesión).** El job **acumula** estado mientras corre: abre
   archivos (cursores con posición y locks), empuja el call stack, escribe en QTEMP, modifica su
   propia library list. Por eso es una *sesión*, no un *snapshot*.
3. **Identidad, aislamiento y ciclo de vida (la "cosa").** Límite de aislamiento, unidad de
   identidad/autoría y unidad de ciclo de vida (nace, está activo, termina).

Si fuera "solo contexto" (pasivo), lo pasaríamos como parámetro de solo lectura. Como es **estado
mutable + recursos propios + ciclo de vida**, tiene que ser una **entidad viva que el runtime
sostiene** entre llamadas: se *abre*, se opera contra su `id`, se *cierra*.

## 4. De dónde vienen los datos: el job se *estampa*

Un job casi no se configura a mano — se estampa de tres fuentes:

- **Job description (`*JOBD`)** — objeto-plantilla con la mayoría de los defaults (library list
  inicial, output queue, job queue, prioridad). Hay una por defecto (`QDFTJOBD`).
- **Perfil de usuario** — lo ligado a identidad (biblioteca actual, programa inicial).
- **System values** — defaults de sistema (`QSYSLIBL`, `QDATFMT`, `QCCSID`…).

Implicación de diseño: **un job se crea con *defaults*, no con un formulario gigante.**

## 5. Datos por defecto (atributos heredados)

| Categoría | Dato | Default típico |
|---|---|---|
| Identidad | número / usuario / nombre, tipo, subsistema | número autoasignado; usuario = quien lo crea |
| Library list | porción del sistema | `QSYS, QSYS2, QHLPSYS, QUSRSYS` (de `QSYSLIBL`) |
| | porción del usuario | `QGPL, QTEMP` (de `QUSRLIBL` vía el JOBD) |
| | biblioteca actual (`*CURLIB`) | `*CRTDFT` (ninguna) o la del perfil |
| QTEMP | biblioteca temporal privada | vacía, recién creada |
| Seguridad | perfil, grupo, autoridad | del perfil que lo arranca |
| Formato | fecha (`DATFMT`) / decimal (`DECFMT`) / fecha del job | `*ISO` típico / punto decimal / fecha del sistema |
| Encoding | `CCSID`, país, idioma | del perfil / system values (ej. CCSID 37) |
| Salida | output queue, impresora | del JOBD / perfil |
| Ejecución | prioridad (`RUNPTY`), time slice, pool | prioridad ~20; resto del subsistema |
| Mensajes | nivel de job log, respuesta a inquiries | `LOG(4 0 *NOLIST)` típico |

## 6. Estado fresco al nacer (no heredado, sino limpio)

Más allá de los atributos estampados, el job arranca con **runtime state** limpio:

- **QTEMP vacía** (se llena con trabajo temporal).
- **Call stack** con solo el programa de entrada (o vacío hasta arrancar).
- **Sin archivos abiertos** (los ODP se abren a demanda).
- **Status = `ACTIVE`**, con timestamp de inicio.
- **Job log** recién empezando.

La tabla de §5 es **contexto heredado** (config estampada); esto es **estado mutable** que evoluciona.

## 7. Ciclo de vida y estados

```
   creado ──▶ JOBQ ──▶ ACTIVE ──▶ (termina) ──▶ OUTQ ──▶ purgado
              (en cola    (corriendo)            (terminó, salida
               esperando)                         aún en spool)
```

- **JOBQ** — en una *job queue* esperando recursos.
- **ACTIVE** — ejecutándose.
- **OUTQ** — terminó, pero su salida spooled persiste; el job "muerto" se puede inspeccionar un rato.

(Batch: el job se envía a una *job queue*, atada a un *subsistema* que decide cuántos jobs corren
a la vez y los activa. La cola es la sala de espera; el subsistema, el administrador.)

## 8. Cómo encaja con la ejecución (cliente ↔ runtime)

En nuestro diseño REST:

- El **flujo de control** (la lógica del programa BBK) corre del lado del **cliente**: el bytecode
  JVM o el binario C-AOT. Eso es lo "hilo".
- El **runtime service** sostiene el **job**: los recursos y el estado que el programa da por
  sentado (library list, QTEMP, archivos abiertos…). Eso es lo "proceso / tabla de recursos".

El cliente *ejecuta*; el runtime le *presta* el job contra el cual ejecutar. Por eso el job es una
**sesión que se abre y se cierra**, no una unidad de despacho que nosotros agendemos.

## 9. Qué modelamos nosotros (alcance)

No emulamos IBM i fielmente — queremos lo **mínimo para correr programas BBK**.

**Núcleo (primer corte candidato):**

- **Identidad**: número autoasignado, usuario (default `QUSER` o el del caller), nombre calificado.
- **Library list**: porción inicial configurable (path de resolución de objetos).
- **Ciclo de vida**: `start → ACTIVE → end`; el job termina como `ENDED` pero queda inspeccionable.
- **Timestamp** de creación.
- **Estado por-sesión** colgado del job (in-memory).

**Pronto (cuando lleguemos a datos / program calls):**

- **QTEMP** (biblioteca temporal del job).
- **Call stack** (para `CALL` / program calls).
- **Archivos abiertos / ODP** (al modelar acceso a datos).
- Quizás atributos de **formato** (decimal / fecha) si los programas los necesitan.

**Omitido o stub (al menos al principio):**

- Activation groups, subsistemas, job queues como administradores reales.
- Output queue / spool, job log, CCSID / país / idioma, prioridad / pools.

> **Decisión abierta:** si el primer corte es solo *identidad + ciclo de vida + library list*
> (lo que ya tocaba el scaffold), o si incluye **QTEMP y call stack** desde el arranque. A definir
> antes de codear.

---

*Referencia conceptual para el modelo de job de `bbk-runtime`. La API REST y el esquema de datos
concretos se documentan aparte cuando se implementen.*
