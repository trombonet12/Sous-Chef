# ADR-0012: Vídeo promocional con Remotion

- Estado: aceptado
- Fecha: 2026-10-20
- Relacionado: `docs/decisiones.md` § 2026-10-20 · Ronda 13, [ADR-0010](0010-distribucion-por-google-play.md), [Google Play](../play.md), skill `video-promo` (`.claude/skills/video-promo/SKILL.md`)

## Contexto
- La ficha de Google Play pide un vídeo promocional en YouTube. Play solo reproduce solos los primeros 30 s, en silencio.
- El montaje anterior era `scripts/video/montar-promo.py`, con ffmpeg y Pillow. Sin dependencias nuevas, pero la
  animación salía tosca.
- El vídeo de la ficha tiene que ser **horizontal**. El vertical sale como Short en YouTube, y el de Play no sirve.
- Joan quiere mejor calidad y un proceso automatizado en el repo.
- Las recetas no cambian; el vídeo muestra la app con los clips reales grabados en el emulador.

## Decisión
- Se crea el proyecto Node `video/` en la raíz, con **Remotion 4.0.533** (versión exacta, publicada hace al menos dos
  semanas), React 19.2.3 y TypeScript.
- Composición `Promo`: 1920×1080, 30 fps, unos 32 s.
  - El móvil se dibuja con los clips reales que graba `scripts/video/grabar-clips.sh` en el emulador.
  - Los rótulos son animados. Trombito se anima a partir de `docs/assets/icono.svg`.
  - Los datos del guion están en `video/src/guion.ts`.
  - La música es CC0 y va versionada en `video/public/musica/`, con `LICENCIA.md`.
- Un solo comando: `node scripts/video/promo.mjs [--grabar] [--control]`. Produce
  `build/promo/sous-chef-promo-1920x1080.mp4`, que no se versiona.
- El render es local. No corre en el CI.
- Se copian a `.claude/skills/` las skills oficiales de Remotion (`best-practices`, `render`, `multimedia`) y se
  crea la skill propia `video-promo`.
- `scripts/video/montar-promo.py` se retira.
- **Licencia:** Remotion es gratuita para individuos y para empresas de hasta 3 empleados
  (<https://github.com/remotion-dev/remotion/blob/main/LICENSE.md>). Este proyecto personal entra en esa
  condición. Remotion no es MIT: es una excepción consciente a la regla de licencias permisivas de la skill
  `dependency-add`, aceptada por Joan.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Mantener ffmpeg + Pillow | Sin dependencias nuevas; ya funciona | Animación tosca; calidad más baja | Joan quiere mejor calidad y animación |
| Motion Canvas | Licencia MIT | Sin skill oficial; menos documentado | Remotion tiene skills oficiales y más documentación |

## Consecuencias
- Positivas: vídeo horizontal con animación de calidad, a partir de clips reales de la app; un solo comando
  reproducible; skills oficiales para el mantenimiento.
- Negativas / costes:
  - `node_modules` ocupa cientos de MB y solo existe en local, dentro de `video/`.
  - La primera ejecución descarga Chrome Headless Shell.
  - Remotion no es MIT. Si cambia su licencia para el uso personal, hay que revisar este ADR.
  - Actualizar Remotion significa subir todos sus paquetes a la vez. No hay skill `remotion-upgrade`: se usa
    `dependency-maintenance`.
- Seguimiento:
  - VERIFICAR que el MP4 renderizado dura lo previsto (~32 s) y que los primeros 30 s muestran la app.
  - VERIFICAR la licencia de Remotion cada vez que se actualice o si cambia el número de empleados.
