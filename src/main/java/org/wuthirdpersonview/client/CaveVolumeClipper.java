package org.wuthirdpersonview.client;

import org.wuthirdpersonview.core.CameraIntent;

/** Checks the union of loaded cave air and surface air, including roofless exit tiles. */
final class CaveVolumeClipper {
    private static final float STEP = 0.10f;
    private static final int MAX_STEPS = 16384;
    private static final float EPSILON = 0.0001f;
    private static final float DIAGONAL = 0.70710678f;

    interface Geometry {
        boolean isLoaded(float x, float z);
        boolean isOpen(int tileX, int tileZ);
        boolean isExit(int tileX, int tileZ);
        float floor(float x, float z);
        float ceiling(float x, float z);
        float surface(float x, float z);
    }

    private CaveVolumeClipper() { }

    static float safeDistance(Geometry geometry, CameraIntent path,
                              float radius, float floorClearance) {
        if (path.rayLength <= EPSILON) return 0.0f;
        float padding = Math.max(0.0f, radius);
        float clearance = Math.max(padding, floorClearance);
        if (!contains(geometry, path.anchorX, path.anchorY, path.anchorZ, padding, clearance))
            return 0.0f;
        int steps = Math.max(1, (int) Math.ceil(path.rayLength / STEP));
        float previous = 0.0f;
        for (int i = 1; i <= Math.min(steps, MAX_STEPS); i++) {
            float distance = Math.min(path.rayLength, i * STEP);
            if (!containsAt(geometry, path, distance, padding, clearance)) {
                float low = previous;
                float high = distance;
                for (int refinement = 0; refinement < 12; refinement++) {
                    float middle = (low + high) * 0.5f;
                    if (containsAt(geometry, path, middle, padding, clearance)) low = middle;
                    else high = middle;
                }
                return Math.max(0.0f, low - EPSILON);
            }
            previous = distance;
        }
        // Do not accept the unsampled remainder of an unusually long frame.
        return previous;
    }

    static boolean contains(Geometry geometry, float x, float y, float z,
                            float radius, float floorClearance) {
        if (!pointAllowed(geometry, x, y, z, radius, floorClearance)) return false;
        // Sample the sphere footprint as well as its centre. This also protects
        // parallel movement near a wall and catches diagonal cave corners.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                float offset = dx != 0 && dz != 0 ? radius * DIAGONAL : radius;
                if (!pointAllowed(geometry, x + dx * offset, y, z + dz * offset,
                        radius, floorClearance)) return false;
            }
        }
        // A circle can touch a solid tile between the eight radial samples.
        // Check every overlapping tile at its closest point as well.
        for (int tileX = tile(x - radius); tileX <= tile(x + radius); tileX++) {
            for (int tileZ = tile(z - radius); tileZ <= tile(z + radius); tileZ++) {
                float closestX = Math.max(tileX * 4.0f + EPSILON,
                        Math.min((tileX + 1) * 4.0f - EPSILON, x));
                float closestZ = Math.max(tileZ * 4.0f + EPSILON,
                        Math.min((tileZ + 1) * 4.0f - EPSILON, z));
                float dx = closestX - x, dz = closestZ - z;
                if (dx * dx + dz * dz <= radius * radius
                        && !pointAllowed(geometry, closestX, y, closestZ, radius, floorClearance))
                    return false;
            }
        }
        return true;
    }

    static float clampHeight(Geometry geometry, float x, float y, float z,
                             float radius, float floorClearance) {
        float surface = geometry.surface(x, z);
        float clearance = Math.max(radius, floorClearance);
        if (finite(surface) && y >= surface + clearance) return y;
        int tileX = tile(x), tileZ = tile(z);
        if (geometry.isLoaded(x, z) && geometry.isOpen(tileX, tileZ)) {
            float floor = geometry.floor(x, z);
            if (!finite(floor)) return y;
            float minimum = floor + clearance;
            if (geometry.isExit(tileX, tileZ)) return Math.max(y, minimum);
            float ceiling = geometry.ceiling(x, z);
            if (!finite(ceiling)) return Math.max(y, minimum);
            float maximum = ceiling - radius;
            if (minimum > maximum) return (floor + ceiling) * 0.5f;
            return Math.max(minimum, Math.min(maximum, y));
        }
        return finite(surface) ? Math.max(y, surface + clearance) : y;
    }

    private static boolean containsAt(Geometry geometry, CameraIntent path, float distance,
                                      float radius, float clearance) {
        float fraction = distance / path.rayLength;
        return contains(geometry,
                lerp(path.anchorX, path.cameraX, fraction),
                lerp(path.anchorY, path.cameraY, fraction),
                lerp(path.anchorZ, path.cameraZ, fraction), radius, clearance);
    }

    private static boolean pointAllowed(Geometry geometry, float x, float y, float z,
                                        float radius, float clearance) {
        float surface = geometry.surface(x, z);
        if (finite(surface) && y >= surface + clearance) return true;
        if (!geometry.isLoaded(x, z)) return false;
        int tileX = tile(x), tileZ = tile(z);
        if (!geometry.isOpen(tileX, tileZ)) return false;
        float floor = geometry.floor(x, z);
        if (!finite(floor) || y < floor + clearance) return false;
        // Wurm does not render a ceiling on TILE_CAVE_EXIT. Neighbouring cave
        // tiles keep their ceiling and solid side walls; the floor still applies.
        if (geometry.isExit(tileX, tileZ)) return true;
        float ceiling = geometry.ceiling(x, z);
        return finite(ceiling) && y <= ceiling - radius;
    }

    private static int tile(float coordinate) { return (int) Math.floor(coordinate / 4.0f); }
    private static boolean finite(float value) { return !Float.isNaN(value) && !Float.isInfinite(value); }
    private static float lerp(float a, float b, float fraction) { return a + (b - a) * fraction; }
}
