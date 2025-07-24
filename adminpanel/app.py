from flask import Flask, render_template, send_from_directory
from utils import ensure_data_json, refresh_data, update_ausgeloest   # wenn du es ausgelagert hast
import os
import json
import logging
app = Flask(__name__)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Pfade definieren
SHARED_PATH = "/app/shared"
DATA_PATH ="/app/shared/data.json"
EXPOSED_FOLDER = os.path.join(SHARED_PATH, "Expose")
LISTEN_DIR = os.path.join(EXPOSED_FOLDER, "Listen")
PROTOKOLL_DIR = os.path.join(EXPOSED_FOLDER, "Protokolle")
ARCHIV_DIR = os.path.join(EXPOSED_FOLDER, "Archiv")
DATA_PATH = os.path.join(SHARED_PATH, "data.json")

def get_dashboard_stats():
    if not os.path.exists(DATA_PATH):
        return {}

    with open(DATA_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    listen = len(data["vn"])
    protokolle = len(os.listdir(PROTOKOLL_DIR)) if os.path.exists(PROTOKOLL_DIR) else 0
    anlagen = 0
    meldegruppen = 0
    melder = 0
    ausgelöst = 0

    for eintrag in data["vn"]:
        for obj in eintrag["objekte"]:
            anlagen += 1
            meldegruppen += obj.get("meldegruppen", 0)
            melder += obj.get("melder_anzahl", 0)
            ausgelöst += obj.get("melder_ausgeloest", 0)

    return {
        "listen": listen,
        "protokolle": protokolle,
        "anlagen": anlagen,
        "meldegruppen": meldegruppen,
        "melder": melder,
        "ausgeloest": ausgelöst
    }
    
#----------------------------------------#
# App Routes ----------------------------#
#----------------------------------------#

@app.route("/")
def index():
    stats = get_dashboard_stats()
    return render_template("index.html", stats=stats)

@app.route("/protokolle")
def show_protokolle():
    files = os.listdir(PROTOKOLL_DIR)
    files = [f for f in files if f.endswith(".xlsx")]
    return render_template("protokolle.html", files=files)

@app.route("/protokolle/download/<filename>")
def download_file(filename):
    return send_from_directory(os.path.join(PROTOKOLL_DIR), filename)

@app.route("/makedb")
def makedb():
    ensure_data_json()
    refresh_data()
    update_ausgeloest()
    stats = get_dashboard_stats()
    return render_template("index.html", stats=stats)
    
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
    