package org.wuthirdpersonview.core;

public final class CameraMath {
    private CameraMath() { }

    public static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static float smoothstep(float edge0, float edge1, float value) {
        if (edge0 == edge1) return value < edge0 ? 0.0f : 1.0f;
        float t = clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    public static float exponentialApproach(float current, float target,
                                            float deltaSeconds, float timeConstantSeconds) {
        if (timeConstantSeconds <= 0.0f) return target;
        if (deltaSeconds <= 0.0f) return current;
        float alpha = 1.0f - (float) Math.exp(-deltaSeconds / timeConstantSeconds);
        return current + (target - current) * alpha;
    }

    public static float distanceForZoom(CameraConfig config, float zoom) {
        float t = smoothstep(0.0f, 1.0f, clamp(zoom, 0.0f, 1.0f));
        return config.minDistance + (config.maxDistance - config.minDistance) * t;
    }

}
