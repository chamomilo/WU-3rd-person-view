package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wuthirdpersonview.core.CameraConfig;
import org.wuthirdpersonview.core.CameraState;

public class CameraStateStoreTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void persistsCameraStateOnly() throws Exception {
        File file = temporary.newFolder("state").toPath().resolve("state.properties").toFile();
        CameraStateStore store = new CameraStateStore(file.toPath());
        CameraState savedCamera = new CameraState(CameraConfig.from(null));
        savedCamera.setThirdPersonActive(false);
        savedCamera.setZoom(0.73f);
        savedCamera.setThirdPersonActive(false);
        store.save(savedCamera);

        CameraState loadedCamera = new CameraState(CameraConfig.from(null));
        new CameraStateStore(file.toPath()).load(loadedCamera);

        assertFalse(loadedCamera.isThirdPersonActive());
        assertEquals(0.73f, loadedCamera.getZoom(), 0.001f);
    }
}
