import os
import json
from datetime import datetime
from openpyxl import load_workbook

LISTEN_DIR = "app/shared/exposed/Listen"
PROTOKOLL_DIR = "app/shared/exposed/Protokolle"
DATA_PATH = "/app/shared/data.json"

def ensure_data_json():
    if not os.path.exists(DATA_PATH):
        print("[INFO] data.json nicht gefunden – neue Datei wird erstellt.")
        with open(DATA_PATH, "w") as f:
            json.dump({"vn": {}}, f, indent=2)
    else:
        print("[INFO] data.json vorhanden.")

def refresh_data():
    data = {"vn": []}

    if not os.path.exists(LISTEN_DIR):
        print(f"[WARN] Verzeichnis {LISTEN_DIR} nicht gefunden.")
        return

    for filename in os.listdir(LISTEN_DIR):
        if not filename.endswith(".xlsx"):
            continue

        file_path = os.path.join(LISTEN_DIR, filename)
        wb = load_workbook(file_path)

        # 🧠 Erste Seite analysieren für Meta-Daten
        sheet = wb[wb.sheetnames[0]]
        kunde = ""
        vn_nr = ""
        wartung = "Quartal"  # Standardwert

        try:
            vn_nr = str(sheet["B1"].value).strip()
            kunde = str(sheet["B2"].value).strip()
            wartung = str(sheet["B3"].value).strip()
        except Exception as e:
            print(f"[WARN] Fehler beim Lesen von {filename}: {e}")
            continue

        if not vn_nr or not kunde:
            print(f"[WARN] Datei {filename} enthält keine gültigen Meta-Informationen.")
            continue

        # 🕒 Quartal bestimmen
        monat = datetime.now().month
        if wartung == "Quartal":
            status = f"Q{(monat - 1) // 3 + 1}"
        elif wartung == "Halbjahr":
            status = "H1" if monat <= 6 else "H2"
        else:
            status = "i.O."

        # 📄 Alle Objekte (Sheets) analysieren
        objekte = []
        for sheet_name in wb.sheetnames:
            sheet = str(sheet["B4"].value).strip()
            melder_summe = 0
            meldegruppen = 0

            for row in sheet.iter_rows(min_row=8, min_col=2, max_col=2):
                zelle = row[0].value
                if zelle is None or str(zelle).strip() == "":
                    break
                try:
                    melder_summe += int(zelle)
                    meldegruppen += 1
                except ValueError:
                    continue

            objekte.append({
                "name": sheet_name.strip(),
                "melder_anzahl": melder_summe,
                "melder_ausgeloest": 0,
                "meldegruppen": meldegruppen
            })

        # 🧾 In Struktur eintragen
        data["vn"].append({
            "vn_nr": vn_nr,
            "kunde": kunde,
            "filename" : filename,
            "wartung": wartung,
            "status": status,
            "letzte_bearbeitung": datetime.now().isoformat(),
            "objekte": objekte
        })

    # 💾 Speichern
    with open(DATA_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print(f"[INFO] {len(data['vn'])} Verträge wurden eingelesen und gespeichert.")
 
def update_ausgeloest():
    if not os.path.exists(DATA_PATH):
        print("[WARN] Keine data.json vorhanden – Abbruch.")
        return

    # Daten einlesen
    with open(DATA_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    updated_vn_count = 0

    for filename in os.listdir(PROTOKOLL_DIR):
        if not filename.endswith(".xlsx"):
            continue

        file_path = os.path.join(PROTOKOLL_DIR, filename)
        try:
            wb = load_workbook(file_path)
        except Exception as e:
            print(f"[ERROR] Fehler beim Laden von {filename}: {e}")
            continue

        # VN aus Inhalt extrahieren
        sheet = wb[wb.sheetnames[0]]
        try: 
            vn_nr = str(sheet["B1"].value).strip()
        except Exception as e:
            vn_nr = ""
        
        if not vn_nr:
            print(f"[WARN] Vertragsnummer in {filename} nicht gefunden.")
            continue

        # Suche nach passender VN in data.json
        vn_eintrag = next((vn for vn in data["vn"] if vn["vn_nr"] == vn_nr), None)
        if not vn_eintrag:
            print(f"[WARN] VN {vn_nr} aus {filename} nicht in data.json vorhanden.")
            continue

        # Update pro Objekt/Sheet
        for sheet_name in wb.sheetnames:
            if sheet_name not in [obj["name"] for obj in vn_eintrag["objekte"]]:
                continue

            ws = wb[sheet_name]
            count = 0
            for row in ws.iter_rows(min_row=8):
                for cell in row[3:]:  # ab Spalte D (Index 3)
                    value = str(cell.value).strip() if cell.value is not None else ""
                    if value in ("Q1", "Q2", "Q3", "Q4", "H1", "H2", "i.O.", "n.i.O."):
                        count += 1

            for obj in vn_eintrag["objekte"]:
                if obj["name"] == sheet_name:
                    obj["melder_ausgeloest"] = count
                    break

        updated_vn_count += 1

    # Speichern
    with open(DATA_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print(f"[INFO] {updated_vn_count} VN-Einträge wurden mit ausgelösten Meldern aktualisiert.")
