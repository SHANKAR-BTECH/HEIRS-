"""Reusable PDF renderer for HEIRS synthetic demo documents.

Builds compact, readable A4 PDFs with a shared document shell:

  - HEIRS project label + synthetic-content disclaimer (mandatory)
  - record metadata block (record id, reference, department, year, status)
  - body blocks: headings, paragraphs, bullets, numbered lists, tables
  - page-number footer

Fonts use the PDF standard font set (Times / Helvetica) to keep every
file small (roughly 5-10 KB) while preserving typographic hierarchy.
Filenames and content come from the caller (content_engine).
"""

import os
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (
    HRFlowable,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

PAGE_W, PAGE_H = A4
MARGIN = 20 * mm          # ~20 mm margins
CONTENT_W = PAGE_W - 2 * MARGIN

ACCENT = colors.HexColor("#34495E")
LIGHT_ROW = colors.HexColor("#F7F7F7")
GRID_COLOR = colors.HexColor("#AAAAAA")

# Standard PDF fonts: compact, embeddable-free, selectable text stays clean.
F_SERIF = "Times-Roman"
F_SERIF_BOLD = "Times-Bold"
F_SERIF_ITALIC = "Times-Italic"
F_SANS = "Helvetica"
F_SANS_BOLD = "Helvetica-Bold"

S = {}
S["title"] = ParagraphStyle(
    "title", fontName=F_SERIF_BOLD, fontSize=19, leading=26,
    textColor=colors.HexColor("#1a1a1a"), spaceBefore=4, spaceAfter=4
)
S["headerLabel"] = ParagraphStyle(
    "hLabel", fontName=F_SANS, fontSize=9.5, leading=13,
    textColor=colors.HexColor("#555555"), spaceBefore=1, spaceAfter=1
)
S["heading"] = ParagraphStyle(
    "heading", fontName=F_SERIF_BOLD, fontSize=14, leading=20,
    spaceBefore=18, spaceAfter=8, textColor=colors.HexColor("#1a1a1a"),
    keepWithNext=True
)
S["subheading"] = ParagraphStyle(
    "subheading", fontName=F_SERIF_BOLD, fontSize=12, leading=17,
    spaceBefore=14, spaceAfter=6, textColor=colors.HexColor("#222222"),
    keepWithNext=True
)
S["body"] = ParagraphStyle(
    "body", fontName=F_SERIF, fontSize=11, leading=15.5,
    alignment=TA_JUSTIFY, spaceBefore=0, spaceAfter=6
)
S["bullet"] = ParagraphStyle(
    "bullet", fontName=F_SERIF, fontSize=11, leading=15.5,
    alignment=TA_JUSTIFY, leftIndent=18, firstLineIndent=-15,
    spaceBefore=0, spaceAfter=4
)
S["numbered"] = ParagraphStyle(
    "num", fontName=F_SERIF, fontSize=11, leading=15.5,
    alignment=TA_JUSTIFY, leftIndent=18, firstLineIndent=-15,
    spaceBefore=0, spaceAfter=4
)
S["intro"] = ParagraphStyle(
    "intro", fontName=F_SERIF, fontSize=11, leading=15.5,
    alignment=TA_JUSTIFY, spaceBefore=0, spaceAfter=10,
    textColor=colors.HexColor("#333333")
)
S["hcell"] = ParagraphStyle(
    "hcell", fontName=F_SANS_BOLD, fontSize=9.5, leading=13,
    textColor=colors.white, alignment=TA_CENTER
)
S["cell"] = ParagraphStyle("cell", fontName=F_SERIF, fontSize=10, leading=13)


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 9)
    canvas.setFillColor(colors.HexColor("#666666"))
    canvas.drawCentredString(PAGE_W / 2, 15 * mm, f"Page {canvas.getPageNumber()}")
    canvas.restoreState()


def make_table(headers, rows, col_widths=None):
    hdr = [Paragraph(escape(h), S["hcell"]) for h in headers]
    data = [hdr]
    for row in rows:
        data.append(
            [Paragraph(escape(str(c)).replace("\n", "<br/>"), S["cell"]) for c in row]
        )
    t = Table(data, colWidths=col_widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), ACCENT),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("ALIGN", (0, 0), (-1, 0), "CENTER"),
        ("ALIGN", (0, 1), (-1, -1), "LEFT"),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.5, GRID_COLOR),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT_ROW]),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
    ]))
    return t


