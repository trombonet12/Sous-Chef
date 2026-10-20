import React from "react";
import { interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { PALETA, FUENTE, TINTA, TINTA_SUAVE } from "./paleta";
import type { Tramo } from "./guion";

type Props = {
  tramo: Tramo;
  /** Frames que dura el rótulo (para el fundido de salida). */
  ranura: number;
};

/** Texto del tramo, a la izquierda. Se monta dentro de una Sequence: el frame es local. */
export const Rotulo: React.FC<Props> = ({ tramo, ranura }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const cfg = { damping: 16, stiffness: 110, mass: 0.8 };
  const barra = spring({ frame, fps, delay: 3, config: cfg });
  const sTitulo = spring({ frame, fps, delay: 7, config: cfg });
  const sSub = spring({ frame, fps, delay: 14, config: cfg });
  const salida = interpolate(frame, [ranura - 14, ranura - 1], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const acento = PALETA[tramo.tono].acento;

  return (
    <div
      style={{
        position: "absolute",
        left: 150,
        top: 0,
        width: 780,
        height: 1080,
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        fontFamily: FUENTE,
        opacity: salida,
        transform: `translateY(${(1 - salida) * -14}px)`,
      }}
    >
      <div
        style={{
          width: 110 * barra,
          height: 10,
          borderRadius: 5,
          backgroundColor: acento,
          marginBottom: 40,
        }}
      />
      <div
        style={{
          fontSize: 104,
          lineHeight: 1.04,
          fontWeight: 800,
          color: TINTA,
          letterSpacing: -2.5,
          textWrap: "balance",
          opacity: sTitulo,
          transform: `translateY(${(1 - sTitulo) * 60}px)`,
        }}
      >
        {tramo.titulo}
      </div>
      <div
        style={{
          marginTop: 32,
          fontSize: 44,
          lineHeight: 1.25,
          fontWeight: 400,
          color: TINTA_SUAVE,
          opacity: sSub,
          transform: `translateY(${(1 - sSub) * 40}px)`,
        }}
      >
        {tramo.subtitulo}
      </div>
    </div>
  );
};
