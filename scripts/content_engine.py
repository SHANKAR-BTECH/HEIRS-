"""Template-based content engine for HEIRS synthetic demo documents.

Given a record dict and a document-type definition, builds a document spec
(a title, metadata fields and a list of body blocks) that pdf_renderer
turns into a PDF.  All content is derived deterministically from the record:

  - title, description, category, department, year, reference, keywords

No randomness, no built-in hash(): every choice uses stable_index() (sha256),
so the same record always produces the same documents.

Content is assembled from role-based section templates.  Each role has a
small pool of heading options and paragraph options; the record's keywords
and description are substituted into the chosen templates, which keeps body
text substantive and topic-specific without repeating full paragraphs across
documents or records.
"""

import hashlib
from datetime import date, timedelta

# ---------------------------------------------------------------------------
# Deterministic selection
# ---------------------------------------------------------------------------

def stable_index(value, modulo):
    """Stable integer in [0, modulo) derived from a string via sha256."""
    digest = hashlib.sha256(value.encode("utf-8")).hexdigest()
    return int(digest[:8], 16) % modulo


def doc_count(record_id):
    """2 for ~70% of records, 3 for ~30%.  Deterministic per record ID."""
    return 3 if stable_index(record_id + "|count", 10) < 3 else 2


# ---------------------------------------------------------------------------
# Context helpers
# ---------------------------------------------------------------------------

CATEGORY_TERM = {
    "Policy": "policy",
    "Scheme": "scheme",
    "Regulation": "regulation",
    "Project": "project",
    "Rules": "rules",
}


def first_sentence(text, limit=220):
    text = " ".join(str(text or "").split())
    if len(text) <= limit:
        return text
    cut = text[:limit].rfind(" ")
    return text[:cut if cut > 0 else limit].rstrip(" ,;.") + "."


def build_context(record, doc_type_name, category):
    keywords = [k for k in (record.get("keywords") or []) if isinstance(k, str) and k.strip()]
    kw = (keywords + ["institutional practice", "higher education",
                      "stakeholder engagement", "sector development"])[:4]
    descr = record.get("description") or ""
    cat_term = CATEGORY_TERM.get(category, category.lower())
    return {
        "title": record["title"],
        "title_lower": record["title"],
        "descr": first_sentence(descr),
        "dept": record.get("department") or "Higher Education Department",
        "year": str(record.get("publicationYear") or 2024),
        "ref": record.get("referenceNumber") or record["id"],
        "category": category,
        "category_lower": category.lower(),
        "cat_term": cat_term,
        "Cat": cat_term.capitalize(),
        "doc_type": doc_type_name,
        "doc_type_lower": doc_type_name.lower(),
        "kw0": kw[0],
        "kw1": kw[1],
        "kw2": kw[2],
        "kw3": kw[3],
        "kwlist": ", ".join(kw[:3]),
        "kwlist2": ", ".join(kw[:2]),
        "kwand": " and ".join(kw[:3]),
        "kwand2": " and ".join(kw[:2]),
    }


# ---------------------------------------------------------------------------
# Role templates: heading options + paragraph pools
# ---------------------------------------------------------------------------

