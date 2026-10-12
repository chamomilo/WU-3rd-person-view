package org.wuthirdpersonview.client;

import org.chamomilo.wurm.update.ModSetting;
import java.util.*;

/** Product metadata for the shared native Settings editor. */
final class ModSettingsSchema {
    private ModSettingsSchema() { }
    static List<ModSetting> specs() {
        return Arrays.asList(
            ModSetting.bool("Camera", "enabled", "Third person enabled", "Switch the current view between third person and native first person.", true, true),
            ModSetting.number("Camera", "defaultZoom", "Default zoom", "Camera distance as a fraction of the minimum to maximum range. Applies to the current zoom without changing first/third-person mode; the normal camera state store remembers later zoom commands.", 0.32, 0.01, 1, false, true),
            ModSetting.number("Camera", "zoomStep", "Zoom step", "Fraction added or removed for each zoom command.", 0.08, 0.01, 0.5, false, true),
            ModSetting.number("Camera", "minDistance", "Minimum distance (m)", "Nearest camera distance behind the character. Must be below maximum distance.", 1.2, 1.2, 3, false, true),
            ModSetting.number("Camera", "maxDistance", "Maximum distance (m)", "Farthest camera distance. Must exceed minimum distance by at least 0.1 m.", 12, 1.3, 50, false, true),
            ModSetting.number("Camera", "headAnchorVerticalOffset", "Head anchor offset (m)", "Vertical offset from the animated eye point.", 0, -1, 1, false, true),
            ModSetting.bool("Head animation", "animateHead", "Animate head", "Turn the visible head using native look input.", true, true),
            ModSetting.number("Head animation", "maxHeadYawDegrees", "Maximum head yaw (degrees)", "Maximum horizontal head turn before the body follows.", 55, 10, 80, false, true),
            ModSetting.number("Head animation", "maxHeadPitchDegrees", "Maximum head pitch (degrees)", "Maximum vertical head turn.", 60, 10, 80, false, true),
            ModSetting.number("Head animation", "bodyFollowSeconds", "Body follow time (s)", "Time used to smooth the body's follow rotation.", 0.12, 0, 1, false, true),
            ModSetting.number("Smoothing and collision", "zoomSmoothingSeconds", "Zoom smoothing (s)", "Time used to approach a new camera distance.", 0.1, 0, 2, false, true),
            ModSetting.number("Smoothing and collision", "collisionReturnSeconds", "Collision return time (s)", "Time used to return behind the character after a camera obstruction.", 0.28, 0, 3, false, true),
            ModSetting.number("Smoothing and collision", "cameraRadius", "Camera radius (m)", "Radius used when probing camera obstructions.", 0.24, 0.05, 1.5, false, true),
            ModSetting.number("Smoothing and collision", "collisionMargin", "Collision margin (m)", "Extra distance kept from an obstruction.", 0.1, 0, 1, false, true),
            ModSetting.number("Smoothing and collision", "surfaceClearance", "Surface clearance (m)", "Minimum camera height above the active surface.", 0.25, 0.05, 2, false, true),
            ModSetting.number("Smoothing and collision", "firstPersonEnterDistance", "Enter first person distance (m)", "Switch to first person when an obstruction brings the camera this close.", 1, 1, 2, false, true),
            ModSetting.number("Smoothing and collision", "firstPersonExitDistance", "Exit first person distance (m)", "Restore third person past this distance. Must exceed enter distance by 0.05 m.", 1.15, 1.05, 3, false, true),
            ModSetting.text("Orbit", "orbitKey", "Orbit key", "LWJGL key name used for held free orbit, for example LSHIFT, RSHIFT, or NONE.", "LSHIFT", true),
            ModSetting.number("Orbit", "orbitSensitivity", "Orbit sensitivity", "Multiplier for orbit mouse motion.", 1, 0.05, 8, false, true),
            ModSetting.bool("Orbit", "orbitReturns", "Return orbit", "Return the camera behind the character when the orbit key is released.", true, true),
            ModSetting.number("Orbit", "orbitReturnSeconds", "Orbit return time (s)", "Time used to recenter released free orbit.", 0.45, 0, 5, false, true),
            ModSetting.number("Orbit", "maxOrbitPitchDegrees", "Maximum orbit pitch (degrees)", "Limit for vertical free orbit.", 35, 5, 80, false, true),
            ModSetting.choice("Input", "inputProvider", "Zoom input", "Auto cooperates with Keybinder and falls back to the native mouse wheel.", "auto", true, "auto", "native", "keybinder"),
            ModSetting.bool("Advanced", "debug", "Camera diagnostics", "Write camera diagnostic messages.", false, true)
        );
    }
}
