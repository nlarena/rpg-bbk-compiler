# -*- coding: utf-8 -*-
"""Genera el informe de estado del proyecto BoxBreaker (rpg-bbk-compiler) en PDF."""

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
)
from reportlab.lib.enums import TA_LEFT

OUT = "BoxBreaker-estado-tareas.pdf"

# ----- Paleta -----
NAVY = colors.HexColor("#1f2a44")
ACCENT = colors.HexColor("#2f6fed")
GREEN = colors.HexColor("#1f8a4c")
GREEN_BG = colors.HexColor("#e7f4ec")
AMBER = colors.HexColor("#b9770e")
AMBER_BG = colors.HexColor("#fdf3e2")
GREY_BG = colors.HexColor("#f1f3f7")
GREY_TX = colors.HexColor("#5b6472")
LINE = colors.HexColor("#d4d9e2")

styles = getSampleStyleSheet()

def style(name, **kw):
    base = kw.pop("parent", styles["Normal"])
    return ParagraphStyle(name, parent=base, **kw)

H_TITLE = style("HTitle", parent=styles["Title"], fontSize=24, textColor=NAVY,
                spaceAfter=2, leading=28)
H_SUB = style("HSub", fontSize=11, textColor=GREY_TX, spaceAfter=2, leading=15)
H1 = style("H1", fontSize=15, textColor=NAVY, spaceBefore=16, spaceAfter=6,
           leading=18, fontName="Helvetica-Bold")
H2 = style("H2", fontSize=12, textColor=NAVY, spaceBefore=10, spaceAfter=4,
           leading=15, fontName="Helvetica-Bold")
BODY = style("Body", fontSize=9.5, textColor=colors.HexColor("#222831"),
             leading=14, spaceAfter=4)
CELL = style("Cell", fontSize=9, leading=12.5, textColor=colors.HexColor("#222831"))
CELL_MUTE = style("CellMute", fontSize=9, leading=12.5, textColor=GREY_TX)
SMALL = style("Small", fontSize=8, textColor=GREY_TX, leading=11)

def chip(text, txt_color, bg_color):
    return Paragraph(
        f'<font color="{txt_color.hexval()}"><b>{text}</b></font>', CELL)

story = []

# ===================== PORTADA / HEADER =====================
story.append(Paragraph("BoxBreaker — Informe de estado", H_TITLE))
story.append(Paragraph("Proyecto rpg-bbk-compiler &nbsp;|&nbsp; Estado de tareas por m&oacute;dulo &nbsp;|&nbsp; 20 de junio de 2026", H_SUB))
story.append(HRFlowable(width="100%", thickness=2, color=ACCENT, spaceBefore=6, spaceAfter=10))

# ===================== RESUMEN EJECUTIVO =====================
story.append(Paragraph("Resumen", H1))
story.append(Paragraph(
    "El proyecto traduce RPG (legacy) a BBK (lenguaje propio moderno) y, a futuro, a ejecutable. "
    "El <b>plugin de IntelliJ para BBK</b> est&aacute; completo (12/12 funcionalidades, 80 tests). El "
    "<b>frontend RPG&rarr;BBK</b> traduce programas free-form completos (loop validado con el plugin) y tiene un "
    "<b>debugger</b> que muestra la traducci&oacute;n l&iacute;nea a l&iacute;nea. El <b>n&uacute;cleo</b> "
    "(bbk-core, Java in-JVM) ya <b>compila y ejecuta BBK en la JVM</b> (lexer + AST + parser + backend a bytecode "
    "con ASM) y <b>ambos backends (JVM y C) cubren todo el lenguaje no-SO</b>: procedimientos, decimales, arrays, "
    "estructuras, subrutinas, monitor y builtins. El <b>runtime</b> (bbk-runtime) dej&oacute; de ser un scaffold: "
    "ya <b>persiste jobs</b> con ciclo de vida real (un trabajo arranca, corre en su hilo y termina), tiene "
    "<b>login JWT + autoridades especiales estilo IBM i</b> y expone una <b>API REST</b> (sign-on, usuarios y "
    "jobs &mdash; listado WRKACTJOB-style + env&iacute;o de trabajo) sobre MySQL/JPA con logging a archivo. Encima "
    "se construy&oacute; un <b>visor .NET (RuntimeVisor)</b> estilo pantalla verde 5250 que consume esa API "
    "(sign-on &rarr; men&uacute; &rarr; trabajos activos), con <b>i18n ES/EN</b>. Es justo la pieza que no existe "
    "en open source y la base sobre la que crecer&iacute;a una versi&oacute;n profesional.", BODY))

