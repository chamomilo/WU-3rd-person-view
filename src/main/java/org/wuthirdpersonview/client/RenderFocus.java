package org.wuthirdpersonview.client;

import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.WorldRender;
import org.wuthirdpersonview.core.RenderFocusPolicy;

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
