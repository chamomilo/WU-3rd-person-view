package org.wuthirdpersonview.core;

public final class CameraIntent {
    public final float anchorX;
    public final float anchorY;
    public final float anchorZ;
    public final float cameraX;
    public final float cameraY;
    public final float cameraZ;
    public final float yawDegrees;
    public final float pitchDegrees;
    public final float rayLength;

    public static CameraIntent between(float startX, float startY, float startZ,
                                       float endX, float endY, float endZ) {
        float dx = endX - startX, dy = endY - startY, dz = endZ - startZ;
        return new CameraIntent(startX, startY, startZ, endX, endY, endZ,
                0.0f, 0.0f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    CameraIntent(float anchorX, float anchorY, float anchorZ,
                 float cameraX, float cameraY, float cameraZ,
                 float yawDegrees, float pitchDegrees, float rayLength) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
        this.rayLength = rayLength;
    }
}
