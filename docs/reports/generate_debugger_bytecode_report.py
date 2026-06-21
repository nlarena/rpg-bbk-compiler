# -*- coding: utf-8 -*-
"""Informe: cambios para acoplar el debugger al programa compilado (bytecode real / JDI)."""

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
)

OUT = "BoxBreaker-debugger-bytecode.pdf"

# ----- Paleta (igual que el informe de estado) -----
NAVY = colors.HexColor("#1f2a44")
ACCENT = colors.HexColor("#2f6fed")
GREEN = colors.HexColor("#1f8a4c")
GREEN_BG = colors.HexColor("#e7f4ec")
AMBER = colors.HexColor("#b9770e")
AMBER_BG = colors.HexColor("#fdf3e2")
BLUE_BG = colors.HexColor("#e8f0fe")
GREY_BG = colors.HexColor("#f1f3f7")
GREY_TX = colors.HexColor("#5b6472")
LINE = colors.HexColor("#d4d9e2")

styles = getSampleStyleSheet()


def style(name, **kw):
    base = kw.pop("parent", styles["Normal"])
    return ParagraphStyle(name, parent=base, **kw)


H_TITLE = style("HTitle", parent=styles["Title"], fontSize=23, textColor=NAVY, spaceAfter=2, leading=27)
H_SUB = style("HSub", fontSize=11, textColor=GREY_TX, spaceAfter=2, leading=15)
H1 = style("H1", fontSize=15, textColor=NAVY, spaceBefore=16, spaceAfter=6, leading=18, fontName="Helvetica-Bold")
H2 = style("H2", fontSize=12, textColor=NAVY, spaceBefore=10, spaceAfter=4, leading=15, fontName="Helvetica-Bold")
BODY = style("Body", fontSize=9.5, textColor=colors.HexColor("#222831"), leading=14, spaceAfter=4)
CELL = style("Cell", fontSize=9, leading=12.5, textColor=colors.HexColor("#222831"))
CELL_MUTE = style("CellMute", fontSize=9, leading=12.5, textColor=GREY_TX)
NUMP = style("NumP", fontSize=11, leading=13, textColor=ACCENT, fontName="Helvetica-Bold", alignment=1)
SMALL = style("Small", fontSize=8, textColor=GREY_TX, leading=11)


def C(s):
    return "<font face='Courier'>%s</font>" % s


story = []

# ===================== PORTADA =====================
story.append(Paragraph("BoxBreaker &mdash; Debugger sobre bytecode real", H_TITLE))
story.append(Paragraph(
    "Proyecto rpg-bbk-compiler &nbsp;|&nbsp; An&aacute;lisis: acoplar el debugger al programa compilado &nbsp;|&nbsp; 20 de junio de 2026",
    H_SUB))
story.append(HRFlowable(width="100%", thickness=2, color=ACCENT, spaceBefore=6, spaceAfter=10))

# ===================== RESUMEN =====================
story.append(Paragraph("Resumen", H1))
story.append(Paragraph(
    "Hoy el debugger <b>no depura el programa compilado</b>: corre un <b>int&eacute;rprete tree-walking</b> propio "
    "(m&oacute;dulo " + C("bbk-debugger") + ") que vuelve a parsear el " + C(".bbk") + " y camina el AST. Es un "
    "<b>tercer motor de ejecuci&oacute;n</b> en paralelo a los dos backends reales (bytecode JVM y C): lo que se "
    "depura no es lo que se ejecuta. Acoplar el debugger al programa compilado significa depurar el <b>bytecode "
    "real</b> que emite " + C("JvmCompiler") + ", usando el mecanismo est&aacute;ndar de la plataforma Java: "
    "<b>JDI/JDWP</b> (la misma t&eacute;cnica que usan los debuggers de Java, Kotlin y Scala en IntelliJ). "
    "El programa se lanza en una <b>JVM aparte</b> con el agente de debug, y el IDE se <b>conecta</b> a ella para "
    "poner breakpoints, leer variables y hacer step.", BODY))
