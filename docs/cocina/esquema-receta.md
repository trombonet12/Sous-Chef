# Esquema de receta y técnica

> Propósito: esquema JSON de `data/recetas/*.json` y `data/tecnicas/*.json` y lista de comprobaciones del validador · Audiencia: Claude Code, `sc validar`, quien edite recetas · Actualizado: 2026-10-13 · Relacionado: docs/cocina/reglas.md, docs/herramientas.md

## Resumen
Los pasos son **una sola lista**: la ficha, el gráfico y la cola de cada carril se derivan de `pasos`, así que no pueden descuadrarse. Formato canónico: `scripts/sc formatear` (indent 2, claves ordenadas, UTF-8, `\n` final). Las horas de reloj no existen en la receta: todo son minutos desde el arranque.

## Receta

```jsonc
{
  "slug": "macarrones-bolonesa",           // minúsculas, sin acentos, con guiones; = nombre del archivo
  "titulo": "Macarrones con boloñesa",     // el nombre del plato, sin subtítulos
  "descripcion": "Dos o tres frases: qué es el plato y dónde está su dificultad.",
  "raciones": 4,
  "totalMin": 82,                          // el paso más tardío acaba aquí
  "datos": [["82 min", "de principio a fin"], ["12 + 70", "cortar + fuego"], ["2", "fuegos"], ["12", "pasos"]],

  "tags": { "base": "pasta", "tecnica": ["sofrito", "reduccion"], "proteina": "carne picada" },
  "perecederos": ["carne picada mixta"],   // frescos que gasta

  "carriles": [                            // un recipiente o zona de trabajo
    { "id": "sarten", "nombre": "SARTÉN", "color": "#F0AE4A", "tipo": "fuego" }
    // tipo: "fuego" | "mesa" | "horno". Colores de la casa: mesa #C0B7A6,
    // sartén #F0AE4A, olla #6FBEE8, cazuela #E8836B, horno #B58CD0
  ],

  "grupos": [                              // ingredientes agrupados por recipiente
    { "n": "SARTÉN", "items": [
      { "a": 500, "u": "g", "n": "carne picada mixta" },   // a entero o decimal
      { "txt": "½ cdta", "n": "pimienta negra", "esc": "sub" }
    ]}
    // formas de item: {a,n,u} · {a,esc,n,u} · {esc,n,txt} · {n,txt}; esc solo "sub" (no escala lineal)
  ],
  "notaEscala": "Qué no escala en línea recta y por qué.",

  "pasos": [
    {
      "id": "sofrito",          // único en la receta; lo usan dep y junto
      "t": "Sudar la verdura",
      "carril": "sarten",
      "off": 13,                // minuto de inicio desde el arranque (puede ser decimal)
      "min": 11,                // duración en minutos (puede ser decimal)
      "s": 660,                 // duración exacta en segundos; el cronómetro usa esto
      "f": 5,                   // potencia 0-9; null si el carril no es fuego
      "tapa": "TAPA PUESTA",    // null | "sin tapa" | "TAPA PUESTA" | "tapa fuera" | "TAPA A MEDIAS"
      "holgura": "flexible",    // flexible | ajustado | critico
      "dep": ["cortar"],        // opcional. Pasos de OTROS carriles que deben estar hechos
      "junto": "macarrones",    // opcional. Paso que corre en paralelo cuando el momento exacto importa
      "meto": "el bol: 160 g de cebolla + 120 g de zanahoria",  // qué entra, cuánto y de qué recipiente sale
      "saco": "",               // qué sale y adónde; el último paso siempre lo lleva
      "nota": "Una o dos frases: la señal que mirar y el error que evitar.",
      "hitos": [{ "en": 180, "t": "Destapa, remueve y vuelve a tapar" }],   // opcional
      "sinHitos": "",           // opcional. Solo en pasos de fuego u horno de 6 min o más SIN hitos
      "p": "Tres o cuatro frases con el porqué físico dentro. Esto es la ficha."
    }
  ],

  "lectura": "Los solapes que hay que respetar, y la suma de los tramos, que tiene que cuadrar con totalMin.",
  "mep": [{ "t": "CUATRO RECIPIENTES", "orden": false, "porq": "Por qué este reparto.",
            "l": ["<b>Bol mediano</b> · 160 g de cebolla en dados de 4 mm. Entra en el minuto 13"] }],
  "cortes": [{ "t": "Cebolla · dados de 4 mm", "p": "El porqué.", "svg": "<svg …/>" }],
  "porque": [["Título de la decisión", "El razonamiento, y aquí también los descartes"]],
  "pie": "Minutos contados desde que coges el cuchillo. Las horas de reloj están en el cronómetro.",
  "creada": "2026-09-10T00:00:00Z",       // opcional
  "actualizada": "2026-09-10T00:00:00Z"
}
```

