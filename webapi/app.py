from flask import Flask, request, jsonify, Response
from crypto_utils import decrypt_payload, encrypt_payload # 🔐 
from helper import compose_response_structure, maybe_compress_then_encrypt  # 📄 eigene Logik
from werkzeug.exceptions import BadRequest
import base64, hashlib, binascii   # <-- NEU
import configparser
import threading 
import time
import os
import json
import openpyxl
from typing import Optional
app = Flask(__name__)

PRIVATE_KEY = base64.b64decode(os.environ.get("PRIVATE_KEY_BASE64", ""))


# === DEBUG: Key-Info beim Start ===
try:
    if PRIVATE_KEY:
        pk_b64 = base64.b64encode(PRIVATE_KEY).decode("utf-8")
        pk_len = len(PRIVATE_KEY)
        pk_sha = hashlib.sha256(PRIVATE_KEY).hexdigest()
        print(f"[DEBUG] PRIVATE_KEY(Base64)={pk_b64}  len={pk_len}  sha256={pk_sha}")
    else:
        print("[DEBUG][WARN] PRIVATE_KEY ist leer! Env var PRIVATE_KEY_BASE64 fehlt/leer.")
except Exception as e:
    print(f"[DEBUG][ERR] Konnte PRIVATE_KEY nicht loggen: {e}")
    
    

SHARED_FOLDER = "/app/shared/"
EXPOSED_FOLDER = "/app/shared/Expose/"
REQUIRED_FOLDERS = ["Listen/", "Protokolle/", "Archiv/"]
LISTEN_FOLDER = os.path.join(SHARED_FOLDER, "Expose", "Listen")
PROTOKOLL_FOLDER = os.path.join(SHARED_FOLDER, "Expose", "Protokolle")
USER_FILE = os.path.join(SHARED_FOLDER, "users.txt")
CONFIG_PATH = "/app/shared/Expose/config.ini"
_config_cache = {"mtime": None, "cfg": None}
_config_lock = threading.Lock()

def ensure_shared_structure():
    os.makedirs(SHARED_FOLDER, exist_ok=True)
    os.makedirs(EXPOSED_FOLDER, exist_ok=True)
    for folder in REQUIRED_FOLDERS:
        path = os.path.join(EXPOSED_FOLDER, folder)
        os.makedirs(path, exist_ok=True)
        
def ensure_default_users_file():
    users_path = os.path.join(SHARED_FOLDER, "users.txt")
    if not os.path.exists(users_path):
        print("[INFO] users.txt nicht gefunden – Standardbenutzer wird erstellt.")
        with open(users_path, "w") as f:
            f.write("admin:admin123\n")
    else:
        print("[INFO] users.txt bereits vorhanden.")

def get_config() -> configparser.ConfigParser:
    """Liest config.ini mit einfachem Auto-Reload, thread-safe."""
    global _config_cache
    with _config_lock:
        try:
            mtime = os.path.getmtime(CONFIG_PATH)
        except FileNotFoundError:
            mtime = None
        if _config_cache["cfg"] is not None and _config_cache["mtime"] == mtime:
            return _config_cache["cfg"]

        cfg = configparser.ConfigParser()
        # Defaults
        cfg["server"] = {
            "response_format": "tsv",
            "gzip_enabled": "true",
            "gzip_min_size": "1024",
            "gzip_level": "5",
            "max_plain_response_size": "10485760",
        }
        # Datei (falls vorhanden) drüberladen
        if mtime is not None:
            cfg.read(CONFIG_PATH, encoding="utf-8")
        _config_cache = {"mtime": mtime, "cfg": cfg}
        return cfg
        
