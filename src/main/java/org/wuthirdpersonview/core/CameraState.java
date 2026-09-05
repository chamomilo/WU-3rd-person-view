package org.wuthirdpersonview.core;

public final class CameraState {
    private static final float MINIMUM_ZOOM_EPSILON = 0.001f;
    private final CameraConfig config;
    private boolean thirdPersonActive;
    private float zoom;
    private float rememberedZoom;
    private float smoothedDistance = -1.0f;
    private float actualRayDistance = -1.0f;
    private float orbitYawDegrees;
    private float orbitPitchDegrees;
    private boolean orbitHeld;
    private boolean collisionFirstPerson;
    private boolean recoveringFromCollision;
    private boolean debug;
    private CameraPose lastPose = CameraPose.stock();

    public CameraState(CameraConfig config) {
        this.config = config;
        zoom = config.defaultZoom;
        rememberedZoom = zoom;
        thirdPersonActive = config.enabled;
        debug = config.debug;
    }

    public void toggle() { setThirdPersonActive(!thirdPersonActive); }

    public void setThirdPersonActive(boolean active) {
        if (thirdPersonActive == active) return;
        thirdPersonActive = active;
        if (active) zoom = CameraMath.clamp(rememberedZoom, 0.0f, 1.0f);
        else rememberedZoom = zoom;
        resetViewTransition();
    }

    private void resetViewTransition() {
        smoothedDistance = -1.0f;
        actualRayDistance = -1.0f;
        collisionFirstPerson = false;
        recoveringFromCollision = false;
        lastPose = CameraPose.stock();
    }

    public void zoomIn() {
        if (!thirdPersonActive) return;
        if (zoom <= MINIMUM_ZOOM_EPSILON) {
            setThirdPersonActive(false);
            return;
        }
        setZoom(Math.max(0.0f, zoom - config.zoomStep));
    }

    public void zoomOut() {
        if (!thirdPersonActive) {
            setZoom(0.0f);
            return;
        }
        setZoom(zoom + config.zoomStep);
    }

    public void setZoom(float value) {
        float clamped = CameraMath.clamp(value, 0.0f, 1.0f);
        boolean activating = !thirdPersonActive;
        zoom = clamped;
        rememberedZoom = clamped;
        thirdPersonActive = true;
        if (activating) resetViewTransition();
    }

    public void beginFrame(float deltaSeconds) {
        if (!thirdPersonActive) return;
        float dt = CameraMath.clamp(deltaSeconds, 0.0f, 0.1f);
        if (!orbitHeld && config.orbitReturns) {
            orbitYawDegrees = CameraMath.exponentialApproach(orbitYawDegrees, 0.0f,
                    dt, config.orbitReturnSeconds);
            orbitPitchDegrees = CameraMath.exponentialApproach(orbitPitchDegrees, 0.0f,
                    dt, config.orbitReturnSeconds);
            if (Math.abs(orbitYawDegrees) < 0.01f) orbitYawDegrees = 0.0f;
            if (Math.abs(orbitPitchDegrees) < 0.01f) orbitPitchDegrees = 0.0f;
        }
        float targetDistance = CameraMath.distanceForZoom(config, zoom);
        if (smoothedDistance < 0.0f) smoothedDistance = targetDistance;
        else smoothedDistance = CameraMath.exponentialApproach(smoothedDistance,
                targetDistance, dt, config.zoomSmoothingSeconds);
    }

