# Esquema de receta y técnica

> Propósito: esquema JSON de `data/recetas/*.json` y `data/tecnicas/*.json` y lista de comprobaciones del validador · Audiencia: Claude Code, `sc validar`, quien edite recetas · Actualizado: 2026-11-15 · Relacionado: docs/cocina/reglas.md, docs/herramientas.md, docs/adr/0017-dos-textos-por-paso-orden-compuesta.md

## Resumen
Los pasos son **una sola lista**: la ficha, el gráfico y la cola de cada carril se derivan de `pasos`, así que no pueden descuadrarse. Formato canónico: `scripts/sc formatear` (indent 2, claves ordenadas, UTF-8, `\n` final). Las horas de reloj no existen en la receta: todo son minutos desde el arranque.

## Receta

```jsonc
{
  "slug": "macarrones-bolonesa",           // minúsculas, sin acentos, con guiones; = nombre del archivo
  "titulo": "Macarrones con boloñesa",     // el nombre del plato, sin subtítulos
  "descripcion": "Dos o tres frases: qué es el plato y dónde está su dificultad.",
  "raciones": 4,
  "escala": { "min": 1, "max": 4, "paso": 1 },   // rango de raciones con el que el plan sigue valiendo sin cambiar pasos
  "totalMin": 82,                          // el paso más tardío acaba aquí
  "datos": [["82 min", "de principio a fin"], ["12 + 70", "cortar + fuego"], ["2", "fuegos"], ["12", "pasos"]],

  "tags": { "base": "pasta", "tecnica": ["sofrito", "reduccion"], "proteina": "carne picada",
            "momento": ["comida", "cena"],     // momento opcional: "comida"|"cena"|"desayuno"|"merienda"|"postre"; ausente = cualquiera
            "equipo": ["batidora"] },          // equipo opcional (aparte de los fuegos): "batidora"|"microondas"|"plancha"; el horno sale de los carriles
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
    // formas de item: {a,n,u} · {a,esc,n,u} · {esc,n,txt} · {n,txt}; `esc` admite "sub" (sublineal) o "fijo" (no escala: aceite de freír, agua de cocción que es la profundidad de la olla)
  ],
  "notaEscala": "Qué no escala en línea recta y por qué.",

  "pasos": [
    {
      "id": "sofrito",          // único en la receta; lo usan dep y junto
      "t": "Sudar la verdura al {5}",
      "carril": "sarten",
      "off": 13,                // minuto de inicio desde el arranque (puede ser decimal)
      "min": 11,                // duración en minutos (puede ser decimal)
      "s": 660,                 // duración exacta en segundos; el cronómetro usa esto
      "f": 5,                   // potencia 0-9 (escala vitro canónica); null si el carril no es fuego
      "tapa": "TAPA PUESTA",    // null | "sin tapa" | "TAPA PUESTA" | "tapa fuera" | "TAPA A MEDIAS"
      "holgura": "flexible",    // flexible | ajustado | critico
      "dep": ["cortar"],        // opcional. Pasos de OTROS carriles que deben estar hechos
      "junto": "macarrones",    // opcional. Paso que corre en paralelo cuando el momento exacto importa
      "meto": "el bol: 160 g de cebolla + 120 g de zanahoria",  // qué entra, cuánto y de qué recipiente sale
      "saco": "",               // qué sale y adónde; el último paso siempre lo lleva
      "nota": "La señal que mirar y la única corrección (80 caracteres, 2 frases). Puede ir vacía.",
      "hitos": [{ "en": 180, "t": "Destapa, remueve y vuelve a tapar" }],   // opcional
      "sinHitos": "",           // opcional. Solo en pasos de fuego u horno de 6 min o más SIN hitos
      "p": "El porqué físico del paso, en párrafos y listas (ver «Prosa estructurada»). Esto es la ficha."
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

### Potencia en el texto
Toda potencia que el texto libre nombra se escribe `{N}` (0-9, escala vitro canónica): «baja al {3}», «del {4} al {5}». La app la traduce a la cocina de cada usuario al mostrar (ADR-0014), y el motor del cronómetro y lo guardado siguen usando la receta canónica. `scripts/sc potencias --marcar` marca lo que falte (idempotente); lo dudoso («del N» suelto, «sube a N», «potencias del 0 al 9») lo lista sin tocarlo y se revisa a mano. No marques minutos, tandas ni cantidades. Ver `docs/herramientas.md`.

### `nota` y `p`
Son los dos textos libres del paso y no se repiten entre sí ([ADR-0017](../adr/0017-dos-textos-por-paso-orden-compuesta.md)). `nota` es la **señal**: cómo sabes que el paso está y la única corrección; se mira de reojo con las manos ocupadas y puede quedar vacía si no hay nada que vigilar. `p` es el **porqué**: se lee antes, sentado, en la ficha y en el detalle del paso. La `nota` se enseña en marcha, junto al reloj y al próximo aviso; no siempre cabe en pantalla, y lo que no cabe se corta con «…» y se lee en el detalle. Lo que la app ya pinta como dato no se escribe en la `nota`:
- la potencia de `f` (ni como `{N}` igual a `f`), la tapa y la duración, que salen de `f`, `tapa` y `s`;
- minutos de reloj («minuto N») ni avisos de paralelismo, que se calculan desde los `off`;
- el porqué, que va en `p`.

A su vez, `p` no repite cantidades, potencias ni el texto de los avisos.

Topes de partida (se calibran con el piloto): `nota` de 80 caracteres y 2 frases como mucho; `p` de 600 y párrafos de 240. Ver reglas 30-36.

### Orden compuesta
La tarjeta del cronómetro no lee un campo de orden: la **compone** con los datos del paso y enseña la parte que toca según el momento. No hay campo nuevo. Si más de 1 de cada 10 pasos se lee mal compuesto tras cocinar el piloto, se añade un `orden` opcional que sustituye la línea METE solo en esos pasos (ADR-0017).

| Momento | Qué enseña | Sale de |
|---|---|---|
| Antes de empezar | METE · fuego · tapa · duración | `meto` · `f` · `tapa` · `s` |
| En marcha | reloj, próximo aviso y SEÑAL | `s`, `hitos`, `nota` |
| Al vencer | SACA (y lo que viene después) | `saco` |

- La tapa sale siempre («sin tapa», «tapada» o «tapa a medias»). Si `f` cambia respecto al paso anterior del carril, el chip dice «fuego 4 → 5».
- `t`: etiqueta del paso, en infinitivo o nombre, de 36 caracteres o menos y sin duración ni tapa. Lleva `{N}` solo si el paso consiste en cambiar la potencia.
- `meto`: lo que entra, `origen: cantidad y qué`, con « + » entre elementos y de 36 caracteres o menos por elemento. Sin medida de corte (va en `mep` y en `cortes`).
- `saco`: lo que sale y adónde, con el mismo separador y el mismo tope por elemento.
- `hitos[].t`: una acción en imperativo, de 44 caracteres o menos.
- Una misma función pura da las tres salidas de la orden: pantalla («METE 500 g de patata · la fuente», «fuego 4 · sin tapa · 18 min»), una línea para la notificación («1ª fritura · METE 500 g de patata · fuego 4») y la frase hablada de la voz.

### Prosa estructurada
`p`, `lectura`, `notaEscala`, `porque`, `mep.porq`, `cortes.p` y el `p` de las técnicas admiten un mini formato dentro de la cadena, sin cambiar el tipo del campo:
- **línea en blanco**: párrafo nuevo;
- **línea que empieza por `- `**: elemento de lista;
- **`<b>`** sigue valiendo, y nada más (ni Markdown ni otro HTML).

En JSON los saltos van como `\n`. `scripts/sc formatear` los conserva. El primer párrafo de `p` es el porqué en una o dos frases y se sostiene solo, porque es lo que lee la voz. Después, si hace falta, el porqué de los avisos en lista y qué pasa si no se hace. Ejemplo:

```
"p": "Aquí la patata se cuece, no se dora: el centro se hace del todo y la costra llega en la segunda fritura.\n\n- Despegar a los 2 minutos: la patata cruda se pega y se dora ahí antes de tiempo.\n- Pinchar a los 15 minutos: es la única señal fiable.\n\nFrita una sola vez sale blanda y sin costra."
```

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
20. AVISO (estricto) · Cambio de potencia que el texto del paso no nombra (lee el número dentro de `{N}`).
21. AVISO (estricto) · `p` de menos de 120 caracteres.
22. AVISO (estricto) · Tapa que cambia a mitad de un paso (debía ser dos pasos).
23. AVISO (estricto) · Tag de `tags.tecnica` sin ficha en `data/tecnicas/`.
24. AVISO · Veto presente en ingredientes (garbanzos, guisantes, champiñones, coliflor, brócoli, casquería, anchoas, alcaparras, marisco).
25. ERROR · El verbo de potencia del texto casa con el cambio real de `f` del carril: «baja al N» exige que el mando baje (N menor que el máximo entre la potencia anterior y `f`), «sube al N» que suba (N mayor que el mínimo). AVISO si «del X al N» y X no es ni la potencia anterior ni `f`. Los números se leen dentro de `{N}`.
26. AVISO (estricto) · `escala {min, max, paso}` ausente. ERROR si es incoherente: `min` o `paso` menores que 1, `max` menor que `min`, o `raciones` fuera del rango o que no cae en la escala (`min` + k·`paso`). AVISO (estricto) si un ingrediente contable (huevos, dientes, unidades) no sale en múltiplos de 0,5 en alguna de las raciones permitidas.
27. ERROR · Potencia sin marcar en un texto libre (sin `{N}`), o llave `{` que no forma `{0-9}`. AVISO (estricto) · Potencia dudosa sin marcar (el detector no está seguro: «del N» suelto, «sube a N», «mantén el N», «potencias/escala N», «N de …»): márcala `{N}` si es potencia.
28. ERROR · `tags.momento`, si está, no va vacío (omítelo para «cualquier momento») ni repite valores. AVISO (estricto) · Receta de más de 60 min con `cena` y sin `comida` (cena larga, poco probable entre semana).
29. ERROR · `tags.equipo`, si está, no va vacío ni repite valores, y `horno` declarado exige un carril `tipo: "horno"`. AVISO · `horno` declarado con carril de horno (redundante: se deriva de los carriles). AVISO (estricto) · Un paso (`t`, `nota`, `p`, `meto`, `saco`, `sinHitos`, hitos) nombra «batidora» o «túrmix» sin `batidora`, o «microondas» sin `microondas`, en `equipoDe` (lo derivado más lo declarado). La plancha va siempre declarada (ocupa un carril de fuego) y no tiene regla de menciones.

### Reglas de legibilidad (30-36)
Nuevas con ADR-0017. **Solo con `sc validar --legibilidad` hasta que pasen las 33 recetas**; entonces se funden en `--estricto`. Hasta entonces `sc validar --estricto` sigue dando 0 errores y 0 avisos. Los caracteres se cuentan como los ve el usuario: `{N}` vale 1 y `<b>` vale 0. Los topes son de partida y se calibran con el piloto.

30. AVISO de legibilidad · `t` de 36 caracteres o menos y sin duración escrita («N min», «N s»).
31. AVISO de legibilidad · `nota` de 80 o menos y 2 frases como mucho; sin «minuto N»; sin `{N}` igual a `f`.
32. AVISO de legibilidad · Cada elemento de `meto` y `saco` de 36 o menos; sin mm ni cm.
33. AVISO de legibilidad · `hitos[].t` de 44 o menos.
34. AVISO de legibilidad · `p` de 600 o menos, con párrafos de 240 o menos y 4 como mucho; `descripcion` de 200 o menos; `lectura` de 800 o menos y con algún salto; `porque` y `mep.porq` de 280 o menos.
35. ERROR · Los pasos del array van en orden de `off` (exige antes reordenar 5 recetas; la cola de cada carril no cambia).
36. AVISO de legibilidad · Frase de la `nota` cuyas palabras de contenido están en `p` en un 80 % o más.

La regla 20 (nombrar la potencia nueva en el texto) deja de hacer falta cuando el chip enseñe «4 → 5»; hasta entonces se cumple desde `t` o `p`. La 21 y la 25 no cambian.

**Estado en el código** (`Validador.kt`, más `tools/Despensa.kt` para la 12): implementadas 1-8, 10-21 y 23-29, y las de legibilidad 30-36 en `ValidadorLegibilidad.kt` (topes en `TopesLegibilidad.kt`; solo con `--legibilidad`; la 36 solo compara frases de la `nota` con 3 o más palabras de contenido). **Sin implementar**: la 9 (dos formas de corte en una línea: es lenguaje natural, sin forma fiable de detectarlo) y la 22 (la tapa es un único campo por paso, así que un cambio a mitad solo se vería en el texto libre de `hitos`, con demasiados falsos positivos). Detalles: la 15 mira `t` y `nota` de los pasos de mesa (cortar, picar, laminar, trocear, rallar, dados); la 19 busca número + mm|cm en `t`/`p` de cada corte; la 20 mira el número de potencia en `t`+`nota` cuando `f` cambia respecto al paso anterior del carril; la 26 y la 27 se describen arriba (la 27 usa el detector `MarcaPotencia` sobre los mismos campos de texto libre que traduce la app: sus `cambios` seguros dan ERROR y sus `dudosos` dan AVISO con el mensaje «potencia dudosa sin marcar»; lo ya marcado `{N}` no da nada); la 25 analiza `t` y `nota` de cada paso con `f` contra la transición potencia anterior del carril → `f` (sin anterior, solo contra `f`), por frases, y los `hitos[].t` como condicionales solo contra `f` (no mira `p`); la 24 usa raíces (garbanzo, guisante, champiñón, coliflor, brócoli, casquería, hígado, anchoa, alcaparra, marisco, gamba, langostino, mejillón, calamar, pulpo, sepia, almeja…) al inicio de palabra, en `grupos`.

Los puntos 14, 15 y 17-18 no estaban en la skill legada (14 y 15 salen de su §3a y §5); la 24 es una red de seguridad. Si el validador implementado difiere de esta lista, manda el código y se corrige este documento.

## Decisiones y fuentes
- Origen: `docs/legado/skill-receta-cronometrada.md` §5 y `docs/spec/app-legado.md` §7.
