package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CameraCollisionProbeTest {
    @Test
    public void finalHeightCannotRemainBelowSurfaceClearance() {
        assertEquals(12.25f, CameraCollisionProbe.clampHeight(10.0f, 12.25f), 0.001f);
        assertEquals(14.0f, CameraCollisionProbe.clampHeight(14.0f, 12.25f), 0.001f);
    }

    @Test
    public void unavailableSurfaceDoesNotCorruptCameraHeight() {
        assertEquals(10.0f, CameraCollisionProbe.clampHeight(10.0f, Float.NaN), 0.001f);
        assertEquals(10.0f,
                CameraCollisionProbe.clampHeight(10.0f, Float.POSITIVE_INFINITY), 0.001f);
    }
}
