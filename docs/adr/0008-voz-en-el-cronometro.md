# ADR-0008: Controlar el cronómetro por voz, solo en primer plano y con las APIs de plataforma

- Estado: aceptado
- Fecha: 2026-10-17
- Relacionado: `docs/decisiones.md` § 2026-10-17 · Ronda 6, [ADR-0005](0005-cronometro-alarmas-sin-fgs.md), [ADR-0007](0007-app-solo-local-sin-supabase.md)

## Contexto
- Joan cocina con las manos ocupadas o sucias. Quiere hablar con la app («¿cuánto queda?», «hecho», «pausa») y que le
  conteste, sin tocar la pantalla.
- El móvil (LineageOS 23.2, Android 16) tiene Speech Services by Google como `RecognitionService` por defecto y como
  motor TTS (comprobado por adb el 2026-10-17).
- ADR-0005 descarta el foreground service. Un micrófono en segundo plano exigiría un FGS de tipo `microphone`, con
  notificación permanente y más permisos.
- Con el cronómetro en pantalla, la app ya está en primer plano y con `FLAG_KEEP_SCREEN_ON`.

## Decisión
- **Escucha solo con `CronoScreen` en RESUMED.** Se para en ON_STOP, así que con la pantalla apagada o fuera de la app no
  escucha nadie. No hay FGS.
- `android.speech.SpeechRecognizer`:
  - en el dispositivo (`createOnDeviceSpeechRecognizer`) si `isOnDeviceRecognitionAvailable` (API 31+);
  - si no, el reconocedor por defecto con `EXTRA_PREFER_OFFLINE`.
  - Es un bucle de escucha que se reinicia al terminar cada resultado o error, con backoff ante errores seguidos.
- Palabra clave **«Chef»** dentro de la transcripción, sin hotword de sistema. El intérprete (`InterpreteVoz`, puro, en
  `:receta`, TDD) convierte el texto en una `OrdenVoz` cerrada:
  - hecho/siguiente [carril];
  - pausa / sigue;
  - +30 s;
  - ¿cuánto queda [carril]?;
  - ¿qué toca?;
  - ¿qué viene?;
  - para (silencia la alarma);
  - repite.
  Fuera de esa gramática no se hace nada.
- Respuestas con `android.speech.tts.TextToSpeech` (es-ES). Mientras habla, la escucha se pausa para no oírse a sí misma.
  Con la cocina en pantalla, los avisos (fin, hito, toca empezar) se anuncian en voz además del pitido.
- Permiso `RECORD_AUDIO` en tiempo de ejecución, pedido al activar la voz. Sin él, la cocina funciona igual sin voz.
  Ajuste `voz.activa` (por defecto sí) y botón de silenciar en la cabecera.
- Privacidad: no se guarda ni se registra audio ni transcripción. Logcat solo lleva el tipo de orden reconocida.
- **Sin dependencias nuevas.**

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| FGS `microphone`, escucha siempre | Funciona con la pantalla apagada | Notificación permanente, batería, contradice ADR-0005 | Con la cocina en pantalla basta |
| Vosk / modelo offline embebido | Sin depender de Google | Dependencia y ~40-50 MB de modelo | El reconocedor del sistema ya está y es local |
| Pulsar para hablar | Más fiable y menos batería | Hay que tocar la pantalla | Joan elige la palabra clave |
| Solo TTS, sin micrófono | Simple | No responde a preguntas | Joan quiere conversación |
| `SpeechRecognizer` + TTS de plataforma, solo en primer plano | Sin dependencias ni FGS | El bucle de reinicio puede pitar según el motor; depende del reconocedor instalado | Elegido |

## Consecuencias
- Positivas: manos libres durante la cocina, sin servicio permanente ni dependencias, y la lógica (intérprete y
  respuestas) se testea en JVM.
- Negativas / costes: depende de que el móvil tenga reconocedor y TTS en español. Hay batería extra mientras el
  cronómetro está en pantalla. En cocinas ruidosas puede haber falsos negativos.
- Seguimiento: **spike en el móvil antes de cerrar la ronda**. `VERIFICAR`:
  - reconocimiento en español sin red;
  - latencia;
  - si el reinicio del bucle pita (si pita, silenciar el stream de notificación durante la escucha o reiniciar sin
    `startListening` redundante);
  - convivencia con el TTS.
- ADR-0005: el cronómetro sigue sin FGS; el micrófono solo en primer plano no lo cambia.
