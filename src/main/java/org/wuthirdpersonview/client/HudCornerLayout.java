package org.wuthirdpersonview.client;

/** Pure screen-edge layout math, kept independent from Wurm's JavaFX classes. */
final class HudCornerLayout {
    private HudCornerLayout() { }

    static int rightAligned(int screenWidth, int componentWidth, int margin) {
        return Math.max(0, screenWidth - componentWidth - margin);
    }

    static int bottomAligned(int screenHeight, int componentHeight, int margin) {
        return Math.max(0, screenHeight - componentHeight - margin);
    }

    static int above(int anchorTop, int componentHeight, int gap) {
        return Math.max(0, anchorTop - componentHeight - gap);
    }
}
