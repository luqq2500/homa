#!/usr/bin/env python3
"""Generates docs/business-rule-coverage.md: which business rule is built, in which layer of which context.

The rules come from docs/homa-business-rule.md (every table row whose first cell is a rule ID such as RES-11). The code
side comes from the tests: a rule is "cited" by a layer when a test in that layer carries @Tag("<rule ID>"). The slice
for each rule comes from the plan's build order (SLICES below, copied from docs/homa-business-rule-development-plan.md).

Usage:  python3 -I tools/rule_coverage.py            # rewrite docs/business-rule-coverage.md
        python3 -I tools/rule_coverage.py --check    # exit 1 when the committed file is out of date

Regenerate after every merge that adds or changes tests. NOTES below is the one hand-kept part: it marks rules that a
test cites but that are only partly built, and it must be updated when a slice finishes one of them.
"""
import collections
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RULES = ROOT / "docs" / "homa-business-rule.md"
TESTS = ROOT / "src" / "test" / "java" / "com" / "homa"
OUT = ROOT / "docs" / "business-rule-coverage.md"

CONTEXTS = ("customer", "verification", "provider", "listing", "reservation", "payment",
            "support", "rating", "conversation", "notification")
LAYERS = ("domain", "usecase", "infra")

OWNER_BY_PREFIX = {
    "RES": "Reservation", "REQ": "Reservation", "ACC": "Reservation", "ROS": "Reservation", "JOB": "Reservation",
    "CAN": "Reservation", "CCX": "Reservation", "PCX": "Reservation", "NRS": "Reservation", "LMC": "Reservation",
    "SUS": "Reservation", "PAY": "Payment", "PRV": "Provider", "HLP": "Provider", "OFR": "Listing",
    "CUS": "Customer", "IDN": "Verification", "MON": "Verification", "RPT": "Support", "NSH": "Support",
    "RTG": "Rating", "CHT": "Conversation", "DAT": "Several", "EXT": "Several",
}
OWNER_OVERRIDES = {"NSH-3": "Reservation", "NSH-5": "Reservation", "NSH-6": "Reservation", "PRV-12": "Support",
                   "CUS-4": "Support", "SUS-5": "Provider", "MON-5": "Verification", "MON-6": "Verification"}

# Copied from the plan's build order. "a-b" ranges are inclusive and stay inside one prefix.
SLICES = {
    0: "PAY-8",
    1: "CUS-1-3 CUS-7 CUS-10 IDN-1-2 PRV-1-3 HLP-1-4 ROS-1-3 ROS-6 OFR-1-6 OFR-9-11 RES-1-12 ACC-1-3 JOB-1-6 PAY-1-3",
    2: "REQ-1-5",
    3: "CAN-1-3 CCX-1-6 PCX-1-2 LMC-1-3 PAY-4-7",
    4: "PRV-6 PRV-11 HLP-5-10 ROS-4-5 ROS-7-8 NRS-1-3",
    5: "RPT-1-11 NSH-1-6 CHT-4",
    6: "MON-1-6 PRV-4-5 PRV-7-10 PRV-12 CUS-4-6 CUS-8-9 SUS-1-5 ROS-9-10 PAY-9-12",
    7: "RTG-1-11",
    8: "CHT-1-3 CHT-5 OFR-7-8",
    9: "DAT-1-15 IDN-3-5",
    10: "EXT-1-7",
}
SLICE_TITLES = {
    0: "Skeleton", 1: "Book and get paid", 2: "Requests and withdrawals", 3: "Cancellations", 4: "Team and roster",
    5: "Reports and no-shows", 6: "Compliance, suspensions and debt", 7: "Ratings and reliability",
    8: "Chat and calendar sync", 9: "Data retention", 10: "External service failures",
}

