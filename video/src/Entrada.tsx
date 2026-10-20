import React from "react";
import { interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { FUENTE, TINTA, TINTA_SUAVE } from "./paleta";
import { Trombito, balanceoEnFrame, parpadeoEnFrame } from "./Trombito";
import { frames, guion } from "./guion";

const TITULO = "Sous Chef";

/** Entrada: Trombito salta, el nombre sale letra a letra y el lema debajo; luego deja sitio al móvil. */
export const Entrada: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const salto = spring({ frame, fps, delay: 3, config: { damping: 8, stiffness: 110, mass: 0.9 } });
  const aparece = interpolate(frame, [3, 9], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const lema = spring({ frame, fps, delay: 34, config: { damping: 18, stiffness: 100 } });

  const inicioSalida = frames(guion.entrada) - 30;
  const salida = interpolate(frame, [inicioSalida, inicioSalida + 24], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: FUENTE,
        opacity: 1 - salida,
        transform: `translateX(${-salida * 260}px)`,
      }}
    >
      <div style={{ display: "flex", flexDirection: "row", alignItems: "center", gap: 48 }}>
        <div style={{ opacity: aparece, transform: `translateY(${(1 - salto) * -380}px)` }}>
          <Trombito
            tamano={290}
            parpadeo={parpadeoEnFrame(frame, fps, 30)}
            balanceoGorro={balanceoEnFrame(frame, fps)}
            rebote={Math.max(0, 1 - salto) * Math.min(1, frame / 12)}
          />
        </div>
        <div style={{ display: "flex", fontSize: 170, fontWeight: 800, color: TINTA, letterSpacing: -3 }}>
          {TITULO.split("").map((letra, i) => {
            const s = spring({ frame, fps, delay: 14 + i * 2.5, config: { damping: 12, stiffness: 160 } });
            return (
              <span
                key={i}
                style={{
                  display: "inline-block",
                  whiteSpace: "pre",
                  opacity: Math.min(1, s * 1.5),
                  transform: `translateY(${(1 - s) * 70}px)`,
                }}
              >
                {letra}
              </span>
            );
          })}
        </div>
      </div>
      <div
        style={{
          marginTop: 28,
          fontSize: 56,
          fontWeight: 400,
          color: TINTA_SUAVE,
          opacity: lema,
          transform: `translateY(${(1 - lema) * 24}px)`,
        }}
      >
        Recetas cronometradas
      </div>
    </div>
  );
};
