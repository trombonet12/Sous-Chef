# Especificación funcional de la app vieja (artifact HTML)

> Propósito: contrato de paridad para la app Android. Todo lo que hacía la app vieja está aquí, con sus algoritmos.
> Audiencia: agentes que implementan F5-F7. · Actualizado: 2026-10-13 · Fuente: `docs/legado/sous-chef-publicada.html`
> (VistaFicha l.1162-1663, VistaCrono l.1664-3050, VistaTecnica l.3051-3141, Shell l.3142-4300).

La UI Android puede cambiar de **forma** (Material 3, mejor tipografía, gestos). No puede perder **funciones**. Las marcadas
**[BUG]** no se copian: se corrigen como se indica.

## 0. Navegación y shell
- Rutas viejas: lista `#/`, ficha `#/r/<slug>`, crono `#/c/<slug>`, técnica `#/t/<slug>`, `#/tecnicas`, `#/historial`.
- Barra de navegación con contador vivo de documentos por pestaña. En Android: abajo **Recetas · Despensa · Historial**;
  Técnicas, Ajustes y Cerrar sesión en el menú superior (decisión del usuario).
- Cabecera con subtítulo de conteo: "N recetas" / "N técnicas" / "N veces".
- **Banda de avisos** arriba de cada pantalla:
  - Sin conexión: "Sin conexión. Ves la última copia guardada en este teléfono y lo que marques se enviará cuando vuelva."
  - "Conectando…" si no hay nada en caché.
  - "N cambio(s) pendiente(s) de enviar." si el outbox no está vacío.
  - Aviso transitorio (20 s por defecto) con botón opcional **Deshacer**.

## 1. Lista de recetas
- **Búsqueda** ("Buscar por plato o ingrediente"): substring en minúsculas sobre titulo, descripcion, tags.base, nombres de
  ingredientes de `grupos` y títulos de `pasos`. Mejora permitida: ignorar acentos.
- **Chips de categoría** por `tags.base`: solo si hay más de una base entre las recetas que pasan el filtro de texto. "Todas"
  + un chip por base (ordenadas) con contador que respeta el texto. Tocar el chip activo lo desactiva.
- **Orden**: favoritas primero, luego `actualizada` descendente.
- **Tarjeta** (abre la ficha):
  - titulo; descripcion recortada a 2 líneas.
  - **Huella** (mini-gantt): una fila de 5 px por carril; cada paso en `left = off/totalMin`, `width = max(1,2 %, min/totalMin)`
    (acotado). Leyenda con cuadrado de color y nombre de cada carril.
  - Chips: "<totalMin> min" (negrita, tabular) · "N fuego(s)" (carriles `tipo=="fuego"`, el horno no cuenta) · tags.base ·
    `compra` (teja) si `compra` no está vacía, si no `despensa` (oliva) · chip "hace X".
- **Chip "hace X"**: la fecha más reciente del historial para ese slug.
  - Nunca cocinada → "sin estrenar", resaltado (teja).
  - días entre medianoches UTC: ≤0 "hoy"; 1 "ayer"; <7 "hace N días"; <30 "hace N semana(s)" (días/7); <365 "hace N mes(es)"
    (días/30); si no "hace N año(s)". Resaltado a partir de 60 días. Detalle: "últ. 12 sep".
- **Estrella de favorita** en la esquina de la tarjeta (☆/★ ámbar): conmuta `preferencias.favorito` y la tarjeta salta arriba.
- Vacíos: "Nada con eso · Prueba con un ingrediente [o quita el filtro de categoría]." / "Todavía no hay recetas".

## 2. Ficha
- **Barra**: volver · "Servir a" (hora, por defecto **15:30**, no se recuerda) · "Cronómetro →" (pasa la hora elegida **una vez**
  al crono) · "Imprimir".
