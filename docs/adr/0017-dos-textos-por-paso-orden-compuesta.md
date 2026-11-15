# ADR-0017: Dos textos por paso: orden compuesta y explicación estructurada

- Estado: aceptado (2026-11-15)
- Fecha: 2026-11-15
- Relacionado: `docs/decisiones.md` § Ronda 19 · legibilidad del cronómetro y de los textos, `docs/analisis/legibilidad-crono-y-textos.md` (§3, §4, §6), [esquema de receta](../cocina/esquema-receta.md) (§ «Orden compuesta», § «Prosa estructurada», reglas 30-36), [reglas de cocina](../cocina/reglas.md), [ADR-0006](0006-ficha-nativa-e-impresion-html.md), [ADR-0008](0008-voz-en-el-cronometro.md), [ADR-0014](0014-cocina-configurable-y-potencia-marcada.md)

## Contexto
- La auditoría de legibilidad (`docs/analisis/legibilidad-crono-y-textos.md`) mide los textos de las 33 recetas (407 pasos) y confirma dos problemas opuestos:
  - **En el cronómetro el texto ejecutivo no se lee.** En APILADA, la disposición por defecto, la `nota` no sale con tres carriles ni, por 2 dp, con dos; METO y SACO van cortados a una línea; con cuatro carriles todo pasa a una línea en movimiento. El esquema decía que «`nota` se ve siempre» y no era cierto (hallazgos B1 y B2).
  - **En la ficha la explicación es un bloque.** `p` tiene 407 caracteres de mediana y 1.275 de máximo, sin un solo salto de línea ni `<b>` en los 407 pasos (H3).
- **El mismo dato se escribe varias veces.** La potencia está en `f` y además en `t` o `nota` en 159 de los 213 pasos con fuego; el 36 % de los pasos repite una frase de la `nota` casi literal en `p`; la tapa se pinta en cinco formatos (H4).
- **Los textos superan lo que cada canal admite.** La voz ya corta «por qué» a 300 caracteres e «info» a unos 160 (`RespuestasVoz.kt`), y el 86 % de los porqués se oyen a medias. El validador exige `p` de 120 o más y no pone ningún máximo; la norma «una nota de más de dos frases sobra» no se comprueba (H3).
- Joan cocina con el móvil apoyado en la encimera y la letra a 1,0×: solo el reloj tiene tamaño para leerse a esa distancia (H6).
- El informe compara dos modelos (§6.2): A, un campo nuevo `orden` escrito a mano, y B, componer la orden con los datos que ya existen.

## Decisión
**Modelo B: dos textos por paso, sin campos nuevos ni tipos cambiados.** `Modelo.kt` no se toca.

- **Orden compuesta (lo que se hace).** La tarjeta del cronómetro compone la orden con los datos del paso y enseña la parte que corresponde al momento:
  - antes de empezar: METE (`meto`) · fuego (`f`) · tapa (`tapa`, siempre visible) · duración (`s`);
  - en marcha: reloj, próximo aviso y SEÑAL (`nota`);
  - al vencer: SACA (`saco`).
- **`nota` = señal.** Cómo sabes que está y la única corrección. Sin porqués, sin la potencia de `f`, sin la tapa y sin minutos de reloj. **Puede quedar vacía** si no hay nada que vigilar.
- **`p` = explicación estructurada (el porqué).** El primer párrafo es el porqué en una o dos frases y se sostiene solo: es lo que lee la voz. Mini formato dentro de la cadena, sin cambiar el tipo del campo:
  - línea en blanco: párrafo nuevo;
  - línea que empieza por `- `: elemento de lista;
  - `<b>` sigue valiendo, y nada más (ni Markdown ni HTML).
  Lo leerá un analizador puro en `:receta` y lo pintarán una pieza Compose y otra HTML (`<p>`, `<ul>`). Vale también para `lectura`, `notaEscala`, `porque`, `mep.porq`, `cortes.p` y las técnicas.
- **Topes de partida** (se calibran con el piloto `huevos-rotos`; los caracteres se cuentan como los ve el usuario, con `{N}` = 1 y `<b>` = 0):

  | Campo | Tope |
  |---|---|
  | `t` | 36, sin duración ni tapa; `{N}` solo si el paso consiste en cambiar la potencia |
  | elemento de `meto` y de `saco` | 36, separados por « + » |
  | `nota` | 80 y 2 frases |
  | `hitos[].t` (aviso) | 44 |
  | `p` | 600 en total y 240 por párrafo (mínimo 120, regla 21) |

