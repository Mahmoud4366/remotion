import {
  CalculateMetadataFunction,
  Composition,
  useCurrentFrame,
  useVideoConfig,
  spring,
  interpolate,
  AbsoluteFill
} from "remotion";

type Props = {};

const calculateMetadata: CalculateMetadataFunction<Props> = () => {
  return {};
};

export const MyComposition = () => {
  return (
    <Composition
      id="MyComp"
      component={MyComponent}
      durationInFrames={150}
      fps={30}
      width={1280}
      height={720}
      calculateMetadata={calculateMetadata}
    />
  );
};

export const MyComponent: React.FC<Props> = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  // Animate scale using a spring
  const scale = spring({
    fps,
    frame,
    config: {
      mass: 0.5,
      damping: 10,
    },
  });

  // Fade in over the first 30 frames
  const opacity = interpolate(frame, [0, 30], [0, 1], {
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill
      style={{
        background: "linear-gradient(135deg, #0f172a 0%, #312e81 100%)", // Modern dark gradient
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <h1
        style={{
          fontFamily: "system-ui, -apple-system, sans-serif",
          fontSize: "100px",
          color: "white",
          fontWeight: "bold",
          textAlign: "center",
          textShadow: "0px 10px 20px rgba(0,0,0,0.5)",
          transform: `scale(${scale})`,
          opacity,
          direction: "rtl"
        }}
      >
        پلتفرم فوق‌حرفه‌ای من
      </h1>
      <h2
        style={{
           fontFamily: "system-ui, -apple-system, sans-serif",
           fontSize: "40px",
           color: "#818cf8",
           marginTop: "30px",
           opacity: interpolate(frame, [30, 60], [0, 1], { extrapolateRight: "clamp" }),
           transform: `translateY(${interpolate(frame, [30, 60], [20, 0], { extrapolateRight: "clamp" })}px)`
        }}
      >
        کاملا واکنش‌گرا و سریع
      </h2>
    </AbsoluteFill>
  );
};