# Tarjetas de resumen
summary_data = [[
    Paragraph('<b>plugin-bbk</b><br/><font size=8 color="#1f8a4c">COMPLETO</font><br/><font size=7 color="#5b6472">12/12 features &middot; 80 tests</font>', CELL),
    Paragraph('<b>rpg-frontend + debugger</b><br/><font size=8 color="#1f8a4c">TRADUCE + VALIDADO</font><br/><font size=7 color="#5b6472">free-form &middot; 63 tests</font>', CELL),
    Paragraph('<b>bbk-core (n&uacute;cleo)</b><br/><font size=8 color="#1f8a4c">EJECUTA BBK (2 backends)</font><br/><font size=7 color="#5b6472">bytecode+C &middot; 97 tests</font>', CELL),
    Paragraph('<b>bbk-runtime</b><br/><font size=8 color="#1f8a4c">JOBS + AUTH + API</font><br/><font size=7 color="#5b6472">JWT &middot; MySQL &middot; 3 tests</font>', CELL),
    Paragraph('<b>RuntimeVisor (.NET)</b><br/><font size=8 color="#1f8a4c">VISOR 5250</font><br/><font size=7 color="#5b6472">WRKACTJOB &middot; i18n ES/EN</font>', CELL),
]]
st = Table(summary_data, colWidths=[34*mm, 34*mm, 34*mm, 34*mm, 34*mm])
st.setStyle(TableStyle([
    ("BACKGROUND", (0,0), (0,0), GREEN_BG),
    ("BACKGROUND", (1,0), (1,0), GREEN_BG),
    ("BACKGROUND", (2,0), (2,0), GREEN_BG),
    ("BACKGROUND", (3,0), (3,0), GREEN_BG),
    ("BACKGROUND", (4,0), (4,0), GREEN_BG),
    ("BOX", (0,0), (-1,-1), 0.5, LINE),
    ("INNERGRID", (0,0), (-1,-1), 0.5, colors.white),
    ("VALIGN", (0,0), (-1,-1), "MIDDLE"),
    ("LEFTPADDING", (0,0), (-1,-1), 6),
    ("RIGHTPADDING", (0,0), (-1,-1), 6),
    ("TOPPADDING", (0,0), (-1,-1), 8),
    ("BOTTOMPADDING", (0,0), (-1,-1), 8),
]))
story.append(Spacer(1, 4))
story.append(st)

# ===================== TABLA HELPER =====================
def status_table(rows, col0="Tarea"):
    data = [[Paragraph(f"<b>{col0}</b>", CELL),
             Paragraph("<b>Estado</b>", CELL),
             Paragraph("<b>Detalle</b>", CELL)]]
    styles_rows = [
        ("BACKGROUND", (0,0), (-1,0), NAVY),
        ("TEXTCOLOR", (0,0), (-1,0), colors.white),
        ("BOX", (0,0), (-1,-1), 0.5, LINE),
        ("LINEBELOW", (0,0), (-1,-1), 0.4, LINE),
        ("VALIGN", (0,0), (-1,-1), "MIDDLE"),
        ("LEFTPADDING", (0,0), (-1,-1), 6),
        ("RIGHTPADDING", (0,0), (-1,-1), 6),
        ("TOPPADDING", (0,0), (-1,-1), 5),
        ("BOTTOMPADDING", (0,0), (-1,-1), 5),
    ]
    for i, (task, st_label, detail) in enumerate(rows, start=1):
        if st_label == "done":
            badge = Paragraph('<font color="#1f8a4c"><b>&#10003; Hecho</b></font>', CELL)
            bg = GREEN_BG
        elif st_label == "wip":
            badge = Paragraph('<font color="#b9770e"><b>&#9679; En curso</b></font>', CELL)
            bg = AMBER_BG
        else:
            badge = Paragraph('<font color="#9aa0ab"><b>&#9675; Pendiente</b></font>', CELL)
            bg = colors.white
        data.append([Paragraph(task, CELL), badge, Paragraph(detail, CELL_MUTE)])
        styles_rows.append(("BACKGROUND", (1, i), (1, i), bg))
    t = Table(data, colWidths=[52*mm, 26*mm, 96*mm], repeatRows=1)
    t.setStyle(TableStyle(styles_rows))
    return t

