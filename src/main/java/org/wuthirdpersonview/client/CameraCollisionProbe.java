package org.wuthirdpersonview.client;

import com.wurmonline.client.collision.CollisionManager;
import com.wurmonline.client.collision.advanced.CollisionManagerAdvanced;
import com.wurmonline.client.collision.advanced.CollisionResult;
import com.wurmonline.client.collision.advanced.CollisionView;
import com.wurmonline.client.collision.advanced.Sphere;
import com.wurmonline.client.collision.advanced.Transform;
import com.wurmonline.client.game.CaveDataBuffer;
import com.wurmonline.client.game.NearTerrainDataBuffer;
import com.wurmonline.client.game.World;
import com.wurmonline.mesh.Tiles;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import org.wuthirdpersonview.core.CameraConfig;
import org.wuthirdpersonview.core.CameraIntent;

/** Uses the client's read-only collision view; it never advances or mutates player physics. */
final class CameraCollisionProbe {
    private static final Logger LOG = Logger.getLogger(CameraCollisionProbe.class.getName());
    private final CameraConfig config;
    private final CaveBufferGeometry caveGeometry = new CaveBufferGeometry();
    private Field collisionViewField;
    private boolean advancedUnavailableLogged;
    private boolean collisionFailureLogged;

    CameraCollisionProbe(CameraConfig config) {
        this.config = config;
        try {
            collisionViewField = CollisionManagerAdvanced.class.getDeclaredField("collisionView");
            collisionViewField.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            LOG.log(Level.WARNING, "Advanced collision view is unavailable", failure);
        }
    }

    float safeDistance(World world, CameraIntent intent) {
        float safe = intent.rayLength;
        try {
            CollisionManager manager = world.getCollisionManager();
            if (manager instanceof CollisionManagerAdvanced && collisionViewField != null) {
                CollisionView view = (CollisionView) collisionViewField.get(manager);
                safe = Math.min(safe, sweep(view, world.getPlayerLayer(), intent));
            } else if (!advancedUnavailableLogged) {
                advancedUnavailableLogged = true;
                LOG.warning("Advanced collision manager is not active; using loaded terrain/cave geometry");
            }
        } catch (Throwable failure) {
            if (!collisionFailureLogged) {
                collisionFailureLogged = true;
                LOG.log(Level.WARNING, "Camera collision sweep failed; using terrain fallback", failure);
            }
        }
        safe = Math.min(safe, geometryDistance(world, intent));
        return CameraMathSupport.clamp(safe, 0.0f, intent.rayLength);
    }

    /** Also used for native free-camera movement, from its position before the frame. */
    float geometryDistance(World world, CameraIntent path) {
        try {
            CaveDataBuffer cave = world.getCaveBuffer();
            if (cave != null && (world.getPlayerLayer() < 0
                    || cave.isValid(path.anchorX, path.anchorZ)
                    || cave.isValid(path.cameraX, path.cameraZ))) {
                caveGeometry.use(cave, world.getNearTerrainBuffer());
                return CaveVolumeClipper.safeDistance(caveGeometry, path,
                        config.cameraRadius + config.collisionMargin, config.surfaceClearance);
            }
            if (world.getPlayerLayer() < 0) return 0.0f;
            return terrainDistance(world, path);
        } catch (RuntimeException failure) {
            if (!collisionFailureLogged) {
                collisionFailureLogged = true;
                LOG.log(Level.WARNING, "Cannot read camera boundary geometry; holding camera", failure);
            }
            return 0.0f;
        }
    }

    float constrainedHeight(World world, float x, float y, float z) {
        CaveDataBuffer cave = world.getCaveBuffer();
        if (cave != null && (world.getPlayerLayer() < 0 || cave.isValid(x, z))) {
            caveGeometry.use(cave, world.getNearTerrainBuffer());
            return CaveVolumeClipper.clampHeight(caveGeometry, x, y, z,
                    config.cameraRadius + config.collisionMargin, config.surfaceClearance);
        }
        return clampHeight(y, minimumCameraHeight(world, x, z));
    }

