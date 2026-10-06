package org.wuthirdpersonview.client;

import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.PlayerBodyRenderable;
import com.wurmonline.client.renderer.cell.AttachedCellRenderable;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.gui.BmlWindowComponent;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.client.renderer.model.ModelDataBounds;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.wuthirdpersonview.core.HolsterItemClass;

/** Local BML editor for live item-to-skeleton carry transforms. */
final class HolsterStudio {
    static final short WINDOW_ID = 32760;
    static final String WINDOW_TITLE = "Holster Studio";
    static final String MARKER = "wu_third_person_holster_studio";
    private static final float POSITION_STEP = 0.02f;
    private static final float ROTATION_STEP = 5.0f;
    private static final int[] SLOTS = {
            HolsterPlacement.RIGHT_HAND_SLOT,
            HolsterPlacement.LEFT_HAND_SLOT,
            HolsterPlacement.SHIELD_SLOT
    };

    private final HolsterProfileStore profiles;
    private final Set<String> sessionKeys = new HashSet<String>();
    private boolean active;
    private boolean pendingOpen;
    private int selectedIndex;
    private Selection selection;
    private HolsterPlacement placement;
    private Field windowXField;
    private Field windowYField;
    private boolean windowFieldsResolved;
    private boolean hasWindowPosition;
    private int windowX;
    private int windowY;

    HolsterStudio(HolsterProfileStore profiles) {
        this.profiles = profiles;
    }

    boolean start(World world) {
        List<Selection> items = selections(world);
        if (items.isEmpty()) return false;
        active = true;
        pendingOpen = true;
        selectedIndex = 0;
        select(items.get(0));
        return true;
    }

    void tick(World world) {
        if (!active || world == null) return;
        HeadsUpDisplay hud = world.getHud();
        if (hud == null) return;
        if (hud.findBml(WINDOW_ID)) {
            rememberWindowPosition(hud);
            return;
        }
        if (!pendingOpen) return;
        List<Selection> items = selections(world);
        if (items.isEmpty()) {
            close();
            hud.consoleOutput("Holster Studio closed: no supported equipment is equipped");
            return;
        }
        Selection live = find(items, selection == null ? null : selection.key);
        if (live == null) {
            selectedIndex = Math.min(selectedIndex, items.size() - 1);
            select(items.get(selectedIndex));
        } else {
            selectedIndex = items.indexOf(live);
            selection = live;
        }
        pendingOpen = false;
        // Wurm names both String parameters identically at the bytecode level:
        // the first String is the title and the last String is the BML markup.
        hud.showBml(WINDOW_ID, WINDOW_TITLE, 570, 620, 0.5f, 0.5f,
                true, true, 0.16f, 0.16f, 0.16f, bml());
        restoreWindowPosition(hud);
    }

    boolean handleResponse(World world, Map<?, ?> values, String button) {
        if (values == null || !"1".equals(string(values.get(MARKER)))) return false;
        rememberWindowPosition(world == null ? null : world.getHud());
        if (!active) return true;
        String action = button == null ? "" : button.trim().toLowerCase(Locale.ENGLISH);
        if (action.isEmpty() || "close".equals(action)) {
            close();
            return true;
        }

        try {
            placement = fromResponse(values, placement);
            sessionKeys.add(selection.key);
            if (action.endsWith("_dec") || action.endsWith("_inc"))
                placement = adjust(placement, action);
            else if ("anchor".equals(action))
                placement = placement.withValues(
                        "SpineTop".equals(placement.anchor) ? "Spine" : "SpineTop",
                        placement.x, placement.y, placement.z,
                        placement.modelX, placement.modelY, placement.modelZ,
                        placement.rotationX, placement.rotationY, placement.rotationZ);
            else if ("defaults".equals(action))
                placement = HolsterPlacement.forItem(selection.type, selection.slot);
            else if ("remove".equals(action)) {
                profiles.remove(selection.key);
                placement = HolsterPlacement.forItem(selection.type, selection.slot);
                output(world, "Removed saved profile for " + selection.label);
            }

            profiles.preview(selection.key, placement);
            if ("save".equals(action)) {
                boolean saved = profiles.save(selection.key, placement);
                output(world, saved ? "Saved Holster profile for " + selection.label
                        : "Could not save the Holster profile; check client.log");
            }

            if ("previous".equals(action) || "next".equals(action)) {
                List<Selection> items = selections(world);
                if (!items.isEmpty()) {
                    int direction = "previous".equals(action) ? -1 : 1;
                    selectedIndex = (selectedIndex + direction + items.size()) % items.size();
                    select(items.get(selectedIndex));
                }
            }
        } catch (IllegalArgumentException invalid) {
            output(world, "Holster Studio: " + invalid.getMessage());
        }
        pendingOpen = true;
        return true;
    }

