package org.wuthirdpersonview.client;

import com.wurmonline.client.game.CaveDataBuffer;
import org.wuthirdpersonview.core.CaveDataCoverage;

/** Shared receipt check for camera collision and visual layer selection. */
final class CaveBufferAccess {
    private CaveBufferAccess() { }

    static boolean isLoaded(CaveDataBuffer cave, float x, float z) {
        if (cave == null || !Float.isFinite(x) || !Float.isFinite(z)) return false;
        if (CaveDataCoverage.isTracked(cave))
            return CaveDataCoverage.hasFloorCorners(cave,
                    (int) Math.floor(x / 4.0f), (int) Math.floor(z / 4.0f));
        // Retain compatibility with buffers filled before receipt tracking is available.
        return cave.isValid(x, z);
    }
}
