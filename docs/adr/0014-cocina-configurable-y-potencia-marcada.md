# ADR-0014: Cocina configurable y potencia marcada en el texto de las recetas

- Estado: aceptado
- Fecha: 2026-10-20
- Relacionado: `docs/decisiones.md` § 2026-10-20 · Ronda 14, [ADR-0003](0003-el-repo-manda-en-las-recetas.md), `docs/cocina/esquema-receta.md`

## Contexto
- Las recetas y la app daban por hecha la cocina de Joan: vitrocerámica digital 0-9. Con testers, hay gas, inducción
  y mandos con otras escalas o sin números.
- La potencia está en `Paso.f` y, además, unas 340 veces dentro del texto libre («baja al 3», «del 4 al 5»).
- Hay que traducir las dos cosas sin reescribir las recetas para cada cocina, y sin confundir una potencia con un
  minuto («del 30 al 39») o una cantidad.

## Decisión
- **Escala canónica:** las recetas se siguen escribiendo en vitro 0-9. La app traduce al mostrar.
- **Cocina del usuario** (`receta/Potencia.kt`): tipo (vitro, inducción, gas), mando (digital 9-20; analógico 4-12
  o sin números) y potencia máxima. Digital: número entero proporcional. Analógico: proporcional a medios («3,5»),
  para que dos niveles no colapsen. Sin números: palabras (mínimo · bajo · medio-bajo · medio · medio-alto · alto ·
  máximo) con su gramática («a fuego medio-alto»).
- **Marcado:** en el texto libre la potencia va como `{N}` («baja al {3}»). Lo marcó una vez `sc potencias --marcar`
  (`receta/MarcaPotencia.kt`), revisado a mano. El validador exige el marcado (R27) y lee los tokens en R20 y R25.
- **Identidad canónica:** el motor del cronómetro, los ids de las casillas y lo guardado usan la receta canónica.
  Solo la UI recibe la receta traducida (`Receta.conCocina`). Cambiar de cocina no borra nada.
- En vitro digital 0-9 el texto mostrado es idéntico al de antes.
- **Raciones:** campo `escala {min, max, paso}` por receta, fijado tras una revisión culinaria (R26). La ficha no
  sale de ese rango.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Reescribir el texto con regex al mostrar, sin marcar | Sin tocar datos | Falsos positivos (minutos, tandas) | Un minuto traducido como potencia engaña cocinando |
| Una versión de cada receta por cocina | Texto perfecto | N copias que mantener | Contra ADR-0003 (una sola copia) |
| Solo traducir `f`, no el texto | Simple | El texto contradice al chip | Incoherente |

## Consecuencias
- Positivas: una receta sirve para cualquier cocina; el validador impide potencias sin marcar.
- Negativas / costes: el JSON lleva llaves en el texto. Las explicaciones de inercia de la vitro no se traducen: en
  gas o inducción la app enseña una nota general.
- Seguimiento: la skill `receta-cronometrada` y `docs/cocina/*` explican el marcado.
