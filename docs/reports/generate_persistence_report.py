# -*- coding: utf-8 -*-
"""Genera el informe de estado de la PERSISTENCIA de archivos de BoxBreaker (qué cubre hoy y qué falta)."""

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
)

OUT = "BoxBreaker-persistencia-pendientes.pdf"

# ----- Paleta -----
NAVY = colors.HexColor("#1f2a44")
ACCENT = colors.HexColor("#2f6fed")
GREEN = colors.HexColor("#1f8a4c")
GREEN_BG = colors.HexColor("#e7f4ec")
AMBER = colors.HexColor("#b9770e")
AMBER_BG = colors.HexColor("#fdf3e2")
RED = colors.HexColor("#b3261e")
RED_BG = colors.HexColor("#fbe9e7")
GREY_BG = colors.HexColor("#f1f3f7")
GREY_TX = colors.HexColor("#5b6472")
LINE = colors.HexColor("#d4d9e2")

styles = getSampleStyleSheet()


def style(name, **kw):
    base = kw.pop("parent", styles["Normal"])
    return ParagraphStyle(name, parent=base, **kw)


H_TITLE = style("HTitle", parent=styles["Title"], fontSize=22, textColor=NAVY, spaceAfter=2, leading=26)
H_SUB = style("HSub", fontSize=11, textColor=GREY_TX, spaceAfter=2, leading=15)
H1 = style("H1", fontSize=15, textColor=NAVY, spaceBefore=16, spaceAfter=6, leading=18, fontName="Helvetica-Bold")
BODY = style("Body", fontSize=9.5, textColor=colors.HexColor("#222831"), leading=14, spaceAfter=4)
CELL = style("Cell", fontSize=9, leading=12.5, textColor=colors.HexColor("#222831"))
CELL_MUTE = style("CellMute", fontSize=9, leading=12.5, textColor=GREY_TX)
SMALL = style("Small", fontSize=8, textColor=GREY_TX, leading=11)

story = []

# ===================== PORTADA =====================
story.append(Paragraph("BoxBreaker — Persistencia de archivos", H_TITLE))
story.append(Paragraph("Capa de acceso a datos &nbsp;|&nbsp; qué cubre hoy y qué falta para persistencia RPG completa &nbsp;|&nbsp; 20 de junio de 2026", H_SUB))
story.append(HRFlowable(width="100%", thickness=2, color=ACCENT, spaceBefore=6, spaceAfter=10))

# ===================== RESUMEN =====================
story.append(Paragraph("Resumen", H1))
story.append(Paragraph(
    "La capa de acceso a datos es una <b>versión inicial funcional</b>, no persistencia RPG completa. "
    "Un archivo declarado <b>es</b> una tabla física real (con RRN) y el ciclo CRUD por RRN "
    "(WRITE/READ/CHAIN/UPDATE/DELETE) anda de punta a punta, más SQL interactivo (STRSQL). "
    "Es un esqueleto vertical que prueba el modelo. Pero le falta lo que más define a un archivo de IBM i: "
    "<b>acceso por clave</b>, el <b>modelo stateful de open/cursor</b>, <b>bloqueo de registro</b> y "
    "<b>commitment control</b> — más una lista de items incrementales (tipos y reglas de DDS, constraints, "
    "marshaling type-fiel). En una frase: tenemos el <b>almacenamiento + el read/write crudo por RRN</b>; "
    "falta el <b>método de acceso</b> que RPG realmente consume.", BODY))

# Tarjetas
summary = [[
    Paragraph('<b>Almacenamiento + CRUD</b><br/><font size=8 color="#1f8a4c">FUNCIONAL</font><br/><font size=7 color="#5b6472">PF&rarr;tabla &middot; RRN CRUD &middot; STRSQL</font>', CELL),
    Paragraph('<b>Acceso por clave</b><br/><font size=8 color="#b3261e">FALTA</font><br/><font size=7 color="#5b6472">CHAIN/SETLL/READE &middot; LF</font>', CELL),
    Paragraph('<b>Open/cursor + locking</b><br/><font size=8 color="#b3261e">FALTA</font><br/><font size=7 color="#5b6472">ODP por job &middot; commit</font>', CELL),
    Paragraph('<b>Tipos / constraints</b><br/><font size=8 color="#b9770e">PARCIAL</font><br/><font size=7 color="#5b6472">4 tipos &middot; sin reglas DDS</font>', CELL),
]]
st = Table(summary, colWidths=[42.5 * mm, 42.5 * mm, 42.5 * mm, 42.5 * mm])
st.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (0, 0), GREEN_BG),
    ("BACKGROUND", (1, 0), (1, 0), RED_BG),
    ("BACKGROUND", (2, 0), (2, 0), RED_BG),
    ("BACKGROUND", (3, 0), (3, 0), AMBER_BG),
    ("BOX", (0, 0), (-1, -1), 0.5, LINE),
    ("INNERGRID", (0, 0), (-1, -1), 0.5, colors.white),
    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ("LEFTPADDING", (0, 0), (-1, -1), 7),
    ("RIGHTPADDING", (0, 0), (-1, -1), 7),
    ("TOPPADDING", (0, 0), (-1, -1), 8),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
]))
story.append(Spacer(1, 4))
story.append(st)


