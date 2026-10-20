import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { PALETA, PAPEL } from "./paleta";

type Forma = {
  color: string;
  x: number;
  y: number;
  w: number;
  h: number;
  radio: string;
  fase: number;
  vel: number;
  amp: number;
};

const FORMAS: Forma[] = [
  { color: PALETA.AMBAR.acento, x: -150, y: -200, w: 760, h: 620, radio: "46%", fase: 0, vel: 0.011, amp: 70 },
  { color: PALETA.OLIVA.acento, x: 1320, y: -260, w: 700, h: 640, radio: "52%", fase: 1.3, vel: 0.009, amp: 60 },
  { color: PALETA.TEJA.acento, x: 80, y: 760, w: 720, h: 560, radio: "40%", fase: 2.6, vel: 0.012, amp: 80 },
  { color: PALETA.AZUL.acento, x: 1000, y: 700, w: 760, h: 640, radio: "50%", fase: 3.9, vel: 0.008, amp: 70 },
  { color: PALETA.LILA.acento, x: 560, y: -300, w: 620, h: 520, radio: "55%", fase: 5.1, vel: 0.01, amp: 60 },
  { color: PALETA.ROSA.acento, x: 1560, y: 330, w: 520, h: 520, radio: "48%", fase: 0.7, vel: 0.013, amp: 50 },
];

export const Fondo: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ backgroundColor: PAPEL, overflow: "hidden" }}>
      {FORMAS.map((f, i) => {
        const dx = Math.sin(frame * f.vel + f.fase) * f.amp;
        const dy = Math.cos(frame * f.vel * 0.8 + f.fase) * f.amp;
        const giro = Math.sin(frame * f.vel * 0.5 + f.fase) * 12;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: f.x + dx,
              top: f.y + dy,
              width: f.w,
              height: f.h,
              borderRadius: f.radio,
              backgroundColor: f.color,
              opacity: 0.2,
              filter: "blur(70px)",
              transform: `rotate(${giro}deg)`,
            }}
          />
        );
      })}
    </AbsoluteFill>
  );
};