# ===================== plugin-bbk =====================
story.append(Paragraph("1 &nbsp; plugin-bbk &mdash; Soporte de lenguaje BBK en IntelliJ", H1))
story.append(Paragraph("12 funcionalidades de asistencia de editor. Estado: <b>completo</b>, 80 tests automatizados verdes.", BODY))

features = [
    ("1. Autocomplete b&aacute;sico", "done", "Ctrl+Space: keywords, tipos, modificadores seg&uacute;n contexto"),
    ("2. Autocomplete de identificadores", "done", "Variables, procs, constantes declaradas en scope"),
    ("3. Autocomplete de miembros", "done", "employee.&lt;campo&gt; sugiere subfields del DS (incl. cross-file)"),
    ("4. Live templates", "done", "dcls + Tab expande a DCL-S; 3 contextos"),
    ("5. Go to declaration", "done", "Ctrl+B / Ctrl+Click salta a la declaraci&oacute;n"),
    ("6. Find usages", "done", "Alt+F7, intra-archivo y cross-file v&iacute;a stub index"),
    ("7. Rename refactor", "done", "Shift+F6 renombra en todos los usos; valida reservadas"),
    ("8. B&uacute;squeda de s&iacute;mbolos cross-file", "done", "Ctrl+Alt+Shift+N por nombre en todo el proyecto"),
    ("9. Smart completion", "done", "Ctrl+Shift+Space filtra candidatos por tipo esperado"),
    ("10. Inspections con tipos", "done", "7 inspecciones: mismatch en asignaci&oacute;n/return/args/condici&oacute;n/INZ + refs sin resolver"),
    ("11. Parameter info", "done", "Ctrl+P muestra la firma con el par&aacute;metro actual en negrita"),
    ("12. Quick documentation", "done", "Ctrl+Q para declaraciones y 21 builtins catalogados"),
]
story.append(status_table(features, "Funcionalidad"))

story.append(Paragraph("Infraestructura de soporte (toda implementada)", H2))
infra = [
    ("Lexer + parser (BBK.bnf)", "done", "JFlex + Grammar-Kit: todas las construcciones del lenguaje"),
    ("PSI + mixins + factory", "done", "Naming, setName, getTextOffset, s&iacute;ntesis de IDENT"),
    ("Scope (3 impls + walker)", "done", "Module / Procedure / Block scope"),
    ("References (4 + contributor)", "done", "Ident / Member / Subroutine / Type"),
    ("Stubs + indexes (7+7)", "done", "Resoluci&oacute;n cross-file persistente"),
    ("Type system (types/)", "done", "BbkType, inferencia, assignability con promociones"),
    ("Editor polish", "done", "Brace matcher, smart typing, prefix matcher con hyphen"),
]
story.append(status_table(infra, "Componente"))

story.append(Paragraph(
    "<b>Bug resuelto recientemente:</b> falso positivo &laquo;Unresolved reference&raquo; en member-access "
    "cross-file (currentOrder.orderId). Causa: BbkTypeResolver no segu&iacute;a el LIKEDS hacia un DS de otro "
    "archivo. Fix: fallback al &iacute;ndice de proyecto. Beneficio extra: ahora el member-access cross-file "
    "resuelve de verdad (Ctrl+B y autocomplete de miembros).", BODY))

# ===================== rpg-frontend =====================
story.append(Paragraph("2 &nbsp; rpg-frontend &mdash; Traductor RPG &rarr; BBK", H1))
story.append(Paragraph(
    "Reci&eacute;n iniciado. Decisiones tomadas: entrada <b>free-format RPGLE</b>, lenguaje <b>Java</b>, "
    "salida <b>texto .bbk</b> (validable con el plugin). Pipeline objetivo: "
    "<i>RPG source &rarr; lexer &rarr; parser &rarr; AST &rarr; emitter &rarr; .bbk</i>.", BODY))
