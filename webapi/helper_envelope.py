from __future__ import annotations
from typing import Dict, Any, List, Optional
from pathlib import Path
import json
from envelope import Envelope, Meta, Protokoll, Anlage, Table, make_head, make_grid, EditedBy, now_utc_iso

def load_meta_rules(meta_path: str) -> Dict[str, Any]:
    p = Path(meta_path)
    if not p.exists():
        raise FileNotFoundError(f"meta.json not found at {meta_path}")
    with p.open("r", encoding="utf-8") as f:
        return json.load(f)

def _merge(a: Dict[str, Any], b: Dict[str, Any]) -> Dict[str, Any]:
    out = dict(a or {})
    for k, v in (b or {}).items():
        if isinstance(v, dict) and isinstance(out.get(k), dict):
            out[k] = _merge(out[k], v)
        else:
            out[k] = v
    return out

def compose_response_envelope(
    vn_nr: str,
    p_type: str,
    w_type: str,
    kunde: str,
    listen_struct: Dict[str, Any],
    protokoll_struct: Optional[Dict[str, Any]],
    meta_rules: Dict[str, Any],
    edited_by_name: str = "ProtokollServer"
) -> bytes:
    defaults = meta_rules.get("defaults", {})
    proto_rules = (meta_rules.get("protocols", {}) or {}).get(p_type, {})
    melder_rule = _merge(defaults.get("melder", {}), proto_rules.get("melder", {}))
    hardware_rule = _merge(defaults.get("hardware", {}), proto_rules.get("hardware", {}))

    melder_types_set = set()
    for _, ldata in (listen_struct or {}).items():
        for g in ldata.get("Gruppen", []):
            for mt in g.get("MelderTypen", []):
                if mt: melder_types_set.add(str(mt))
    melder_types = sorted(melder_types_set) or ["I","R","DM"]

    anlagen_out: List[Anlage] = []
    for anlagen_name, ldata in (listen_struct or {}).items():
        head_labels = melder_rule.get("header", ["Nr","Ort","Typ"])
        spans = melder_rule.get("spans", [{"r0":0,"c0":0,"r1":0,"c1":2,"label":"Melderübersicht"}])
        head = make_head(head_labels, spans)

        rows: List[List[str]] = []
        pmap = (protokoll_struct or {}).get(anlagen_name, {})

        for g in ldata.get("Gruppen", []):
            mg = str(g.get("Nummer",""))
            anz = int(g.get("Anz", 0) or 0)
            melder_typen = g.get("MelderTypen", [])
            for idx in range(anz):
                typ = str(melder_typen[idx]) if idx < len(melder_typen) else "-"
                ort = ""
                rows.append([mg, ort, typ])

        columns_editable = {int(k): v for k, v in (melder_rule.get("columnsEditable") or {"0":"int","1":"string","2":"melderType"}).items()}
        melder_table = Table(head=head, grid=make_grid(rows, columns_editable))

        hardware_table = None
        enabled = hardware_rule.get("enabled", True)
        if enabled:
            hw_map = ldata.get("Hardware") or {}
            if isinstance(hw_map, dict) and hw_map:
                hw_header = hardware_rule.get("header", ["Bauteil","Anzahl"])
                hw_spans = hardware_rule.get("spans", [])
                hw_head = make_head(hw_header, hw_spans)
                hw_rows: List[List[str]] = []
                for k, v in hw_map.items():
                    hw_rows.append([str(k), str(v)])
                hw_edit = {int(k): v for k, v in (hardware_rule.get("columnsEditable") or {"0":"string","1":"int"}).items()}
                hardware_table = Table(head=hw_head, grid=make_grid(hw_rows, hw_edit))

        anlagen_out.append(Anlage(name=anlagen_name, melder=melder_table, hardware=hardware_table))

    env = Envelope(
        Meta = Meta(pType=p_type, wType=w_type, VNnr=vn_nr, Kunde=kunde),
        Protokoll = Protokoll(
            melderTypes = melder_types,
            anlagen = anlagen_out,
            editedBy = EditedBy(name=edited_by_name, ts=now_utc_iso())
        )
    )
    return env.to_json_bytes()