- **Reglas de validador 30-36** (detalle en el esquema): avisos de legibilidad que solo corren con `sc validar --legibilidad` hasta que pasen las 33 recetas; entonces se funden en `--estricto`. La 35 (pasos del array en orden de `off`) es ERROR.
- **Salida de escape.** Si tras cocinar el piloto más de 1 de cada 10 pasos se lee mal compuesto, se añade un campo `orden` opcional que sustituye la línea METE solo en esos pasos. No se crea antes.
- **Una función pura en `:receta` (`OrdenPaso`)** da las tres salidas de la misma orden: pantalla, una línea (notificación y «Toca empezar») y frase hablada.
- **La app pinta lo que es dato:** potencia (también «4 → 5» si cambia), tapa y duración salen de `f`, `tapa` y `s`, y no se repiten en la `nota`. El porqué de cada paso vive en su paso; `porque` de receta solo recoge decisiones de receta.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| A · campo nuevo `orden` escrito a mano | Lectura natural con el verbo que toque; los casos raros (tandas) se escriben sin problema | La potencia pasa a estar en 4 sitios, la tapa en 3 y las cantidades en 3; hacen falta reglas que crucen la frase con `f`, `tapa` y `meto`; 407 frases nuevas; campo nuevo en `Modelo.kt`, validador, voz y HTML; una frase fija para todos los momentos | Añade una copia en vez de quitarlas. Queda como salida de escape solo para los pasos que B lea mal |
| B · orden compuesta (elegida) | Cada dato en un sitio; la coherencia la da el dato; el chip ya se traduce a la cocina del usuario (ADR-0014); cada momento enseña su parte; una función da pantalla, línea y voz; sin cambios de esquema | Lectura más seca, con etiquetas fijas; obliga a ser breve en tandas y casos raros; hay que reescribir 407 `nota`, 324 `meto` y los avisos largos | — |
| Dejar el modelo actual y solo arreglar la tarjeta | Sin trabajo en las recetas | La `nota` de 110 caracteres de mediana (el 78 % pasa de 80) no cabe en ninguna disposición sin moverse, y el texto sigue duplicado | No resuelve la causa: el texto es más largo que el sitio |

## Consecuencias
- Positivas:
  - Cada momento del paso contesta una pregunta: qué meto, qué miro, qué saco.
  - Se quitan copias (potencia, tapa, cantidades) en vez de añadir un campo, y la coherencia entre fuego, tapa y texto la da el dato.
  - La ficha lee `p` en párrafos y listas; una sola función sirve pantalla, notificación y voz.
  - Los topes hacen que quepa sin marquesina («nada se mueve», decisiones Ronda 19).
- Negativas / costes:
  - Hay que reescribir las 33 recetas (407 `nota`, 324 `meto` y los avisos largos) con guardas mecánicas (cantidades, tiempos, potencias y dependencias idénticos) y revisión de `chef-reviewer`, sin cambiar la cocina. Se hace por lotes, empezando por el piloto.
  - Lectura más seca y con etiquetas fijas.
  - Normas que antes eran solo de estilo (una `nota` de más de dos frases sobra; no repetir en `p` lo que dice la `nota`) pasan a validarse con las reglas 31 y 36.
- Efecto en cada pieza:
  - **Voz** (`ui/crono/voz/`): `info` dice la orden compuesta y deja de cortarse; `porQue` lee el primer párrafo. La búsqueda de ingredientes (`IngredientesVoz`) mira `t`, `meto`, `saco` y `nota`: al acortar hay que conservar el nombre del ingrediente y un test lo fija.
  - **Notificaciones y alarma**: la línea del carril pasa de `t · fuego N` a la orden de una línea; «Toca empezar» añade lo que se mete y «hecho» lo que se saca; los avisos de 44 caben en una línea.
  - **Ficha impresa** ([ADR-0006](0006-ficha-nativa-e-impresion-html.md) sigue valiendo): bloques `<p>` y `<ul>` con `break-inside: avoid`, señal impresa y los ingredientes una sola vez.
  - **Skills**: `receta-cronometrada` (plantilla de paso con los topes, dos ejemplos buenos y dos malos, y «lo que no se escribe porque lo pinta la app»); `receta-revisar` y el agente `chef-reviewer` (añadir `--legibilidad` y comprobar que acortar no cambia la cocina).
  - **Historial**: `pasosReales` guarda `t`, así que los títulos nuevos solo afectan a las cocinadas nuevas.
  - **Esquema y reglas**: `docs/cocina/esquema-receta.md` (§ «`nota` y `p`», «Orden compuesta», «Prosa estructurada» y reglas 30-36) y `docs/cocina/reglas.md`.
- Seguimiento:
  - Calibrar los topes cocinando el piloto `huevos-rotos`: a 18 sp caben unos 36 caracteres por línea (medido en las capturas, `VERIFICAR` en el móvil).
  - Medir la salida de escape: si más de 1 de cada 10 pasos se lee mal compuesto, abrir un ADR para el campo `orden` opcional.
  - Cuando las 33 recetas pasen `--legibilidad`, fundirlo en `--estricto` y actualizar el esquema.
  - El plan que sigue al cocinero (horas de arranque y `junto` como ancla) y el diagrama único no se deciden aquí: tendrán su propio ADR.
