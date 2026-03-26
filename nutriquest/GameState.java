package nutriquest;

/**
 * GameState — single source of truth for all mutable game data.
 *
 * Extracts every field that was scattered across PlayableGame:
 * score, health, speed, powerup state, invincibility, banners, menus.
 * PlayableGame, GameRenderer, and collision handlers all read/write
 * this object instead of reaching into each other.
 */
public class GameState {

    // ── Window dimensions (read-only, set at startup) ────────────────
    public final int windowWidth;
    public final int windowHeight;

    // ── HP ───────────────────────────────────────────────────────────
    public static final int MAX_HEALTH         = 100;
    public static final int HP_LOSS_BAD_FOOD   = 20;
    public static final int HP_RESTORE_CORRECT = 5;
    public int playerHealth = MAX_HEALTH;

    // ── Score / session ──────────────────────────────────────────────
    public int   score     = 0;
    public int   highScore = 0;
    public float gameTime  = 0f;

    // ── Player speed ─────────────────────────────────────────────────
    public static final float PLAYER_BASE_SPEED  = 220f;
    public static final float PLAYER_SPEED_BOOST = 8f;
    public static final float PLAYER_MAX_SPEED   = 380f;
    public float playerCurrentSpeed = PLAYER_BASE_SPEED;

    // ── Bad-food speed (escalates after each quiz) ───────────────────
    public float badFoodBaseSpeed = 75f;

    // ── Invincibility ────────────────────────────────────────────────
    public static final float INVINCIBILITY_DURATION = 1.5f;
    public float invincibilityTimer = 0f;

    // ── Short-lived UI feedback ──────────────────────────────────────
    public float flashTimer = 0f;
    public float flashDuration = 0f;
    public int   flashR = 255;
    public int   flashG = 255;
    public int   flashB = 255;
    public int   flashMaxAlpha = 120;

    public String floatingText = "";
    public float  floatingTextX = 0f;
    public float  floatingTextY = 0f;
    public float  floatingTextTimer = 0f;
    public float  floatingTextDuration = 0f;
    public int    floatingTextR = 255;
    public int    floatingTextG = 255;
    public int    floatingTextB = 255;

    public boolean lastQuizCorrect = false;
    public float   quizResultBannerTimer = 0f;
    public float   quizGraceTimer = 0f;

    // ── Pac-Man mouth animation ───────────────────────────────────────
    public float pacmanMouthAngle = 45f;
    public float pacmanMouthDir   = -1f;
    public float pacmanFacing     = 0f;

    // ── Level-up banner ──────────────────────────────────────────────
    public float  levelUpBannerTimer = 0f;
    public String levelUpBannerText  = "";

    // ── Powerup availability ─────────────────────────────────────────
    public static final int[] POWERUP_THRESHOLDS = { 8, 20, 40 };
    public int     powerupsRedeemed = 0;
    public boolean powerupAvailable = false;

    // ── Active power-up objects (Strategy pattern) ───────────────────
    public FreezePowerup       freeze       = new FreezePowerup();
    public ShieldPowerup       shield       = new ShieldPowerup();
    public DoublePointsPowerup doublePoints = new DoublePointsPowerup();

    /** Returns the display name + timer of whichever timed power-up is currently running, or null. */
    public String getActivePowerupLabel() {
        if (freeze.isActive())       return freeze.getDisplayName()       + " " + (int) freeze.getTimeRemaining()       + "s";
        if (doublePoints.isActive()) return doublePoints.getDisplayName() + " " + (int) doublePoints.getTimeRemaining() + "s";
        if (shield.isActive())       return shield.getDisplayName();
        return null;
    }

    // ── Powerup popup ────────────────────────────────────────────────
    public boolean powerupPopupOpen = false;
    public int     powerupPopupSel  = 0;

    public static final String[] POWERUP_OPTS = {
        "Freeze bad food 5s", "Shield: block 1 hit", "Double Points 10s"
    };

    // ── Menu ─────────────────────────────────────────────────────────
    public int      menuSelection = 0;
    public boolean fullscreenEnabled = false; // kept for compatibility; fullscreen is forced on startup
    public String[] menuOptions   = {"Start Game", "How to Play", "Exit"};

    // ── Pause ────────────────────────────────────────────────────────
    public boolean paused = false;

