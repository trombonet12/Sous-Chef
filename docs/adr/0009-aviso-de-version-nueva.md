# ADR-0009: Avisar de versión nueva consultando la API pública de releases

- Estado: aceptado
- Fecha: 2026-10-17
- Relacionado: `docs/decisiones.md` § 2026-10-17 · Ronda 8, [ADR-0007](0007-app-solo-local-sin-supabase.md)

## Contexto
- Joan instala las versiones con Obtainium. Quiere que la propia app le avise cuando haya una release nueva.
- ADR-0007 deja la app solo en local: sin backend, sin sync y, hasta ahora, sin permiso de Internet.
- El repo era privado, y la API de releases pedía token. Joan decidió hacerlo público (2026-10-17), después de revisar
  el historial: sin secretos.
- La release sale del CI al cambiar `version.properties`. El tag es `v<versionName>` y lleva `sous-chef-<v>.apk`.

## Decisión
- La app pide `INTERNET` y `ACCESS_NETWORK_STATE`, y hace una sola cosa con la red: `GET
  https://api.github.com/repos/trombonet12/Sous-Chef/releases/latest`, sin token y sin enviar datos. Solo lee
  `tag_name` y `html_url`.
- Cuándo comprueba:
  - al abrir la app, como mucho cada 6 h;
  - una vez al día en segundo plano, con `JobScheduler` de la plataforma (periódico, persistente y solo con red).
    Sin WorkManager ni dependencias nuevas.
- Si el tag es mayor que la `versionName` instalada y esa versión no se avisó ya, publica una notificación en el canal
  «Actualizaciones». Al tocarla abre Obtainium, o la página de la release si Obtainium no está.
- La build debug (`app.souschef.dev`) no comprueba nada.
- El resto sigue igual: los datos del usuario no salen del móvil.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Aviso propio de Obtainium | Sin código ni red en la app | Depende de configurarlo en Obtainium | Joan quiere el aviso en la app |
| Token de solo lectura en Ajustes | Repo privado | Gestionar y rotar un token | Joan prefirió el repo público |
| Fichero público de versión (gist) | Repo privado | Otro artefacto que el CI mantiene | Más piezas que la API pública |
| WorkManager | API cómoda | Dependencia nueva | JobScheduler basta (minSdk 26) |

## Consecuencias
- Positivas: Joan se entera de las versiones nuevas sin abrir Obtainium, y cada versión se avisa una sola vez.
- Negativas / costes:
  - un permiso de red y una petición diaria a GitHub;
  - el límite sin token es de 60 peticiones por hora y por IP, de sobra para este uso;
  - código y recetas, públicos.
- Seguimiento: VERIFICAR en el móvil que el job diario se ejecuta (`adb shell cmd jobscheduler run`) y que la
  notificación abre Obtainium.