ROLES = {
    # ----- primary document roles -----
    "background": {
        "headings": ["Background and Rationale", "Context and Background",
                     "Background and Scope"],
        "paras": [
            "The {category_lower} landscape has evolved considerably in recent years, "
            "and institutions under {dept} have responded with renewed attention to "
            "{kwlist}. This {doc_type_lower} provides the framework needed to guide "
            "consistent practice across the sector.",
            "Stakeholders across {dept} have emphasised the importance of {kw0} as a "
            "foundation for credible, well-governed higher education. The present "
            "{doc_type_lower} responds to that expectation by setting out clear, "
            "practical direction.",
            "Following sustained engagement with universities, colleges and sector "
            "bodies, {dept} has prepared this {doc_type_lower} to give effect to "
            "{kwlist2}. It builds on earlier guidance while updating expectations to "
            "reflect current conditions.",
        ],
    },
    "purpose": {
        "headings": ["Purpose", "Purpose and Intent", "Object and Purpose"],
        "paras": [
            "The purpose of this document is to establish a consistent basis for "
            "{kwlist2} across {dept}-affiliated institutions, to clarify "
            "responsibilities, and to define the outcomes expected of every "
            "stakeholder.",
            "This {doc_type_lower} is intended to translate the objectives of "
            "{title} into actionable direction for institutions, officials and other "
            "stakeholders, ensuring that {kw0} is pursued in a structured and "
            "accountable manner.",
            "By setting out the rationale, scope and operating expectations for "
            "{kw0}, the document aims to reduce ambiguity, support institutional "
            "planning and improve the quality of {kw1} across the system.",
        ],
    },
    "objectives": {
        "headings": ["Objectives", "Policy Objectives", "Goals"],
        "paras": [
            "The principal objectives are to strengthen {kw0}, harmonise "
            "institutional approaches to {kw1}, and establish measurable indicators "
            "through which progress can be tracked and reported.",
            "This document seeks to enhance institutional capacity for {kwlist2}, "
            "promote transparency in decision-making, and ensure that resources "
            "committed to related initiatives are used effectively.",
            "In pursuing these objectives, {dept} intends to create conditions in "
            "which institutions can deliver consistent, high-quality outcomes in "
            "respect of {kwlist}, with clear lines of accountability.",
        ],
    },
    "scope": {
        "headings": ["Scope and Applicability", "Scope", "Coverage"],
        "paras": [
            "The provisions apply to all universities and colleges under {dept}. "
            "Sector partners, including employers and professional bodies, are "
            "expected to support implementation within their respective spheres.",
            "This document covers all activities and stakeholders associated with "
            "{kw0} within institutions affiliated to {dept}, together with "
            "arrangements for monitoring, review and reporting of outcomes.",
            "Applicable across the full range of institutions governed by {dept}, "
            "the document addresses planning, delivery, resourcing and evaluation, "
            "and may be supplemented by operational guidance issued subsequently.",
        ],
    },
    "provisions": {
        "headings": ["Key Provisions", "Core Provisions", "Principal Provisions"],
        "paras": [
            "Key provisions establish minimum standards for {kw0}, define the "
            "responsibilities of institutional leadership, and require documented "
            "arrangements for {kw1}.",
            "Institutions are required to integrate {kwlist2} into annual planning "
            "cycles, designate accountable officers, and maintain records "
            "demonstrating compliance with the expectations set out here.",
            "The document mandates periodic self-assessment against {kw0}, disclosure "
            "of material information to {dept}, and corrective action where "
            "institutional performance falls short of the stated expectations.",
        ],
    },
    "implementation": {
        "headings": ["Implementation Framework", "Implementation Arrangements",
                     "Implementation Pathway"],
        "paras": [
            "Implementation will proceed through phased institutional adoption, "
            "supported by professional development, shared guidance and a common "
            "reporting calendar established by {dept}.",
            "Institutions are expected to appoint a lead authority for "
            "implementation, adapt the framework to local context, and report "
            "progress to {dept} in line with the prescribed cycle.",
            "A dedicated implementation pathway, covering readiness assessment, "
            "capacity building and progressive rollout, has been designed to allow "
            "institutions of varying size and maturity to comply.",
        ],
    },
    "roles_resp": {
        "headings": ["Roles and Responsibilities", "Institutional Responsibilities",
                     "Accountability Framework"],
        "paras": [
            "{dept} provides policy direction, capacity support and performance "
            "oversight, while institutions translate the framework into operational "
            "practice and maintain day-to-day accountability.",
            "Heads of institutions bear overall responsibility for embedding {kw0}, "
            "supported by designated coordinators, academic bodies and administrative "
            "units within each institution.",
            "Clear allocation of responsibility is expected at institutional level, "
            "with {dept} acting as the coordinating authority for evaluation, "
            "escalation and cross-institutional learning.",
        ],
    },
    "monitoring": {
        "headings": ["Monitoring and Evaluation", "Monitoring Arrangements",
                     "Performance Monitoring"],
        "paras": [
            "Progress will be monitored through annual institutional returns, "
            "targeted reviews and outcome indicators maintained centrally by "
            "{dept}. Findings will inform guidance and, where necessary, corrective "
            "measures.",
            "{dept} will review implementation against agreed indicators, publish "
            "consolidated outcomes, and engage institutions that require additional "
            "support or show exemplary practice.",
            "Monitoring covers both compliance and quality, combining documented "
            "evidence with stakeholder feedback to build a complete picture of "
            "institutional performance over time.",
        ],
    },
    "review": {
        "headings": ["Review and Amendment", "Continuous Improvement", "Review Cycle"],
        "paras": [
            "This document will be reviewed within a defined cycle to reflect "
            "changing needs, emerging practice and feedback from institutions, with "
            "revisions issued to affected stakeholders.",
            "A periodic review will assess the continued relevance of the "
            "provisions, the effectiveness of supporting arrangements, and the "
            "adequacy of resourcing committed to {kw0}.",
            "Revisions will be introduced through consultation with institutions and "
            "sector bodies, and published with clear dates of application.",
        ],
    },
    "conclusion": {
        "headings": ["Conclusion", "Way Forward", "Closing Note"],
        "paras": [
            "Effective implementation of {title} will depend on sustained "
            "institutional commitment and close coordination between {dept} and the "
            "sector. Institutions are encouraged to embed the arrangements described "
            "here into their normal planning and review cycles.",
            "The success of this {cat_term} rests on shared understanding and "
            "consistent application. {dept} stands ready to support institutions in "
            "implementing {kw0} and looks forward to reviewing collective progress.",
            "This document is designed to evolve with institutional experience. "
            "Feedback from stakeholders will shape refinements, ensuring that the "
            "framework remains practical, proportionate and fit for purpose.",
        ],
    },
    # ----- secondary (guide/plan) document roles -----
    "introduction": {
        "headings": ["Introduction", "Purpose of this Guide", "Overview"],
        "paras": [
            "This guide supports institutions, officials and beneficiaries in "
            "applying the provisions of {title} consistently. It explains eligibility, "
            "expected procedures and the supporting arrangements that govern "
            "day-to-day practice.",
            "Issued in support of {title}, this guide translates the framework into "
            "practical steps for implementation. It is intended to be read alongside "
            "the principal document and any related circulars.",
            "This document accompanies {title} and provides operational detail for "
            "{kwlist2}. Its purpose is to help those responsible for implementation "
            "act correctly, consistently and on time.",
        ],
    },
    "applicability": {
        "headings": ["Applicability", "Scope of Application", "To Whom It Applies"],
        "paras": [
            "The guidance applies to all institutions and stakeholders falling "
            "within the scope of {title}. Specific requirements vary according to "
            "institutional type, size and stage of implementation.",
            "Covered entities are expected to align their internal procedures with "
            "the expectations set out here, adapting them only where local "
            "conditions or statutory requirements justify a documented variation.",
            "Except where expressly stated otherwise, the guidance applies uniformly "
            "across {dept}, and institutions should treat it as the reference point "
            "for day-to-day decisions on {kw0}.",
        ],
    },
    "prerequisites": {
        "headings": ["Preconditions", "Eligibility and Preconditions", "Readiness"],
        "paras": [
            "Prior to implementation, institutions should confirm readiness in "
            "respect of {kwlist2}, designate the officers responsible, and document "
            "any existing arrangements that will be retained or replaced.",
            "Eligible participants should meet the conditions set out in {title}, "
            "provide the required information, and comply with the procedural "
            "expectations described in this guide.",
            "Implementation should be preceded by a readiness review covering "
            "{kw0}, resourcing and staff awareness, so that obligations are "
            "understood before they take effect.",
        ],
    },
    "steps": {
        "headings": ["Implementation Steps", "Key Steps", "Sequence of Actions"],
        "paras": [
            "The recommended sequence is to establish governance, communicate "
            "expectations, adopt the required arrangements for {kw0}, verify "
            "compliance, and report outcomes through the agreed channel.",
            "Institutions should first designate accountability, then prepare "
            "supporting documentation, brief staff, apply the arrangements and "
            "schedule a formal review of the resulting practice.",
            "A staged approach is recommended, beginning with assessment and "
            "planning, followed by rollout, monitoring of early experience, and "
            "refinement in light of feedback before wider adoption.",
        ],
        "bullets": [
            "Appoint a lead officer and notify {dept} of the designated contact.",
            "Conduct an initial readiness review against {kwlist2}.",
            "Brief governing bodies, staff and relevant stakeholders on the expectations.",
            "Apply the arrangements and maintain the required records.",
            "Report on a regular cycle and act on review findings.",
        ],
    },
    "responsibilities": {
        "headings": ["Responsibilities", "Roles in Implementation",
                     "Allocation of Responsibility"],
        "paras": [
            "{dept} coordinates implementation and reviews institutional returns, "
            "while institutions manage day-to-day application, maintain records and "
            "respond to queries from stakeholders.",
            "Named officials within each institution are accountable for operational "
            "application, for reporting variances, and for ensuring that staff "
            "understand and follow the expectations set out in {title}.",
            "Clear segregation of duties is encouraged, with institutional leadership "
            "providing oversight, the designated office managing administration, and "
            "internal controls verifying that practice matches policy.",
        ],
    },
    "procedure": {
        "headings": ["Operational Procedure", "Procedure", "Operating Steps"],
        "paras": [
            "Institutions should maintain a written procedure capturing each step "
            "of the process, including eligibility checks, documentation, "
            "decision-making and appeals, consistent with the provisions of "
            "{title}.",
            "Operational practice must follow an auditable sequence: application or "
            "initiation, verification, decision, communication, recording and, where "
            "applicable, renewal or extension.",
            "All procedural steps should be documented and time-bound, with "
            "responsibilities assigned at each stage and escalation paths agreed for "
            "complex or exceptional cases.",
        ],
    },
    "reporting": {
        "headings": ["Reporting Requirements", "Reporting", "Submission Cycle"],
        "paras": [
            "Reports should be submitted in the prescribed format and within the "
            "stated deadlines, summarising compliance, outcomes, exceptions and any "
            "corrective actions taken.",
            "Institutions are expected to retain supporting evidence for the "
            "reporting period and to make it available for verification by {dept} or "
            "its authorised representatives.",
            "A consolidated reporting cycle will allow {dept} to track performance "
            "across the sector and to identify emerging issues requiring guidance or "
            "intervention.",
        ],
    },
    # ----- tertiary (annexure / checklist) document roles -----
    "checklist": {
        "headings": ["Compliance Checklist", "Checklist", "Verification Checklist"],
        "paras": [
            "The following checklist supports institutions in confirming that the "
            "arrangements required by {title} are in place before implementation "
            "proceeds.",
            "Use this checklist to verify that each area of {kw0} has been "
            "addressed, and record the status and supporting evidence for every item.",
        ],
    },
    "indicators": {
        "headings": ["Performance Indicators", "Indicator Framework",
                     "Outcome Indicators"],
        "paras": [
            "Performance indicators set out below provide a standard basis for "
            "measuring progress against the objectives of {title}.",
            "Institutions should report against these indicators using consistent "
            "definitions, so that aggregate trends can be compared across the "
            "sector.",
        ],
    },
    "milestones": {
        "headings": ["Implementation Milestones", "Milestone Framework",
                     "Phased Timelines"],
        "paras": [
            "The milestone framework indicates the sequence and timing of key "
            "implementation steps under {title}, subject to local adaptation.",
            "Milestones should be confirmed in each institution's annual operating "
            "plan, with owners assigned and progress reviewed on a regular cycle.",
        ],
    },
    "responsibility": {
        "headings": ["Responsibility Matrix", "Ownership of Activities",
                     "Activity Ownership"],
        "paras": [
            "The responsibility matrix assigns primary accountability for the main "
            "activities under {title}, recognising that many actions require "
            "coordination across units.",
            "Each activity should have a clearly named owner at institutional level, "
            "with {dept} providing the coordinating and oversight functions.",
        ],
    },
    "notes": {
        "headings": ["Notes", "Interpretation and Queries", "Supporting Guidance"],
        "paras": [
            "Where institutions require clarification, queries may be directed to "
            "the coordinating office in {dept}, which will issue guidance to ensure "
            "consistent interpretation.",
            "Additional operational circulars may be issued to supplement this "
            "annexure; institutions should apply the most recent version of any "
            "related guidance.",
        ],
    },
}

