package nutriquest;

import engine.entities.Entity;
import engine.entities.TextureObject;
import engine.managers.EntityManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * GameRenderer — responsible solely for painting every screen in Nutrition Quest.
 *
 * Extracted from PlayableGame to satisfy the Single Responsibility Principle.
 * It holds no mutable game state of its own; everything it needs is read from
 * the {@link GameState} snapshot passed at construction, plus the scene/entity
 * references injected via the update methods below.
 *
 * PlayableGame calls {@code renderer.paint(g2d, sceneName)} each frame from
 * {@code paintComponent()}; the renderer decides which draw-method to invoke.
 */
public class GameRenderer {

    private static final int W = 800;
    private static final int H = 600;

    // Pac-Man mouth animation: chomp while moving, nearly closed while idle.
    private static final float PACMAN_MOUTH_IDLE_DEG  = 6f;
    private static final float PACMAN_MOUTH_MIN_DEG   = 10f;
    private static final float PACMAN_MOUTH_MAX_DEG   = 46f;
    private static final float PACMAN_MOUTH_WIGGLE_HZ = 10.0f;

    // ── References injected by PlayableGame ──────────────────────────
    private final GameState        state;
    private final EntityManager    entityManager;
    private       LevelScene       currentLevelScene;
    private       LevelCompleteScene levelCompleteScene;
    private       engine.scene.EndScene endScene;
    private       engine.entities.Player player;
    private       float lastPlayerX;
    private       float lastPlayerY;
    private       boolean hasLastPlayerPos = false;
    private       List<FoodItem>   goodFoods;
    private       List<FoodItem>   badFoods;
    @SuppressWarnings("unused")
    private       engine.managers.InputOutputManager inputManager;

    public GameRenderer(GameState state, EntityManager entityManager) {
        this.state         = state;
        this.entityManager = entityManager;
    }

    // Setters called by PlayableGame whenever these references change ─
    public void setCurrentLevelScene(LevelScene s)          { this.currentLevelScene    = s; }
    public void setLevelCompleteScene(LevelCompleteScene s) { this.levelCompleteScene   = s; }
    public void setEndScene(engine.scene.EndScene s)        { this.endScene             = s; }
    public void setPlayer(engine.entities.Player p) {
        this.player = p;
        this.hasLastPlayerPos = false;
    }
    public void setFoodLists(List<FoodItem> good, List<FoodItem> bad) {
        this.goodFoods = good;
        this.badFoods  = bad;
    }
    public void setInputManager(engine.managers.InputOutputManager im) { this.inputManager = im; }