    private float sweep(CollisionView view, int layer, CameraIntent intent) {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m03 = intent.anchorX;
        matrix.m13 = intent.anchorY;
        matrix.m23 = intent.anchorZ;
        Transform start = new Transform(matrix);
        Vector3f velocity = new Vector3f(intent.cameraX - intent.anchorX,
                intent.cameraY - intent.anchorY, intent.cameraZ - intent.anchorZ);
        Sphere camera = new Sphere(config.cameraRadius, layer);
        CollisionResult result = new CollisionResult();
        float minimumFraction = -config.collisionMargin / Math.max(0.001f, intent.rayLength);
        float fraction = view.calcImpact(camera, start, velocity, result, null,
                minimumFraction, true);
        if (Float.isNaN(fraction) || fraction >= 1.0f) return intent.rayLength;
        return Math.max(0.0f, intent.rayLength * Math.max(0.0f, fraction)
                - config.collisionMargin);
    }

    private float terrainDistance(World world, CameraIntent intent) {
        if (intent.rayLength <= 0.001f) return intent.rayLength;
        int samples = Math.max(2, (int) Math.ceil(intent.rayLength / 0.20f));
        float previousDistance = 0.0f;
        for (int i = 1; i <= samples; i++) {
            float t = i / (float) samples;
            float x = lerp(intent.anchorX, intent.cameraX, t);
            float y = lerp(intent.anchorY, intent.cameraY, t);
            float z = lerp(intent.anchorZ, intent.cameraZ, t);
            float minimumHeight = minimumCameraHeight(world, x, z);
            if (!Float.isNaN(minimumHeight) && y < minimumHeight) {
                return Math.max(0.0f, previousDistance - config.collisionMargin);
            }
            previousDistance = intent.rayLength * t;
        }
        return intent.rayLength;
    }

    float minimumCameraHeight(World world, float x, float z) {
        if (world == null) return Float.NaN;
        float floor;
        try {
            if (world.getPlayerLayer() < 0) {
                CaveDataBuffer cave = world.getCaveBuffer();
                if (cave == null || !cave.isValid(x, z)) return Float.NaN;
                floor = cave.getInterpolatedFloor(x, z);
            } else {
                NearTerrainDataBuffer terrain = world.getNearTerrainBuffer();
                if (terrain == null || !terrain.isValid(x, z)) return Float.NaN;
                floor = terrain.getInterpolatedHeight(x, z);
            }
        } catch (RuntimeException outsideLoadedTerrain) {
            return Float.NaN;
        }
        if (Float.isNaN(floor) || Float.isInfinite(floor)) return Float.NaN;
        return floor + Math.max(config.cameraRadius, config.surfaceClearance);
    }

    static float clampHeight(float cameraHeight, float minimumHeight) {
        if (Float.isNaN(minimumHeight) || Float.isInfinite(minimumHeight))
            return cameraHeight;
        return Math.max(cameraHeight, minimumHeight);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    String backend(World world) {
        if (world == null) return "unavailable";
        boolean advanced = world.getCollisionManager() instanceof CollisionManagerAdvanced;
        if (world.getPlayerLayer() < 0) return advanced ? "advanced+cave" : "cave";
        return advanced ? "advanced+terrain" : "terrain-only";
    }

    private static final class CaveBufferGeometry implements CaveVolumeClipper.Geometry {
        private CaveDataBuffer cave;
        private NearTerrainDataBuffer terrain;

        private void use(CaveDataBuffer value, NearTerrainDataBuffer surface) {
            cave = value;
            terrain = surface;
        }

        @Override public boolean isLoaded(float x, float z) { return cave.isValid(x, z); }
        @Override public boolean isOpen(int x, int z) { return !Tiles.isSolidCave(cave.getTileType(x, z)); }
        @Override public boolean isExit(int x, int z) { return cave.getTileType(x, z) == Tiles.Tile.TILE_CAVE_EXIT; }
        @Override public float floor(float x, float z) { return cave.getInterpolatedFloor(x, z); }
        @Override public float ceiling(float x, float z) { return cave.getInterpolatedCeiling(x, z); }
        @Override public float surface(float x, float z) {
            return terrain != null && terrain.isValid(x, z)
                    ? terrain.getInterpolatedHeight(x, z) : Float.NaN;
        }
    }

    /** Local helper avoids making implementation-only clamping part of the public core API. */
    private static final class CameraMathSupport {
        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
