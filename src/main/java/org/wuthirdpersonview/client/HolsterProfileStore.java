package org.wuthirdpersonview.client;

import com.wurmonline.client.renderer.cell.AttachedCellRenderable;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.resources.ResourceUrl;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wuthirdpersonview.core.HolsterItemClass;

/** Saved and temporary item-specific carry transforms used by Holster Studio. */
final class HolsterProfileStore {
    private static final Logger LOG = Logger.getLogger(HolsterProfileStore.class.getName());
    private static final String PREFIX = "profile.";

    private final Path path;
    private final Map<String, HolsterPlacement> saved =
            new HashMap<String, HolsterPlacement>();
    private final Map<String, HolsterPlacement> previews =
            new HashMap<String, HolsterPlacement>();

    HolsterProfileStore() {
        this(Paths.get("mods", "WU-third_person_view", "holster-profiles.properties"));
    }

    HolsterProfileStore(Path path) {
        this.path = path;
    }

    synchronized void load() {
        saved.clear();
        previews.clear();
        if (!Files.isRegularFile(path)) return;
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            values.load(input);
            for (String property : values.stringPropertyNames()) {
                if (!property.startsWith(PREFIX)) continue;
                String key = decode(property.substring(PREFIX.length()));
                HolsterPlacement placement = parse(values.getProperty(property));
                if (key != null && placement != null) saved.put(key, placement);
            }
        } catch (Exception failure) {
            LOG.log(Level.WARNING, "Could not load Holster Studio profiles", failure);
        }
    }

    synchronized HolsterPlacement resolve(AttachedCellRenderable attachment,
                                           HolsterItemClass type, int slot) {
        return resolve(profileKey(attachment, slot), type, slot);
    }

    synchronized HolsterPlacement resolve(String key, HolsterItemClass type, int slot) {
        HolsterPlacement placement = previews.get(key);
        if (placement == null) placement = saved.get(key);
        return placement == null ? HolsterPlacement.forItem(type, slot) : placement;
    }

    synchronized void preview(String key, HolsterPlacement placement) {
        if (key != null && placement != null) previews.put(key, placement);
    }

    synchronized void discardPreview(String key) {
        if (key != null) previews.remove(key);
    }

    synchronized boolean save(String key, HolsterPlacement placement) {
        if (key == null || placement == null) return false;
        saved.put(key, placement);
        previews.put(key, placement);
        return write();
    }

    synchronized boolean remove(String key) {
        if (key == null) return false;
        previews.remove(key);
        saved.remove(key);
        return write();
    }

    synchronized boolean hasSaved(String key) {
        return saved.containsKey(key);
    }

    static String profileKey(AttachedCellRenderable attachment, int slot) {
        String identity = modelIdentity(attachment);
        return identity == null ? null : identity + "#slot=" + slot;
    }

    static String displayName(AttachedCellRenderable attachment, int slot) {
        String name = attachment == null ? null : attachment.getItemName();
        if (name == null || name.trim().isEmpty()) name = modelIdentity(attachment);
        if (name == null || name.trim().isEmpty()) name = "Unknown item";
        return name + " (" + slotName(slot) + ")";
    }

    private static String modelIdentity(AttachedCellRenderable attachment) {
        ModelResourceWrapper wrapper = attachment == null ? null : attachment.getModelWrapper();
        ResourceUrl resource = wrapper == null ? null : wrapper.getResourceUrl();
        String value = resource == null ? null : resource.getFilePath();
        if (value == null || value.trim().isEmpty())
            value = attachment == null ? null : attachment.getItemName();
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim().toLowerCase(Locale.ENGLISH).replace('\\', '/');
    }

    private boolean write() {
        Properties values = new Properties();
        for (Map.Entry<String, HolsterPlacement> entry : saved.entrySet())
            values.setProperty(PREFIX + encode(entry.getKey()), format(entry.getValue()));
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        Path temp = path.resolveSibling(path.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            if (parent != null) Files.createDirectories(parent);
            try (OutputStream output = Files.newOutputStream(temp)) {
                values.store(output, "WU-third_person_view Holster Studio profiles");
            }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception failure) {
            LOG.log(Level.WARNING, "Could not save Holster Studio profiles", failure);
            try { Files.deleteIfExists(temp); } catch (Exception ignored) { }
            return false;
        }
    }

    private static String format(HolsterPlacement value) {
        return value.anchor + "," + number(value.x) + "," + number(value.y)
                + "," + number(value.z) + "," + number(value.modelX)
                + "," + number(value.modelY) + "," + number(value.modelZ)
                + "," + number(value.rotationX) + "," + number(value.rotationY)
                + "," + number(value.rotationZ) + "," + number(value.scale)
                + "," + number(value.transferClearance);
    }

    private static HolsterPlacement parse(String value) {
        if (value == null) return null;
        String[] fields = value.split(",", -1);
        if (fields.length != 12 || !validAnchor(fields[0])) return null;
        try {
            float scale = bounded(fields[10], 0.1f, 3.0f);
            float clearance = bounded(fields[11], 0.0f, 1.0f);
            HolsterPlacement base = HolsterPlacement.forItem(
                    HolsterItemClass.SHORT_BELT, HolsterPlacement.RIGHT_HAND_SLOT);
            return base.withAll(fields[0],
                    bounded(fields[1], -2.0f, 2.0f),
                    bounded(fields[2], -2.0f, 2.0f),
                    bounded(fields[3], -2.0f, 2.0f),
                    bounded(fields[4], -5.0f, 5.0f),
                    bounded(fields[5], -5.0f, 5.0f),
                    bounded(fields[6], -5.0f, 5.0f),
                    bounded(fields[7], -360.0f, 360.0f),
                    bounded(fields[8], -360.0f, 360.0f),
                    bounded(fields[9], -360.0f, 360.0f), scale, clearance);
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private static float bounded(String value, float minimum, float maximum) {
        float parsed = Float.parseFloat(value.trim());
        if (Float.isNaN(parsed) || Float.isInfinite(parsed))
            throw new NumberFormatException("non-finite value");
        return Math.max(minimum, Math.min(maximum, parsed));
    }

    private static boolean validAnchor(String value) {
        return "Spine".equals(value) || "SpineTop".equals(value);
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.5f", value);
    }

    private static String encode(String key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                key.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String key) {
        try {
            return new String(Base64.getUrlDecoder().decode(key), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static String slotName(int slot) {
        if (slot == HolsterPlacement.LEFT_HAND_SLOT) return "left hand";
        if (slot == HolsterPlacement.RIGHT_HAND_SLOT) return "right hand";
        return "shield";
    }
}
