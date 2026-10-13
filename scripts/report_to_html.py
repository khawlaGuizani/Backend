#!/usr/bin/env python3
"""
report_to_html.py — Rapports HTML (style "ZAP Scanning Report") pour chaque
job de la pipeline DevSecOps TransitFlow. Python 3 standard uniquement.

  junit     <dossier surefire-reports>  out.html [--jacoco target/site/jacoco/jacoco.csv]
  gitleaks  gitleaks.json               out.html
  sarif     fichier.sarif               out.html --title "Semgrep — SAST Report"
  cyclonedx sbom.cyclonedx.json         out.html
  licenses  THIRD-PARTY.txt             out.html
  text      fichier.log                 out.html --title "..."
  gate      -                           out.html --results "Build=success,Gitleaks=failure"
  index     dossier-des-rapports        dossier/index.html
"""
import argparse, csv, glob, html, json, os, re, sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

LEVELS = ["High", "Medium", "Low", "Informational"]
COLORS = {"High": "#ff0000", "Medium": "#ffa500", "Low": "#ffff00",
          "Informational": "#0000ff", "PASS": "#008000", "FAIL": "#ff0000", "SKIP": "#888888"}

CSS = """
body{font-family:Arial,Helvetica,sans-serif;margin:20px 40px;color:#000;background:#fff}
h1{font-size:30px;text-align:center;margin:10px 0 30px}
h2{font-size:20px;margin:28px 0 10px}
h3{font-size:16px;margin:8px 0}
table{border-collapse:separate;border-spacing:2px;margin-bottom:16px}
th{background:#666;color:#fff;padding:8px 14px;font-size:14px;text-align:center}
td{background:#e8e8e8;padding:5px 10px;font-size:13px;vertical-align:top}
td.lvl{min-width:200px}
.results{width:100%}
.results td{word-break:break-word}
.meta td:first-child{font-weight:bold;width:160px}
pre{background:#f4f4f4;border:1px solid #ccc;padding:12px;white-space:pre-wrap;font-size:12px}
.footer{margin-top:40px;font-size:12px;color:#555}
.badge{display:inline-block;padding:2px 10px;border-radius:3px;color:#fff;font-weight:bold}
a{color:#00e}
"""


def esc(v):
    return html.escape("" if v is None else str(v))


def lvl(level):
    fg = "#000" if level == "Low" else "#fff"
    return f'<td class="lvl" style="background:{COLORS.get(level, "#666")};color:{fg}">{esc(level)}</td>'


def badge(status):
    return f"<span class='badge' style='background:{COLORS.get(status, '#666')}'>{esc(status)}</span>"


def page(title, tool, summary, body):
    now = datetime.now(timezone.utc).strftime("%a, %d %b %Y %H:%M:%S UTC")
    e = os.environ.get
    return f"""<!DOCTYPE html>
<html lang="fr"><head><meta charset="utf-8"><title>{esc(title)}</title><style>{CSS}</style></head><body>
<h1>{esc(title)}</h1>
<h2>Projet : {esc(e("GITHUB_REPOSITORY", "TransitFlow"))}</h2>
<h3>Généré le {now}</h3>
<h3>Outil : {esc(tool)}</h3>
<table class="meta">
<tr><td>Branche</td><td>{esc(e("GITHUB_REF_NAME", "-"))}</td></tr>
<tr><td>Commit</td><td>{esc(e("GITHUB_SHA", "-")[:7])}</td></tr>
<tr><td>Pipeline run</td><td>#{esc(e("GITHUB_RUN_NUMBER", "local"))}</td></tr>
<tr><td>Job</td><td>{esc(e("GITHUB_JOB", "-"))}</td></tr>
</table>
{summary}
{body}
<div class="footer">Rapport généré automatiquement par la pipeline DevSecOps TransitFlow (GitHub Actions).</div>
</body></html>"""


def summary_alerts(rows):
    c = {}
    for r in rows:
        c[r[0]] = c.get(r[0], 0) + 1
    trs = "".join(f"<tr>{lvl(l)}<td style='text-align:center'>{c.get(l, 0)}</td></tr>" for l in LEVELS)
    return f"<h2>Summary of Alerts</h2><table><tr><th>Risk Level</th><th>Number of Alerts</th></tr>{trs}</table>"


