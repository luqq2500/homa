#!/usr/bin/env python3
"""Post-development quality gates that Maven does not enforce on its own (Discipline §9).

Run after `mvn verify`, which already enforces unit tests, the dependency rule (ArchUnit) and the
90% mutation threshold (PIT). This script adds:

  * CRAP index below 6 for every method, from JaCoCo's report.
  * Mutation ratchet: a module's mutation score and test strength may never drop below its last
    passing score, stored in quality/ratchet.json. Pass --update to raise the ratchet after a pass.

Usage: python3 tools/quality_gates.py [--update]
"""
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RATCHET = ROOT / "quality" / "ratchet.json"
CRAP_LIMIT = 6.0


def modules():
    tree = ET.parse(ROOT / "pom.xml")
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    return [m.text for m in tree.getroot().findall("m:modules/m:module", ns)]


def counter(element, kind):
    for c in element.findall("counter"):
        if c.get("type") == kind:
            return int(c.get("missed")), int(c.get("covered"))
    return 0, 0


def crap_failures(module):
    report = ROOT / module / "target" / "site" / "jacoco" / "jacoco.xml"
    if not report.exists():
        return [f"{module}: no JaCoCo report at {report.relative_to(ROOT)}; run mvn verify first"], 0.0
    failures, worst = [], 0.0
    for cls in ET.parse(report).getroot().iter("class"):
        for method in cls.findall("method"):
            missed_c, covered_c = counter(method, "COMPLEXITY")
            missed_i, covered_i = counter(method, "INSTRUCTION")
            complexity = missed_c + covered_c
            total = missed_i + covered_i
            coverage = covered_i / total if total else 1.0
            crap = complexity ** 2 * (1 - coverage) ** 3 + complexity
            worst = max(worst, crap)
            if crap >= CRAP_LIMIT:
                name = cls.get("name").replace("/", ".") + "#" + method.get("name")
                failures.append(f"{module}: {name} CRAP {crap:.1f} (complexity {complexity}, coverage {coverage:.0%})")
    return failures, worst


def mutation_scores(module):
    report = ROOT / module / "target" / "pit-reports" / "mutations.xml"
    if not report.exists():
        return None
    statuses = [m.get("status") for m in ET.parse(report).getroot().iter("mutation")]
    total = len(statuses)
    killed = sum(1 for s in statuses if s in ("KILLED", "TIMED_OUT", "MEMORY_ERROR"))
    covered = total - sum(1 for s in statuses if s == "NO_COVERAGE")
    score = round(100 * killed / total, 2) if total else 100.0
    strength = round(100 * killed / covered, 2) if covered else 100.0
    return {"score": score, "strength": strength, "mutants": total}


def main(update):
    ratchet = json.loads(RATCHET.read_text()) if RATCHET.exists() else {}
    failures = []
    print(f"{'module':<14}{'worst CRAP':>12}{'mutation':>11}{'strength':>11}{'ratchet':>10}")
    for module in modules():
        crap, worst = crap_failures(module)
        failures += crap
        scores = mutation_scores(module)
        last = ratchet.get(module, {"score": 0.0, "strength": 0.0})
        if scores is None:
            failures.append(f"{module}: no PIT report; run mvn verify first")
            print(f"{module:<14}{worst:>12.1f}{'-':>11}{'-':>11}{last['score']:>9.2f}%")
            continue
        for key in ("score", "strength"):
            if scores[key] < last[key]:
                failures.append(f"{module}: mutation {key} {scores[key]}% fell below the ratchet {last[key]}%")
        print(f"{module:<14}{worst:>12.1f}{scores['score']:>10.2f}%{scores['strength']:>10.2f}%{last['score']:>9.2f}%")
        if update:
            ratchet[module] = {"score": max(scores["score"], last["score"]),
                               "strength": max(scores["strength"], last["strength"])}

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