- **Enlaces a técnicas**: chip "Técnica: <titulo>" por cada `tags.tecnica` que exista como técnica (las que no existen se omiten).
- **Raciones recordadas**: stepper 1..20; tras 600 ms sin tocar se guarda `preferencias.raciones`; al abrir se restaura.
- Hoja tipo "papel" (siempre clara, también en tema oscuro). Secciones en orden, se omiten las vacías:
  1. **Portada**: titulo grande, descripcion en cursiva, fila de datos: celda viva "N raciones" + cada par `datos` `[grande, pequeño]`.
  2. **Ingredientes**: stepper RACIONES; grupos en rejilla (cabecera `g.n`, cantidad alineada a la derecha + nombre); `notaEscala`.
  3. **El tiempo**: gantt (ver §2.1), leyenda de carriles + "eje en minutos desde el arranque", y `lectura`.
  4. **Antes de empezar**: un bloque por `mep`: título `t`, `porq` en gris, líneas `l` (`orden:true` → lista numerada;
     `false` → viñetas). Las líneas llevan `<b>` (único HTML admitido).
  5. **Los pasos**: lista numerada: `t`; línea meta en mayúsculas "SARTÉN · 12 MIN · FUEGO 8 · SIN TAPA" ("SIN FUEGO" si `f`
     es null; sin tapa si `tapa` es null); prosa `p` (si falta, `nota`); sub-lista de hitos "+m:ss <texto>".
  6. **Técnica y decisiones**: `cortes` (SVG + `t` + `p`) y `porque` como lista de definiciones `[título, texto]`.
  7. **Hay que comprar**: lista escalada o "Sale entera de la despensa."; botón **Copiar la lista** (y en Android:
     **Añadir a la lista de la compra**, nuevo).
  8. `pie` en pequeño.
- **Copiar compra** (texto plano):
  ```
  Compra · <titulo> · N raciones   ("1 ración")

  <cantidad escalada> <nombre>     (sin el " *" final)
  ```
- **Imprimir**: A4, margen 13 mm, fondo blanco, 10 pt, se oculta el cromo, `print-color-adjust: exact` en el gantt,
  `break-inside: avoid` en filas, pasos, bloques mep, cortes y decisiones.

### 2.1 Algoritmos de la ficha
- **Escalado** (ingredientes y compra): `f = rac / receta.raciones`. Si el ítem tiene `txt`: no escala, se añade " *" si
  `esc=="sub"` y f≠1. Si no: `v = a · (esc=="sub" ? f^0,75 : f)`; formato: `v ≥ 10` → entero redondeado; si no, 1 decimal con
  coma; + " u" si hay unidad; + " *" si sub y f≠1.
- **Minutos**: redondeo a 0,5 con coma decimal ("2,5").
- **Eje del gantt**: `granu = total ≤ 60 ? 5 : 10`; `salto = ceil(total/5/granu)·granu`; marcas en 0, salto, …; si la última
  no es `total` y `total − última < 0,5·salto` se quita; se añade `total`. Rejilla cada 10 min.
  Barra: `left = off/total`, `width = min/total` (acotados). Si `totalMin` es inválido: `max(off+min)`.
- Fila del gantt: `t` + sublínea "<CARRIL> · fuego 5 · TAPA PUESTA" (o "sin fuego"); duración "<min>′" con "min a-b".

## 3. Técnicas
- **Lista**: búsqueda sobre titulo, resumen, categoria, `t`/`p` de pasos y consejos; chips por `categoria` (mismas reglas);
  orden alfabético; tarjeta con titulo, resumen, chips categoria y "N pasos".
- **Ficha de técnica**: enlaces inversos "Receta: <titulo>" a toda receta cuyo `tags.tecnica` la contiene (alfabético);
  portada (titulo, resumen, datos: categoría y "N paso(s)"); CÓMO SE HACE (`pasos` numerados); EL CORTE (si hay `cortes`);
  CONSEJOS ("— texto"); `pie`. Imprimible.

## 4. Cronómetro
Pantalla completa, **siempre oscura**, sin scroll salvo el panel central.

### 4.1 Apertura
- Espera a tener receta y técnicas. Estado guardado: el más reciente entre el local y el remoto (Android: solo local, ADR-0004).
- **Hora de servir inicial**: la que viene de la ficha si se pasó en esta apertura; si no, la del estado guardado; si no, 15:30.
  **[BUG corregido]**: antes el estado guardado pisaba la hora recién elegida en la ficha.
- Carriles = `carriles` declarados (sin duplicados). Un paso con carril no declarado crea carril gris #B0A899 tipo mesa. Se
  quitan carriles sin pasos. Cola de cada carril = pasos de ese carril **en orden del array**.