frontend = [
    ("Setup del m&oacute;dulo", "done", "rpg-frontend en Gradle (Java 21 + JUnit), registrado en settings"),
    ("RpgLexer + tokens (free-form completo)", "done",
     "Toda la superficie l&eacute;xica fully-free (grammar &sect;2): IDENT, keywords, %BIFs, 9 literales, "
     "operadores, `.` calificado, directivas /COPY //IF. 19 tests"),
    ("AST de RPG (completo, &sect;4.2/4.3/4.4)", "done",
     "sealed interfaces + records: 10 declaraciones, 15 statements, 9 expresiones, tipos (Scalar/Like/LikeDs/LikeRec). 5 tests"),
    ("RpgParser (gram&aacute;tica free-form completa)", "done",
     "Recursive descent: todas las declaraciones (ctl-opt, dcl-s/c/ds/pr/pi/f/proc) y statements (if, select, dow, dou, "
     "for, monitor, begsr, file ops...); precedencia &sect;4.4; resuelve la ambig&uuml;edad del '='. 20 tests"),
    ("BbkEmitter (RPG &rarr; texto BBK)", "done",
     "Traduce toda la superficie: tipos en may&uacute;s, '='&rarr;'==', loops &rarr; llaves (while/do-while/for), "
     "dcl-pi &rarr; par&aacute;metros inline, ':'&rarr;',', figurativas. 19 tests"),
    ("Validaci&oacute;n del loop con plugin-bbk", "done",
     "8 tests round-trip (RPG &rarr; BBK &rarr; parser del plugin, 0 errores). Encontr&oacute; y corrigi&oacute; un mismatch: "
     "%subst &rarr; substr (BBK no usa el prefijo '%')"),
]
story.append(status_table(frontend, "Tarea"))
story.append(Paragraph(
    "<b>63 tests verdes</b> en rpg-frontend. El frontend <b>traduce programas RPG free-form completos a BBK</b> "
    "(todas las declaraciones, statements y expresiones de la gram&aacute;tica &sect;4) y el <b>loop est&aacute; "
    "verificado</b>: el BBK generado parsea sin errores con plugin-bbk (test round-trip en el build del plugin). "
    "El '*' se desambigua por contexto. &Uacute;nico gap l&eacute;xico deferido: asignaci&oacute;n compuesta "
    "(`+= -= *= /=`), ausente en la gram&aacute;tica. Detalle en <b>docs/rpg-frontend/overview.md</b>.", BODY))
story.append(Paragraph(
    "<b>M&oacute;dulo debugger:</b> muestra la traducci&oacute;n l&iacute;nea a l&iacute;nea (RPG a la izquierda con "
    "n&uacute;mero de l&iacute;nea, BBK a la derecha) m&aacute;s el BBK completo. Ejecutable: "
    "<font face='Courier'>gradlew :debugger:run --args=\"examples/sample.rpgle\"</font>. 5 tests.", BODY))

# ===================== núcleo =====================
story.append(Paragraph("3 &nbsp; N&uacute;cleo del compilador (bbk-core) &mdash; <font color='#1f8a4c'>ejecuta BBK</font>", H1))
story.append(Paragraph(
    "Toma el texto BBK del frontend y lo compila. <b>Java in-JVM</b> (el plugin lo invoca en el mismo proceso, sin "
    "binarios nativos). Un solo AST de BBK alimenta <b>dos backends</b>: BBK &rarr; <b>bytecode JVM</b> "
    "(corre in-process) y BBK &rarr; <b>C</b> (compila con gcc), ambos a la par. No hay IR separado: el AST + su an&aacute;lisis "
    "<i>es</i> el IR (evita duplicar). Parser headless propio (el del plugin est&aacute; atado a IntelliJ).", BODY))
