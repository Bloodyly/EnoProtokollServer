# ✂️ pack das in dieselbe Datei wie deinen Flask-Code (oder in ein utils-Modul)

from pathlib import Path
from typing import List, Tuple, Optional, Literal
from datetime import datetime, timezone
import io, os, re, json
from openpyxl import load_workbook

# Falls oben schon definiert, diese Konstanten ggf. entfernen/zusammenführen:
SHARED_FOLDER = "/app/shared/"
LISTEN_FOLDER = os.path.join(SHARED_FOLDER, "Expose", "Listen")
PROTOKOLL_FOLDER = os.path.join(SHARED_FOLDER, "Expose", "Protokolle")

# ---- Kernfunktion ----------------------------------------------------------

def compose_response_structure(
    vn_nr: str,
    excel_template_path: str,
    protokoll_root: str,
    *,
    objekt: Optional[str] = None,
    output: Literal["tsv","json"] = "tsv",
) -> bytes | dict:
    """
    Liest Meta aus Vorlage (B1..B4) und Daten/Counts aus jüngstem Protokoll für VN.
    Gibt standardmäßig eine kompakte TSV-Struktur (bytes) zurück.
    Bei output='json' eine Python-Struct (dict), die JSON-serialisierbar ist.
    """
    # --- 1) Meta aus Vorlage -------------------------------------------------
    template_path = Path(excel_template_path)
    kunde, meta_vn, meta_obj, meta_date = read_meta_from_template(template_path)

    # Fallbacks/Normalisierung
    kunde = kunde or "-"
    letzte_bearbeitung = dt_iso_utc(template_path.stat().st_mtime)

    # --- 2) Jüngstes Protokoll finden ---------------------------------------
    proto_path = find_latest_protokoll_for_vn(protokoll_root, vn_nr, objekt_filter=objekt)

    # --- 3) Protokoll parsen -------------------------------------------------
    columns: List[str] = []
    rows: List[List[str]] = []
    summary = {"ausgeloest": 0, "nio": 0, "gesamt": 0}
    if proto_path:
        columns, rows, summary = parse_protokoll_xlsx(proto_path, objekt_filter=objekt)

    # --- 4) Verpacken --------------------------------------------------------
    if output == "json":
        return {
            "version": 1,
            "generated": now_iso_utc(),
            "vn": {
                "vn_nr": vn_nr,
                "kunde": kunde,
                "filename": template_path.name,
                "letzte_bearbeitung": letzte_bearbeitung,
            },
            "objekt": objekt or meta_obj or "-",
            "columns": columns,
            "rows": rows,        # Liste von Listen (Strings)
            "summary": summary,  # {"ausgeloest":x,"nio":y,"gesamt":z}
        }

    # TSV (kompakt, menschenlesbar; kein JSON)
    lines: List[List[str]] = []
    lines.append(["#VERSION", "1"])
    lines.append(["#GENERATED", now_iso_utc()])
    lines.append(["#VN", vn_nr, kunde, template_path.name, letzte_bearbeitung])
    if objekt or meta_obj:
        # falls du weitere Objektfelder hast, hier anhängen:
        lines.append(["#OBJEKT", objekt or meta_obj or "-", "-", "-", "-", "-", "-"])
    if columns:
        lines.append(["#COLUMNS", *columns])
        lines.extend(rows)
    lines.append([
        "#SUMMARY",
        f"ausgeloest={summary['ausgeloest']}",
        f"nio={summary['nio']}",
        f"gesamt={summary['gesamt']}",
    ])

    payload = build_tsv(lines)
    return payload  # bytes


# ---- Helfer ----------------------------------------------------------------

def now_iso_utc() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

