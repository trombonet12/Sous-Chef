# Revisión de la v1.7

> Propósito: guion para que Joan revise todo lo nuevo de la 1.7 antes del push · Audiencia: Joan · Actualizado: 2026-10-26 · Relacionado: docs/analisis/v1.7.md, docs/decisiones.md § Ronda 17 y § Ronda 18, docs/adr/0015-copia-automatica-del-historial-en-carpeta-saf.md, docs/adr/0016-temporizador-libre-y-recordatorio-de-empezar.md

## Resumen
- El emulador `trombito-36` está abierto con la **1.7**, instalada encima de la 1.4 de debug, que traía historial.
- Para instalarla en el móvil: `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Es el paquete `.dev`, que convive con la app de Play.
- **Cómo marcar:** cada punto dice qué tocar y qué tiene que pasar. Marca **OK** o **KO**. Si es KO, apunta el número y lo que viste, con captura si puedes.
- Hay muchos puntos. Si vas justo de tiempo, prueba primero los marcados con ★ y deja el resto para cuando cocines.

## Ya comprobado por Claude
- [x] Gate completo en verde:
  - app: 1033 tests;
  - receta: 365;
  - tools: 6;
  - ktlint, detekt y lint;
  - `sc validar --estricto`: 33 recetas, 0 errores y 0 avisos;
  - check-docs, check-claude-assets, test-hooks y los tests del script de Play.
- [x] Al pasar de la 1.4 a la 1.7 salen las novedades. La migración de la base de datos conserva el historial: 11 cocinadas más 1 nueva.
- [x] Recetas: dice «33 recetas». Salen «¿Qué cocino hoy?» con 3 propuestas y el cuadradito «Cenas rápidas» (7). Esa lista va filtrada por «Hasta 30 min» y «Cena».
- [x] Ficha del wrap:
  - «Avísame a las 15:06»;
  - el marcador «Quiero hacerla»;
  - «Pide plancha»;
  - «Anotar que la he cocinado», que suma «La has cocinado 1 vez · última hoy».
- [x] Cocina:
  - en el ⋮ están Ingredientes, «+ Temporizador» y Órdenes de voz;
  - el temporizador de 1 min avisa con la notificación «Temporizador: 1 min» y marca «¡Listo!».
- [x] Historial:
  - arriba sale «Este mes: …»;
  - en las tarjetas, «Editar nota» y «Borrar».
- [x] Ajustes → Historial → «Copia automática»: está desactivada y con «Elegir carpeta».
- [x] Logcat sin errores de la app.
- Capturas: `docs/capturas/v1.7-*.png`.

## Cambios tras la revisión (2026-10-26)
- «¿Qué cocino hoy?» pasa a ser un chip junto a «¿Qué me da tiempo?», con un panel flotante y ↻.
- «Cenas rápidas» queda como filtro: la rejilla vuelve a ser 3×3 y ya no hay acceso directo de cenas.
- La hora de cenar sale en el tutorial (página Horario) y en el resumen de Ajustes.
- La ficha sigue el modo oscuro.
- Los menús ⋮ llevan icono y el mismo estilo. «Ingredientes» se ha quitado de la cocina.
- O2 y O3 aceptadas tal cual.

## Recetas y cocina
1. [x] **Cenas rápidas:** Filtros → «Hasta 30 min» + «Cena».
2. [ ] **Hora de cenar.** Ajustes → Horario → «Hora de cenar», a las 21:00. Después de las 17:00, la ficha de una receta de cena dice «para cenar a las 21:00».
3. [x] **¿Qué cocino hoy?** El chip abre un panel flotante, y ↻ cambia las propuestas.
4. [ ] **Filtros:**
   - sección «Momento» (Comida, Cena, Desayuno, Merienda, Postre);
   - «Hasta 30 min»;
   - «Quiero hacerla» en Mostrar;
   - «Con lo que tengo» en Equipo.
5. [ ] **Tu equipo.** Ajustes → Tu cocina: «Tengo batidora / microondas / plancha». Quita la batidora: la crema de calabacín dice «No tienes batidora» y desaparece con «Con lo que tengo».
6. [ ] **Búsqueda vacía.** Busca algo que no exista, como «paella». Salen «Pedir esta receta», que abre el Buzón con el plato escrito, y «Quitar filtros».

## Ficha
7. [ ] ★ **«Tu ritmo»** dice «Sueles tardar N min más: empieza a las…» y, debajo, «según la receta…». En la lista, «con tu ritmo».
8. [ ] **Tu historial con la receta:** «La has cocinado N veces · última hace… · sueles tardar…».
9. [ ] **«Anotar que la he cocinado»**: la fecha y la hora se ajustan, pero nunca a una hora futura.
10. [ ] **«Avísame a las HH:mm»:**
    - ponlo un par de minutos en el futuro, con «Servir a» en una hora cercana;
    - apaga la pantalla;
    - llega la notificación «Toca empezar…» y, al tocarla, se abre la ficha.
11. [ ] **«Compartir "Hoy comemos"»** al pie de Ingredientes.
12. [ ] **Notas de «La última vez»** con fecha («hace 3 semanas · 12 oct»).

## Cronómetro
13. [ ] ★ **Cierre:**
    - «Real N min · previsto M (+D)»;
    - la valoración «Repetir / Ajustar / No repetir», que también se puede elegir durante «Se guarda en N s»;
    - «Guardar y salir ya»;
    - «Deshacer»;
    - el botón «Ya está, a la mesa», siempre visible abajo.
14. [x] ~~Ingredientes en la cocina~~: retirado a petición de Joan.
15. [ ] **Voz:**
    - «Chef, cuánto de sal»;
    - «Chef, ingredientes de este paso»;
    - «Chef, pon 5 minutos»;
    - ⋮ → «Órdenes de voz» enseña la lista.
16. [ ] **Temporizador con la pantalla bloqueada:** suena como un paso.
17. [ ] **Valoración de Play:** no se puede probar en debug porque no viene de Play. Comprobarlo en la versión de Play tras la 3.ª cocinada.

## Historial y copia
18. [ ] **Valoración en la tarjeta:** «Para repetir», «A ajustar» o «No repetir».
19. [ ] **Editar nota**, y **Borrar** con «Cocinada borrada · Deshacer».
20. [ ] ★ **Copia automática:**
    - elige una carpeta;
    - cierra o anota una cocinada;
    - en la carpeta aparece `souschef-historial-AAAAMMDD-HHmm.json`;
    - tras 6 cocinadas quedan 5 archivos;
    - una copia hecha a mano (`sous-chef-historial-…`) no se borra nunca.
21. [ ] **Buzón:** la casilla «Adjuntar resumen de uso» está desmarcada. Al marcarla se ve el resumen: solo cifras, sin notas ni nombres.

## Accesos directos
22. [ ] Mantén pulsado el icono:
    - «¿Qué me da tiempo?»;
    - «Volver a la cocina», con una cocinada en marcha.

## Recetas nuevas (revísalas enteras: ingredientes, pasos y tiempos)
23. [ ] **Quesadillas de pollo y pimiento** (30 min, 1 fuego). Dudas:
    - ¿Tus tortillas miden unos 25 cm? Si son de 30, sale una por tanda y pasa de 30 min.
24. [ ] **Salmón glaseado con soja y miel** (27 min, 2 fuegos). Dudas:
    - los lomos tienen que ser de 2,5-3 cm;
    - ¿te parece cómodo quitar la grasa con papel y pinzas?
25. [ ] **Wrap César de pollo** (24 min, plancha). Dudas:
    - ¿tu salsa César de bote lleva anchoa? Mira la etiqueta: si la lleva, hay que cambiarla;
    - la ensalada de noquis también la usa.
26. [ ] **Tacos de pollo con piña** (25 min, 2 fuegos, minitortillas). Dudas:
    - ¿la piña es en su jugo o en almíbar?
    - ¿caben 4 rodajas en la plancha?
27. [ ] **Albóndigas en salsa** (60 min, 32 albóndigas, la mitad al congelador). Dudas:
    - el sofrito va al {4};
    - ¿te cuadra formar 32 bolas en 12 min?
28. [ ] **Berenjenas rellenas de atún** (60 min, horno a 200 °C y grill). Dudas:
    - ¿la quieres también para cenar?
    - ¿lleva Grana o basta con el queso rallado?
29. [ ] **Quiche de bacon y queso** (62 min, molde de 24 cm, base horneada a ciegas con arroz como peso). Dudas:
    - ¿el molde es de 24 cm y desmontable?
    - ¿la lámina de hojaldre es redonda o rectangular?
30. [ ] **Natillas de limón y canela** (35 min + 2 h de nevera, 6 yemas, leche desnatada + nata). Dudas:
    - la galleta va en caliente y queda blanda; ¿o crujiente al servir?
    - las 6 claras van a un táper.
31. [ ] **Focaccia de cherry y olivas** (180 min, fuente de 28×20, levado por volumen). Dudas:
    - ¿la fuente es de cristal o de cerámica?
    - ¿las olivas llevan hueso?
    - para la próxima compra: sal en escamas y romero.
32. [ ] **Lasaña** (ajustada con lo que viste): 16 placas (4×4), 750 g de carne, 1,25 l de bechamel, 4 raciones y 161 min.
33. [ ] **Leche desnatada:** el arroz con leche lleva 40 g de mantequilla.

## Al aprobar
- Dime OK, o los KO, y hago el push a main.
- El CI publica la **v1.7.N** en GitHub, en la pista interna **y en la prueba cerrada (Alpha)**. Es la primera vez que se promociona sola.
- Después compruebo en el CI que sale en las dos pistas y rebaso el PR #5 de Dependabot.
