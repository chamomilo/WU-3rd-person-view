package org.wuthirdpersonview.client;

import org.junit.Test;
import org.wuthirdpersonview.core.CameraIntent;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class CaveVolumeClipperTest {
    private static final float PADDING = 0.35f;

    @Test public void stopsUpwardMovementBelowClosedCeiling() {
        assertEquals(0.65f, safe(new Cave(), path(2, 2, 2, 2, 15, 2)), 0.001f);
    }

    @Test public void stopsDownwardMovementAboveFloor() {
        assertEquals(1.65f, safe(new Cave(), path(2, 2, 2, 2, -8, 2)), 0.001f);
    }

    @Test public void respectsLargerConfiguredFloorClearance() {
        assertEquals(1.4f, CaveVolumeClipper.safeDistance(new Cave(),
                path(2, 2, 2, 2, -8, 2), PADDING, 0.6f), 0.001f);
    }

    @Test public void clipsWallsFromBothDirectionsAndOnBothAxes() {
        assertEquals(1.65f, safe(new Cave().solid(1, 0), path(2, 2, 2, 8, 2, 2)), 0.001f);
        assertEquals(1.65f, safe(new Cave().solid(0, 0), path(6, 2, 2, 0, 2, 2)), 0.001f);
        assertEquals(1.65f, safe(new Cave().solid(0, 1), path(2, 2, 2, 2, 2, 8)), 0.001f);
        assertEquals(1.65f, safe(new Cave().solid(0, 0), path(2, 2, 6, 2, 2, 0)), 0.001f);
    }

    @Test public void stopsSphereEvenWhenCentreRemainsBeforeWall() {
        assertEquals(1.65f, safe(new Cave().solid(1, 0), path(2, 2, 2, 3.8f, 2, 2)), 0.001f);
    }

    @Test public void cannotTunnelAcrossSolidTileIntoAnotherOpenTile() {
        assertEquals(1.65f, safe(new Cave().solid(1, 0), path(2, 2, 2, 22, 2, 2)), 0.001f);
    }

    @Test public void checksCornerOverlapBetweenRadialSamples() {
        Cave cave = new Cave().solid(1, 1);
        assertFalse(CaveVolumeClipper.contains(cave, 3.7f, 2, 3.85f, PADDING, PADDING));
        assertTrue(CaveVolumeClipper.contains(cave, 3.6f, 2, 3.6f, PADDING, PADDING));
    }

    @Test public void checksSlopingCeilingAlongHorizontalMovement() {
        Cave cave = new Cave() {
            @Override public float ceiling(float x, float z) { return 4 - x * 0.25f; }
        };
        float safe = safe(cave, path(2, 2, 2, 10, 2, 2));
        assertTrue(safe > 4 && safe < 4.6f);
    }

    @Test public void exitTileHasNoArtificialCeilingButKeepsFloor() {
        Cave exit = new Cave().exit(0, 0);
        CameraIntent up = path(2, 2, 2, 2, 15, 2);
        assertEquals(up.rayLength, safe(exit, up), 0.001f);
        assertEquals(1.65f, safe(exit, path(2, 2, 2, 2, -8, 2)), 0.001f);
    }

    @Test public void exitDoesNotRemoveNeighbouringRoofOrSideWalls() {
        Cave exit = new Cave().exit(0, 0).solid(0, 1);
        assertEquals(1.65f, safe(exit, path(2, 2, 2, 2, 2, 8)), 0.001f);
        assertFalse(CaveVolumeClipper.contains(exit, 4.5f, 4, 2, PADDING, PADDING));
    }

    @Test public void canLeaveAndReturnThroughExitWhilePlayerStaysUnderground() {
        Cave cave = new Cave().exit(0, 0).solid(1, 0);
        CameraIntent out = path(2, 2, 2, 2, 15, 2);
        assertEquals(out.rayLength, safe(cave, out), 0.001f);
        CameraIntent over = path(2, 15, 2, 6, 15, 2);
        assertEquals(over.rayLength, safe(cave, over), 0.001f);
        assertEquals(4.65f, safe(cave, path(6, 15, 2, 6, 2, 2)), 0.001f);
        CameraIntent back = path(2, 15, 2, 2, 2, 2);
        assertEquals(back.rayLength, safe(cave, back), 0.001f);
    }

    @Test public void canEnterCaveThroughExitAndContinueUnderNeighbouringRoof() {
        Cave cave = new Cave().exit(0, 0);
        CameraIntent down = path(2, 15, 2, 2, 2, 2);
        assertEquals(down.rayLength, safe(cave, down), 0.001f);
        CameraIntent inside = path(2, 2, 2, 6, 2, 2);
        assertEquals(inside.rayLength, safe(cave, inside), 0.001f);
    }

    @Test public void blocksUnloadedCaveInsteadOfTreatingItAsAnExit() {
        Cave cave = new Cave() {
            @Override public boolean isLoaded(float x, float z) { return x < 4; }
        };
        assertEquals(1.65f, safe(cave, path(2, 2, 2, 8, 2, 2)), 0.001f);
    }

    @Test public void canLeaveOneCaveAndEnterASeparateLoadedCaveThroughItsExit() {
        Cave cave = separateCaves();
        CameraIntent leave = path(2, 2, 2, 2, 12, 2);
        CameraIntent fly = path(2, 12, 2, 82, 12, 2);
        CameraIntent enter = path(82, 12, 2, 82, 2, 2);
        CameraIntent inside = path(82, 2, 2, 86, 2, 2);
        for (CameraIntent movement : new CameraIntent[] {leave, fly, enter, inside})
            assertEquals(movement.rayLength, safe(cave, movement), 0.001f);
        assertEquals(0.65f, safe(cave, path(86, 2, 2, 86, 12, 2)), 0.001f);
    }

    @Test public void separateUnloadedCaveStillBlocksEntryBelowTheSurface() {
        Cave cave = new Cave() {
            @Override public boolean isLoaded(float x, float z) { return x < 80; }
        }.exit(20, 0);
        assertEquals(1.65f, safe(cave, path(82, 12, 2, 82, 2, 2)), 0.001f);
    }

    private static Cave separateCaves() {
        return new Cave() {
            @Override public boolean isOpen(int x, int z) {
                return z == 0 && ((x >= 0 && x <= 2) || (x >= 20 && x <= 22));
            }
        }.exit(0, 0).exit(20, 0);
    }

    @Test public void missingCeilingDataIsNotAnOpening() {
        Cave cave = new Cave() {
            @Override public float ceiling(float x, float z) { return Float.NaN; }
        };
        assertEquals(0, safe(cave, path(2, 2, 2, 2, 8, 2)), 0);
    }

    @Test public void clampsNativeHeightInsideCaveAndLeavesRooflessExitsOpen() {
        Cave cave = new Cave();
        assertEquals(2.65f, CaveVolumeClipper.clampHeight(cave, 2, 5, 2, PADDING, PADDING), 0.001f);
        assertEquals(0.35f, CaveVolumeClipper.clampHeight(cave, 2, -1, 2, PADDING, PADDING), 0.001f);
        assertEquals(5, CaveVolumeClipper.clampHeight(cave.exit(0, 0), 2, 5, 2, PADDING, PADDING), 0);
    }

    @Test public void negativeCoordinatesAndStationaryFramesRemainSafe() {
        assertEquals(1.65f, safe(new Cave().solid(-2, -1), path(-2, 2, -2, -6, 2, -2)), 0.001f);
        assertEquals(0, safe(new Cave(), path(2, 2, 2, 2, 2, 2)), 0);
    }

    private static float safe(CaveVolumeClipper.Geometry cave, CameraIntent path) {
        return CaveVolumeClipper.safeDistance(cave, path, PADDING, PADDING);
    }

    private static CameraIntent path(float x, float y, float z, float ex, float ey, float ez) {
        return CameraIntent.between(x, y, z, ex, ey, ez);
    }

    private static class Cave implements CaveVolumeClipper.Geometry {
        private final Set<String> solid = new HashSet<String>();
        private final Set<String> exits = new HashSet<String>();
        Cave solid(int x, int z) { solid.add(x + ":" + z); return this; }
        Cave exit(int x, int z) { exits.add(x + ":" + z); return this; }
        @Override public boolean isLoaded(float x, float z) { return true; }
        @Override public boolean isOpen(int x, int z) { return !solid.contains(x + ":" + z); }
        @Override public boolean isExit(int x, int z) { return exits.contains(x + ":" + z); }
        @Override public float floor(float x, float z) { return 0; }
        @Override public float ceiling(float x, float z) { return 3; }
        @Override public float surface(float x, float z) { return 10; }
    }
}
