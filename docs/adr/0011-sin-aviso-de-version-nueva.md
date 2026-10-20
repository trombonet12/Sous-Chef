# ADR-0011: Quitar el aviso de versión nueva y que la app no use la red

- Estado: aceptado. Sustituye a [ADR-0009](0009-aviso-de-version-nueva.md)
- Fecha: 2026-10-20
- Relacionado: `docs/decisiones.md` § 2026-10-20 · Ronda 12, [ADR-0007](0007-app-solo-local-sin-supabase.md), [ADR-0009](0009-aviso-de-version-nueva.md), [ADR-0010](0010-distribucion-por-google-play.md), [Google Play](../play.md)

## Contexto
- ADR-0009 añadió a la app una consulta a la API pública de releases de GitHub para avisar de versión nueva. Pedía
  `INTERNET` y `ACCESS_NETWORK_STATE`, y un trabajo diario con `JobScheduler`.
- Joan instala las versiones con Obtainium, que ya avisa de las releases nuevas. Con Play, la tienda actualiza sola.
  El aviso dentro de la app ya no hace falta.
- Joan no quiere que la app revise las actualizaciones (ronda 12, 2026-10-20).
- Sin esa consulta, la app no necesita ningún permiso de red. Eso simplifica la ficha de seguridad de datos de Play
  ([ADR-0010](0010-distribucion-por-google-play.md)).

## Decisión
- Se borran `ComprobadorActualizaciones`, `TrabajoActualizaciones` (el `JobScheduler` diario), `VersionNueva` y la
  notificación y el canal «Actualizaciones».
- El manifest pierde `INTERNET`, `ACCESS_NETWORK_STATE` y los `<queries>` de Obtainium.
- La app queda **sin ningún acceso a red**. Sus datos siguen en el móvil, como dice [ADR-0007](0007-app-solo-local-sin-supabase.md).
- Las versiones nuevas se ven con el aviso propio de Obtainium, que sigue las Releases de GitHub, o con Play.
  La release de GitHub no cambia.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Mantener el aviso de ADR-0009 | Joan se entera dentro de la app | Pide red y permisos; una petición diaria a GitHub; más código; ficha de Play con red | Joan no lo quiere; menos permisos y ninguna red es mejor para la ficha de seguridad de datos de Play |

## Consecuencias
- Positivas: menos permisos, ninguna conexión de red y menos código (trabajo programado, notificación y canal).
  La ficha de seguridad de datos de Play queda más simple.
- Negativas / costes: Joan ya no ve el aviso dentro de la app. Tiene que mirar Obtainium o Play para saber si hay
  versión nueva.
- Seguimiento: VERIFICAR en el APK final, con el manifest fusionado, que no aparecen `INTERNET` ni
  `ACCESS_NETWORK_STATE`, porque una dependencia podría volver a añadirlos. VERIFICAR también que Obtainium avisa de
  la release nueva.
