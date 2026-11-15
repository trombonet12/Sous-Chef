#!/usr/bin/env node
// Guarda mecánica al reescribir textos de recetas (ADR-0017): compara cada receta con su versión en una ref de git
// (por defecto HEAD) y falla si cambia algo que no sea texto. Uso:
//   node scripts/guarda-recetas.mjs [--base <ref>] [--permitir-enlaces] [slug…]
// --permitir-enlaces deja cambiar `dep` y `junto` (ADR-0018: enlazar pasos que solo seguían al reloj); salen como AVISO.
// Sin slugs, mira todas las recetas modificadas respecto a la base. Los cambios en avisos (hitos[].en) salen como AVISO:
// se admiten si se justifican (p. ej. un aviso que sale del título), el resto es ERROR.
import { execFileSync } from 'node:child_process';
import { readFileSync, existsSync } from 'node:fs';

const args = process.argv.slice(2);
let base = 'HEAD';
let permitirEnlaces = false;
const slugs = [];
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--base') base = args[++i];
  else if (args[i] === '--permitir-enlaces') permitirEnlaces = true;
  else slugs.push(args[i].replace(/\.json$/, '').replace(/^data\/recetas\//, ''));
}

const git = (...a) => execFileSync('git', a, { encoding: 'utf8' });
if (slugs.length === 0) {
  git('diff', '--name-only', base, '--', 'data/recetas')
    .split('\n')
    .filter((f) => f.endsWith('.json'))
    .forEach((f) => slugs.push(f.replace(/^data\/recetas\//, '').replace(/\.json$/, '')));
}

const CAMPOS_PASO = ['carril', 'off', 'min', 's', 'f', 'tapa', 'holgura', 'dep', 'junto'];
const CAMPOS_RECETA = ['slug', 'raciones', 'escala', 'totalMin', 'carriles', 'grupos', 'tags', 'perecederos'];
const igual = (a, b) => JSON.stringify(a ?? null) === JSON.stringify(b ?? null);

let errores = 0;
let avisos = 0;
for (const slug of slugs) {
  const ruta = `data/recetas/${slug}.json`;
  if (!existsSync(ruta)) {
    console.log(`${slug}: ERROR no existe ${ruta}`);
    errores++;
    continue;
  }
  let antes;
  try {
    antes = JSON.parse(git('show', `${base}:${ruta}`));
  } catch {
    console.log(`${slug}: nueva respecto a ${base}, sin guarda`);
    continue;
  }
  const ahora = JSON.parse(readFileSync(ruta, 'utf8'));
  const fallos = [];
  const notas = [];
  for (const c of CAMPOS_RECETA) if (!igual(antes[c], ahora[c])) fallos.push(`receta.${c} cambia`);
  const cortesSvg = (r) => (r.cortes ?? []).map((c) => c.svg);
  if (!igual(cortesSvg(antes), cortesSvg(ahora))) fallos.push('cortes[].svg cambia');
  const porId = (r) => Object.fromEntries(r.pasos.map((p) => [p.id, p]));
  const a = porId(antes);
  const b = porId(ahora);
  for (const id of Object.keys(a)) if (!(id in b)) fallos.push(`paso ${id} desaparece`);
  for (const id of Object.keys(b)) if (!(id in a)) fallos.push(`paso ${id} es nuevo`);
  for (const id of Object.keys(a).filter((x) => x in b)) {
    for (const c of CAMPOS_PASO) {
      if (igual(a[id][c], b[id][c])) continue;
      const linea = `${id}.${c}: ${JSON.stringify(a[id][c])} → ${JSON.stringify(b[id][c])}`;
      if (permitirEnlaces && (c === 'dep' || c === 'junto')) notas.push(linea);
      else fallos.push(linea);
    }
    const en = (p) => (p.hitos ?? []).map((h) => h.en).sort((x, y) => x - y);
    if (!igual(en(a[id]), en(b[id]))) notas.push(`${id}.hitos[].en: ${JSON.stringify(en(a[id]))} → ${JSON.stringify(en(b[id]))}`);
  }
  const offs = ahora.pasos.map((p) => p.off);
  if (offs.some((o, i) => i > 0 && o < offs[i - 1])) fallos.push('pasos fuera de orden de off');
  fallos.forEach((f) => console.log(`${slug}: ERROR ${f}`));
  notas.forEach((n) => console.log(`${slug}: AVISO ${n}`));
  if (fallos.length === 0 && notas.length === 0) console.log(`${slug}: OK`);
  errores += fallos.length;
  avisos += notas.length;
}
console.log(`guarda: ${slugs.length} recetas · ${errores} errores · ${avisos} avisos (base ${base})`);
process.exit(errores > 0 ? 1 : 0);
