package nutriquest;

/**
 * Double Points power-up: awards 2 points per food item for a fixed duration.
 */
public class DoublePointsPowerup implements Powerup {

    private static final float DURATION = 10f;
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

    @Override public String getDisplayName()    { return "2X PTS"; }
    @Override public float  getTimeRemaining()  { return timeRemaining; }
    public boolean isActive() { return active; }
}

