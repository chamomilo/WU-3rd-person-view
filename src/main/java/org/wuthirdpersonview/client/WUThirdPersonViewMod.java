package org.wuthirdpersonview.client;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.game.World;
import com.wurmonline.client.options.Options;
import com.wurmonline.client.renderer.ItemPlacer;
import com.wurmonline.client.renderer.WorldRender;
import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.cell.AttachedCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.model.collada.ColladaModelData;
import java.lang.reflect.Field;
import java.util.Map;
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
import org.wuthirdpersonview.core.HolsterSnapshot;
import org.wuthirdpersonview.core.HolsterState;

public final class WUThirdPersonViewMod
        implements org.gotti.wurmunlimited.modloader.interfaces.ModListener, WurmClientMod, Configurable, PreInitable, Initable, ConsoleListener {
    public static final String VERSION = "0.3.9";
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
            {"Toggle holster", "tp holster-toggle"},
            {"Holster equipment", "tp holster-on"},
            {"Draw equipment", "tp holster-off"},
            {"Show holster status", "tp holster-status"},
            {"Open Holster Studio", "tp holster-studio"},
            {"Show camera status", "tp status"},
            {"Enable camera debug", "tp debug on"},
            {"Disable camera debug", "tp debug off"}
    };
    private static final Logger LOG = Logger.getLogger(WUThirdPersonViewMod.class.getName());
    private static volatile WUThirdPersonViewMod instance;
    private static Field mouseSensitivityField;

    private CameraConfig config;
    private CameraState state;
    private HolsterState holsterState;
    private HolsterProfileStore holsterProfiles;
    private HolsterStudio holsterStudio;
    private CameraCollisionProbe collision;
    private CameraFieldAccess cameraFields;
    private PlayerHeadAnimator headAnimator;
    private HolsterArmAnimator holsterAnimator;
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
    private volatile boolean indicatorRenderFailureLogged;
    private volatile boolean holsterAnimationFailureLogged;
    private volatile boolean holsterStudioGizmoFailureLogged;

    public WUThirdPersonViewMod() {
        instance = this;
    }

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
        config = CameraConfig.from(properties);
        state = new CameraState(config);
        holsterState = new HolsterState(config.holsterDurationSeconds);
        holsterProfiles = new HolsterProfileStore();
        holsterProfiles.load();
        holsterStudio = new HolsterStudio(holsterProfiles);
        stateStore.load(state, holsterState);
        orbitKeyCode = "NONE".equals(config.orbitKey) ? -1 : Keyboard.getKeyIndex(config.orbitKey);
        if (orbitKeyCode <= 0 && !"NONE".equals(config.orbitKey))
            LOG.warning("Unknown orbitKey '" + config.orbitKey + "'; orbit hold is disabled");
    }

    @Override
    public void preInit() {
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
            holsterAnimator = new HolsterArmAnimator();
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

    private synchronized void ensureConfigured() {
        if (config == null) configure(new Properties());
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
        updateHolster(world.getPlayer(), dt);
        if (holsterStudio != null) holsterStudio.tick(world);
        freeLookIndicator.update(world.getHud(), render.isFreeCamera());
        if (render.isFreeCamera()) {
            clampCameraAboveFloor(render, world);
            stateStore.saveIfDue(state, holsterState);
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
            clampCameraAboveFloor(render, world);
            stateStore.saveIfDue(state, holsterState);
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
        clampCameraAboveFloor(render, world);
        stateStore.saveIfDue(state, holsterState);
        debugLog(world, pose);
    }

    private void updateHolster(PlayerObj player, float deltaSeconds) {
        if (holsterState == null || player == null) return;
        if (holsterState.updateTarget(player.hasTarget())) {
            stateStore.markDirty();
            output("Holster interrupted: target acquired; drawing equipment");
        }
        com.wurmonline.client.renderer.PlayerBodyRenderable body = player.getPlayerBody();
        boolean hasWeapon = body != null && (body.getEquipment(0) != null
                || body.getEquipment(1) != null);
        boolean hasShield = body != null && body.getEquipment(11) != null;
        holsterState.advance(deltaSeconds, hasWeapon, hasShield);
        if (holsterAnimator != null && !holsterAnimationFailureLogged)
            holsterAnimator.update(player, holsterState.snapshot());
    }

    private void clampCameraAboveFloor(WorldRender render, World world) {
        float minimumHeight = collision.minimumCameraHeight(world,
                render.getCameraX(), render.getCameraZ());
        try {
            cameraFields.raiseTo(render, minimumHeight);
        } catch (IllegalAccessException failure) {
            if (!floorClampFailureLogged) {
                floorClampFailureLogged = true;
                LOG.log(Level.WARNING, "Could not apply camera surface clearance", failure);
            }
        }
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
        if (mod.holsterAnimator != null && !mod.holsterAnimationFailureLogged) {
            try {
                mod.holsterAnimator.applyBeforeSkinning(modelData);
            } catch (Throwable failure) {
                mod.holsterAnimator.deactivate();
                mod.holsterAnimationFailureLogged = true;
                LOG.log(Level.WARNING,
                        "Holster arm animation failed; equipment transfer remains active",
                        failure);
            }
        }
    }

    public static HolsterSnapshot holsterSnapshot() {
        WUThirdPersonViewMod mod = instance;
        return mod == null || mod.holsterState == null ? null : mod.holsterState.snapshot();
    }

    static HolsterPlacement holsterPlacement(
            com.wurmonline.client.renderer.cell.AttachedCellRenderable attachment,
            org.wuthirdpersonview.core.HolsterItemClass type, int slot) {
        WUThirdPersonViewMod mod = instance;
        return mod == null || mod.holsterProfiles == null
                ? HolsterPlacement.forItem(type, slot)
                : mod.holsterProfiles.resolve(attachment, type, slot);
    }

    static void renderHolsterStudioGizmo(AttachedCellRenderable attachment, int slot,
                                         Queue queue, Matrix anchorMatrix,
                                         HolsterPlacement placement) {
        WUThirdPersonViewMod mod = instance;
        if (mod == null || mod.holsterStudio == null) return;
        try {
            mod.holsterStudio.renderGizmo(
                    attachment, slot, queue, anchorMatrix, placement);
        } catch (Throwable failure) {
            // A calibration aid must never disable equipment rendering.
            if (!mod.holsterStudioGizmoFailureLogged) {
                mod.holsterStudioGizmoFailureLogged = true;
                LOG.log(Level.WARNING, "Could not render Holster Studio axes", failure);
            }
        }
    }

    public static boolean handleHolsterStudioResponse(Map<?, ?> values, String button) {
        WUThirdPersonViewMod mod = instance;
        return mod != null && mod.holsterStudio != null
                && mod.holsterStudio.handleResponse(mod.lastWorld, values, button);
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
                    + "holster-toggle|holster-on|holster-off|holster-status|"
                    + "holster-studio|status");
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
            else if ("holster-toggle".equals(action)) toggleHolster();
            else if ("holster-on".equals(action)) setHolstered(true);
            else if ("holster-off".equals(action)) setHolstered(false);
            else if ("holster-studio".equals(action)) {
                openHolsterStudio();
                return true;
            }
            else if ("holster-status".equals(action)) {
                outputHolsterStatus();
                return true;
            }
            else if ("debug".equals(action) && parts.length >= 3)
                state.setDebug("on".equalsIgnoreCase(parts[2]));
            else if ("status".equals(action)) {
                CameraPose pose = state.getLastPose();
                output(String.format(Locale.ROOT,
                        "3rd person: active=%s zoom=%.2f distance=%.2f "
                                + "orbit=%s freelook=%s holster=%s keybinder=%s collision=%s",
                        state.isThirdPersonActive(), state.getZoom(), pose.actualDistance,
                        state.isOrbitHeld(), isFreelook(), holsterDescription(), keybinder.isPresent(),
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

    private void toggleHolster() {
        boolean target = hasTarget();
        if (!holsterState.toggle(target)) {
            output("Cannot holster equipment while a target is selected");
            return;
        }
        stateStore.markDirty();
        output(holsterState.isDesiredHolstered()
                ? "Holstering equipment" : "Drawing equipment");
    }

    private void openHolsterStudio() {
        if (lastWorld == null || lastWorld.getPlayer() == null) {
            output("Holster Studio is unavailable before entering the world");
            return;
        }
        if (hasTarget()) {
            output("Holster Studio is unavailable while a target is selected");
            return;
        }
        holsterState.setHolsteredImmediately(true);
        stateStore.markDirty();
        if (holsterStudio == null || !holsterStudio.start(lastWorld))
            output("Holster Studio requires a weapon, tool, or shield to be equipped");
    }

    private void setHolstered(boolean value) {
        if (!holsterState.requestHolstered(value, hasTarget())) {
            output("Cannot holster equipment while a target is selected");
            return;
        }
        stateStore.markDirty();
        output(value ? "Holstering equipment" : "Drawing equipment");
    }

    private boolean hasTarget() {
        return lastWorld != null && lastWorld.getPlayer() != null
                && lastWorld.getPlayer().hasTarget();
    }

    private void outputHolsterStatus() {
        HolsterSnapshot snapshot = holsterState.snapshot();
        output(String.format(Locale.ROOT,
                "Holster: requested=%s transitioning=%s weapon=%.2f shield=%.2f target=%s",
                snapshot.desiredHolstered, snapshot.isTransitioning(),
                snapshot.weaponAmount, snapshot.shieldAmount, snapshot.targetPresent));
    }

    private String holsterDescription() {
        HolsterSnapshot snapshot = holsterState.snapshot();
        if (snapshot.isTransitioning())
            return snapshot.desiredHolstered ? "holstering" : "drawing";
        return snapshot.desiredHolstered ? "holstered" : "drawn";
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
