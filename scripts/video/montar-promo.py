#!/usr/bin/env python3
"""Monta el video promocional vertical de Sous Chef (Google Play / YouTube).

Salida: build/promo/sous-chef-promo.mp4  (1080x1920, 30 fps, H.264 yuv420p, faststart, sin audio, ~30 s)
Temporales: build/promo/tmp/            (todo `build/` esta ignorado por git)

Estructura: intro (mascota + nombre) -> 5 tramos de la app con rotulo -> cierre (mascota + lema).
Cada tramo usa el clip real build/promo/clips/<id>.mp4; si no existe, la captura equivalente de
distribution/listing/graphics/phone/ con zoom suave. Sin dependencias nuevas: ffmpeg, Pillow y Chrome.

Uso (desde la raiz del repo): python -I scripts/video/montar-promo.py
"""

from __future__ import annotations

import json
import math
import os
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

# ----------------------------------------------------------------------------------------------
# Parametros (retocar aqui)
# ----------------------------------------------------------------------------------------------
ANCHO, ALTO, FPS = 1080, 1920, 30

INTRO_S = 2.0  # duracion de la intro
CIERRE_S = 3.0  # duracion del cierre
SEG_S = 5.5  # duracion maxima de cada tramo de app
SEG_MIN_S = 4.0  # duracion minima (si el clip es mas corto se congela el ultimo fotograma)
TRANSICION_S = 0.4  # xfade entre piezas
TRANSICION_INTRO_CIERRE = "fade"  # intro -> tramo 1 y tramo 5 -> cierre
TRANSICION_ENTRE_TRAMOS = "slideleft"  # entre tramos de app

TITULO = "Sous Chef"
LEMA = "Sin cuentas \u00b7 Sin anuncios \u00b7 Sin conexi\u00f3n"

# (id del clip, captura de relleno, rotulo, segundo de inicio dentro del clip)
TRAMOS = [
    ("c1-recetas", "01-recetas.png", "Elige por categor\u00eda", 0.0),
    ("c2-ficha", "02-ficha.png", "Te dice a qu\u00e9 hora empezar", 0.0),
    ("c3-cronometro", "03-cronometro.png", "Un cron\u00f3metro por recipiente", 0.0),
    ("c4-tecnicas", "04-tecnicas.png", "T\u00e9cnicas paso a paso", 0.0),
    ("c5-historial", "05-historial.png", "Lo que has cocinado, guardado", 0.0),
]

# Rotulo superior
ROTULO_FUENTE_PX = 56
ROTULO_Y = 22  # borde superior de la barra: sobre la barra de estado, sin tapar la barra de la app
ROTULO_PAD_X, ROTULO_PAD_Y = 44, 26
ROTULO_FONDO = (23, 21, 15, 215)  # tinta semitransparente
ROTULO_TEXTO = (243, 239, 228, 255)  # papel
ROTULO_ENTRA_S, ROTULO_FADE_S = 0.35, 0.3

# Paleta (ui/theme/Color.kt, FichaPalette)
PAPEL = (0xF3, 0xEF, 0xE4)
TINTA = (0x17, 0x15, 0x0F)
TINTA2 = (0x4E, 0x48, 0x3C)
AMBAR = (0x8C, 0x5A, 0x04)

# ----------------------------------------------------------------------------------------------
# Rutas
# ----------------------------------------------------------------------------------------------
RAIZ = Path(__file__).resolve().parents[2]
PROMO = RAIZ / "build" / "promo"
CLIPS = PROMO / "clips"
TMP = PROMO / "tmp"
SALIDA = PROMO / "sous-chef-promo.mp4"
CAPTURAS = RAIZ / "distribution" / "listing" / "graphics" / "phone"
ICONO_SVG = RAIZ / "docs" / "assets" / "icono.svg"
CONTROL_SEGUNDOS = (1, 8, 18, 29)