    // ── Quiz bank ────────────────────────────────────────────────────
    public static final String[][] QUIZ = {
        {"Which vitamin is abundant in oranges?",   "Vitamin A","Vitamin C","Vitamin D","Vitamin K","1"},
        {"Which food is a good source of protein?", "Candy","Chicken","Soda","Chips","1"},
        {"What is a healthy breakfast choice?",     "Donut","Oatmeal","Fries","Cookie","1"},
        {"Which helps build strong bones?",         "Sugar","Calcium","Salt","Oil","1"},
        {"Which is a whole grain?",                 "White bread","Brown rice","Cake","Candy","1"},
        {"What should you drink most of?",          "Soda","Energy drink","Water","Milkshake","2"},
        {"Which food is high in fiber?",            "Ice cream","Broccoli","Candy","Chips","1"},
        {"What nutrient do carrots provide?",       "Protein","Vitamin A","Fat","Sugar","1"},
        {"Which is a healthy snack?",               "Apple","Cookie","Candy bar","Cake","0"},
        {"What does iron help with?",               "Taste","Blood health","Smell","Hair color","1"}
    };
    public int quizIndex = 0;

    // ── Constructor ──────────────────────────────────────────────────
    public GameState(int windowWidth, int windowHeight) {
        this.windowWidth  = windowWidth;
        this.windowHeight = windowHeight;
    }

    /** Reset everything to new-game defaults, preserving highScore. */
    public void reset() {
        playerHealth        = MAX_HEALTH;
        score               = 0;
        gameTime            = 0f;
        playerCurrentSpeed  = PLAYER_BASE_SPEED;
        badFoodBaseSpeed    = 75f;
        invincibilityTimer  = 0f;
        flashTimer          = 0f;
        flashDuration       = 0f;
        flashR              = 255;
        flashG              = 255;
        flashB              = 255;
        flashMaxAlpha       = 120;
        floatingText        = "";
        floatingTextX       = 0f;
        floatingTextY       = 0f;
        floatingTextTimer   = 0f;
        floatingTextDuration= 0f;
        floatingTextR       = 255;
        floatingTextG       = 255;
        floatingTextB       = 255;
        lastQuizCorrect     = false;
        quizResultBannerTimer = 0f;
        quizGraceTimer      = 0f;
        pacmanMouthAngle    = 45f;
        pacmanMouthDir      = -1f;
        pacmanFacing        = 0f;
        levelUpBannerTimer  = 0f;
        levelUpBannerText   = "";
        powerupsRedeemed    = 0;
        powerupAvailable    = false;
        powerupPopupOpen    = false;
        powerupPopupSel     = 0;
        quizIndex           = 0;
        paused              = false;
        menuSelection       = 0;
        // Reset powerup objects
        freeze.deactivate();
        shield.deactivate();
        doublePoints.deactivate();
    }

    /** Award points, respecting the double-points power-up. */
    public void addScore(int base) {
        score += doublePoints.isActive() ? base * 2 : base;
    }

    /** Tick all active timed power-ups; freeze flag drives food speed externally. */
    public void tickPowerups(float dt) {
        if (freeze.isActive()       && !freeze.tick(dt))       freeze.deactivate();
        if (doublePoints.isActive() && !doublePoints.tick(dt)) doublePoints.deactivate();
        // Shield has no timer — one-shot, deactivated on absorbHit()
    }

    /** Check thresholds and unlock a new power-up slot if score qualifies. */
    public void checkPowerupUnlock() {
        if (powerupsRedeemed < POWERUP_THRESHOLDS.length
                && score >= POWERUP_THRESHOLDS[powerupsRedeemed]) {
            powerupAvailable = true;
        }
    }

    public void triggerFlash(int r, int g, int b, int maxAlpha, float duration) {
        flashR = r;
        flashG = g;
        flashB = b;
        flashMaxAlpha = Math.max(0, maxAlpha);
        flashDuration = Math.max(0.01f, duration);
        flashTimer = flashDuration;
    }

    public void triggerFloatingText(String text, float x, float y, int r, int g, int b, float duration) {
        floatingText = text == null ? "" : text;
        floatingTextX = x;
        floatingTextY = y;
        floatingTextR = r;
        floatingTextG = g;
        floatingTextB = b;
        floatingTextDuration = Math.max(0.01f, duration);
        floatingTextTimer = floatingTextDuration;
    }

    public void triggerQuizResultBanner(boolean correct, float duration) {
        lastQuizCorrect = correct;
        quizResultBannerTimer = Math.max(0.01f, duration);
    }

    public void tickFeedback(float dt) {
        if (flashTimer > 0f) flashTimer -= dt;
        if (floatingTextTimer > 0f) {
            floatingTextTimer -= dt;
            floatingTextY -= 28f * dt;
        }
        if (quizResultBannerTimer > 0f) quizResultBannerTimer -= dt;
        if (quizGraceTimer > 0f) quizGraceTimer -= dt;
    }
}
