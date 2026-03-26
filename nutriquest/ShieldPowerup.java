package nutriquest;

/**
 * Shield power-up: absorbs the next bad-food hit instead of dealing damage.
 * One-shot — deactivates immediately on consumption.
 */
public class ShieldPowerup implements Powerup {

    private boolean active = false;

    @Override
    public void activate() {
        active = true;
    }

    @Override
    public boolean tick(float dt) {
        return active; // stays until consumed by a hit
    }

    @Override
    public void deactivate() {
        active = false;
    }

    /** Consume the shield on a hit; returns true if the hit was blocked. */
    public boolean absorbHit() {
        if (active) {
            deactivate();
            return true;
        }
        return false;
    }

    @Override public String getDisplayName()   { return "SHIELD"; }
    @Override public float  getTimeRemaining() { return 0f; } // no timer — one-shot
    public boolean isActive() { return active; }
}

