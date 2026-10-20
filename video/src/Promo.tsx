import React from "react";
import { AbsoluteFill, interpolate, Sequence, staticFile, useCurrentFrame } from "remotion";
import { Audio } from "@remotion/media";
import { Fondo } from "./Fondo";
import { Entrada } from "./Entrada";
import { Movil } from "./Movil";
import { Rotulo } from "./Rotulo";
import { Cierre } from "./Cierre";
import { duracionTotal, finTramos, frames, guion, inicioTramo } from "./guion";

export const Promo: React.FC = () => {
  const frame = useCurrentFrame();
  const total = duracionTotal();
  const fin = finTramos();
  const fps = guion.fps;

  // Móvil y rótulos se funden a papel antes del cierre.
  const escena = interpolate(frame, [fin - 14, fin + 6], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const musica = guion.musica;

  return (
    <AbsoluteFill>
      <Fondo />
      <Sequence durationInFrames={frames(guion.entrada)} name="Entrada">
        <Entrada />
      </Sequence>
      <AbsoluteFill style={{ opacity: escena }}>
        <Movil />
        {guion.tramos.map((t, i) => {
          const ultimo = i === guion.tramos.length - 1;
          const ranura = frames(t.duracion) - (ultimo ? 0 : frames(guion.transicion));
          return (
            <Sequence key={i} from={inicioTramo(i)} durationInFrames={ranura} name={`Rótulo ${i + 1}`}>
              <Rotulo tramo={t} ranura={ranura} />
            </Sequence>
          );
        })}
      </AbsoluteFill>
      <Sequence from={fin} durationInFrames={frames(guion.cierre)} name="Cierre">
        <Cierre duracion={frames(guion.cierre)} />
      </Sequence>
      {musica ? (
        <Audio
          src={staticFile(musica.archivo)}
          volume={(f) =>
            musica.volumen *
            interpolate(f, [0, fps, total - Math.round(1.5 * fps), total], [0, 1, 1, 0], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            })
          }
        />
      ) : null}
    </AbsoluteFill>
  );
};