core = [
    ("Lenguaje del n&uacute;cleo", "done", "Java in-JVM (decidido)"),
    ("Lexer de BBK", "done", "Completo, grounded en BBK.bnf: case-sensitive, operadores C-style, hex/oct/float/dec"),
    ("AST de BBK", "done", "Completo: 9 declaraciones, 17 statements, 11 expresiones (ternario, bitwise, [] vs ())"),
    ("Parser de BBK", "done", "Toda la gram&aacute;tica; sin ambig&uuml;edad del '=' (== vs =); parsea el output del frontend"),
    ("Backend BBK &rarr; bytecode JVM", "done",
     "ASM &rarr; clase bbk.Main; corre in-process en la JVM. Enteros/bool/strings, aritm./bitwise/l&oacute;gica/ternario, "
     "if/while/do-while/for/select, break/continue, print. <font face='Courier'>gradlew :bbk-core:run</font>"),
    ("Backend JVM: lenguaje no-SO completo", "done",
     "Procedimientos (DCL-PROC&rarr;m&eacute;todos, recursi&oacute;n, CTL-OPT MAIN), decimales exactos "
     "(PACKED/ZONED&rarr;BigDecimal con escala+redondeo @halfup/@trunc), FLOAT, arrays (incl. arrays de DS), "
     "estructuras (DCL-DS/TEMPLATE/LIKEDS), subrutinas (BEGSR/EXSR/LEAVESR), monitor/on-error, constantes, "
     "valores especiales, 15 builtins puros. 73 tests. Deferido: OVERLAY, fechas, file/EXTPGM (SO)"),
    ("Backend BBK &rarr; C: lenguaje no-SO completo", "done",
     "A la par del JVM: emite C self-contained (prelude de helpers string/decimal/monitor). Procedimientos, "
     "decimales (long double con escala), arrays (incl. de DS), estructuras, subrutinas, monitor (setjmp), "
     "builtins. <b>Verificado con gcc real</b> (WinLibs MinGW 16.1): compila+corre v&iacute;a "
     "<font face='Courier'>--run-c</font>, los tests gated ejecutan (no se saltan). Deferido: OVERLAY, fechas, file (SO)"),
    ("An&aacute;lisis sem&aacute;ntico compartido", "done",
     "Paquete <font face='Courier'>semantic</font>: un solo analizador resuelve nombres + infiere el tipo de cada "
     "expresi&oacute;n (Type neutral) y junta diagn&oacute;sticos. Los dos backends consumen el SemanticModel "
     "(borrado el typeOf/lookup duplicado); solo conservan el storage f&iacute;sico. Unific&oacute; <font face='Courier'>**</font> a FLOAT"),
    ("docs/bbk-spec.md", "done",
     "Spec formal: l&eacute;xico, sistema de tipos + torre num&eacute;rica, declaraciones, sentencias, "
     "precedencia de operadores, builtins, valores especiales, lowering (qu&eacute; entra y qu&eacute; no) y EBNF. "
     "Grounded en bbk-core"),
    ("Lenguaje de bbk-runtime", "done", "Spring Boot, servicio REST standalone (decidido sobre Rust)"),
]
story.append(status_table(core, "Tarea"))
story.append(Paragraph(
    "<b>97 tests verdes</b> en bbk-core. El loop completo del proyecto cierra de punta a punta: "
    "RPG &rarr; frontend &rarr; texto BBK &rarr; bbk-core (parser &rarr; AST) &rarr; <b>dos backends</b> "
    "(bytecode JVM que corre in-process, y C que <b>compila+corre con gcc real</b> &mdash; WinLibs MinGW 16.1). "
    "Verificado end-to-end en ambos: <font face='Courier'>factorial(5)=120</font>, "
    "<font face='Courier'>price(199.95)*qty(3)=599.85</font> (escala 2), arrays de DS, procedimientos, select, "
    "monitor atrapando divisi&oacute;n por cero. La verificaci&oacute;n con gcc destap&oacute; y corrigi&oacute; "
    "dos bugs de MinGW + long double (stdio ANSI para <font face='Courier'>%Lf</font>; snap del truncado decimal). "
    "Decisiones abiertas: BigDecimal (JVM) / long double (C) vs BCD propio exacto; arrays 0-based.", BODY))

story.append(Paragraph("4 &nbsp; Runtime (bbk-runtime) &mdash; <font color='#1f8a4c'>jobs + auth + API REST</font>", H1))
story.append(Paragraph(
    "Servicio <b>Spring Boot standalone invocado por REST/HTTP</b> (elegido sobre Rust). Maneja la superficie "
    "<i>gruesa</i> de IBM i; la aritm&eacute;tica BCD/strings <b>no</b> cruza la red &mdash; queda local en bbk-core / "
    "el c&oacute;digo generado (decisi&oacute;n #4 intacta). Ya dej&oacute; de ser scaffold: <b>persiste jobs con "
    "ciclo de vida real</b>, tiene <b>login JWT con autoridades especiales</b> estilo IBM i y una <b>API REST</b> "
    "operativa sobre MySQL/JPA.", BODY))
