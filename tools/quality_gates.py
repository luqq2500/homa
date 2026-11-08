#!/usr/bin/env python3
"""Post-development quality gates that Maven does not enforce on its own (Discipline §9).

Run after `mvn verify`, which already enforces unit tests, the dependency rule (ArchUnit) and the
90% mutation threshold for the whole project (PIT). This script reads the same reports per context,
where a context is a top-level package under src/main/java (kernel, catalogue, policy, ...):

  * CRAP index below 6 for every method, from JaCoCo's report.
  * Mutation score and test strength of at least 90% for each context, not just overall.
  * Mutation ratchet: a context's score and test strength may never drop below its last passing
    score, stored in quality/ratchet.json. Pass --update to raise the ratchet after a pass.
  * Every context is in PIT's targetClasses in pom.xml, so a new context cannot skip the gate.

Usage: python3 tools/quality_gates.py [--update]
"""
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCES = ROOT / "src" / "main" / "java"
JACOCO = ROOT / "target" / "site" / "jacoco" / "jacoco.xml"
PIT = ROOT / "target" / "pit-reports" / "mutations.xml"
RATCHET = ROOT / "quality" / "ratchet.json"
CRAP_LIMIT = 6.0
MUTATION_THRESHOLD = 90.0


def contexts():
    return sorted(p.name for p in SOURCES.iterdir() if p.is_dir())


def context_of(class_name):
    return re.split(r"[./]", class_name, maxsplit=1)[0]


def counter(element, kind):
    for c in element.findall("counter"):
        if c.get("type") == kind:
            return int(c.get("missed")), int(c.get("covered"))
    return 0, 0


def crap_by_context(failures):
    worst = defaultdict(float)
    for cls in ET.parse(JACOCO).getroot().iter("class"):
        context = context_of(cls.get("name"))
        for method in cls.findall("method"):
            missed_c, covered_c = counter(method, "COMPLEXITY")
            missed_i, covered_i = counter(method, "INSTRUCTION")
            complexity = missed_c + covered_c
            total = missed_i + covered_i
            coverage = covered_i / total if total else 1.0
            crap = complexity ** 2 * (1 - coverage) ** 3 + complexity
            worst[context] = max(worst[context], crap)
            if crap >= CRAP_LIMIT:
                name = cls.get("name").replace("/", ".") + "#" + method.get("name")
                failures.append(f"{name} CRAP {crap:.1f} (complexity {complexity}, coverage {coverage:.0%})")
    return worst


def mutation_by_context():
    statuses = defaultdict(list)
    for m in ET.parse(PIT).getroot().iter("mutation"):
        statuses[context_of(m.findtext("mutatedClass"))].append(m.get("status"))
    scores = {}
    for context, found in statuses.items():
        total = len(found)
        killed = sum(1 for s in found if s in ("KILLED", "TIMED_OUT", "MEMORY_ERROR"))
        covered = total - sum(1 for s in found if s == "NO_COVERAGE")
        scores[context] = {
            "score": round(100 * killed / total, 2) if total else 100.0,
            "strength": round(100 * killed / covered, 2) if covered else 100.0,
        }
    return scores


def pit_targets():
    return set(re.findall(r"<param>(\w+)\.\*</param>", (ROOT / "pom.xml").read_text()))


def main(update):
    for report in (JACOCO, PIT):
        if not report.exists():
            print(f"No report at {report.relative_to(ROOT)}; run mvn verify first")
            return 1
    ratchet = json.loads(RATCHET.read_text()) if RATCHET.exists() else {}
    failures = []
    worst = crap_by_context(failures)
    mutation = mutation_by_context()
    targets = pit_targets()

    print(f"{'context':<14}{'worst CRAP':>12}{'mutation':>11}{'strength':>11}{'ratchet':>10}")
    for context in contexts():
        last = ratchet.get(context, {"score": 0.0, "strength": 0.0})
        if context not in targets:
            failures.append(f"{context}: not in PIT targetClasses in pom.xml")
        scores = mutation.get(context)
        if scores is None:
            failures.append(f"{context}: no mutants in the PIT report")
            print(f"{context:<14}{worst[context]:>12.1f}{'-':>11}{'-':>11}{last['score']:>9.2f}%")
            continue
        for key in ("score", "strength"):
            if scores[key] < MUTATION_THRESHOLD:
                failures.append(f"{context}: mutation {key} {scores[key]}% is below {MUTATION_THRESHOLD}%")
            if scores[key] < last[key]:
                failures.append(f"{context}: mutation {key} {scores[key]}% fell below the ratchet {last[key]}%")
        print(f"{context:<14}{worst[context]:>12.1f}{scores['score']:>10.2f}%{scores['strength']:>10.2f}%{last['score']:>9.2f}%")
        if update:
            ratchet[context] = {key: max(scores[key], last[key]) for key in ("score", "strength")}

    if failures:
        print("\nQuality gates FAILED:")
        for f in failures:
            print("  - " + f)
        return 1
    if update:
        RATCHET.write_text(json.dumps(ratchet, indent=2, sort_keys=True) + "\n")
        print(f"\nRatchet updated in {RATCHET.relative_to(ROOT)}")
    print("\nQuality gates passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main("--update" in sys.argv[1:]))
