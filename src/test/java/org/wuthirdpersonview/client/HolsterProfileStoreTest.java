package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wuthirdpersonview.core.HolsterItemClass;

public class HolsterProfileStoreTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void savesAndReloadsAnExactItemSlotProfile() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("holster-profiles.properties");
        HolsterProfileStore store = new HolsterProfileStore(path);
        String key = "weapon/twohandedsword.wom#slot=1";
        HolsterPlacement custom = HolsterPlacement.forItem(
                HolsterItemClass.TWO_HANDED_SWORD_CENTER, 1).withValues(
                "SpineTop", 0.07f, 0.31f, -0.21f,
                0.0f, 0.03f, -0.04f, -90.0f, 7.0f, 2.0f);

        store.preview(key, custom);
        assertEquals(0.07f, store.resolve(key,
                HolsterItemClass.TWO_HANDED_SWORD_CENTER, 1).x, 0.001f);
        assertTrue(store.save(key, custom));

        HolsterProfileStore reloaded = new HolsterProfileStore(path);
        reloaded.load();
        HolsterPlacement actual = reloaded.resolve(key,
                HolsterItemClass.TWO_HANDED_SWORD_CENTER, 1);
        assertEquals("SpineTop", actual.anchor);
        assertEquals(0.31f, actual.y, 0.001f);
        assertEquals(-0.04f, actual.modelZ, 0.001f);
        assertEquals(-90.0f, actual.rotationX, 0.001f);
        assertEquals(7.0f, actual.rotationY, 0.001f);
        assertTrue(reloaded.hasSaved(key));

        assertTrue(reloaded.remove(key));
        assertFalse(reloaded.hasSaved(key));
    }

    @Test
    public void discardedPreviewFallsBackToSavedProfile() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("profiles.properties");
        HolsterProfileStore store = new HolsterProfileStore(path);
        String key = "tools/sickle.wom#slot=1";
        HolsterPlacement saved = HolsterPlacement.forItem(
                HolsterItemClass.SHORT_BELT, 1);
        HolsterPlacement preview = saved.withValues(saved.anchor,
                0.5f, saved.y, saved.z, saved.modelX, saved.modelY, saved.modelZ,
                saved.rotationX, saved.rotationY, saved.rotationZ);
        assertTrue(store.save(key, saved));
        store.preview(key, preview);
        assertEquals(0.5f, store.resolve(key,
                HolsterItemClass.SHORT_BELT, 1).x, 0.001f);
        store.discardPreview(key);
        assertEquals(saved.x, store.resolve(key,
                HolsterItemClass.SHORT_BELT, 1).x, 0.001f);
    }
}