- Normalización de paso: `s` manda (si no, `min·60`); hitos: `en` redondeado, se descartan ≤0 o ≥s, ordenados, texto por
  defecto "Aviso"; `holgura` ∈ {flexible, ajustado, critico} (por defecto ajustado); `dep` [] y `junto` null por defecto.
- Sin pasos → "Esta receta no tiene pasos · No hay nada que cronometrar." + Volver.

### 4.2 Estado por carril
`{ i, restante, finAt, corriendo, pausado, a10, aFin, hechos{idxHito}, fin, salto, avisado, inicioPaso }` + global
`{ marcados, servir, abierto, foco, arrancado, pasosReales, ts }`.
- **Caducidad 12 h** desde `ts`. **[BUG corregido]**: caducado = se descarta entero (antes restauraba índices y marcas).
- `pasosReales` **se persiste** **[BUG corregido]**.
- Ids de la checklist estables por contenido (hash de la línea) **[BUG corregido]**: antes posicionales `mep-b-l`.

### 4.3 Tiempo de pared
- `quedan(c)`: fin → 0; parado o en pausa → `restante`; corriendo → `ceil((finAt − ahora)/1000)` (puede ser negativo: se
  muestra `+mm:ss`). **Nunca avanza solo**: hay que pulsar Hecho.
- `sugerido(off) = horaServir(hoy) − (totalMin − off) min`. **[BUG corregido]**: se calcula en segundos, sin truncar medios minutos.
- `restante(c) = max(0, quedan) + Σ s` de los pasos siguientes del carril. **LLEGADA** = ahora + max(restante(c)).

### 4.4 Portada (mise en place)
- Atrás, titulo, descripcion. "ANTES DE ENCENDER NADA": cada bloque `mep` con su título y sus líneas como **casillas
  marcables** (tachado + verde; se guardan). Sin `porq` (eso es de la ficha). Sin `mep`: "SACA ESTO ANTES DE EMPEZAR" desde
  `grupos` ("<b>cantidad</b> · nombre").
- Pie fijo: "SERVIR A LAS" + botón grande **Abrir la cocina**.

### 4.5 Abrir la cocina
- Foco: el carril preferido si vale; si no, el primero accionable; si no, el siguiente por hora; si no, el 0.
- `arrancado = ahora` si no lo estaba; `inicioPaso = ahora` en todo carril sin terminar que no lo tenga.
- Marca como avisados los carriles cuya hora ya pasó (que no pite todo a la vez). Pide permiso de notificaciones, mantiene
  la pantalla encendida, guarda y arranca el tic de 1 s. Si todo está hecho, cierra.

### 4.6 Cabecera
- **Banda "MIENTRAS NO MIRABAS" / "REANUDADO"** (hasta 4 líneas) con **REINICIAR** (doble toque: el primero muestra
  "¿SEGURO?" en rojo 4 s; el segundo borra el estado y vuelve a la portada) y ✕. Es el único sitio con REINICIAR.
- Fila: ‹ volver (a la ficha; el estado se conserva) · SERVIR (editable, sincronizado con la portada) · AHORA · LLEGADA.
- **Plan plegable** (abierto por defecto, ▴/▾):
  - Cabecera: carril vencido → "**<CARRIL> ya está** · pulsa Hecho para seguir"; tarde → "**N min por detrás.** Comerás sobre
    las **HH:MM**" (N = max(retraso, difFin)); si no → "**Vas en hora.** Llegada **HH:MM**, prevista **HH:MM**". Borde rojo si
    tarde o vencido.
  - Cuerpo: "ahora toca": una fila por carril sin terminar, ordenadas por `estado().orden` (número, nombre en su color,
    paso actual, texto de estado, relojito). Tocar enfoca el carril.
  - Si tarde: "**Prevista** HH:MM · **realista** HH:MM" · "**Aguantan la espera:** <títulos únicos con holgura flexible desde
    el índice actual de cada carril>." · "**No esperan, encadénalos:** <ídem crítico>."
- `retraso = max sobre carriles en estado 'ya' de round((ahora − sugerido(off actual))/60000)`;
  `difFin = round((llegada − servir)/60000)`; `tarde = retraso > 2 || difFin > 1`.

