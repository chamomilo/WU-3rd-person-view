package org.wuthirdpersonview.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HolsterStateTest {
    @Test
    public void shieldCompletesBeforeWeaponWithinOneSecond() {
        HolsterState state = new HolsterState(1.0f);
        assertTrue(state.requestHolstered(true, false));

        advance(state, 41, true, true);
        HolsterSnapshot beforeShieldEnd = state.snapshot();
        assertTrue(beforeShieldEnd.shieldAmount > 0.9f);
        assertEquals(0.0f, beforeShieldEnd.weaponAmount, 0.001f);

        advance(state, 1, true, true);
        assertEquals(1.0f, state.snapshot().shieldAmount, 0.001f);
        advance(state, 58, true, true);
        HolsterSnapshot finished = state.snapshot();
        assertEquals(1.0f, finished.weaponAmount, 0.001f);
        assertEquals(1.0f, finished.shieldAmount, 0.001f);
        assertFalse(finished.isTransitioning());
    }

    @Test
    public void unholsterReversesOrderAndStartsWithWeapon() {
        HolsterState state = new HolsterState(1.0f);
        state.setHolsteredImmediately(true);
        assertTrue(state.requestHolstered(false, false));

        advance(state, 20, true, true);
        assertTrue(state.snapshot().weaponAmount < 1.0f);
        assertEquals(1.0f, state.snapshot().shieldAmount, 0.001f);

        advance(state, 38, true, true);
        assertEquals(0.0f, state.snapshot().weaponAmount, 0.001f);
        advance(state, 42, true, true);
        assertEquals(0.0f, state.snapshot().shieldAmount, 0.001f);
        assertFalse(state.snapshot().isTransitioning());
    }

    @Test
    public void dualWeaponsShareOneSynchronizedAmount() {
        HolsterState state = new HolsterState(1.0f);
        state.requestHolstered(true, false);
        advance(state, 50, true, false);
        assertEquals(0.5f, state.snapshot().weaponAmount, 0.015f);
        assertEquals(1.0f, state.snapshot().reachForWeapon(), 0.01f);
    }

    @Test
    public void targetBlocksHolsterAndImmediatelyReversesAnActiveTransition() {
        HolsterState state = new HolsterState(1.0f);
        assertFalse(state.requestHolstered(true, true));
        assertFalse(state.isDesiredHolstered());

        assertTrue(state.requestHolstered(true, false));
        advance(state, 30, true, false);
        float partial = state.snapshot().weaponAmount;
        assertTrue(partial > 0.0f);
        assertTrue(state.updateTarget(true));
        assertFalse(state.isDesiredHolstered());
        advance(state, 1, true, false);
        assertTrue(state.snapshot().weaponAmount < partial);
    }

    @Test
    public void emptySlotsAdoptTheRequestedEndpoint() {
        HolsterState state = new HolsterState(1.0f);
        state.requestHolstered(true, false);
        state.advance(0.01f, false, false);
        assertFalse(state.snapshot().isTransitioning());
        assertEquals(1.0f, state.snapshot().weaponAmount, 0.001f);
        assertEquals(1.0f, state.snapshot().shieldAmount, 0.001f);
    }

    private static void advance(HolsterState state, int hundredths,
                                boolean weapon, boolean shield) {
        for (int i = 0; i < hundredths; i++) state.advance(0.01f, weapon, shield);
    }
}