# Rules a test cites but that are only partly built. Keep this honest: edit it when a slice completes a rule.
NOTES = {
    "CAN-1": "Only the Base Cancellation Fee amount (RM45); cancelling is slice 3.",
    "CCX-1": "Only the Grace Period and tier values; the cancellation itself is slice 3.",
    "PCX-2": "Only the fine and compensation values; the provider cancellation is slice 3.",
    "NSH-5": "Only the customer no-show fee values; the no-show is slice 5.",
    "NSH-6": "Only voiding the payout; confirming a no-show is slice 5.",
    "RPT-1": "Only the Protection Window time; reports are slice 5.",
    "RPT-6": "Only the payout side: it waits while a report is open; reports are slice 5.",
    "RPT-9": "Only the payout drop for a member leaving; the report and refund are slice 5.",
    "ROS-8": "Only the payout drop; the smaller-roster offer is slice 4.",
    "MON-1": "Only the next due date; reminders and lapses are slice 6.",
    "PRV-7": "Business-held payout accounts are not matched yet; business verification is slice 6.",
    "PRV-10": "A changed payout account is checked afresh; payouts waiting for it are slice 6.",
    "IDN-4": "A member's photo fact is carried and checked; the photo check flow is slice 6.",
    "CUS-5": "A suspended customer can't reserve; cancelling their reservations is slice 6.",
    "OFR-8": "Weekly working hours only; calendar sync is slice 8.",
    "DAT-2": "Only the end-of-year date helper; purging payment records is slice 9.",
    "DAT-7": "Only the sign-in block after deletion; the rest is slice 9.",
    "DAT-11": "Only the privacy-notice languages in the country policy; consent is slice 9.",
    "DAT-12": "Only the 21-day answer time in the country policy; data requests are slice 9.",
    "EXT-2": "The gateway confirmation path; the 'provider unavailable' behaviour is slice 10.",
    "EXT-3": "Failures are retried by the relay; the single retry then Support is slice 10.",
    "EXT-5": "Resubmitting a pending verification only; outage handling is slice 10.",
}


def rule_key(rule_id):
    prefix, number = rule_id.split("-")
    return prefix, int(number)


def expand(spec):
    ids = set()
    for token in spec.split():
        m = re.fullmatch(r"([A-Z]{3})-(\d+)(?:-(\d+))?", token)
        prefix, a, b = m.group(1), int(m.group(2)), m.group(3)
        for n in range(a, int(b or a) + 1):
            ids.add(f"{prefix}-{n}")
    return ids


def read_rules():
    """Every rule row on the rules page, in page order: (id, text, section)."""
    rules, section = [], ""
    for line in RULES.read_text().splitlines():
        if line.startswith("## ") or line.startswith("### "):
            section = line.lstrip("#").strip()
            continue
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 2 and re.fullmatch(r"[A-Z]{3}-\d+", cells[0]):
            text = re.sub(r"\*\*|`", "", cells[1])
            rules.append((cells[0], text, section))
    return rules


def read_citations():
    """rule id -> {layer: number of tests}, where layer is '<context>.<layer>' or 'kernel', 'values', 'app', 'ie'."""
    cited = collections.defaultdict(collections.Counter)
    for path in TESTS.rglob("*.java"):
        parts = path.relative_to(TESTS).parts
        top = parts[0]
        if top in CONTEXTS:
            layer = parts[1] if len(parts) > 2 and parts[1] in LAYERS else "other"
            where = f"{top}.{layer}"
        else:
            where = top
        for rule_id in re.findall(r'@Tag\("([A-Z]{3}-\d+)"\)', path.read_text()):
            cited[rule_id][where] += 1
    return cited


def status_of(where):
    layers = {w.split(".")[1] for w in where if "." in w}
    if "domain" in layers and "usecase" in layers:
        return "Domain + usecase"
    if "domain" in layers:
        return "Domain only"
    if "usecase" in layers:
        return "Usecase only"
    if where:
        return "Elsewhere only"
    return "Not started"


def owner_of(rule_id):
    return OWNER_OVERRIDES.get(rule_id, OWNER_BY_PREFIX[rule_id.split("-")[0]])


def cell(text, limit=0):
    text = text.replace("|", "/")
    return text if not limit or len(text) <= limit else text[: limit - 1].rstrip() + "…"


def build():
    rules = read_rules()
    cited = read_citations()
    slice_of = {r: s for s, spec in SLICES.items() for r in expand(spec)}
    rows = []
    for rule_id, text, section in rules:
        where = cited.get(rule_id, {})
        rows.append({
            "id": rule_id, "text": text, "section": section, "owner": owner_of(rule_id),
            "slice": slice_of.get(rule_id), "where": where, "status": status_of(where),
            "partial": rule_id in NOTES and status_of(where) != "Not started",
            "tests": sum(where.values()),
        })
    return rows


def count_line(rows):
    n = collections.Counter(r["status"] for r in rows)
    partial = sum(1 for r in rows if r["partial"])
    return n, partial


STATUSES = ("Domain + usecase", "Domain only", "Usecase only", "Elsewhere only", "Not started")


