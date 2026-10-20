import React from "react";
import { interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { FUENTE, TINTA, TINTA_SUAVE } from "./paleta";
import { Trombito, balanceoEnFrame, parpadeoEnFrame } from "./Trombito";

/** Cierre: Trombito centrado guiñando, nombre y tres rasgos. Frame local. */
export const Cierre: React.FC<{ duracion: number }> = ({ duracion }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const entra = spring({ frame, fps, delay: 8, config: { damping: 11, stiffness: 120, mass: 0.9 } });
  const nombre = spring({ frame, fps, delay: 18, config: { damping: 16, stiffness: 110 } });
  const rasgos = spring({ frame, fps, delay: 30, config: { damping: 18, stiffness: 100 } });
  const aparece = interpolate(frame, [0, 14], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  // Guiño hacia la mitad del cierre (~0,4 s).
  const inicioGuino = Math.round(fps * 1.2);
  const guino = interpolate(
    frame,
    [inicioGuino, inicioGuino + 4, inicioGuino + 12, inicioGuino + 17],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );
  const salidaFinal = interpolate(frame, [duracion - 8, duracion], [1, 0.0], {
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
        opacity: aparece * salidaFinal,
      }}
    >
      <div style={{ transform: `translateY(${(1 - entra) * -120}px)` }}>
        <Trombito
          tamano={300}
          parpadeo={frame > inicioGuino + 20 ? parpadeoEnFrame(frame, fps, 10) : 0}
          guino={guino}
          balanceoGorro={balanceoEnFrame(frame, fps)}
          rebote={Math.max(0, 1 - entra)}
        />
      </div>
      <div
        style={{
          marginTop: 24,
          fontSize: 150,
          fontWeight: 800,
          color: TINTA,
          letterSpacing: -3,
          opacity: nombre,
          transform: `translateY(${(1 - nombre) * 40}px)`,
        }}
      >
        Sous Chef
      </div>
      <div
        style={{
          marginTop: 20,
          fontSize: 48,
          color: TINTA_SUAVE,
          opacity: rasgos,
          transform: `translateY(${(1 - rasgos) * 24}px)`,
        }}
      >
        Sin cuentas · Sin anuncios · Sin conexión
      </div>
    </div>
  );
};