    public CameraIntent intent(float anchorX, float anchorY, float anchorZ,
                               float viewYawDegrees, float viewPitchDegrees) {
        if (!thirdPersonActive) return null;
        if (smoothedDistance < 0.0f)
            smoothedDistance = CameraMath.distanceForZoom(config, zoom);
        float yaw = viewYawDegrees + orbitYawDegrees;
        float pitch = CameraMath.clamp(viewPitchDegrees + orbitPitchDegrees,
                -89.0f, 89.0f);
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        float horizontal = (float) (Math.cos(pitchRad) * smoothedDistance);
        float x = anchorX - (float) Math.sin(yawRad) * horizontal;
        float y = anchorY + (float) Math.sin(pitchRad) * smoothedDistance;
        float z = anchorZ + (float) Math.cos(yawRad) * horizontal;
        float dx = x - anchorX;
        float dy = y - anchorY;
        float dz = z - anchorZ;
        return new CameraIntent(anchorX, anchorY, anchorZ, x, y, z, yaw, pitch,
                (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    public CameraPose resolve(CameraIntent intent, float safeRayDistance, float deltaSeconds) {
        if (!thirdPersonActive || intent == null) {
            lastPose = CameraPose.stock();
            return lastPose;
        }
        float target = Math.min(intent.rayLength,
                CameraMath.clamp(safeRayDistance, 0.0f, intent.rayLength));
        float dt = CameraMath.clamp(deltaSeconds, 0.0f, 0.1f);
        boolean constrained = target + 0.01f < intent.rayLength;
        if (actualRayDistance < 0.0f) {
            actualRayDistance = target;
            recoveringFromCollision = constrained;
        } else if (constrained) {
            recoveringFromCollision = true;
            if (target < actualRayDistance) actualRayDistance = target;
            else actualRayDistance = CameraMath.exponentialApproach(actualRayDistance, target,
                    dt, config.collisionReturnSeconds);
        } else if (recoveringFromCollision) {
            actualRayDistance = CameraMath.exponentialApproach(actualRayDistance,
                    intent.rayLength, dt, config.collisionReturnSeconds);
            if (Math.abs(actualRayDistance - intent.rayLength) < 0.01f) {
                actualRayDistance = intent.rayLength;
                recoveringFromCollision = false;
            }
        } else {
            // Zoom smoothing is already represented by intent.rayLength.
            actualRayDistance = intent.rayLength;
        }

        if (collisionFirstPerson) {
            float exitDistance = Math.min(config.firstPersonExitDistance, intent.rayLength);
            if (actualRayDistance >= exitDistance)
                collisionFirstPerson = false;
        } else if (constrained && actualRayDistance <= config.firstPersonEnterDistance) {
            collisionFirstPerson = true;
        }
        if (collisionFirstPerson) {
            lastPose = CameraPose.collisionFirstPerson(actualRayDistance, intent.rayLength);
            return lastPose;
        }

        float scale = intent.rayLength <= 0.0001f ? 0.0f : actualRayDistance / intent.rayLength;
        float x = intent.anchorX + (intent.cameraX - intent.anchorX) * scale;
        float y = intent.anchorY + (intent.cameraY - intent.anchorY) * scale;
        float z = intent.anchorZ + (intent.cameraZ - intent.anchorZ) * scale;
        boolean limited = actualRayDistance + 0.01f < intent.rayLength;
        lastPose = CameraPose.thirdPerson(x, y, z, intent.yawDegrees, intent.pitchDegrees,
                actualRayDistance, intent.rayLength, limited);
        return lastPose;
    }

    public void addOrbitInput(float yawDeltaDegrees, float pitchDeltaDegrees) {
        orbitYawDegrees = wrap(orbitYawDegrees + yawDeltaDegrees);
        orbitPitchDegrees = CameraMath.clamp(orbitPitchDegrees + pitchDeltaDegrees,
                -config.maxOrbitPitchDegrees, config.maxOrbitPitchDegrees);
    }

    public void resetOrbit() {
        orbitYawDegrees = 0.0f;
        orbitPitchDegrees = 0.0f;
    }

    private static float wrap(float value) {
        float wrapped = value % 360.0f;
        if (wrapped > 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    public CameraConfig getConfig() { return config; }
    public boolean isThirdPersonActive() { return thirdPersonActive; }
    public boolean isOrbitHeld() { return orbitHeld; }
    public void setOrbitHeld(boolean held) { orbitHeld = held; }
    public float getZoom() { return zoom; }
    public float getOrbitYawDegrees() { return orbitYawDegrees; }
    public float getOrbitPitchDegrees() { return orbitPitchDegrees; }
    public boolean isDebug() { return debug; }
    public void setDebug(boolean value) { debug = value; }
    public CameraPose getLastPose() { return lastPose; }
}
