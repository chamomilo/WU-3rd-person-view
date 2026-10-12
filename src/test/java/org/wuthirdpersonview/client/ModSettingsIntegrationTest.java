package org.wuthirdpersonview.client;

import org.chamomilo.wurm.update.ModSetting;
import org.chamomilo.wurm.update.SharedModSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wuthirdpersonview.core.CameraState;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class ModSettingsIntegrationTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    private String previousHome;

    @Before public void isolateSavedOverrides() {
        previousHome = System.getProperty("user.home");
        System.setProperty("user.home", temporary.getRoot().getAbsolutePath());
    }

    @After public void restoreHome() {
        System.setProperty("user.home", previousHome);
    }

    @Test public void editorCoversEveryCameraPropertyInTheDescriptor() throws Exception {
        Properties descriptor = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get("mods/WU-third_person_view.properties"))) {
            descriptor.load(input);
        }
        Set<String> expected = new HashSet<String>();
        for (String key : descriptor.stringPropertyNames()) {
            if (!key.startsWith("update") && !key.equals("version") && !key.equals("classname")
                    && !key.equals("classpath") && !key.equals("sharedClassLoader")) expected.add(key);
        }
        Set<String> actual = new HashSet<String>();
        for (ModSetting setting : ModSettingsSchema.specs()) {
            assertTrue("duplicate setting: " + setting.key, actual.add(setting.key));
            assertNotNull(setting.validate(descriptor.getProperty(setting.key)));
            assertFalse(setting.description.trim().isEmpty());
        }
        assertEquals(expected, actual);
    }

    @Test public void exactDistanceGapsApplyAndInvalidPairsRollBack() throws Exception {
        WUThirdPersonViewMod mod = configuredMod();
        SharedModSettings.Provider provider = SharedModSettings.provider("wu-third-person-view");
        assertSame(provider, SharedModSettings.provider("WU-third_person_view"));
        provider.change("maxDistance", "1.3");
        provider.change("firstPersonExitDistance", "1.05");
        assertEquals(1.3f, state(mod).getConfig().maxDistance, 0.00001f);
        assertEquals(1.05f, state(mod).getConfig().firstPersonExitDistance, 0.00001f);

        try {
            provider.change("minDistance", "2");
            fail("A maximum below the new minimum must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Maximum distance"));
        }
        assertEquals(1.2f, state(mod).getConfig().minDistance, 0.00001f);
        assertEquals(1.2, Double.parseDouble(provider.value("minDistance")), 0.00001);
        Properties saved = savedOverrides();
        assertEquals("1.3", saved.getProperty("maxDistance"));
        assertEquals("1.05", saved.getProperty("firstPersonExitDistance"));
        assertFalse(saved.containsKey("minDistance"));
    }

    @Test public void liveTuningPreservesViewAndExplicitChangesSurviveRestart() throws Exception {
        WUThirdPersonViewMod mod = configuredMod();
        CameraState state = state(mod);
        state.setZoom(0.75f);
        state.setOrbitHeld(true);
        state.addOrbitInput(20, 10);
        state.setThirdPersonActive(false);
        SharedModSettings.Provider provider = SharedModSettings.provider("wu-third-person-view");
        provider.change("zoomStep", "0.2");
        assertFalse(state.isThirdPersonActive());
        assertEquals(0.75f, state.getZoom(), 0.00001f);
        assertEquals(20, state.getOrbitYawDegrees(), 0.00001f);
        assertEquals(10, state.getOrbitPitchDegrees(), 0.00001f);

        provider.change("defaultZoom", "0.4");
        assertFalse(state.isThirdPersonActive());
        assertEquals(0.4f, state.getZoom(), 0.00001f);
        provider.change("enabled", "false");
        provider.change("enabled", "true");
        assertTrue(state.isThirdPersonActive());
        provider.change("debug", "true");
        assertTrue(state.isDebug());

        WUThirdPersonViewMod restarted = configuredMod();
        assertEquals(0.2f, state(restarted).getConfig().zoomStep, 0.00001f);
        assertEquals(0.4f, state(restarted).getZoom(), 0.00001f);
        assertTrue(state(restarted).isDebug());
        assertEquals(4, savedOverrides().size());
    }

    private WUThirdPersonViewMod configuredMod() {
        Properties source = new Properties();
        source.setProperty("orbitKey", "NONE");
        WUThirdPersonViewMod mod = new WUThirdPersonViewMod();
        mod.configure(source);
        return mod;
    }

    private CameraState state(WUThirdPersonViewMod mod) throws Exception {
        Field field = WUThirdPersonViewMod.class.getDeclaredField("state");
        field.setAccessible(true);
        return (CameraState) field.get(mod);
    }

    private Properties savedOverrides() throws Exception {
        Path file = temporary.getRoot().toPath().resolve(".chamomilo/mod-settings/wu-third-person-view.properties");
        Properties saved = new Properties();
        try (InputStream input = Files.newInputStream(file)) { saved.load(input); }
        return saved;
    }
}