FUENTES_NEGRITA = [
    "C:/Windows/Fonts/segoeuib.ttf",
    "C:/Windows/Fonts/arialbd.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/roboto/Roboto-Bold.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
]
FUENTES_NORMAL = [
    "C:/Windows/Fonts/segoeui.ttf",
    "C:/Windows/Fonts/arial.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/roboto/Roboto-Regular.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
]
CHROME_CANDIDATOS = [
    "C:/Program Files/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
    "/usr/bin/google-chrome",
    "/usr/bin/google-chrome-stable",
    "/usr/bin/chromium",
    "/usr/bin/chromium-browser",
]


# ----------------------------------------------------------------------------------------------
# Utilidades
# ----------------------------------------------------------------------------------------------
def fallar(msg: str) -> None:
    print(f"montar-promo: {msg}", file=sys.stderr)
    sys.exit(1)


def ejecutar(cmd: list[str]) -> str:
    r = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        fallar(f"fallo el comando:\n  {' '.join(cmd)}\n{r.stderr[-3000:]}")
    return r.stdout


def ffmpeg(*args: str) -> None:
    ejecutar(["ffmpeg", "-y", "-hide_banner", "-loglevel", "error", *args])


def duracion(ruta: Path) -> float:
    out = ejecutar(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "json", str(ruta)]
    )
    return float(json.loads(out)["format"]["duration"])


def cargar_fuente(candidatas: list[str], px: int) -> ImageFont.FreeTypeFont:
    for c in candidatas:
        if os.path.exists(c):
            return ImageFont.truetype(c, px)
    return ImageFont.load_default(px)


def ease_out_cubic(t: float) -> float:
    t = min(max(t, 0.0), 1.0)
    return 1 - (1 - t) ** 3


def ease_in_out(t: float) -> float:
    t = min(max(t, 0.0), 1.0)
    return 0.5 - 0.5 * math.cos(math.pi * t)


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


# ----------------------------------------------------------------------------------------------
# Mascota: icono.svg sin fondo -> PNG transparente con Chrome headless
# ----------------------------------------------------------------------------------------------
def localizar_chrome() -> str:
    env = os.environ.get("CHROME")
    if env and os.path.exists(env):
        return env
    for c in CHROME_CANDIDATOS:
        if os.path.exists(c):
            return c
    fallar("no se encuentra Chrome; define la variable CHROME con la ruta del ejecutable")
    return ""


def renderizar_mascota() -> Image.Image:
    import re

    ancho, alto = 912, 992  # viewBox 24 24 57 62 (la mascota con gorro, sin margen)
    svg = ICONO_SVG.read_text(encoding="utf-8")
    svg = re.sub(r' width="\d+" height="\d+"', f' width="{ancho}" height="{alto}"', svg, count=1)
    svg = re.sub(r'viewBox="[^"]*"', 'viewBox="24 24 57 62"', svg, count=1)
    svg = re.sub(r"<rect [^>]*/>", "", svg, count=1)
    html = (
        '<!doctype html><html><head><meta charset="utf-8"><style>'
        "html,body{margin:0;padding:0;background:transparent;overflow:hidden}svg{display:block}"
        f"</style></head><body>{svg}</body></html>"
    )
    archivo_html = TMP / "mascota.html"
    archivo_html.write_text(html, encoding="utf-8")
    png = TMP / "mascota.png"
    png.unlink(missing_ok=True)
    perfil = TMP / "chrome-perfil"
    ejecutar(
        [
            localizar_chrome(),
            "--headless=new",
            "--disable-gpu",
            "--hide-scrollbars",
            "--force-device-scale-factor=1",
            "--default-background-color=00000000",
            f"--user-data-dir={perfil}",
            f"--window-size={ancho},{alto}",
            f"--screenshot={png}",
            archivo_html.as_uri(),
        ]
    )
    if not png.exists():
        fallar("Chrome no genero la mascota")
    img = Image.open(png).convert("RGBA")
    bbox = img.getchannel("A").getbbox()
    if bbox is None:
        fallar("la mascota salio vacia (sin canal alfa)")
    return img.crop(bbox)


