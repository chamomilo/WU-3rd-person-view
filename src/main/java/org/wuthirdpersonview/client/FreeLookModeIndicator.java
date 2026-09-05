package org.wuthirdpersonview.client;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.StaticComponent;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.client.renderer.gui.text.TextFont;
import java.lang.reflect.Field;

/** Non-interactive bottom-right HUD marker for Wurm's flying free camera. */
public final class FreeLookModeIndicator extends StaticComponent {
    static final int PANEL_WIDTH = 178;
    static final int PANEL_HEIGHT = 42;
    static final int RIGHT_MARGIN = 24;
    static final int FALLBACK_BOTTOM_MARGIN = 180;
    static final int EVENT_WINDOW_GAP = 12;
    private static final String LABEL = "FREE LOOK MODE";

    private final TextFont labelFont;
    private final Field eventWindowField;
    private final Field componentYField;
    private volatile int left;
    private volatile int top;
    private volatile boolean active;

    public FreeLookModeIndicator() {
        super("WU free look mode indicator", 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        labelFont = TextFont.getBoldText();
        eventWindowField = optionalField(HeadsUpDisplay.class, "eventWindow");
        componentYField = optionalField(WurmComponent.class, "y");
    }

    /**
     * Synchronizes the marker with the actual renderer mode. The marker is
     * painted by the HUD-render hook and is never registered as an interactive
     * component, so mouse picking cannot select it.
     */
    public void update(HeadsUpDisplay hud, boolean enabled) {
        active = enabled;
        if (hud == null) return;
        left = HudCornerLayout.rightAligned(hud.getWidth(), PANEL_WIDTH,
                RIGHT_MARGIN);
        top = topAboveEventWindow(hud);
        setPosition(left, top);
    }

    private int topAboveEventWindow(HeadsUpDisplay hud) {
        int fallback = HudCornerLayout.bottomAligned(hud.getHeight(),
                PANEL_HEIGHT, FALLBACK_BOTTOM_MARGIN);
        if (eventWindowField == null || componentYField == null) return fallback;
        try {
            Object eventWindow = eventWindowField.get(hud);
            if (eventWindow == null) return fallback;
            int eventTop = componentYField.getInt(eventWindow);
            if (eventTop < PANEL_HEIGHT + EVENT_WINDOW_GAP
                    || eventTop > hud.getHeight()) return fallback;
            return HudCornerLayout.above(eventTop, PANEL_HEIGHT,
                    EVENT_WINDOW_GAP);
        } catch (ReflectiveOperationException unavailable) {
            return fallback;
        }
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

    @Override
    protected void renderComponent(Queue queue, float alpha) {
        if (!active || labelFont == null) return;
        float opacity = Math.max(0.0f, Math.min(1.0f, alpha));

        // Cut-corner charcoal plate with a warm Wurm-style brass edge.
        fillRect(queue, 0.025f, 0.029f, 0.032f, 0.86f * opacity,
                left + 4, top, PANEL_WIDTH - 8, PANEL_HEIGHT);
        fillRect(queue, 0.025f, 0.029f, 0.032f, 0.86f * opacity,
                left, top + 4, PANEL_WIDTH, PANEL_HEIGHT - 8);
        fillRect(queue, 0.78f, 0.61f, 0.24f, 0.92f * opacity,
                left + 5, top, PANEL_WIDTH - 10, 1);
        fillRect(queue, 0.42f, 0.32f, 0.13f, 0.92f * opacity,
                left + 5, top + PANEL_HEIGHT - 1, PANEL_WIDTH - 10, 1);
        fillRect(queue, 0.72f, 0.55f, 0.20f, 0.92f * opacity,
                left, top + 5, 1, PANEL_HEIGHT - 10);
        fillRect(queue, 0.42f, 0.32f, 0.13f, 0.92f * opacity,
                left + PANEL_WIDTH - 1, top + 5, 1, PANEL_HEIGHT - 10);

        // Accent rail and status lamp make the mode readable at a glance.
        fillRect(queue, 0.91f, 0.72f, 0.27f, opacity,
                left + 10, top + 9, 3, PANEL_HEIGHT - 18);
        fillRect(queue, 0.16f, 0.78f, 0.73f, opacity,
                left + 21, top + 18, 7, 7);
        fillRect(queue, 0.57f, 1.0f, 0.91f, 0.45f * opacity,
                left + 22, top + 19, 5, 2);

        int textX = left + 39;
        int textBaseline = top + (PANEL_HEIGHT + labelFont.getAscent()
                - labelFont.getDescent()) / 2;
        // A one-pixel shadow preserves contrast over bright scenery.
        labelFont.moveTo(textX + 1, textBaseline + 1);
        labelFont.paint(queue, LABEL, 0.0f, 0.0f, 0.0f, 0.80f * opacity);
        labelFont.moveTo(textX, textBaseline);
        labelFont.paint(queue, LABEL, 0.96f, 0.88f, 0.65f, opacity);
    }

}
