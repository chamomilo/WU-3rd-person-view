package org.wuthirdpersonview.client;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Optional, reflection-only cooperation with Keybinder 0.7.x. */
final class KeybinderBridge {
    private static final Logger LOG = Logger.getLogger(KeybinderBridge.class.getName());
    private boolean discoveryAttempted;
    private long lastDiscoveryAttempt;
    private boolean present;
    private Field registryField;
    private Method findEnabledByChord;
    private Method snapshot;

    boolean isChordManaged(HeadsUpDisplay hud, String wheelName) {
        discover();
        if (!present || registryField == null || findEnabledByChord == null) return false;
        try {
            return findRecord(chord(hud, wheelName)) != null;
        } catch (ReflectiveOperationException failure) {
            LOG.log(Level.FINE, "Could not query Keybinder chord ownership", failure);
            return false;
        }
    }

    boolean isOrbitChord(HeadsUpDisplay hud, String keyName) {
        discover();
        if (!present) return false;
        try {
            Object record = findRecord(chord(hud, keyName));
            if (record == null) record = findRecord(keyName);
            return containsOrbitHold(record);
        } catch (ReflectiveOperationException failure) {
            LOG.log(Level.FINE, "Could not resolve Keybinder orbit chord", failure);
            return false;
        }
    }

    boolean hasOrbitBinding() {
        discover();
        if (!present || registryField == null || snapshot == null) return false;
        try {
            Object registry = registryField.get(null);
            if (registry == null) return false;
            List<?> records = (List<?>) snapshot.invoke(registry);
            for (Object record : records) {
                boolean enabled = (Boolean) record.getClass().getMethod("isEnabled").invoke(record);
                String key = (String) record.getClass().getMethod("getKey").invoke(record);
                if (enabled && key != null && !key.trim().isEmpty() && containsOrbitHold(record))
                    return true;
            }
        } catch (ReflectiveOperationException failure) {
            LOG.log(Level.FINE, "Could not inspect Keybinder orbit bindings", failure);
        }
        return false;
    }

    boolean isPresent() {
        discover();
        return present;
    }

    private void discover() {
        if (present) return;
        long now = System.nanoTime();
        if (discoveryAttempted && now - lastDiscoveryAttempt < 2_000_000_000L) return;
        discoveryAttempted = true;
        lastDiscoveryAttempt = now;
        registryField = null;
        findEnabledByChord = null;
        snapshot = null;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> mod = Class.forName("org.keybinder.wurm.KeybinderMod", false, loader);
            Class<?> registry = Class.forName("org.keybinder.wurm.KeybindRegistry", false, loader);
            registryField = mod.getDeclaredField("registry");
            registryField.setAccessible(true);
            findEnabledByChord = registry.getMethod("findEnabledByChord", String.class);
            snapshot = registry.getMethod("snapshot");
            present = true;
            LOG.info("Keybinder detected: managed chords will take precedence");
        } catch (Throwable absent) {
            present = false;
        }
    }

    private Object findRecord(String canonicalChord) throws ReflectiveOperationException {
        if (registryField == null || findEnabledByChord == null) return null;
        Object registry = registryField.get(null);
        return registry == null ? null : findEnabledByChord.invoke(registry, canonicalChord);
    }

    private static boolean containsOrbitHold(Object record) throws ReflectiveOperationException {
        if (record == null) return false;
        List<?> steps = (List<?>) record.getClass().getMethod("getKeybindSteps").invoke(record);
        for (Object step : steps) {
            if (!"org.keybinder.wurm.model.ConsoleCommandStep".equals(
                    step.getClass().getName())) continue;
            String command = (String) step.getClass().getMethod("getCommand").invoke(step);
            String normalized = command == null ? "" : command.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("/")) normalized = normalized.substring(1);
            if ("tp orbit-hold".equals(normalized)) return true;
        }
        return false;
    }

    private static String chord(HeadsUpDisplay hud, String wheelName) {
        StringBuilder value = new StringBuilder();
        if (hud != null && hud.isControlDown()) value.append("CTRL+");
        if (hud != null && hud.isShiftDown()) value.append("SHIFT+");
        if (hud != null && hud.isAltDown()) value.append("ALT+");
        return value.append(wheelName).toString();
    }
}