    // ── Entry point ──────────────────────────────────────────────────
    public void paint(Graphics2D g, String sceneName) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        switch (sceneName) {
            case "MenuScene":
                drawMenu(g); break;
            case "Level1": case "Level2": case "Level3":
                drawGame(g);
                if (state.paused && !state.powerupPopupOpen) drawPaused(g);
                if (state.powerupPopupOpen)  drawPowerupPopup(g);
                if (state.levelUpBannerTimer > 0) drawBanner(g);
                if (currentLevelScene != null && currentLevelScene.isGameOver()) drawGameOverOverlay(g);
                break;
            case "LevelComplete":
                drawLevelComplete(g); break;
            case "EndScene":
                drawEndScreen(g); break;
            default:
                drawMenu(g);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // MENU
    // ═════════════════════════════════════════════════════════════════
    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(26, 58, 26));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(34, 74, 34));
        for (int x = 0; x < W; x += 80) g.fillRect(x, 0, 40, H);

        for (int x = 0; x < W; x += 40) {
            g.setColor((x / 40) % 2 == 0 ? new Color(210, 180, 120) : new Color(190, 155, 95));
            g.fillRect(x, H - 36, 40, 36);
        }

        g.setColor(new Color(42, 90, 42));
        g.fillRect(0, 0, W, 22);
        int[] lightX = {60, 200, 360, 520, 680};
        for (int lx : lightX) {
            if (lx + 80 > W) continue;
            g.setColor(new Color(255, 248, 210));
            g.fillRoundRect(lx, 4, 80, 14, 6, 6);
            g.setColor(new Color(255, 248, 180, 30));
            g.fillRect(lx - 10, 18, 100, 20);
        }

        g.setColor(new Color(74, 52, 24));
        g.fillRect(0, 38, W, 52);
        g.setColor(new Color(90, 64, 30));
        g.fillRect(0, 84, W, 6);
        String[] shelfFoods = {"Apple","Broccoli","Banana","Carrot","Orange","Grapes","Spinach","Tomato"};
        for (int i = 0; i < 8; i++) drawShelfFoodItem(g, shelfFoods[i], 30 + i * 95, 44, 40, 40);

        g.setColor(new Color(40, 30, 10));
        g.fillRoundRect(58, 103, W - 116, 70, 10, 10);
        g.setColor(new Color(232, 192, 32));
        g.fillRoundRect(60, 105, W - 120, 66, 8, 8);
        g.setColor(new Color(184, 144, 10));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(60, 105, W - 120, 66, 8, 8);

        int[] dotX = {120, 260, 400, 540, 680};
        for (int dx : dotX) {
            if (dx > W - 60) continue;
            g.setColor(new Color(200, 0, 0));
            g.fillOval(dx - 8, 98, 16, 16);
            g.setColor(new Color(140, 0, 0));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(dx - 8, 98, 16, 16);
        }

        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.setColor(new Color(58, 32, 0));
        drawC(g, "NUTRITION QUEST", 143);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(new Color(90, 58, 0));
        drawC(g, "Collect healthy food  \u2022  Avoid junk  \u2022  Answer quizzes", 163);

        // ── Power-up logo bubbles (menu decoration) ─────────────────
        // Place them in the empty side areas next to the menu buttons.
        // "Lightly floating" uses a tiny time-based wobble.
        long now = System.currentTimeMillis();
        int rMain = 16; // bigger side bubbles
        int yStart = 324; // keep them clear of the first button
        int xLeft  = 100;
        int xRight = W - 100;

        int wob1 = (int)(Math.sin(now * 0.004 + 0.0) * 3);
        int wob2 = (int)(Math.sin(now * 0.004 + 1.2) * 3);
        int wob3 = (int)(Math.sin(now * 0.004 + 2.4) * 2);

        // Double Points ("2X") on the left, Freeze on the right.
        drawPowerupBubble(g, xLeft,  yStart + wob1, rMain,
                new Color(232,170, 20,  135), new Color(180,120,10, 230), "double");
        drawPowerupBubble(g, xRight, yStart + wob2, rMain,
                new Color(60, 140, 220, 120), new Color(20, 70, 155, 220), "freeze");

        // Small Shield bubble floating in the gap between "Start Game" and "How to Play".
        int rSmall = 8;
        int yGap = 384; // between button rows, avoid overlap
        drawPowerupBubble(g, 210, yGap + wob3, rSmall,
                new Color(60, 190, 80,  120), new Color(25,120, 50, 220), "shield");

        int cardW = 168, cardH = 130, cardY = 188;
        int cardStartX = (W - (cardW * 3 + 24)) / 2;
        String[] aisleNums = {"AISLE 1","AISLE 2","AISLE 3"};
        String[] mealNames = {"BREAKFAST","LUNCH","DINNER"};
        String[] mealDesc  = {"Slow & steady","Medium pace","Fast chaos"};
        String[] lvlLabel  = {"Lv1 - Breakfast","Lv2 - Lunch","Lv3 - Dinner"};
        Color[] cardBg  = {new Color(255,248,232),new Color(232,248,232),new Color(234,232,248)};
        Color[] cardBdr = {new Color(232,160,32), new Color(64,168,64),  new Color(96,64,192)};
        Color[] cardHdr = {new Color(232,160,32), new Color(64,168,64),  new Color(96,64,192)};
        Color[] cardTxt = {new Color(58,32,0),    new Color(10,58,10),   new Color(42,16,96)};

        for (int i = 0; i < 3; i++) {
            int cx = cardStartX + i * (cardW + 12);
            g.setColor(new Color(0,0,0,60));
            g.fillRoundRect(cx+4, cardY+4, cardW, cardH, 12, 12);
            g.setColor(cardBg[i]);
            g.fillRoundRect(cx, cardY, cardW, cardH, 12, 12);
            g.setColor(cardBdr[i]);
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(cx, cardY, cardW, cardH, 12, 12);
            g.setColor(cardHdr[i]);
            g.fillRoundRect(cx, cardY, cardW, 26, 12, 12);
            g.fillRect(cx, cardY+14, cardW, 12);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.setColor(i==2 ? new Color(224,216,255) : cardTxt[i]);
            drawCInRect(g, aisleNums[i], cx, cardY+17, cardW);
            drawMealIcon(g, i, cx+cardW/2, cardY+60);
            g.setFont(new Font("Arial", Font.BOLD, 13));
            g.setColor(cardTxt[i]);
            drawCInRect(g, mealNames[i], cx, cardY+88, cardW);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.setColor(cardTxt[i].brighter());
            drawCInRect(g, mealDesc[i], cx, cardY+102, cardW);
            g.setColor(cardHdr[i]);
            g.fillRoundRect(cx+14, cardY+110, cardW-28, 16, 6, 6);
            g.setFont(new Font("Arial", Font.BOLD, 9));
            g.setColor(i==2 ? new Color(224,216,255) : cardTxt[i]);
            drawCInRect(g, lvlLabel[i], cx, cardY+122, cardW);
        }

        int btnW = 260, btnStartY = 340, btnSpacing = 54;
        int btnX = W/2 - btnW/2;
        for (int i = 0; i < state.menuOptions.length; i++) {
            int bY = btnStartY + i * btnSpacing;
            boolean selected = (i == state.menuSelection);
            g.setColor(new Color(0,0,0,70));
            g.fillRoundRect(btnX+4, bY+4, btnW, 42, 10, 10);
            if (selected) {
                g.setColor(new Color(232,192,32));
                g.fillRoundRect(btnX, bY, btnW, 42, 10, 10);
                g.setColor(new Color(184,144,10));
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(btnX, bY, btnW, 42, 10, 10);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.setColor(new Color(58,32,0));
            } else {
                // i==1 is "How to Play" (green). Other buttons keep warm/brown theme.
                g.setColor(i==1 ? new Color(74,138,74) : new Color(90,64,32));
                g.fillRoundRect(btnX, bY, btnW, 42, 10, 10);
                g.setColor(i==1 ? new Color(42,106,42) : new Color(58,42,16));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(btnX, bY, btnW, 42, 10, 10);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                g.setColor(i==1 ? new Color(232,255,232) : new Color(232,216,184));
            }
            drawC(g, state.menuOptions[i], bY+27);
        }

        if (state.highScore > 0) {
            g.setColor(new Color(232,192,32));
            g.setFont(new Font("Arial", Font.BOLD, 14));
            drawC(g, "\u25cf Best Score: " + state.highScore, 514);
        }

        g.setColor(new Color(58,42,16));
        g.fillRect(0, H-72, W, 36);
        g.setColor(new Color(90,64,30));
        g.fillRect(0, H-73, W, 4);
        String[] junkNames = {"Burger","Soda","Chips","Candy","Pizza","Donut"};
        for (int i = 0; i < junkNames.length; i++) drawShelfFoodItem(g, junkNames[i], 30+i*130, H-70, 32, 32);
        g.setFont(new Font("Arial", Font.ITALIC, 10));
        g.setColor(Color.BLACK);
        drawC(g, "--- junk food zone: avoid! ---", H-24);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.setColor(Color.BLACK);
        drawC(g, "^ v navigate     ENTER select", H-10);
    }

    private void drawPowerupBubble(Graphics2D g, int cx, int cy, int r, Color fill, Color border, String iconType) {
        // Shadow layer (gives a "bubble" look)
        g.setColor(new Color(0, 0, 0, 55));
        g.fillOval(cx - r + 2, cy - r + 4, r * 2, r * 2);

        // Bubble body
        g.setColor(fill);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Border ring
        g.setColor(border);
        g.setStroke(new BasicStroke(3));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);

        // Outer rim (makes it more obvious on busy backgrounds)
        g.setColor(new Color(255, 255, 255, 40));
        g.setStroke(new BasicStroke(4));
        g.drawOval(cx - r - 1, cy - r - 1, r * 2 + 2, r * 2 + 2);

        // Highlight
        g.setColor(new Color(255, 255, 255, Math.min(190, fill.getAlpha())));
        g.fillOval(cx - (int)(r * 0.58), cy - (int)(r * 0.62), (int)(r * 1.16), (int)(r * 0.64));

        // Icon (simple vector so it doesn't depend on external images)
        g.setColor(new Color(0, 0, 0, 150));
        if ("freeze".equals(iconType)) {
            int s = (int)(r * 0.85);
            for (int i = 0; i < 3; i++) {
                double ang = i * Math.PI / 3.0;
                int x = cx + (int)(Math.cos(ang) * s);
                int y = cy + (int)(Math.sin(ang) * s);
                int x2 = cx - (int)(Math.cos(ang) * s);
                int y2 = cy - (int)(Math.sin(ang) * s);
                g.drawLine(x2, y2, x, y);
            }
            g.fillOval(cx - r/6, cy - r/6, r/3, r/3);
        } else if ("shield".equals(iconType)) {
            // A minimal shield: top arc + pointed bottom.
            int topW = (int)(r * 1.15);
            int topH = (int)(r * 0.90);
            int[] xs = {cx - topW/2, cx + topW/2, cx + topW/2, cx, cx - topW/2};
            int[] ys = {cy - topH,   cy - topH,   cy + topH/2, cy + topH, cy + topH/2};
            g.drawPolygon(xs, ys, xs.length);
            g.drawLine(cx - r/4, cy - topH/4, cx + r/4, cy + topH/3);
        } else {
            // double
            g.setFont(new Font("Arial", Font.BOLD, (int)(r * 1.6)));
            g.drawString("2X", cx - g.getFontMetrics().stringWidth("2X")/2, cy + r/2 - 1);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // GAME SCREEN
    // ═════════════════════════════════════════════════════════════════
    private void drawGame(Graphics2D g) {
        LevelConfig cfg = currentLevelScene != null ? currentLevelScene.getConfig() : null;
        if (cfg != null) {
            GradientPaint gp = new GradientPaint(0,0,cfg.bgTop,0,H,cfg.bgBottom);
            g.setPaint(gp); g.fillRect(0,0,W,H);
            g.setColor(new Color(cfg.accentColor.getRed(),cfg.accentColor.getGreen(),cfg.accentColor.getBlue(),50));
            g.setStroke(new BasicStroke(1));
            for (int x=0;x<W;x+=50) g.drawLine(x,55,x,H);
            for (int y=55;y<H;y+=50) g.drawLine(0,y,W,y);
            drawDecorations(g,cfg);
        }
        for (Entity e : entityManager.getActiveEntities()) {
            if (e instanceof TextureObject) drawWall(g, e);
        }
        if (goodFoods != null) for (FoodItem f : goodFoods) if (f!=null&&f.isActive()) drawFood(g,f);
        if (badFoods  != null) for (FoodItem f : badFoods)  if (f!=null&&f.isActive()) drawFood(g,f);
        if (player != null && player.isActive()) drawPlayer(g);
        drawTransientGameplayFeedback(g);
        drawHUD(g);
        drawFlashOverlay(g);
    }

    private void drawHUD(Graphics2D g) {
        g.setColor(new Color(0,0,0,210)); g.fillRect(0,0,W,52);
        g.setColor(new Color(255,255,255,25)); g.drawLine(0,52,W,52);

        g.setFont(new Font("Arial",Font.BOLD,17)); g.setColor(Color.WHITE);
        g.drawString("Score: " + state.score, 14, 33);

        if (currentLevelScene != null) {
            g.setFont(new Font("Arial",Font.BOLD,14)); g.setColor(new Color(200,230,255));
            drawC(g, currentLevelScene.getConfig().title, 32);
            int next = currentLevelScene.getConfig().scoreToAdvance;
            if (next < Integer.MAX_VALUE) {
                g.setFont(new Font("Arial",Font.PLAIN,11)); g.setColor(new Color(170,170,170));
                g.drawString("Next lv: " + state.score + "/" + next, 14, 48);
            }
        }

        int hbX=W-220, hbY=12, hbW=170, hbH=20;
        g.setColor(new Color(40,40,40)); g.fillRoundRect(hbX,hbY,hbW,hbH,8,8);
        Color hc = state.playerHealth>60 ? new Color(60,210,90)
                 : state.playerHealth>30 ? new Color(240,180,30) : new Color(220,50,50);
        g.setColor(hc); g.fillRoundRect(hbX,hbY,(int)(hbW*state.playerHealth/(float)GameState.MAX_HEALTH),hbH,8,8);
        g.setColor(new Color(255,255,255,100)); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(hbX,hbY,hbW,hbH,8,8);
        g.setFont(new Font("Arial",Font.BOLD,12)); g.setColor(Color.WHITE);
        g.drawString("HP " + state.playerHealth + "/" + GameState.MAX_HEALTH, hbX+4, hbY+14);

        if (state.powerupAvailable) {
            g.setColor(new Color(255,220,50)); g.setFont(new Font("Arial",Font.BOLD,13));
            g.drawString("[P] Powerup!", W/2+70, 33);
        }
        String puLabel = state.getActivePowerupLabel();
        if (puLabel != null) {
            g.setColor(new Color(100,220,255)); g.setFont(new Font("Arial",Font.BOLD,12));
            g.drawString(puLabel, W/2+70, 48);
        }
        if (state.invincibilityTimer > 0) {
            g.setFont(new Font("Arial",Font.BOLD,13));
            g.setColor(new Color(255,220,50,220));
            g.drawString("** SAFE " + String.format("%.1f", state.invincibilityTimer) + "s", 14, 48);
        }
        if (state.playerCurrentSpeed > GameState.PLAYER_BASE_SPEED + 1f) {
            int boosts = (int)((state.playerCurrentSpeed - GameState.PLAYER_BASE_SPEED) / GameState.PLAYER_SPEED_BOOST);
            g.setFont(new Font("Arial",Font.BOLD,12));
            g.setColor(new Color(100,240,255,220));
            g.drawString(">> Speed +" + boosts, W-215, 48);
        }
    }

    private void drawDecorations(Graphics2D g, LevelConfig cfg) {
        switch(cfg.levelNumber) {
            case 1:
                for (int tx=0;tx<W;tx+=80) for (int ty=55;ty<H;ty+=80) {
                    if(((tx/80)+(ty/80))%2==0){g.setColor(new Color(255,240,200,30));g.fillRect(tx,ty,80,80);}
                }
                g.setColor(new Color(255,240,120,18));
                for (int ray=0;ray<6;ray++){int[] xs={0,0,200+ray*90},ys={55,55+ray*60,55};g.fillPolygon(xs,ys,3);}
                g.setColor(new Color(255,220,190,110));
                g.fillRoundRect(0,H-90,W,40,0,0);
                g.setColor(new Color(230,180,130,80));
                g.fillRect(0,H-95,W,6);
                int[] toastX={60,200,400,620,760};
                for (int tx2:toastX){g.setColor(new Color(220,170,90,130));g.fillRoundRect(tx2-12,H-86,24,20,4,4);g.setColor(new Color(180,120,50,100));g.fillRoundRect(tx2-10,H-84,20,4,2,2);}
                int[] plateX={130,320,520,700};
                for (int px:plateX){g.setColor(new Color(255,255,255,90));g.fillOval(px-14,H-84,28,18);g.setColor(new Color(200,200,200,70));g.setStroke(new BasicStroke(1));g.drawOval(px-14,H-84,28,18);}
                int[] winX={50,250,550,700};
                for (int wx:winX){g.setColor(new Color(255,245,180,45));g.fillRoundRect(wx,70,55,45,6,6);g.setColor(new Color(255,220,100,30));g.fillRoundRect(wx+4,74,47,37,4,4);g.setColor(new Color(200,160,80,40));g.setStroke(new BasicStroke(1.5f));g.drawRoundRect(wx,70,55,45,6,6);g.setColor(new Color(200,160,80,30));g.drawLine(wx+27,70,wx+27,115);g.drawLine(wx,92,wx+55,92);}
                break;
            case 2:
                g.setColor(new Color(100,180,255,22));
                for (int ty=80;ty<H;ty+=70){g.setStroke(new BasicStroke(2));g.drawLine(0,ty,W,ty);}
                g.setColor(new Color(150,200,255,55));
                g.fillRoundRect(0,H-85,W,35,0,0);
                g.setColor(new Color(100,160,220,50));
                g.fillRect(0,H-88,W,4);
                int[] trayX2={40,180,350,530,680,760};
                for (int txv:trayX2){g.setColor(new Color(255,255,255,100));g.fillOval(txv-16,H-82,32,18);g.setColor(new Color(180,210,240,80));g.drawOval(txv-16,H-82,32,18);g.setColor(new Color(255,200,100,90));g.fillOval(txv-10,H-80,20,10);}
                g.setColor(new Color(60,80,60,110));
                g.fillRoundRect(300,62,200,70,8,8);
                g.setColor(new Color(80,110,80,90));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(300,62,200,70,8,8);
                g.setFont(new Font("Arial",Font.BOLD,9));
                g.setColor(new Color(220,240,200,130));
                g.drawString("TODAY'S MENU",348,78);
                g.setFont(new Font("Arial",Font.PLAIN,8));
                g.setColor(new Color(200,220,180,100));
                g.drawString("\u2665 Fresh Veggies",326,92);
                g.drawString("\u2665 Fruit Salad",326,104);
                g.drawString("\u2665 Whole Grain",326,116);
                g.setColor(new Color(255,240,200,40));
                g.fillRoundRect(560,64,160,60,6,6);
                g.setColor(new Color(220,180,100,50));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(560,64,160,60,6,6);
                break;
            case 3:
                int[] lampX={100,300,500,700};
                for (int lx2:lampX){for (int r2=60;r2>=10;r2-=10){int alpha=8+(60-r2)/4;g.setColor(new Color(255,200,80,alpha));g.fillOval(lx2-r2,55,r2*2,r2*2);}}
                for (int lx3:lampX){g.setColor(new Color(80,60,40,180));g.setStroke(new BasicStroke(2));g.drawLine(lx3,55,lx3,85);int[] lsx={lx3-14,lx3+14,lx3+8,lx3-8},lsy={85,85,105,105};g.setColor(new Color(200,140,60,200));g.fillPolygon(lsx,lsy,4);g.setColor(new Color(240,200,100,160));g.setStroke(new BasicStroke(1));g.drawPolygon(lsx,lsy,4);g.setColor(new Color(255,240,150,180));g.fillOval(lx3-5,87,10,10);}
                int[] tableX={60,300,540};
                for (int tbx:tableX){g.setColor(new Color(180,60,60,40));g.fillRoundRect(tbx,H-100,160,14,4,4);g.setColor(new Color(220,100,100,30));g.setStroke(new BasicStroke(1));g.drawRoundRect(tbx,H-100,160,14,4,4);g.setColor(new Color(255,250,240,170));g.fillRoundRect(tbx+72,H-118,8,20,2,2);g.setColor(new Color(255,200,50,200));g.fillOval(tbx+73,H-122,6,7);g.setColor(new Color(255,160,0,120));g.fillOval(tbx+71,H-125,10,8);}
                g.setColor(new Color(180,120,60,20));
                for (int fx3=0;fx3<W;fx3+=60) for (int fy=55;fy<H;fy+=60){if(((fx3/60)+(fy/60))%2==0)g.fillRect(fx3,fy,60,60);}
                break;
        }
        g.setStroke(new BasicStroke(1));
    }

    private void drawFood(Graphics2D g, FoodItem f) {
        int x=(int)(f.getX()-f.getWidth()/2), y=(int)(f.getY()-f.getHeight()/2);
        int w=(int)f.getWidth(), h=(int)f.getHeight();
        Color glow=f.isGoodFood()?new Color(80,220,100,55):new Color(220,70,70,55);
        g.setColor(glow); g.fillOval(x-5,y-5,w+10,h+10);
        Image img=FoodImageLoader.getFoodImage(f.getFoodName());
        if (img!=null) {
            g.drawImage(img,x,y,w,h,null);
        } else {
            Color fill=f.isGoodFood()?new Color(60,190,80):new Color(220,70,70);
            g.setColor(fill);g.fillOval(x,y,w,h);g.setColor(fill.darker());
            g.setStroke(new BasicStroke(2));g.drawOval(x,y,w,h);
            g.setColor(Color.WHITE);g.setFont(new Font("Arial",Font.BOLD,10));
            String nm=f.getFoodName();g.drawString(nm,x+(w-g.getFontMetrics().stringWidth(nm))/2,y+h/2+4);
        }
        Color border=f.isGoodFood()?new Color(60,200,80,150):new Color(220,60,60,150);
        g.setColor(border);g.setStroke(new BasicStroke(2.5f));g.drawOval(x-1,y-1,w+2,h+2);
    }

    private void drawWall(Graphics2D g, Entity e) {
        if (!e.isActive()) return;
        int x=(int)(e.getX()-e.getWidth()/2), y=(int)(e.getY()-e.getHeight()/2);
        int w=(int)e.getWidth(), h=(int)e.getHeight();
        Color wallFill, wallBorder;
        if (currentLevelScene != null) {
            int lvl=currentLevelScene.getConfig().levelNumber;
            if      (lvl==1){wallFill=new Color(160,120,70,200);wallBorder=new Color(130,95,50,255);}
            else if (lvl==2){wallFill=new Color(80,110,60,200); wallBorder=new Color(55,85,35,255);}
            else             {wallFill=new Color(30,60,30,210);  wallBorder=new Color(20,45,20,255);}
        } else {wallFill=new Color(100,100,100,200);wallBorder=new Color(70,70,70,255);}
        g.setColor(new Color(0,0,0,60)); g.fillRoundRect(x+3,y+3,w,h,6,6);
        g.setColor(wallFill); g.fillRoundRect(x,y,w,h,6,6);
        g.setColor(new Color(255,255,255,35)); g.fillRoundRect(x,y,w,h/3,6,6);
        g.setColor(wallBorder); g.setStroke(new BasicStroke(2)); g.drawRoundRect(x,y,w,h,6,6);
    }

    private void drawPlayer(Graphics2D g) {
        float currX = player.getX();
        float currY = player.getY();
        int cx=(int)currX, cy=(int)currY;
        int r=(int)(player.getWidth()/2);
        if (!hasLastPlayerPos) {
            lastPlayerX = currX;
            lastPlayerY = currY;
            hasLastPlayerPos = true;
        }

        // When invincible, we blink by skipping every other render.
        // Still update cached position so facing/mouth logic doesn't "jump".
        if (state.invincibilityTimer > 0 && ((int)(state.invincibilityTimer*6.66f)%2==0)) {
            lastPlayerX = currX;
            lastPlayerY = currY;
            return;
        }

        float dx = currX - lastPlayerX;
        float dy = currY - lastPlayerY;
        float moveSq = dx * dx + dy * dy;
        boolean moving = moveSq > 0.001f;
        if (moving) {
            // Convert to screen-friendly angle: 0=right, 90=up, -90=down.
            state.pacmanFacing = (float) Math.toDegrees(Math.atan2(-dy, dx));
        }

        if (moving) {
            float osc = (float) ((Math.sin(state.gameTime * PACMAN_MOUTH_WIGGLE_HZ) + 1f) * 0.5f);
            state.pacmanMouthAngle = PACMAN_MOUTH_MIN_DEG
                    + (PACMAN_MOUTH_MAX_DEG - PACMAN_MOUTH_MIN_DEG) * osc;
        } else {
            state.pacmanMouthAngle = PACMAN_MOUTH_IDLE_DEG;
        }

        g.setColor(new Color(0,0,0,60)); g.fillOval(cx-r+3,cy+r-4,r*2,8);
        int startAngle = (int) (state.pacmanFacing + state.pacmanMouthAngle / 2f);
        int arcExtent = (int) (360 - state.pacmanMouthAngle);

        // Pac-Man body (eyeless): yellow wedge.
        g.setColor(new Color(255, 220, 50));
        g.fillArc(cx-r, cy-r, r*2, r*2, startAngle, arcExtent);
        g.setColor(new Color(220, 170, 0));
        g.setStroke(new BasicStroke(2f));
        g.drawArc(cx-r, cy-r, r*2, r*2, startAngle, arcExtent);
        double edge1 = Math.toRadians(state.pacmanFacing + state.pacmanMouthAngle / 2f);
        double edge2 = Math.toRadians(state.pacmanFacing - state.pacmanMouthAngle / 2f);
        g.drawLine(cx, cy, cx + (int) (r * Math.cos(edge1)), cy - (int) (r * Math.sin(edge1)));
        g.drawLine(cx, cy, cx + (int) (r * Math.cos(edge2)), cy - (int) (r * Math.sin(edge2)));
        if (state.invincibilityTimer>0){
            float alpha=Math.min(1f,state.invincibilityTimer/GameState.INVINCIBILITY_DURATION);
            g.setColor(new Color(255,220,50,(int)(160*alpha)));
            g.setStroke(new BasicStroke(3f)); g.drawOval(cx-r-6,cy-r-6,(r+6)*2,(r+6)*2);
        }

        lastPlayerX = currX;
        lastPlayerY = currY;
    }

    private void drawBanner(Graphics2D g) {
        float alpha=Math.min(1f,state.levelUpBannerTimer/0.5f); int a=(int)(alpha*230);
        g.setColor(new Color(15,15,15,a)); g.fillRoundRect(100,218,600,92,20,20);
        g.setColor(new Color(255,220,50,a)); g.setStroke(new BasicStroke(3));
        g.drawRoundRect(100,218,600,92,20,20);
        g.setFont(new Font("Arial",Font.BOLD,32)); g.setColor(new Color(255,220,50,a));
        drawC(g,">> "+state.levelUpBannerText,266);
        g.setFont(new Font("Arial",Font.PLAIN,16)); g.setColor(new Color(200,200,200,a));
        drawC(g,"Things are about to get faster \u2014 stay sharp!",294);
    }

    private void drawGameOverOverlay(Graphics2D g) {
        g.setColor(new Color(0,0,0,185)); g.fillRect(0,0,W,H);
        g.setFont(new Font("Arial",Font.BOLD,50)); g.setColor(new Color(220,50,50));
        drawC(g,"GAME OVER",268);
        g.setFont(new Font("Arial",Font.PLAIN,21)); g.setColor(Color.WHITE);
        drawC(g,"Your health ran out \u2014 heading to results...",314);
    }

    private void drawTransientGameplayFeedback(Graphics2D g) {
        if (state.floatingTextTimer > 0f && state.floatingText != null && !state.floatingText.isEmpty()) {
            float a = Math.min(1f, state.floatingTextTimer / Math.max(0.01f, state.floatingTextDuration));
            int alpha = (int) (255 * a);
            int tx = (int) state.floatingTextX;
            int ty = (int) state.floatingTextY;
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(new Color(0, 0, 0, Math.min(180, alpha)));
            g.drawString(state.floatingText, tx - g.getFontMetrics().stringWidth(state.floatingText) / 2 + 2, ty + 2);
            g.setColor(new Color(state.floatingTextR, state.floatingTextG, state.floatingTextB, alpha));
            g.drawString(state.floatingText, tx - g.getFontMetrics().stringWidth(state.floatingText) / 2, ty);
        }

        if (state.quizResultBannerTimer > 0f) {
            int bw = 410;
            int bh = 56;
            int bx = W / 2 - bw / 2;
            int by = 70;
            float p = Math.min(1f, state.quizResultBannerTimer / 1.2f);
            int alpha = (int) (220 * p);
            Color fill = state.lastQuizCorrect ? new Color(54, 160, 74, alpha) : new Color(185, 58, 58, alpha);
            Color border = state.lastQuizCorrect ? new Color(170, 255, 190, alpha) : new Color(255, 190, 190, alpha);
            String txt = state.lastQuizCorrect ? "Correct! Great choice." : "Wrong answer. Stay sharp!";
            g.setColor(new Color(0, 0, 0, Math.min(140, alpha)));
            g.fillRoundRect(bx + 4, by + 4, bw, bh, 14, 14);
            g.setColor(fill);
            g.fillRoundRect(bx, by, bw, bh, 14, 14);
            g.setColor(border);
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(bx, by, bw, bh, 14, 14);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(new Color(255, 255, 255, alpha));
            drawC(g, txt, by + 35);
        }
    }

    private void drawFlashOverlay(Graphics2D g) {
        if (state.flashTimer <= 0f || state.flashDuration <= 0f) return;
        float p = Math.min(1f, state.flashTimer / state.flashDuration);
        int alpha = (int) (state.flashMaxAlpha * p);
        g.setColor(new Color(state.flashR, state.flashG, state.flashB, Math.max(0, alpha)));
        g.fillRect(0, 0, W, H);
    }

    // ═════════════════════════════════════════════════════════════════
    // LEVEL COMPLETE
    // ═════════════════════════════════════════════════════════════════
    private void drawLevelComplete(Graphics2D g) {
        g.setColor(new Color(26,58,26)); g.fillRect(0,0,W,H);
        for (int x=0;x<W;x+=80){g.setColor(new Color(34,74,34));g.fillRect(x,0,40,H);}
        for (int x=0;x<W;x+=40){g.setColor((x/40)%2==0?new Color(210,180,120):new Color(190,155,95));g.fillRect(x,H-36,40,36);}
        g.setColor(new Color(42,90,42)); g.fillRect(0,0,W,22);
        int[] lightX={60,200,360,520,680};
        for (int lx:lightX){if(lx+80>W)continue;g.setColor(new Color(255,248,210));g.fillRoundRect(lx,4,80,14,6,6);g.setColor(new Color(255,248,180,30));g.fillRect(lx-10,18,100,20);}
        g.setColor(new Color(74,52,24)); g.fillRect(0,38,W,52);
        g.setColor(new Color(90,64,30)); g.fillRect(0,84,W,6);
        String[] shelf={"Apple","Broccoli","Banana","Carrot","Orange","Grapes","Spinach","Tomato"};
        for (int i=0;i<8;i++) drawShelfFoodItem(g,shelf[i],30+i*95,44,40,40);

        int bannerW=480,bannerH=54,bannerX=W/2-bannerW/2,bannerY=106;
        g.setColor(new Color(0,0,0,80)); g.fillRoundRect(bannerX+4,bannerY+4,bannerW,bannerH,10,10);
        g.setColor(new Color(232,192,32)); g.fillRoundRect(bannerX,bannerY,bannerW,bannerH,10,10);
        g.setColor(new Color(184,144,10)); g.setStroke(new BasicStroke(3)); g.drawRoundRect(bannerX,bannerY,bannerW,bannerH,10,10);
        for (int dx=bannerX+40;dx<bannerX+bannerW-20;dx+=60){g.setColor(new Color(200,34,20));g.fillOval(dx-6,bannerY-8,12,12);g.setColor(new Color(140,16,8));g.setStroke(new BasicStroke(1));g.drawOval(dx-6,bannerY-8,12,12);}
        g.setFont(new Font("Arial",Font.BOLD,28)); g.setColor(new Color(58,32,0));
        drawC(g,"AISLE "+levelCompleteScene.getLevelNumber()+" CLEARED!",bannerY+36);

        int cardW=460,cardH=240,cardX=W/2-cardW/2,cardY=178;
        g.setColor(new Color(0,0,0,100)); g.fillRoundRect(cardX+5,cardY+5,cardW,cardH,8,8);
        g.setColor(new Color(255,254,240)); g.fillRoundRect(cardX,cardY,cardW,cardH,8,8);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(3)); g.drawRoundRect(cardX,cardY,cardW,cardH,8,8);
        g.setFont(new Font("Arial",Font.BOLD,15)); g.setColor(new Color(58,32,0));
        drawC(g,"NUTRITION QUEST \u2014 LEVEL "+levelCompleteScene.getLevelNumber()+" SUMMARY",cardY+26);
        g.setFont(new Font("Arial",Font.PLAIN,10)); g.setColor(new Color(122,80,32));
        drawC(g,"Great work! Keep collecting those healthy foods!",cardY+42);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{8,6},0));
        g.drawLine(cardX+20,cardY+50,cardX+cardW-20,cardY+50);
        g.setStroke(new BasicStroke(1)); g.setColor(new Color(0,0,0,12)); g.fillRect(cardX+1,cardY+58,cardW-2,30);
        g.setFont(new Font("Arial",Font.PLAIN,14)); g.setColor(new Color(90,58,20));
        g.drawString("Score so far",cardX+28,cardY+78);
        g.setFont(new Font("Arial",Font.BOLD,18)); g.setColor(new Color(40,140,40));
        String scoreStr=levelCompleteScene.getScore()+" pts";
        g.drawString(scoreStr,cardX+cardW-28-g.getFontMetrics().stringWidth(scoreStr),cardY+78);
        g.setFont(new Font("Arial",Font.PLAIN,14)); g.setColor(new Color(90,58,20));
        g.drawString("HP remaining",cardX+28,cardY+112);
        int hbX=cardX+180,hbY=cardY+96,hbW=cardW-220,hbH=20;
        g.setColor(new Color(40,40,40)); g.fillRoundRect(hbX,hbY,hbW,hbH,6,6);
        Color hc=state.playerHealth>60?new Color(60,210,90):state.playerHealth>30?new Color(240,180,30):new Color(220,50,50);
        g.setColor(hc); g.fillRoundRect(hbX,hbY,(int)(hbW*state.playerHealth/(float)GameState.MAX_HEALTH),hbH,6,6);
        g.setColor(new Color(255,255,255,100)); g.setStroke(new BasicStroke(1)); g.drawRoundRect(hbX,hbY,hbW,hbH,6,6);
        g.setFont(new Font("Arial",Font.BOLD,11)); g.setColor(Color.WHITE);
        String hpStr=state.playerHealth+" / "+GameState.MAX_HEALTH;
        g.drawString(hpStr,hbX+(hbW-g.getFontMetrics().stringWidth(hpStr))/2,hbY+14);
        g.setColor(new Color(0,0,0,12)); g.fillRect(cardX+1,cardY+126,cardW-2,30);
        g.setFont(new Font("Arial",Font.PLAIN,14)); g.setColor(new Color(90,58,20));
        g.drawString("Up next",cardX+28,cardY+146);
        g.setFont(new Font("Arial",Font.BOLD,14)); g.setColor(new Color(32,80,180));
        String nextStr=levelCompleteScene.getNextLevelName();
        g.drawString(nextStr,cardX+cardW-28-g.getFontMetrics().stringWidth(nextStr),cardY+146);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{8,6},0));
        g.drawLine(cardX+20,cardY+164,cardX+cardW-20,cardY+164);
        float t=levelCompleteScene.getTimeRemaining();
        int arcR=20,arcX=cardX+cardW-28-arcR*2,arcY=cardY+172;
        g.setFont(new Font("Arial",Font.PLAIN,12)); g.setColor(new Color(122,80,32));
        String countdownText="Starting in "+String.format("%.1f",t)+"s   (ENTER to skip)";
        int ctW=g.getFontMetrics().stringWidth(countdownText);
        g.drawString(countdownText,cardX+(cardW-ctW)/2,arcY+arcR+2);
        g.setColor(new Color(60,40,10,60)); g.fillOval(arcX,arcY,arcR*2,arcR*2);
        g.setColor(new Color(232,192,32)); g.setStroke(new BasicStroke(4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.drawArc(arcX,arcY,arcR*2,arcR*2,90,-(int)(360*(t/3.5f)));
        g.setFont(new Font("Arial",Font.ITALIC,10)); g.setColor(new Color(122,80,32));
        drawC(g,"Thank you for shopping at Nutrition Quest!",cardY+225);

        int btnW=260,btnH=44,btnX=W/2-btnW/2,btnY2=cardY+cardH+16;
        g.setColor(new Color(0,0,0,80)); g.fillRoundRect(btnX+4,btnY2+4,btnW,btnH,10,10);
        g.setColor(new Color(74,138,74)); g.fillRoundRect(btnX,btnY2,btnW,btnH,10,10);
        g.setColor(new Color(42,106,42)); g.setStroke(new BasicStroke(2.5f)); g.drawRoundRect(btnX,btnY2,btnW,btnH,10,10);
        g.setFont(new Font("Arial",Font.BOLD,15)); g.setColor(new Color(232,255,232));
        drawC(g,"ENTER \u2014 Next Level",btnY2+28);

        g.setColor(new Color(58,42,16)); g.fillRect(0,H-72,W,36);
        g.setColor(new Color(90,64,30)); g.fillRect(0,H-73,W,4);
        String[] junkNamesLC={"Burger","Soda","Chips","Candy","Pizza","Donut"};
        for (int i=0;i<junkNamesLC.length;i++) drawShelfFoodItem(g,junkNamesLC[i],30+i*130,H-70,32,32);
        g.setFont(new Font("Arial",Font.ITALIC,10)); g.setColor(Color.BLACK);
        drawC(g,"--- junk food zone: avoid! ---",H-24);
    }

    // ═════════════════════════════════════════════════════════════════
    // END SCREEN
    // ═════════════════════════════════════════════════════════════════
    private void drawEndScreen(Graphics2D g) {
        g.setColor(new Color(26,26,26)); g.fillRect(0,0,W,H);
        for (int y=0;y<H;y+=20){g.setColor(new Color(58,58,58));g.fillRect(0,y,W,18);g.setColor(new Color(42,42,42));g.fillRect(0,y+18,W,2);g.setColor(new Color(70,70,70,80));g.fillRect(0,y,W,4);}
        g.setColor(new Color(90,90,90)); g.fillRoundRect(W/2-120,28,240,10,6,6);
        g.setColor(new Color(110,110,110)); g.setStroke(new BasicStroke(1)); g.drawRoundRect(W/2-120,28,240,10,6,6);
        g.setColor(new Color(70,70,70));
        for (int nx=W/2-80;nx<W/2+80;nx+=16) g.fillRect(nx,31,8,4);

        int signW=220,signH=52,signX=W/2-signW/2,signY=60;
        g.setColor(new Color(0,0,0,100)); g.fillRoundRect(signX+5,signY+5,signW,signH,8,8);
        g.setColor(new Color(200,48,32)); g.fillRoundRect(signX,signY,signW,signH,8,8);
        g.setColor(new Color(144,26,16)); g.setStroke(new BasicStroke(3)); g.drawRoundRect(signX,signY,signW,signH,8,8);
        g.setFont(new Font("Arial",Font.BOLD,30)); g.setColor(Color.WHITE); drawC(g,"CLOSED",signY+36);
        g.setColor(new Color(144,26,16));
        g.fillOval(signX+10,signY+10,10,10);g.fillOval(signX+signW-20,signY+10,10,10);
        g.fillOval(signX+10,signY+signH-20,10,10);g.fillOval(signX+signW-20,signY+signH-20,10,10);

        int cardW=380,cardH=248,cardX=W/2-cardW/2,cardY=120;
        g.setColor(new Color(0,0,0,120)); g.fillRoundRect(cardX+6,cardY+6,cardW,cardH,8,8);
        g.setColor(new Color(255,254,240)); g.fillRoundRect(cardX,cardY,cardW,cardH,8,8);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(3)); g.drawRoundRect(cardX,cardY,cardW,cardH,8,8);
        g.setFont(new Font("Arial",Font.BOLD,16)); g.setColor(new Color(58,32,0)); drawC(g,"NUTRITION QUEST",cardY+28);
        g.setFont(new Font("Arial",Font.PLAIN,10)); g.setColor(new Color(122,80,32)); drawC(g,"Session Summary",cardY+44);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{8,6},0));
        g.drawLine(cardX+20,cardY+52,cardX+cardW-20,cardY+52);

        float[] res=endScene.getResults();
        g.setStroke(new BasicStroke(1));
        String[] labels={"Final Score","Time Played","HP Remaining"};
        String[] values={(int)res[0]+" pts",String.format("%.1f",res[1])+"s",state.playerHealth+" / "+GameState.MAX_HEALTH};
        Color[] valCols={new Color(58,32,0),new Color(58,32,0),state.playerHealth<=0?new Color(200,48,32):new Color(40,140,40)};
        for (int i=0;i<labels.length;i++){
            int rowY=cardY+78+i*36;
            if (i%2==0){g.setColor(new Color(0,0,0,12));g.fillRect(cardX+1,rowY-16,cardW-2,28);}
            g.setFont(new Font("Arial",Font.PLAIN,14));g.setColor(new Color(90,58,20));g.drawString(labels[i],cardX+28,rowY);
            g.setFont(new Font("Arial",Font.BOLD,14));g.setColor(valCols[i]);int vw=g.getFontMetrics().stringWidth(values[i]);g.drawString(values[i],cardX+cardW-28-vw,rowY);
        }
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{8,6},0));
        g.drawLine(cardX+20,cardY+cardH-52,cardX+cardW-20,cardY+cardH-52);
        if ((int)res[0]>=state.highScore&&(int)res[0]>0){
            g.setColor(new Color(200,168,48));g.fillRoundRect(cardX+cardW/2-90,cardY+cardH-46,180,22,6,6);
            g.setFont(new Font("Arial",Font.BOLD,11));g.setColor(new Color(58,32,0));
            drawC(g,"\u25cf  NEW HIGH SCORE!  \u25cf",cardY+cardH-30);
        }
        g.setFont(new Font("Arial",Font.ITALIC,10)); g.setColor(new Color(122,80,32));
        drawC(g,"Thank you for shopping at Nutrition Quest!",cardY+cardH-12);

        int btnY=cardY+cardH+20,btnW2=210,btnH2=44;
        g.setColor(new Color(0,0,0,80)); g.fillRoundRect(W/2-btnW2-8+4,btnY+4,btnW2,btnH2,10,10);
        g.setColor(new Color(232,192,32)); g.fillRoundRect(W/2-btnW2-8,btnY,btnW2,btnH2,10,10);
        g.setColor(new Color(184,144,10)); g.setStroke(new BasicStroke(2.5f)); g.drawRoundRect(W/2-btnW2-8,btnY,btnW2,btnH2,10,10);
        g.setFont(new Font("Arial",Font.BOLD,14)); g.setColor(new Color(58,32,0));
        g.drawString("ENTER \u2014 Play Again",W/2-btnW2-8+(btnW2-g.getFontMetrics().stringWidth("ENTER \u2014 Play Again"))/2,btnY+27);
        g.setColor(new Color(0,0,0,80)); g.fillRoundRect(W/2+8+4,btnY+4,btnW2,btnH2,10,10);
        g.setColor(new Color(74,74,74)); g.fillRoundRect(W/2+8,btnY,btnW2,btnH2,10,10);
        g.setColor(new Color(42,42,42)); g.setStroke(new BasicStroke(2.5f)); g.drawRoundRect(W/2+8,btnY,btnW2,btnH2,10,10);
        g.setFont(new Font("Arial",Font.BOLD,14)); g.setColor(new Color(200,200,200));
        g.drawString("ESC \u2014 Main Menu",W/2+8+(btnW2-g.getFontMetrics().stringWidth("ESC \u2014 Main Menu"))/2,btnY+27);
    }

    // ═════════════════════════════════════════════════════════════════
    // OVERLAYS (paused, powerup popup)
    // ═════════════════════════════════════════════════════════════════
    private void drawPaused(Graphics2D g) {
        g.setColor(new Color(0,0,0,160)); g.fillRect(0,0,W,H);
        int cardW=380,cardH=220,cardX=W/2-cardW/2,cardY=H/2-cardH/2;
        g.setColor(new Color(0,0,0,100)); g.fillRoundRect(cardX+5,cardY+5,cardW,cardH,10,10);
        g.setColor(new Color(139,105,20)); g.fillRoundRect(cardX,cardY,cardW,cardH,10,10);
        for (int i=cardX;i<cardX+cardW;i+=12){g.setColor(new Color(155,121,36,50));g.fillRect(i,cardY,6,cardH);}
        g.setColor(new Color(107,74,8)); g.setStroke(new BasicStroke(4)); g.drawRoundRect(cardX,cardY,cardW,cardH,10,10);
        int pX=cardX+12,pY=cardY+12,pW=cardW-24,pH=cardH-24;
        g.setColor(new Color(0,0,0,50)); g.fillRoundRect(pX+3,pY+3,pW,pH,6,6);
        g.setColor(new Color(255,254,240)); g.fillRoundRect(pX,pY,pW,pH,6,6);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(2)); g.drawRoundRect(pX,pY,pW,pH,6,6);
        int[][] pins={{pX+20,pY+10},{pX+pW-20,pY+10}};
        for (int[] p:pins){g.setColor(new Color(200,34,20));g.fillOval(p[0]-7,p[1]-7,14,14);g.setColor(new Color(140,16,8));g.setStroke(new BasicStroke(1));g.drawOval(p[0]-7,p[1]-7,14,14);}
        g.setFont(new Font("Arial",Font.BOLD,32)); g.setColor(new Color(200,48,32)); g.setStroke(new BasicStroke(2));
        int titleW=g.getFontMetrics().stringWidth("\u2014 PAUSED \u2014"); g.drawString("\u2014 PAUSED \u2014",W/2-titleW/2,cardY+76);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{8,5},0));
        g.drawLine(pX+20,cardY+86,pX+pW-20,cardY+86);
        g.setFont(new Font("Arial",Font.BOLD,14)); g.setColor(new Color(58,32,0));
        drawC(g,"ESC \u2014 Resume shopping",cardY+118); drawC(g,"Q \u2014 Leave the store",cardY+142);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{6,4},0));
        g.drawLine(pX+20,cardY+158,pX+pW-20,cardY+158);
        g.setFont(new Font("Arial",Font.ITALIC,10)); g.setColor(new Color(122,80,32));
        drawC(g,"Take a break \u2014 your cart is saved!",cardY+176);
    }

    private void drawPowerupPopup(Graphics2D g) {
        g.setColor(new Color(0,0,0,170)); g.fillRect(0,0,W,H);
        int cardW=420,cardH=280,cardX=W/2-cardW/2,cardY=H/2-cardH/2;
        g.setColor(new Color(0,0,0,120)); g.fillRoundRect(cardX+6,cardY+6,cardW,cardH,14,14);
        g.setColor(new Color(139,105,20)); g.fillRoundRect(cardX,cardY,cardW,cardH,14,14);
        for (int i=cardX;i<cardX+cardW;i+=12){g.setColor(new Color(155,121,36,50));g.fillRect(i,cardY,6,cardH);}
        g.setColor(new Color(107,74,8)); g.setStroke(new BasicStroke(5)); g.drawRoundRect(cardX,cardY,cardW,cardH,14,14);
        int pX=cardX+14,pY=cardY+14,pW=cardW-28;
        g.setColor(new Color(0,0,0,50)); g.fillRoundRect(pX+3,pY+3,pW,cardH-28,8,8);
        g.setColor(new Color(255,254,240)); g.fillRoundRect(pX,pY,pW,cardH-28,8,8);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(2)); g.drawRoundRect(pX,pY,pW,cardH-28,8,8);
        int[][] pins={{pX+22,pY+12},{pX+pW-22,pY+12}};
        for (int[] p:pins){g.setColor(new Color(200,34,20));g.fillOval(p[0]-8,p[1]-8,16,16);g.setColor(new Color(140,16,8));g.setStroke(new BasicStroke(1.5f));g.drawOval(p[0]-8,p[1]-8,16,16);}
        g.setFont(new Font("Arial",Font.BOLD,17)); g.setColor(new Color(58,32,0));
        int titleW2=g.getFontMetrics().stringWidth("** POWER-UP UNLOCKED! **");
        g.drawString("** POWER-UP UNLOCKED! **",W/2-titleW2/2,cardY+50);
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{8,5},0));
        g.drawLine(pX+20,cardY+58,pX+pW-20,cardY+58);
        g.setFont(new Font("Arial",Font.PLAIN,12)); g.setColor(new Color(90,58,20));
        int subW=g.getFontMetrics().stringWidth("Choose your power-up wisely!");
        g.drawString("Choose your power-up wisely!",W/2-subW/2,cardY+76);

        Color[] optColors={new Color(60,140,220),new Color(60,190,80),new Color(232,170,20)};
        String[] optIcons={"[~]","[O]","[x2]"};
        for (int i=0;i<GameState.POWERUP_OPTS.length;i++){
            int rowY=cardY+96+i*46; boolean sel=(i==state.powerupPopupSel);
            if (sel){g.setColor(optColors[i]);g.fillRoundRect(pX+16,rowY,pW-32,38,8,8);g.setColor(optColors[i].darker());g.setStroke(new BasicStroke(2.5f));g.drawRoundRect(pX+16,rowY,pW-32,38,8,8);}
            else    {g.setColor(new Color(0,0,0,18));g.fillRoundRect(pX+16,rowY,pW-32,38,8,8);g.setColor(new Color(200,168,48,100));g.setStroke(new BasicStroke(1f));g.drawRoundRect(pX+16,rowY,pW-32,38,8,8);}
            g.setFont(new Font("Arial",Font.BOLD,13)); g.setColor(sel?Color.WHITE:optColors[i].darker());
            g.drawString(optIcons[i],pX+28,rowY+24);
            g.setColor(sel?Color.WHITE:new Color(42,24,0)); g.drawString(GameState.POWERUP_OPTS[i],pX+68,rowY+24);
            if (sel){g.setFont(new Font("Arial",Font.BOLD,14));g.setColor(Color.WHITE);int arrW=g.getFontMetrics().stringWidth(">>>");g.drawString(">>>",pX+pW-32-arrW,rowY+24);}
        }
        g.setColor(new Color(200,168,48)); g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{6,4},0));
        g.drawLine(pX+20,cardY+cardH-48,pX+pW-20,cardY+cardH-48);
        g.setFont(new Font("Arial",Font.BOLD,11)); g.setColor(new Color(58,32,0));
        int hintW=g.getFontMetrics().stringWidth("^ / v  Navigate     ENTER  Select     ESC  Cancel");
        g.drawString("^ / v  Navigate     ENTER  Select     ESC  Cancel",W/2-hintW/2,cardY+cardH-28);
    }

    // ═════════════════════════════════════════════════════════════════
    // HELPER DRAWING METHODS
    // ═════════════════════════════════════════════════════════════════
    private void drawShelfFoodItem(Graphics2D g, String name, int x, int y, int w, int h) {
        Image img = FoodImageLoader.getFoodImage(name);
        if (img != null) {
            g.setColor(new Color(0,0,0,40)); g.fillOval(x+3,y+h-4,w,6);
            g.drawImage(img,x,y,w,h,null);
        } else {
            int hash=Math.abs(name.hashCode());
            Color[] fallback={new Color(220,50,50),new Color(60,180,60),new Color(255,210,0),new Color(255,140,0),new Color(130,60,180),new Color(60,200,80)};
            Color col=fallback[hash%fallback.length];
            g.setColor(col);g.fillOval(x,y,w,h);g.setColor(col.darker());g.setStroke(new BasicStroke(1.5f));g.drawOval(x,y,w,h);
            g.setColor(Color.WHITE);g.setFont(new Font("Arial",Font.BOLD,7));
            String label=name.length()>5?name.substring(0,5):name;int tw=g.getFontMetrics().stringWidth(label);
            g.drawString(label,x+(w-tw)/2,y+h/2+3);
        }
    }

    private void drawCInRect(Graphics2D g, String text, int rx, int y, int rw) {
        int tw=g.getFontMetrics().stringWidth(text);
        g.drawString(text,rx+(rw-tw)/2,y);
    }

    private void drawMealIcon(Graphics2D g, int mealIndex, int cx, int cy) {
        g.setStroke(new BasicStroke(2));
        switch (mealIndex) {
            case 0:
                g.setColor(new Color(80,80,80));g.fillRoundRect(cx-20,cy+8,40,8,4,4);g.fillRect(cx+18,cy+9,12,5);
                g.setColor(new Color(255,255,255));g.fillOval(cx-14,cy-8,28,20);g.setColor(new Color(200,200,200));g.drawOval(cx-14,cy-8,28,20);
                g.setColor(new Color(255,200,0));g.fillOval(cx-7,cy-5,14,14);g.setColor(new Color(220,160,0));g.drawOval(cx-7,cy-5,14,14);
                break;
            case 1:
                g.setColor(new Color(210,160,80));g.fillRoundRect(cx-20,cy+8,40,8,3,3);
                g.setColor(new Color(60,180,60));g.fillRoundRect(cx-22,cy+2,44,8,2,2);
                g.setColor(new Color(255,210,40));g.fillRect(cx-18,cy-3,36,6);
                g.setColor(new Color(180,80,60));g.fillRoundRect(cx-20,cy-10,40,8,2,2);
                g.setColor(new Color(210,160,80));g.fillRoundRect(cx-18,cy-18,36,10,5,5);
                g.setColor(new Color(255,240,180));g.fillOval(cx-8,cy-16,4,4);g.fillOval(cx+2,cy-17,4,4);g.fillOval(cx+10,cy-15,4,4);
                break;
            case 2:
                g.setColor(new Color(0,0,0,40));g.fillOval(cx-20,cy+12,40,8);
                g.setColor(new Color(240,240,240));g.fillOval(cx-22,cy-10,44,22);g.setColor(new Color(200,200,200));g.drawOval(cx-22,cy-10,44,22);
                g.setColor(new Color(180,80,40));g.fillOval(cx-10,cy-6,14,10);
                g.setColor(new Color(60,160,60));g.fillOval(cx+2,cy-5,10,8);
                g.setColor(new Color(220,180,80));g.fillOval(cx-14,cy-4,10,7);
                break;
        }
    }

    public void drawC(Graphics2D g, String t, int y) {
        g.drawString(t,(W-g.getFontMetrics().stringWidth(t))/2,y);
    }

    // ── How To Play dialog (owns its own UI, called from PlayableGame) ──
    public void showHowToPlay(java.awt.Component parent) {
        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(parent) instanceof Frame
                ? (Frame) SwingUtilities.getWindowAncestor(parent) : null,
            "How to Play", true);
        dialog.setUndecorated(true);
        dialog.setSize(600, 420);
        dialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int pw=getWidth(), ph=getHeight();
                g.setColor(new Color(139,105,20));g.fillRect(0,0,pw,ph);
                for (int i=0;i<pw;i+=12){g.setColor(new Color(155,121,36,60));g.fillRect(i,0,6,ph);}
                g.setColor(new Color(107,74,8));g.setStroke(new BasicStroke(10));g.drawRect(5,5,pw-10,ph-10);
                g.setColor(new Color(0,0,0,60));g.fillRoundRect(28,28,pw-46,ph-46,6,6);
                g.setColor(new Color(255,254,240));g.fillRoundRect(24,24,pw-48,ph-48,6,6);
                g.setColor(new Color(200,168,48));g.setStroke(new BasicStroke(2));g.drawRoundRect(24,24,pw-48,ph-48,6,6);
                int[][] pins={{80,18},{pw-80,18},{80,ph-22},{pw-80,ph-22}};
                for (int[] p:pins){g.setColor(new Color(200,34,20));g.fillOval(p[0]-8,p[1]-8,16,16);g.setColor(new Color(140,16,8));g.setStroke(new BasicStroke(1.5f));g.drawOval(p[0]-8,p[1]-8,16,16);g.setColor(new Color(255,100,80,120));g.fillOval(p[0]-4,p[1]-4,6,6);}
                g.setFont(new Font("Arial",Font.BOLD,17));g.setColor(new Color(58,32,0));
                int tw=g.getFontMetrics().stringWidth("STORE NOTICE \u2014 HOW TO PLAY");
                g.drawString("STORE NOTICE \u2014 HOW TO PLAY",pw/2-tw/2,58);
                g.setColor(new Color(200,168,48));g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{8,5},0));
                g.drawLine(40,66,pw-40,66);
                int[][] sections={{36,76,(pw/2)-44,150},{pw/2+8,76,(pw/2)-44,150},{36,234,(pw/2)-44,130},{pw/2+8,234,(pw/2)-44,130}};
                Color[] bgCols={new Color(232,240,200),new Color(252,232,200),new Color(200,232,200),new Color(232,232,248)};
                Color[] bdrCols={new Color(90,138,32),new Color(216,112,32),new Color(64,160,64),new Color(96,96,192)};
                String[] titles={"CONTROLS","GAMEPLAY","LEVELS","POWERUPS"};
                String[][] lines={
                    {"WASD / Arrows -- Move","ESC -- Pause / Resume","Q -- Quit to menu","P -- Open Powerup popup"},
                    {"Green food = +1 pt (respawns)","Red food = -20 HP then quiz","Correct = +1 pt  +5 HP","Wrong = -10 HP","HP = 0 -> Game Over"},
                    {"Lv1 Breakfast -- reach 10 pts","Lv2 Lunch  -- reach 25 pts","Lv3 Dinner  -- survive!"},
                    {"Unlock at 8, 20, 40 pts","Popup: ^v navigate options","ENTER -- pick powerup","Freeze/Shield/2x pts"}
                };
                g.setStroke(new BasicStroke(2));
                for (int i=0;i<4;i++){
                    int sx=sections[i][0],sy=sections[i][1],sw=sections[i][2],sh=sections[i][3];
                    g.setColor(bgCols[i]);g.fillRoundRect(sx,sy,sw,sh,6,6);
                    g.setColor(bdrCols[i]);g.fillRoundRect(sx,sy,5,sh,3,3);g.drawRoundRect(sx,sy,sw,sh,6,6);
                    g.setFont(new Font("Arial",Font.BOLD,10));g.setColor(bdrCols[i].darker());g.drawString(titles[i],sx+12,sy+16);
                    g.setFont(new Font("Arial",Font.PLAIN,11));g.setColor(new Color(42,24,0));
                    for (int j=0;j<lines[i].length;j++) g.drawString(lines[i][j],sx+12,sy+30+j*18);
                }
                g.setColor(new Color(200,48,32,180));g.setStroke(new BasicStroke(2));g.drawRoundRect(pw/2-110,ph-52,220,26,4,4);
                g.setFont(new Font("Arial",Font.BOLD,10));g.setColor(new Color(200,48,32));
                int sw2=g.getFontMetrics().stringWidth("POSTED BY: STORE MANAGER");
                g.drawString("POSTED BY: STORE MANAGER",pw/2-sw2/2,ph-34);
            }
        };
        panel.setPreferredSize(new Dimension(600,420));
        panel.addKeyListener(new KeyAdapter(){@Override public void keyPressed(KeyEvent e){dialog.dispose();}});
        panel.setFocusable(true);

        JButton ok = new JButton("OK \u2014 Got it!") {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g=(Graphics2D)g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(232,192,32));g.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g.setColor(new Color(184,144,10));g.setStroke(new BasicStroke(2));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                g.setFont(new Font("Arial",Font.BOLD,13));g.setColor(new Color(58,32,0));
                FontMetrics fm=g.getFontMetrics();g.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,getHeight()/2+5);
            }
        };
        ok.setBorderPainted(false);ok.setContentAreaFilled(false);ok.setFocusPainted(false);
        ok.setPreferredSize(new Dimension(140,36));ok.addActionListener(e->dialog.dispose());

        JPanel south=new JPanel(new FlowLayout(FlowLayout.RIGHT,20,8));
        south.setBackground(new Color(107,74,8));south.add(ok);
        dialog.setLayout(new BorderLayout());
        dialog.add(panel,BorderLayout.CENTER);dialog.add(south,BorderLayout.SOUTH);
        dialog.pack();dialog.setVisible(true);panel.requestFocusInWindow();
    }
}

