from flask import Flask, render_template, request, redirect, url_for, session, flash, send_from_directory
from utils import ensure_data_json, refresh_data, update_from_protokolle   # wenn du es ausgelagert hast
from functools import wraps
import os
import json
import logging
app = Flask(__name__)
app.secret_key = "#snfl=&smnmc_29nd9Ndlc_$cM"

ensure_data_json()
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Pfade definieren
SHARED_PATH = "/app/shared"
DATA_PATH = os.path.join(SHARED_PATH,"data.json")
USER_FILE = os.path.join(SHARED_PATH,"users.txt")
EXPOSED_FOLDER = os.path.join(SHARED_PATH, "Expose")
LISTEN_DIR = os.path.join(EXPOSED_FOLDER, "Listen")
PROTOKOLL_DIR = os.path.join(EXPOSED_FOLDER, "Protokolle")
ARCHIV_DIR = os.path.join(EXPOSED_FOLDER, "Archiv")
    
#----------------------------------------#
# Functions -----------------------------#
#----------------------------------------#
def load_data_json():
    try:
        with open(DATA_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        print(f"[ERROR] Konnte JSON nicht laden: {e}")
        return {"vn": []}
        
def get_users():
    default = {"admin": "ad-123456"}
    if not os.path.exists(USER_FILE):
        return default

    users = {}
    with open(USER_FILE, "r") as f:
        for line in f:
            if ":" in line:
                username, password = line.strip().split(":", 1)
                users[username] = password
    return users

def login_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if "user" not in session:
            return redirect(url_for("login"))
        return f(*args, **kwargs)
    return decorated
    
# Automatisch `data` in jedem Template verfügbar machen
@app.context_processor
def inject_data():
    if os.path.exists(DATA_PATH):
        try:
            with open(DATA_PATH, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception:
            data = {}
    return {
        "data": data,
        "vertragsdaten": data.get("vn", [])
    }

#----------------------------------------#
# App Routes ----------------------------#
#----------------------------------------#

@app.route("/")
@login_required
def index():
    return render_template("index.html", active_page="dashboard")

@app.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "POST":
        users = get_users()
        username = request.form.get("username")
        password = request.form.get("password")
        if users.get(username) == password:
            session["user"] = username
            return redirect(url_for("index"))
        else:
            flash("Login fehlgeschlagen. Bitte prüfen Sie Benutzername und Passwort.")
            return redirect(url_for("login"))
    return render_template("login.html")
    
@app.route("/logout")
@login_required
def logout():
    session.clear()
    return redirect(url_for("login"))

@app.route("/vertraege")
@login_required
def vertraege():
    data = load_data_json()
    vn_nr = request.args.get("vn")
    selected_vn = None
    
    if vn_nr:
        selected_vn = next((vn for vn in data.get("vn", []) if vn["vn_nr"] == vn_nr), None)

    return render_template(
        "vertraege.html",
        vertragsdaten=data.get("vn", []),
        selected_vn=selected_vn,
        active_page="vertraege"
        )

@app.route("/archiv")
@login_required
def archiv():
    return render_template("archiv.html", active_page="archiv")

@app.route("/statistiken")
@login_required
def statistiken():
    return render_template("statistiken.html", active_page="statistiken")

@app.route("/user")
@login_required
def user():
    return render_template("user.html", users=get_users(), active_page="users")

@app.route("/add_user", methods=["POST"])
@login_required
def add_user():
    username = request.form.get("username")
    password = request.form.get("password")
    if not username or not password:
        flash("Fehlende Eingaben")
        return redirect(url_for("users"))

    users = get_users()
    if username in users:
        flash("Benutzer existiert bereits.")
    else:
        with open(USER_FILE, "a") as f:
            f.write(f"{username}:{password}\n")
        flash(f"Benutzer '{username}' hinzugefügt.")

    return redirect(url_for("user"))

@app.route("/delete_user", methods=["POST"])
@login_required
def delete_user():
    username = request.form.get("username")
    if username == "admin":
        flash("Admin kann nicht gelöscht werden.")
        return redirect(url_for("user"))

    users = get_users()
    if username not in users:
        flash("Benutzer nicht gefunden.")
    else:
        with open(USER_FILE, "w") as f:
            for user, pw in users.items():
                if user != username:
                    f.write(f"{user}:{pw}\n")
        flash(f"Benutzer '{username}' gelöscht.")

    return redirect(url_for("users"))

@app.route("/makedb")
@login_required
def makedb():
    refresh_data()
    update_from_protokolle()
    return render_template("index.html")
    
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
