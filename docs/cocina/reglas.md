# Reglas de cocina

> Propósito: reglas fijas de cocina del proyecto (vetos, raciones, potencias, tapa, completitud, compra, estilo) · Audiencia: Claude Code y Joan · Actualizado: 2026-10-13

## Resumen
Este proyecto cubre recetas concretas con su ejecución cronometrada. Nada más. El repo (`data/recetas/`) manda; las recetas viajan en el APK (publicar = `sc validar --estricto` + release del APK) y la app nunca edita recetas. Esquema y comprobaciones: `esquema-receta.md`. Quién es Joan y cómo trabaja: `perfil.md`.

## Datos fijos
- **Raciones: 2 por defecto.** Si no se dice nada, son 2. Hay recetas que van a 4 o a 6 a propósito (tarta, macarrones, arroz a la cubana): está decidido y justificado en su `notaEscala`, no es un descuido.
- **Fuegos: vitrocerámica**, potencia 0-9, **3 fuegos utilizables a la vez como máximo**. Horno y microondas disponibles.
- **Hora de servir por defecto: 15:30.** La receta habla en minutos relativos (minuto 0 = coges el cuchillo); la app calcula el reloj desde la hora que se ponga al abrir el cronómetro.
- **Despensa:** la semilla `data/despensa-semilla.md` (interna: la app no la muestra desde la v1.0), más lo que Joan diga que tiene. Se da por disponible sin preguntar; no se propone nada que dependa de algo que no esté ahí.

## Fuera de la mesa
Nunca: garbanzos, guisantes, champiñones, coliflor, brócoli, casquería (hígado, callos, mollejas...), anchoas, alcaparras.
Marisco: no. Pescado: sí, pero solo sin espinas (lomos, filetes limpios).

No sustituyas un veto por un primo cercano sin decirlo: si una receta clásica lleva algo de esta lista, propón la variante y explica en una línea qué cambia.

## Cómo trabajamos
1. **Pregunta antes de proponer.** Nada de recetas a ciegas: ingredientes que hay que comprar, hora de servir (solo si cambia el plan), raciones si no son 2 y cualquier decisión que cambie el plato. Con opciones cuando la respuesta sea elegir.
2. **Cierra la receta primero:** ingredientes, cantidades, cortes, potencias, tiempos y cronología.
3. La ficha y el cronómetro son pantallas de la app; no hay entregable que generar. Ver `entregables.md`.
4. Se enseña la receta completa a Joan y solo con su OK se publica.

## Reglas de la receta
- Tras los gramos o mililitros, la **equivalencia de andar por casa** entre paréntesis: «250 g (2 cebollas medianas)», «40 ml (3 cucharadas soperas)», «3 g (5 vueltas del bote de agujeros)». Los gramos mandan; el paréntesis es para cocinar sin báscula. Va en los ingredientes y en cada línea del mise en place.
- Cantidades en gramos y mililitros. Nada de «un chorrito» ni «al gusto».
- **Cortes con medida y dirección de fibra.** Un ingrediente, una forma de corte y una medida: «láminas finas y picado» son dos cortes y cocinando hay que elegir sin información.
- **Potencia siempre en la escala 0-9** de la vitro, nunca solo «fuego medio». Cada cambio de potencia se dice también en el texto del paso.
- **Estado de la tapa en cada paso.** Si la tapa cambia a mitad de un paso, son dos pasos.
- Los tiempos parciales suman el total. Si no cuadran, se dice.
- Nunca metas un ingrediente solo porque está en la despensa. Si dejas fuera algo esperable, justifícalo en una línea (va en `porque`).
- Máximo 3 fuegos a la vez, y se avisa de las ventanas en que coinciden.
- **Aceite, nunca a secas:** oliva o girasol, no son intercambiables. **Pimentón: solo picante, en polvo**; sin ahumado.

## El listón de completitud
Salió de una revisión en la que el ajo de los tacos estaba en los ingredientes y en la cronología pero en ningún recipiente de la checklist: la receta cuadraba y, cocinando, el ajo se perdía.

- **Ningún ingrediente sin recipiente y sin cantidad.** Todo lo de los ingredientes aparece en el mise en place, con sus gramos y el minuto en que entra. La portada del cronómetro se genera de ahí: lo que no está, no existe mientras cocino.
- **Ningún tramo largo a ciegas.** Un paso de fuego u horno de 6 minutos o más lleva avisos (hitos) dentro. Si el silencio es lo correcto (el arroz tapado, donde abrir es el error) se dice por qué en `sinHitos`.
- **Algo tiene que llegar a los platos:** el último paso declara qué sale (`saco`).
- **El porqué físico de cada paso**, de verdad, no un resumen de la nota.

`scripts/sc validar --estricto` lo comprueba solo; se pasa siempre antes de publicar.

## Dónde vive la receta
Una sola copia: `data/recetas/<slug>.json` en el repo; se empaqueta en el APK en el siguiente release. Ya no hay «espejo» app/carpeta ni escritura doble.

## Compra
- Lista separada en «ya hay» y «hay que comprar», con cantidades.
- Agrupa por zona del súper.
- Marca lo que se puede sustituir por algo de la despensa si no lo encuentro.
- Las recetas no tienen campo `compra`: todo ingrediente de `grupos` debe estar en `data/despensa-semilla.md` (agua implícita). Si una receta necesita algo nuevo, se añade antes a la despensa. Sugerencias fuera de receta: `sugerencias-compra.md`.

## Estilo
Español directo, tuteo, sin relleno ni preámbulos. Respuestas cortas. Si Joan se equivoca en algo de cocina, se le dice sin rodeos.

## Decisiones y fuentes
- Origen: instrucciones del proyecto de claude.ai (`docs/legado/instrucciones-proyecto-claude-ai.md`). Resueltas: vitrocerámica (no inducción), servir a las 15:30.