runtime = [
    ("Base Spring Boot", "done",
     "Spring Boot 3.5.5, Gradle Kotlin DSL, Java 21 (release 21 sobre JDK 25). Persistencia <b>MySQL + JPA/Hibernate</b> "
     "(ddl-auto update) y <b>logging a archivo</b> (Logback con rolling)"),
    ("Jobs persistidos + ciclo de vida", "done",
     "Entidad <font face='Courier'>Job</font> (n&uacute;mero estilo IBM i, estado ACTIVE/ENDED) con historial de "
     "<b>eventos</b> (audit BPMS-style) y <b>atributos EAV</b>. Un trabajo <b>arranca, corre en su propio hilo y "
     "termina</b> (JobRunner sincr&oacute;nico y as&iacute;ncrono)"),
    ("Library list (*LIBL)", "done", "<font face='Courier'>JobLibrary</font> ordenada: path de b&uacute;squeda de objetos por job"),
    ("Login JWT + autoridades especiales", "done",
     "<font face='Courier'>UserProfile</font> con hash BCrypt y <b>special authorities</b> (*ALLOBJ, *SECADM, *JOBCTL...). "
     "Sign-on emite JWT (jjwt); Spring Security stateless; perfil semilla <font face='Courier'>QSECOFR</font>"),
    ("API REST", "done",
     "<font face='Courier'>/api/auth</font> (login + me), <font face='Courier'>/api/users</font> (alta/listado, exige "
     "*SECADM) y <font face='Courier'>/api/jobs</font>: listado <b>WRKACTJOB-style</b> + <b>POST</b> de un trabajo que "
     "espera (exige *JOBCTL). Autorizaci&oacute;n por endpoint (401/403)"),
    ("Acceso a datos / registros (DDS)", "todo", "Capa record-level sobre las tablas de negocio"),
    ("Program calls (CALL a *PGM)", "todo", "Invocaci&oacute;n de otros programas dentro de un job"),
    ("Activation groups / Spool files", "todo", "Grupos de activaci&oacute;n y archivos de spool"),
    ("Lado cliente (bytecode / C-AOT &rarr; runtime)", "todo", "Que el c&oacute;digo generado arranque un job y llame al runtime por HTTP"),
]
story.append(status_table(runtime, "Tarea"))
story.append(Paragraph(
    "<b>3 tests</b> en bbk-runtime sobre <b>MySQL real</b>: ciclo de vida de jobs, sign-on/JWT y "
    "<font face='Courier'>contextLoads</font>. Verificado end-to-end: login <font face='Courier'>QSECOFR</font> &rarr; "
    "<font face='Courier'>GET /api/jobs</font> &rarr; grid del visor; un trabajo <font face='Courier'>BBKWAIT</font> "
    "enviado por <font face='Courier'>POST</font> aparece <b>ACTIVE</b> y pasa solo a <b>ENDED</b> al terminar su espera.", BODY))

# ===================== RuntimeVisor (.NET) =====================
story.append(Paragraph("5 &nbsp; RuntimeVisor &mdash; <font color='#1f8a4c'>visor .NET (pantalla verde 5250)</font>", H1))
story.append(Paragraph(
    "Cliente de escritorio <b>.NET WinForms (.NET Framework 4.8)</b> que consume la API del runtime con est&eacute;tica "
    "de terminal <b>5250</b> (verde sobre negro, Consolas). Practica .NET y, a la vez, le pone cara al runtime. "
    "Flujo: <i>sign-on &rarr; Home (cabecera 5250) &rarr; trabajos activos</i>, navegaci&oacute;n h&iacute;brida "
    "(comando <font face='Courier'>WRKACTJOB</font> / opci&oacute;n num&eacute;rica + teclas de funci&oacute;n).", BODY))
visor = [
    ("Sign-on contra el runtime", "done", "Login JWT (usuario/contrase&ntilde;a &rarr; token guardado en la sesi&oacute;n); mensajes de error claros"),
    ("Home estilo 5250", "done", "Cabecera Sistema/Subsistema/Terminal/Usuario/Fecha/Hora con reloj en vivo; se <b>colapsa</b> al navegar"),
    ("WRKACTJOB (trabajos activos)", "done",
     "Grilla que consulta <font face='Courier'>GET /api/jobs</font> y muestra n&uacute;mero/usuario/nombre/estado/creado/"
     "terminado. <b>F5</b> refresca, <b>F3</b> vuelve"),
    ("Internacionalizaci&oacute;n (i18n)", "done",
     "<font face='Courier'>.resx</font> + ResourceManager: espa&ntilde;ol (base) e ingl&eacute;s (sat&eacute;lite). "
     "Idioma por cultura del SO u override <font face='Courier'>--lang=en</font> / <font face='Courier'>BBK_LANG</font>"),
    ("Enviar trabajo desde el visor", "todo", "Un F6 que haga el POST y refresque, para lanzar y ver un job activo sin salir"),
    ("Vistas adicionales (usuarios, etc.)", "todo", "Reusar la navegaci&oacute;n para m&aacute;s superficies del runtime"),
]
story.append(status_table(visor, "Tarea"))
story.append(Paragraph(
    "Compila con MSBuild de Visual Studio. Pendiente de higiene: quitar las credenciales precargadas "
    "(<font face='Courier'>QSECOFR/qsecofr</font>) y agregar <font face='Courier'>.gitignore</font> para los "
    "artefactos .NET (.vs/, obj/, bin/).", BODY))

