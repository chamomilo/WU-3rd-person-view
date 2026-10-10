package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.wurmonline.client.game.CaveDataBuffer;
import com.wurmonline.client.game.World;
import com.wurmonline.mesh.Tiles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.junit.Test;
import org.wuthirdpersonview.core.CameraIntent;
import org.wuthirdpersonview.core.CaveDataCoverage;

public class CaveBufferAccessTest {
    @Test public void receivedExitAtMinusTenMetresIsNotMissingData() throws Exception {
        final CaveDataBuffer cave = fixture((short) -100);
        CaveDataCoverage.beforeStrip(cave, 20, 20, 2, 2);
        CaveDataCoverage.afterStrip(cave, 20, 20, 2, 2);
        assertFalse("native isValid confuses a real floor with its sentinel", cave.isValid(82, 82));
        assertTrue(CaveBufferAccess.isLoaded(cave, 82, 82));
        assertEquals(-10.0f, cave.getInterpolatedFloor(82, 82), 0.001f);
        CaveVolumeClipper.Geometry geometry = new CaveVolumeClipper.Geometry() {
            public boolean isLoaded(float x, float z) { return CaveBufferAccess.isLoaded(cave, x, z); }
            public boolean isOpen(int x, int z) { return !Tiles.isSolidCave(cave.getTileType(x, z)); }
            public boolean isExit(int x, int z) { return cave.getTileType(x, z) == Tiles.Tile.TILE_CAVE_EXIT; }
            public float floor(float x, float z) { return cave.getInterpolatedFloor(x, z); }
            public float ceiling(float x, float z) { return cave.getInterpolatedCeiling(x, z); }
            public float surface(float x, float z) { return 10; }
        };
        CameraIntent enter = CameraIntent.between(82, 12, 82, 82, -8, 82);
        assertEquals(enter.rayLength,
                CaveVolumeClipper.safeDistance(geometry, enter, 0.35f, 0.35f), 0.001f);
    }

    @Test public void unreceivedOrOverwrittenCornersStayBlockedDespiteOldNativeHeights() throws Exception {
        CaveDataBuffer cave = fixture((short) 20);
        assertTrue(cave.isValid(82, 82));
        CaveDataCoverage.beforeStrip(cave, 0, 0, 3, 3);
        CaveDataCoverage.afterStrip(cave, 0, 0, 3, 3);
        assertFalse(CaveBufferAccess.isLoaded(cave, 82, 82));
        CaveDataCoverage.beforeStrip(cave, 20, 20, 2, 2);
        assertFalse(CaveBufferAccess.isLoaded(cave, 82, 82));
        CaveDataCoverage.afterStrip(cave, 20, 20, 2, 2);
        assertTrue(CaveBufferAccess.isLoaded(cave, 82, 82));
        CaveDataCoverage.beforeStrip(cave, 84, 20, 2, 2);
        CaveDataCoverage.afterStrip(cave, 84, 20, 2, 2);
        assertFalse(CaveBufferAccess.isLoaded(cave, 82, 82));
    }

    private static CaveDataBuffer fixture(short floor) throws Exception {
        // Allocate only a read-only world shell; no Wurm client or GL context is started.
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field(sun.misc.Unsafe.class, "theUnsafe").get(null);
        World world = (World) unsafe.allocateInstance(World.class);
        Constructor<CaveDataBuffer> constructor = CaveDataBuffer.class.getDeclaredConstructor(World.class);
        constructor.setAccessible(true);
        CaveDataBuffer cave = constructor.newInstance(world);
        for (String name : new String[] {"minx", "miny", "maxx", "maxy"})
            field(CaveDataBuffer.class, name).setShort(cave, (short) (name.startsWith("min") ? 0 : 63));
        short[] floors = (short[]) field(CaveDataBuffer.class, "floors").get(cave);
        short[] ceilings = (short[]) field(CaveDataBuffer.class, "ceilings").get(cave);
        byte[] types = (byte[]) field(CaveDataBuffer.class, "types").get(cave);
        for (int x = 20; x <= 21; x++) for (int z = 20; z <= 21; z++) {
            int offset = cave.getOffset(x, z);
            floors[offset] = floor;
            ceilings[offset] = (short) (floor + 30);
            types[offset] = Tiles.Tile.TILE_CAVE.id;
        }
        types[cave.getOffset(20, 20)] = Tiles.Tile.TILE_CAVE_EXIT.id;
        return cave;
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