Solo `creada` e `importada` son opcionales a nivel de receta; `dep`, `junto`, `hitos` y `sinHitos` lo son a nivel de paso. Lo demás es obligatorio.

### `nota` y `p`
Son los dos niveles del mismo paso, y la única duplicación admitida. `p` argumenta (se lee antes, sentado); `nota` dicta (se mira de reojo con las manos ocupadas). Una `nota` de más de dos frases sobra. No pongas en las notas avisos de paralelismo (los calcula la app desde los `off`). En el cronómetro `nota` se ve siempre y `p` queda a un toque.

### Dependencias
`dep` solo apunta a pasos de otros carriles: el orden dentro de un carril ya lo da la cola. Si un paso arranca a mitad de otro largo (el agua durante el chup-chup), su requisito es el paso anterior al largo, no el largo, o quedaría bloqueado hasta que acabe.

### Hitos, `sinHitos` y partir pasos
- **Cambia el montaje** (potencia, tapa, contenido del recipiente) → dos pasos.
- **No cambia nada, solo hay que hacer algo** (remover, voltear, rascar) → un hito.
- `en` en segundos desde el inicio del paso, en orden y siempre menor que la duración. Texto en imperativo corto.
- Paso de fuego u horno de 6 min o más: o lleva `hitos` o lleva `sinHitos` con la frase que explica por qué el silencio es correcto.

### Holgura
- `flexible`: aguanta esperar sin estropearse (reducción a fuego bajo, sofrito tapado, reposo).
- `ajustado`: se estira unos minutos con coste de textura.
- `critico`: no espera (pasta escurrida, ajo, emplatado, masas montadas).

### `mep`
Cada bloque: `t` título, `orden` (`true` si la secuencia es obligatoria, como medidor y tabla; `false` si es solo una lista, como recipientes), `porq` el porqué del reparto y `l[]` las líneas. **Solo se admite la etiqueta `<b>`** en las líneas. Cada línea lleva cantidad (con equivalencia casera) y minuto de entrada.

### Cortes SVG
`svg` en línea, `viewBox="0 0 120 88"`, ilustrativo: la pieza con relleno tenue de su color real, la fibra en líneas finas del mismo color, los cortes en discontinuas oscuras. Si la receta corta algo, `cortes` no se queda vacío.

## Técnica
```jsonc
{ "slug": "sofrito", "titulo": "Sofrito", "resumen": "Una frase.", "categoria": "base",
  "pasos": [{ "t": "Cortar fino y parejo", "p": "El porqué físico." }],
  "consejos": ["Señal concreta y error que evitar."],
  "cortes": [], "pie": "",              // cortes y pie opcionales
  "creada": "…", "actualizada": "…" }
```
Una receta enlaza una ficha poniendo su `slug` en `tags.tecnica`.

## Validación
Lo hace `scripts/sc validar [--estricto]`. **ERROR** falla siempre. **AVISO** solo informa, salvo con `--estricto`, donde pasa a error. Las marcadas «(estricto)» son AVISO que `--estricto` convierte en error.

