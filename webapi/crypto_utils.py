from Crypto.Cipher import AES
from Crypto.Util.Padding import pad, unpad

def encrypt_payload(data, key: bytes) -> bytes:
    """
    data: str ODER bytes
    Rückgabe: verschlüsselte bytes
    """
    if isinstance(data, str):
        data_bytes = data.encode("utf-8")
    else:
        data_bytes = data  # bereits bytes (z.B. gzipped TSV/JSON)
    cipher = AES.new(key, AES.MODE_ECB)
    return cipher.encrypt(pad(data_bytes, 16))

def decrypt_payload(data: bytes, key: bytes) -> bytes:
    """
    Rückgabe: K L A R T E X T  als bytes (NICHT .decode())
    - Für JSON-Text: caller macht .decode('utf-8')
    - Für gzipped Daten: caller entpackt als bytes
    """
    cipher = AES.new(key, AES.MODE_ECB)
    return unpad(cipher.decrypt(data), 16)