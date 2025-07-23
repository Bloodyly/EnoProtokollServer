from Crypto.Cipher import AES
from Crypto.Util.Padding import pad, unpad

def encrypt_payload(data, key):
    cipher = AES.new(key, AES.MODE_ECB)
    encrypted = cipher.encrypt(pad(data.encode(), 16))
    return encrypted

def decrypt_payload(data, key):
    cipher = AES.new(key, AES.MODE_ECB)
    decrypted = unpad(cipher.decrypt(data), 16)
    return decrypted.decode()