1. ERROR · El paso más tardío acaba exactamente en `totalMin` y alguno arranca en el minuto 0.
2. ERROR · Todo `dep` y todo `junto` apuntan a un `id` existente, de otro carril, que termina antes de que empiece el paso que lo declara. Sin ciclos.
3. ERROR · Nunca más de 3 carriles de `tipo:"fuego"` solapados.
4. ERROR · `f` entre 0 y 9, y `null` si el carril no es fuego.
5. ERROR · Todo `carril` usado por un paso está declarado, y todo carril declarado se usa.
6. ERROR · `tags.base` y `tags.tecnica` puestos (el historial los usa para avisar de repeticiones).
7. ERROR · Los `hitos` van en orden y ninguno llega a la duración del paso.
8. ERROR · Todo ingrediente de `grupos` aparece en alguna línea de `mep`, con cantidad y minuto de entrada. La coincidencia es por palabra completa; el plural admite `-es` solo tras l, r, n, d, z, s, x, y, j (cebolla/cebollas, pan/panes, sal/sales); `-s` tras vocal. Nunca por subcadena («sal» no casa en «salsa»). Heurística: se usa `n` sin paréntesis ni lo que sigue a la coma; si no casa entero, vale su palabra núcleo (primera de 3+ letras, también en singular). Sin `mep` no aplica. Limitación: «semillas de sésamo» o «salsa de soja» solo casan si el mep dice «semillas» / «salsa».
9. ERROR · Ninguna línea describe dos formas de corte para el mismo ingrediente.
10. ERROR · Ningún paso de fuego u horno de 6 min o más sin `hitos` ni `sinHitos`.
11. ERROR · El último paso declara `saco`.
12. ERROR · Todo ingrediente de `grupos` está en la despensa (`data/despensa-semilla.md`); no hay campo de compra, así que si una receta necesita algo nuevo se añade antes a la despensa. Coincidencia por palabra completa, sin acentos ni mayúsculas, en ambos sentidos (la entrada «Ajos» casa con «diente de ajo»; se usa el nombre sin paréntesis ni coma y su palabra núcleo). «agua» siempre vale. La implementa `tools/` (`Despensa.kt`); la única fuente es la semilla (`sc validar` imprime `despensa: semilla …`).
13. ERROR · Cantidad legible: cada ingrediente tiene `a` o `txt`, y ningún texto dice «al gusto», «un chorrito» o «c/s».
14. ERROR · Hay al menos un paso que arranca en el minuto 0 en un carril de tipo `mesa` si la receta tiene `cortes` (el corte es un paso, no una nota previa).
15. AVISO (estricto) · Si algún paso de mesa corta algo (cortar, picar, laminar, trocear…), `cortes` no está vacío.
16. ERROR · No hay solape entre dos pasos del mismo carril.
17. ERROR · `s` coherente con `min`: |s − min·60| ≤ 30 s.
18. ERROR · `mep` solo usa la etiqueta `<b>`.
19. AVISO (estricto) · Corte sin medida (mm o cm).
20. AVISO (estricto) · Cambio de potencia que el texto del paso no nombra.
21. AVISO (estricto) · `p` de menos de 120 caracteres.
22. AVISO (estricto) · Tapa que cambia a mitad de un paso (debía ser dos pasos).
23. AVISO (estricto) · Tag de `tags.tecnica` sin ficha en `data/tecnicas/`.
24. AVISO · Veto presente en ingredientes (garbanzos, guisantes, champiñones, coliflor, brócoli, casquería, anchoas, alcaparras, marisco).
25. ERROR · El verbo de potencia del texto casa con el cambio real de `f` del carril: «baja al N» exige que el mando baje (N menor que el máximo entre la potencia anterior y `f`), «sube al N» que suba (N mayor que el mínimo). AVISO si «del X al N» y X no es ni la potencia anterior ni `f`.

**Estado en el código** (`Validador.kt`, más `tools/Despensa.kt` para la 12): implementadas 1-8, 10-21 y 23-25. **Sin implementar**: la 9 (dos formas de corte en una línea: es lenguaje natural, sin forma fiable de detectarlo) y la 22 (la tapa es un único campo por paso, así que un cambio a mitad solo se vería en el texto libre de `hitos`, con demasiados falsos positivos). Detalles: la 15 mira `t` y `nota` de los pasos de mesa (cortar, picar, laminar, trocear, rallar, dados); la 19 busca número + mm|cm en `t`/`p` de cada corte; la 20 mira el número de potencia en `t`+`nota` cuando `f` cambia respecto al paso anterior del carril; la 25 analiza `t` y `nota` de cada paso con `f` contra la transición potencia anterior del carril → `f` (sin anterior, solo contra `f`), por frases, y los `hitos[].t` como condicionales solo contra `f` (no mira `p`); la 24 usa raíces (garbanzo, guisante, champiñón, coliflor, brócoli, casquería, hígado, anchoa, alcaparra, marisco, gamba, langostino, mejillón, calamar, pulpo, sepia, almeja…) al inicio de palabra, en `grupos`.

Los puntos 14, 15 y 17-18 no estaban en la skill legada (14 y 15 salen de su §3a y §5); la 24 es una red de seguridad. Si el validador implementado difiere de esta lista, manda el código y se corrige este documento.

## Decisiones y fuentes
- Origen: `docs/legado/skill-receta-cronometrada.md` §5 y `docs/spec/app-legado.md` §7.
