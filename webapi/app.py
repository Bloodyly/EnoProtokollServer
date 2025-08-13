from flask import Flask, request, response, jsonify, current_app
from datetime import datetime
from crypto_utils import decrypt_payload, encrypt_payload # 🔐 
from helper import compose_response_structure    # 📄 eigene Logik
from werkzeug.exceptions import BadRequest
from cryptography.fernet import Fernet
import base64
import os
import json
import openpyxl

PRIVATE_KEY = base64.b64decode(os.environ.get("PRIVATE_KEY_BASE64", ""))

app = Flask(__name__)
SHARED_FOLDER = "/app/shared/"
EXPOSED_FOLDER = "/app/shared/Expose/"
REQUIRED_FOLDERS = ["Listen/", "Protokolle/", "Archiv/"]
LISTEN_FOLDER = os.path.join(SHARED_FOLDER, "Expose", "Listen")
PROTOKOLL_FOLDER = os.path.join(SHARED_FOLDER, "Expose", "Protokolle")
USER_FILE = os.path.join(SHARED_FOLDER, "users.txt")

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

@app.route("/get_protokoll", methods=["POST"])
def check_request():
    try:
        #Decrypten der nachricht
        encrypted_data = request.get_data()
        decrypted_json = decrypt_payload(encrypted_data, PRIVATE_KEY_PATH)
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
            
        #Prüfen der Angefragten datei
        excel_path = find_excel_by_vn(vn_nr)
        if not excel_path:
            return jsonify({"status": "error", "message": f"Keine Datei für VN {vn_nr} gefunden"}), 404
        
        # ✨ TSV erzeugen (bytes)
        tsv_bytes = compose_response_structure(vn_nr, PROTOKOLL_FOLDER, LISTEN_FOLDER, output="tsv")

        # 🔐 verschlüsseln; encrypt_payload erwartet str|bytes → hier bytes
        encrypted_response = encrypt_payload(tsv_bytes, PRIVATE_KEY_PATH)

        return encrypted_response, 200

    except (json.JSONDecodeError, UnicodeDecodeError, ValueError) as e:
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