INTRO_POOL = [
    "This {doc_type_lower}, prepared by {dept}, supports the implementation of "
    "{title}. It sets out {descr} and provides the operational direction needed "
    "for consistent, accountable practice.",
    "Issued in {year} by {dept} under reference {ref}, this {doc_type_lower} "
    "establishes the arrangements supporting {title}. In scope are {kwlist}, "
    "together with related expectations for institutions and stakeholders.",
    "This {doc_type_lower} gives effect to {title} and covers the purposes, "
    "responsibilities and operating expectations associated with {kw0}. It "
    "should be read alongside supporting guidance and any subsequent circulars.",
]

CONCLUSION_POOL = [
    "Effective implementation of {title} will depend on sustained institutional "
    "commitment and close coordination between {dept} and the sector. "
    "Institutions are encouraged to embed the arrangements described here into "
    "their normal planning and review cycles.",
    "The success of this {cat_term} rests on shared understanding and consistent "
    "application. {dept} stands ready to support institutions in implementing "
    "{kw0} and looks forward to reviewing collective progress.",
    "This document is designed to evolve with institutional experience. Feedback "
    "from stakeholders will shape refinements, ensuring that the framework "
    "remains practical, proportionate and fit for purpose.",
]


def checklist_rows(ctx):
    items = [
        "Arrangements for {kw0} are documented and approved".format(**ctx),
        "A responsible officer is designated for {kw1}".format(**ctx),
        "Staff have been informed of expectations related to {kwlist2}".format(**ctx),
        "Reporting schedules and formats are agreed with {dept}".format(**ctx),
        "Records are maintained in the prescribed format".format(**ctx),
        "A review cycle is scheduled for the current year".format(**ctx),
        "Supporting evidence is available for verification".format(**ctx),
    ]
    return [["Item", "Status", "Evidence Reference"],
            *[[item, "In place / Pending", "Section {n}".format(n=i + 1)]
              for i, item in enumerate(items[:6])]]


