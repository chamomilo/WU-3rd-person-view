package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FreeLookModeIndicatorTest {
    @Test
    public void placesPanelAtConfiguredBottomRightMargins() {
        assertEquals(798, HudCornerLayout.rightAligned(1000, 178, 24));
        assertEquals(678, HudCornerLayout.bottomAligned(768, 42, 48));
    }

    @Test
    public void keepsPanelOnScreenAtSmallResolutions() {
        assertEquals(0, HudCornerLayout.rightAligned(160, 178, 24));
        assertEquals(0, HudCornerLayout.bottomAligned(80, 42, 48));
    }

    @Test
    public void placesPanelAboveEventWindowWithGap() {
        assertEquals(878, HudCornerLayout.above(932, 42, 12));
        assertEquals(0, HudCornerLayout.above(30, 42, 12));
    }
}
