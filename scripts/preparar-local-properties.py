"""Genera local.properties de Sous Chef sin imprimir valores.

- sdk.dir: %LOCALAPPDATA%/Android/Sdk (o ANDROID_HOME).
- supabase.url y supabase.publishableKey: se copian de C:/Github/Trombito/apps/android/local.properties
  (mismo proyecto Supabase, ADR-0002). Si no existe, se dejan vacías (la app compila igual).
Uso: python -I scripts/preparar-local-properties.py
"""

import os
import pathlib

RAIZ = pathlib.Path(__file__).resolve().parent.parent
DESTINO = RAIZ / "local.properties"
TROMBITO = pathlib.Path("C:/Github/Trombito/apps/android/local.properties")
CLAVES = ("supabase.url", "supabase.publishableKey")


def leer(path: pathlib.Path) -> dict:
    datos = {}
    if path.is_file():
        for linea in path.read_text(encoding="utf-8").splitlines():
            if "=" in linea and not linea.lstrip().startswith("#"):
                k, v = linea.split("=", 1)
                datos[k.strip()] = v.strip()
    return datos


def main() -> None:
    actual = leer(DESTINO)
    sdk = os.environ.get("ANDROID_HOME") or str(pathlib.Path(os.environ["LOCALAPPDATA"]) / "Android" / "Sdk")
    actual.setdefault("sdk.dir", sdk.replace("\\", "/"))
    origen = leer(TROMBITO)
    for k in CLAVES:
        if not actual.get(k) and origen.get(k):
            actual[k] = origen[k]
    lineas = ["# Generado por scripts/preparar-local-properties.py. No se versiona (secretos)."]
    lineas += [f"{k}={v}" for k, v in actual.items()]
    DESTINO.write_text("\n".join(lineas) + "\n", encoding="utf-8")
    estado = {k: ("ok" if actual.get(k) else "vacía") for k in ("sdk.dir", *CLAVES)}
    print("local.properties:", ", ".join(f"{k} {v}" for k, v in estado.items()))


if __name__ == "__main__":
    main()
