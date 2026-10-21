# ADR-0013: Actualización obligatoria desde Google Play (In-App Updates)

- Estado: aceptado. Complementa a [ADR-0011](0011-sin-aviso-de-version-nueva.md)
- Fecha: 2026-10-20
- Relacionado: `docs/decisiones.md` § 2026-10-20 · Ronda 14, [ADR-0010](0010-distribucion-por-google-play.md), [Google Play](../play.md)

## Contexto
- Empiezan las pruebas con testers en la pista de Play. Joan quiere que todos usen la última versión para que los
  fallos que cuenten sean de la versión actual.
- ADR-0011 dejó la app sin red y sin aviso propio de versión nueva. Play no tiene un ajuste de consola que fuerce la
  actualización: la fuerza la app con la API In-App Updates, y cada versión lleva una prioridad (0-5).

## Decisión
- Dependencia `com.google.android.play:app-update-ktx` **2.1.0**. Habla con la app de Play Store dentro del móvil y no
  añade permisos. Su manifest y los de sus transitivas (`core-common` 2.0.3, `play-services-basement` 18.1.0,
  `play-services-tasks` 18.0.2) se revisaron el 2026-10-20: ninguno pide `INTERNET`.
- **Licencia: Play Core SDK Terms of Service**, gratuita y no libre. Joan acepta la excepción a la regla de licencias
  permisivas de `dependency-add` (2026-10-20), igual que con Remotion.
- `MainActivity`, en `onResume`, lanza el flujo **INMEDIATO** (pantalla de Play bloqueante) si se cumplen todas:
  la app se instaló desde Play (`com.android.vending`), no hay cocinada viva, hay actualización con prioridad ≥ 4 y
  Play permite `IMMEDIATE`. Si una actualización inmediata quedó a medias, se reanuda.
- Si el usuario cancela la pantalla de Play o falla (p. ej. sin red), no se relanza hasta el siguiente arranque en frío:
  la app es solo local y no puede quedar bloqueada sin conexión.
- El CI sube cada versión con `inAppUpdatePriority: 5`. Para publicar una versión no obligatoria se baja a 3.
- Instalaciones desde GitHub u Obtainium: no cambia nada.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Flujo flexible | No bloquea | El tester puede seguir días en la versión vieja | Joan quiere forzarla |
| Aviso propio con red (ADR-0009) | Sin librería de Google | Pide `INTERNET` | ADR-0011 |
| Solo la actualización automática de Play | Sin código | Depende de los ajustes de cada móvil | No fuerza nada |

## Consecuencias
- Positivas: los testers siempre están en la última versión; sin permisos nuevos.
- Negativas / costes: una librería cerrada de Google; la pantalla de Play interrumpe al abrir la app (nunca cocinando).
- Seguimiento: VERIFICAR en el manifest fusionado del release que no aparece `INTERNET`. VERIFICAR con un tester de
  la pista interna que la pantalla sale al publicar una versión nueva.