def table_alerts(headers, rows, empty="Aucune alerte détectée."):
    if not rows:
        return f"<h2>Alerts</h2><p>✅ {esc(empty)}</p>"
    order = {l: i for i, l in enumerate(LEVELS)}
    rows = sorted(rows, key=lambda r: order.get(r[0], 9))
    th = "".join(f"<th>{esc(h)}</th>" for h in headers)
    body = "".join("<tr>" + lvl(r[0]) + "".join(f"<td>{esc(c)}</td>" for c in r[1:]) + "</tr>" for r in rows)
    return f"<h2>Alerts</h2><table class='results'><tr>{th}</tr>{body}</table>"


def load_json(path):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return None


def kv_table(title, pairs):
    trs = "".join(f"<tr><td>{esc(k)}</td><td style='text-align:center'>{v}</td></tr>" for k, v in pairs)
    return f"<h2>{esc(title)}</h2><table class='meta'>{trs}</table>"


# ------------------------------------------------------------------ JUnit + JaCoCo
def r_junit(src, a):
    tests = []
    for f in sorted(glob.glob(os.path.join(src, "TEST-*.xml")) if os.path.isdir(src) else glob.glob(src)):
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        for tc in root.iter("testcase"):
            st, msg = "PASS", ""
            for tag, s in (("failure", "FAIL"), ("error", "FAIL"), ("skipped", "SKIP")):
                n = tc.find(tag)
                if n is not None:
                    st, msg = s, (n.get("message") or n.get("type") or "")[:300]
                    break
            tests.append((st, tc.get("classname", "").split(".")[-1], tc.get("name"), tc.get("time", ""), msg))
    n = len(tests)
    ok = sum(t[0] == "PASS" for t in tests)
    ko = sum(t[0] == "FAIL" for t in tests)
    sk = sum(t[0] == "SKIP" for t in tests)
    rate = f"{ok * 100 / (n - sk):.1f} %" if n - sk else "-"
    summary = kv_table("Summary of Tests", [("Total", n), ("Réussis", badge("PASS") + f" {ok}"),
                                           ("Échoués", badge("FAIL") + f" {ko}"), ("Ignorés", badge("SKIP") + f" {sk}"),
                                           ("Taux de réussite", rate)])
    if a.jacoco and os.path.exists(a.jacoco):
        tot = {}
        with open(a.jacoco, encoding="utf-8") as f:
            for row in csv.DictReader(f):
                for k in ("INSTRUCTION", "BRANCH", "LINE", "METHOD", "CLASS"):
                    m, c = int(row.get(k + "_MISSED", 0) or 0), int(row.get(k + "_COVERED", 0) or 0)
                    tm, tc = tot.get(k, (0, 0))
                    tot[k] = (tm + m, tc + c)
        names = {"INSTRUCTION": "Instructions", "BRANCH": "Branches", "LINE": "Lignes", "METHOD": "Méthodes", "CLASS": "Classes"}
        summary += kv_table("Couverture de code (JaCoCo)",
                            [(names[k], f"{c * 100 / (m + c):.1f} % ({c}/{m + c})" if m + c else "-")
                             for k, (m, c) in tot.items()])
    th = "".join(f"<th>{h}</th>" for h in ("Statut", "Classe", "Test", "Durée (s)", "Message"))
    body = "".join(f"<tr><td>{badge(t[0])}</td>" + "".join(f"<td>{esc(x)}</td>" for x in t[1:]) + "</tr>"
                   for t in sorted(tests, key=lambda t: (t[0] != "FAIL", t[1], t[2])))
    body = f"<h2>Détail des tests</h2><table class='results'><tr>{th}</tr>{body}</table>" if tests else \
        "<h2>Détail des tests</h2><p>Aucun résultat de test trouvé.</p>"
    return page(a.title or "Build & Test Report", "Maven Surefire + JaCoCo", summary, body)


