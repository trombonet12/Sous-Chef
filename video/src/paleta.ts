import type { Tono } from "./guion";

/** Pares acento/tinte claros de Paleta.kt. */
export const PALETA: Record<Tono | "ROSA" | "VERDE", { acento: string; tinte: string }> = {
  AMBAR: { acento: "#C99A2E", tinte: "#F5EBD3" },
  OLIVA: { acento: "#8C9A5B", tinte: "#EAEDDB" },
  TEJA: { acento: "#B9774A", tinte: "#F3E3D6" },
  AZUL: { acento: "#4F86A6", tinte: "#DEEAF0" },
  VERDE: { acento: "#5E9A6B", tinte: "#DCEBDF" },
  ROSA: { acento: "#B5707A", tinte: "#F2DEE0" },
  LILA: { acento: "#8A78B0", tinte: "#E7E2F0" },
};

export const PAPEL = "#F3EFE4";
export const TINTA = "#17150F";
export const TINTA_SUAVE = "#5B5648";
export const FUENTE = '"Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif';
