package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Assume;
import org.junit.Test;

/** Runs when the sibling Keybinder development build is available. */
public class KeybinderRealCodecCompatibilityTest {
    @Test
    public void currentKeybinderCanReadBundledPreset() throws Exception {
        Path jar = Paths.get("..", "wurm", "build", "libs", "keybinder.jar")
                .toAbsolutePath().normalize();
        Assume.assumeTrue("Sibling Keybinder build is unavailable", Files.isRegularFile(jar));
        try (URLClassLoader loader = new URLClassLoader(new URL[] { jar.toUri().toURL() },
                getClass().getClassLoader());
             InputStream input = getClass().getResourceAsStream(
                     "/keybinder/WU-third_person_view.keybinder")) {
            Class<?> codec = Class.forName(
                    "org.keybinder.wurm.transfer.KeybindTransferStore", true, loader);
            Object store = codec.newInstance();
            Method read = codec.getMethod("read", InputStream.class);
            List<?> records = (List<?>) read.invoke(store, input);
            assertEquals(6, records.size());
        }
    }

    @Test
    public void currentKeybinderDiscoversPublishedCommandCatalog() throws Exception {
        Path jar = Paths.get("..", "wurm", "build", "libs", "keybinder.jar")
                .toAbsolutePath().normalize();
        Assume.assumeTrue("Sibling Keybinder build is unavailable", Files.isRegularFile(jar));
        try (URLClassLoader loader = new URLClassLoader(new URL[] { jar.toUri().toURL() },
                getClass().getClassLoader())) {
            Class<?> catalogType = Class.forName(
                    "org.keybinder.wurm.catalog.ModCommandCatalog", true, loader);
            Object catalog = catalogType.getMethod("load", Class.class, String.class)
                    .invoke(null, WUThirdPersonViewMod.class, "Third Person View");
            assertTrue((Boolean) catalogType.getMethod("isVisible").invoke(catalog));
            List<?> entries = (List<?>) catalogType.getMethod("getEntries").invoke(catalog);
            assertEquals(20, entries.size());

            Class<?> entryType = Class.forName(
                    "org.keybinder.wurm.catalog.ModCommandCatalog$Entry",
                    true, loader);
            Method command = entryType.getMethod("getCommand");
            boolean hasOrbitHold = false;
            for (Object entry : entries) {
                String value = (String) command.invoke(entry);
                if ("tp orbit-hold".equals(value)) hasOrbitHold = true;
                assertTrue(value.startsWith("tp "));
            }
            assertTrue(hasOrbitHold);
        }
    }
}
