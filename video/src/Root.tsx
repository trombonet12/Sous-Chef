import React from "react";
import { AbsoluteFill, Composition } from "remotion";

const Promo: React.FC = () => {
  return (
    <AbsoluteFill
      style={{
        backgroundColor: "#F3EFE4",
        justifyContent: "center",
        alignItems: "center",
        fontFamily: "sans-serif",
        fontSize: 160,
        fontWeight: 700,
        color: "#2B2B2B",
      }}
    >
      Sous Chef
    </AbsoluteFill>
  );
};

export const RemotionRoot: React.FC = () => {
  return (
    <Composition
      id="Promo"
      component={Promo}
      width={1920}
      height={1080}
      fps={30}
      durationInFrames={60}
    />
  );
};
