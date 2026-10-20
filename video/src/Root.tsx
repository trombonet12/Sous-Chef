import React from "react";
import { Composition } from "remotion";
import { Promo } from "./Promo";
import { duracionTotal, guion } from "./guion";

export const RemotionRoot: React.FC = () => {
  return (
    <Composition
      id="Promo"
      component={Promo}
      width={1920}
      height={1080}
      fps={guion.fps}
      durationInFrames={duracionTotal()}
      calculateMetadata={() => ({ durationInFrames: duracionTotal(), fps: guion.fps })}
    />
  );
};