### 4.7 `estado(c)`
| clase | condición | texto | orden |
|---|---|---|---|
| fin | carril terminado | "terminado" | 9 |
| atender | corriendo y q ≤ 0 | "¡listo! pulsa Hecho" | 0 |
| pausa | en pausa | "en pausa" | 5 |
| marcha | corriendo | "en marcha" | 4 |
| bloq | dependencias pendientes | "espera a <paso (CARRIL)>" | 6 |
| espera | sugerido − ahora > 3 min | "a las HH:MM" | 3 |
| ya | resto | "empieza ya" (+ " · tocaba a las HH:MM" si tarde) | 1 |

**RANGO** para elegir foco (distinto del orden de la lista): atender 0, ya 1, marcha 2, pausa 3, espera 4, bloq 5, fin 9;
empates por hora sugerida. `siguienteAccionable` = primer carril con rango ≤ espera (los bloqueados nunca se enfocan solos).

### 4.8 Pestañas de carril
- Scroll horizontal; cada pestaña: nombre en su color, reloj mm:ss (gris parado, rojo pasado de cero, "—" terminado), paso
  actual o "terminado", segunda línea "fuego N · tapada|tapa a medias|destapada · <estado>" / "sin fuego · …" / naranja
  "espera a …" / "fuera del plan". Activa: borde superior de 3 px del color del carril. Parpadeo rojo si pasó de cero
  (respeta reducir movimiento). Pista "Toca una tarea o una pestaña para ir a ese recipiente" hasta el primer cambio.
- Cambiar de carril: tocar pestaña, fila del plan o fila de paralelos; **swipe** (|dx| > 60 y |dx| > 1,8·|dy|, cíclico).

### 4.9 Panel (un paso del carril enfocado; acento = color del carril)
- "<CARRIL> · paso i de n · sugerido HH:MM"; título grande.
- **Fuego** (solo `tipo=="fuego"`): número grande (gris si 0), barra de 9 segmentos, tapa (resaltada si PUESTA/MEDIAS).
- Píldoras: holgura ("aguanta espera" verde / "ajustado" / "no espera" rojo), "X min", "sin arrancar"/"en pausa".
- Requisitos: con `dep` → "ANTES HAY QUE TERMINAR" (rojo si falta alguno) o "REQUISITO CUMPLIDO" (verde) + "<paso> (<CARRIL>)";
  con `junto` → "A LA VEZ QUE <paso>".
- **MIENTRAS ESTO CORRE** (`paralelo()`): por cada otro carril: se salta si terminado, si corre con q > 0 o si tiene deps
  pendientes; `m = round((sugerido(off actual) − ahora)/60000)`; se incluye si m ≤ 12; orden por m. Etiqueta: vencido →
  "ya · Hecho"; pausa → "en pausa"; m ≤ 0 → "ahora"; si no "en N min". Línea 2: "fuego F · tapa" | "sin fuego" · "X min" ·
  "sugerido HH:MM". Cada fila salta a ese carril; las de m ≤ 0 resaltadas.
- Reloj grande (rojo si negativo); barra de progreso `(s−q)/s` con marcas de hitos; chips de hitos: `mm:ss` → "ahora"
  (resaltado 90 s) → "hecho".
- METO (verde) · SACO (salmón) · TAPA (rojo anaranjado); `nota` en serif.
- **Ver el porqué ▾**: despliega `p` + botones "Técnica: X →" (abren la técnica; el progreso se guarda). Se pliega al cambiar
  de carril y al pulsar Hecho.
- Pie: "Después aquí: **<siguiente t>** · X min al F" o "Último paso de este recipiente."

### 4.10 Mandos (fila fija abajo, ≥ 44 dp)
- **Principal**: "Hecho · siguiente" / "Hecho · cerrar" (último paso del carril); rojo si q ≤ 0.
  Bloqueado (deps pendientes, sin correr, sin `salto`): contorno rojo "Antes: <paso dep (16 car.)…> · ir a <CARRIL>" → salta a
  ese carril; el segundo botón pasa a **Empezar igual** (arranca y pone `salto=true`).