# ----------------------------------------------------------------------------------------------
# Intro y cierre (fotograma a fotograma con Pillow)
# ----------------------------------------------------------------------------------------------
def componer(base: Image.Image, capa: Image.Image, centro: tuple[float, float], alfa: float = 1.0) -> None:
    if alfa < 1.0:
        capa = capa.copy()
        capa.putalpha(capa.getchannel("A").point(lambda v: int(v * alfa)))
    x = int(round(centro[0] - capa.width / 2))
    y = int(round(centro[1] - capa.height / 2))
    base.alpha_composite(capa, (x, y))


def texto_capa(texto: str, fuente: ImageFont.FreeTypeFont, color: tuple[int, int, int]) -> Image.Image:
    caja = fuente.getbbox(texto)
    capa = Image.new("RGBA", (caja[2] - caja[0] + 8, caja[3] - caja[1] + 24), (0, 0, 0, 0))
    ImageDraw.Draw(capa).text((4 - caja[0], 12 - caja[1]), texto, font=fuente, fill=color + (255,))
    return capa


def ajustar_fuente(candidatas: list[str], texto: str, px: int, ancho_max: int) -> ImageFont.FreeTypeFont:
    while px > 20:
        f = cargar_fuente(candidatas, px)
        if f.getlength(texto) <= ancho_max:
            return f
        px -= 2
    return cargar_fuente(candidatas, px)


def escalar(mascota: Image.Image, ancho: float) -> Image.Image:
    alto = ancho * mascota.height / mascota.width
    return mascota.resize((max(1, round(ancho)), max(1, round(alto))), Image.LANCZOS)


def generar_intro(mascota: Image.Image, destino: Path) -> None:
    carpeta = TMP / "intro"
    shutil.rmtree(carpeta, ignore_errors=True)
    carpeta.mkdir(parents=True)
    n = round(INTRO_S * FPS)
    base_w = 620
    cy = 800
    fuente = ajustar_fuente(FUENTES_NEGRITA, TITULO, 160, 900)
    titulo = texto_capa(TITULO, fuente, TINTA)
    raya = Image.new("RGBA", (96, 8), AMBAR + (255,))
    for i in range(n):
        t = i / FPS
        if t < 0.55:
            esc = lerp(0.6, 1.05, ease_out_cubic(t / 0.55))
        elif t < 0.85:
            esc = lerp(1.05, 1.0, ease_in_out((t - 0.55) / 0.30))
        else:
            esc = 1.0
        alfa_m = min(1.0, t / 0.15)
        cuadro = Image.new("RGBA", (ANCHO, ALTO), PAPEL + (255,))
        componer(cuadro, escalar(mascota, base_w * esc), (ANCHO / 2, cy), alfa_m)
        p = ease_out_cubic((t - 0.7) / 0.5)  # el nombre aparece a partir de 0.7 s
        componer(cuadro, titulo, (ANCHO / 2, 1250 + (1 - p) * 50), p)
        componer(cuadro, raya, (ANCHO / 2, 1370), p)
        cuadro.convert("RGB").save(carpeta / f"f{i:04d}.png")
    codificar_frames(carpeta, destino)


def generar_cierre(mascota: Image.Image, destino: Path) -> None:
    carpeta = TMP / "cierre"
    shutil.rmtree(carpeta, ignore_errors=True)
    carpeta.mkdir(parents=True)
    n = round(CIERRE_S * FPS)
    fuente = ajustar_fuente(FUENTES_NEGRITA, TITULO, 150, 900)
    titulo = texto_capa(TITULO, fuente, TINTA)
    fuente_lema = ajustar_fuente(FUENTES_NORMAL, LEMA, 52, 880)
    lema = texto_capa(LEMA, fuente_lema, TINTA2)
    raya = Image.new("RGBA", (96, 8), AMBAR + (255,))
    mascota_g = escalar(mascota, 560)
    for i in range(n):
        t = i / FPS
        cuadro = Image.new("RGBA", (ANCHO, ALTO), PAPEL + (255,))
        componer(cuadro, mascota_g, (ANCHO / 2, 720))
        componer(cuadro, titulo, (ANCHO / 2, 1130))
        componer(cuadro, raya, (ANCHO / 2, 1240))
        p = ease_out_cubic((t - 0.3) / 0.5)
        componer(cuadro, lema, (ANCHO / 2, 1340 + (1 - p) * 24), p)
        cuadro.convert("RGB").save(carpeta / f"f{i:04d}.png")
    codificar_frames(carpeta, destino)


