from pathlib import Path
from typing import Dict, Any, List, Tuple, Optional, Literal, Iterable
import json, os, gzip
# NEU: Multi-Sheet Strict-Body Parser (Header via headerWidth, Body via descriptor/quarterCols)
from helper_meta_parser_tv_strict_body_multi import compose_envelope_from_workbooks as _compose_envelope


SHARED_FOLDER = "/app/shared"
DATA_JSON = os.path.join(SHARED_FOLDER, "data.json")
LISTEN_FOLDER = os.path.join(SHARED_FOLDER, "Expose", "Listen")
PROTOKOLL_FOLDER = os.path.join(SHARED_FOLDER, "Expose", "Protokolle")
META_JSON = os.path.join(SHARED_FOLDER, "meta.json")


def compose_response_structure(vn_nr: str,) -> bytes:
    """
 Baut das neue Envelope-JSON (Multi-Anlagen, T/V-Trennung, qStartCol, itemsEditable).
    Gibt **UTF-8 JSON bytes** zurück – direkt geeignet für Komprimierung/Verschlüsselung.
    """

    # 1) filename über data.json
    filename = _resolve_filename_from_datajson(vn_nr)
    if not filename:
        raise FileNotFoundError(f"VN {vn_nr}: kein Eintrag in data.json gefunden.")

    listen_path = Path(LISTEN_FOLDER) / filename
    if not listen_path.exists():
        raise FileNotFoundError(f"Listen-Datei fehlt: {listen_path}")
    
    # Optional : Protokoll Pfad festlegen
    proto_path = _resolve_protokoll_path(vn_nr, filename)  # kann None sein

    # 3) meta.json
    meta_path = Path(META_JSON)
    if not meta_path.exists():
        raise FileNotFoundError(f"meta.json nicht gefunden: {meta_path}")
        
    # 4) Envelope bauen (der Parser liest alle Blätter = Anlagen, Name aus B4)
    return _compose_envelope(
        str(listen_path),
        str(proto_path) if proto_path else None,
        str(meta_path)
    )

# ----------------------------- Filename Resolver -------------------------------------

def _resolve_filename_from_datajson(vn_nr: str) -> Optional[str]:
    norm_vn = _normalize_vn(vn_nr)
    if not os.path.exists(DATA_JSON):
        return None
    with open(DATA_JSON, "r", encoding="utf-8") as f:
        data = json.load(f)
    for entry in data.get("vn", []):
        if _normalize_vn(entry.get("vn_nr","")) == norm_vn:
            return entry.get("filename")
    return None

def _resolve_protokoll_path(vn_nr: str, filename: str) -> Optional[Path]:
    vn_dir = Path(PROTOKOLL_FOLDER) / _normalize_vn(vn_nr)
    exact = vn_dir / filename
    if exact.exists():
        return exact
    if vn_dir.exists():
        files = sorted(vn_dir.rglob("*.xlsx"), key=lambda p: p.stat().st_mtime, reverse=True)
        return files[0] if files else None
    return None

def _normalize_vn(vn: str) -> str:
    s = (vn or "").strip().upper()
    return s if s.startswith("VN") else f"VN{s}"

# ----------------------------- Meta & Utils ---------------------------------

def maybe_compress_then_encrypt(
    plain_bytes: bytes,
    encrypt_func,
    key_bytes: bytes,
    cfg=None,
):
    """
    Komprimiert optional (abhängig von cfg) und verschlüsselt dann.
    Rückgabe: (cipher_bytes, compressed_bool, original_size_int)
    """
    # --- sichere Defaults ---
    gzip_enabled = True
    gzip_min_size = 1024
    gzip_level = 5
    max_plain = 10 * 1024 * 1024  # 10 MB

    # --- cfg übersteuert Defaults, wenn vorhanden ---
    try:
        if cfg is not None and "server" in cfg:
            sec = cfg["server"]
            # getboolean/getint werfen nicht, wenn fallback übergeben wird
            gzip_enabled = sec.getboolean("gzip_enabled", fallback=gzip_enabled)
            gzip_min_size = sec.getint("gzip_min_size", fallback=gzip_min_size)
            gzip_level = sec.getint("gzip_level", fallback=gzip_level)
            if gzip_level < 1: gzip_level = 1
            if gzip_level > 9: gzip_level = 9
            max_plain = sec.getint("max_plain_response_size", fallback=max_plain)
    except Exception:
        # Falls die Config kaputt ist: auf Defaults laufen
        pass

    size = len(plain_bytes)
    if size > max_plain:
        raise ValueError(f"Antwort zu groß ({size} Bytes), abgebrochen.")

    compressed = False
    out_bytes = plain_bytes
    
    if gzip_enabled and size >= gzip_min_size:
        out_bytes = gzip.compress(plain_bytes, compresslevel=gzip_level)
        compressed = True

    # Jetzt verschlüsseln (deine encrypt_payload erwartet (bytes, key))
    cipher = encrypt_func(out_bytes, key_bytes)
    return cipher, compressed, size
