import base64
import gzip
import json
import sys
import hashlib
from getpass import getpass
from typing import Tuple

import requests
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad, unpad

DEFAULT_URL = "http://localhost:5000/get_protokoll"

def parse_key(key_input: str) -> bytes:
    """
    Nimmt entweder einen Roh-Schlüssel (16/24/32 Bytes als Latin1-ähnlicher String)
    ODER einen Base64-codierten Schlüssel entgegen. Gibt AES-Key-Bytes zurück.
    """
    s = key_input.strip()
    # 1) zuerst Base64 versuchen
    try:
        raw = base64.b64decode(s, validate=True)
        if len(raw) in (16, 24, 32):
            return raw
    except Exception:
        pass
    # 2) als "roher" Text interpretieren
    raw = s.encode("utf-8")
    if len(raw) in (16, 24, 32):
        return raw
    raise ValueError("Ungültiger PRIVATE_KEY. Erwartet 16/24/32 Byte oder gültiges Base64 für 16/24/32 Byte.")

def aes_encrypt_ecb_pkcs7(data: bytes, key: bytes) -> bytes:
    cipher = AES.new(key, AES.MODE_ECB)
    return cipher.encrypt(pad(data, 16))

def aes_decrypt_ecb_pkcs7(data: bytes, key: bytes) -> bytes:
    cipher = AES.new(key, AES.MODE_ECB)
    return unpad(cipher.decrypt(data), 16)

def prompt_inputs() -> Tuple[str, bytes, str, str, str]:
    url = input(f"Server-URL [{DEFAULT_URL}]: ").strip() or DEFAULT_URL
    key_in = input("PRIVATE_KEY (Base64 oder roh, wird nicht angezeigt): ").strip()
    key = parse_key(key_in)
    print_key_info(key)
    username = input("Benutzername: ").strip()
    password = getpass("Passwort (wird nicht angezeigt): ").strip()
    vn = input("VN (z.B. VN123456): ").strip()
    if not vn.upper().startswith("VN"):
        vn = "VN" + vn
    return url, key, username, password, vn

def print_key_info(key: bytes):
    b64 = base64.b64encode(key).decode("utf-8")
    sha = hashlib.sha256(key).hexdigest()
    print(f"[CLIENT] PRIVATE_KEY(Base64)={b64}  len={len(key)}  sha256={sha}")

def main():
    try:
        url, key, username, password, vn = prompt_inputs()
    except Exception as e:
        print(f"[Eingabe-Fehler] {e}")
        sys.exit(1)

    # 1) Anfragekörper (JSON) bauen und VERSCHLÜSSELN
    req_obj = {
        "username": username,
        "password": password,
        "vn": vn
    }
    req_json = json.dumps(req_obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    enc_body = aes_encrypt_ecb_pkcs7(req_json, key)

    print("\n→ Sende verschlüsselte Anfrage …")
    try:
        resp = requests.post(url, data=enc_body, timeout=30)
    except Exception as e:
        print(f"[HTTP-Fehler] {e}")
        sys.exit(1)

    print(f"← HTTP {resp.status_code}")
    if resp.status_code != 200:
        # Versuch, Fehlermeldung zu lesen (dein Server gibt bei Fehlern JSON zurück)
        try:
            print(resp.json())
        except Exception:
            print(resp.text[:500])
        sys.exit(1)

    # 2) Antwort entschlüsseln
    cipher = resp.content
    try:
        plain = aes_decrypt_ecb_pkcs7(cipher, key)
    except Exception as e:
        print(f"[Decrypt-Fehler] {e}")
        sys.exit(1)

    # 3) Optional GZIP entpacken
    compressed_hdr = (resp.headers.get("X-Content-Compressed", "") or "").lower()
    if compressed_hdr == "gzip" or (len(plain) >= 2 and plain[:2] == b"\x1f\x8b"):
        try:
            plain = gzip.decompress(plain)
            print("(entpackt: gzip)")
        except Exception as e:
            print(f"[GZIP-Fehler] {e}")
            # nicht abbrechen – evtl. war es doch nicht gzip

    x_fmt = (resp.headers.get("X-Format", "") or "").lower()

    print("\n=== Server-Header ===")
    for k in ["X-Format", "X-Content-Compressed", "X-Original-Size", "Cache-Control"]:
        if k in resp.headers:
            print(f"{k}: {resp.headers[k]}")

    print("\n=== Inhalt ===")
    if x_fmt == "json":
        try:
            obj = json.loads(plain.decode("utf-8"))
            print(json.dumps(obj, ensure_ascii=False, indent=2))
        except Exception as e:
            print(f"[JSON-Parse-Fehler] {e}")
            print(plain[:500])
    else:
        # Standard: TSV text/plain (UTF-8)
        try:
            text = plain.decode("utf-8")
        except UnicodeDecodeError:
            text = plain.decode("latin-1", errors="replace")

        # Einen kleinen Parser/Viewer:
        lines = text.strip().splitlines()
        # Zeige max. 80 Zeilen, damit die Konsole nicht explodiert
        preview = 80
        for i, line in enumerate(lines[:preview], 1):
            print(line)
        if len(lines) > preview:
            print(f"\n… {len(lines)-preview} weitere Zeilen nicht angezeigt")

        # Kleines Summary:
        anlagen = sum(1 for l in lines if l.startswith("#ANLAGE"))
        gm_rows = sum(1 for l in lines if not l.startswith("#") and "\t" in l)
        print(f"\n[Summary] Anlagen: {anlagen} · Datenzeilen: {gm_rows}")

if __name__ == "__main__":
    main()
