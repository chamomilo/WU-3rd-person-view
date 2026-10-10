package org.wuthirdpersonview.core;

/** Pure policy for moving Wurm's visual detail origin from player to camera. */
public final class RenderFocusPolicy {
    public static final float METERS_PER_TILE = 4.0f;
    public static final int MAXIMUM_VEGETATION_REACH_TILES = 8;

    private RenderFocusPolicy() { }

    public static float blend(boolean freeCamera, boolean thirdPerson, float zoom) {
        if (freeCamera) return 1.0f;
        if (!thirdPerson) return 0.0f;
        return CameraMath.smoothstep(0.0f, 1.0f, zoom);
    }
}