story.append(Paragraph(
    "Esto elimina el int&eacute;rprete como motor, pero <b>traslada el costo</b> a tres lugares: el compilador "
    "tiene que emitir <b>m&aacute;s informaci&oacute;n de debug</b>, hay que <b>lanzar y orquestar un proceso "
    "externo</b> (hoy todo corre in-process dentro del IDE), y hay que <b>mapear los nombres y valores de la "
    "JVM de vuelta a BBK</b> (un " + C("emp.salary") + " de BBK vive como campo " + C("emp$salary") + ", un INT "
    "como " + C("long") + ", un decimal como " + C("BigDecimal") + "). Es un cambio de arquitectura cohesivo, no "
    "una mejora incremental del int&eacute;rprete actual.", BODY))

# tarjetas hoy -> objetivo
cards = [[
    Paragraph('<b>HOY</b><br/><font size=8 color="#b9770e">Int&eacute;rprete (3er motor)</font><br/>'
              '<font size=7 color="#5b6472">camina el AST &middot; in-process</font>', CELL),
    Paragraph('<b>&rarr;</b>', style("arrow", fontSize=16, textColor=ACCENT, alignment=1)),
    Paragraph('<b>OBJETIVO</b><br/><font size=8 color="#1f8a4c">Bytecode real (JDI/JDWP)</font><br/>'
              '<font size=7 color="#5b6472">se depura lo que se ejecuta &middot; JVM aparte</font>', CELL),
    Paragraph('<b>YA HECHO</b><br/><font size=8 color="#1f8a4c">LineNumberTable</font><br/>'
              '<font size=7 color="#5b6472">n&uacute;meros de l&iacute;nea BBK en el bytecode</font>', CELL),
]]
ct = Table(cards, colWidths=[52*mm, 12*mm, 56*mm, 50*mm])
ct.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (0, 0), AMBER_BG),
    ("BACKGROUND", (2, 0), (2, 0), BLUE_BG),
    ("BACKGROUND", (3, 0), (3, 0), GREEN_BG),
    ("BOX", (0, 0), (0, 0), 0.5, LINE),
    ("BOX", (2, 0), (3, 0), 0.5, LINE),
    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7),
    ("TOPPADDING", (0, 0), (-1, -1), 8), ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
]))
story.append(Spacer(1, 4))
story.append(ct)

# ===================== 1. PUNTO DE PARTIDA =====================
story.append(Paragraph("1 &nbsp; Punto de partida (lo que hay hoy)", H1))


