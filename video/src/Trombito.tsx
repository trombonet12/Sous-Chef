import React from "react";

export type TrombitoProps = {
  /** 0 = ojos abiertos, 1 = ojos cerrados (los dos). */
  parpadeo?: number;
  /** 0..1: intensidad del estirado/aplastado del salto. */
  rebote?: number;
  /** Grados de giro del gorro. */
  balanceoGorro?: number;
  /** 0..1: cierra solo el ojo derecho (guiño). */
  guino?: number;
  /** Ancho en px. */
  tamano?: number;
};

const OJOS = [
  { cx: 60, cy: 50, id: "ojo-i" },
  { cx: 76, cy: 50, id: "ojo-d" },
];
const R_OJO = 7.6;
const PIEL = "#F9D768";

const Parpado: React.FC<{ cx: number; cy: number; id: string; cierre: number }> = ({ cx, cy, id, cierre }) => {
  if (cierre <= 0.02) return null;
  const alto = 2 * R_OJO * cierre;
  const y = cy - R_OJO;
  return (
    <g>
      <clipPath id={id}>
        <circle cx={cx} cy={cy} r={R_OJO - 1.2} />
      </clipPath>
      <rect x={cx - R_OJO} y={y} width={2 * R_OJO} height={alto} fill={PIEL} clipPath={`url(#${id})`} />
      {cierre > 0.5 ? (
        <path
          d={`M${cx - 5.2},${y + alto - 0.6}Q${cx},${y + alto + 1.6} ${cx + 5.2},${y + alto - 0.6}`}
          fill="none"
          stroke="#6B3F08"
          strokeWidth={1.6}
          strokeLinecap="round"
        />
      ) : null}
    </g>
  );
};

/** Trombito con gorro (icono de la app), sin el fondo. Todo se anima por props. */
export const Trombito: React.FC<TrombitoProps> = ({
  parpadeo = 0,
  rebote = 0,
  balanceoGorro = 0,
  guino = 0,
  tamano = 300,
}) => {
  const escalaY = 1 + rebote * 0.1;
  const escalaX = 1 - rebote * 0.06;
  const cierreI = parpadeo;
  const cierreD = Math.max(parpadeo, guino);
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="20 14 68 76"
      width={tamano}
      height={tamano * (76 / 68)}
      role="img"
      aria-label="Sous Chef"
      style={{ overflow: "visible", transformOrigin: "50% 100%", transform: `scale(${escalaX}, ${escalaY})` }}
    >
      <g transform="translate(0 8.4) translate(54 54) scale(0.75) translate(-54 -54)">
        <path d="M46,47H27A7,7 0 0 0 27,61H46" fill="none" stroke="#B97A0C" strokeWidth="5.4" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M46,47H27A7,7 0 0 0 27,61H46" fill="none" stroke="#F5C033" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M42,44V64" fill="none" stroke="#B97A0C" strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M30,48C46,48 54,36 66,26.5L66,81.5C54,72 46,60 30,60Z" fill="#E8A92A" />
        <path d="M49,54a19,28 0 1,0 38,0a19,28 0 1,0 -38,0Z" fill="#C98A12" />
        <path d="M52.2,54a15.8,24.6 0 1,0 31.6,0a15.8,24.6 0 1,0 -31.6,0Z" fill={PIEL} />
        <path d="M52.5,63a3,3 0 1,0 6,0a3,3 0 1,0 -6,0Z" fill="#F6A58A" />
        <path d="M77.5,63a3,3 0 1,0 6,0a3,3 0 1,0 -6,0Z" fill="#F6A58A" />
        <path d="M52.4,50a7.6,7.6 0 1,0 15.2,0a7.6,7.6 0 1,0 -15.2,0Z" fill="#FFFFFF" stroke="#D8232A" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M68.4,50a7.6,7.6 0 1,0 15.2,0a7.6,7.6 0 1,0 -15.2,0Z" fill="#FFFFFF" stroke="#D8232A" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M57.4,51a3.4,3.4 0 1,0 6.8,0a3.4,3.4 0 1,0 -6.8,0Z" fill="#2B1B0E" />
        <path d="M73.4,51a3.4,3.4 0 1,0 6.8,0a3.4,3.4 0 1,0 -6.8,0Z" fill="#2B1B0E" />
        <path d="M60.8,49.8a1.2,1.2 0 1,0 2.4,0a1.2,1.2 0 1,0 -2.4,0Z" fill="#FFFFFF" />
        <path d="M76.8,49.8a1.2,1.2 0 1,0 2.4,0a1.2,1.2 0 1,0 -2.4,0Z" fill="#FFFFFF" />
        <Parpado cx={OJOS[0].cx} cy={OJOS[0].cy} id={OJOS[0].id} cierre={cierreI} />
        <Parpado cx={OJOS[1].cx} cy={OJOS[1].cy} id={OJOS[1].id} cierre={cierreD} />
        <path d="M62,64Q68,70 74,64" fill="none" stroke="#6B3F08" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
        <g transform={`rotate(${balanceoGorro} 68 33)`}>
          <path d="M52,30L52,20A7,7 0 0 1 59,13A9.5,9.5 0 0 1 77,13A7,7 0 0 1 84,20L84,30Z" fill="#FFFFFF" stroke="#CBBE9F" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M62,19V28M68,16.5V28M74,19V28" fill="none" stroke="#E3D8BF" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M53,27H83A2,2 0 0 1 85,29V32A2,2 0 0 1 83,34H53A2,2 0 0 1 51,32V29A2,2 0 0 1 53,27Z" fill="#FFFFFF" stroke="#CBBE9F" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
        </g>
      </g>
    </svg>
  );
};

/** Curva de parpadeo: ~0,15 s cada ~2,5 s. */
export const parpadeoEnFrame = (frame: number, fps: number, desfase = 0): number => {
  const periodo = Math.round(2.5 * fps);
  const dur = 0.15 * fps;
  const p = (((frame + desfase) % periodo) + periodo) % periodo;
  return p < dur ? Math.sin((Math.PI * p) / dur) : 0;
};

export const balanceoEnFrame = (frame: number, fps: number): number =>
  4 * Math.sin((2 * Math.PI * frame) / (1.8 * fps));
