package nutriquest;

/**
 * PlayableGame — game loop, scene orchestration, input dispatch, collision wiring.
 *
 * All mutable game data lives in {@link GameState}.
 * All rendering lives in {@link GameRenderer}.
 * All power-up behaviour lives in {@link FreezePowerup}, {@link ShieldPowerup},
 * {@link DoublePointsPowerup} (each implementing {@link Powerup}).
 */
import engine.core.GameMaster;
import engine.entities.Player;
import engine.entities.Entity;
import engine.entities.TextureObject;
import engine.managers.*;
import engine.input.InputAction;
import engine.collision.ICollisionListener;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class PlayableGame extends JPanel implements KeyListener, MouseListener, MouseMotionListener, Runnable {

    private static final int    WINDOW_WIDTH  = 800;
    private static final int    WINDOW_HEIGHT = 600;
    private static final String TITLE         = "Nutrition Quest";
    private static final long   KEY_COOLDOWN  = 200;

    // Named sound effects.
    private static final String SOUND_BEGINNING      = "beginning";
    private static final String SOUND_CHOMP          = "chomp";
    private static final String SOUND_PICKUP_GOOD    = "pickup_good";
    private static final String SOUND_HIT_BAD        = "hit_bad";
    private static final String SOUND_SHIELD_BLOCK   = "shield_block";
    private static final String SOUND_QUIZ_CORRECT   = "quiz_correct";
    private static final String SOUND_QUIZ_WRONG     = "quiz_wrong";
    private static final String SOUND_LEVEL_UP       = "level_up";

    private static final float SOUND_BEGINNING_VOL = 0.7f;
    private static final float SOUND_CHOMP_VOL     = 0.9f;
    private static final float SOUND_PICKUP_VOL    = 0.95f;
    private static final float SOUND_HIT_BAD_VOL   = 0.70f;
    private static final float SOUND_BLOCK_VOL     = 0.85f;
    private static final float SOUND_QUIZ_VOL      = 0.85f;
    private static final float SOUND_LEVEL_UP_VOL  = 0.95f;

    private static final String SETTINGS_FILE_NAME = ".nutriquest.properties";

    // ── Food name pools ──────────────────────────────────────────────
    private static final String[] GOOD_NAMES = {"Apple","Banana","Broccoli","Carrot","Orange","Grapes","Spinach","Tomato"};
    private static final String[] GOOD_BEH   = {"wander","patrol","wander","patrol","wander","wander","patrol","wander"};
    private static final String[] BAD_NAMES  = {"Burger","Soda","Chips","Candy","Pizza","Donut"};
    private static final String[] BAD_BEH    = {"patrol","wander","patrol","wander","patrol","wander"};

    // ── Engine ───────────────────────────────────────────────────────
    private GameMaster         gameMaster;
    private SceneManager       sceneManager;
    private EntityManager      entityManager;
    private InputOutputManager inputManager;
    private MovementManager    movementManager;
    private CollisionManager   collisionManager;
    private TimeManager        timeManager;
    private Player             player;

    // ── Scenes ───────────────────────────────────────────────────────
    private engine.scene.MenuScene  menuScene;
    private engine.scene.EndScene   endScene;
    private KitchenScene            kitchenScene;
    private MarketScene             marketScene;
    private JungleScene             jungleScene;
    private LevelScene              currentLevelScene;
    private LevelCompleteScene      levelCompleteScene;
    private LevelScene              pendingNextLevel  = null;
    private String                  pendingNextBanner = null;

    // ── Entity lists ─────────────────────────────────────────────────
    private List<FoodItem> goodFoods = new ArrayList<>();
    private List<FoodItem> badFoods  = new ArrayList<>();

    // ── OOP-extracted collaborators ──────────────────────────────────
    private GameState    state;
    private GameRenderer renderer;

    // ── Loop ─────────────────────────────────────────────────────────
    private boolean running;
    private Thread  gameThread;
    private long    lastTime;
    private long    lastKeyPress = 0;
    private String  lastSceneName = null;
    private final Properties settings = new Properties();
    private File settingsFile;

    // ════════════════════════════════════════════════════════════════
    public PlayableGame() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        setDoubleBuffered(true);

        state = new GameState(WINDOW_WIDTH, WINDOW_HEIGHT);
        loadSettings();
        initEngine();
        initSounds();
        renderer = new GameRenderer(state, entityManager);
        renderer.setInputManager(inputManager);
        initScenes();
        renderer.setLevelCompleteScene(levelCompleteScene);
        renderer.setEndScene(endScene);
    }

    private void initEngine() {
        gameMaster       = new GameMaster(); gameMaster.initialize();
        sceneManager     = gameMaster.getSceneManager();
        entityManager    = gameMaster.getEntityManager();
        inputManager     = gameMaster.getInputOutputManager();
        movementManager  = gameMaster.getMovementManager();
        collisionManager = gameMaster.getCollisionManager();
        timeManager      = gameMaster.getTimeManager();
        inputManager.bindAction(InputAction.CONFIRM, KeyEvent.VK_ENTER);
        inputManager.bindAction(InputAction.ACTION_2, KeyEvent.VK_Q);
    }

    private void initScenes() {
        menuScene          = new engine.scene.MenuScene(inputManager);
        endScene           = new engine.scene.EndScene(inputManager);
        kitchenScene       = new KitchenScene(entityManager, movementManager, collisionManager, inputManager);
        marketScene        = new MarketScene (entityManager, movementManager, collisionManager, inputManager);
        jungleScene        = new JungleScene (entityManager, movementManager, collisionManager, inputManager);
        levelCompleteScene = new LevelCompleteScene(inputManager);
        sceneManager.addScene("MenuScene",     menuScene);
        sceneManager.addScene("EndScene",      endScene);
        sceneManager.addScene("LevelComplete", levelCompleteScene);
        sceneManager.addScene("Level1",        kitchenScene);
        sceneManager.addScene("Level2",        marketScene);
        sceneManager.addScene("Level3",        jungleScene);
        sceneManager.loadScene("MenuScene");
    }

    private void initSounds() {
        // Load named sounds with resilient relative-path probing.
        // If no sound files exist, Speaker.loadSound() falls back to a generated beep.
        loadSoundFromCandidates(SOUND_BEGINNING, new String[] {
                // When running from `Game/out`, the working directory is `out`.
                "../assets/sounds/pacman_beginning.wav",
                "assets/sounds/pacman_beginning.wav"
        });
        loadSoundFromCandidates(SOUND_CHOMP, new String[] {
                "../assets/sounds/pacman_chomp.wav",
                "assets/sounds/pacman_chomp.wav"
        });
        loadSoundFromCandidates(SOUND_PICKUP_GOOD, new String[] {
                "../assets/sounds/pacman_chomp.wav",
                "assets/sounds/pacman_chomp.wav"
        });
        loadSoundFromCandidates(SOUND_HIT_BAD, new String[] {
                "../assets/sounds/pacman_beginning.wav",
                "assets/sounds/pacman_beginning.wav"
        });
        loadSoundFromCandidates(SOUND_SHIELD_BLOCK, new String[] {
                "missing_shield_block"
        });
        loadSoundFromCandidates(SOUND_QUIZ_CORRECT, new String[] {
                "../assets/sounds/pacman_beginning.wav",
                "assets/sounds/pacman_beginning.wav"
        });
        loadSoundFromCandidates(SOUND_QUIZ_WRONG, new String[] {
                "missing_quiz_wrong"
        });
        loadSoundFromCandidates(SOUND_LEVEL_UP, new String[] {
                "../assets/sounds/pacman_beginning.wav",
                "assets/sounds/pacman_beginning.wav"
        });
    }

    private void loadSoundFromCandidates(String soundName, String[] candidates) {
        if (candidates == null || candidates.length == 0) {
            inputManager.getSpeaker().loadSound(soundName, "missing_" + soundName);
            return;
        }

        for (String p : candidates) {
            if (p == null) continue;
            try {
                File f = new File(p);
                if (f.exists() && f.isFile()) {
                    inputManager.getSpeaker().loadSound(soundName, p);
                    return;
                }
            } catch (Exception ignored) {
                // Keep trying other candidates.
            }
        }

        // Fallback: deliberately point to a non-existent file so Speaker generates a beep.
        inputManager.getSpeaker().loadSound(soundName, "missing_" + soundName);
    }

    private void loadSettings() {
        settingsFile = new File(System.getProperty("user.home"), SETTINGS_FILE_NAME);
        if (!settingsFile.exists()) return;

        try (FileInputStream in = new FileInputStream(settingsFile)) {
            settings.clear();
            settings.load(in);
            state.highScore = Integer.parseInt(settings.getProperty("highScore", "0"));
            state.fullscreenEnabled = Boolean.parseBoolean(settings.getProperty("fullscreenEnabled", "false"));
        } catch (Exception ignored) {
            state.highScore = Math.max(0, state.highScore);
        }
    }

    private void saveSettings() {
        if (settingsFile == null) settingsFile = new File(System.getProperty("user.home"), SETTINGS_FILE_NAME);
        settings.setProperty("highScore", Integer.toString(Math.max(0, state.highScore)));
        settings.setProperty("fullscreenEnabled", Boolean.toString(state.fullscreenEnabled));
        try (FileOutputStream out = new FileOutputStream(settingsFile)) {
            settings.store(out, "NutriQuest Settings");
        } catch (Exception ignored) {
            // Save failure should not interrupt gameplay.
        }
    }

    // ── New game ─────────────────────────────────────────────────────
    private void startNewGame() {
        int savedHigh = state.highScore;
        state.reset();
        state.highScore = savedHigh;
        timeManager.reset();
        timeManager.resume();
        kitchenScene.reset(); marketScene.reset(); jungleScene.reset();
        startLevel(kitchenScene);
        inputManager.getSpeaker().play(SOUND_BEGINNING, SOUND_BEGINNING_VOL);
    }

    private void startLevel(LevelScene level) {
        entityManager.clear(); collisionManager.clearAll();
        goodFoods.clear(); badFoods.clear();
        inputManager.getKeyboard().clearAll();
        for (Entity old : new ArrayList<>(movementManager.getEntities())) movementManager.removeEntity(old);

        currentLevelScene      = level;
        LevelConfig cfg        = level.getConfig();
        state.badFoodBaseSpeed = cfg.badFoodSpeed;
        state.invincibilityTimer = 0f;

        float px = WINDOW_WIDTH/2f, py = WINDOW_HEIGHT/2f;
        player = new Player(px, py, "Player");
        player.setSpeed(state.playerCurrentSpeed); player.setWidth(36); player.setHeight(36);
        entityManager.addEntity(player); movementManager.addEntity(player);
        collisionManager.addCollidable(player, "player");
        level.setPlayer(player);
        renderer.setPlayer(player);
        renderer.setCurrentLevelScene(level);
        renderer.setFoodLists(goodFoods, badFoods);

        Random rnd = new Random(); int pad = 70;
        for (int i = 0; i < GOOD_NAMES.length; i++) {
            float x, y;
            do { x=pad+rnd.nextFloat()*(WINDOW_WIDTH-2*pad); y=pad+rnd.nextFloat()*(WINDOW_HEIGHT-2*pad);
            } while (dist(x,y,px,py)<110f);
            spawnGood(x, y, GOOD_BEH[i], GOOD_NAMES[i], cfg.goodFoodSpeed);
        }
        for (int i = 0; i < BAD_NAMES.length; i++) {
            float x, y;
            do { x=pad+rnd.nextFloat()*(WINDOW_WIDTH-2*pad); y=pad+rnd.nextFloat()*(WINDOW_HEIGHT-2*pad);
            } while (dist(x,y,px,py)<110f);
            spawnBad(x, y, BAD_BEH[i], BAD_NAMES[i], state.badFoodBaseSpeed);
        }
        createWalls(); wirePlayerRefs(); setupCollisions();
        sceneManager.loadScene(level.getSceneName());
    }

    private void advanceToLevel(LevelScene next, String banner) {
        entityManager.clear(); collisionManager.clearAll();
        goodFoods.clear(); badFoods.clear();
        inputManager.getKeyboard().clearAll();
        for (Entity old : new ArrayList<>(movementManager.getEntities())) movementManager.removeEntity(old);

        next.reset();
        LevelConfig cfg        = next.getConfig();
        state.badFoodBaseSpeed = cfg.badFoodSpeed;

        float px = WINDOW_WIDTH/2f, py = WINDOW_HEIGHT/2f;
        player = new Player(px, py, "Player");
        player.setSpeed(state.playerCurrentSpeed); player.setWidth(36); player.setHeight(36);
        entityManager.addEntity(player); movementManager.addEntity(player);
        collisionManager.addCollidable(player, "player");
        next.setPlayer(player);
        renderer.setPlayer(player);
        renderer.setCurrentLevelScene(next);
        renderer.setFoodLists(goodFoods, badFoods);

        Random rnd = new Random(); int pad = 70;
        for (int i = 0; i < GOOD_NAMES.length; i++) {
            float x, y;
            do { x=pad+rnd.nextFloat()*(WINDOW_WIDTH-2*pad); y=pad+rnd.nextFloat()*(WINDOW_HEIGHT-2*pad);
            } while (dist(x,y,px,py)<110f);
            spawnGood(x, y, GOOD_BEH[i], GOOD_NAMES[i], cfg.goodFoodSpeed);
        }
        for (int i = 0; i < BAD_NAMES.length; i++) {
            float x, y;
            do { x=pad+rnd.nextFloat()*(WINDOW_WIDTH-2*pad); y=pad+rnd.nextFloat()*(WINDOW_HEIGHT-2*pad);
            } while (dist(x,y,px,py)<110f);
            spawnBad(x, y, BAD_BEH[i], BAD_NAMES[i], state.badFoodBaseSpeed);
        }
        createWalls(); wirePlayerRefs(); setupCollisions();
        currentLevelScene        = next;
        state.levelUpBannerText  = banner;
        state.levelUpBannerTimer = 2.5f;
        state.invincibilityTimer = 1.5f;
        sceneManager.loadScene(next.getSceneName());
    }

    // ── Entity spawning ──────────────────────────────────────────────
    private void spawnGood(float x, float y, String beh, String name, float speed) {
        FoodItem f = new FoodItem(x, y, beh, name, true);
        f.setWidth(56); f.setHeight(56); f.setSpeed(speed);
        f.setWorldBounds(WINDOW_WIDTH, WINDOW_HEIGHT);
        goodFoods.add(f); entityManager.addEntity(f); collisionManager.addCollidable(f, "good_food");
    }

    private void spawnBad(float x, float y, String beh, String name, float speed) {
        FoodItem f = new FoodItem(x, y, beh, name, false);
        f.setWidth(56); f.setHeight(56); f.setSpeed(speed);
        f.setWorldBounds(WINDOW_WIDTH, WINDOW_HEIGHT);
        badFoods.add(f); entityManager.addEntity(f); collisionManager.addCollidable(f, "bad_food");
    }

    private void wirePlayerRefs() {
        for (FoodItem bf : badFoods) bf.setPlayerRef(player);
    }

    private void spawnWall(float x, float y, float w, float h) {
        TextureObject wall = new TextureObject("wall", x, y, 1.0f);
        wall.setWidth(w); wall.setHeight(h);
        entityManager.addEntity(wall); collisionManager.addCollidable(wall, "wall");
    }

    private void createWalls() {
        spawnWall(200,200,80,24); spawnWall(400,160,24,80); spawnWall(580,220,80,24);
        spawnWall(140,360,24,90); spawnWall(640,340,24,90); spawnWall(340,440,110,24);
        spawnWall(100,130,60,20); spawnWall(640,130,60,20);
        spawnWall(100,490,60,20); spawnWall(640,490,60,20);
    }

    private float dist(float x1,float y1,float x2,float y2) {
        float dx=x1-x2, dy=y1-y2; return (float)Math.sqrt(dx*dx+dy*dy);
    }

    // ── Collision setup ──────────────────────────────────────────────
    private void setupCollisions() {
        collisionManager.registerHandler("player","good_food", new ICollisionListener() {
            @Override public void onCollision(Entity a, Entity b) {
                if (!(b instanceof FoodItem)) return;
                FoodItem f = (FoodItem) b;
                if (!f.isGoodFood() || !f.isActive()) return;
                state.addScore(1);
                inputManager.getSpeaker().play(SOUND_PICKUP_GOOD, SOUND_PICKUP_VOL);
                state.triggerFlash(80, 220, 120, 80, 0.12f);
                state.triggerFloatingText("+1", player.getX(), player.getY() - 26f, 90, 235, 130, 0.65f);
                state.playerCurrentSpeed = Math.min(GameState.PLAYER_MAX_SPEED, state.playerCurrentSpeed + GameState.PLAYER_SPEED_BOOST);
                if (player != null) player.setSpeed(state.playerCurrentSpeed);
                f.setActive(false); entityManager.removeEntity(f); collisionManager.removeCollidable(f); goodFoods.remove(f);
                LevelConfig cfg = currentLevelScene.getConfig();
                Random rnd = new Random(); int pad=70, idx=rnd.nextInt(GOOD_NAMES.length);
                float nx, ny;
                do { nx=pad+rnd.nextFloat()*(WINDOW_WIDTH-2*pad); ny=pad+rnd.nextFloat()*(WINDOW_HEIGHT-2*pad);
                } while (dist(nx,ny,player.getX(),player.getY())<110f);
                spawnGood(nx, ny, GOOD_BEH[idx], GOOD_NAMES[idx], cfg.goodFoodSpeed);
            }
        });

        collisionManager.registerHandler("player","bad_food", new ICollisionListener() {
            @Override public void onCollision(Entity a, Entity b) {
                if (!(b instanceof FoodItem)) return;
                FoodItem f = (FoodItem) b;
                if (f.isGoodFood() || !f.isActive()) return;
                if (state.invincibilityTimer > 0) return;

                inputManager.getSpeaker().play(SOUND_HIT_BAD, SOUND_HIT_BAD_VOL);

                // Delegate hit absorption to ShieldPowerup
                if (state.shield.absorbHit()) {
                    state.triggerFlash(110, 210, 255, 100, 0.16f);
                    state.triggerFloatingText("BLOCK!", player.getX(), player.getY() - 22f, 120, 220, 255, 0.8f);
                    inputManager.getSpeaker().play(SOUND_SHIELD_BLOCK, SOUND_BLOCK_VOL);
                    f.setActive(false); entityManager.removeEntity(f); collisionManager.removeCollidable(f); badFoods.remove(f);
                    state.invincibilityTimer = GameState.INVINCIBILITY_DURATION;
                    if (player != null) { player.setPosition(WINDOW_WIDTH/2f, WINDOW_HEIGHT/2f); player.getVelocity().set(0,0); }
                    Random rnd2 = new Random(); int pad2=80; float nx2, ny2;
                    do { nx2=pad2+rnd2.nextFloat()*(WINDOW_WIDTH-2*pad2); ny2=pad2+rnd2.nextFloat()*(WINDOW_HEIGHT-2*pad2);
                    } while (dist(nx2,ny2,WINDOW_WIDTH/2f,WINDOW_HEIGHT/2f)<180f);
                    int idx2=rnd2.nextInt(BAD_NAMES.length);
                    spawnBad(nx2, ny2, BAD_BEH[idx2], BAD_NAMES[idx2], state.badFoodBaseSpeed);
                    if (!badFoods.isEmpty()) badFoods.get(badFoods.size()-1).setPlayerRef(player);
                    return;
                }

                state.playerHealth -= GameState.HP_LOSS_BAD_FOOD;
                state.triggerFlash(255, 70, 70, 130, 0.20f);
                state.triggerFloatingText("-" + GameState.HP_LOSS_BAD_FOOD + " HP", player.getX(), player.getY() - 22f, 255, 110, 110, 0.9f);
                if (state.playerHealth <= 0) { state.playerHealth=0; currentLevelScene.setGameOver(true); return; }
                handleBadFoodTouch(f);
            }
        });

        ICollisionListener wallPush = new ICollisionListener() {
            @Override public void onCollision(Entity a, Entity b) {
                Entity mover = (b instanceof TextureObject) ? a : b;
                Entity wall  = (b instanceof TextureObject) ? b : a;
                float dx=mover.getX()-wall.getX(), dy=mover.getY()-wall.getY();
                float ovX=(mover.getWidth()/2+wall.getWidth()/2)-Math.abs(dx);
                float ovY=(mover.getHeight()/2+wall.getHeight()/2)-Math.abs(dy);
                if (ovX<=0||ovY<=0) return;
                if (ovX<ovY) { float push=(dx==0?1:Math.signum(dx))*ovX; mover.setPosition(mover.getX()+push,mover.getY()); }
                else         { float push=(dy==0?1:Math.signum(dy))*ovY; mover.setPosition(mover.getX(),mover.getY()+push); }
            }
        };
        collisionManager.registerHandler("player","wall",    wallPush);
        collisionManager.registerHandler("good_food","wall", wallPush);
        collisionManager.registerHandler("bad_food","wall",  wallPush);
    }

    private void handleBadFoodTouch(FoodItem f) {
        f.setActive(false); entityManager.removeEntity(f); collisionManager.removeCollidable(f); badFoods.remove(f);
        for (FoodItem bf : badFoods) bf.setSpeed(0f);
        inputManager.getKeyboard().clearAll();

        String[] q = GameState.QUIZ[state.quizIndex % GameState.QUIZ.length]; state.quizIndex++;
        String[] opts = {q[1],q[2],q[3],q[4]}; int correct = Integer.parseInt(q[5]);
        Frame frm = SwingUtilities.getWindowAncestor(this) instanceof Frame
                    ? (Frame)SwingUtilities.getWindowAncestor(this)
                    : (Frame.getFrames().length>0 ? Frame.getFrames()[0] : null);
        final boolean[] res = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    QuizDialog qd = new QuizDialog(frm, q[0], opts, correct);
                    qd.setAlwaysOnTop(true); qd.setVisible(true);
                    res[0] = qd.selectedAnswer == qd.correctIndex;
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        } catch (InterruptedException | InvocationTargetException ex) { ex.printStackTrace(); }

        inputManager.getKeyboard().clearAll();
        float cx=WINDOW_WIDTH/2f, cy=WINDOW_HEIGHT/2f;
        if (player != null) { player.setPosition(cx,cy); player.getVelocity().set(0,0); }
        state.invincibilityTimer = GameState.INVINCIBILITY_DURATION;
        state.quizGraceTimer = 0.9f;

        if (res[0]) {
            state.addScore(1);
            state.playerHealth = Math.min(GameState.MAX_HEALTH, state.playerHealth + GameState.HP_RESTORE_CORRECT);
            state.triggerQuizResultBanner(true, 1.2f);
            state.triggerFlash(95, 220, 120, 95, 0.15f);
            state.triggerFloatingText("Correct! +1 / +" + GameState.HP_RESTORE_CORRECT + " HP", cx, cy - 28f, 115, 240, 140, 1.2f);
            inputManager.getSpeaker().play(SOUND_QUIZ_CORRECT, SOUND_QUIZ_VOL);
        } else {
            state.playerHealth -= 10;
            state.triggerQuizResultBanner(false, 1.2f);
            state.triggerFlash(255, 90, 90, 115, 0.16f);
            state.triggerFloatingText("Wrong! -10 HP", cx, cy - 28f, 255, 130, 130, 1.2f);
            if (state.playerHealth<=0) {
                state.playerHealth=0;
                inputManager.getSpeaker().play(SOUND_QUIZ_WRONG, SOUND_QUIZ_VOL);
                currentLevelScene.setGameOver(true);
                return;
            }
            // Quiz resolved (wrong answer): cue the next action.
            inputManager.getSpeaker().play(SOUND_QUIZ_WRONG, SOUND_QUIZ_VOL);
        }

        LevelConfig cfg = currentLevelScene.getConfig();
        state.badFoodBaseSpeed += cfg.badFoodSpeedIncrement;
        for (FoodItem bf : new ArrayList<>(badFoods)) bf.setSpeed(state.badFoodBaseSpeed);

        Random rnd = new Random(); int pad=80; float nx, ny;
        do { nx=pad+rnd.nextFloat()*(WINDOW_WIDTH-2*pad); ny=pad+rnd.nextFloat()*(WINDOW_HEIGHT-2*pad);
        } while (dist(nx,ny,cx,cy)<180f);
        int idx=rnd.nextInt(BAD_NAMES.length);
        spawnBad(nx, ny, BAD_BEH[idx], BAD_NAMES[idx], state.badFoodBaseSpeed);
        if (!badFoods.isEmpty()) badFoods.get(badFoods.size()-1).setPlayerRef(player);
    }

    // ── Powerup popup ────────────────────────────────────────────────
    private void openPowerupPopup() {
        if (!state.powerupAvailable || state.powerupPopupOpen) return;
        state.powerupPopupOpen = true;
        state.powerupPopupSel  = 0;
        state.paused = true;
        timeManager.pause();
        inputManager.getKeyboard().clearAll();
    }

    private void confirmPowerupPopup() {
        state.powerupPopupOpen = false;
        state.paused           = false;
        state.powerupAvailable = false;
        state.powerupsRedeemed++;
        timeManager.resume();
        lastTime = System.nanoTime();
        switch (state.powerupPopupSel) {
            case 0: state.freeze.activate();       break;
            case 1: state.shield.activate();       break;
            case 2: state.doublePoints.activate(); break;
        }
        inputManager.getSpeaker().beep();
        inputManager.getKeyboard().clearAll();
    }

    private void cancelPowerupPopup() {
        state.powerupPopupOpen = false;
        state.paused = false;
        timeManager.resume();
        lastTime = System.nanoTime();
        inputManager.getKeyboard().clearAll();
    }

    // ── Game loop ────────────────────────────────────────────────────
    public void start() { if(running)return; running=true; gameThread=new Thread(this); gameThread.start(); }

    @Override public void run() {
        final long TARGET_NS = 1_000_000_000L / 60;
        lastTime = System.nanoTime();

        while (running) {
            long frameStart = System.nanoTime();
            float rawDt = (frameStart - lastTime) / 1_000_000_000.0f;
            lastTime = frameStart;
            rawDt = Math.min(rawDt, 0.05f);

            timeManager.update(rawDt);
            float dt = timeManager.getDeltaTime();
            inputManager.pollInput();
            sceneManager.update(dt);
            inputManager.processOutput();
            updateState(dt);
            checkTransitions();
            repaint();
            long sleepNs = TARGET_NS - (System.nanoTime()-frameStart);
            if (sleepNs>1_000_000L) { try{Thread.sleep(sleepNs/1_000_000L,(int)(sleepNs%1_000_000L));}catch(InterruptedException ignored){} }
        }
    }

    private void updateState(float dt) {
        engine.scene.Scene cur = sceneManager.getActiveScene(); if(cur==null) return;
        String n = cur.getSceneName();
        if (!"Level1".equals(n)&&!"Level2".equals(n)&&!"Level3".equals(n)) return;
        if (currentLevelScene==null||currentLevelScene.isGameOver()) return;

        state.gameTime = timeManager.getTotalTime();
        keepInBounds();
        if (state.invincibilityTimer > 0) state.invincibilityTimer -= dt;
        if (state.levelUpBannerTimer  > 0) state.levelUpBannerTimer -= dt;

        state.tickPowerups(dt);
        state.tickFeedback(dt);
        float badSpeed = (state.freeze.isActive() || state.quizGraceTimer > 0f) ? 0f : state.badFoodBaseSpeed;
        for (FoodItem bf : badFoods) bf.setSpeed(badSpeed);
        state.checkPowerupUnlock();
    }

    private void checkTransitions() {
        engine.scene.Scene cur = sceneManager.getActiveScene(); if(cur==null) return;
        String n = cur.getSceneName();

        // We rely on PlayableGame's `keyPressed()` for menu navigation.
        // Prevent any engine-driven "transitionRequested" state from auto-starting gameplay
        // (which could replay/leave the `beginning` sound on the menu).
        if ("MenuScene".equals(n) && !"MenuScene".equals(lastSceneName)) {
            inputManager.getSpeaker().stop(SOUND_BEGINNING);
        } else if (currentLevelScene!=null && currentLevelScene.isGameOver()) {
            state.gameTime = timeManager.getTotalTime();
            if (state.score > state.highScore) {
                state.highScore = state.score;
                saveSettings();
            }
            currentLevelScene = null;
            renderer.setCurrentLevelScene(null);
            endScene.setResults(state.score, state.gameTime);
            sceneManager.loadScene("EndScene");
        } else if ("Level1".equals(n) && state.score>=kitchenScene.getConfig().scoreToAdvance) {
            showLevelComplete(1,"Level 2 — Lunch");
        } else if ("Level2".equals(n) && state.score>=marketScene.getConfig().scoreToAdvance) {
            showLevelComplete(2,"Level 3 — Dinner");
        } else if ("LevelComplete".equals(n) && levelCompleteScene.isAdvanceRequested()) {
            if (pendingNextLevel!=null) {
                advanceToLevel(pendingNextLevel, pendingNextBanner);
                pendingNextLevel=null; pendingNextBanner=null;
            }
        } else if ("EndScene".equals(n)) {
            if (endScene.isRestartRequested())      { endScene.resetRequests(); startNewGame(); }
            else if (endScene.isExitRequested())    {
                endScene.resetRequests();
                // Leaving gameplay back to the main menu: stop the "beginning" cue.
                inputManager.getSpeaker().stop(SOUND_BEGINNING);
                sceneManager.loadScene("MenuScene");
            }
        }

        lastSceneName = n;
    }

    private void showLevelComplete(int completedLevel, String nextName) {
        if (completedLevel==1) { pendingNextLevel=marketScene; pendingNextBanner="Level 2 — Lunch!"; }
        else                   { pendingNextLevel=jungleScene;  pendingNextBanner="Level 3 — Dinner!"; }
        levelCompleteScene.prepare(state.score, completedLevel, nextName);
        inputManager.getSpeaker().play(SOUND_LEVEL_UP, SOUND_LEVEL_UP_VOL);
        state.triggerFlash(255, 220, 90, 90, 0.18f);
        state.triggerFloatingText("Level Up!", WINDOW_WIDTH / 2f, WINDOW_HEIGHT / 2f - 40f, 255, 225, 120, 1.0f);
        sceneManager.loadScene("LevelComplete");
    }

    private void keepInBounds() {
        if (player==null) return;
        float hw=player.getWidth()/2f, hh=player.getHeight()/2f, minY=55f+hh;
        if (player.getX()<hw)               player.setX(hw);
        if (player.getX()>WINDOW_WIDTH-hw)  player.setX(WINDOW_WIDTH-hw);
        if (player.getY()<minY)             player.setY(minY);
        if (player.getY()>WINDOW_HEIGHT-hh) player.setY(WINDOW_HEIGHT-hh);
    }

    // ── Rendering — delegates entirely to GameRenderer ────────────────
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        engine.scene.Scene cur = sceneManager.getActiveScene();
        String sceneName = (cur==null) ? "MenuScene" : cur.getSceneName();
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform old = g2.getTransform();

        // Scale the 800x600 logical render to the current window size.
        double sx = getWidth() / (double) WINDOW_WIDTH;
        double sy = getHeight() / (double) WINDOW_HEIGHT;
        double s = Math.min(sx, sy);
        double tx = (getWidth() - WINDOW_WIDTH * s) / 2.0;
        double ty = (getHeight() - WINDOW_HEIGHT * s) / 2.0;
        g2.translate(tx, ty);
        g2.scale(s, s);

        renderer.paint(g2, sceneName);
        g2.setTransform(old);
    }

    // ── Input ────────────────────────────────────────────────────────
    @Override public void keyPressed(KeyEvent e) {
        int k=e.getKeyCode(); long t=System.currentTimeMillis();
        inputManager.getKeyboard().keyDown(k);
        if(k==KeyEvent.VK_UP)   inputManager.getKeyboard().keyDown(87);
        if(k==KeyEvent.VK_DOWN) inputManager.getKeyboard().keyDown(83);
        if(k==KeyEvent.VK_LEFT) inputManager.getKeyboard().keyDown(65);
        if(k==KeyEvent.VK_RIGHT)inputManager.getKeyboard().keyDown(68);

        // Toggle fullscreen with F11 (and Alt+Enter as an extra option).
        if ((k == KeyEvent.VK_F11) || (k == KeyEvent.VK_ENTER && e.isAltDown())) {
            if (t - lastKeyPress >= KEY_COOLDOWN) {
                toggleFullscreen();
                inputManager.getSpeaker().beep();
                lastKeyPress = t;
            }
            return;
        }

        engine.scene.Scene cur = sceneManager.getActiveScene(); if(cur==null) return;
        String n = cur.getSceneName();

        if ("MenuScene".equals(n)) {
            if (t-lastKeyPress<KEY_COOLDOWN) return;
            if (inputManager.isPressed(InputAction.MOVE_UP))        { state.menuSelection=(state.menuSelection-1+state.menuOptions.length)%state.menuOptions.length; inputManager.getSpeaker().beep(); lastKeyPress=t; }
            else if (inputManager.isPressed(InputAction.MOVE_DOWN)) { state.menuSelection=(state.menuSelection+1)%state.menuOptions.length; inputManager.getSpeaker().beep(); lastKeyPress=t; }
            else if (inputManager.isPressed(InputAction.CONFIRM))   { handleMenu(); lastKeyPress=t; }

        } else if ("LevelComplete".equals(n)) {
            if (k==KeyEvent.VK_ENTER) inputManager.getSpeaker().play(SOUND_BEGINNING, SOUND_BEGINNING_VOL);
            if (k==KeyEvent.VK_ENTER||k==KeyEvent.VK_SPACE) levelCompleteScene.requestAdvance();

        } else if ("Level1".equals(n)||"Level2".equals(n)||"Level3".equals(n)) {
            if (state.powerupPopupOpen) {
                long now=System.currentTimeMillis();
                if (now-lastKeyPress<KEY_COOLDOWN) return;
                if      (k==KeyEvent.VK_UP||k==KeyEvent.VK_W)           { state.powerupPopupSel=(state.powerupPopupSel-1+GameState.POWERUP_OPTS.length)%GameState.POWERUP_OPTS.length; lastKeyPress=now; }
                else if (k==KeyEvent.VK_DOWN||k==KeyEvent.VK_S)         { state.powerupPopupSel=(state.powerupPopupSel+1)%GameState.POWERUP_OPTS.length; lastKeyPress=now; }
                else if (k==KeyEvent.VK_ENTER||k==KeyEvent.VK_SPACE)    { confirmPowerupPopup(); lastKeyPress=now; }
                else if (k==KeyEvent.VK_ESCAPE)                          { cancelPowerupPopup();  lastKeyPress=now; }
                return;
            }
            if (inputManager.isPressed(InputAction.PAUSE)) {
                if (state.paused) { state.paused=false; timeManager.resume(); lastTime=System.nanoTime(); }
                else              { state.paused=true;  timeManager.pause();  inputManager.getKeyboard().clearAll(); }
            } else if (inputManager.isPressed(InputAction.ACTION_2)&&state.paused) {
                state.paused=false; timeManager.resume(); inputManager.getKeyboard().clearAll();
                // Leaving gameplay back to the main menu: stop the "beginning" cue.
                inputManager.getSpeaker().stop(SOUND_BEGINNING);
                sceneManager.loadScene("MenuScene");
            } else if (k==KeyEvent.VK_P&&!state.paused&&state.powerupAvailable) {
                openPowerupPopup();
            }

        } else if ("EndScene".equals(n)) {
            if (k==KeyEvent.VK_SPACE||inputManager.isPressed(InputAction.CONFIRM)) { inputManager.getKeyboard().clearAll(); endScene.resetRequests(); startNewGame(); lastTime=System.nanoTime(); }
            else if (k==KeyEvent.VK_ESCAPE) {
                inputManager.getKeyboard().clearAll();
                endScene.resetRequests();
                timeManager.resume();
                // Leaving gameplay back to the main menu: stop the "beginning" cue.
                inputManager.getSpeaker().stop(SOUND_BEGINNING);
                sceneManager.loadScene("MenuScene");
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        int k=e.getKeyCode(); inputManager.getKeyboard().keyUp(k);
        if(k==KeyEvent.VK_UP)   inputManager.getKeyboard().keyUp(87);
        if(k==KeyEvent.VK_DOWN) inputManager.getKeyboard().keyUp(83);
        if(k==KeyEvent.VK_LEFT) inputManager.getKeyboard().keyUp(65);
        if(k==KeyEvent.VK_RIGHT)inputManager.getKeyboard().keyUp(68);
    }

    @Override public void keyTyped(KeyEvent e) {}

    private Point toLogicalPoint(int panelX, int panelY) {
        double sx = getWidth() / (double) WINDOW_WIDTH;
        double sy = getHeight() / (double) WINDOW_HEIGHT;
        double s = Math.min(sx, sy);
        if (s <= 0) return new Point(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);

        double tx = (getWidth() - WINDOW_WIDTH * s) / 2.0;
        double ty = (getHeight() - WINDOW_HEIGHT * s) / 2.0;
        int logicalX = (int) Math.round((panelX - tx) / s);
        int logicalY = (int) Math.round((panelY - ty) / s);

        logicalX = Math.max(0, Math.min(WINDOW_WIDTH, logicalX));
        logicalY = Math.max(0, Math.min(WINDOW_HEIGHT, logicalY));
        return new Point(logicalX, logicalY);
    }

    private void updateMouseTarget(MouseEvent e) {
        Point logical = toLogicalPoint(e.getX(), e.getY());
        inputManager.getMouse().mouseMoved(logical.x, logical.y);
    }

    private boolean isInsideRect(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    private boolean handleMenuMouseClick(int x, int y) {
        int btnW = 260;
        int btnStartY = 340;
        int btnSpacing = 54;
        int btnH = 42;
        int btnX = WINDOW_WIDTH / 2 - btnW / 2;

        for (int i = 0; i < state.menuOptions.length; i++) {
            int by = btnStartY + i * btnSpacing;
            if (isInsideRect(x, y, btnX, by, btnW, btnH)) {
                state.menuSelection = i;
                inputManager.getSpeaker().beep();
                handleMenu();
                return true;
            }
        }
        return false;
    }

    private boolean handleLevelCompleteMouseClick(int x, int y) {
        int btnW = 260;
        int btnH = 44;
        int btnX = WINDOW_WIDTH / 2 - btnW / 2;
        int btnY = 178 + 240 + 16;

        if (isInsideRect(x, y, btnX, btnY, btnW, btnH)) {
            levelCompleteScene.requestAdvance();
            inputManager.getSpeaker().play(SOUND_BEGINNING, SOUND_BEGINNING_VOL);
            return true;
        }
        return false;
    }

    private boolean handleEndSceneMouseClick(int x, int y) {
        int btnY = 120 + 248 + 20;
        int btnW = 210;
        int btnH = 44;
        int leftX = WINDOW_WIDTH / 2 - btnW - 8;
        int rightX = WINDOW_WIDTH / 2 + 8;

        if (isInsideRect(x, y, leftX, btnY, btnW, btnH)) {
            inputManager.getKeyboard().clearAll();
            endScene.resetRequests();
            startNewGame();
            lastTime = System.nanoTime();
            return true;
        }
        if (isInsideRect(x, y, rightX, btnY, btnW, btnH)) {
            inputManager.getKeyboard().clearAll();
            endScene.resetRequests();
            timeManager.resume();
            inputManager.getSpeaker().stop(SOUND_BEGINNING);
            sceneManager.loadScene("MenuScene");
            return true;
        }
        return false;
    }

    private boolean handlePowerupPopupMouseClick(int x, int y) {
        int cardX = WINDOW_WIDTH / 2 - 420 / 2;
        int cardY = WINDOW_HEIGHT / 2 - 280 / 2;
        int pX = cardX + 14;
        int pW = 420 - 28;

        for (int i = 0; i < GameState.POWERUP_OPTS.length; i++) {
            int rowY = cardY + 96 + i * 46;
            if (isInsideRect(x, y, pX + 16, rowY, pW - 32, 38)) {
                state.powerupPopupSel = i;
                confirmPowerupPopup();
                return true;
            }
        }
        return false;
    }

    private boolean handleMouseUiSelection(int x, int y, int button) {
        if (button != MouseEvent.BUTTON1) return false;
        engine.scene.Scene cur = sceneManager.getActiveScene();
        if (cur == null) return false;
        String n = cur.getSceneName();

        if ("MenuScene".equals(n)) return handleMenuMouseClick(x, y);
        if ("LevelComplete".equals(n)) return handleLevelCompleteMouseClick(x, y);
        if ("EndScene".equals(n)) return handleEndSceneMouseClick(x, y);
        if (("Level1".equals(n) || "Level2".equals(n) || "Level3".equals(n)) && state.powerupPopupOpen) {
            return handlePowerupPopupMouseClick(x, y);
        }
        return false;
    }

    @Override public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        updateMouseTarget(e);
        Point logical = toLogicalPoint(e.getX(), e.getY());
        inputManager.getMouse().touchDown(logical.x, logical.y, 0, e.getButton());

        handleMouseUiSelection(logical.x, logical.y, e.getButton());
    }

    @Override public void mouseReleased(MouseEvent e) {
        updateMouseTarget(e);
        Point logical = toLogicalPoint(e.getX(), e.getY());
        inputManager.getMouse().touchUp(logical.x, logical.y, 0, e.getButton());
    }

    @Override public void mouseDragged(MouseEvent e) {
        updateMouseTarget(e);
    }

    @Override public void mouseMoved(MouseEvent e) {
        updateMouseTarget(e);
    }

    @Override public void mouseClicked(MouseEvent e) {}

    @Override public void mouseEntered(MouseEvent e) {}

    @Override public void mouseExited(MouseEvent e) {}

    private void handleMenu() {
        switch (state.menuSelection) {
            case 0:
                startNewGame();
                lastTime=System.nanoTime();
                break;
            case 1:
                renderer.showHowToPlay(this);
                break;
            case 2:
                saveSettings();
                System.exit(0);
                break;
        }
    }

    // Windows fullscreen (toggle) without a menu button.
    // Uses the standard GraphicsDevice.setFullScreenWindow(...) method.
    private void setFullscreen(boolean enable) {
        JFrame frame = null;
        try {
            java.awt.Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof JFrame) frame = (JFrame) w;
        } catch (Exception ignored) {}
        if (frame == null) return;

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean previous = state.fullscreenEnabled;
        state.fullscreenEnabled = enable;
        try {
            frame.setVisible(false);
            if (enable) {
                frame.dispose();
                frame.setUndecorated(true);
                device.setFullScreenWindow(frame);
                frame.setVisible(true);
                frame.validate();
            } else {
                device.setFullScreenWindow(null);
                frame.dispose();
                frame.setUndecorated(false);
                frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.validate();
            }
            saveSettings();
        } catch (Exception ignored) {
            // If toggling fails, flip the boolean back to keep state consistent.
            state.fullscreenEnabled = previous;
        }
    }

    private void toggleFullscreen() {
        setFullscreen(!state.fullscreenEnabled);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(TITLE);
            PlayableGame game = new PlayableGame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    game.saveSettings();
                }
            });
            if (game.state.fullscreenEnabled) game.setFullscreen(true);
            game.requestFocusInWindow();
            game.start();
        });
    }
}