# ------------------------------------------------------------------ Gitleaks JSON
def r_gitleaks(src, a):
    rows = [["High", f.get("RuleID"), f.get("Description"), f"{f.get('File')}:{f.get('StartLine')}",
             (f.get("Commit") or "")[:7], f.get("Author"), f.get("Date")] for f in (load_json(src) or [])]
    return page(a.title or "Gitleaks — Secret Scanning Report", "Gitleaks", summary_alerts(rows),
                table_alerts(["Risk", "Règle", "Description", "Fichier:Ligne", "Commit", "Auteur", "Date"], rows,
                             "Aucun secret détecté dans l'historique Git."))


# ------------------------------------------------------------------ SARIF (Semgrep, Grype, Trivy, ...)
def sarif_level(result, rule):
    sev = (rule.get("properties") or {}).get("security-severity")
    try:
        s = float(sev)
        return "High" if s >= 7 else "Medium" if s >= 4 else "Low" if s > 0 else "Informational"
    except (TypeError, ValueError):
        pass
    level = result.get("level") or (rule.get("defaultConfiguration") or {}).get("level") or "warning"
    return {"error": "High", "warning": "Medium", "note": "Low"}.get(level, "Informational")


def r_sarif(src, a):
    data = load_json(src) or {}
    rows, tool = [], "SARIF"
    for run in data.get("runs", []):
        drv = (run.get("tool") or {}).get("driver") or {}
        tool = drv.get("name", tool)
        rules = {r.get("id"): r for r in drv.get("rules", []) or []}
        for res in run.get("results", []) or []:
            rule = rules.get(res.get("ruleId"), {})
            loc = ((res.get("locations") or [{}])[0].get("physicalLocation") or {})
            where = (loc.get("artifactLocation") or {}).get("uri", "")
            line = (loc.get("region") or {}).get("startLine")
            msg = (res.get("message") or {}).get("text") or (rule.get("shortDescription") or {}).get("text", "")
            rows.append([sarif_level(res, rule), res.get("ruleId"), msg[:500], f"{where}:{line}" if line else where])
    return page(a.title or f"{tool} Report", tool, summary_alerts(rows),
                table_alerts(["Risk", "Règle / CVE", "Description", "Emplacement"], rows))


# ------------------------------------------------------------------ CycloneDX SBOM
def r_cyclonedx(src, a):
    data = load_json(src) or {}
    comps = []
    for c in data.get("components", []) or []:
        lic = []
        for l in c.get("licenses", []) or []:
            x = l.get("license") or {}
            lic.append(x.get("id") or x.get("name") or l.get("expression") or "")
        comps.append((c.get("type", ""), c.get("name", ""), c.get("version", ""), ", ".join(filter(None, lic)) or "—",
                      c.get("purl", "")))
    comps.sort()
    by_type = {}
    for c in comps:
        by_type[c[0]] = by_type.get(c[0], 0) + 1
    summary = kv_table("Summary", [("Composants inventoriés", len(comps))] + sorted(by_type.items()))
    th = "".join(f"<th>{h}</th>" for h in ("Type", "Composant", "Version", "Licence(s)", "PURL"))
    body = "".join("<tr>" + "".join(f"<td>{esc(x)}</td>" for x in c) + "</tr>" for c in comps)
    return page(a.title or "SBOM — Software Bill of Materials", "Syft (CycloneDX)", summary,
                f"<h2>Composants</h2><table class='results'><tr>{th}</tr>{body}</table>")


# ------------------------------------------------------------------ Licences (THIRD-PARTY.txt)
LIC_RE = re.compile(r"^\s*((?:\([^()]*\)\s*)+)(.*?)\s+\(([^:\s]+):([^:\s]+):([^\s]+)\s+-\s+(.*)\)\s*$")


def lic_risk(lic):
    l = lic.lower()
    if re.search(r"\b(a?gpl)\b|general public license", l) and "lesser" not in l and "lgpl" not in l \
            and "classpath" not in l:
        return "High"
    if re.search(r"lgpl|lesser|mpl|mozilla|epl|eclipse|cddl", l):
        return "Medium"
    if "unknown" in l or not l.strip():
        return "Low"
    return "Informational"