# ===================== HELPERS DE TABLA =====================
def covered_table(rows):
    data = [[Paragraph("<b>Capacidad</b>", CELL), Paragraph("<b>Estado</b>", CELL), Paragraph("<b>Detalle</b>", CELL)]]
    srows = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("BOX", (0, 0), (-1, -1), 0.5, LINE),
        ("LINEBELOW", (0, 0), (-1, -1), 0.4, LINE),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    for i, (cap, detail) in enumerate(rows, start=1):
        badge = Paragraph('<font color="#1f8a4c"><b>&#10003; Hecho</b></font>', CELL)
        data.append([Paragraph(cap, CELL), badge, Paragraph(detail, CELL_MUTE)])
        srows.append(("BACKGROUND", (1, i), (1, i), GREEN_BG))
    t = Table(data, colWidths=[52 * mm, 26 * mm, 96 * mm], repeatRows=1)
    t.setStyle(TableStyle(srows))
    return t


def gap_table(rows):
    data = [[Paragraph("<b>Lo que falta</b>", CELL), Paragraph("<b>Impacto</b>", CELL), Paragraph("<b>Por qué importa</b>", CELL)]]
    srows = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("BOX", (0, 0), (-1, -1), 0.5, LINE),
        ("LINEBELOW", (0, 0), (-1, -1), 0.4, LINE),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    impact_map = {
        "alto": ('<font color="#b3261e"><b>&#9679; Alto</b></font>', RED_BG),
        "medio": ('<font color="#b9770e"><b>&#9679; Medio</b></font>', AMBER_BG),
        "bajo": ('<font color="#5b6472"><b>&#9675; Bajo</b></font>', GREY_BG),
    }
    for i, (cap, impact, detail) in enumerate(rows, start=1):
        txt, bg = impact_map[impact]
        data.append([Paragraph(cap, CELL), Paragraph(txt, CELL), Paragraph(detail, CELL_MUTE)])
        srows.append(("BACKGROUND", (1, i), (1, i), bg))
    t = Table(data, colWidths=[52 * mm, 24 * mm, 98 * mm], repeatRows=1)
    t.setStyle(TableStyle(srows))
    return t


# ===================== 1. LO QUE YA CUBRE =====================
story.append(Paragraph("1 &nbsp; Lo que ya cubre (verificado end-to-end)", H1))
covered = [
    ("Declarar archivo (CRTPF)", "El PF declarado crea una tabla física real con RRN (clave) + las columnas del record format"),
    ("Cambiar estructura (CHGPF)", "ALTER TABLE: DROP/ADD/MODIFY de columnas, en un único ALTER atómico"),
    ("WRITE / READ / CHAIN", "INSERT (devuelve RRN), SELECT secuencial (orden RRN), lectura aleatoria por RRN"),
    ("UPDATE / DELETE", "Por RRN; el UPDATE parcial respeta los campos no enviados"),
    ("SQL interactivo (STRSQL)", "Cualquier sentencia directa contra la base; result set o filas afectadas"),
    ("Tipos de campo", "CHAR, DECIMAL (exacto), INTEGER, DATE &rarr; columna SQL"),
    ("Seguridad del acceso", "Sólo columnas del record format; valores siempre como parámetros JDBC (sin inyección)"),
]
story.append(covered_table(covered))
story.append(Paragraph(
    "Es una base real y funcional: el ciclo <b>declarar &rarr; ver/cambiar estructura &rarr; consultar (SQL) &rarr; "
    "leer/escribir registros</b> está cerrado y probado. Lo que sigue es lo que separa esto de \"persistencia RPG\".", BODY))

