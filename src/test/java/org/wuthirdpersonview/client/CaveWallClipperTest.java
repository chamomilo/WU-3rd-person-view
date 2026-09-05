package org.wuthirdpersonview.client;

import org.junit.Test;
import org.wuthirdpersonview.core.CameraIntent;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class CaveWallClipperTest {
    private static final float PADDING = 0.35f;

    @Test
    public void clipsZoomBeforeCrossingWestCaveWall() {
        FakeWalls walls = new FakeWalls().west(1, 0);
        CameraIntent zoom = intent(2.0f, 2.0f, 6.0f, 2.0f);

        assertEquals(1.65f, CaveWallClipper.safeDistance(walls, zoom, PADDING), 0.001f);
    }

    @Test
    public void clipsCameraSphereEvenWhenItsCenterStopsShortOfWall() {
        FakeWalls walls = new FakeWalls().west(1, 0);
        CameraIntent close = intent(2.0f, 2.0f, 3.8f, 2.0f);

        assertEquals(1.65f, CaveWallClipper.safeDistance(walls, close, PADDING), 0.001f);
    }

    @Test
    public void clipsOrbitInEitherDirectionAndOnNorthWalls() {
        FakeWalls west = new FakeWalls().west(1, 0);
        FakeWalls north = new FakeWalls().north(0, 1);

        assertEquals(1.65f, CaveWallClipper.safeDistance(
                west, intent(6.0f, 2.0f, 2.0f, 2.0f), PADDING), 0.001f);
        assertEquals(1.65f, CaveWallClipper.safeDistance(
                north, intent(2.0f, 2.0f, 2.0f, 6.0f), PADDING), 0.001f);
        assertEquals(1.65f, CaveWallClipper.safeDistance(
                north, intent(2.0f, 6.0f, 2.0f, 2.0f), PADDING), 0.001f);
    }

    @Test
    public void leavesArmUnchangedWhenNoCaveBorderIsClosed() {
        CameraIntent clear = intent(2.0f, 2.0f, 10.0f, 2.0f);

        assertEquals(clear.rayLength, CaveWallClipper.safeDistance(
                new FakeWalls(), clear, PADDING), 0.001f);
    }

    private static CameraIntent intent(float anchorX, float anchorZ,
                                       float cameraX, float cameraZ) {
        float dx = cameraX - anchorX;
        float dz = cameraZ - anchorZ;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        try {
            Constructor<CameraIntent> constructor = CameraIntent.class.getDeclaredConstructor(
                    float.class, float.class, float.class,
                    float.class, float.class, float.class,
                    float.class, float.class, float.class);
            constructor.setAccessible(true);
            return constructor.newInstance(anchorX, 2.0f, anchorZ,
                    cameraX, 2.0f, cameraZ, 0.0f, 0.0f, length);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static final class FakeWalls implements CaveWallClipper.WallGrid {
        private final Set<String> north = new HashSet<String>();
        private final Set<String> west = new HashSet<String>();

        private FakeWalls north(int x, int y) {
            north.add(key(x, y));
            return this;
        }

        private FakeWalls west(int x, int y) {
            west.add(key(x, y));
            return this;
        }

        @Override
        public boolean hasNorthWall(int tileX, int tileY) {
            return north.contains(key(tileX, tileY));
        }

        @Override
        public boolean hasWestWall(int tileX, int tileY) {
            return west.contains(key(tileX, tileY));
        }

        private static String key(int x, int y) { return x + ":" + y; }
    }
}
