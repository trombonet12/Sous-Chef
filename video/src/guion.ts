export type Tono = "AMBAR" | "OLIVA" | "TEJA" | "AZUL" | "LILA";

export type Tramo = {
  clip: string;
  /** Segundo del clip en el que empieza el tramo. */
  inicio: number;
  /** Duración visible del tramo, en segundos. */
  duracion: number;
  titulo: string;
  subtitulo: string;
  tono: Tono;
};

export const guion = {
  fps: 30,
  entrada: 2.5,
  cierre: 3.2,
  transicion: 0.5,
  musica: { archivo: "musica/cozy-puzzle-in-game-3.mp3", volumen: 0.55 } as null | { archivo: string; volumen: number },
  tramos: [
    {
      clip: "clips/c1-recetas.mp4",
      inicio: 0.1,
      duracion: 5.6,
      titulo: "Elige por categoría",
      subtitulo: "Pasta, arroz, pescado… y su lista",
      tono: "AMBAR",
    },
    {
      clip: "clips/c2-ficha.mp4",
      inicio: 0,
      duracion: 5.6,
      titulo: "Te dice a qué hora empezar",
      subtitulo: "Eliges cuándo servir y calcula el resto",
      tono: "OLIVA",
    },
    {
      clip: "clips/c3-cronometro.mp4",
      inicio: 0.4,
      duracion: 5.6,
      titulo: "Un cronómetro por recipiente",
      subtitulo: "Sartén, olla y horno a la vez, con avisos",
      tono: "TEJA",
    },
    {
      clip: "clips/c4-tecnicas.mp4",
      inicio: 0.6,
      duracion: 5.6,
      titulo: "Técnicas paso a paso",
      subtitulo: "Enlazadas desde cada receta",
      tono: "AZUL",
    },
    {
      clip: "clips/c5-historial.mp4",
      inicio: 0.5,
      duracion: 5.6,
      titulo: "Lo que has cocinado, guardado",
      subtitulo: "Con copia en un archivo tuyo",
      tono: "LILA",
    },
  ] as Tramo[],
};

/** El móvil entra medio segundo antes de que acabe la entrada. */
export const ANTICIPO_MOVIL_S = 0.5;

export const frames = (s: number): number => Math.round(s * guion.fps);

/** Frame global en que empiezan los tramos (y entra el móvil). */
export const inicioTramos = (): number => frames(guion.entrada - ANTICIPO_MOVIL_S);

/** Frame global de inicio de cada tramo (los tramos se solapan `transicion`). */
export const inicioTramo = (i: number): number => {
  let f = inicioTramos();
  for (let k = 0; k < i; k++) {
    f += frames(guion.tramos[k].duracion) - frames(guion.transicion);
  }
  return f;
};

export const finTramos = (): number => {
  const n = guion.tramos.length - 1;
  return inicioTramo(n) + frames(guion.tramos[n].duracion);
};

export const duracionTotal = (): number => finTramos() + frames(guion.cierre);
