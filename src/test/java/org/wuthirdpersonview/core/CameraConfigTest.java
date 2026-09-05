package org.wuthirdpersonview.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import org.junit.Test;

public class CameraConfigTest {
    @Test
    public void invalidValuesFallBackAndUnsafeValuesAreClamped() {
        Properties values = new Properties();
        values.setProperty("defaultZoom", "not-a-number");
        values.setProperty("zoomStep", "99");
        values.setProperty("minDistance", "0");
        values.setProperty("maxDistance", "0.01");
        values.setProperty("headAnchorVerticalOffset", "88");
        values.setProperty("maxHeadYawDegrees", "900");
        values.setProperty("maxHeadPitchDegrees", "1");
        values.setProperty("bodyFollowSeconds", "99");
        values.setProperty("surfaceClearance", "99");
        values.setProperty("inputProvider", "mystery");
        CameraConfig config = CameraConfig.from(values);
        assertEquals(0.32f, config.defaultZoom, 0.001f);
        assertEquals(0.5f, config.zoomStep, 0.001f);
        assertEquals(1.20f, config.minDistance, 0.001f);
        assertEquals(1.00f, config.firstPersonEnterDistance, 0.001f);
        assertEquals(1.15f, config.firstPersonExitDistance, 0.001f);
        assertTrue(config.maxDistance > config.minDistance);
        assertEquals(1.0f, config.headAnchorVerticalOffset, 0.001f);
        assertEquals(80.0f, config.maxHeadYawDegrees, 0.001f);
        assertEquals(10.0f, config.maxHeadPitchDegrees, 0.001f);
        assertEquals(1.0f, config.bodyFollowSeconds, 0.001f);
        assertEquals(2.0f, config.surfaceClearance, 0.001f);
        assertEquals("auto", config.inputProvider);
    }
}
