# Planificación: tiempos, paralelismo, holgura, escalado

La planificación es el núcleo. Todo en minutos relativos al arranque (`off`, minuto 0 = coges el cuchillo); las horas de reloj las calcula la app.

**a) Hacia atrás desde la hora de servir.** Coloca el último paso pegado a la hora de comer y retrocede. Si la receta corta algo, el corte es un paso de la MESA en el minuto 0, no una nota previa: sin él, el minuto 0 significa otra cosa en esa receta que en las demás.

**b) Busca el paralelismo.** Identifica el camino crítico (la cadena más larga, casi siempre la salsa o el asado) y mete todo lo demás en sus huecos muertos. El agua no se pone «por si acaso»: se pone en el minuto exacto en que, tardando lo que tarda en hervir, rompe justo cuando la necesitas. No escribas el aviso del paralelismo en ninguna nota: la app lo calcula desde los `off`.

**c) Los tiempos muertos son un recurso.** Cortar, pesar, rallar, poner la mesa y limpiar caben dentro de un chup-chup o de un precalentado de horno. Si un carril espera, algo debería pasar en otro.

**d) Holgura de cada paso** (la app la usa para decir dónde se recupera un retraso):
- `flexible`: aguanta esperar sin estropearse (reducción a fuego bajo, sofrito tapado, reposo).
- `ajustado`: se estira unos minutos con coste de textura.
- `critico`: no espera (pasta escurrida, ajo, emplatado, masas montadas).

**e) Aritmética.** El paso que acaba más tarde termina exactamente en `totalMin` y algún paso arranca en el 0. Si no cuadra, corrígelo y dilo.

**f) Escalar no es multiplicar el tiempo.** Al cambiar raciones, ajusta y explica:

| Qué | Cómo se comporta al doblar |
|---|---|
| Sudar verdura | +30-40 % de tiempo: más masa en la misma superficie |
| Dorar carne | Por tandas, o sube tiempo un 60-70 %; amontonada se cuece en su vapor |
| Reducir salsa | Lo que peor escala: la superficie de evaporación no cambia. Puede ser +40 % o más |
| Hervir agua | Proporcional al volumen; tapada, un 30 % menos |
| Cocer pasta o arroz | Casi igual, si hay agua de sobra |
| Horno | Casi igual con una bandeja; dos bandejas piden rotación y algún minuto más |
| Sal y especias | Sublineales: empieza por 1,5× y corrige al final |

Si el total crece, dilo en la primera línea de la respuesta con la cifra concreta.

**g) Máximo 3 fuegos a la vez**, y avisa de las ventanas en que coinciden, en minutos del plan.
