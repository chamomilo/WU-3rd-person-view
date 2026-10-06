package org.wuthirdpersonview.core;

import java.util.Locale;

/** Model-name rules backed by a size fallback for server-added equipment. */
public final class HolsterItemClassifier {
    private HolsterItemClassifier() { }

    public static HolsterItemClass classify(String modelName, boolean shield,
                                            boolean twoHanded, float longestDimension) {
        String name = normalize(modelName);
        if (shield) return shield(name, longestDimension);

        if (containsAny(name, "swordtwo", "twohandedsword", "two-handed-sword",
                "two_handed_sword", "twohand.sword"))
            return HolsterItemClass.TWO_HANDED_SWORD_CENTER;

        if (twoHanded || containsAny(name, "hugeaxe", "axe.large", "largemaul",
                "maul.large", "halberd", "spear", "polearm", "staff", "scythe",
                "longbow", "mediumbow", "shortbow", "fishingrod", "dredge"))
            return HolsterItemClass.TWO_HANDED_DIAGONAL;

        if (containsAny(name, "swordlong", "longsword", "axe.medium", "largeaxe",
                "maul.medium", "medmaul", "pickaxe", "shovel", "rake", "crowbar",
                "range_pole", "rangepole", "leggat", "sickle"))
            return HolsterItemClass.MEDIUM_SHOULDER;

        if (containsAny(name, "swordshort", "shortsword", "axe.small", "smallaxe",
                "maul.small", "smallmaul", "hatchet", "hammer", "mallet",
                "knife", "saw", "awl", "chisel", "file", "trowel", "pliers",
                "scissor", "spatula", "fork", "brandingiron", "branding_iron"))
            return HolsterItemClass.SHORT_BELT;

        if (longestDimension >= 1.35f) return HolsterItemClass.TWO_HANDED_DIAGONAL;
        if (longestDimension >= 0.75f) return HolsterItemClass.MEDIUM_SHOULDER;
        return HolsterItemClass.SHORT_BELT;
    }

    private static HolsterItemClass shield(String name, float longestDimension) {
        if (name.contains("large") || longestDimension >= 1.05f)
            return HolsterItemClass.SHIELD_LARGE;
        if (name.contains("medium") || longestDimension >= 0.72f)
            return HolsterItemClass.SHIELD_MEDIUM;
        return HolsterItemClass.SHIELD_SMALL;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH)
                .replace('\\', '/').replace(" ", "");
    }
}
