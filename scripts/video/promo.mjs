#!/usr/bin/env node
// Regenera el vídeo promocional de Sous Chef (Remotion, ADR-0012). Único comando del flujo.
//
// Uso: node scripts/video/promo.mjs [--grabar] [--solo-grabar] [--control] [--sin-render]
//   --grabar        graba antes los 5 clips con el emulador (bash scripts/video/grabar-clips.sh todos)
//   --solo-grabar   graba los clips y termina (implica --grabar)
//   --control       tras renderizar, saca 6 stills repartidos (build/promo/control-NN.png) y una
//                   hoja de contacto build/promo/control-hoja.png (si hay Python + Pillow)
//   --sin-render    solo prepara y comprueba (clips, node_modules); no renderiza
// Salida: build/promo/sous-chef-promo-1920x1080.mp4 (ignorado por git).
// Sale con código distinto de 0 si cualquier paso falla.

import { spawnSync } from 'node:child_process';
import { copyFileSync, existsSync, mkdirSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const RAIZ = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const VIDEO = join(RAIZ, 'video');
const CLIPS_ORIGEN = join(RAIZ, 'build', 'promo', 'clips');
const CLIPS_DESTINO = join(VIDEO, 'public', 'clips');
const SALIDA_DIR = join(RAIZ, 'build', 'promo');
const MP4 = join(SALIDA_DIR, 'sous-chef-promo-1920x1080.mp4');
const MP4_ARG = '../build/promo/sous-chef-promo-1920x1080.mp4';
const CLIPS_ESPERADOS = ['c1', 'c2', 'c3', 'c4', 'c5'];
const WIN = process.platform === 'win32';

const args = new Set(process.argv.slice(2));
const conocidos = new Set(['--grabar', '--solo-grabar', '--control', '--sin-render']);
for (const a of args) {
  if (!conocidos.has(a)) fallar(`argumento desconocido: ${a}\nUso: node scripts/video/promo.mjs [--grabar] [--solo-grabar] [--control] [--sin-render]`);
}
const soloGrabar = args.has('--solo-grabar');
const grabar = args.has('--grabar') || soloGrabar;
const control = args.has('--control');
const sinRender = args.has('--sin-render');

function fallar(msg) {
  console.error(`promo.mjs: ${msg}`);
  process.exit(1);
}

function paso(msg) {
  console.log(`\n== ${msg}`);
}

/** Ejecuta un comando heredando la salida; aborta si falla. En Windows, npm/npx son .cmd y necesitan shell. */
function ejecutar(cmd, argv, { cwd = RAIZ, shell = false, capturar = false } = {}) {
  const r = spawnSync(cmd, argv, { cwd, shell, stdio: capturar ? 'pipe' : 'inherit', encoding: 'utf8' });
  if (r.error) fallar(`no se pudo ejecutar ${cmd}: ${r.error.message}`);
  return r;
}

function ejecutarOFallar(cmd, argv, opts) {
  const r = ejecutar(cmd, argv, opts);
  if (r.status !== 0) fallar(`falló «${cmd} ${argv.join(' ')}» (código ${r.status})`);
  return r;
}

/** Comando de Node ejecutable (npx/npm) con cwd en video/. Los argumentos no llevan espacios. */
function npx(argv, opts = {}) {
  return ejecutar(WIN ? 'npx.cmd' : 'npx', argv, { cwd: VIDEO, shell: WIN, ...opts });
}

function encontrarBash() {
  const candidatos = WIN ? ['C:\\Program Files\\Git\\bin\\bash.exe', 'C:\\Program Files (x86)\\Git\\bin\\bash.exe'] : [];
  for (const c of candidatos) if (existsSync(c)) return c;
  // En Windows, el bash de System32 es WSL: se evita a favor de Git Bash.
  const r = spawnSync(WIN ? 'where' : 'which', ['bash'], { encoding: 'utf8' });
  if (r.status === 0) {
    const lineas = r.stdout.split(/\r?\n/).filter(Boolean);
    const buena = lineas.find((l) => !/system32/i.test(l)) ?? lineas[0];
    if (buena) return buena;
  }
  return null;
}

function tieneComando(cmd, versionArg = '-version') {
  const r = spawnSync(cmd, [versionArg], { encoding: 'utf8' });
  return !r.error && r.status === 0;
}

// --- 1. Grabación opcional -----------------------------------------------------------------
if (grabar) {
  paso('Grabando clips (scripts/video/grabar-clips.sh todos)');
  const bash = encontrarBash();
  if (!bash) fallar('no se encontró bash (instala Git for Windows o añade bash al PATH)');
  ejecutarOFallar(bash, ['scripts/video/grabar-clips.sh', 'todos']);
  if (soloGrabar) {
    console.log(`\nClips grabados en ${CLIPS_ORIGEN}`);
    process.exit(0);
  }
}

// --- 2. Copiar clips nuevos a video/public/clips ---------------------------------------------
paso('Sincronizando clips en video/public/clips');
mkdirSync(CLIPS_DESTINO, { recursive: true });
if (existsSync(CLIPS_ORIGEN)) {
  for (const f of readdirSync(CLIPS_ORIGEN).filter((n) => n.endsWith('.mp4'))) {
    const origen = join(CLIPS_ORIGEN, f);
    const destino = join(CLIPS_DESTINO, f);
    if (!existsSync(destino) || statSync(origen).mtimeMs > statSync(destino).mtimeMs) {
      copyFileSync(origen, destino);
      console.log(`  copiado ${f}`);
    }
  }
}

// --- 3. Comprobar los 5 clips --------------------------------------------------------------
const presentes = readdirSync(CLIPS_DESTINO).filter((n) => n.endsWith('.mp4'));
const faltan = CLIPS_ESPERADOS.filter((c) => !presentes.some((n) => n.startsWith(`${c}-`)));
if (faltan.length > 0) {
  fallar(
    `faltan clips en video/public/clips: ${faltan.join(', ')}.\n` +
      'Remedio: arranca el emulador con la app debug y ejecuta  node scripts/video/promo.mjs --grabar',
  );
}
console.log(`  clips OK: ${CLIPS_ESPERADOS.join(', ')}`);

// --- 4. Dependencias de Remotion -----------------------------------------------------------
if (!existsSync(join(VIDEO, 'node_modules'))) {
  paso('Instalando dependencias (npm ci en video/)');
  ejecutarOFallar(WIN ? 'npm.cmd' : 'npm', ['ci'], { cwd: VIDEO, shell: WIN });
} else {
  console.log('  video/node_modules OK');
}

// --- 5. Render -----------------------------------------------------------------------------
mkdirSync(SALIDA_DIR, { recursive: true });
if (sinRender) {
  console.log('\n--sin-render: comprobaciones correctas, no se renderiza.');
  process.exit(0);
}

paso('Renderizando composición Promo');
{
  const r = npx(['remotion', 'render', 'src/index.ts', 'Promo', MP4_ARG]);
  if (r.status !== 0) fallar(`falló el render de Remotion (código ${r.status})`);
}
if (!existsSync(MP4)) fallar(`el render terminó sin generar ${MP4}`);

// --- 6. Control visual (opcional) ----------------------------------------------------------
const hayFfprobe = tieneComando('ffprobe');

function duracionSegundos() {
  if (!hayFfprobe) return null;
  const r = spawnSync('ffprobe', ['-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', MP4], { encoding: 'utf8' });
  const d = parseFloat((r.stdout ?? '').trim());
  return Number.isFinite(d) ? d : null;
}

if (control) {
  paso('Control visual: 6 stills y hoja de contacto');
  let frames = null;
  const comp = npx(['remotion', 'compositions', 'src/index.ts'], { capturar: true });
  if (comp.status === 0) {
    const m = /^Promo\s+(\d+)\s+\S+\s+(\d+)/m.exec(comp.stdout.replace(/\x1b\[[0-9;]*m/g, ''));
    if (m) frames = parseInt(m[2], 10);
  }
  if (frames === null) {
    const seg = duracionSegundos();
    const fps = comp.status === 0 ? parseInt(/^Promo\s+(\d+)/m.exec(comp.stdout ?? '')?.[1] ?? '30', 10) : 30;
    if (seg !== null) frames = Math.round(seg * fps);
  }
  if (frames === null || frames < 12) fallar('no se pudo determinar la duración (frames) de la composición Promo');
  console.log(`  composición Promo: ${frames} frames`);

  const stills = [];
  for (let i = 0; i < 6; i++) {
    const frame = Math.min(frames - 1, Math.round(((i + 0.5) / 6) * frames));
    const nombre = `control-${String(i + 1).padStart(2, '0')}.png`;
    const r = npx(['remotion', 'still', 'src/index.ts', 'Promo', `../build/promo/${nombre}`, `--frame=${frame}`]);
    if (r.status !== 0) fallar(`falló el still del frame ${frame} (código ${r.status})`);
    stills.push(join(SALIDA_DIR, nombre));
  }

  const py = `
import sys
from PIL import Image
out, files = sys.argv[1], sys.argv[2:]
ims = [Image.open(f).convert('RGB') for f in files]
w = 640; h = round(ims[0].height * w / ims[0].width)
sheet = Image.new('RGB', (w * 3, h * 2), 'white')
for i, im in enumerate(ims):
    sheet.paste(im.resize((w, h)), ((i % 3) * w, (i // 3) * h))
sheet.save(out)
`;
  const hoja = join(SALIDA_DIR, 'control-hoja.png');
  let hecho = false;
  for (const py_cmd of WIN ? ['python', 'py'] : ['python3', 'python']) {
    const r = spawnSync(py_cmd, ['-c', py, hoja, ...stills], { encoding: 'utf8' });
    if (!r.error && r.status === 0) {
      hecho = true;
      break;
    }
  }
  console.log(hecho ? `  hoja de contacto: ${hoja}` : '  Python+Pillow no disponible: stills sueltos en build/promo/control-NN.png');
}

// --- 7. Resumen ----------------------------------------------------------------------------
paso('Listo');
const tamMb = (statSync(MP4).size / (1024 * 1024)).toFixed(1);
const dur = duracionSegundos();
console.log(`MP4: ${MP4}`);
console.log(`Tamaño: ${tamMb} MB${dur !== null ? ` · Duración: ${dur.toFixed(1)} s` : hayFfprobe ? '' : ' · (ffprobe no disponible: sin duración)'}`);
console.log(`
Subir a YouTube y enlazar en Play:
  1. YouTube Studio > Crear > Subir vídeo: selecciona el MP4 anterior.
  2. Visibilidad: «No listado» (oculto); no lo marques como contenido para niños.
  3. Copia la URL de la forma https://www.youtube.com/watch?v=XXXXXXXXXXX
  4. Play Console > Ficha de Play Store > Vídeo promocional: pega esa URL watch?v=.`);
