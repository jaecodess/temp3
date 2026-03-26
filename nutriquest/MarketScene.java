package nutriquest;

import engine.managers.CollisionManager;
import engine.managers.EntityManager;
import engine.managers.InputOutputManager;
import engine.managers.MovementManager;
import java.awt.Color;

/**
 * Level 2 — Lunch Cafeteria.
 * Mid-game challenge. Food speeds up noticeably. Bright midday background.
 */
public class MarketScene extends LevelScene {

    private static final LevelConfig CONFIG = new LevelConfig(
        2,
        "Level 2 — Lunch",
        25,                         // advance at 25 points
        90f,                        // good food speed
        120f,                       // bad food speed
        30f,                        // speed bump per quiz hit
        new Color(200, 230, 255),   // bright sky cafeteria top
        new Color(130, 200, 180),   // mint green bottom
        new Color(60, 150, 130)     // accent: teal grid
    );

    public MarketScene(EntityManager em, MovementManager mm,
                       CollisionManager cm, InputOutputManager io) {
        super("Level2", em, mm, cm, io);
    }

    @Override
    public LevelConfig getConfig() { return CONFIG; }
}