def make_meta_table(doc):
    meta = [
        ["Document Type", doc["doc_type"], "Reference Record", doc["record_id"]],
        ["Department", doc.get("department", "Higher Education Department"),
         "Reference", doc["reference"]],
        ["Year", str(doc["year"]), "Status", doc.get("status", "Active")],
    ]
    t = Table(meta, colWidths=[90, 150, 90, 145])
    t.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (0, -1), F_SANS_BOLD),
        ("FONTNAME", (2, 0), (2, -1), F_SANS_BOLD),
        ("FONTSIZE", (0, 0), (-1, -1), 9.5),
        ("TEXTCOLOR", (0, 0), (-1, -1), colors.HexColor("#333333")),
        ("TOPPADDING", (0, 0), (-1, -1), 2),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 2),
        ("LEFTPADDING", (0, 0), (-1, -1), 0),
    ]))
    return t


DISCLAIMER_TEXT = (
    "<b>Demo / Synthetic Document:</b> This document was created solely for "
    "demonstration of the HEIRS project. It is not an official government order, "
    "regulation, policy, scheme, or legal document. All content is synthetic and "
    "presented only for project presentation purposes."
)


def add_header(story, doc):
    story.append(Paragraph(
        "HEIRS \u2014 HIGHER EDUCATION INFORMATION RETRIEVAL SYSTEM", S["headerLabel"]))
    story.append(Paragraph(
        "DEMO DOCUMENT \u2014 SYNTHETIC CONTENT FOR PROJECT PRESENTATION",
        S["headerLabel"]))
    story.append(Spacer(1, 10))
    story.append(Paragraph(escape(doc["title"]), S["title"]))
    story.append(Spacer(1, 6))
    story.append(make_meta_table(doc))
    story.append(Spacer(1, 8))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#888888")))
    story.append(Spacer(1, 8))
    disc_para = Paragraph(
        DISCLAIMER_TEXT,
        ParagraphStyle("discBody", fontName=F_SANS, fontSize=9.5,
                       leading=13, alignment=TA_JUSTIFY))
    disc_table = Table([[disc_para]], colWidths=[CONTENT_W])
    disc_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F5F5F5")),
        ("BOX", (0, 0), (-1, -1), 0.5, colors.HexColor("#CCCCCC")),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
        ("LEFTPADDING", (0, 0), (-1, -1), 10),
        ("RIGHTPADDING", (0, 0), (-1, -1), 10),
    ]))
    story.append(disc_table)
    story.append(Spacer(1, 14))


def add_body(story, blocks):
    for block in blocks:
        t = block.get("type")
        if t == "heading":
            story.append(Paragraph(escape(block["text"]), S["heading"]))
        elif t == "subheading":
            story.append(Paragraph(escape(block["text"]), S["subheading"]))
        elif t == "paragraphs":
            for p in block["items"]:
                story.append(Paragraph(escape(p), S["body"]))
        elif t == "intro":
            for p in block["items"]:
                story.append(Paragraph(escape(p), S["intro"]))
        elif t == "bullets":
            for item in block["items"]:
                story.append(Paragraph(f"\u2022\u00a0\u00a0{escape(item)}", S["bullet"]))
        elif t == "numbered":
            for i, item in enumerate(block["items"], 1):
                story.append(Paragraph(f"{i}.\u00a0\u00a0{escape(item)}", S["numbered"]))
        elif t == "table":
            story.append(Spacer(1, 6))
            story.append(make_table(block["headers"], block["rows"],
                                    block.get("col_widths")))
            story.append(Spacer(1, 12))
        elif t == "spacer":
            story.append(Spacer(1, block.get("height", 12)))
        elif t == "pagebreak":
            story.append(PageBreak())


def render(doc_spec, out_dir):
    """Render a document spec to PDF. Returns the output file path (str)."""
    os.makedirs(out_dir, exist_ok=True)
    story = []
    add_header(story, doc_spec)
    add_body(story, doc_spec["body"])
    out = os.path.join(out_dir, doc_spec["filename"])
    pdf = SimpleDocTemplate(
        out,
        pagesize=A4,
        leftMargin=MARGIN,
        rightMargin=MARGIN,
        topMargin=MARGIN,
        bottomMargin=16 * mm,
        title=doc_spec["title"],
        author="HEIRS Demo",
    )
    pdf.build(story, onFirstPage=footer, onLaterPages=footer)
    return out