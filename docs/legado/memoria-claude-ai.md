# Memoria del proyecto Sous Chef

Lo que Claude había aprendido de ti en este proyecto, limpio y sin duplicados, listo
para pegar en la cuenta nueva (ver el paso 6 de `../LEEME.md`). Volcado el 2026-10-13
desde los cinco archivos de memoria del proyecto y el de la cuenta.

Las reglas que ya están en las instrucciones del proyecto (vetos, raciones, potencias,
tapa, completitud, espejo) no se repiten aquí: viven en `1-proyecto/instrucciones.md`.

## Quién eres

- Joan López Ferrer, vives en Sant Joan (Mallorca).
- Proyecto personal de cocina en español: recetas concretas con ejecución cronometrada,
  para 2 raciones, con poco desperdicio.

## Tu cocina

- Tres fuegos: grande, mediano y pequeño. Escala de potencia 0-9.
- Tres sartenes con tapa y una plancha. La plancha y la sartén más grande solo encajan
  en el fuego grande, así que no pueden trabajar a la vez.
- Una sartén de saltear nueva, más honda, que encaja en el fuego mediano.
- Una sola bandeja de horno. Horno y microondas.
- Compras sobre todo en Mercadona. La carne picada mixta viene en palanganas de 500 g
  divididas en dos de 250 g.

## Cómo usas el sistema

- Consultas las recetas en el móvil mientras cocinas, no en el PC del trabajo.
- La app tiene dos pestañas de uso diario, Recetas e Historial (y Técnicas). **Quitaste
  la cola de platos porque no la usabas: no quieres que te la propongan.**
- Las recetas anteriores a la app eran fichas y cronómetros HTML sueltos (están en
  `importar/` y ya convertidas en `data/recetas/`).
- El cronómetro tiene que ser autoexplicativo: cada acción (p. ej. pintar el pescado con
  aceite) aparece como orden en el paso donde toca, no solo en la portada o la checklist.

## Cómo quieres que trabaje Claude

- Lee la despensa (`referencias/despensa.md`) antes de preguntar o proponer nada; es la
  fuente de verdad, no la memoria.
- Si falta algo, propón la sustitución desde la despensa (lima → limón, queso fresco
  mexicano → feta, albaricoque → melocotón).
- Enseña la receta completa para revisar antes de lanzar ningún proceso; tú ajustas los
  tiempos si hace falta.
- Las tablas de tiempos se comprueban aritméticamente: tú pillas los errores.
- Prefieres ver opciones y elegir antes de que Claude se comprometa con una dirección.
- Cocinando en directo, responde exactamente a la pregunta que haces antes de seguir.
- Mise en place con pocos boles: el recipiente frío sirve de contenedor, y en la tabla
  de cortar primero lo seco.
- La cebolla crujiente de bote va siempre al final, encima, nunca mezclada.
- Platos con caldo: sal poco a poco a mitad; pimienta al final, antes de mantecar.

## A confirmar al importar

La memoria tenía dos datos que se contradicen. Elige uno y borra el otro antes de pegar:

- **Hora de servir por defecto: 15:00** (lo dijiste el 28 de septiembre) **o 15:30**
  (resumen automático anterior). La skill, de todos modos, no pregunta la hora: la app
  recalcula el reloj desde la que pongas al abrir el cronómetro.
- **Vitrocerámica o inducción.** Las instrucciones y la skill dicen vitrocerámica; un
  resumen automático decía inducción. Se ha quedado vitro en todo lo demás.

## Descartado por obsoleto (no lo importes)

- «Reconstruir la cola de platos con prioridad de perecederos»: quitaste la cola.
- Rutas `/mnt/skills/plugins/receta-cronometrada/...` y el flujo de construir dos HTML
  (ficha + cronómetro) por receta: hoy la receta es un documento en la base de datos de
  la app y la ficha y el cronómetro son vistas de la app.
- El id `5b09ba82-…` de la app: era el iframe interno de la misma app, no otra.
