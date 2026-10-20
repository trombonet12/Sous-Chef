import React from "react";
import { AbsoluteFill, interpolate, Sequence, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { Video } from "@remotion/media";
import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { slide } from "@remotion/transitions/slide";
import { frames, guion, inicioTramo, inicioTramos } from "./guion";

const PANTALLA_W = 548;
const PANTALLA_H = 974; // 1080x1920 escalado (marco total ~1010 px)
const BORDE = 18;
const RADIO_MARCO = 70;
const CENTRO_X = 1290;
const CENTRO_Y = 540;
const COLOR_MARCO = "#17150F";
const COLOR_PANTALLA = "#F5F1E6";

/** Marco de móvil fijo con los tramos encadenados dentro. */
export const Movil: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const entra = spring({
    frame: frame - (inicioTramos() - 5),
    fps,
    config: { damping: 15, stiffness: 90, mass: 0.9 },
  });
  const opacidad = interpolate(frame - (inicioTramos() - 5), [0, 12], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const flota = Math.sin((2 * Math.PI * frame) / (4 * fps)) * 4;
  const giro = Math.sin((2 * Math.PI * frame) / (6 * fps)) * 0.5;
  const desplazamiento = (1 - entra) * 420;
  // Empuje suave por tramo: 1.00 -> 1.04 -> 1.00 (campana), sin saltos en las transiciones.
  let empuje = 0;
  for (let i = 0; i < guion.tramos.length; i++) {
    const ini = inicioTramo(i);
    const largo = frames(guion.tramos[i].duracion) - (i < guion.tramos.length - 1 ? frames(guion.transicion) : 0);
    if (frame >= ini && frame < ini + largo) {
      const p = (frame - ini) / largo;
      empuje = 0.04 * Math.pow(Math.sin(Math.PI * p), 2);
    }
  }
  const escala = 1 + empuje;

  return (
    <div
      style={{
        position: "absolute",
        left: CENTRO_X - (PANTALLA_W + 2 * BORDE) / 2,
        top: CENTRO_Y - (PANTALLA_H + 2 * BORDE) / 2,
        width: PANTALLA_W + 2 * BORDE,
        height: PANTALLA_H + 2 * BORDE,
        boxSizing: "border-box",
        borderRadius: RADIO_MARCO,
        border: `${BORDE}px solid ${COLOR_MARCO}`,
        backgroundColor: COLOR_PANTALLA,
        boxShadow: "0 40px 80px rgba(23,21,15,0.28), 0 10px 24px rgba(23,21,15,0.18)",
        opacity: opacidad,
        transform: `translate(${desplazamiento}px, ${flota}px) rotate(${giro}deg) scale(${escala})`,
        overflow: "hidden",
      }}
    >
      <AbsoluteFill style={{ backgroundColor: COLOR_PANTALLA }}>
        <Sequence from={inicioTramos()} layout="none">
        <TransitionSeries>
          {guion.tramos.flatMap((t, i) => {
            const elementos: React.ReactElement[] = [];
            if (i > 0) {
              elementos.push(
                <TransitionSeries.Transition
                  key={`t${i}`}
                  presentation={slide({ direction: "from-right" })}
                  timing={linearTiming({ durationInFrames: frames(guion.transicion) })}
                />,
              );
            }
            elementos.push(
              <TransitionSeries.Sequence key={`s${i}`} durationInFrames={frames(t.duracion)}>
                <Video
                  src={staticFile(t.clip)}
                  trimBefore={frames(t.inicio)}
                  muted
                  style={{ width: PANTALLA_W, height: PANTALLA_H, objectFit: "cover" }}
                />
              </TransitionSeries.Sequence>,
            );
            return elementos;
          })}
        </TransitionSeries>
        </Sequence>
      </AbsoluteFill>
      {/* isla / cámara */}
      <div
        style={{
          position: "absolute",
          top: 12,
          left: "50%",
          width: 26,
          height: 26,
          marginLeft: -13,
          borderRadius: 13,
          backgroundColor: COLOR_MARCO,
          boxShadow: "inset 0 0 0 4px #2A2720",
        }}
      />
    </div>
  );
};
