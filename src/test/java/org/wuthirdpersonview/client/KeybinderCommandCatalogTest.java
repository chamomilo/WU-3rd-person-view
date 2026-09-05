package org.wuthirdpersonview.client;

import org.junit.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeybinderCommandCatalogTest {
    @Test
    public void exposesCompleteUniqueCommandsWithoutEditorArguments() {
        String[][] rows = WUThirdPersonViewMod.getKeybinderCommandCatalog();
        Set<String> commands = new HashSet<String>();

        assertEquals(20, rows.length);
        for (String[] row : rows) {
            assertEquals(2, row.length);
            assertFalse(row[0].trim().isEmpty());
            String command = row[1].trim();
            assertTrue(command.equals("tp") || command.startsWith("tp "));
            assertFalse(command.contains("<"));
            assertFalse(command.contains(">"));
            assertFalse(command.toLowerCase(Locale.ENGLISH).contains("tool"));
            assertFalse(command.toLowerCase(Locale.ENGLISH).contains("target"));
            assertTrue("duplicate command: " + command,
                    commands.add(command.toLowerCase(Locale.ENGLISH)));
        }
        assertTrue(commands.contains("tp toggle"));
        assertFalse(commands.contains("tp shoulder-left"));
        assertFalse(commands.contains("tp shoulder-right"));
        assertTrue(commands.contains("tp orbit-hold"));
        assertTrue(commands.contains("tp freelook-toggle"));
        assertTrue(commands.contains("tp freelook-on"));
        assertTrue(commands.contains("tp freelook-off"));
        assertTrue(commands.contains("tp status"));
    }

    @Test
    public void returnsDefensiveCopy() {
        String[][] first = WUThirdPersonViewMod.getKeybinderCommandCatalog();
        first[0][1] = "tp corrupted";

        assertEquals("tp toggle", WUThirdPersonViewMod.getKeybinderCommandCatalog()[0][1]);
    }
}
