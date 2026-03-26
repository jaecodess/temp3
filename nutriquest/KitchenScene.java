package nutriquest;

import engine.managers.CollisionManager;
import engine.managers.EntityManager;
import engine.managers.InputOutputManager;
import engine.managers.MovementManager;
import java.awt.Color;

/**
 * Level 1 — Breakfast Cafeteria.
 * Calm starting level. Food moves slowly. Warm morning background.
 */
public class KitchenScene extends LevelScene {

    private static final LevelConfig CONFIG = new LevelConfig(
        1,
        "Level 1 — Breakfast",
        10,                         // advance at 10 points
        55f,                        // good food speed
        75f,                        // bad food speed
        20f,                        // speed bump per quiz hit
        new Color(255, 245, 215),   // warm morning cream top
        new Color(250, 220, 170),   // soft orange bottom
        new Color(220, 170, 100)    // accent: golden grid lines
    );

    public KitchenScene(EntityManager em, MovementManager mm,
                        CollisionManager cm, InputOutputManager io) {
        super("Level1", em, mm, cm, io);
    }

    @Override
    public LevelConfig getConfig() { return CONFIG; }
}

