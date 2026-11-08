#!/usr/bin/env python3
"""Post-development quality gates that Maven does not enforce on its own (Discipline §9).

Run after `mvn verify`, which already enforces unit tests, the dependency rule (ArchUnit) and PIT's
whole-project mutation floor. This script reads the same reports per layer. A context is a top-level
package under src/main/java; its layers are the packages below it (catalogue.domain, catalogue.usecase,
...). `kernel` and `ie` have no layers.

  * CRAP index below 6 for every method, from JaCoCo's report.
  * Mutation score and test strength per layer: at least 90% for kernel and every domain, at least 80%
    for every usecase. Infra has no mutation gate; it is verified by integration tests.
  * Mutation ratchet: a layer's score and test strength may never drop below its last passing score,
    stored in quality/ratchet.json. Pass --update to raise the ratchet after a pass.
  * Every context is in PIT's targetClasses in pom.xml, so a new context cannot skip the gate.
  * The integration event contracts in ie/ have no logic and are exempt from the mutation gate.

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
LAYERS = ("domain", "usecase", "infra")
MUTATION_THRESHOLDS = {"kernel": 90.0, "domain": 90.0, "usecase": 80.0}
# Event records have no logic, so the mutation gate does not apply to them (domain plan, Integration events).
NO_MUTATION_GATE = {"ie"}


def layers():
    """Every gated unit: kernel, ie, and each <context>.<layer> that has code."""
    found = []
    for context in sorted(p for p in SOURCES.iterdir() if p.is_dir()):
        present = [layer for layer in LAYERS if (context / layer).is_dir()]
        found += [f"{context.name}.{layer}" for layer in present] if present else [context.name]
    return found


def layer_of(class_name):
    parts = re.split(r"[./]", class_name)
    return f"{parts[0]}.{parts[1]}" if len(parts) > 2 and parts[1] in LAYERS else parts[0]


def threshold_of(layer):
    kind = layer.split(".")[-1]
    return MUTATION_THRESHOLDS.get(kind)


def counter(element, kind):
    for c in element.findall("counter"):
        if c.get("type") == kind:
            return int(c.get("missed")), int(c.get("covered"))
    return 0, 0


def crap_by_layer(failures):
    worst = defaultdict(float)
    for cls in ET.parse(JACOCO).getroot().iter("class"):
        layer = layer_of(cls.get("name"))
        for method in cls.findall("method"):
            missed_c, covered_c = counter(method, "COMPLEXITY")
            missed_i, covered_i = counter(method, "INSTRUCTION")
            complexity = missed_c + covered_c
            total = missed_i + covered_i
            coverage = covered_i / total if total else 1.0
            crap = complexity ** 2 * (1 - coverage) ** 3 + complexity
            worst[layer] = max(worst[layer], crap)
            if crap >= CRAP_LIMIT:
                name = cls.get("name").replace("/", ".") + "#" + method.get("name")
                failures.append(f"{name} CRAP {crap:.1f} (complexity {complexity}, coverage {coverage:.0%})")
    return worst


def mutation_by_layer():
    statuses = defaultdict(list)
    for m in ET.parse(PIT).getroot().iter("mutation"):
        statuses[layer_of(m.findtext("mutatedClass"))].append(m.get("status"))
    scores = {}
    for layer, found in statuses.items():
        total = len(found)
        killed = sum(1 for s in found if s in ("KILLED", "TIMED_OUT", "MEMORY_ERROR"))
        covered = total - sum(1 for s in found if s == "NO_COVERAGE")
        scores[layer] = {
            "score": round(100 * killed / total, 2) if total else 100.0,
            "strength": round(100 * killed / covered, 2) if covered else 100.0,
            "mutants": total,
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
    worst = crap_by_layer(failures)
    mutation = mutation_by_layer()
    targets = pit_targets()

    print(f"{'layer':<22}{'worst CRAP':>11}{'mutants':>9}{'mutation':>10}{'strength':>10}{'limit':>7}{'ratchet':>9}")
    for layer in layers():
        context = layer.split(".")[0]
        threshold = threshold_of(layer)
        last = ratchet.get(layer, {"score": 0.0, "strength": 0.0})
        if context in NO_MUTATION_GATE or threshold is None:
            print(f"{layer:<22}{worst[layer]:>11.1f}{'-':>9}{'n/a':>10}{'n/a':>10}{'n/a':>7}{'n/a':>9}")
            continue
        if context not in targets:
            failures.append(f"{context}: not in PIT targetClasses in pom.xml")
        scores = mutation.get(layer)
        if scores is None:
            failures.append(f"{layer}: no mutants in the PIT report")
            print(f"{layer:<22}{worst[layer]:>11.1f}{'-':>9}{'-':>10}{'-':>10}{threshold:>6.0f}%{last['score']:>8.2f}%")
            continue
        for key in ("score", "strength"):
            if scores[key] < threshold:
                failures.append(f"{layer}: mutation {key} {scores[key]}% is below {threshold:.0f}%")
            if scores[key] < last[key]:
                failures.append(f"{layer}: mutation {key} {scores[key]}% fell below the ratchet {last[key]}%")
        print(f"{layer:<22}{worst[layer]:>11.1f}{scores['mutants']:>9}{scores['score']:>9.2f}%{scores['strength']:>9.2f}%"
              f"{threshold:>6.0f}%{last['score']:>8.2f}%")
        if update:
            ratchet[layer] = {key: max(scores[key], last[key]) for key in ("score", "strength")}

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
