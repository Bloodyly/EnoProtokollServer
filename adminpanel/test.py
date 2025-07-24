from flask import Flask, render_template,  request, redirect, url_for, session, flash
from functools import wraps
import os
import json
app = Flask(__name__)
app.secret_key = "#snfl=&smnmc_29nd9Ndlc_$cM"

USER_FILE = "/app/shared/users.txt"
SHARED_DATA_PATH = "/app/shared/data.json"
DEMO_DATA_PATH = "static/demo.json"

def load_data_json():
    path = SHARED_DATA_PATH if os.path.exists(SHARED_DATA_PATH) else DEMO_DATA_PATH
    try:
        with open(path, "r", encoding="utf-8") as f:
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
def logout():
    session.clear()
    return redirect(url_for("login"))

@app.route("/")
@login_required
def index():
    data = load_data_json()
    return render_template("index.html", vertragsdaten=data["vn"], active_page="dashboard")

@app.route("/vertraege")
@login_required
def vertraege():
    data = load_data_json()
    return render_template("vertraege.html", vertragsdaten=data["vn"], active_page="vertraege")

@app.route("/archiv")
@login_required
def archiv():
    data = load_data_json()
    return render_template("archiv.html", vertragsdaten=data["vn"], active_page="archiv")

@app.route("/statistiken")
@login_required
def statistiken():
    data = load_data_json()
    return render_template("statistiken.html", vertragsdaten=data["vn"], active_page="statistiken")

@app.route("/user")
@login_required
def user():
    data = load_data_json()
    return render_template("user.html", vertragsdaten=data["vn"], users=get_users(), active_page="users")

@app.route("/add_user", methods=["POST"])
@login_required
def add_user():
    data = load_data_json()
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

    return redirect(url_for("users"))

@app.route("/delete_user", methods=["POST"])
@login_required
def delete_user():
    username = request.form.get("username")
    if username == "admin":
        flash("Admin kann nicht gelöscht werden.")
        return redirect(url_for("users"))

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


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5080, debug=True)
