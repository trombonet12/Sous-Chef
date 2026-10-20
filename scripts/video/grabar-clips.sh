#!/usr/bin/env bash
# Graba los 5 clips verticales (1080x1920) de Sous Chef para el vídeo promocional.
#
# Uso:   scripts/video/grabar-clips.sh [todos | c1 c2 c3 c4 c5 ...]
#        (sin argumentos = todos). Salida: build/promo/clips/cN-*.mp4 (ignorado por git).
#
# Requisitos:
#   - Git Bash (o bash) con adb; SDK en $LOCALAPPDATA/Android/Sdk (o ADB=/ruta/adb).
#   - Emulador trombito-36 arrancado (o dispositivo único) con la app debug
#     app.trombito.souschef.dev instalada (adb install -r app/build/outputs/apk/debug/app-debug.apk).
#   - Con datos reales: receta «Arroz a la cubana con bacon», cuadraditos Arroz/Salsa, técnica «Bechamel»,
#     historial con al menos una cocinada.
#   - Permisos de micrófono/notificaciones ya concedidos (haz un ensayo del cronómetro antes) para que
#     ningún diálogo salga en pantalla.
#   - ffprobe (opcional) para la validación final.
# El script fija 1080x1920/420dpi, tema claro, font_scale 1.0 y al terminar restaura wm size/density.
# No usa modo demo: la hora de la barra de estado es la real.

set -u
export MSYS_NO_PATHCONV=1
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb}"
PKG="${PKG:-app.trombito.souschef.dev}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$RAIZ/build/promo/clips"
DUMP="${TMPDIR:-/tmp}/sc-ui.xml"
REC_PID=""

a() { "$ADB" "$@"; }
sh_() { a shell "$@"; }
pausa() { sleep "${1:-1.5}"; }

# --- UI via uiautomator ------------------------------------------------------
volcar() { sh_ uiautomator dump /sdcard/sc-ui.xml >/dev/null 2>&1; a exec-out cat /sdcard/sc-ui.xml | tr -d '\r' | sed 's/<node /\n<node /g' >"$DUMP"; }

# regex del nodo: "=Texto" = coincidencia exacta; "Texto" = subcadena
regex_nodo() { case "$1" in =*) echo "(text|content-desc)=\"${1#=}\"" ;; *) echo "(text|content-desc)=\"[^\"]*$1[^\"]*\"" ;; esac; }

# centro "x y" del n-ésimo nodo cuyo text o content-desc coincide con $1 (ver regex_nodo)
centro() {
  local patron="$1" n="${2:-1}" b
  b=$(grep -E "$(regex_nodo "$patron")" "$DUMP" | sed -n "${n}p" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | grep -oE '[0-9]+' | tr '\n' ' ')
  [ -z "$b" ] && return 1
  set -- $b
  echo "$(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))"
}

# toca el elemento que contiene el texto; falla si no existe
tocar() {
  local xy; volcar
  xy=$(centro "$1" "${2:-1}") || { echo "ERROR: no encuentro «$1» en pantalla" >&2; return 1; }
  sh_ input tap $xy; pausa "${3:-1.5}"
}

existe() { volcar; grep -qE "$(regex_nodo "$1")" "$DUMP"; }

# swipe lento en el eje central: bajar = ver más contenido (dedo sube) | subir = volver arriba
#   deslizar bajar|subir [duración ms] [pausa posterior s]
deslizar() {
  case "$1" in
    bajar) sh_ input swipe 540 1500 540 500 "${2:-900}" ;;
    subir) sh_ input swipe 540 500 540 1500 "${2:-900}" ;;
    bajar+) sh_ input swipe 540 1750 540 250 "${2:-800}" ;;   # recorrido largo (fichas)
  esac
  pausa "${3:-1}"
}
tap_xy() { sh_ input tap $1; pausa "${2:-1}"; }

