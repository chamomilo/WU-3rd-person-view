package org.wuthirdpersonview.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import org.junit.Test;

public class CameraMathTest {
    private final CameraConfig config = CameraConfig.from(new Properties());

    @Test
    public void distanceCurveIsMonotonicAndBounded() {
        float previous = CameraMath.distanceForZoom(config, 0.0f);
        assertEquals(config.minDistance, previous, 0.001f);
        for (int i = 1; i <= 100; i++) {
            float current = CameraMath.distanceForZoom(config, i / 100.0f);
            assertTrue(current >= previous);
            previous = current;
        }
        assertEquals(config.maxDistance, previous, 0.001f);
    }

    @Test
    public void dampingDoesNotOvershoot() {
        float result = CameraMath.exponentialApproach(2.0f, 10.0f, 0.1f, 0.2f);
        assertTrue(result > 2.0f && result < 10.0f);
        assertEquals(10.0f, CameraMath.exponentialApproach(2.0f, 10.0f, 0.1f, 0.0f),
                0.001f);
        assertEquals(2.0f, CameraMath.exponentialApproach(2.0f, 10.0f, 0.0f, 0.2f),
                0.001f);
    }
}
