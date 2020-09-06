from base64 import b64decode


def desanitize(b64hash: str):
    return b64decode(b64hash).decode()