- **Play**: Empezar (`finAt = ahora + restante`, `salto=true`) / Pausa (`restante = quedan`) / Seguir.
- **+30 s**: corriendo → `finAt += 30 s`; si no `restante += 30`; rearma `aFin` si q > 0 y `a10` si q > 10. Vale vencido.
- **Hecho**: `pasosReales[id] = max(1, round((ahora − inicioPaso)/1000))`. No último: `i++`, `restante = s`, flags a cero,
  `hechos = {}`, `avisado = (sugerido ≤ ahora)`, `inicioPaso = ahora`. Último: `fin = true`; si todos terminados → cierre; si no,
  foco al siguiente accionable. Se puede pulsar Hecho sin haber arrancado el reloj.
- Carril terminado: título "Terminado", "Este recipiente ya está fuera del plan. Sigue por **<CARRIL>**." y un botón
  "Siguiente: <CARRIL> · HH:MM".
- **Anti doble toque**: cerrojo por "mando:carril" que se libera en la siguiente vuelta del bucle (no una ventana de tiempo).
  Reiniciar no lleva cerrojo (su doble toque de 4 s es la guarda).

### 4.11 Avisos (en cada tic y al volver a la app)
| Evento | Sonido | Vibración (ms) | Notificación |
|---|---|---|---|
| Hito alcanzado (solo si hace < 90 s; los más viejos se marcan sin sonar) | 660 Hz 0,14 s + 990 Hz 0,18 s a los 0,2 s | 90,70,90 | (CARRIL, texto del hito) |
| Quedan 10 s (`a10`) | 880 Hz 0,18 s | 120 | — |
| Cero (`aFin`) | 1046 Hz ×3 a 0/0,3/0,6 s (el último 0,45 s) | 150,100,150,100,300 | "<paso>: hecho" |
| Parado y desbloqueado llega a su hora sugerida (una vez, `avisado`; se rearma mientras está bloqueado) | 880 Hz | 100,80,100 | "Toca empezar: <paso>" |

En Android los avisos con la app cerrada los da la cadena de alarmas (ADR-0005); en primer plano, el tic.

### 4.12 Vuelta del segundo plano y recarga
- Si estuvo fuera > 8 s: banda "MIENTRAS NO MIRABAS" con hasta 4 líneas + triple pitido + vibración 200,90,200:
  hitos cruzados "<CARRIL>: <hito> · hace N min"; carriles que pasaron de cero "<CARRIL> · <paso>: terminado · hace N min";
  carriles parados y desbloqueados cuya hora pasó "<CARRIL>: toca empezar <paso> · hace N min".
- Al reabrir con estado `abierto` no caducado y no terminado: reanuda, barre y muestra el resumen desde `ts` o
  "REANUDADO · Seguimos donde lo dejaste."

### 4.13 Cierre
- "Todo fuera del fuego" · "<titulo>, listo. Emplata y llévalo antes de que pierda temperatura."
- Resumen: por carril "<CARRIL> N pasos · M min [de fuego]"; "HAS TARDADO N min desde que abriste la cocina";
  "SERVIDO HH:MM, previsto HH:MM". Triple pitido, vibración 200,100,200, notificación "Todo listo. A la mesa.".
- "LA PRÓXIMA VEZ… (OPCIONAL)" (≤ 160 car.) + **Ya está, a la mesa** →
  historial `{id: <slug>-YYYY-MM-DD-HHMMSS, slug, titulo, fecha, base, tecnica, minutosReales (null si no 0<m≤720), pasosReales
  [{id, t, segundos}] o null, nota}`; borra el estado a los **8 s**; aviso "Anotado: <titulo> · «nota»" con **Deshacer** 8 s
  (borra la entrada y cancela el borrado del estado); cierra el crono y va al historial.
- Accesibilidad: región viva que anuncia "<CARRIL> — <paso>: <estado>" solo cuando cambia (nunca el reloj).

### 4.14 Persistencia del crono
- Guardado con debounce de 400 ms. Doc `{slug, marcados, carriles, servir, abierto, foco, arrancado, pasosReales, ts}`.
- **[BUG corregido]** "más avanzado": antes sumaba índices de todos los carriles y solo en local. En Android el estado es
  solo local (una escritura por cambio, transaccional en Room); la regla deja de hacer falta.

## 5. Historial
- Orden por fecha (y hora del id) descendente.
- **Aviso de repetición** sobre la lista completa: una entrada se marca si la siguiente más vieja tiene la misma `base`
  → chip "<base> · dos seguidas" en teja.
