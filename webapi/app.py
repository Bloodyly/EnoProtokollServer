from flask import Flask, request, response, jsonify
from crypto_utils import decrypt_payload, encrypt_payload # 🔐 
from helper import compose_response_structure, maybe_compress_then_encrypt  # 📄 eigene Logik
from werkzeug.exceptions import BadRequest
import base64
import configparser
import threading, 
import time
import os
import json
import openpyxl

from typing import Optional

PRIVATE_KEY = base64.b64decode(os.environ.get("PRIVATE_KEY_BASE64", ""))

app = Flask(__name__)
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
        
@app.route("/get_protokoll", methods=["POST"])
def check_request():
    try:
        #Decrypten der nachricht
        encrypted_data = request.get_data()
        # decrypt liefert BYTES (siehe crypto_utils)
        decrypted_bytes = decrypt_payload(encrypted_data, PRIVATE_KEY)
        decrypted_json = decrypted_bytes.decode("utf-8")
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
        fmt = (cfg["server"].get("response_format", "tsv") or "tsv").lower()
        if fmt == "json":
            model = compose_response_structure(vn_nr, None, PROTOKOLL_FOLDER, output="json")
            plain_bytes = json.dumps(model, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        else:
            # default TSV (kompakt)
            plain_bytes = compose_response_structure(vn_nr, None, PROTOKOLL_FOLDER, output="tsv")

        # Jetzt ggf. GZIP → dann ENCRYPT
        cfg = get_config()
        cipher, was_gzip, original_size = maybe_compress_then_encrypt(
            plain_bytes, encrypt_payload, PRIVATE_KEY, cfg=cfg
            )
 
        # Wichtig: verschlüsselte Binärantwort, kein Content-Encoding setzen
        # (das würde sich auf die verschlüsselte Nutzlast beziehen und ist wirkungslos)
        resp = Response(cipher, status=200, mimetype="application/octet-stream")
        # Meta-Hinweise für den Client:
        # 1) sagt: die ENTschlüsselte Nutzlast ist gzip-komprimiert
        if was_gzip:
            resp.headers["X-Content-Compressed"] = "gzip"
        # 2) für Debug/Monitoring:
        resp.headers["X-Original-Size"] = str(original_size)
        resp.headers["X-Format"] = fmt  # "tsv" oder "json"
        # Damit Caches/Proxies auf Clientseite nicht kaputt gehen:
        resp.headers["Cache-Control"] = "no-store"
        return resp
 
        # ✨ TSV erzeugen (bytes)

        # 🔐 verschlüsseln; encrypt_payload erwartet str|bytes → hier bytes

    except (json.JSONDecodeError, UnicodeDecodeError, ValueError):
        # 👇 passiert bei falschem Key
        return jsonify({
            "status": "error",
            "message": "Entschlüsselung oder JSON-Parsing fehlgeschlagen. Möglicherweise falscher Key."
        }), 400

    except Exception as e:
        # Allgemeiner Fehler
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500


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