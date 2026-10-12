package org.wuthirdpersonview.client;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.game.World;
import com.wurmonline.client.options.Options;
import com.wurmonline.client.renderer.ItemPlacer;
import com.wurmonline.client.renderer.WorldRender;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.model.collada.ColladaModelData;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;
import org.gotti.wurmunlimited.modsupport.console.ConsoleListener;
import org.gotti.wurmunlimited.modsupport.console.ModConsole;
import org.lwjgl.input.Keyboard;
import org.wuthirdpersonview.core.CameraConfig;
import org.wuthirdpersonview.core.CameraIntent;
import org.wuthirdpersonview.core.CameraPose;
import org.wuthirdpersonview.core.CameraState;

public final class WUThirdPersonViewMod
        implements org.gotti.wurmunlimited.modloader.interfaces.ModListener, WurmClientMod, Configurable, PreInitable, Initable, ConsoleListener {
    public static final String VERSION = "0.5.0";
    private static final String[][] KEYBINDER_COMMAND_CATALOG = {
            {"Toggle third-person view", "tp toggle"},
            {"Enable third-person view", "tp on"},
            {"Disable third-person view", "tp off"},
            {"Zoom in", "tp zoom-in"},
            {"Zoom out", "tp zoom-out"},
            {"Minimum third-person distance", "tp zoom 0"},
            {"Close camera (25%)", "tp zoom 0.25"},
            {"Medium camera (50%)", "tp zoom 0.50"},
            {"Far camera (75%)", "tp zoom 0.75"},
            {"Maximum camera distance", "tp zoom 1.0"},
            {"Reset orbit", "tp reset"},
            {"Hold free orbit", "tp orbit-hold"},
            {"Start free orbit", "tp orbit-on"},
            {"Stop free orbit", "tp orbit-off"},
            {"Toggle flying freelook", "tp freelook-toggle"},
            {"Enable flying freelook", "tp freelook-on"},
            {"Disable flying freelook", "tp freelook-off"},
            {"Show camera status", "tp status"},
            {"Enable camera debug", "tp debug on"},
            {"Disable camera debug", "tp debug off"}
    };
    private static final Logger LOG = Logger.getLogger(WUThirdPersonViewMod.class.getName());
    private static volatile WUThirdPersonViewMod instance;
    private static Field mouseSensitivityField;

    private CameraConfig config;
    private CameraState state;
    private CameraCollisionProbe collision;
    private CameraFieldAccess cameraFields;
    private PlayerHeadAnimator headAnimator;
    private volatile FreeLookModeIndicator freeLookIndicator;
    private final CameraStateStore stateStore = new CameraStateStore();
    private final KeybinderBridge keybinder = new KeybinderBridge();
    private int orbitKeyCode = -1;
    private int activeOrbitKeyCode = -1;
    private World lastWorld;
    private long lastDebugLog;
    private boolean runtimeReady;
    private boolean runtimeInitializationFailed;
    private boolean floorClampFailureLogged;
    private WorldRender freeMoveRenderer;
    private float freeMoveX, freeMoveY, freeMoveZ;
    private volatile boolean indicatorRenderFailureLogged;

    public WUThirdPersonViewMod() {
        instance = this;
    }

    static WUThirdPersonViewMod instanceForRenderFocus() { return instance; }

    boolean isThirdPersonActiveForRenderFocus() {
        return state != null && state.isThirdPersonActive();
    }

    float zoomForRenderFocus() { return state == null ? 0.0f : state.getZoom(); }

    @Override
    public String getVersion() { return VERSION; }

    /**
     * Optional, dependency-free discovery contract consumed by compatible
     * Keybinder versions. Each row is a display label followed by one complete
     * console command; Keybinder must not add source or target arguments.
     */
    public static String[][] getKeybinderCommandCatalog() {
        String[][] copy = new String[KEYBINDER_COMMAND_CATALOG.length][2];
        for (int i = 0; i < KEYBINDER_COMMAND_CATALOG.length; i++) {
            copy[i][0] = KEYBINDER_COMMAND_CATALOG[i][0];
            copy[i][1] = KEYBINDER_COMMAND_CATALOG[i][1];
        }
        return copy;
    }

    @Override
    public synchronized void configure(Properties properties) {
        properties = org.chamomilo.wurm.update.SharedModSettings.configure(
                "wu-third-person-view", properties, ModSettingsSchema.specs(), this::applyUserSettings);
        config = CameraConfig.from(properties);
        state = new CameraState(config);
        stateStore.load(state);
        orbitKeyCode = "NONE".equals(config.orbitKey) ? -1 : Keyboard.getKeyIndex(config.orbitKey);
        if (orbitKeyCode <= 0 && !"NONE".equals(config.orbitKey))
            LOG.warning("Unknown orbitKey '" + config.orbitKey + "'; orbit hold is disabled");
    }

    @Override
    public void preInit() {
        ClientHooks.exposeModClasses();
        org.chamomilo.wurm.update.SharedUpdateHooks.install();
        ensureConfigured();
        try {
            // Keep preInit bytecode-only. Loading any corresponding runtime class
            // here freezes it and prevents later mods from adding their hooks.
            ClientHooks.install();
            LOG.info("WU-third_person_view " + VERSION + " hooks installed");
        } catch (Exception failure) {
            LOG.log(Level.SEVERE, "Could not install WU-third_person_view hooks", failure);
            throw new RuntimeException("Could not install WU-third_person_view hooks", failure);
        }
    }

    @Override
    public void init() {
        org.chamomilo.wurm.update.SharedUpdateHooks.registerHost("wu-third-person-view");
        ensureConfigured();
        LOG.info("WU-third_person_view armed; runtime initialization is deferred");
    }

    private synchronized boolean ensureRuntimeInitialized() {
        if (runtimeReady) return true;
        if (runtimeInitializationFailed) return false;
        try {
            // The first rendered world frame happens after every mod has completed
            // preInit and init, including legacy mods that still add hooks in init.
            cameraFields = new CameraFieldAccess();
            mouseSensitivityField = PlayerObj.class.getDeclaredField("mouseSensitivity");
            mouseSensitivityField.setAccessible(true);
            collision = new CameraCollisionProbe(config);
            headAnimator = new PlayerHeadAnimator(config);
            freeLookIndicator = new FreeLookModeIndicator();
            ModConsole.addConsoleListener(this);
            runtimeReady = true;
            LOG.info("WU-third_person_view ready; Keybinder="
                    + (keybinder.isPresent() ? "detected" : "not detected"));
            return true;
        } catch (Throwable failure) {
            runtimeInitializationFailed = true;
            LOG.log(Level.SEVERE, "Could not initialize WU-third_person_view runtime access",
                    failure);
            return false;
        }
    }

    private synchronized void applyUserSettings(String key, Properties values) {
        double minimum = Double.parseDouble(values.getProperty("minDistance"));
        double maximum = Double.parseDouble(values.getProperty("maxDistance"));
        double enter = Double.parseDouble(values.getProperty("firstPersonEnterDistance"));
        double exit = Double.parseDouble(values.getProperty("firstPersonExitDistance"));
        if (maximum + 1e-9 < minimum + 0.1)
            throw new IllegalArgumentException("Maximum distance must exceed minimum distance by at least 0.1 m.");
        if (exit + 1e-9 < enter + 0.05)
            throw new IllegalArgumentException("Exit distance must exceed enter distance by at least 0.05 m.");
        String orbit = values.getProperty("orbitKey").toUpperCase(Locale.ROOT);
        int code = "NONE".equals(orbit) ? -1 : Keyboard.getKeyIndex(orbit);
        if (!"NONE".equals(orbit) && code <= 0) throw new IllegalArgumentException("Unknown orbit key: " + orbit);
        CameraConfig next = CameraConfig.from(values);
        if (headAnimator != null) headAnimator.deactivate();
        config = next;
        state.reconfigure(next);
        if (key.equals("enabled")) state.setThirdPersonActive(next.enabled);
        if (key.equals("debug")) state.setDebug(next.debug);
        if (key.equals("defaultZoom")) {
            boolean active = state.isThirdPersonActive();
            state.setZoom(next.defaultZoom);
            state.setThirdPersonActive(active);
        }
        if (key.equals("enabled") || key.equals("defaultZoom")) stateStore.markDirty();
        orbitKeyCode = code;
        activeOrbitKeyCode = -1;
        if (runtimeReady) {
            collision = new CameraCollisionProbe(next);
            headAnimator = new PlayerHeadAnimator(next);
        }
    }

    private synchronized void ensureConfigured() {
        if (config == null) configure(new Properties());
    }

    public static void beforeCalculateCamera(WorldRender render) {
        WUThirdPersonViewMod mod = instance;
        if (mod == null || !render.isFreeCamera()) return;
        mod.freeMoveRenderer = render;
        mod.freeMoveX = render.getCameraX();
        mod.freeMoveY = render.getCameraY();
        mod.freeMoveZ = render.getCameraZ();
    }

    public static void afterCalculateCamera(WorldRender render) {
        WUThirdPersonViewMod mod = instance;
        if (mod != null && mod.ensureRuntimeInitialized()) mod.updateCamera(render);
    }

    /** Paints outside the HUD component list so the marker is never clickable. */
    public static void renderFreeLookIndicator(Queue queue) {
        WUThirdPersonViewMod mod = instance;
        FreeLookModeIndicator indicator = mod == null ? null : mod.freeLookIndicator;
        if (indicator == null || queue == null) return;
        try {
            indicator.render(queue, 1.0f);
        } catch (Throwable failure) {
            if (!mod.indicatorRenderFailureLogged) {
                mod.indicatorRenderFailureLogged = true;
                LOG.log(Level.WARNING, "Could not render free-look HUD marker", failure);
            }
        }
    }

    private synchronized void updateCamera(WorldRender render) {
        if (state == null || cameraFields == null || render == null) return;
        World world = render.getWorld();
        if (world == null || world.getPlayer() == null) return;
        lastWorld = world;
        float dt = world.getFrameDelta();
        freeLookIndicator.update(world.getHud(), render.isFreeCamera());
        if (render.isFreeCamera()) {
            constrainFreelookMovement(render, world);
            constrainCameraHeight(render, world);
            stateStore.saveIfDue(state);
            return;
        }
        state.beginFrame(dt);
        // calculateCamera has already resolved the animated model's NullCamera0
        // (and mount carrier offset) into the native camera position. Orbit that
        // true eye point instead of playerH, which is only the support surface.
        CameraIntent intent = state.intent(render.getCameraX(),
                render.getCameraY() + config.headAnchorVerticalOffset,
                render.getCameraZ(), world.getPlayerRotX(), world.getPlayerRotY());
        if (intent == null) {
            headAnimator.deactivate();
            constrainCameraHeight(render, world);
            stateStore.saveIfDue(state);
            return;
        }
        headAnimator.update(world.getPlayer(), world.getPlayerRotX(),
                world.getPlayerRotY(), dt);
        float safe = collision.safeDistance(world, intent);
        CameraPose pose = state.resolve(intent, safe, dt);
        if (pose.overrideCamera) {
            try {
                cameraFields.apply(render, pose);
            } catch (IllegalAccessException failure) {
                LOG.log(Level.WARNING, "Could not apply third-person camera", failure);
            }
        }
        constrainCameraHeight(render, world);
        stateStore.saveIfDue(state);
        debugLog(world, pose);
    }

    private void constrainCameraHeight(WorldRender render, World world) {
        try {
            float height = collision.constrainedHeight(world, render.getCameraX(),
                    render.getCameraY(), render.getCameraZ());
            cameraFields.moveTo(render, render.getCameraX(), height, render.getCameraZ());
        } catch (RuntimeException | IllegalAccessException failure) {
            if (!floorClampFailureLogged) {
                floorClampFailureLogged = true;
                LOG.log(Level.WARNING, "Could not constrain camera floor/ceiling clearance", failure);
            }
        }
    }

    private void constrainFreelookMovement(WorldRender render, World world) {
        if (freeMoveRenderer != render) return;
        CameraIntent movement = CameraIntent.between(freeMoveX, freeMoveY, freeMoveZ,
                render.getCameraX(), render.getCameraY(), render.getCameraZ());
        float safe = collision.geometryDistance(world, movement);
        float fraction = movement.rayLength <= 0.0001f ? 0.0f : safe / movement.rayLength;
        try {
            cameraFields.moveTo(render,
                    freeMoveX + (movement.cameraX - freeMoveX) * fraction,
                    freeMoveY + (movement.cameraY - freeMoveY) * fraction,
                    freeMoveZ + (movement.cameraZ - freeMoveZ) * fraction);
        } catch (IllegalAccessException failure) {
            if (!floorClampFailureLogged) {
                floorClampFailureLogged = true;
                LOG.log(Level.WARNING, "Could not constrain flying camera movement", failure);
            }
        }
        freeMoveRenderer = null;
    }

    private void debugLog(World world, CameraPose pose) {
        if (!state.isDebug()) return;
        long now = System.currentTimeMillis();
        if (now - lastDebugLog < 1000L) return;
        lastDebugLog = now;
        LOG.info(String.format(Locale.ROOT,
                "camera zoom=%.2f actual=%.2f desired=%.2f collision=%s fp=%s backend=%s",
                state.getZoom(), pose.actualDistance, pose.desiredDistance,
                pose.collisionLimited, pose.collisionFirstPerson, collision.backend(world)));
    }

    public static boolean handleMouseWheel(World world, int x, int y, int delta) {
        WUThirdPersonViewMod mod = instance;
        return mod != null && mod.mouseWheel(world, x, y, delta);
    }

    private synchronized boolean mouseWheel(World world, int x, int y, int delta) {
        if (state == null || world == null || delta == 0) return false;
        HeadsUpDisplay hud = world.getHud();
        try {
            if (hud == null || hud.getComponentAt(x, y) != null) return false;
        } catch (Throwable failedOpen) {
            return false;
        }
        WorldRender renderer = world.getWorldRenderer();
        if (renderer != null && renderer.isFreeCamera()) return false;
        ItemPlacer placer = renderer == null ? null : renderer.getItemPlacer();
        if (placer != null && placer.isActive()) return false;

        String wheel = delta < 0 ? "MOUSE_WHEEL_UP" : "MOUSE_WHEEL_DOWN";
        if ("keybinder".equals(config.inputProvider)
                || ("auto".equals(config.inputProvider) && keybinder.isChordManaged(hud, wheel)))
            return false;

        if (delta < 0) state.zoomIn();
        else state.zoomOut();
        stateStore.markDirty();
        return true;
    }

    public static void onKeyEvent(int key, boolean pressed) {
        WUThirdPersonViewMod mod = instance;
        if (mod != null) mod.keyEvent(key, pressed);
    }

    private synchronized void keyEvent(int key, boolean pressed) {
        if (state == null) return;
        if (!pressed) {
            if (key == activeOrbitKeyCode) {
                state.setOrbitHeld(false);
                activeOrbitKeyCode = -1;
            }
            return;
        }
        HeadsUpDisplay hud = lastWorld == null ? null : lastWorld.getHud();
        boolean allowManaged = !"native".equals(config.inputProvider);
        boolean managed = allowManaged && keybinder.isOrbitChord(hud,
                Keyboard.getKeyName(key));
        if (managed) {
            activeOrbitKeyCode = key;
            state.setOrbitHeld(true);
            return;
        }
        boolean allowFallback = "native".equals(config.inputProvider)
                || ("auto".equals(config.inputProvider) && !keybinder.hasOrbitBinding());
        if (allowFallback && orbitKeyCode > 0 && key == orbitKeyCode) {
            activeOrbitKeyCode = key;
            state.setOrbitHeld(true);
        }
    }

    public static void releaseAllInput() {
        WUThirdPersonViewMod mod = instance;
        if (mod != null && mod.state != null) {
            mod.state.setOrbitHeld(false);
            mod.activeOrbitKeyCode = -1;
        }
    }

    public static boolean interceptTurn(PlayerObj player, int dx, int dy,
                                        boolean freeLookSensitivity) {
        WUThirdPersonViewMod mod = instance;
        return mod != null && mod.turn(player, dx, dy, freeLookSensitivity);
    }

    private synchronized boolean turn(PlayerObj player, int dx, int dy,
                                      boolean freeLookSensitivity) {
        if (state == null || !state.isThirdPersonActive() || !state.isOrbitHeld()
                || state.getLastPose().collisionFirstPerson) return false;
        float sensitivity = 1.0f;
        try {
            sensitivity = mouseSensitivityField == null ? sensitivity
                    : mouseSensitivityField.getFloat(player);
            if (freeLookSensitivity) sensitivity = Options.freeLookModeSensitivity.value();
            if (player.isUsingBinoculars()) sensitivity *= 0.2f;
        } catch (Throwable ignored) { }
        int adjustedY = Options.inverseMouse.value() ? -dy : dy;
        float scale = sensitivity * config.orbitSensitivity / 20.0f;
        state.addOrbitInput(dx * scale, adjustedY * scale);
        return true;
    }

    public static boolean suppressAnimationCamera() {
        WUThirdPersonViewMod mod = instance;
        return mod != null && mod.state != null && mod.state.getLastPose().overrideCamera;
    }

    public static float renderedPlayerBodyYaw(float nativeYawDegrees) {
        WUThirdPersonViewMod mod = instance;
        return mod == null || mod.headAnimator == null ? nativeYawDegrees
                : mod.headAnimator.renderedBodyYaw(nativeYawDegrees);
    }

    public static void beforePlayerSkinning(ColladaModelData modelData) {
        WUThirdPersonViewMod mod = instance;
        if (mod == null) return;
        if (mod.headAnimator != null) mod.headAnimator.applyBeforeSkinning(modelData);
    }

    @Override
    public synchronized boolean handleInput(String input, Boolean silent) {
        if (input == null) return false;
        String command = input.trim();
        if (command.startsWith("/")) command = command.substring(1);
        if (!command.toLowerCase(Locale.ROOT).startsWith("tp")) return false;
        if (command.length() > 2 && !Character.isWhitespace(command.charAt(2))) return false;
        String[] parts = command.isEmpty() ? new String[0] : command.split("\\s+");
        if (parts.length == 1) {
            output("Usage: tp toggle|on|off|zoom-in|zoom-out|zoom <0..1>|reset|"
                    + "orbit-on|orbit-off|freelook-toggle|freelook-on|freelook-off|"
                    + "status");
            return true;
        }
        try {
            String action = parts[1].toLowerCase(Locale.ROOT);
            if ("toggle".equals(action)) state.toggle();
            else if ("on".equals(action)) state.setThirdPersonActive(true);
            else if ("off".equals(action)) state.setThirdPersonActive(false);
            else if ("zoom-in".equals(action)) {
                if (canAdjustZoom()) state.zoomIn();
            } else if ("zoom-out".equals(action)) {
                if (canAdjustZoom()) state.zoomOut();
            } else if ("zoom".equals(action) && parts.length >= 3) {
                if (canAdjustZoom()) state.setZoom(Float.parseFloat(parts[2]));
            }
            else if ("reset".equals(action)) state.resetOrbit();
            else if ("orbit-on".equals(action)) state.setOrbitHeld(true);
            else if ("orbit-hold".equals(action)) state.setOrbitHeld(true);
            else if ("orbit-off".equals(action)) state.setOrbitHeld(false);
            else if ("freelook-toggle".equals(action)) toggleFreelook();
            else if ("freelook-on".equals(action)) setFreelook(true);
            else if ("freelook-off".equals(action)) setFreelook(false);
            else if ("debug".equals(action) && parts.length >= 3)
                state.setDebug("on".equalsIgnoreCase(parts[2]));
            else if ("status".equals(action)) {
                CameraPose pose = state.getLastPose();
                output(String.format(Locale.ROOT,
                        "3rd person: active=%s zoom=%.2f distance=%.2f "
                                + "orbit=%s freelook=%s keybinder=%s collision=%s",
                        state.isThirdPersonActive(), state.getZoom(), pose.actualDistance,
                        state.isOrbitHeld(), isFreelook(), keybinder.isPresent(),
                        collision.backend(lastWorld)));
                return true;
            } else {
                output("Unknown tp command: " + command);
                return true;
            }
            stateStore.markDirty();
            return true;
        } catch (RuntimeException invalid) {
            output("Invalid tp command: " + command);
            return true;
        }
    }

    private void toggleFreelook() {
        WorldRender renderer = lastWorld == null ? null : lastWorld.getWorldRenderer();
        if (renderer == null) {
            output("Flying freelook is unavailable before entering the world");
            return;
        }
        renderer.toggleFreeCamera();
        state.setOrbitHeld(false);
        activeOrbitKeyCode = -1;
        output("Flying freelook " + (renderer.isFreeCamera() ? "enabled" : "disabled"));
    }

    private void setFreelook(boolean enabled) {
        WorldRender renderer = lastWorld == null ? null : lastWorld.getWorldRenderer();
        if (renderer == null) {
            output("Flying freelook is unavailable before entering the world");
            return;
        }
        if (renderer.isFreeCamera() != enabled) renderer.toggleFreeCamera();
        state.setOrbitHeld(false);
        activeOrbitKeyCode = -1;
        output("Flying freelook " + (enabled ? "enabled" : "disabled"));
    }

    private boolean isFreelook() {
        WorldRender renderer = lastWorld == null ? null : lastWorld.getWorldRenderer();
        return renderer != null && renderer.isFreeCamera();
    }

    private boolean canAdjustZoom() {
        if (lastWorld == null || lastWorld.getWorldRenderer() == null) return true;
        ItemPlacer placer = lastWorld.getWorldRenderer().getItemPlacer();
        return placer == null || !placer.isActive();
    }

    private void output(String message) {
        try {
            if (lastWorld != null && lastWorld.getHud() != null)
                lastWorld.getHud().consoleOutput(message);
            else LOG.info(message);
        } catch (Throwable unavailable) {
            LOG.info(message);
        }
    }

    @Override public void modInitialized(org.gotti.wurmunlimited.modloader.interfaces.ModEntry<?> entry) {
        org.chamomilo.wurm.update.SharedUpdateCoordinator.modInitialized(entry);
    }}