def indicator_rows(ctx):
    rows = [
        ["Coverage of {kw0}".format(**ctx), "Share of institutions meeting the standard for {kw0}".format(**ctx), "Annual"],
        ["Capacity for {kw1}".format(**ctx), "Number of institutions with documented arrangements".format(**ctx), "Annual"],
        ["Implementation of {kw2}".format(**ctx), "Share of planned actions completed on schedule".format(**ctx), "Bi-annual"],
        ["Stakeholder awareness", "Proportion of institutions with completed awareness sessions", "Annual"],
        ["Outcome reported", "Aggregate results reported to {dept}".format(**ctx), "Annual"],
    ]
    return [["Indicator", "Definition", "Reporting Cycle"], *rows]


def milestone_rows(ctx):
    rows = [
        ["Readiness assessment", "Quarter 1", "Institutions"],
        ["Adoption of arrangements for {kw0}".format(**ctx), "Quarter 2", "Institutional leads"],
        ["Staff engagement and communication", "Quarter 2-3", "Institutional leads"],
        ["First reporting cycle", "Quarter 4", "Institutions / {dept}".format(**ctx)],
        ["Review and refinement", "Following year", "{dept}".format(**ctx)],
    ]
    return [["Milestone", "Timeline", "Responsibility"], *rows]


