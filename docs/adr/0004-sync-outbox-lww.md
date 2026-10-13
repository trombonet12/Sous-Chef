# ADR-0004: Sincronizar con Room + outbox y last-write-wins

- Estado: aceptado
- Fecha: 2026-10-13
- Relacionado: `docs/decisiones.md` § Sync, [ADR-0002](0002-supabase-compartido-esquema-sous-chef.md), [ADR-0005](0005-cronometro-alarmas-sin-fgs.md), [arquitectura](../arquitectura.md)

## Contexto
- Datos editables por el usuario: despensa, lista de la compra, historial y preferencias. Recetas y técnicas son de solo lectura (ADR-0003).
- Un único usuario y normalmente un único móvil; la app debe funcionar sin red (cocina) y subir los cambios al volver.
- Trombito usa una sync más compleja (cursor `xid8`) pensada para varios dispositivos y notas con conflictos reales; aquí sería sobreingeniería.

## Decisión
- Room es la fuente de la UI. Cada escritura del usuario se guarda junto con su entrada en el outbox **en la misma transacción**.
- `SyncWorker` (WorkManager, con red) primero empuja el outbox con upserts idempotentes y luego baja cada tabla con `updated_at > cursor - 5 min`, deduplicando por id.
- `updated_at` lo pone un trigger con `clock_timestamp()`. El solape de 5 min absorbe transacciones que confirman tarde.
- Un pull **no pisa** filas con una entrada pendiente en el outbox.
- Conflictos: last-write-wins por `editado_en` (reloj del cliente) más un trigger que rechaza un UPDATE con `editado_en` más viejo que el guardado.
- Borrado lógico con `deleted_at`.
- `recetas` y `tecnicas` solo se bajan. El estado del cronómetro es **solo local** (Room) y no se sube.
- Se sincroniza al arrancar, al volver a la app, al tirar para refrescar y de forma periódica. Una banda muestra "Sin conexión" o "N cambios pendientes".

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Sync `xid8` de Trombito | Reutiliza diseño y código | Pensada para multi-dispositivo y conflictos de notas; mucha complejidad | Desproporcionada para pocas tablas pequeñas |
| Online-only sin Room | Muy simple | No funciona sin red en la cocina | Rompe el requisito offline |
| CRDT | Fusión correcta sin depender del reloj | Complejidad y dependencias | Un usuario, un móvil |
| Subir también el estado del crono | Reanudar en otro móvil | Más tráfico y conflictos; sin necesidad real | Un solo móvil; se mantiene local |
| Outbox + LWW | Simple, idempotente, robusta ante cortes | Pierde ediciones concurrentes con relojes desfasados | Elegido |

## Consecuencias
- Positivas: sin pérdida por cortes de red; lógica de sync pequeña y testeable (TDD).
- Negativas / costes: LWW descarta la edición más antigua en un conflicto; depende de que el reloj del móvil sea razonable.
- Seguimiento: si se añade un segundo dispositivo, revisar este ADR; escenario de QA en modo avión (marcar "falta" y comprobar que sube al reconectar).
