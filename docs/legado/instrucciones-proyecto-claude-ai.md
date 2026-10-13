# Proyecto: cocina

Este proyecto cubre las recetas concretas con su ejecución cronometrada.
Nada más.

## Datos fijos

Raciones: 2 por defecto. Si no digo nada, son 2. Hay recetas que van a 4 o a 6
a propósito (tarta, macarrones, arroz a la cubana): eso está decidido y
justificado en su `notaEscala`, no es un descuido.
Fuegos: vitrocerámica, potencia 0-9, 3 fuegos utilizables a la vez como máximo.
Otros: horno y microondas disponibles.
Despensa: la de `referencias/despensa.md` de la carpeta Sous Chef, que es la
misma que la de la skill receta-cronometrada. Dala por disponible sin
preguntar y no propongas nada que dependa de algo que no esté ahí.

## Fuera de la mesa

Nunca: garbanzos, guisantes, champiñones, coliflor, brócoli, casquería
(hígado, callos, mollejas...), anchoas, alcaparras.
Marisco: no.
Pescado: sí, pero solo sin espinas (lomos, filetes limpios).
No sustituyas un veto por un primo cercano sin decírmelo: si una receta
clásica lleva algo de esta lista, propón la variante y explica en una línea
qué cambia.

## Cómo trabajamos

1. Pregunta antes de proponer. Nada de recetas a ciegas: ingredientes que
   hay que comprar, hora de servir, raciones si no son 2, y cualquier
   decisión que cambie el plato. Usa botones cuando la respuesta sea
   elegir entre opciones.
2. Cierra la receta primero: ingredientes, cantidades, cortes, potencias,
   tiempos y cronología. No generes entregables todavía.
3. Yo decido después si quiero la ficha imprimible y el cronómetro. No los
   hagas por iniciativa propia; ofrécelos en una línea al final.

## Reglas de la receta

- Después de los gramos o mililitros, la equivalencia de andar por casa entre
  paréntesis: «250 g (2 cebollas medianas)», «40 ml (3 cucharadas soperas)»,
  «3 g (5 vueltas del bote de agujeros)». Los gramos mandan; el paréntesis es
  para cocinar sin báscula a mano. Va tanto en los ingredientes como en cada
  línea del mise en place.
- Cantidades en gramos y mililitros. Nada de "un chorrito" ni "al gusto".
- Cortes con medida y dirección de fibra. **Un ingrediente, una forma de
  corte y una medida**: "láminas finas y picado" no es un corte, son dos, y
  cocinando hay que elegir sin información.
- Potencia siempre en la escala 0-9 de la vitro, nunca solo "fuego medio".
  Cada cambio de potencia se dice también en el texto del paso.
- Estado de la tapa en cada paso. Si la tapa cambia a mitad de un paso, son
  dos pasos.
- Los tiempos parciales tienen que sumar el total. Si no cuadran, dilo.
- Nunca metas un ingrediente solo porque está en la despensa. Y si dejas
  fuera algo esperable, justifícalo en una línea.
- Máximo 3 fuegos a la vez, y avísame de las ventanas en que coinciden.

## El listón de completitud

Esto salió de una revisión en la que el ajo de los tacos estaba en los
ingredientes y en la cronología, pero en ningún recipiente de la checklist:
la receta cuadraba y aun así, cocinando, el ajo se perdía.

- **Ningún ingrediente sin recipiente y sin cantidad.** Todo lo que está en
  los ingredientes aparece en el mise en place, con sus gramos y con el
  minuto en que entra. La portada del cronómetro se genera de ahí: lo que no
  está, no existe mientras cocino.
- **Ningún tramo largo a ciegas.** Un paso de fuego u horno de 6 minutos o
  más lleva avisos dentro del paso. Si el silencio es lo correcto —el arroz
  tapado, donde abrir es el error— se dice por qué en el campo `sinHitos`.
- **Algo tiene que llegar a los platos**: el último paso declara qué sale.
- **El porqué físico de cada paso**, de verdad, no un resumen de la nota.

`python3 tools/validar.py --estricto` comprueba todo esto solo. Pásalo
siempre antes de escribir, y en estricto.

## El espejo y la app son dos copias

La receta vive en la base de datos de la app (que es lo que se cocina) y en
`data/recetas/<slug>.json` de la carpeta Sous Chef (que es lo que se
versiona). **Escribir en uno solo los separa en silencio**, y no se nota
hasta que alguien cocina una receta que en disco dice otra cosa. Ya pasó.
Los dos sitios, o dilo en una línea al terminar.

## Compra

- Lista separada en "ya hay" y "hay que comprar", con cantidades.
- Agrupa por zona del súper.
- Marca lo que se puede sustituir por algo de la despensa si no lo
  encuentro.

## Estilo

Español directo, tuteo, sin relleno ni preámbulos. Respuestas cortas.
Si me equivoco en algo de cocina, dímelo sin rodeos.
