# Mise en place

Una cocina real tiene **una tabla de cortar, un vaso medidor y pocos boles**. Da eso por supuesto y planifica en consecuencia, sin preguntar.

**Recipientes: el mínimo, y razonado.** Un recipiente solo se justifica cuando lo que guarda entra en la olla en un momento distinto al del resto:
- Lo que entra a la vez comparte recipiente. Un bol por ingrediente es un error de planificación.
- Lo que entra desde el principio no necesita ninguno: la olla y la sartén en frío son los mejores recipientes de preparación.
- Lo que ya viene en un recipiente se queda en él (la carne en su bandeja, la pasta pesada en su paquete).

Cuenta los recipientes en voz alta y di cuál se reutiliza.

**Ningún ingrediente sin recipiente y sin cantidad.** Todo lo que está en `grupos` aparece en alguna línea de `mep`, con sus gramos o mililitros y el minuto en que entra. La portada del cronómetro se genera desde `mep`: lo que no está ahí no existe mientras cocinas. Es el error que más veces ha pasado desapercibido.

**Un solo vaso medidor: el orden lo decide el residuo.** Primero lo que no se puede rectificar, luego lo que comparte destino, y la grasa siempre al final, porque es la única que deja un film que obliga a lavar.

**Una sola tabla: manda la humedad, no el olor.** Lo duro y lo seco primero (una tabla mojada convierte el corte fino en un problema de seguridad), luego lo que suelta agua, y el ajo al final. Si hay carne cruda, va primero y la tabla se lava una vez.

**Sentido común:** no salpimentes la carne en la mise en place. La sal saca agua y luego hay que evaporarla otra vez.

Formato en `mep`: `orden:true` cuando la secuencia es obligatoria (medidor, tabla), `orden:false` cuando es solo una lista (recipientes). Solo `<b>` en las líneas. Equivalencia casera tras cada medida en gramos o mililitros. Esquema: `docs/cocina/esquema-receta.md`.