/*
package nutriquest;

import engine.entities.Entity;
import engine.entities.TextureObject;
import engine.managers.EntityManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * GameRenderer — responsible solely for painting every screen in Nutrition Quest.
 *
 * Extracted from PlayableGame to satisfy the Single Responsibility Principle.
 * It holds no mutable game state of its own; everything it needs is read from
 * the {@link GameState} snapshot passed at construction, plus the scene/entity
 * references injected via the update methods below.
 *
 * PlayableGame calls {@code renderer.paint(g2d, sceneName)} each frame from
 * {@code paintComponent()}; the renderer decides which draw-method to invoke.
 * /
public class GameRenderer {

    private static final int W = 800;
    private static final int H = 600;

    // Pac-Man mouth behaviour is controlled by proximity to walls/foods.
    // The mouth opens only when a "near collision" inflated AABB intersects
    // any active food item or wall.
    private static final float PACMAN_MOUTH_PROXIMITY_PX = 24f;
    private static final float PACMAN_MOUTH_CLOSED_DEG   = 2f;
    private static final float PACMAN_MOUTH_OPEN_DEG     = 45f;
    private static final float PACMAN_MOUTH_WIGGLE_HZ   = 8.0f;

    // ── References injected by PlayableGame ──────────────────────────
    private final GameState        state;
    private final EntityManager    entityManager;
    private       LevelScene       currentLevelScene;
    private       LevelCompleteScene levelCompleteScene;
    private       engine.scene.EndScene endScene;
    private       engine.entities.Player player;
    private       float lastPlayerX;
    private       float lastPlayerY;
    private       boolean hasLastPlayerPos = false;
    private       List<FoodItem>   goodFoods;
    private       List<FoodItem>   badFoods;
    private       engine.managers.InputOutputManager inputManager;

    public GameRenderer(GameState state, EntityManager entityManager) {
        this.state         = state;
        this.entityManager = entityManager;
    }

    // Setters called by PlayableGame whenever these references change ─
    public void setCurrentLevelScene(LevelScene s)          { this.currentLevelScene    = s; }
    public void setLevelCompleteScene(LevelCompleteScene s) { this.levelCompleteScene   = s; }
    public void setEndScene(engine.scene.EndScene s)        { this.endScene             = s; }
    public void setPlayer(engine.entities.Player p) {
        this.player = p;
        this.hasLastPlayerPos = false;
    }
    public void setFoodLists(List<FoodItem> good, List<FoodItem> bad) {
        this.goodFoods = good;
        this.badFoods  = bad;
    }
    public void setInputManager(engine.managers.InputOutputManager im) { this.inputManager = im; }

    // ── Entry point ──────────────────────────────────────────────────
    public void paint(Graphics2D g, String sceneName) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        switch (sceneName) {
            case "MenuScene":
                drawMenu(g); break;
            case "Level1": case "Level2": case "Level3":
                drawGame(g);
                if (state.paused && !state.powerupPopupOpen) drawPaused(g);
                if (state.powerupPopupOpen)  drawPowerupPopup(g);
                if (state.levelUpBannerTimer > 0) drawBanner(g);
                if (currentLevelScene != null && currentLevelScene.isGameOver()) drawGameOverOverlay(g);
                break;
            case "LevelComplete":
                drawLevelComplete(g); break;
            case "EndScene":
                drawEndScreen(g); break;
            default:
                drawMenu(g);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // MENU
    // ═════════════════════════════════════════════════════════════════
    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(26, 58, 26));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(34, 74, 34));
        for (int x = 0; x < W; x += 80) g.fillRect(x, 0, 40, H);

        for (int x = 0; x < W; x += 40) {
            g.setColor((x / 40) % 2 == 0 ? new Color(210, 180, 120) : new Color(190, 155, 95));
            g.fillRect(x, H - 36, 40, 36);
        }

        g.setColor(new Color(42, 90, 42));
        g.fillRect(0, 0, W, 22);
        int[] lightX = {60, 200, 360, 520, 680};
        for (int lx : lightX) {
            if (lx + 80 > W) continue;
            g.setColor(new Color(255, 248, 210));
            g.fillRoundRect(lx, 4, 80, 14, 6, 6);
            g.setColor(new Color(255, 248, 180, 30));
            g.fillRect(lx - 10, 18, 100, 20);
        }

        g.setColor(new Color(74, 52, 24));
        g.fillRect(0, 38, W, 52);
        g.setColor(new Color(90, 64, 30));
        g.fillRect(0, 84, W, 6);
        String[] shelfFoods = {"Apple","Broccoli","Banana","Carrot","Orange","Grapes","Spinach","Tomato"};
        for (int i = 0; i < 8; i++) drawShelfFoodItem(g, shelfFoods[i], 30 + i * 95, 44, 40, 40);

        g.setColor(new Color(40, 30, 10));
        g.fillRoundRect(58, 103, W - 116, 70, 10, 10);
        g.setColor(new Color(232, 192, 32));
        g.fillRoundRect(60, 105, W - 120, 66, 8, 8);
        g.setColor(new Color(184, 144, 10));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(60, 105, W - 120, 66, 8, 8);

        int[] dotX = {120, 260, 400, 540, 680};
        for (int dx : dotX) {
            if (dx > W - 60) continue;
            g.setColor(new Color(200, 0, 0));
            g.fillOval(dx - 8, 98, 16, 16);
            g.setColor(new Color(140, 0, 0));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(dx - 8, 98, 16, 16);
        }

        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.setColor(new Color(58, 32, 0));
        drawC(g, "NUTRITION QUEST", 143);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(new Color(90, 58, 0));
        drawC(g, "Collect healthy food  \u2022  Avoid junk  \u2022  Answer quizzes", 163);

        int cardW = 168, cardH = 130, cardY = 188;
        int cardStartX = (W - (cardW * 3 + 24)) / 2;
        String[] aisleNums = {"AISLE 1","AISLE 2","AISLE 3"};
        String[] mealNames = {"BREAKFAST","LUNCH","DINNER"};
        String[] mealDesc  = {"Slow & steady","Medium pace","Fast chaos"};
        String[] lvlLabel  = {"Lv1 - Breakfast","Lv2 - Lunch","Lv3 - Dinner"};
        Color[] cardBg  = {new Color(255,248,232),new Color(232,248,232),new Color(234,232,248)};
        Color[] cardBdr = {new Color(232,160,32), new Color(64,168,64),  new Color(96,64,192)};
        Color[] cardHdr = {new Color(232,160,32), new Color(64,168,64),  new Color(96,64,192)};
        Color[] cardTxt = {new Color(58,32,0),    new Color(10,58,10),   new Color(42,16,96)};

        for (int i = 0; i < 3; i++) {
            int cx = cardStartX + i * (cardW + 12);
            g.setColor(new Color(0,0,0,60));
            g.fillRoundRect(cx+4, cardY+4, cardW, cardH, 12, 12);
            g.setColor(cardBg[i]);
            g.fillRoundRect(cx, cardY, cardW, cardH, 12, 12);
            g.setColor(cardBdr[i]);
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(cx, cardY, cardW, cardH, 12, 12);
            g.setColor(cardHdr[i]);
            g.fillRoundRect(cx, cardY, cardW, 26, 12, 12);
            g.fillRect(cx, cardY+14, cardW, 12);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.setColor(i==2 ? new Color(224,216,255) : cardTxt[i]);
            drawCInRect(g, aisleNums[i], cx, cardY+17, cardW);
            drawMealIcon(g, i, cx+cardW/2, cardY+60);
            g.setFont(new Font("Arial", Font.BOLD, 13));
            g.setColor(cardTxt[i]);
            drawCInRect(g, mealNames[i], cx, cardY+88, cardW);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.setColor(cardTxt[i].brighter());
            drawCInRect(g, mealDesc[i], cx, cardY+102, cardW);
            g.setColor(cardHdr[i]);
            g.fillRoundRect(cx+14, cardY+110, cardW-28, 16, 6, 6);
            g.setFont(new Font("Arial", Font.BOLD, 9));
            g.setColor(i==2 ? new Color(224,216,255) : cardTxt[i]);
            drawCInRect(g, lvlLabel[i], cx, cardY+122, cardW);
        }

        int btnW = 260, btnStartY = 340, btnSpacing = 54;
        int btnX = W/2 - btnW/2;
        for (int i = 0; i < state.menuOptions.length; i++) {
            int bY = btnStartY + i * btnSpacing;
            boolean selected = (i == state.menuSelection);
            g.setColor(new Color(0,0,0,70));
            g.fillRoundRect(btnX+4, bY+4, btnW, 42, 10, 10);
            if (selected) {
                g.setColor(new Color(232,192,32));
                g.fillRoundRect(btnX, bY, btnW, 42, 10, 10);
                g.setColor(new Color(184,144,10));
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(btnX, bY, btnW, 42, 10, 10);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.setColor(new Color(58,32,0));
            } else {
                g.setColor(i==1 ? new Color(74,138,74) : new Color(90,64,32));
                g.fillRoundRect(btnX, bY, btnW, 42, 10, 10);
                g.setColor(i==1 ? new Color(42,106,42) : new Color(58,42,16));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(btnX, bY, btnW, 42, 10, 10);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                g.setColor(i==1 ? new Color(232,255,232) : new Color(232,216,184));
            }
            drawC(g, state.menuOptions[i], bY+27);
        }

        if (state.highScore > 0) {
            g.setColor(new Color(232,192,32));
            g.setFont(new Font("Arial", Font.BOLD, 14));
            drawC(g, "\u25cf Best Score: " + state.highScore, 514);
        }

        g.setColor(new Color(58,42,16));
        g.fillRect(0, H-72, W, 36);
        g.setColor(new Color(90,64,30));
        g.fillRect(0, H-73, W, 4);
        String[] junkNames = {"Burger","Soda","Chips","Candy","Pizza","Donut"};
        for (int i = 0; i < junkNames.length; i++) drawShelfFoodItem(g, junkNames[i], 30+i*130, H-70, 32, 32);
        g.setFont(new Font("Arial", Font.ITALIC, 10));
        g.setColor(Color.BLACK);
        drawC(g, "--- junk food zone: avoid! ---", H-24);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.setColor(Color.BLACK);
        drawC(g, "^ v navigate     ENTER select", H-10);
    }

}


*/