def codificar_frames(carpeta: Path, destino: Path) -> None:
    ffmpeg(
        "-framerate", str(FPS), "-i", str(carpeta / "f%04d.png"),
        "-vf", "format=yuv420p,settb=AVTB,setsar=1",
        "-r", str(FPS), "-c:v", "libx264", "-preset", "veryfast", "-crf", "12",
        "-pix_fmt", "yuv420p", "-an", str(destino),
    )  # fmt: skip


# ----------------------------------------------------------------------------------------------
# Rotulos y tramos de app
# ----------------------------------------------------------------------------------------------
def generar_rotulo(texto: str, destino: Path) -> None:
    fuente = ajustar_fuente(FUENTES_NEGRITA, texto, ROTULO_FUENTE_PX, ANCHO - 2 * ROTULO_PAD_X - 80)
    caja = fuente.getbbox(texto)
    ancho = (caja[2] - caja[0]) + 2 * ROTULO_PAD_X
    alto = (caja[3] - caja[1]) + 2 * ROTULO_PAD_Y
    esc = 3  # supersampling para bordes suaves
    img = Image.new("RGBA", (ancho * esc, alto * esc), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, ancho * esc - 1, alto * esc - 1), radius=alto * esc // 2, fill=ROTULO_FONDO)
    img = img.resize((ancho, alto), Image.LANCZOS)
    ImageDraw.Draw(img).text(
        (ROTULO_PAD_X - caja[0], ROTULO_PAD_Y - caja[1]), texto, font=fuente, fill=ROTULO_TEXTO
    )
    img.save(destino)


def montar_tramo(indice: int, clip_id: str, captura: str, rotulo: str, inicio: float) -> tuple[Path, float, str]:
    rotulo_png = TMP / f"rotulo{indice}.png"
    generar_rotulo(rotulo, rotulo_png)
    destino = TMP / f"tramo{indice}.mp4"
    clip = CLIPS / f"{clip_id}.mp4"
    entrada_rotulo = ["-loop", "1", "-framerate", str(FPS), "-i", str(rotulo_png)]
    usa_clip = clip.exists() and clip.stat().st_size > 0
    if usa_clip:
        dur = min(SEG_S, max(SEG_MIN_S, duracion(clip) - inicio))
        base = (
            f"[0:v]scale={ANCHO}:{ALTO}:force_original_aspect_ratio=increase,crop={ANCHO}:{ALTO},"
            f"fps={FPS},setpts=PTS-STARTPTS,tpad=stop_mode=clone:stop_duration={SEG_S},"
            f"trim=duration={dur:.3f},setpts=PTS-STARTPTS,setsar=1,format=yuv420p[v]"
        )
        entrada = ["-ss", f"{inicio:.3f}", "-i", str(clip)]
        origen = "clip"
    else:
        dur = SEG_S
        n = round(dur * FPS)
        # Se amplia 2x antes de zoompan para que el zoom no tiemble; zoom 1.0 -> 1.07 centrado.
        base = (
            f"[0:v]scale={ANCHO * 2}:{ALTO * 2}:force_original_aspect_ratio=increase,crop={ANCHO * 2}:{ALTO * 2},"
            f"zoompan=z='1+0.07*on/{n}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d={n}:s={ANCHO}x{ALTO}:fps={FPS},"
            f"setsar=1,format=yuv420p[v]"
        )
        entrada = ["-i", str(CAPTURAS / captura)]
        origen = "captura"
    sale = max(0.0, dur - ROTULO_ENTRA_S - ROTULO_FADE_S)
    filtro = (
        f"{base};"
        f"[1:v]format=rgba,fade=t=in:st={ROTULO_ENTRA_S}:d={ROTULO_FADE_S}:alpha=1,"
        f"fade=t=out:st={sale:.3f}:d={ROTULO_FADE_S}:alpha=1[r];"
        f"[v][r]overlay=x=(W-w)/2:y={ROTULO_Y}:format=auto:shortest=1,format=yuv420p,settb=AVTB,setsar=1[out]"
    )
    ffmpeg(
        *entrada, *entrada_rotulo,
        "-filter_complex", filtro, "-map", "[out]",
        "-t", f"{dur:.3f}", "-r", str(FPS), "-c:v", "libx264", "-preset", "veryfast", "-crf", "12",
        "-pix_fmt", "yuv420p", "-an", str(destino),
    )  # fmt: skip
    return destino, duracion(destino), origen


