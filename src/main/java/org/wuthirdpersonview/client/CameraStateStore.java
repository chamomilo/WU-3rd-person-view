package org.wuthirdpersonview.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wuthirdpersonview.core.CameraState;

final class CameraStateStore {
    private static final Logger LOG = Logger.getLogger(CameraStateStore.class.getName());
    private static final long SAVE_DELAY_NANOS = 750_000_000L;
    private final Path path;
    private boolean dirty;
    private long dirtySince;

    CameraStateStore() {
        this(Paths.get("mods", "WU-third_person_view", "state.properties"));
    }

    CameraStateStore(Path path) {
        this.path = path;
    }

    void load(CameraState state) {
        if (!Files.isRegularFile(path)) return;
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            values.load(input);
            state.setZoom(parse(values.getProperty("zoom"), state.getZoom(), 0.0f, 1.0f));
            state.setThirdPersonActive(Boolean.parseBoolean(values.getProperty("active", "true")));
        } catch (Exception failure) {
            LOG.log(Level.WARNING, "Could not load camera state", failure);
        }
    }

    void markDirty() {
        dirty = true;
        dirtySince = System.nanoTime();
    }

    void saveIfDue(CameraState state) {
        if (dirty && System.nanoTime() - dirtySince >= SAVE_DELAY_NANOS)
            save(state);
    }

    void save(CameraState state) {
        Properties values = new Properties();
        values.setProperty("active", Boolean.toString(state.isThirdPersonActive()));
        values.setProperty("zoom", Float.toString(state.getZoom()));
        Path temp = path.resolveSibling(path.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            try (OutputStream output = Files.newOutputStream(temp)) {
                values.store(output, "WU-third_person_view state");
            }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (Exception failure) {
            LOG.log(Level.WARNING, "Could not save camera state", failure);
            try { Files.deleteIfExists(temp); } catch (Exception ignored) { }
        }
    }

    private static float parse(String value, float fallback, float min, float max) {
        try { return Math.max(min, Math.min(max, Float.parseFloat(value))); }
        catch (Exception ignored) { return fallback; }
    }
}