    void renderGizmo(AttachedCellRenderable attachment, int slot, Queue queue,
                     Matrix anchorMatrix, HolsterPlacement livePlacement) {
        if (!isSelected(attachment, slot) || queue == null || anchorMatrix == null
                || livePlacement == null) return;
        HolsterStudioGizmo.render(queue, anchorMatrix, livePlacement);
    }

    private boolean isSelected(AttachedCellRenderable attachment, int slot) {
        return active && selection != null && selection.slot == slot
                && selection.key.equals(HolsterProfileStore.profileKey(attachment, slot));
    }

    /** Captures the draggable BML window before Wurm removes it after a response. */
    private void rememberWindowPosition(HeadsUpDisplay hud) {
        if (hud == null) return;
        BmlWindowComponent window = hud.getBmlWindow(WINDOW_ID);
        if (window == null) return;
        resolveWindowFields();
        if (windowXField == null || windowYField == null) return;
        try {
            windowX = windowXField.getInt(window);
            windowY = windowYField.getInt(window);
            hasWindowPosition = true;
        } catch (ReflectiveOperationException unavailable) {
            windowXField = null;
            windowYField = null;
        }
    }

    private void restoreWindowPosition(HeadsUpDisplay hud) {
        if (!hasWindowPosition || hud == null) return;
        BmlWindowComponent window = hud.getBmlWindow(WINDOW_ID);
        if (window != null) window.setPosition(windowX, windowY);
    }

    private void resolveWindowFields() {
        if (windowFieldsResolved) return;
        windowFieldsResolved = true;
        windowXField = optionalField(WurmComponent.class, "x");
        windowYField = optionalField(WurmComponent.class, "y");
    }

