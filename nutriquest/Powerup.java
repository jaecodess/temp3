package nutriquest;

/**
 * Strategy interface for player power-ups.
 * Each concrete implementation encapsulates one power-up's activation,
 * deactivation, tick logic, and display metadata — removing all
 * string-comparison dispatch from PlayableGame.
 */
public interface Powerup {

    /** Called once when the player confirms the power-up. */
    void activate();

    /**
     * Called every game frame while the power-up is active.
     * @param dt delta time in seconds
     * @return true while the power-up should remain active
     */
    boolean tick(float dt);

    /** Called when the power-up expires or is consumed. */
    void deactivate();

    /** Human-readable label shown in the HUD. */
    String getDisplayName();

    /** Remaining duration in seconds (0 for instant/one-shot power-ups). */
    float getTimeRemaining();
}