# --- grabación ---------------------------------------------------------------
# grabar <nombre> <segundos máx>: arranca screenrecord en segundo plano (tope de seguridad = máx)
iniciar() {
  sh_ rm -f "/sdcard/$1.mp4"
  sh_ screenrecord --size 1080x1920 --bit-rate 8000000 --time-limit "${2:-10}" "/sdcard/$1.mp4" &
  REC_PID=$!
  sleep 1
}
parar() { sh_ pkill -INT screenrecord >/dev/null 2>&1; wait "$REC_PID" 2>/dev/null; sleep 1.5; }
ruta_win() { command -v cygpath >/dev/null && cygpath -w "$1" || echo "$1"; }   # adb.exe no entiende /c/...
pull() {
  mkdir -p "$OUT"
  if a pull "/sdcard/$1.mp4" "$(ruta_win "$OUT/$1.mp4")" >/dev/null; then sh_ rm -f "/sdcard/$1.mp4"; echo "-> $OUT/$1.mp4"; else echo "ERROR pull $1" >&2; fi
}

# --- hora: ajustar «Servir a» para que el inicio previsto quede a ~1-2 min de la hora real ----
segundos_ahora() { local h; h=$(sh_ date +%H:%M:%S | tr -d '
'); echo $(( 10#${h:0:2}*3600 + 10#${h:3:2}*60 + 10#${h:6:2} )); }
hora_nodo() { volcar; grep -E "$(regex_nodo "$1")" "$DUMP" | head -1 | grep -oE '[0-9]{2}:[0-9]{2}' | head -1; }

# ajustar_inicio <patrón del nodo con la hora de inicio> <margen s>
# El inicio previsto cambia de 15 en 15 min con los botones «Servir 15 minutos antes/más tarde».
# Se elige el primer inicio T >= ahora+margen y se espera (sin grabar) hasta T-100 s.
ajustar_inicio() {
  local h0 s0 ahora k t i xy espera
  h0=$(hora_nodo "$1") || return 1
  [ -z "$h0" ] && { echo "ERROR: no hay hora en «$1»" >&2; return 1; }
  s0=$(( 10#${h0%%:*}*3600 + 10#${h0##*:}*60 ))
  ahora=$(segundos_ahora)
  k=$(( (ahora + $2 - s0 + 900*200 + 899) / 900 - 200 ))
  t=$(( s0 + 900*k ))
  if [ "$k" -gt 0 ]; then xy=$(centro "Servir 15 minutos más tarde"); else xy=$(centro "Servir 15 minutos antes"); fi
  for i in $(seq 1 ${k#-}); do [ "$k" -eq 0 ] && break; sh_ input tap $xy; sleep 0.4; done
  pausa 1
  espera=$(( t - 100 - $(segundos_ahora) ))
  echo "inicio previsto $(printf '%02d:%02d' $((t/3600%24)) $((t/60%60))); espero ${espera} s" >&2
  [ "$espera" -gt 0 ] && sleep "$espera"
  return 0
}

# --- preparación / restauración -------------------------------------------------
preparar() {
  TZ_ORIG=$(sh_ getprop persist.sys.timezone | tr -d '
')
  sh_ cmd alarm set-timezone Europe/Madrid >/dev/null 2>&1   # hora de la barra de estado = hora real de la máquina
  sh_ wm size 1080x1920; sh_ wm density 420
  sh_ settings put system font_scale 1.0
  sh_ cmd uimode night no >/dev/null
  sh_ input keyevent KEYCODE_WAKEUP; sh_ wm dismiss-keyguard >/dev/null 2>&1
  sh_ cmd statusbar collapse >/dev/null 2>&1
}
restaurar() {
  sh_ pkill -INT screenrecord >/dev/null 2>&1
  sh_ wm size reset; sh_ wm density reset
  [ -n "${TZ_ORIG:-}" ] && sh_ cmd alarm set-timezone "$TZ_ORIG" >/dev/null 2>&1
  sh_ am force-stop "$PKG"
  sh_ monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
}
trap restaurar EXIT

# reinicia la app y la deja en la pestaña Recetas, arriba y estable
inicio() {
  sh_ am force-stop "$PKG"; pausa 1
  sh_ monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; pausa 4
  tocar "=Recetas" 1 2; pausa 1
}

# --- clips ------------------------------------------------------------------------
clip1() {
  inicio
  volcar; local xy; xy=$(centro "Arroz, 2 recetas") || return 1
  iniciar c1-recetas 12
  pausa 0.8
  deslizar bajar 900 0.8
  deslizar subir 900 0.6
  tap_xy "$xy" 2.5
  parar; pull c1-recetas
}

# c2 y c3 van seguidos: comparten el mismo inicio previsto (sin esperar otros 15 min)
clip2() {
  inicio
  tocar "Arroz, 2 recetas" 1 2
  tocar "=Arroz a la cubana con bacon" 1 2.5
  ajustar_inicio "Empieza a las" 60 || return 1      # sin grabar
  iniciar c2-ficha 14
  pausa 1.5
  deslizar bajar+ 800 1.2        # cabecera -> ingredientes
  deslizar bajar+ 800 1
  deslizar bajar+ 800 1          # -> «El tiempo» (diagrama)
  deslizar bajar+ 800 1.2
  parar; pull c2-ficha
}

clip3() {
  # parte de la ficha de clip2 (desplazada): volver arriba rápido
  for _ in 1 2 3 4 5 6; do sh_ input swipe 540 500 540 1700 150; sleep 0.3; done; pausa 1
  tocar "=Cronómetro" 1 3                      # pantalla «Antes de encender nada»
  tocar "=Abrir la cocina" 1 3                 # carriles (parados)
  ajustar_inicio "Prevista a las" 0 || return 1 # sin grabar; si c2 acaba de terminar, no espera
  volcar
  local e i2 i3 i4 h
  e=$(centro "=EMPEZAR") || return 1
  i2=$(centro "=Empezar igual" 1); i3=$(centro "=Empezar igual" 2); i4=$(centro "=Empezar igual" 3)
  tap_xy "$e" 1
  volcar; h=$(centro "=HECHO") || return 1
  tap_xy "$i2" 0.6; tap_xy "$i3" 0.6; tap_xy "$i4" 1
  iniciar c3-cronometro 12
  pausa 3
  tap_xy "$h" 3.5
  parar; pull c3-cronometro
  # fuera de la grabación: abandonar la cocinada y salir
  tocar "=Más opciones" 1 1; tocar "=ABANDONAR" 1 1.5; tocar "=Abandonar" 1 2
}

clip4() {
  inicio
  tocar "=Técnicas" 1 2
  # ensayo (sin grabar) para conocer la posición de «Bechamel» dentro de «Salsa»
  tocar "Salsa, 5 técnicas" 1 2
  volcar; local xb xs; xb=$(centro "=Bechamel") || return 1
  tocar "=Volver" 1 2
  volcar; xs=$(centro "Salsa, 5 técnicas") || return 1
  iniciar c4-tecnicas 12
  pausa 0.8
  tap_xy "$xs" 1.6
  tap_xy "$xb" 2
  deslizar bajar 900 1.2
  parar; pull c4-tecnicas
}

clip5() {
  inicio
  tocar "=Historial" 1 2
  iniciar c5-historial 12
  pausa 0.8
  deslizar bajar 900 0.8
  deslizar bajar 900 0.8
  deslizar subir 900 1
  parar; pull c5-historial
}

validar() {
  command -v ffprobe >/dev/null || return 0
  local f
  for f in "$OUT"/*.mp4; do echo "$(basename "$f"): $(ffprobe -v error -select_streams v:0 -show_entries stream=width,height,duration -of csv=p=0 "$(ruta_win "$f")")"; done
}

# --- main ----------------------------------------------------------------------------
# Orden por defecto: c1 c4 c5 c2 c3 (c2 y c3 comparten la espera de la hora; c3 abandona la cocinada).
a get-state >/dev/null 2>&1 || { echo "ERROR: no hay emulador/dispositivo (adb devices)" >&2; exit 1; }
PEDIDOS=("$@"); [ ${#PEDIDOS[@]} -eq 0 ] || [ "${PEDIDOS[0]}" = todos ] && PEDIDOS=(c1 c4 c5 c2 c3)
preparar
for c in "${PEDIDOS[@]}"; do
  echo "== $c =="
  case "$c" in
    c1) clip1 ;; c2) clip2 ;; c3) clip3 ;; c4) clip4 ;; c5) clip5 ;;
    *) echo "clip desconocido: $c" >&2 ;;
  esac || echo "FALLO en $c" >&2
done
validar
