# Potencias (vitrocerámica 0-9)

Escala 0-9 de vitrocerámica. Referencia:
- **1-2** mantener caliente, fundir, reposos
- **3-4** chup-chup, reducciones largas, guisos
- **5-6** sofritos, sudar verdura, arrancar
- **7-8** dorar, sellar, desglasar, hervir mantenido
- **9** llevar agua a ebullición, golpe de calor

La vitro guarda calor: al bajar de potencia, la inercia dura un par de minutos. Cuando importe, dilo en el paso.

Cada cambio de potencia se dice también en el texto del paso, en esta escala. Si un paso hereda la potencia del anterior, no hace falta repetirla (pero el validador estricto avisa si cambia y el texto no la nombra).

Fuegos de Joan: grande, mediano y pequeño. La plancha y la sartén más grande solo caben en el grande, así que no trabajan a la vez (ver `docs/cocina/perfil.md`).

## Hitos frente a partir el paso
Un paso de 11 minutos en el que hay que remover cada 3 no es mudo: son **hitos**, avisos en segundos desde que el paso arranca.

```jsonc
"hitos": [ { "en": 180, "t": "Destapa, remueve y vuelve a tapar" },
           { "en": 360, "t": "Destapa, remueve y vuelve a tapar" } ]
```
- Cambia el montaje (potencia, tapa, contenido) → **dos pasos**.
- No cambia nada, solo hay que hacer algo (remover, voltear, rascar el fondo) → **un hito**.
- `en` en orden y siempre menor que la duración. Texto en imperativo corto. No los repitas en `nota`.
- Paso de fuego u horno de 6 min o más sin hitos → `sinHitos` con la frase que explica por qué el silencio es lo correcto (arroz tapado: abrir es el error).