story.append(Paragraph("6 &nbsp; Otros m&oacute;dulos &mdash; sin empezar", H1))
others = [
    ("plugin-rpg: highlighting", "todo", "Resaltado de sintaxis RPG en IntelliJ"),
    ("plugin-rpg: editor parser", "todo", "Errores inline en RPG"),
    ("plugin-rpg: 'translate to BBK'", "todo", "Comando que invoca el rpg-frontend"),
    ("boxbreaker-ide: bundle", "todo", "IDE empaquetada sobre IntelliJ Platform SDK (solo scaffolding)"),
    ("boxbreaker-ide: branding + installer", "todo", "Splash, icono, build de distribuci&oacute;n"),
    ("Testing E2E", "todo", "Compilar RPG &rarr; ejecutar &rarr; verificar; equivalencia int&eacute;rprete vs AOT"),
    ("Distribuci&oacute;n", "todo", "Publicar plugins en Marketplace; distribuir la IDE"),
]
story.append(status_table(others, "Tarea"))

# ===================== Higiene pendiente =====================
story.append(Paragraph("7 &nbsp; Deuda t&eacute;cnica / higiene pendiente", H1))
hygiene = [
    ("Credenciales precargadas en el visor", "todo", "Form1 trae QSECOFR/qsecofr hardcodeado para no tipear; quitar antes de uso real (ver TODO.md del visor)"),
    ("<font face='Courier'>.gitignore</font> para el m&oacute;dulo .NET", "todo", "Se cuelan como untracked .vs/, obj/, bin/, *.suo, *.vsidx"),
    ("Secreto JWT y datasource de dev", "todo", "<font face='Courier'>application.properties</font> tiene secreto JWT y root/root de MySQL en claro (solo dev)"),
    ("Quitar logs de diagn&oacute;stico", "todo", "BBK-REF / BBK-RESOLVE / BBK-SCOPE quedaron del debug de cross-file"),
    ("Unificar cat&aacute;logo de builtins", "todo", "BbkBifProvider tiene lista hardcodeada en paralelo al nuevo registry"),
    ("Inspections V1.5 + quick-fixes", "todo", "Unused / Shadowed / ReservedWord / LikeCycle + fixes (diferidas)"),
]
story.append(status_table(hygiene, "Tarea"))

story.append(Spacer(1, 10))
story.append(HRFlowable(width="100%", thickness=0.6, color=LINE, spaceAfter=4))
story.append(Paragraph(
    "Generado autom&aacute;ticamente a partir del estado del repositorio. "
    "Leyenda: <font color='#1f8a4c'><b>&#10003; Hecho</b></font> &nbsp; "
    "<font color='#b9770e'><b>&#9679; En curso</b></font> &nbsp; "
    "<font color='#9aa0ab'><b>&#9675; Pendiente</b></font>.", SMALL))


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(GREY_TX)
    canvas.drawString(20*mm, 12*mm, "BoxBreaker — rpg-bbk-compiler")
    canvas.drawRightString(190*mm, 12*mm, "Página %d" % doc.page)
    canvas.setStrokeColor(LINE)
    canvas.line(20*mm, 15*mm, 190*mm, 15*mm)
    canvas.restoreState()


doc = SimpleDocTemplate(
    OUT, pagesize=A4,
    leftMargin=20*mm, rightMargin=20*mm, topMargin=18*mm, bottomMargin=20*mm,
    title="BoxBreaker - Informe de estado", author="rpg-bbk-compiler",
)
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print("OK ->", OUT)