# ===================== 2. FALTA — FUNDAMENTAL =====================
story.append(Paragraph("2 &nbsp; Falta — fundamental (cambia el modelo, no es un agregado)", H1))
fundamental = [
    ("Acceso por clave / access paths", "alto",
     "El RPG real casi no usa RRN: usa claves. CHAIN por campo clave, SETLL/SETGT/READE/READP que posicionan y leen "
     "en orden de clave. Hoy sólo hay CHAIN por RRN. Sin esto no es acceso RPG"),
    ("Logical files (LF)", "alto",
     "Órdenes de acceso alternativos, selección/omisión de registros y join logical files sobre los physical files"),
    ("Modelo stateful open/cursor (ODP)", "alto",
     "OPEN/CLOSE con posición actual por job: READ avanza, READE lee el siguiente-igual. Hoy las operaciones son "
     "sin estado. Es, además, la ABI stateful que diseñamos para v2"),
    ("Bloqueo de registro (locking)", "alto",
     "El UPDATE de RPG lee con lock y libera al actualizar. Hoy no hay locks &rarr; updates concurrentes harían carrera"),
    ("Commitment control / transacciones", "alto",
     "COMMIT/ROLBK y journaling. Hoy cada operación auto-commitea; no hay límites transaccionales multi-operación"),
]
story.append(gap_table(fundamental))

# ===================== 3. FALTA — INCREMENTAL =====================
story.append(Paragraph("3 &nbsp; Falta — incremental (importante, pero se suma de a poco)", H1))
incremental = [
    ("Tipos y reglas de DDS", "medio",
     "Packed vs zoned, varchar, time/timestamp con formatos, campos null-capable (ALWNULL) y valores por defecto (DFT)"),
    ("Constraints", "medio",
     "Claves UNIQUE, clave primaria de negocio, foreign keys, validación CHECK/RANGE/VALUES/COMP"),
    ("Marshaling type-fiel", "medio",
     "Los decimales entran como Double (riesgo de precisión float); deberían viajar como BigDecimal. Hoy lo tapa que "
     "MySQL redondea a la escala"),
    ("DDL transaccional", "medio",
     "CREATE/ALTER hacen commit implícito en MySQL &rarr; posible tabla huérfana ante un fallo raro (mitigado validando antes)"),
    ("Presentación de campos", "bajo",
     "Edit codes (EDTCDE/EDTWRD), column headings, campos por referencia (REFFLD)"),
]
story.append(gap_table(incremental))

# ===================== 4. FALTA — BORDE =====================
story.append(Paragraph("4 &nbsp; Falta — borde / más adelante", H1))
borde = [
    ("Members", "bajo", "Un PF puede tener varios members; OVRDBF para redirigir archivo/member en runtime"),
    ("Multi-format / join", "bajo", "Archivos con varios formatos de registro; join logical files de varios PF"),
    ("Triggers e índices extra", "bajo", "Triggers de base de datos e índices de performance más allá de la clave"),
]
story.append(gap_table(borde))

# ===================== BOTTOM LINE =====================
story.append(Paragraph("5 &nbsp; Bottom line y próximo paso", H1))
story.append(Paragraph(
    "Lo construido es el <b>disco + el read/write crudo por RRN</b>: sólido y verificado, pero es el primitivo de "
    "almacenamiento. Lo que lo convierte en \"persistencia RPG\" de verdad es la capa de arriba — el <b>método de "
    "acceso</b>: acceso por clave, el modelo stateful de open/cursor, locking y transacciones. Esos cuatro (Impacto "
    "alto) son los que más definen \"esto es un archivo de IBM i\".", BODY))
story.append(Paragraph(
    "<b>Próximo paso de mayor impacto:</b> el <b>acceso por clave</b> (definir campos clave en el archivo + "
    "CHAIN/SETLL/READE por clave) — es lo que desbloquea correr programas RPG reales sobre estos archivos. El modelo "
    "stateful open/cursor va de la mano y es, además, la ABI de datos de la versión v2 (programa corriendo adentro "
    "del runtime).", BODY))

story.append(Spacer(1, 10))
story.append(HRFlowable(width="100%", thickness=0.6, color=LINE, spaceAfter=4))
story.append(Paragraph(
    "Generado a partir del estado del repositorio. Impacto: "
    "<font color='#b3261e'><b>&#9679; Alto</b></font> &nbsp; "
    "<font color='#b9770e'><b>&#9679; Medio</b></font> &nbsp; "
    "<font color='#5b6472'><b>&#9675; Bajo</b></font>.", SMALL))


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(GREY_TX)
    canvas.drawString(20 * mm, 12 * mm, "BoxBreaker — Persistencia de archivos")
    canvas.drawRightString(190 * mm, 12 * mm, "Página %d" % doc.page)
    canvas.setStrokeColor(LINE)
    canvas.line(20 * mm, 15 * mm, 190 * mm, 15 * mm)
    canvas.restoreState()


doc = SimpleDocTemplate(
    OUT, pagesize=A4,
    leftMargin=20 * mm, rightMargin=20 * mm, topMargin=18 * mm, bottomMargin=20 * mm,
    title="BoxBreaker - Persistencia de archivos: estado y pendientes", author="rpg-bbk-compiler",
)
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print("OK ->", OUT)