    private static Field optionalField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException unavailable) {
            return null;
        }
    }

    private void select(Selection next) {
        selection = next;
        placement = profiles.resolve(next.key, next.type, next.slot);
        sessionKeys.add(next.key);
    }

    private void close() {
        for (String key : sessionKeys) profiles.discardPreview(key);
        sessionKeys.clear();
        active = false;
        pendingOpen = false;
        selection = null;
        placement = null;
    }

    private HolsterPlacement fromResponse(Map<?, ?> values, HolsterPlacement fallback) {
        if (fallback == null) throw new IllegalArgumentException("no item is selected");
        return fallback.withValues(fallback.anchor,
                value(values, "mx", fallback.x, -2.0f, 2.0f),
                value(values, "my", fallback.y, -2.0f, 2.0f),
                value(values, "mz", fallback.z, -2.0f, 2.0f),
                value(values, "ax", fallback.modelX, -5.0f, 5.0f),
                value(values, "ay", fallback.modelY, -5.0f, 5.0f),
                value(values, "az", fallback.modelZ, -5.0f, 5.0f),
                value(values, "rx", fallback.rotationX, -360.0f, 360.0f),
                value(values, "ry", fallback.rotationY, -360.0f, 360.0f),
                value(values, "rz", fallback.rotationZ, -360.0f, 360.0f));
    }

    private static HolsterPlacement adjust(HolsterPlacement value, String action) {
        float direction = action.endsWith("_dec") ? -1.0f : 1.0f;
        float mx = value.x, my = value.y, mz = value.z;
        float ax = value.modelX, ay = value.modelY, az = value.modelZ;
        float rx = value.rotationX, ry = value.rotationY, rz = value.rotationZ;
        String field = action.substring(0, action.length() - 4);
        if ("mx".equals(field)) mx += POSITION_STEP * direction;
        else if ("my".equals(field)) my += POSITION_STEP * direction;
        else if ("mz".equals(field)) mz += POSITION_STEP * direction;
        else if ("ax".equals(field)) ax += POSITION_STEP * direction;
        else if ("ay".equals(field)) ay += POSITION_STEP * direction;
        else if ("az".equals(field)) az += POSITION_STEP * direction;
        else if ("rx".equals(field)) rx += ROTATION_STEP * direction;
        else if ("ry".equals(field)) ry += ROTATION_STEP * direction;
        else if ("rz".equals(field)) rz += ROTATION_STEP * direction;
        return value.withValues(value.anchor, mx, my, mz, ax, ay, az, rx, ry, rz);
    }

    private String bml() {
        return buildBml(selection.label, selection.key, selection.bounds,
                placement, profiles.hasSaved(selection.key));
    }

    static String buildBml(String itemLabel, String profileKey, String modelBounds,
                           HolsterPlacement placement, boolean savedProfile) {
        StringBuilder value = new StringBuilder(2400);
        // A top-level varray is intentional. The legacy WU border renderer
        // indexes five positional children even when named sections are omitted.
        value.append("varray{")
                .append("passthrough{id='").append(MARKER).append("';text='1'};")
                .append("label{text='").append(escape(itemLabel)).append("'};")
                .append("text{text='Model: ").append(escape(profileKey)).append("'};")
                .append("text{text='Model bounds: ").append(escape(modelBounds)).append("'};")
                .append("label{text='Saved profile: ")
                .append(savedProfile ? "yes" : "no").append("'};")
                .append("harray{button{id='previous';text='Previous item'};")
                .append("button{id='next';text='Next item'}};")
                .append("harray{label{text='Skeleton anchor: ")
                .append(placement.anchor).append("'};")
                .append("button{id='anchor';text='Switch anchor'}};")
                .append("text{text='Mount point is on the skeleton. Model point is the exact point on the item that is placed there.'};")
                .append("text{text='The highlighted mount point is crossed by its axes: X red, Y green, Z blue.'};")
                .append("label{text='Skeleton mount point (metres)'};");
        row(value, "mx", "Mount X", placement.x);
        row(value, "my", "Mount Y", placement.y);
        row(value, "mz", "Mount Z", placement.z);
        value.append("label{text='Item model attachment point (model coordinates)'};");
        row(value, "ax", "Model X", placement.modelX);
        row(value, "ay", "Model Y", placement.modelY);
        row(value, "az", "Model Z", placement.modelZ);
        value.append("label{text='Item rotation (degrees)'};");
        row(value, "rx", "Rotate X", placement.rotationX);
        row(value, "ry", "Rotate Y", placement.rotationY);
        row(value, "rz", "Rotate Z", placement.rotationZ);
        value.append("text{text='Buttons change position by 0.02 m and rotation by 5 degrees.'};")
                .append("harray{button{id='defaults';text='Load defaults'};")
                .append("button{id='remove';text='Remove saved profile'}}")
                .append("harray{")
                .append("button{id='apply';text='Apply preview'};")
                .append("button{id='save';text='Save item profile'};")
                .append("button{id='close';text='Close'}")
                .append("}}");
        return value.toString();
    }

    private static void row(StringBuilder bml, String id, String label, float value) {
        bml.append("harray{label{text='").append(label).append("'};")
                .append("button{id='").append(id).append("_dec';text='-'};")
                .append("input{id='").append(id).append("';text='")
                .append(number(value)).append("';maxchars='12'};")
                .append("button{id='").append(id).append("_inc';text='+'}};");
    }

    static float value(Map<?, ?> values, String key, float fallback,
                       float minimum, float maximum) {
        String text = string(values.get(key));
        if (text == null || text.trim().isEmpty()) return fallback;
        final float parsed;
        try {
            parsed = Float.parseFloat(text.trim().replace(',', '.'));
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        if (Float.isNaN(parsed) || Float.isInfinite(parsed))
            throw new IllegalArgumentException(key + " must be finite");
        if (parsed < minimum || parsed > maximum)
            throw new IllegalArgumentException(key + " must be between "
                    + minimum + " and " + maximum);
        return parsed;
    }

    private static List<Selection> selections(World world) {
        List<Selection> result = new ArrayList<Selection>();
        PlayerBodyRenderable body = world == null || world.getPlayer() == null
                ? null : world.getPlayer().getPlayerBody();
        if (body == null) return result;
        for (int slot : SLOTS) {
            AttachedCellRenderable attachment = body.getEquipment(slot);
            String key = HolsterProfileStore.profileKey(attachment, slot);
            if (attachment == null || key == null) continue;
            result.add(new Selection(slot, key,
                    HolsterEquipmentRenderer.classify(body, attachment, slot),
                    HolsterProfileStore.displayName(attachment, slot),
                    modelBounds(attachment)));
        }
        return result;
    }

    private static Selection find(List<Selection> items, String key) {
        if (key == null) return null;
        for (Selection item : items) if (key.equals(item.key)) return item;
        return null;
    }

    private static String modelBounds(AttachedCellRenderable attachment) {
        try {
            ModelResourceWrapper wrapper = attachment == null
                    ? null : attachment.getModelWrapper();
            ModelDataBounds bounds = wrapper == null ? null : wrapper.getBounds();
            if (bounds == null) return "unavailable";
            // ModelDataBounds calls vertical model Y "H" and model Z "Y".
            return String.format(Locale.ROOT,
                    "X %.3f..%.3f, Y %.3f..%.3f, Z %.3f..%.3f",
                    bounds.getX0(), bounds.getX1(), bounds.getH0(), bounds.getH1(),
                    bounds.getY0(), bounds.getY1());
        } catch (Throwable unavailable) {
            return "unavailable";
        }
    }

    private static void output(World world, String message) {
        if (world != null && world.getHud() != null) world.getHud().consoleOutput(message);
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String escape(String value) {
        return value == null ? "Unknown item"
                : value.replace('\\', '/').replace('\'', ' ').replace('{', '(')
                        .replace('}', ')').replace(';', ',');
    }

    private static final class Selection {
        final int slot;
        final String key;
        final HolsterItemClass type;
        final String label;
        final String bounds;

        private Selection(int slot, String key, HolsterItemClass type,
                          String label, String bounds) {
            this.slot = slot;
            this.key = key;
            this.type = type;
            this.label = label;
            this.bounds = bounds;
        }
    }
}
