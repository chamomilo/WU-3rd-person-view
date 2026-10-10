package org.wuthirdpersonview.client;

import com.wurmonline.client.game.World;
import com.wurmonline.client.game.CaveDataBuffer;
import com.wurmonline.client.game.NearTerrainDataBuffer;
import com.wurmonline.client.renderer.WorldRender;
import com.wurmonline.mesh.Tiles;
import org.wuthirdpersonview.core.RenderFocusPolicy;
import org.wuthirdpersonview.core.RenderLayerPolicy;

/** Supplies a camera-aware reference point to Wurm's visual render subsystems. */
public final class RenderFocus {
    private RenderFocus() { }

    public static float x(World world, float playerX) {
        WorldRender renderer = renderer(world);
        return renderer == null ? playerX
                : mix(playerX, renderer.getCameraX(), blend(renderer));
    }

    public static float y(World world, float playerY) {
        WorldRender renderer = renderer(world);
        return renderer == null ? playerY
                : mix(playerY, renderer.getCameraZ(), blend(renderer));
    }

    public static float height(World world, float playerHeight) {
        WorldRender renderer = renderer(world);
        return renderer == null ? playerHeight
                : mix(playerHeight, renderer.getCameraY(), blend(renderer));
    }

    public static int tileX(World world, int playerTileX) {
        WorldRender renderer = renderer(world);
        if (renderer == null) return playerTileX;
        float playerX = playerTileX * RenderFocusPolicy.METERS_PER_TILE;
        return tile(mix(playerX, renderer.getCameraX(), blend(renderer)));
    }

    public static int tileY(World world, int playerTileY) {
        WorldRender renderer = renderer(world);
        if (renderer == null) return playerTileY;
        float playerY = playerTileY * RenderFocusPolicy.METERS_PER_TILE;
        return tile(mix(playerY, renderer.getCameraZ(), blend(renderer)));
    }

    /** Used only by visual visibility/lighting decisions, never by world physics. */
    public static int layer(World world, int playerLayer) {
        WorldRender renderer = renderer(world);
        if (!cameraOwnsView(renderer)) return playerLayer;
        float x = renderer.getCameraX();
        float y = renderer.getCameraY();
        float z = renderer.getCameraZ();
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return playerLayer;
        float surface = Float.NaN;
        float floor = Float.NaN;
        float ceiling = Float.NaN;
        boolean exit = false;
        try {
            NearTerrainDataBuffer terrain = world.getNearTerrainBuffer();
            if (terrain != null && terrain.isValid(x, z))
                surface = terrain.getInterpolatedHeight(x, z);
            if (Float.isFinite(surface) && y >= surface) return 0;
            CaveDataBuffer cave = world.getCaveBuffer();
            if (CaveBufferAccess.isLoaded(cave, x, z)) {
                Tiles.Tile type = cave.getTileType(tile(x), tile(z));
                if (!Tiles.isSolidCave(type)) {
                    exit = type == Tiles.Tile.TILE_CAVE_EXIT;
                    floor = cave.getInterpolatedFloor(x, z);
                    if (!exit) ceiling = cave.getInterpolatedCeiling(x, z);
                }
            }
        } catch (RuntimeException outsideLoadedGeometry) {
            // A buffer can move between frames; retain the native layer if unknown.
        }
        return RenderLayerPolicy.layer(playerLayer, y, surface, floor, ceiling, exit);
    }

    public static boolean onCaveEntrance(WorldRender renderer, boolean playerOnEntrance) {
        if (!cameraOwnsView(renderer)) return playerOnEntrance;
        float x = renderer.getCameraX();
        float z = renderer.getCameraZ();
        if (!Float.isFinite(x) || !Float.isFinite(z)) return playerOnEntrance;
        try {
            CaveDataBuffer cave = renderer.getWorld().getCaveBuffer();
            if (CaveBufferAccess.isLoaded(cave, x, z))
                return cave.getTileType(tile(x), tile(z)) == Tiles.Tile.TILE_CAVE_EXIT;
        } catch (RuntimeException outsideLoadedGeometry) {
            // Preserve Wurm's entrance fallback when the camera tile is not loaded.
        }
        return playerOnEntrance;
    }

    public static boolean cameraOwnsView(WorldRender renderer) {
        if (renderer == null) return false;
        WUThirdPersonViewMod mod = WUThirdPersonViewMod.instanceForRenderFocus();
        return mod != null && (renderer.isFreeCamera() || mod.isThirdPersonActiveForRenderFocus());
    }

    private static WorldRender renderer(World world) {
        return world == null ? null : world.getWorldRenderer();
    }

    private static float blend(WorldRender renderer) {
        WUThirdPersonViewMod mod = WUThirdPersonViewMod.instanceForRenderFocus();
        if (mod == null) return 0.0f;
        return RenderFocusPolicy.blend(renderer.isFreeCamera(),
                mod.isThirdPersonActiveForRenderFocus(), mod.zoomForRenderFocus());
    }

    private static float mix(float player, float camera, float amount) {
        if (!Float.isFinite(camera)) return player;
        return player + (camera - player) * amount;
    }

    private static int tile(float metres) {
        return (int) Math.floor(metres / RenderFocusPolicy.METERS_PER_TILE);
    }
}
