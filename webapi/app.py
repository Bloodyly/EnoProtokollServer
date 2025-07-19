from flask import Flask, request
import openpyxl
import os
from datetime import datetime

app = Flask(__name__)
UPLOAD_FOLDER = "/app/shared"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route("/upload", methods=["POST"])
def upload():
    data = request.get_data()
    
    # Beispiel: dekodierte Werte in Tabellenform
    rows = [("Zeit", "Wert"), (datetime.now().isoformat(), data.hex()[:16])]

    filename = f"tabelle_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx"
    filepath = os.path.join(UPLOAD_FOLDER, filename)

    wb = openpyxl.Workbook()
    ws = wb.active
    for row in rows:
        ws.append(row)
    wb.save(filepath)

    return {"status": "ok", "file": filename}, 200

@app.route("/", methods=["GET"])
def index():
    return "EnoProtokollServer API bereit", 200
