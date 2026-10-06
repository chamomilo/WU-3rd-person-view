package org.wuthirdpersonview.core;

/** Immutable render-time view of the holster transition. */
public final class HolsterSnapshot {
    public final boolean desiredHolstered;
    public final boolean targetPresent;
    public final float weaponAmount;
    public final float shieldAmount;

    HolsterSnapshot(boolean desiredHolstered, boolean targetPresent,
                    float weaponAmount, float shieldAmount) {
        this.desiredHolstered = desiredHolstered;
        this.targetPresent = targetPresent;
        this.weaponAmount = CameraMath.clamp(weaponAmount, 0.0f, 1.0f);
        this.shieldAmount = CameraMath.clamp(shieldAmount, 0.0f, 1.0f);
    }

    public boolean replacesVanillaEquipment() {
        return desiredHolstered || weaponAmount > 0.0001f || shieldAmount > 0.0001f;
    }

    public boolean isTransitioning() {
        float target = desiredHolstered ? 1.0f : 0.0f;
        return Math.abs(weaponAmount - target) > 0.0001f
                || Math.abs(shieldAmount - target) > 0.0001f;
    }

    public float reachForWeapon() { return reachAt(weaponAmount); }
    public float reachForShield() { return reachAt(shieldAmount); }

    /**
     * The hand is in its native pose at either endpoint and reaches the mount
     * at the middle, where the rendered item changes attachment.
     */
    static float reachAt(float amount) {
        return (float) Math.sin(Math.PI * CameraMath.clamp(amount, 0.0f, 1.0f));
    }
}