# ----------------------------------------------------------------------------------------------
# Union con xfade y salida final
# ----------------------------------------------------------------------------------------------
def unir(piezas: list[tuple[Path, float]], transiciones: list[str]) -> None:
    entradas: list[str] = []
    for ruta, _ in piezas:
        entradas += ["-i", str(ruta)]
    pasos = [
        f"[{i}:v]settb=AVTB,fps={FPS},setsar=1,format=yuv420p[n{i}]" for i in range(len(piezas))
    ]
    acumulado = piezas[0][1]
    previo = "n0"
    for i in range(1, len(piezas)):
        offset = acumulado - TRANSICION_S
        etiqueta = f"x{i}"
        pasos.append(
            f"[{previo}][n{i}]xfade=transition={transiciones[i - 1]}:duration={TRANSICION_S}:offset={offset:.3f}[{etiqueta}]"
        )
        acumulado = offset + piezas[i][1]
        previo = etiqueta
    pasos.append(f"[{previo}]format=yuv420p[final]")
    ffmpeg(
        *entradas,
        "-filter_complex", ";".join(pasos), "-map", "[final]",
        "-r", str(FPS), "-c:v", "libx264", "-preset", "medium", "-crf", "18",
        "-pix_fmt", "yuv420p", "-movflags", "+faststart", "-an", str(SALIDA),
    )  # fmt: skip


def fotogramas_control() -> None:
    for s in CONTROL_SEGUNDOS:
        destino = PROMO / f"control-{s:02d}s.png"
        ffmpeg("-ss", str(s), "-i", str(SALIDA), "-frames:v", "1", str(destino))


def main() -> None:
    for herramienta in ("ffmpeg", "ffprobe"):
        if shutil.which(herramienta) is None:
            fallar(f"{herramienta} no esta en el PATH")
    shutil.rmtree(TMP, ignore_errors=True)
    TMP.mkdir(parents=True)

    print("mascota...")
    mascota = renderizar_mascota()
    print("intro y cierre...")
    intro, cierre = TMP / "intro.mp4", TMP / "cierre.mp4"
    generar_intro(mascota, intro)
    generar_cierre(mascota, cierre)

    piezas: list[tuple[Path, float]] = [(intro, duracion(intro))]
    for i, (clip_id, captura, rotulo, inicio) in enumerate(TRAMOS, start=1):
        ruta, dur, origen = montar_tramo(i, clip_id, captura, rotulo, inicio)
        print(f"tramo {i}: {origen} ({clip_id}) {dur:.2f} s")
        piezas.append((ruta, dur))
    piezas.append((cierre, duracion(cierre)))

    transiciones = [TRANSICION_INTRO_CIERRE] + [TRANSICION_ENTRE_TRAMOS] * (len(TRAMOS) - 1)
    transiciones.append(TRANSICION_INTRO_CIERRE)
    print("uniendo...")
    unir(piezas, transiciones)
    fotogramas_control()
    print(f"{SALIDA.relative_to(RAIZ).as_posix()}  duracion {duracion(SALIDA):.2f} s")


if __name__ == "__main__":
    main()