def responsibility_rows(ctx):
    rows = [
        ["Policy direction and oversight", "{dept}".format(**ctx), "Institutional leadership"],
        ["Implementation of {kw0}".format(**ctx), "Institutions", "{dept}".format(**ctx)],
        ["Capacity building for {kw1}".format(**ctx), "Institutions", "{dept}".format(**ctx)],
        ["Monitoring and reporting", "{dept}".format(**ctx), "Institutions"],
        ["Evaluation and refinement", "{dept}".format(**ctx), "All stakeholders"],
    ]
    return [["Activity", "Lead", "Support"], *rows]


# ---------------------------------------------------------------------------
# Document assembly
# ---------------------------------------------------------------------------

PRIMARY_ROLES = ["background", "purpose", "objectives", "scope", "provisions",
                 "implementation", "roles_resp", "monitoring", "conclusion"]

SECONDARY_ROLES = ["introduction", "applicability", "prerequisites", "steps",
                   "responsibilities", "procedure", "reporting", "conclusion"]

TERTIARY_TABLES = [
    ("checklist", "checklist", checklist_rows, 0.0),
    ("indicators", "indicators", indicator_rows, 0.0),
    ("milestones", "milestones", milestone_rows, 0.0),
    ("responsibility", "responsibility", responsibility_rows, 0.0),
]


def _paragraph(role, ctx, record_id, doc_suffix, slot):
    pool = ROLES[role]["paras"]
    idx = stable_index(f"{record_id}|{doc_suffix}|{role}|para{slot}", len(pool))
    return pool[idx].format(**ctx)


def _heading(role, ctx, record_id, doc_suffix):
    pool = ROLES[role]["headings"]
    idx = stable_index(f"{record_id}|{doc_suffix}|{role}|head", len(pool))
    return pool[idx].format(**ctx)


