package org.wuthirdpersonview.core;

public final class CameraPose {
    private static final CameraPose STOCK = new CameraPose(false, 0, 0, 0, 0, 0,
            0, 0, false, false);

    public final boolean overrideCamera;
    public final float x;
    public final float y;
    public final float z;
    public final float yawRadians;
    public final float pitchRadians;
    public final float actualDistance;
    public final float desiredDistance;
    public final boolean collisionLimited;
    public final boolean collisionFirstPerson;

    private CameraPose(boolean overrideCamera, float x, float y, float z,
                       float yawRadians, float pitchRadians, float actualDistance,
                       float desiredDistance, boolean collisionLimited,
                       boolean collisionFirstPerson) {
        this.overrideCamera = overrideCamera;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yawRadians = yawRadians;
        this.pitchRadians = pitchRadians;
        this.actualDistance = actualDistance;
        this.desiredDistance = desiredDistance;
        this.collisionLimited = collisionLimited;
        this.collisionFirstPerson = collisionFirstPerson;
    }

    public static CameraPose stock() { return STOCK; }

    static CameraPose thirdPerson(float x, float y, float z, float yawDegrees,
                                  float pitchDegrees, float actualDistance,
                                  float desiredDistance, boolean limited) {
        return new CameraPose(true, x, y, z,
                (float) Math.toRadians(yawDegrees), (float) Math.toRadians(pitchDegrees),
                actualDistance, desiredDistance, limited, false);
    }

    static CameraPose collisionFirstPerson(float actualDistance, float desiredDistance) {
        return new CameraPose(false, 0, 0, 0, 0, 0, actualDistance, desiredDistance,
                true, true);
    }
}