# --- NEU: bevorzugtes Antwortformat bestimmen ---
def _pick_response_format(cfg) -> str:
    # a) Query-Param ?fmt=json|tsv
    q = (request.args.get("fmt") or "").strip().lower()
    # b) Header X-Prefer-Format: json|tsv
    h = (request.headers.get("X-Prefer-Format") or "").strip().lower()
    # c) Fallback aus config.ini
    c = (cfg["server"].get("response_format", "tsv") or "tsv").strip().lower()
    for cand in (q, h, c):
        if cand in ("json", "tsv"):
            return cand
    return "json"
        
@app.route("/get_protokoll", methods=["POST"])
def check_request():
    try:
        # --- Debug: eingehende Nutzlast ---
        enc = request.get_data()  # bytes
        print(f"[DEBUG] /get_protokoll: incoming {len(enc)} bytes")
        
        # --- Robust decrypt: bytes ODER str kompatibel ---
        dec = decrypt_payload(enc, PRIVATE_KEY)  # kann bytes ODER str liefern (je nach crypto_utils-Version)
        if isinstance(dec, bytes):
            decrypted_json = dec.decode("utf-8", errors="strict")
        else:
            decrypted_json = dec  # already str
            
        print(f"[DEBUG] decrypted JSON length={len(decrypted_json)}")
    
        data = json.loads(decrypted_json)
        
        #Check User
        username = data.get("username")
        password = data.get("password")
        vn_nr = data.get("vn")
        
        #FehlerBehandlung
        if not all([username, password, vn_nr]):
            raise BadRequest("Ungültige Anfrageparameter.")

        if not validate_user(username, password):
            return jsonify({"status": "error", "message": "Benutzer oder Passwort ungültig"}), 401
        
        cfg = get_config()
        fmt = _pick_response_format(cfg)   # <-- statt fix aus config
        if fmt == "json":
            model = compose_response_structure(vn_nr, None, PROTOKOLL_FOLDER, output="json")
            # schlank serialisieren (keine Leerzeichen)
            plain_bytes = json.dumps(model, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        else:
            # kompaktes TSV (default)
            plain_bytes = compose_response_structure(vn_nr, None, PROTOKOLL_FOLDER, output="tsv")

        print(f"[DEBUG] plain payload size before gzip/encrypt: {len(plain_bytes)} bytes (fmt={fmt})")

        
        # Jetzt ggf. GZIP → dann ENCRYPT
        cfg = get_config()
        cipher, was_gzip, original_size = maybe_compress_then_encrypt(
            plain_bytes, encrypt_payload, PRIVATE_KEY, cfg=cfg
            )
 

        resp = Response(cipher, status=200, mimetype="application/octet-stream")
        if was_gzip:
            resp.headers["X-Content-Compressed"] = "gzip"
        resp.headers["X-Original-Size"] = str(original_size)
        resp.headers["X-Format"] = fmt
        resp.headers["Cache-Control"] = "no-store"
        return resp

    except (json.JSONDecodeError, UnicodeDecodeError, ValueError) as e:
        print(f"[DEBUG][ERR] decrypt/json error: {e}")
        return jsonify({
            "status": "error",
            "message": "Entschlüsselung oder JSON-Parsing fehlgeschlagen. Möglicherweise falscher Key."
        }), 400

    except Exception as e:
        print(f"[DEBUG][ERR] unexpected: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


def validate_user(username, password):
    if not os.path.exists(USER_FILE):
        return False
    with open(USER_FILE, "r") as f:
        for line in f:
            stored_user, stored_pass = line.strip().split(":")
            if username == stored_user and password == stored_pass:
                return True
    return False


def find_excel_by_vn(vn):
    for file in os.listdir(LISTEN_FOLDER):
        if file.lower().startswith(f"vn{vn.lower()}") and file.endswith(".xlsx"):
            return os.path.join(LISTEN_FOLDER, file)
    return None


@app.route("/", methods=["GET"])
def index():
    return "EnoProtokollServer API bereit", 200


    
# ✅ Erst jetzt die Initialisierung aufrufen
ensure_shared_structure()
ensure_default_users_file()

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)