package nutriquest;

/**
 * Freeze power-up: stops all bad food movement for a fixed duration.
 * The actual speed change is applied by GameState which owns the food lists;
 * this class only tracks active/expired state and remaining time.
 */
public class FreezePowerup implements Powerup {

    private static final float DURATION = 5f;
    private float timeRemaining = 0f;
    private boolean active = false;

    @Override
    public void activate() {
        timeRemaining = DURATION;
        active = true;
    }

    @Override
    public boolean tick(float dt) {
        if (!active) return false;
        timeRemaining -= dt;
        if (timeRemaining <= 0) {
            deactivate();
            return false;
        }
        return true;
    }

    @Override
    public void deactivate() {
        active = false;
        timeRemaining = 0f;
    }

    @Override public String getDisplayName()    { return "FREEZE"; }
    @Override public float  getTimeRemaining()  { return timeRemaining; }
    public boolean isActive() { return active; }
}

