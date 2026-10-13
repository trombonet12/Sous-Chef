"""Crea la keystore de release de Sous Chef en ~/.souschef/ (una sola vez) sin imprimir secretos.

Genera contraseñas aleatorias, llama a keytool y escribe ~/.souschef/release-signing.properties con
SOUSCHEF_KEYSTORE_FILE, SOUSCHEF_KEYSTORE_PASSWORD, SOUSCHEF_KEY_ALIAS y SOUSCHEF_KEY_PASSWORD.
Si la keystore ya existe, no hace nada: perderla impide actualizar la app instalada.
Uso: python -I scripts/crear-keystore.py
"""

import os
import pathlib
import secrets
import shutil
import subprocess
import sys

DIR = pathlib.Path.home() / ".souschef"
KEYSTORE = DIR / "souschef-release.jks"
PROPS = DIR / "release-signing.properties"
ALIAS = "souschef"


def keytool() -> str:
    for base in (os.environ.get("JAVA_HOME"), "C:/Program Files/Microsoft/jdk-21.0.12.101-hotspot"):
        if base and (pathlib.Path(base) / "bin" / "keytool.exe").is_file():
            return str(pathlib.Path(base) / "bin" / "keytool.exe")
    encontrado = shutil.which("keytool")
    if not encontrado:
        sys.exit("No encuentro keytool (JDK).")
    return encontrado


def main() -> None:
    if KEYSTORE.exists():
        print(f"Ya existe {KEYSTORE.name}: no se toca.")
        return
    DIR.mkdir(parents=True, exist_ok=True)
    clave = secrets.token_urlsafe(24)
    subprocess.run(
        [
            keytool(), "-genkeypair", "-v", "-keystore", str(KEYSTORE), "-storetype", "PKCS12",
            "-alias", ALIAS, "-keyalg", "RSA", "-keysize", "4096", "-validity", "10000",
            "-dname", "CN=Sous Chef, O=Joan Lopez Ferrer, C=ES",
            "-storepass:env", "SC_STOREPASS", "-keypass:env", "SC_STOREPASS",
        ],
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        env={**os.environ, "SC_STOREPASS": clave},
    )
    # PKCS12: la clave de la entrada es la misma que la del almacén.
    PROPS.write_text(
        "\n".join(
            [
                f"SOUSCHEF_KEYSTORE_FILE={KEYSTORE.as_posix()}",
                f"SOUSCHEF_KEYSTORE_PASSWORD={clave}",
                f"SOUSCHEF_KEY_ALIAS={ALIAS}",
                f"SOUSCHEF_KEY_PASSWORD={clave}",
            ]
        )
        + "\n",
        encoding="utf-8",
    )
    print(f"Creada {KEYSTORE.name} y {PROPS.name} en ~/.souschef (valores no mostrados). Haz copia de seguridad.")


if __name__ == "__main__":
    main()
