# video/ — Vídeo promocional de Sous Chef

Proyecto [Remotion](https://www.remotion.dev/) (React + TypeScript) que compone el vídeo promocional 1920x1080 a 30 fps.
Los clips de la app (`build/promo/clips/*.mp4`, generados con `scripts/promo`) se copian a `public/clips/` (ignorado por git);
la música va en `public/musica/`.

## Comandos (desde `video/`)

```
npm install                # una vez; usa package-lock.json (versiones exactas)
npm run studio             # editor interactivo
npm run render             # -> ../build/promo/sous-chef-promo-1920x1080.mp4
npm run typecheck          # tsc --noEmit
```

Todos los paquetes `remotion` / `@remotion/*` van a la misma versión exacta (sin `^`). En Windows, `--props` debe ser un archivo.
Remotion descarga Chrome Headless Shell la primera vez.

## Licencia

Remotion es gratuito para particulares; ver https://github.com/remotion-dev/remotion/blob/main/LICENSE.md
