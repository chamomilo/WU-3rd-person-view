package org.wuthirdpersonview.core;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RenderLayerPolicyTest {
    @Test
    public void undergroundPlayerCanViewSurfaceAfterCameraLeavesExit() {
        assertEquals(0, RenderLayerPolicy.layer(-1, 12.0f, 10.0f, 0.0f, 3.0f, false));
        assertEquals(0, RenderLayerPolicy.layer(-1, 10.0f, 10.0f, 0.0f, 3.0f, true));
    }

    @Test
    public void undergroundPlayerCanViewSurfaceAboveSolidCaveTile() {
        assertEquals(0, RenderLayerPolicy.layer(-1, 12.0f, 10.0f,
                Float.NaN, Float.NaN, false));
    }

    @Test
    public void cameraReturningBelowSurfaceSelectsCaveEvenWithSurfacePlayer() {
        assertEquals(-1, RenderLayerPolicy.layer(0, 2.0f, 10.0f, 0.0f, 3.0f, false));
        assertEquals(-1, RenderLayerPolicy.layer(-1, 2.0f, 10.0f, 0.0f, 3.0f, false));
    }

    @Test
    public void rooflessExitKeepsCaveVisibleBelowSurfaceDespiteNominalCeiling() {
        assertEquals(-1, RenderLayerPolicy.layer(0, 7.0f, 10.0f, 0.0f, 3.0f, true));
        assertEquals(-1, RenderLayerPolicy.layer(0, 7.0f, Float.NaN,
                0.0f, Float.NaN, true));
    }

    @Test
    public void missingOrInvalidGeometryRetainsNativeLayer() {
        assertEquals(-1, RenderLayerPolicy.layer(-1, 12.0f, Float.NaN,
                Float.NaN, Float.NaN, false));
        assertEquals(0, RenderLayerPolicy.layer(0, Float.NaN, 10.0f, 0.0f, 3.0f, false));
        assertEquals(0, RenderLayerPolicy.layer(0, Float.POSITIVE_INFINITY,
                10.0f, 0.0f, 3.0f, true));
    }

    @Test
    public void solidRockBetweenFloorAndSurfaceIsNotAnOpenCave() {
        assertEquals(0, RenderLayerPolicy.layer(0, 7.0f, 10.0f, 0.0f, 3.0f, false));
        assertEquals(0, RenderLayerPolicy.layer(0, -1.0f, 10.0f, 0.0f, 3.0f, true));
    }
}