def render(rows):
    total = len(rows)
    n, partial = count_line(rows)
    cited_total = total - n["Not started"]
    out = []
    w = out.append
    w("# Business rule coverage")
    w("")
    w("> Generated by `tools/rule_coverage.py` from `docs/homa-business-rule.md` and the `@Tag`s on the tests. "
      "Do not edit by hand; run `python3 -I tools/rule_coverage.py` after a merge that changes tests.")
    w("")
    w("Every rule on the rules page has an ID and a case. Each test that specifies a rule carries "
      "`@Tag(\"<rule ID>\")` and a `@DisplayName` quoting the rule's case, so `grep -rn '@Tag(\"RES-11\")' src/test` "
      "lists the tests behind a rule. This page counts those tags.")
    w("")
    w("## How to read it")
    w("")
    w("| Status | Meaning |")
    w("| --- | --- |")
    w("| **Domain + usecase** | A test in the owning context's domain and one in a usecase cite the rule. |")
    w("| **Domain only** | The business object enforces it; no use case drives it yet. |")
    w("| **Usecase only** | A use case cites it, and no domain test does. |")
    w("| **Elsewhere only** | Cited only by kernel, values, app or infra tests, such as a number held in a values class. |")
    w("| **Not started** | No test cites it. |")
    w("")
    w("A rule marked **partial** is cited, but only part of it is built; the note says which part. "
      "\"Cited\" means a test names the rule. It does not prove the rule is complete.")
    w("")
    w("## Summary")
    w("")
    w(f"**{cited_total} of {total}** rules are cited by at least one test; **{partial}** of those are only partly built.")
    w("")
    w("### By slice (the plan's build order)")
    w("")
    w("| Slice | Name | Rules | Domain + usecase | Domain only | Usecase only | Elsewhere only | Not started |")
    w("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |")
    for s in sorted(SLICES):
        sr = [r for r in rows if r["slice"] == s]
        c = collections.Counter(r["status"] for r in sr)
        w(f"| {s} | {SLICE_TITLES[s]} | {len(sr)} | {c['Domain + usecase']} | {c['Domain only']} | "
          f"{c['Usecase only']} | {c['Elsewhere only']} | {c['Not started']} |")
    unsliced = [r for r in rows if r["slice"] is None]
    if unsliced:
        c = collections.Counter(r["status"] for r in unsliced)
        w(f"| – | Not in the plan's build order | {len(unsliced)} | {c['Domain + usecase']} | {c['Domain only']} | "
          f"{c['Usecase only']} | {c['Elsewhere only']} | {c['Not started']} |")
    w("")
    w("### By owning context (the plan's ownership)")
    w("")
    w("| Context | Rules | Domain + usecase | Domain only | Usecase only | Elsewhere only | Not started | Partial |")
    w("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
    for owner in sorted({r["owner"] for r in rows}):
        orows = [r for r in rows if r["owner"] == owner]
        c = collections.Counter(r["status"] for r in orows)
        w(f"| {owner} | {len(orows)} | {c['Domain + usecase']} | {c['Domain only']} | {c['Usecase only']} | "
          f"{c['Elsewhere only']} | {c['Not started']} | {sum(1 for r in orows if r['partial'])} |")
    w("")
    w("## Every rule")
    w("")
    section = None
    for r in rows:
        if r["section"] != section:
            section = r["section"]
            w("")
            w(f"### {section}")
            w("")
            w("| Rule | What it says | Owner | Slice | Status | Cited by (tests) |")
            w("| --- | --- | --- | :-: | --- | --- |")
        where = ", ".join(f"{k} ({v})" for k, v in sorted(r["where"].items())) or "–"
        status = r["status"] + (" **(partial)**" if r["partial"] else "")
        note = f" {NOTES[r['id']]}" if r["partial"] else ""
        w(f"| {r['id']} | {cell(r['text'], 90)} | {r['owner']} | {r['slice'] or '–'} | {status}{cell(note)} | {where} |")
    w("")
    return "\n".join(out)


def main():
    text = render(build())
    if "--check" in sys.argv[1:]:
        if not OUT.exists() or OUT.read_text() != text:
            print(f"{OUT.relative_to(ROOT)} is out of date; run python3 -I tools/rule_coverage.py")
            return 1
        print("Business rule coverage is up to date.")
        return 0
    OUT.write_text(text)
    rows = build()
    n, partial = count_line(rows)
    print(f"Wrote {OUT.relative_to(ROOT)}: {len(rows) - n['Not started']} of {len(rows)} rules cited, "
          f"{partial} partial.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
