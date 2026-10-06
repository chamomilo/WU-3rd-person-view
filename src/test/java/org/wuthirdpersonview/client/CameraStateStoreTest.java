package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wuthirdpersonview.core.CameraConfig;
import org.wuthirdpersonview.core.CameraState;
import org.wuthirdpersonview.core.HolsterState;

public class CameraStateStoreTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void persistsHolsterAlongsideCameraState() throws Exception {
        File file = temporary.newFolder("state").toPath().resolve("state.properties").toFile();
        CameraStateStore store = new CameraStateStore(file.toPath());
        CameraState savedCamera = new CameraState(CameraConfig.from(null));
        savedCamera.setThirdPersonActive(false);
        savedCamera.setZoom(0.73f);
        savedCamera.setThirdPersonActive(false);
        HolsterState savedHolster = new HolsterState(1.0f);
        savedHolster.setHolsteredImmediately(true);
        store.save(savedCamera, savedHolster);

        CameraState loadedCamera = new CameraState(CameraConfig.from(null));
        HolsterState loadedHolster = new HolsterState(1.0f);
        new CameraStateStore(file.toPath()).load(loadedCamera, loadedHolster);

        assertFalse(loadedCamera.isThirdPersonActive());
        assertEquals(0.73f, loadedCamera.getZoom(), 0.001f);
        assertTrue(loadedHolster.isDesiredHolstered());
        assertEquals(1.0f, loadedHolster.snapshot().weaponAmount, 0.001f);
    }
}