def fact_table(rows):
    data = [[Paragraph("<b>Pieza</b>", CELL), Paragraph("<b>Estado actual</b>", CELL), Paragraph("<b>Implicancia</b>", CELL)]]
    sr = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("BOX", (0, 0), (-1, -1), 0.5, LINE), ("LINEBELOW", (0, 0), (-1, -1), 0.4, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    for piece, st, imp in rows:
        data.append([Paragraph(piece, CELL), Paragraph(st, CELL_MUTE), Paragraph(imp, CELL_MUTE)])
    t = Table(data, colWidths=[40*mm, 67*mm, 67*mm], repeatRows=1)
    t.setStyle(TableStyle(sr))
    return t


story.append(fact_table([
    ("Motor del debugger",
     C("bbk-debugger") + ": int&eacute;rprete tree-walking (" + C("Interpreter") + ") que reparsea el "
     + C(".bbk") + " y camina el AST, emitiendo un " + C("TraceStep") + " por sentencia.",
     "Se depura una <b>segunda ejecuci&oacute;n</b>, no el bytecode real. Es el punto que el objetivo elimina."),
    ("Proceso de debug (plugin)",
     C("BbkDebugProcess") + " corre el int&eacute;rprete en un hilo del pool y se bloquea en una cola "
     "(" + C("resumeSignal") + ") en cada pausa; mapea " + C("TraceStep") + " &rarr; posici&oacute;n en el editor.",
     "Toda la l&oacute;gica de breakpoints/step/variables est&aacute; atada al modelo del int&eacute;rprete; "
     "se reescribe sobre JDI."),
    ("Ejecuci&oacute;n (Run y Debug)",
     "In-process: " + C("BbkProcessHandler") + " &rarr; " + C("BbkRunner.compileAndRun") + " carga el bytecode "
     "con un classloader propio y corre " + C("bbk.Main") + " <b>dentro del mismo JVM del IDE</b>.",
     "Para JDI hay que <b>lanzar una JVM aparte</b>: el agente JDWP no se conecta a la VM que ya corre el IDE."),
    ("Info de debug en el bytecode",
     "<b>LineNumberTable ya emitido</b> (" + C("JvmCompiler.lineNumber()") + " desde el " + C("SourceMap") +
     "; test " + C("JvmDebugInfoTest") + " verde).",
     "Es la base y <b>ya est&aacute;</b>. Falta el resto de la info de debug (abajo)."),
    ("Mapa de almacenamiento",
     "Globales &rarr; campos " + C("static") + " de " + C("bbk.Main") + "; subcampos de DS &rarr; campos "
     + C("emp$salary") + " (aplanados); locales &rarr; <i>slots</i> an&oacute;nimos; INT&rarr;" + C("long") +
     ", decimal&rarr;" + C("BigDecimal") + ", CHAR&rarr;" + C("String") + ".",
     "JDI lee campos y slots, pero con <b>nombres y tipos de la JVM</b>: hay que revertirlos a la vista BBK."),
]))

# ===================== 2. MODELO OBJETIVO =====================
story.append(Paragraph("2 &nbsp; Modelo objetivo (c&oacute;mo funciona JDI)", H1))
story.append(Paragraph(
    "JDI (" + C("com.sun.jdi") + ", m&oacute;dulo " + C("jdk.jdi") + " del JDK) es el cliente est&aacute;ndar de "
    "debug de la JVM &mdash; el mismo que usa el debugger de Java de IntelliJ. El IDE no &laquo;mete mano&raquo; "
    "dentro del programa: lanza una JVM con el agente JDWP, se conecta por un socket y le pide al agente que ponga "
    "breakpoints, frene, lea variables y haga step. La ventaja t&eacute;cnica concreta es que el <b>step</b> deja "
    "de ser conteo de profundidad casero y pasa a " + C("StepRequest") + " nativo (INTO/OVER/OUT a nivel "
    "l&iacute;nea); los breakpoints, el call stack y las excepciones son los <b>reales</b> de la ejecuci&oacute;n.",
    BODY))

# ===================== 3. PASOS =====================
story.append(Paragraph("3 &nbsp; Pasos para acoplarlo", H1))
story.append(Paragraph(
    "Esta es la secuencia de cambios concretos, en orden, de principio a fin. Es <b>un solo trabajo</b>: el "
    "debugger no funciona hasta que est&aacute;n todos. El " + C("paso 1") + " ya est&aacute; hecho.", BODY))


def step_table(rows):
    data = [[Paragraph("<b>#</b>", CELL), Paragraph("<b>Paso</b>", CELL),
             Paragraph("<b>D&oacute;nde / c&oacute;mo</b>", CELL), Paragraph("<b>Verificable sin runIde?</b>", CELL)]]
    sr = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("BOX", (0, 0), (-1, -1), 0.5, LINE), ("LINEBELOW", (0, 0), (-1, -1), 0.4, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"), ("ALIGN", (0, 0), (0, -1), "CENTER"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    for i, (paso, how, ver, ok) in enumerate(rows, start=1):
        if ok == "done":
            badge = Paragraph('<font color="#1f8a4c"><b>&#10003; Ya hecho</b></font><br/>'
                              '<font size=7.5 color="#5b6472">%s</font>' % ver, CELL)
            bg = GREEN_BG
        elif ok == "yes":
            badge = Paragraph('<font color="#1f8a4c"><b>&#10003; S&iacute;</b></font><br/>'
                              '<font size=7.5 color="#5b6472">%s</font>' % ver, CELL)
            bg = GREEN_BG
        elif ok == "partial":
            badge = Paragraph('<font color="#b9770e"><b>&#9679; Parcial</b></font><br/>'
                              '<font size=7.5 color="#5b6472">%s</font>' % ver, CELL)
            bg = AMBER_BG
        else:
            badge = Paragraph('<font color="#9aa0ab"><b>&#9675; No</b></font><br/>'
                              '<font size=7.5 color="#5b6472">%s</font>' % ver, CELL)
            bg = colors.white
        data.append([Paragraph(str(i), NUMP), Paragraph(paso, CELL), Paragraph(how, CELL_MUTE), badge])
        sr.append(("BACKGROUND", (3, i), (3, i), bg))
    t = Table(data, colWidths=[8*mm, 44*mm, 76*mm, 46*mm], repeatRows=1)
    t.setStyle(TableStyle(sr))
    return t


story.append(step_table([
    ("N&uacute;meros de l&iacute;nea (LineNumberTable)",
     C("JvmCompiler.lineNumber()") + " ya los emite desde el " + C("SourceMap") + ".",
     "test ASM ya verde", "done"),
    ("Atributo SourceFile",
     C("cw.visitSource(\"Main.bbk\", null)") + " &mdash; hoy va " + C("null") + ". JDI necesita el nombre del fuente.",
     "ClassReader asserta el nombre", "yes"),
    ("LocalVariableTable",
     "Por cada local, " + C("mv.visitLocalVariable(nombre, desc, null, start, end, slot)") + ". Hoy los slots son "
     "an&oacute;nimos: el compilador conoce el nombre (la clave del mapa) pero lo descarta. Hay que llevar labels "
     "de rango vivo por m&eacute;todo.",
     "ClassReader asserta la tabla", "yes"),
    ("Escribir el .class a disco",
     "El bytecode de " + C("JvmCompiler") + " a un dir temporal (hoy vive en memoria, lo carga un classloader propio).",
     "test headless de escritura", "yes"),
    ("Forkear la JVM con el agente JDWP",
     C("java -agentlib:jdwp=...,server=y,suspend=y -cp tmp bbk.Main") + ". Reemplaza la corrida in-process "
     "<b>solo en modo Debug</b> (Run sigue in-process).",
     "se prueba por l&iacute;nea de comandos", "yes"),
    ("Conectar por JDI",
     "Nuevo m&oacute;dulo headless " + C("bbk-jvm-debug") + ": " + C("AttachingConnector") + " al socket + "
     + C("ClassPrepareRequest") + " de " + C("bbk.Main") + ".",
     "JUnit: attach + carga", "yes"),
    ("Mapear l&iacute;nea &harr; BBK",
     "Reverso del LineNumberTable: " + C("Location.lineNumber()") + " &harr; (archivo, l&iacute;nea) BBK, para "
     "instalar breakpoints y ubicar las pausas.",
     "JUnit sobre la tabla", "yes"),
    ("Instalar breakpoints",
     C("BreakpointRequest") + " diferidos hasta el ClassPrepare, en las " + C("Location") + " de las l&iacute;neas "
     "mapeadas.",
     "JUnit: frena donde toca", "yes"),
    ("Event loop",
     "Hilo que consume " + C("EventSet") + "s (breakpoint, step, muerte de la VM) y los traduce a "
     + C("positionReached") + ".",
     "JUnit: secuencia de eventos", "yes"),
    ("Step nativo",
     C("StepRequest") + " INTO/OVER/OUT a nivel l&iacute;nea &mdash; reemplaza el conteo de profundidad del "
     "int&eacute;rprete.",
     "JUnit: pasos esperados", "yes"),
    ("Leer frames y variables",
     C("ThreadReference.frames()") + ", " + C("StackFrame.visibleVariables()") + " y los campos " + C("static") +
     " de la clase.",
     "JUnit: valores le&iacute;dos", "yes"),
    ("Mapear nombres y valores JVM &rarr; BBK",
     C("emp$salary") + " &rarr; " + C("emp.salary") + "; reconstruir el DS como &aacute;rbol; formatear "
     + C("long") + "/" + C("BigDecimal") + "/" + C("String") + "/" + C("boolean") + " estilo BBK.",
     "l&oacute;gica testeable; la vista, runIde", "partial"),
    ("Evaluador de expresiones",
     "Evaluate / breakpoints condicionales / watches: reusar el parser de expresiones BBK y un mini-evaluador "
     "alimentado con los valores le&iacute;dos del frame JDI.",
     "JUnit del evaluador", "yes"),
    ("Reescribir BbkDebugProcess sobre JDI",
     "El " + C("XDebugProcess") + " del plugin pasa a orquestar la sesi&oacute;n " + C("bbk-jvm-debug") + ": "
     "breakpoints, frames, variables y step contra eventos JDI en vez del int&eacute;rprete.",
     "solo runIde", "no"),
    ("Pipe de salida + ciclo de vida del proceso",
     "Volcar stdout/stderr del proceso a la consola y manejar fin/kill (puerto JDWP, timeout, hu&eacute;rfanos "
     "&mdash; el problema que ya tuvimos con " + C("bootRun") + " en :8080).",
     "parte headless, parte runIde", "partial"),
    ("Cross-file: SMAP (JSR-45)",
     "Todo compila a un solo " + C("bbk.Main") + " con un SourceFile. Para depurar varios " + C(".bbk") +
     " hay que emitir " + C("SourceDebugExtension") + " (lo que hace Kotlin/JSP) o arrancar single-file. "
     "<b>Decisi&oacute;n a tomar de entrada</b> porque cambia el formato de debug del compilador.",
     "test del SMAP emitido", "partial"),
]))

# ===================== 4. CONSERVA / RETIRA =====================
story.append(Paragraph("4 &nbsp; Qu&eacute; se conserva y qu&eacute; se retira", H1))
keep = [[Paragraph("<b>Se conserva</b>", CELL), Paragraph("<b>Se retira / reemplaza</b>", CELL)]]
keep.append([
    Paragraph("&bull; Toda la costura XDebugger del plugin (breakpoint type, editors provider, runner, consola).<br/>"
              "&bull; El parser de expresiones BBK (lo reusa el evaluador).<br/>"
              "&bull; El " + C("LineNumberTable") + " ya emitido.<br/>"
              "&bull; El " + C("BbkRunner") + " in-process para el modo <b>Run</b> (no-debug).", CELL_MUTE),
    Paragraph("&bull; El " + C("Interpreter") + " como <b>motor de ejecuci&oacute;n</b> del debug "
              "(deja de existir el &laquo;tercer backend&raquo;).<br/>"
              "&bull; El modelo " + C("DebugListener") + "/" + C("TraceStep") + "/" + C("resumeSignal") +
              " (lo reemplazan los eventos JDI).<br/>"
              "&bull; El " + C("BbkDebugProcess") + " actual (se reescribe).", CELL_MUTE),
])
kt = Table(keep, colWidths=[87*mm, 87*mm])
kt.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (0, 0), GREEN_BG), ("BACKGROUND", (1, 0), (1, 0), AMBER_BG),
    ("BOX", (0, 0), (-1, -1), 0.5, LINE), ("INNERGRID", (0, 0), (-1, -1), 0.5, colors.white),
    ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7),
    ("TOPPADDING", (0, 0), (-1, -1), 7), ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
]))
story.append(kt)
story.append(Paragraph(
    "El " + C("Interpreter") + " podr&iacute;a sobrevivir aparte como motor del comando <b>Trace BBK</b> "
    "(traza est&aacute;tica sin proceso), pero ya no como el debugger. Esa es una decisi&oacute;n a tomar.", BODY))