def r_licenses(src, a):
    rows = []
    files = glob.glob(os.path.join(src, "**", "*.txt"), recursive=True) if os.path.isdir(src) else [src]
    for f in files:
        if not os.path.exists(f):
            continue
        for line in open(f, encoding="utf-8", errors="replace"):
            m = LIC_RE.match(line)
            if m:
                lic = ", ".join(x.strip("() ") for x in re.findall(r"\([^()]*\)", m.group(1)))
                rows.append([lic_risk(lic), m.group(2), f"{m.group(3)}:{m.group(4)}", m.group(5), lic])
    legend = ("<p><b>Légende :</b> High = copyleft fort (GPL/AGPL) · Medium = copyleft faible (LGPL/MPL/EPL/CDDL) · "
              "Low = licence inconnue · Informational = permissive (Apache, MIT, BSD...)</p>")
    return page(a.title or "License Compliance Report", "license-maven-plugin", summary_alerts(rows) + legend,
                table_alerts(["Risk", "Dépendance", "GroupId:ArtifactId", "Version", "Licence(s)"], rows,
                             "Aucune dépendance tierce trouvée."))


# ------------------------------------------------------------------ Texte / Gate / Index
def r_text(src, a):
    content = open(src, encoding="utf-8", errors="replace").read() if os.path.exists(src) else "(aucune sortie)"
    return page(a.title or "Pipeline Job Report", "GitHub Actions", "", f"<h2>Sortie du job</h2><pre>{esc(content)}</pre>")


def r_gate(_src, a):
    rows, ok = "", True
    for item in filter(str.strip, a.results.split(",")):
        name, _, status = item.partition("=")
        status = status.strip() or "unknown"
        b = {"success": "PASS", "skipped": "SKIP"}.get(status, "FAIL")
        if b == "FAIL" and name.strip() in a.blocking.split(";"):
            ok = False
        rows += f"<tr><td>{esc(name.strip())}</td><td>{badge(b)} {esc(status)}</td>" \
                f"<td>{'Bloquant' if name.strip() in a.blocking.split(';') else 'Informatif'}</td></tr>"
    verdict = badge("PASS") + " PASSED — déploiement autorisé" if ok else badge("FAIL") + " FAILED — déploiement bloqué"
    body = f"<h2>Décision : {verdict}</h2><table class='results'><tr><th>Job</th><th>Résultat</th><th>Type</th></tr>{rows}</table>"
    return page(a.title or "Security Gate Report", "Security Gate", "", body)


def r_index(src, a):
    files = sorted(p for p in glob.glob(os.path.join(src, "**", "*.html"), recursive=True)
                   if os.path.abspath(p) != os.path.abspath(a.output))
    base = os.path.dirname(a.output) or "."
    rows = "".join(f"<tr><td>{esc(os.path.relpath(os.path.dirname(p), src))}</td>"
                   f"<td><a href='{esc(os.path.relpath(p, base))}'>{esc(os.path.basename(p))}</a></td></tr>" for p in files)
    return page(a.title or "TransitFlow — Rapports de la pipeline", "GitHub Actions",
                kv_table("Summary", [("Rapports HTML", len(files))]),
                f"<h2>Rapports par job</h2><table class='results'><tr><th>Job</th><th>Rapport</th></tr>{rows}</table>")


RENDER = {"junit": r_junit, "gitleaks": r_gitleaks, "sarif": r_sarif, "cyclonedx": r_cyclonedx,
          "licenses": r_licenses, "text": r_text, "gate": r_gate, "index": r_index}


def main():
    p = argparse.ArgumentParser()
    p.add_argument("kind", choices=sorted(RENDER))
    p.add_argument("input")
    p.add_argument("output")
    p.add_argument("--title")
    p.add_argument("--jacoco")
    p.add_argument("--results", default="")
    p.add_argument("--blocking", default="", help="jobs bloquants séparés par ';'")
    a = p.parse_args()
    os.makedirs(os.path.dirname(a.output) or ".", exist_ok=True)
    out = RENDER[a.kind](a.input, a)
    with open(a.output, "w", encoding="utf-8") as f:
        f.write(out)
    print(f"Rapport HTML écrit : {a.output}")


if __name__ == "__main__":
    sys.exit(main())
