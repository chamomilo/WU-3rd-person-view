package org.wuthirdpersonview.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RenderFocusPolicyTest {
    @Test
    public void normalFirstPersonKeepsPlayerAsFocus() {
        assertEquals(0.0f, RenderFocusPolicy.blend(false, false, 1.0f), 0.0f);
    }

    @Test
    public void thirdPersonMovesFocusProgressivelyToCamera() {
        assertEquals(0.0f, RenderFocusPolicy.blend(false, true, 0.0f), 0.0f);
        assertEquals(0.5f, RenderFocusPolicy.blend(false, true, 0.5f), 0.0001f);
        assertEquals(1.0f, RenderFocusPolicy.blend(false, true, 1.0f), 0.0f);
        assertEquals(8, RenderFocusPolicy.MAXIMUM_VEGETATION_REACH_TILES);
    }

    @Test
    public void freeCameraAlwaysOwnsTheRenderFocus() {
        assertEquals(1.0f, RenderFocusPolicy.blend(true, false, 0.0f), 0.0f);
    }
}
