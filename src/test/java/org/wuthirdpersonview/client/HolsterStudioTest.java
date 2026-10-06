package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.wurmonline.client.renderer.Matrix;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.wuthirdpersonview.core.HolsterItemClass;

public class HolsterStudioTest {
    @Test
    public void generatedEditorBmlContainsEveryTransformControl() {
        HolsterPlacement placement = HolsterPlacement.forItem(
                HolsterItemClass.TWO_HANDED_SWORD_CENTER, 1);
        String bml = HolsterStudio.buildBml("two handed sword",
                "weapon/twohandedsword.wom#slot=1",
                "X -0.129..0.129, Y 0.009..0.050, Z -1.107..0.204",
                placement, false);

        assertTrue(bml.startsWith("varray{"));
        assertEquals(9, occurrences(bml, "input{id="));
        assertEquals(26, occurrences(bml, "button{id="));
        assertEquals(1, occurrences(bml, "passthrough{id="));
        assertEquals(0, braceBalance(bml));
        assertTrue(bml.contains("Z -1.107..0.204"));
        assertTrue(bml.contains("X red, Y green, Z blue"));
        assertEquals("Holster Studio", HolsterStudio.WINDOW_TITLE);
        assertTrue(!bml.equals(HolsterStudio.WINDOW_TITLE));

    }

    @Test
    public void gizmoPivotUsesTheEditedMountPointInAnchorSpace() {
        Matrix anchor = new Matrix().fromTranslationRotation(
                10.0f, 20.0f, 30.0f, 0.0f, 0.0f, 0.0f);
        Matrix pivot = HolsterStudioGizmo.pivotMatrix(anchor,
                -0.25f, 0.5f, 0.75f);

        assertEquals(9.75f, pivot._m30(), 0.001f);
        assertEquals(20.5f, pivot._m31(), 0.001f);
        assertEquals(30.75f, pivot._m32(), 0.001f);
    }

    @Test
    public void numericFieldsAcceptDecimalCommaAndRejectNonFiniteValues() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("mx", "-0,125");
        assertEquals(-0.125f,
                HolsterStudio.value(values, "mx", 0.0f, -2.0f, 2.0f), 0.001f);

        values.put("mx", "NaN");
        try {
            HolsterStudio.value(values, "mx", 0.0f, -2.0f, 2.0f);
            fail("NaN must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("mx must be finite", expected.getMessage());
        }
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static int braceBalance(String value) {
        int balance = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '{') balance++;
            else if (value.charAt(i) == '}') balance--;
            if (balance < 0) return balance;
        }
        return balance;
    }
}
