# Revisión de la v1.6

> Propósito: guion para que Joan revise todo lo nuevo de la 1.5 y la 1.6 antes del push · Audiencia: Joan · Actualizado: 2026-10-24 · Relacionado: docs/analisis/v1.6.md, docs/decisiones.md § Ronda 15 y § Ronda 16

## Resumen
- El emulador `trombito-36` está abierto con la **1.6.0** instalada sobre la 1.4 de debug.
- Para el móvil: `adb install -r app/build/outputs/apk/debug/app-debug.apk`. El debug convive con la app de Play porque su paquete termina en `.dev`.
- Cada punto dice qué tocar y qué tiene que pasar. Marca **OK** o **KO**.
- Para un KO, apunta el número del punto y lo que viste. Si puedes, añade una captura. Me lo pasas todo junto y lo corrijo antes del push.

## Ya comprobado por Claude en el emulador
- [x] Al actualizar de la 1.4 a la 1.6 salen las novedades con confeti, y «También en 1.5» se despliega.
- [x] Con Atrás se cierra, y al volver a abrir la app no se repite.
- [x] Recetas: dice 24 recetas, y el chip «Nueva» sale en las recetas sin estrenar.
- [x] Ficha del costillar: «Empieza a las 12:38» a las 15:30, el botón «Empezar a cocinar», el diagrama de tiempos y Compartir al pie de Ingredientes.
- [x] Logcat sin errores de la app.
- La suite completa está en verde: 661 tests o más, más ktlint, detekt, lint, `sc validar --estricto` (24 recetas con 0 errores y 0 avisos), check-docs y test-hooks.

## Observaciones para que decidas
- **O1. Confeti.** Cae por encima del texto, no por detrás (captura `docs/capturas/v1.6-novedades.png`). ¿Te vale o lo paso detrás?
- **O2. Familias.** Las bases nuevas (asado, fritura, hamburguesa, postre y crema) entran en la familia «Otras», con 5 recetas. ¿Les doy familia propia? Por ejemplo, «Postres» y «Carnes», con su color.
- **O3. «Todo junto».** Suma los repetidos sin la equivalencia casera. Por ejemplo, en el costillar, «25 ml vinagre de vino», que son 15 + 10.

## Novedades (1.6)
1. [ ] En el móvil, al actualizar desde la versión que tengas, salen las novedades una sola vez.
2. [ ] «A cocinar», tocar fuera y Atrás cierran las novedades.
3. [ ] Con Ajustes de Android → Accesibilidad → «Quitar animaciones», el confeti sale quieto o no sale.
4. [ ] Ajustes → Acerca de → Novedades: salen la 1.6 y la 1.5, sin confeti.
5. [ ] En la 1.5, «Receta nueva: costillar…» abre su ficha.
6. [ ] Con una cocinada en marcha, al reabrir la app no salen las novedades.

## Recetas
7. [ ] El chip «Nueva» y el filtro «Nuevas»: Filtros → Mostrar → Nuevas da 7 recetas. Cocina una y deja de ser nueva.
8. [ ] Revisa cada receta nueva entera: ingredientes, pasos y tiempos. Dudas abiertas:
   - **Espaguetis carbonara** (25 min, 2 fuegos): ¿cuántos minutos dice tu paquete de espaguetis? He puesto 9. ¿El Grana lo tienes en cuña o rallado?
   - **Pechugas empanadas** (30 min, 1 fuego): ¿tus filetes miden 5-6 mm? Si pasan de 1 cm, hay que aplanarlos.
   - **Hamburguesa smash** (35 min, plancha en el grande y sartén en el mediano): ¿caben 2 bolas aplastadas de 11 cm en la zona caliente de la plancha? Sobran unos 140 g de mayonesa: ¿te vale?
   - **Tortilla de patatas** (50 min, 1 fuego): ¿tu sartén mediana mide 20-22 cm o 24? ¿La quieres así de jugosa?
   - **Arroz con leche** (84 min, 4 raciones: 2 templadas hoy y 2 en la nevera): ¿la leche es entera? ¿Te valen 4 raciones?
   - **Crema de calabacín** (35 min, unos 450 g por cuenco): ¿la quieres de plato único o de primero, que daría para 3?
9. [ ] **Pollo con almendras:** la salsa pasa a 200 ml de caldo, 8 g de maicena y 25 ml de soja, para que moje el arroz. El vaso medidor queda casi lleno.
10. [ ] **Costillar** (1.5): lo revisaste antes. Comprueba que sale con el boniato en gajos de 3 cm.

## Ficha
11. [ ] Compartir, al pie de Ingredientes: manda la lista que tengas en el selector («Todo junto» o «Por zona»), con el título y las raciones.
12. [ ] «La última vez»: cierra una cocinada con nota. La nota sale arriba de Resumen y en la portada del cronómetro, y «Ver N más» enseña las anteriores.
13. [ ] «Tu ritmo»: con 2 o más cocinadas de la misma receta que se desvíen 3 min o más, sale «Tu ritmo: sueles tardar…» debajo de «Empieza a las».

## Cronómetro
14. [ ] Ajustes → Tu cocina en gas, y arranca una receta: la notificación y la alarma enseñan la potencia en tu escala.
15. [ ] La alarma con la pantalla bloqueada: el botón «Parar» se lee bien (contraste) y para la alarma.
16. [ ] Con TalkBack: en cada carril, «Hecho» dice el carril y el paso. Un paso vencido se anuncia como «Vencido».
17. [ ] Deshacer, Pausa y +30 s se tocan bien con el dedo, porque ya miden 48 dp. Prueba con letra grande.
18. [ ] Con las animaciones quitadas, las tarjetas compactas (4 carriles) no tienen texto en movimiento, sino 2 líneas.
19. [ ] Cierre:
   - el campo de la nota dice «Para la próxima vez (opcional)»;
   - el teclado no tapa «Ya está, a la mesa»;
   - después sale un botón «Deshacer», durante 12 s.
20. [ ] «Compartir informe» en el cierre: manda el desvío de cada paso.
21. [ ] Las notificaciones del crono siguen abriendo el cronómetro al tocarlas. El deep link público se ha quitado.

## Ajustes e historial
22. [ ] Ajustes → Historial → Cargar copia: carga tu copia buena y no duplica nada. Con un archivo cualquiera, da error.
23. [ ] Los textos nuevos: «Te damos la bienvenida», «Empezar a cocinar» y la pantalla vacía sin «sincronizar».

## Herramientas (en el PC)
24. [ ] `scripts/sc historial <copia.json>` con una copia de tu historial: lista los pasos que más se desvían.
25. [ ] `./gradlew -q :app:imprimirVersion` da `1.6.N` y el versionCode.

## Al aprobar
- Dime OK y hago el push a main. El CI publica la v1.6.N en GitHub y en la pista interna de Play.
- Antes, se pueden subir las capturas nuevas a la ficha de Play.

## Decisiones y fuentes
- El alcance y las decisiones están en `docs/analisis/v1.6.md` y en `docs/decisiones.md` § Ronda 15 y § Ronda 16.
