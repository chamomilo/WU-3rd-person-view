package org.wuthirdpersonview.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CaveDataCoverageTest {
    @Test public void separateReceivedCaveNeedsNoConnectionToPlayersCave() {
        Object buffer = new Object();
        receive(buffer, 0, 0, 4, 4);
        receive(buffer, 20, 0, 4, 4);
        assertTrue(CaveDataCoverage.hasFloorCorners(buffer, 1, 1));
        assertTrue(CaveDataCoverage.hasFloorCorners(buffer, 21, 1));
        assertFalse(CaveDataCoverage.hasFloorCorners(buffer, 10, 1));
    }

    @Test public void incompleteOrFailedStripIsNotGeometry() {
        Object buffer = new Object();
        receive(buffer, 10, 10, 3, 3);
        CaveDataCoverage.beforeStrip(buffer, 11, 11, 1, 1);
        assertFalse(CaveDataCoverage.hasFloorCorners(buffer, 10, 10));
        CaveDataCoverage.afterStrip(buffer, 11, 11, 1, 1);
        assertTrue(CaveDataCoverage.hasFloorCorners(buffer, 10, 10));
    }

    @Test public void ringOverwriteDoesNotExposeOldCoordinates() {
        Object buffer = new Object();
        receive(buffer, 0, 0, 64, 4);
        receive(buffer, 64, 0, 2, 4);
        assertFalse(CaveDataCoverage.hasFloorCorners(buffer, 0, 1));
        assertTrue(CaveDataCoverage.hasFloorCorners(buffer, 64, 1));
    }

    @Test public void cornerAcrossReceivedBoundaryRemainsUnknown() {
        Object buffer = new Object();
        receive(buffer, 20, 20, 2, 2);
        assertTrue(CaveDataCoverage.hasFloorCorners(buffer, 20, 20));
        assertFalse(CaveDataCoverage.hasFloorCorners(buffer, 21, 20));
    }

    @Test public void reconnectClearsCoverageAndBuffersAreIndependent() {
        Object buffer = new Object(), other = new Object();
        receive(buffer, 0, 0, 3, 3);
        assertFalse(CaveDataCoverage.isTracked(other));
        CaveDataCoverage.clear(buffer);
        assertFalse(CaveDataCoverage.isTracked(buffer));
        assertFalse(CaveDataCoverage.hasFloorCorners(buffer, 0, 0));
    }

    private static void receive(Object buffer, int x, int y, int width, int height) {
        CaveDataCoverage.beforeStrip(buffer, x, y, width, height);
        CaveDataCoverage.afterStrip(buffer, x, y, width, height);
    }
}
