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
story.append(Paragraph("Proyecto rpg-bbk-compiler &nbsp;|&nbsp; Estado de tareas por m&oacute;dulo &nbsp;|&nbsp; 14 de junio de 2026", H_SUB))
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
    "estructuras, subrutinas, monitor y builtins. El <b>runtime</b> (bbk-runtime) qued&oacute; <b>inicializado</b> "
    "como m&oacute;dulo Spring Boot (scaffold de Spring Initializr, servicio REST); falta construir la superficie "
    "de IBM i (jobs, datos, spool, program calls) sobre esa base.", BODY))

# Tarjetas de resumen
summary_data = [[
    Paragraph('<b>plugin-bbk</b><br/><font size=8 color="#1f8a4c">COMPLETO</font><br/><font size=7 color="#5b6472">12/12 features &middot; 80 tests</font>', CELL),
    Paragraph('<b>rpg-frontend + debugger</b><br/><font size=8 color="#1f8a4c">TRADUCE + VALIDADO</font><br/><font size=7 color="#5b6472">free-form &middot; 63 tests</font>', CELL),
    Paragraph('<b>bbk-core (n&uacute;cleo)</b><br/><font size=8 color="#1f8a4c">EJECUTA BBK (2 backends)</font><br/><font size=7 color="#5b6472">bytecode+C &middot; sem&aacute;ntica compartida &middot; 97 tests</font>', CELL),
    Paragraph('<b>bbk-runtime</b><br/><font size=8 color="#b9770e">INICIADO</font><br/><font size=7 color="#5b6472">scaffold Spring Boot &middot; 1 test</font>', CELL),
]]
st = Table(summary_data, colWidths=[43.5*mm, 43.5*mm, 43.5*mm, 43.5*mm])
st.setStyle(TableStyle([
    ("BACKGROUND", (0,0), (0,0), GREEN_BG),
    ("BACKGROUND", (1,0), (1,0), GREEN_BG),
    ("BACKGROUND", (2,0), (2,0), GREEN_BG),
    ("BACKGROUND", (3,0), (3,0), AMBER_BG),
    ("BOX", (0,0), (-1,-1), 0.5, LINE),
    ("INNERGRID", (0,0), (-1,-1), 0.5, colors.white),
    ("VALIGN", (0,0), (-1,-1), "MIDDLE"),
    ("LEFTPADDING", (0,0), (-1,-1), 8),
    ("RIGHTPADDING", (0,0), (-1,-1), 8),
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
    ("docs/bbk-spec.md", "todo", "Spec formal de BBK (hoy: BBK.bnf + gram&aacute;tica + ejemplos de facto)"),
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

story.append(Paragraph("4 &nbsp; Runtime (bbk-runtime) &mdash; <font color='#b9770e'>inicializado (scaffold Spring Boot)</font>", H1))
story.append(Paragraph(
    "Decisi&oacute;n (2026-06-14): el runtime es un <b>servicio Spring Boot standalone invocado por REST/HTTP</b> "
    "(elegido sobre Rust). Va a manejar la superficie <i>gruesa</i> de IBM i (jobs, datos, spool, program calls); la "
    "aritm&eacute;tica BCD/strings <b>no</b> cruza la red &mdash; queda local en bbk-core / el c&oacute;digo generado "
    "(decisi&oacute;n #4 intacta). Por ahora el m&oacute;dulo est&aacute; <b>solo inicializado</b> con el scaffold de "
    "Spring Initializr.", BODY))
runtime = [
    ("M&oacute;dulo Spring Boot inicializado", "done",
     "Scaffold de Spring Initializr (Spring Boot 3.5.5, Gradle Kotlin DSL, Java 21). Booteable; compila sobre "
     "Gradle 9.5 + JDK 25 (release 21). Test <font face='Courier'>contextLoads</font>"),
    ("Jobs / sesiones", "todo", "Contexto de ejecuci&oacute;n por-job: usuario, estado, library list"),
    ("Library list (*LIBL)", "todo", "Path de b&uacute;squeda de objetos por job"),
    ("Acceso a datos / registros (DDS)", "todo", "Capa record-level. Falta elegir el almac&eacute;n (SQLite / H2 / Postgres)"),
    ("Program calls (CALL a *PGM)", "todo", "Invocaci&oacute;n de otros programas dentro de un job"),
    ("Activation groups", "todo", "Grupos de activaci&oacute;n"),
    ("Spool files", "todo", "Archivos de spool"),
    ("Lado cliente (bytecode / C-AOT &rarr; runtime)", "todo", "Que el c&oacute;digo generado arranque un job y llame al runtime por HTTP"),
]
story.append(status_table(runtime, "Tarea"))
story.append(Paragraph(
    "<b>1 test</b> en bbk-runtime (<font face='Courier'>contextLoads</font>): el contexto Spring arranca limpio. "
    "La superficie de IBM i se construye sobre esta base.", BODY))

story.append(Paragraph("5 &nbsp; Otros m&oacute;dulos &mdash; sin empezar", H1))
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
story.append(Paragraph("6 &nbsp; Deuda t&eacute;cnica / higiene pendiente", H1))
hygiene = [
    ("Quitar logs de diagn&oacute;stico", "todo", "BBK-REF / BBK-RESOLVE / BBK-SCOPE quedaron del debug de cross-file"),
    ("Actualizar TODO.md", "todo", "Desactualizado: lista autocomplete como pendiente cuando ya est&aacute; hecho"),
    ("Unificar cat&aacute;logo de builtins", "todo", "BbkBifProvider tiene lista hardcodeada en paralelo al nuevo registry"),
    ("Verificaci&oacute;n manual en GUI", "todo", "Smart completion / inspections / Ctrl+P / Ctrl+Q a&uacute;n sin probar a mano end-to-end"),
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