def dt_iso_utc(epoch_sec: float) -> str:
    return datetime.fromtimestamp(epoch_sec, tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

def build_tsv(lines: List[List[str]]) -> bytes:
    buf = io.StringIO()
    for line in lines:
        # Tabs sind robust; falls Felder Tabs enthalten könnten, hier notfalls ersetzen/escapen
        buf.write("\t".join("" if v is None else str(v) for v in line) + "\n")
    return buf.getvalue().encode("utf-8")

def read_meta_from_template(xlsx_path: Path) -> Tuple[str, str, str, str]:
    """
    Erwartet Meta im ersten Sheet:
      B1: Kunde
      B2: VN-Nummer
      B3: Objekt (optional)
      B4: Datum (optional)
    Passe das 1:1 an deinen Aufbau an.
    """
    wb = load_workbook(filename=str(xlsx_path), read_only=True, data_only=True)
    ws = wb.worksheets[0]
    b1 = safe_cell(ws, "B1")
    b2 = safe_cell(ws, "B2")
    b3 = safe_cell(ws, "B3")
    b4 = safe_cell(ws, "B4")
    wb.close()
    return b1, b2, b3, b4

def safe_cell(ws, ref: str) -> str:
    try:
        v = ws[ref].value
        return str(v).strip() if v is not None else ""
    except Exception:
        return ""

def find_latest_protokoll_for_vn(protokoll_root: str, vn_nr: str, objekt_filter: Optional[str] = None) -> Optional[Path]:
    """
    Sucht im Ordner Expose/Protokolle/<VN> nach der neuesten .xlsx.
    Wenn objekt_filter gesetzt, bevorzugt eine Datei, deren Name das Objekt enthält.
    """
    base = Path(protokoll_root) / str(vn_nr)
    if not base.exists():
        return None
    candidates = sorted(base.rglob("*.xlsx"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not candidates:
        return None
    if objekt_filter:
        ol = objekt_filter.lower()
        for c in candidates:
            if ol in c.name.lower():
                return c
    return candidates[0]

def parse_protokoll_xlsx(xlsx_path: Path, objekt_filter: Optional[str] = None) -> Tuple[List[str], List[List[str]], dict]:
    """
    Robustes Header-Mapping:
      erwartete Headernamen (case-insensitiv):
        melder_id | ausloesung | status | bemerkung
    Gern erweitern (z.B. 'objekt') – unten markiert.
    """
    wb = load_workbook(filename=str(xlsx_path), read_only=True, data_only=True)
    ws = wb.worksheets[0]  # ggf. bestimmtes Blatt wählen

    # Header-Zeile (1)
    header_raw = [str(c.value).strip().lower() if c.value else "" for c in ws[1]]

    def find_idx(names: List[str]) -> int:
        for n in names:
            if n in header_raw:
                return header_raw.index(n)
        return -1

    idx_id     = find_idx(["melder_id", "melder", "id"])
    idx_aus    = find_idx(["ausloesung", "auslösung", "trigger", "value"])
    idx_status = find_idx(["status", "zustand"])
    idx_bem    = find_idx(["bemerkung", "comment", "notes"])
    idx_obj    = find_idx(["objekt", "anlage", "bereich"])  # optional

    columns = ["melder_id", "ausloesung", "status", "bemerkung"]
    rows: List[List[str]] = []
    ausg, nio, total = 0, 0, 0

    for r in ws.iter_rows(min_row=2, values_only=True):
        if all(c is None for c in r):
            continue

        if objekt_filter and idx_obj >= 0:
            val_obj = (str(r[idx_obj]).strip() if r[idx_obj] is not None else "")
            if val_obj and val_obj.lower() != objekt_filter.lower():
                continue

        melder_id  = str(r[idx_id]).strip() if idx_id >= 0 and r[idx_id] is not None else "-"
        aus_raw    = r[idx_aus] if idx_aus >= 0 else 0
        status_raw = (str(r[idx_status]).strip().upper() if idx_status >= 0 and r[idx_status] else "-")
        bemerk     = (str(r[idx_bem]).strip() if idx_bem >= 0 and r[idx_bem] else "-")

        # normalisieren
        a = 0.0
        try:
            a = float(aus_raw) if aus_raw is not None and str(aus_raw).strip() != "" else 0.0
        except Exception:
            a = 0.0

        total += 1
        if a > 0: ausg += 1
        if status_raw == "NIO": nio += 1

        rows.append([
            melder_id,
            str(int(a)) if float(a).is_integer() else str(a),
            status_raw or "-",
            bemerk or "-",
        ])

    wb.close()
    summary = {"ausgeloest": ausg, "nio": nio, "gesamt": total}
    return columns, rows, summary
