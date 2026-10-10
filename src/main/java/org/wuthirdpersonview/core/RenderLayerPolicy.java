package org.wuthirdpersonview.core;

/** Selects a visual layer from loaded geometry without changing the player layer. */
public final class RenderLayerPolicy {
    private RenderLayerPolicy() { }

    public static int layer(int playerLayer, float cameraHeight, float surfaceHeight,
                            float caveFloor, float caveCeiling, boolean caveExit) {
        if (!Float.isFinite(cameraHeight)) return playerLayer;
        if (Float.isFinite(surfaceHeight) && cameraHeight >= surfaceHeight) return 0;
        if (Float.isFinite(caveFloor) && cameraHeight >= caveFloor
                && (caveExit || (Float.isFinite(caveCeiling) && cameraHeight <= caveCeiling)))
            return -1;
        return playerLayer;
    }
}
