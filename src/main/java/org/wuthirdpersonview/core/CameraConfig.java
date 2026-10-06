package org.wuthirdpersonview.core;

import java.util.Locale;
import java.util.Properties;

public final class CameraConfig {
    public final boolean enabled;
    public final float defaultZoom;
    public final float zoomStep;
    public final float minDistance;
    public final float maxDistance;
    public final float headAnchorVerticalOffset;
    public final boolean animateHead;
    public final float maxHeadYawDegrees;
    public final float maxHeadPitchDegrees;
    public final float bodyFollowSeconds;
    public final float zoomSmoothingSeconds;
    public final float collisionReturnSeconds;
    public final float cameraRadius;
    public final float collisionMargin;
    public final float surfaceClearance;
    public final float firstPersonEnterDistance;
    public final float firstPersonExitDistance;
    public final String orbitKey;
    public final float orbitSensitivity;
    public final boolean orbitReturns;
    public final float orbitReturnSeconds;
    public final float maxOrbitPitchDegrees;
    public final String inputProvider;
    public final float holsterDurationSeconds;
    public final boolean debug;

    private CameraConfig(Properties properties) {
        enabled = bool(properties, "enabled", true);
        defaultZoom = number(properties, "defaultZoom", 0.32f, 0.01f, 1.0f);
        zoomStep = number(properties, "zoomStep", 0.08f, 0.01f, 0.5f);
        // Anything closer than this places the camera inside common head/helmet meshes.
        minDistance = number(properties, "minDistance", 1.20f, 1.20f, 3.0f);
        maxDistance = number(properties, "maxDistance", 12.0f,
                minDistance + 0.1f, 50.0f);
        headAnchorVerticalOffset = number(properties, "headAnchorVerticalOffset", 0.0f,
                -1.0f, 1.0f);
        animateHead = bool(properties, "animateHead", true);
        maxHeadYawDegrees = number(properties, "maxHeadYawDegrees", 55.0f,
                10.0f, 80.0f);
        maxHeadPitchDegrees = number(properties, "maxHeadPitchDegrees", 60.0f,
                10.0f, 80.0f);
        bodyFollowSeconds = number(properties, "bodyFollowSeconds", 0.12f,
                0.0f, 1.0f);
        zoomSmoothingSeconds = number(properties, "zoomSmoothingSeconds", 0.10f,
                0.0f, 2.0f);
        collisionReturnSeconds = number(properties, "collisionReturnSeconds", 0.28f,
                0.0f, 3.0f);
        cameraRadius = number(properties, "cameraRadius", 0.24f, 0.05f, 1.5f);
        collisionMargin = number(properties, "collisionMargin", 0.10f, 0.0f, 1.0f);
        surfaceClearance = number(properties, "surfaceClearance", 0.25f,
                0.05f, 2.0f);
        firstPersonEnterDistance = number(properties, "firstPersonEnterDistance", 1.00f,
                1.00f, 2.0f);
        firstPersonExitDistance = number(properties, "firstPersonExitDistance", 1.15f,
                firstPersonEnterDistance + 0.05f, 3.0f);
        orbitKey = text(properties, "orbitKey", "LSHIFT").toUpperCase(Locale.ROOT);
        orbitSensitivity = number(properties, "orbitSensitivity", 1.0f, 0.05f, 8.0f);
        orbitReturns = bool(properties, "orbitReturns", true);
        orbitReturnSeconds = number(properties, "orbitReturnSeconds", 0.45f, 0.0f, 5.0f);
        maxOrbitPitchDegrees = number(properties, "maxOrbitPitchDegrees", 35.0f, 5.0f, 80.0f);
        String provider = text(properties, "inputProvider", "auto").toLowerCase(Locale.ROOT);
        inputProvider = provider.equals("native") || provider.equals("keybinder")
                ? provider : "auto";
        holsterDurationSeconds = number(properties, "holsterDurationSeconds", 1.0f,
                0.4f, 2.0f);
        debug = bool(properties, "debug", false);
    }

    public static CameraConfig from(Properties properties) {
        return new CameraConfig(properties == null ? new Properties() : properties);
    }

    private static String text(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static float number(Properties properties, String key, float fallback,
                                float minimum, float maximum) {
        try {
            return CameraMath.clamp(Float.parseFloat(properties.getProperty(key)), minimum, maximum);
        } catch (Exception ignored) {
            return CameraMath.clamp(fallback, minimum, maximum);
        }
    }
}
