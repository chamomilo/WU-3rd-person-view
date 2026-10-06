package org.wuthirdpersonview.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HolsterItemClassifierTest {
    @Test
    public void classifiesNamedWeaponAndToolFamilies() {
        assertType(HolsterItemClass.SHORT_BELT, "model.weapon.swordshort", false, false);
        assertType(HolsterItemClass.MEDIUM_SHOULDER, "model.tool.sickle", false, false);
        assertType(HolsterItemClass.SHORT_BELT, "tools/ButcherKnife/butcherknife.wom",
                false, false);
        assertType(HolsterItemClass.MEDIUM_SHOULDER, "model.weapon.swordlong",
                false, false);
        assertType(HolsterItemClass.MEDIUM_SHOULDER, "model.tool.pickaxe", false, false);
        assertType(HolsterItemClass.TWO_HANDED_DIAGONAL, "model.weapon.axe.large",
                false, true);
        assertType(HolsterItemClass.TWO_HANDED_DIAGONAL, "model.weapon.polearm.halberd",
                false, true);
        assertType(HolsterItemClass.TWO_HANDED_SWORD_CENTER, "model.weapon.swordtwo",
                false, true);
    }

    @Test
    public void classifiesShieldSizesAndUnknownItemsByBounds() {
        assertEquals(HolsterItemClass.SHIELD_SMALL,
                HolsterItemClassifier.classify("SmallMetalShield", true, false, 0.5f));
        assertEquals(HolsterItemClass.SHIELD_MEDIUM,
                HolsterItemClassifier.classify("MediumWoodenShield", true, false, 0.8f));
        assertEquals(HolsterItemClass.SHIELD_LARGE,
                HolsterItemClassifier.classify("customShield", true, false, 1.2f));
        assertEquals(HolsterItemClass.SHORT_BELT,
                HolsterItemClassifier.classify("server.custom", false, false, 0.5f));
        assertEquals(HolsterItemClass.MEDIUM_SHOULDER,
                HolsterItemClassifier.classify("server.custom", false, false, 0.9f));
        assertEquals(HolsterItemClass.TWO_HANDED_DIAGONAL,
                HolsterItemClassifier.classify("server.custom", false, false, 1.5f));
    }

    private static void assertType(HolsterItemClass expected, String name,
                                   boolean shield, boolean twoHanded) {
        assertEquals(expected, HolsterItemClassifier.classify(name, shield, twoHanded, 0.0f));
    }
}
