package org.wuthirdpersonview.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import org.junit.Test;

public class CameraStateTest {
    @Test public void liveTuningPreservesZoomEnabledStateAndOrbit() {
        CameraState state=new CameraState(instantConfig());state.setZoom(.7f);
        state.setOrbitHeld(true);state.addOrbitInput(20,10);state.setThirdPersonActive(false);
        Properties values=new Properties();values.setProperty("zoomStep","0.2");
        state.reconfigure(CameraConfig.from(values));
        assertFalse(state.isThirdPersonActive());assertEquals(.7f,state.getZoom(),.0001f);
        assertEquals(20,state.getOrbitYawDegrees(),.0001f);assertEquals(10,state.getOrbitPitchDegrees(),.0001f);
        assertEquals(.2f,state.getConfig().zoomStep,.0001f);
    }
    private static CameraConfig instantConfig() {
        Properties values = new Properties();
        values.setProperty("zoomSmoothingSeconds", "0");
        values.setProperty("collisionReturnSeconds", "0");
        return CameraConfig.from(values);
    }

    @Test
    public void zoomOutFromFirstPersonEntersThirdPersonAtMinimumZoom() {
        CameraState state = new CameraState(instantConfig());
        state.setThirdPersonActive(false);

        state.zoomOut();

        assertTrue(state.isThirdPersonActive());
        assertEquals(0.0f, state.getZoom(), 0.001f);
    }

    @Test
    public void zoomInAtMinimumZoomSwitchesToFirstPerson() {
        CameraState state = new CameraState(instantConfig());
        state.setZoom(0.0f);

        state.zoomIn();

        assertFalse(state.isThirdPersonActive());
        assertEquals(0.0f, state.getZoom(), 0.001f);
    }

    @Test
    public void zoomInReachesMinimumBeforeSwitchingToFirstPerson() {
        CameraState state = new CameraState(instantConfig());
        state.zoomIn();
        state.zoomIn();
        state.zoomIn();
        state.zoomIn();
        assertTrue(state.isThirdPersonActive());
        assertEquals(0.0f, state.getZoom(), 0.001f);

        state.zoomIn();

        assertFalse(state.isThirdPersonActive());
    }

    @Test
    public void unobstructedMinimumZoomRemainsAThirdPersonPose() {
        CameraState state = new CameraState(instantConfig());
        state.setZoom(0.0f);
        state.beginFrame(0.016f);
        CameraIntent intent = state.intent(10.0f, 20.0f, 30.0f, 0.0f, 0.0f);

        CameraPose pose = state.resolve(intent, intent.rayLength, 0.016f);

        assertTrue(state.isThirdPersonActive());
        assertTrue(pose.overrideCamera);
        assertFalse(pose.collisionFirstPerson);
        assertEquals(state.getConfig().minDistance, pose.actualDistance, 0.001f);
    }

    @Test
    public void collisionFallbackKeepsRequestedZoomAndUsesHysteresis() {
        CameraState state = new CameraState(instantConfig());
        float zoom = state.getZoom();
        state.beginFrame(0.016f);
        CameraIntent intent = state.intent(10.0f, 20.0f, 30.0f, 0.0f, 0.0f);
        CameraPose blocked = state.resolve(intent, 0.20f, 0.016f);
        assertTrue(blocked.collisionFirstPerson);
        assertFalse(blocked.overrideCamera);
        assertEquals(zoom, state.getZoom(), 0.001f);

        CameraPose clear = state.resolve(intent, intent.rayLength, 0.016f);
        assertTrue(clear.overrideCamera);
        assertFalse(clear.collisionFirstPerson);
        assertEquals(zoom, state.getZoom(), 0.001f);
    }

    @Test
    public void orbitDoesNotChangeBodyYawAndReturnsAfterRelease() {
        CameraState state = new CameraState(instantConfig());
        state.setOrbitHeld(true);
        state.addOrbitInput(90.0f, 12.0f);
        state.beginFrame(0.016f);
        CameraIntent orbit = state.intent(0, 2, 0, 25.0f, -10.0f);
        assertEquals(115.0f, orbit.yawDegrees, 0.001f);
        assertEquals(2.0f, orbit.pitchDegrees, 0.001f);
        state.setOrbitHeld(false);
        for (int i = 0; i < 100; i++) state.beginFrame(0.1f);
        assertEquals(0.0f, state.getOrbitYawDegrees(), 0.02f);
        assertEquals(0.0f, state.getOrbitPitchDegrees(), 0.02f);
    }

    @Test
    public void neutralHeadViewPlacesCameraDirectlyBehindHead() {
        CameraState state = new CameraState(instantConfig());
        state.beginFrame(0.016f);
        CameraIntent intent = state.intent(4.0f, 2.0f, 8.0f, 0.0f, 0.0f);
        assertEquals(4.0f, intent.cameraX, 0.001f);
        assertEquals(2.0f, intent.cameraY, 0.001f);
        assertTrue(intent.cameraZ > 8.0f);
    }

    @Test
    public void nativeHeadPitchTiltsTheEntireCameraBoom() {
        CameraState state = new CameraState(instantConfig());
        state.beginFrame(0.016f);

        CameraIntent lookingUp = state.intent(4.0f, 2.0f, 8.0f,
                0.0f, -35.0f);
        assertTrue(lookingUp.cameraY < lookingUp.anchorY);
        assertEquals(-35.0f, lookingUp.pitchDegrees, 0.001f);

        CameraIntent lookingDown = state.intent(4.0f, 2.0f, 8.0f,
                0.0f, 35.0f);
        assertTrue(lookingDown.cameraY > lookingDown.anchorY);
        assertEquals(35.0f, lookingDown.pitchDegrees, 0.001f);
    }

    @Test
    public void zoomChangesOnlyDistanceAlongTheHeadViewLine() {
        CameraState state = new CameraState(instantConfig());
        state.setZoom(0.0f);
        state.beginFrame(0.016f);
        CameraIntent close = state.intent(1.0f, 2.0f, 3.0f, 40.0f, -20.0f);

        state.setZoom(1.0f);
        state.beginFrame(0.016f);
        CameraIntent far = state.intent(1.0f, 2.0f, 3.0f, 40.0f, -20.0f);

        assertEquals(close.yawDegrees, far.yawDegrees, 0.001f);
        assertEquals(close.pitchDegrees, far.pitchDegrees, 0.001f);
        assertTrue(far.rayLength > close.rayLength);
        assertEquals((close.cameraX - close.anchorX) / close.rayLength,
                (far.cameraX - far.anchorX) / far.rayLength, 0.001f);
        assertEquals((close.cameraY - close.anchorY) / close.rayLength,
                (far.cameraY - far.anchorY) / far.rayLength, 0.001f);
        assertEquals((close.cameraZ - close.anchorZ) / close.rayLength,
                (far.cameraZ - far.anchorZ) / far.rayLength, 0.001f);
    }
}