def _numbered(role, ctx, record_id, doc_suffix):
    pool = ROLES[role].get("bullets") or []
    order = list(range(len(pool)))
    idx = stable_index(f"{record_id}|{doc_suffix}|{role}|order", len(pool) or 1)
    if len(order) > 1:
        order = order[idx:] + order[:idx]
    return [pool[i].format(**ctx) for i in order]


def _primary_blocks(ctx, record_id, suffix):
    blocks = []
    for role in PRIMARY_ROLES:
        blocks.append({"type": "heading", "text": _heading(role, ctx, record_id, suffix)})
        if role == "steps":
            blocks.append({"type": "paragraphs", "items": [
                _paragraph(role, ctx, record_id, suffix, 0)]})
            blocks.append({"type": "numbered", "items": _numbered(role, ctx, record_id, suffix)})
            continue
        items = [_paragraph(role, ctx, record_id, suffix, 0)]
        if role not in ("monitoring", "review", "conclusion"):
            items.append(_paragraph(role, ctx, record_id, suffix, 1))
        blocks.append({"type": "paragraphs", "items": items})
    return blocks


def _secondary_blocks(ctx, record_id, suffix):
    blocks = []
    for role in SECONDARY_ROLES:
        blocks.append({"type": "heading", "text": _heading(role, ctx, record_id, suffix)})
        if role == "steps":
            items = [_paragraph(role, ctx, record_id, suffix, 0)]
            blocks.append({"type": "paragraphs", "items": items})
            blocks.append({"type": "numbered", "items": _numbered(role, ctx, record_id, suffix)})
            continue
        items = [_paragraph(role, ctx, record_id, suffix, 0)]
        if role in ("introduction", "applicability", "steps"):
            items.append(_paragraph(role, ctx, record_id, suffix, 1))
        blocks.append({"type": "paragraphs", "items": items})
    return blocks


def _tertiary_blocks(ctx, record_id, suffix):
    blocks = []
    chosen = [t for t in TERTIARY_TABLES
              if stable_index(f"{record_id}|{suffix}|table:{t[1]}", 10) < 6]
    if len(chosen) < 2:
        chosen = TERTIARY_TABLES[:2]
    for role, tkey, builder, _ in chosen:
        blocks.append({"type": "heading", "text": _heading(role, ctx, record_id, suffix)})
        blocks.append({"type": "paragraphs",
                       "items": [_paragraph(role, ctx, record_id, suffix, 0)]})
        blocks.append({"type": "table", "headers": builder(ctx)[0],
                       "rows": builder(ctx)[1:]})
    blocks.append({"type": "heading", "text": _heading("notes", ctx, record_id, suffix)})
    blocks.append({"type": "paragraphs",
                   "items": [_paragraph("notes", ctx, record_id, suffix, 0)]})
    return blocks


def build_document_spec(record, doc_type, category):
    """doc_type is a dict from category doc-type definitions."""
    ctx = build_context(record, doc_type["name"], category)
    record_id = record["id"]
    suffix = doc_type["suffix"]
    rank = doc_type["rank"]

    blocks = [{"type": "intro", "items": [INTRO_POOL[
        stable_index(f"{record_id}|{suffix}|intro", len(INTRO_POOL))].format(**ctx)]}]

    if rank == "primary":
        blocks.extend(_primary_blocks(ctx, record_id, suffix))
    elif rank == "secondary":
        blocks.extend(_secondary_blocks(ctx, record_id, suffix))
    else:
        blocks.extend(_tertiary_blocks(ctx, record_id, suffix))

    doc_type_label = doc_type["name"]
    title = f"{record['title']} \u2014 {doc_type_label}"

    return {
        "filename": f"{record_id}_{suffix}.pdf",
        "record_id": record_id,
        "title": title,
        "doc_type": doc_type_label,
        "reference": record.get("referenceNumber") or record_id,
        "department": record.get("department") or "Higher Education Department",
        "year": record.get("publicationYear") or 2024,
        "status": record.get("status") or "Active",
        "body": blocks,
    }


# ---------------------------------------------------------------------------
# Timestamps
# ---------------------------------------------------------------------------

def parse_published(record):
    raw = (record.get("publishedDate") or "").strip()
    try:
        d = date.strptime(raw, "%d %B %Y")
    except (ValueError, TypeError):
        y = record.get("publicationYear") or 2024
        d = date(int(y), 6, 30)
    return d


def uploaded_at(record, day_offset):
    d = parse_published(record) + timedelta(days=day_offset)
    return d.strftime("%Y-%m-%dT09:00:00")