# ===================== 5. RIESGOS / DECISIONES =====================
story.append(Paragraph("5 &nbsp; Decisiones abiertas y riesgos", H1))


def risk_table(rows):
    data = [[Paragraph("<b>Tema</b>", CELL), Paragraph("<b>El punto</b>", CELL)]]
    sr = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("BOX", (0, 0), (-1, -1), 0.5, LINE), ("LINEBELOW", (0, 0), (-1, -1), 0.4, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    for k, v in rows:
        data.append([Paragraph(k, CELL), Paragraph(v, CELL_MUTE)])
    t = Table(data, colWidths=[42*mm, 132*mm], repeatRows=1)
    t.setStyle(TableStyle(sr))
    return t


story.append(risk_table([
    ("Cross-file (multi-archivo)",
     "El int&eacute;rprete junta todos los " + C(".bbk") + " de la carpeta y depura cualquiera. Con un solo "
     + C("bbk.Main") + ", JDI necesita <b>SMAP/JSR-45</b> para mapear varias fuentes a una clase. Conviene "
     "decidir de entrada: implementar SMAP, o empezar por single-file y sumarlo despu&eacute;s."),
    ("Proceso externo",
     "Pasamos de in-process a <b>forkear una JVM</b>: hay que resolver el binario " + C("java") + ", puerto JDWP, "
     "stdout, y matar hu&eacute;rfanos (el mismo tipo de problema que ya tuvimos con " + C("bootRun") + " en :8080)."),
    ("Granularidad del step",
     "El int&eacute;rprete frena limpio por sentencia. El bytecode frena por l&iacute;nea del LineNumberTable: "
     "expresiones compuestas o control de flujo pueden dar saltos menos &laquo;prolijos&raquo;. Aceptable, pero "
     "distinto a lo de hoy."),
    ("Evaluaci&oacute;n de expresiones",
     "JDI puede " + C("invokeMethod") + ", pero para expresiones BBK arbitrarias conviene evaluar <b>del lado del "
     "plugin</b> (parser BBK + valores le&iacute;dos por JDI), no compilar cada expresi&oacute;n. M&aacute;s simple "
     "y suficiente para watches/condicionales."),
    ("Dependencia jdk.jdi",
     C("com.sun.jdi") + " viene en el JDK pero hay que ponerlo en el module path del nuevo m&oacute;dulo / plugin. "
     "Sin misterio, pero es wiring que hoy no existe."),
]))

# ===================== 6. VERIFICACIÓN =====================
story.append(Paragraph("6 &nbsp; Verificaci&oacute;n", H1))
story.append(Paragraph(
    "Buena parte es <b>testeable headless</b> (sin abrir el IDE), por eso conviene meter la sesi&oacute;n JDI en un "
    "m&oacute;dulo aparte " + C("bbk-jvm-debug") + ", igual que " + C("bbk-debugger") + " es UI-agn&oacute;stico:", BODY))
story.append(Paragraph(
    "&bull; <b>Headless (JUnit):</b> que el compilador emita SourceFile + LocalVariableTable (+ SMAP) v&iacute;a "
    + C("ClassReader") + "; y una <b>sesi&oacute;n JDI m&iacute;nima</b> &mdash; lanzar un programita, poner un "
    "breakpoint, verificar que frena y leer una variable. JDI corre sin GUI &mdash; son los pasos 1 a 13.<br/>"
    "&bull; <b>Solo runIde (lo verific&aacute;s vos):</b> el paso 14 (el " + C("BbkDebugProcess") + " nuevo, el "
    "panel de Variables, el step con los botones, Evaluate). Esa parte <b>no la puedo verificar yo</b> &mdash; es "
    "la misma dependencia de siempre.", BODY))

# ===================== 7. RECOMENDACIÓN =====================
story.append(Paragraph("7 &nbsp; Recomendaci&oacute;n", H1))
story.append(Paragraph(
    "S&iacute; vale acoplarlo al bytecode: es lo correcto (se depura lo que se ejecuta) y mata el tercer motor. "
    "El camino est&aacute;ndar es <b>JDI/JDWP con la sesi&oacute;n en un m&oacute;dulo headless</b> "
    "(" + C("bbk-jvm-debug") + ") que el plugin orquesta &mdash; as&iacute; el grueso queda <b>cubierto por tests</b> "
    "y solo la c&aacute;scara visual depende de runIde. Sobre cross-file conviene <b>decidir de entrada</b> si "
    "entra SMAP (multi-archivo) o arrancamos single-file, porque condiciona el formato de debug del compilador.", BODY))
story.append(Paragraph(
    "Es un cambio de arquitectura, no un parche: la secuencia reci&eacute;n da un debugger funcional cuando "
    "est&aacute;n todos los pasos. Por eso necesita ir <b>completo</b>, con tu verificaci&oacute;n en runIde para "
    "el paso final de UI &mdash; no a medias.", BODY))

story.append(Spacer(1, 10))
story.append(HRFlowable(width="100%", thickness=0.6, color=LINE, spaceAfter=4))
story.append(Paragraph(
    "An&aacute;lisis grounded en el c&oacute;digo actual: " + C("JvmCompiler") + ", " + C("BbkRunner") + ", "
    + C("BbkDebugProcess") + ", " + C("BbkProcessHandler") + " y el m&oacute;dulo " + C("bbk-debugger") + ". "
    "Leyenda de &laquo;verificable&raquo;: <font color='#1f8a4c'><b>&#10003;</b></font> headless &nbsp; "
    "<font color='#b9770e'><b>&#9679; Parcial</b></font> &nbsp; "
    "<font color='#9aa0ab'><b>&#9675; No</b></font> (solo runIde).", SMALL))


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(GREY_TX)
    canvas.drawString(20*mm, 12*mm, "BoxBreaker — Debugger sobre bytecode real")
    canvas.drawRightString(190*mm, 12*mm, "Página %d" % doc.page)
    canvas.setStrokeColor(LINE)
    canvas.line(20*mm, 15*mm, 190*mm, 15*mm)
    canvas.restoreState()


doc = SimpleDocTemplate(
    OUT, pagesize=A4,
    leftMargin=20*mm, rightMargin=20*mm, topMargin=18*mm, bottomMargin=20*mm,
    title="BoxBreaker - Debugger sobre bytecode real", author="rpg-bbk-compiler",
)
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print("OK ->", OUT)
