package org.wuthirdpersonview.core;

/**
 * One-second reversible holster state machine. With a shield and weapon, the
 * shield owns the first 42% while holstering and the last 42% while drawing.
 */
public final class HolsterState {
    static final float SHIELD_STAGE_FRACTION = 0.42f;
    private static final float EPSILON = 0.0001f;

    private final float durationSeconds;
    private boolean desiredHolstered;
    private boolean targetPresent;
    private float weaponAmount;
    private float shieldAmount;

    public HolsterState(float durationSeconds) {
        this.durationSeconds = Math.max(0.1f, durationSeconds);
    }

    public synchronized boolean toggle(boolean hasTarget) {
        return requestHolstered(!desiredHolstered, hasTarget);
    }

    /** Returns false only when a holster request is blocked by a live target. */
    public synchronized boolean requestHolstered(boolean holstered, boolean hasTarget) {
        targetPresent = hasTarget;
        if (holstered && hasTarget) return false;
        desiredHolstered = holstered;
        return true;
    }

    /** Returns true when target acquisition forced an unholster transition. */
    public synchronized boolean updateTarget(boolean present) {
        boolean forced = present && !targetPresent && desiredHolstered;
        targetPresent = present;
        if (present) desiredHolstered = false;
        return forced;
    }

    public synchronized void advance(float deltaSeconds, boolean hasWeapon,
                                     boolean hasShield) {
        float dt = CameraMath.clamp(deltaSeconds, 0.0f, 0.1f);
        float endpoint = desiredHolstered ? 1.0f : 0.0f;
        if (!hasWeapon) weaponAmount = endpoint;
        if (!hasShield) shieldAmount = endpoint;
        if (desiredHolstered) advanceHolstering(dt, hasWeapon, hasShield);
        else advanceUnholstering(dt, hasWeapon, hasShield);
        if (finished()) {
            weaponAmount = endpoint;
            shieldAmount = endpoint;
        }
    }

    private void advanceHolstering(float dt, boolean hasWeapon, boolean hasShield) {
        if (hasWeapon && hasShield) {
            if (shieldAmount < 1.0f - EPSILON) {
                shieldAmount = approach(shieldAmount, 1.0f,
                        dt / (durationSeconds * SHIELD_STAGE_FRACTION));
            } else {
                weaponAmount = approach(weaponAmount, 1.0f,
                        dt / (durationSeconds * (1.0f - SHIELD_STAGE_FRACTION)));
            }
        } else if (hasWeapon) {
            weaponAmount = approach(weaponAmount, 1.0f, dt / durationSeconds);
        } else if (hasShield) {
            shieldAmount = approach(shieldAmount, 1.0f, dt / durationSeconds);
        }
    }

    private void advanceUnholstering(float dt, boolean hasWeapon, boolean hasShield) {
        if (hasWeapon && hasShield) {
            if (weaponAmount > EPSILON) {
                weaponAmount = approach(weaponAmount, 0.0f,
                        dt / (durationSeconds * (1.0f - SHIELD_STAGE_FRACTION)));
            } else {
                shieldAmount = approach(shieldAmount, 0.0f,
                        dt / (durationSeconds * SHIELD_STAGE_FRACTION));
            }
        } else if (hasWeapon) {
            weaponAmount = approach(weaponAmount, 0.0f, dt / durationSeconds);
        } else if (hasShield) {
            shieldAmount = approach(shieldAmount, 0.0f, dt / durationSeconds);
        }
    }

    private boolean finished() {
        float endpoint = desiredHolstered ? 1.0f : 0.0f;
        return Math.abs(weaponAmount - endpoint) <= EPSILON
                && Math.abs(shieldAmount - endpoint) <= EPSILON;
    }

    private static float approach(float value, float target, float delta) {
        if (value < target) return Math.min(target, value + delta);
        return Math.max(target, value - delta);
    }

    public synchronized void setHolsteredImmediately(boolean holstered) {
        desiredHolstered = holstered;
        weaponAmount = holstered ? 1.0f : 0.0f;
        shieldAmount = weaponAmount;
    }

    public synchronized boolean isDesiredHolstered() { return desiredHolstered; }
    public synchronized boolean isTargetPresent() { return targetPresent; }

    public synchronized HolsterSnapshot snapshot() {
        return new HolsterSnapshot(desiredHolstered, targetPresent,
                weaponAmount, shieldAmount);
    }
}
