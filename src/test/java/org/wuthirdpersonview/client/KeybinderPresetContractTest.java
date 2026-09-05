package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.junit.Test;

public class KeybinderPresetContractTest {
    @Test
    public void presetUsesSupportedSchemaAndExpectedCommands() throws Exception {
        Properties values = new Properties();
        try (InputStream input = getClass().getResourceAsStream(
                "/keybinder/WU-third_person_view.keybinder")) {
            assertTrue(input != null);
            values.load(input);
        }
        assertEquals("keybinder-transfer", values.getProperty("format"));
        assertEquals("1", values.getProperty("version"));
        assertEquals("10", values.getProperty("definitionSchema"));
        assertEquals("6", values.getProperty("count"));
        Set<String> commands = new HashSet<String>();
        for (int i = 0; i < 6; i++) {
            String prefix = "record." + i + ".variant.0.step.0.";
            assertEquals("CONSOLE_COMMAND", values.getProperty(prefix + "kind"));
            commands.add(decode(values.getProperty(prefix + "command")));
        }
        assertTrue(commands.contains("tp toggle"));
        assertTrue(commands.contains("tp zoom-in"));
        assertTrue(commands.contains("tp zoom-out"));
        assertTrue(commands.contains("tp reset"));
        assertTrue(commands.contains("tp orbit-hold"));
        assertTrue(commands.contains("tp freelook-toggle"));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
