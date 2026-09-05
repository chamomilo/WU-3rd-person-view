package org.wuthirdpersonview.client;

import org.wuthirdpersonview.core.CameraIntent;

/**
 * Clips the camera arm against Wurm cave-tile borders. Cave walls are not
 * always present in the advanced collision view, while CaveDataBuffer remains
 * authoritative about whether each north/west tile border has a wall.
 */
final class CaveWallClipper {
    private static final float TILE_SIZE = 4.0f;
    private static final float DIRECTION_EPSILON = 0.00001f;
    private static final float CORNER_EPSILON = 0.0001f;
    private static final int MAX_BOUNDARIES_PER_AXIS = 128;

    interface WallGrid {
        boolean hasNorthWall(int tileX, int tileY);
        boolean hasWestWall(int tileX, int tileY);
    }

    private CaveWallClipper() { }

    static float safeDistance(WallGrid walls, CameraIntent intent, float padding) {
        if (walls == null || intent == null || intent.rayLength <= DIRECTION_EPSILON)
            return intent == null ? 0.0f : intent.rayLength;
        float safe = intent.rayLength;
        safe = Math.min(safe, clipWestWalls(walls, intent, Math.max(0.0f, padding)));
        safe = Math.min(safe, clipNorthWalls(walls, intent, Math.max(0.0f, padding)));
        return clamp(safe, 0.0f, intent.rayLength);
    }

    private static float clipWestWalls(WallGrid walls, CameraIntent intent, float padding) {
        float deltaX = intent.cameraX - intent.anchorX;
        if (Math.abs(deltaX) <= DIRECTION_EPSILON) return intent.rayLength;
        int direction = deltaX > 0.0f ? 1 : -1;
        int boundaryIndex = direction > 0
                ? floorTile(intent.anchorX) + 1
                : (int) Math.ceil(intent.anchorX / TILE_SIZE) - 1;
        float safe = intent.rayLength;
        for (int crossed = 0; crossed < MAX_BOUNDARIES_PER_AXIS; crossed++) {
            float boundaryX = boundaryIndex * TILE_SIZE;
            float t = (boundaryX - intent.anchorX) / deltaX;
            float safeT = t - padding / Math.abs(deltaX);
            if (safeT > 1.0f + DIRECTION_EPSILON) break;
            if (t >= -DIRECTION_EPSILON) {
                float z = lerp(intent.anchorZ, intent.cameraZ, t);
                int tileY = floorTile(z);
                if (walls.hasWestWall(boundaryIndex, tileY)
                        || (onTileBoundary(z)
                        && walls.hasWestWall(boundaryIndex, tileY - 1)))
                    safe = Math.min(safe, Math.max(0.0f, safeT) * intent.rayLength);
            }
            boundaryIndex += direction;
        }
        return safe;
    }

    private static float clipNorthWalls(WallGrid walls, CameraIntent intent, float padding) {
        float deltaZ = intent.cameraZ - intent.anchorZ;
        if (Math.abs(deltaZ) <= DIRECTION_EPSILON) return intent.rayLength;
        int direction = deltaZ > 0.0f ? 1 : -1;
        int boundaryIndex = direction > 0
                ? floorTile(intent.anchorZ) + 1
                : (int) Math.ceil(intent.anchorZ / TILE_SIZE) - 1;
        float safe = intent.rayLength;
        for (int crossed = 0; crossed < MAX_BOUNDARIES_PER_AXIS; crossed++) {
            float boundaryZ = boundaryIndex * TILE_SIZE;
            float t = (boundaryZ - intent.anchorZ) / deltaZ;
            float safeT = t - padding / Math.abs(deltaZ);
            if (safeT > 1.0f + DIRECTION_EPSILON) break;
            if (t >= -DIRECTION_EPSILON) {
                float x = lerp(intent.anchorX, intent.cameraX, t);
                int tileX = floorTile(x);
                if (walls.hasNorthWall(tileX, boundaryIndex)
                        || (onTileBoundary(x)
                        && walls.hasNorthWall(tileX - 1, boundaryIndex)))
                    safe = Math.min(safe, Math.max(0.0f, safeT) * intent.rayLength);
            }
            boundaryIndex += direction;
        }
        return safe;
    }

    private static int floorTile(float coordinate) {
        return (int) Math.floor(coordinate / TILE_SIZE);
    }

    private static boolean onTileBoundary(float coordinate) {
        float tileCoordinate = coordinate / TILE_SIZE;
        return Math.abs(tileCoordinate - Math.round(tileCoordinate)) <= CORNER_EPSILON;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