- Búsqueda (si hay entradas) sobre titulo, slug, base, nota, tecnica.
- Fila: fecha "12 sep", titulo, chips (base, hasta 3 técnicas, "N min reales"), nota «…»; acciones **Repetir →** (si la
  receta existe; abre la ficha) y **Borrar** (doble toque, "¿Seguro?" 4 s).
- Vacíos: "Nada cocinado todavía · Al terminar un plato en el cronómetro, se anota aquí." / "Nada con eso".
- Nuevo en Android (opcional): ver los `pasosReales` de una entrada frente a lo previsto.

## 6. Diseño visual
- **Tokens shell** (claro / oscuro): papel #EFEDE3/#12130F · carta #FBFAF6/#1B1C17 · carta-2 #E7E5D9/#24261F ·
  tinta #191A14/#EDEDE2 · tinta-2 #4A4C40/#BFC0B2 · tinta-3 #6C6E60/#939586 · linea #D6D5C6/#2E3028 ·
  linea-fuerte #BDBCAA/#43463A · ámbar #B07408 (fondo #F6E7C6) / #E5AB46 (#33280F) · azul #2C6E92 (#D6E7F0) / #79BEDE
  (#14262F) · oliva #4F6B26 (#E0E8CD) / #A3C46A (#1D2611) · teja #A8421C (#F5DED4) / #E8865F (#331709). Radio 10.
- **Ficha (siempre papel)**: papel #F3EFE4, papel-2 #EAE3D2, tinta #17150F, tinta-2 #4E483C, tinta-3 #857C6A, meta #6E6656
  (4,94:1, línea de fuego y tapa), linea #D5CDB9.
- **Crono (siempre oscuro)**: fondo #0D0C0A, carta #1C1915, carta-2 #272219, linea #3A342C, texto #F4F1E8, apagado #B0A899,
  tenue #948B7C, meto #9ACB68, saco #EE9B79, tapa #E8836B, alerta #FF6B52, ok #8FC97A, acento por defecto #F0AE4A,
  borde-ok #3E5A34, borde-mal #5C2318, parpadeo #33140F; principal #F4F1E8 sobre #0D0C0A, urgente #FF6B52 sobre #2A0A05.
- **Color de carril**: acento, nunca texto de cuerpo. Para texto se aclara hacia blanco hasta contraste 4,6:1 sobre #272219.
  Colores de la casa: mesa #C0B7A6, sartén #F0AE4A, olla #6FBEE8, cazuela #E8836B, horno #B58CD0.
- **Fuentes**: Bricolage Grotesque (titulares, UI, números, todo el crono) y Newsreader (texto corrido, descripciones,
  notas). Números tabulares en relojes y contadores. Botones ≥ 44 dp. Respeta reducir movimiento.

## 7. Datos (forma observada, 2026-10-13)
- `recetas` (13): siempre `slug, titulo, descripcion, raciones, totalMin, datos[[a,b]], tags{base, tecnica[], proteina},
  perecederos[], compra[{a,n,u}], carriles[{id,nombre,color,tipo}], grupos[{n, items}], notaEscala, pasos, lectura,
  mep[{t,orden,porq,l[]}], cortes[{t,p,svg}], porque[[t,x]], pie, actualizada`; opcionales `creada`, `importada`.
  - items de grupos: `{a,n,u}`, `{a,esc,n,u}`, `{esc,n,txt}`, `{n,txt}`; `a` entero o decimal; `esc` solo "sub".
  - pasos: siempre `id,t,carril,off,min,s,f,tapa,holgura,meto,saco,nota,p`; opcionales `dep, junto, hitos[{en,t}], sinHitos`.
    `off`/`min` pueden ser decimales (salteado). `tapa` ∈ null, "sin tapa", "TAPA PUESTA", "tapa fuera", "TAPA A MEDIAS".
- `tecnicas` (17): `slug, titulo, resumen, categoria, pasos[{t,p}], consejos[], creada, actualizada`; opcional `cortes`, `pie`.
- `historial` (11): `slug, titulo, fecha, base, tecnica[], nota, minutosReales`; opcional `_id`, `pasosReales`. Ids legados
  `<slug>-<fecha>` y `<slug>-<fecha>-<HHMMSS>`.
- `preferencias` (2): `{slug, favorito, raciones, ts}`.
- `estado` (8): no se migra (todos caducados; el estado del crono pasa a ser local).
