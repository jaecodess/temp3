package nutriquest;

import engine.managers.CollisionManager;
import engine.managers.EntityManager;
import engine.managers.InputOutputManager;
import engine.managers.MovementManager;
import java.awt.Color;

/**
 * Level 3 — Dinner Cafeteria.
 * Hardest level. Food is very fast and punishing. Warm evening cafeteria.
 * No score threshold — survive as long as possible.
 */
public class JungleScene extends LevelScene {

    private static final LevelConfig CONFIG = new LevelConfig(
        3,
        "Level 3 — Dinner",
        Integer.MAX_VALUE,          // final level, no advance
        135f,                       // good food speed
        175f,                       // bad food speed
        40f,                        // speed bump per quiz hit
        new Color(80, 40, 20),      // warm dinner amber top
        new Color(40, 20, 10),      // deep evening bottom
        new Color(160, 90, 40)      // accent: warm orange grid
    );

    public JungleScene(EntityManager em, MovementManager mm,
                       CollisionManager cm, InputOutputManager io) {
        super("Level3", em, mm, cm, io);
    }

    @Override
    public LevelConfig getConfig() { return CONFIG; }
}

