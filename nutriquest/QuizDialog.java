package nutriquest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Timer;
import java.util.TimerTask;

public class QuizDialog extends JDialog {

    private static final int COUNTDOWN_SECONDS = 30;

    private static final Color TEXT_DIM      = new Color(155, 155, 175);

    int selectedAnswer = -1;
    private int hoveredIndex = 0;
    final int correctIndex;
    private int countdown = COUNTDOWN_SECONDS;
    private Timer countdownTimer;
    private volatile boolean answered = false;

    private AnswerButton[] answerButtons;
    private TimerPanel timerPanel;

    public QuizDialog(Frame parent, String question, String[] options, int correctIndex) {
        super(parent, "Nutrition Quiz", true);
        this.correctIndex = correctIndex;

        setUndecorated(true);
        setSize(580, 420);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(42, 90, 42));
                g2.fillRect(0, 0, getWidth(), getHeight());
                for (int x = 0; x < getWidth(); x += 60) {
                    g2.setColor(new Color(52, 106, 52, 80));
                    g2.fillRect(x, 0, 30, getHeight());
                }
                g2.setColor(new Color(255, 251, 230));
                g2.fillRoundRect(getWidth()/2 - 60, 4, 120, 10, 5, 5);
                g2.setColor(new Color(26, 58, 26, 230));
                g2.fill(new RoundRectangle2D.Float(8, 20, getWidth()-16, getHeight()-28, 16, 16));
                g2.setColor(new Color(90, 160, 60));
                g2.setStroke(new BasicStroke(3));
                g2.draw(new RoundRectangle2D.Float(8, 20, getWidth()-16, getHeight()-28, 16, 16));
                g2.setColor(new Color(232, 192, 32));
                g2.fillRoundRect(8, 20, getWidth()-16, 6, 4, 4);
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(new EmptyBorder(22, 24, 8, 24));

        JLabel icon = new JLabel("?");
        icon.setFont(new Font("Arial", Font.BOLD, 22));
        icon.setForeground(new Color(232, 192, 32));

        JLabel title = new JLabel("NUTRITION QUIZ");
        title.setFont(new Font("Arial", Font.BOLD, 17));
        title.setForeground(new Color(232, 192, 32));
        title.setBorder(new EmptyBorder(0, 10, 0, 0));

        timerPanel = new TimerPanel();

        JPanel titleLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleLeft.setOpaque(false);
        titleLeft.add(icon);
        titleLeft.add(title);

        titleRow.add(titleLeft, BorderLayout.WEST);
        titleRow.add(timerPanel, BorderLayout.EAST);

        // Question panel
        JPanel qPanel = new JPanel(new BorderLayout());
        qPanel.setOpaque(false);
        qPanel.setBorder(new EmptyBorder(10, 28, 10, 28));

        JLabel qLabel = new JLabel("<html><div style='text-align:center'>" + question + "</div></html>");
        qLabel.setFont(new Font("Arial", Font.BOLD, 19));
        qLabel.setForeground(new Color(255, 254, 240));
        qLabel.setHorizontalAlignment(SwingConstants.CENTER);
        qPanel.add(qLabel, BorderLayout.CENTER);

        JLabel hint = new JLabel("↑↓ change row   ←→ change column   ENTER to confirm");
        hint.setFont(new Font("Arial", Font.PLAIN, 11));
        hint.setForeground(TEXT_DIM);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        qPanel.add(hint, BorderLayout.SOUTH);

        // Answer buttons
        JPanel answerPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        answerPanel.setOpaque(false);
        answerPanel.setBorder(new EmptyBorder(4, 24, 24, 24));

        answerButtons = new AnswerButton[options.length];
        String[] prefixes = {"A", "B", "C", "D"};
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            answerButtons[i] = new AnswerButton(prefixes[i], options[i]);
            answerButtons[i].addActionListener(e -> selectAnswer(idx));
            answerButtons[i].addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setHovered(idx); }
            });
            answerPanel.add(answerButtons[i]);
        }

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleRow, BorderLayout.NORTH);
        top.add(qPanel,   BorderLayout.CENTER);

        root.add(top,         BorderLayout.NORTH);
        root.add(answerPanel, BorderLayout.CENTER);
        setContentPane(root);

        setHovered(0);

        final long openTime = System.currentTimeMillis();
        final long KEY_OPEN_GUARD_MS = 150;

        KeyAdapter keyNav = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (System.currentTimeMillis() - openTime < KEY_OPEN_GUARD_MS) return;
                int k = e.getKeyCode();
                if      (k == KeyEvent.VK_UP    || k == KeyEvent.VK_W) { setHovered(hoveredIndex ^ 2); return; }
                else if (k == KeyEvent.VK_DOWN  || k == KeyEvent.VK_S) { setHovered(hoveredIndex ^ 2); return; }
                else if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A) { setHovered(hoveredIndex ^ 1); return; }
                else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) { setHovered(hoveredIndex ^ 1); return; }
                else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) { selectAnswer(hoveredIndex); }
            }
        };
        addKeyListener(keyNav);
        root.addKeyListener(keyNav);
        qPanel.addKeyListener(keyNav);
        for (AnswerButton ab : answerButtons) ab.addKeyListener(keyNav);

        setFocusable(true);
        requestFocus();

        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (answered) { countdownTimer.cancel(); return; }
                    countdown--;
                    timerPanel.setSeconds(countdown);
                    if (countdown <= 0) {
                        answered = true;
                        countdownTimer.cancel();
                        dispose();
                    }
                });
            }
        }, 1000, 1000);
    }

    private void setHovered(int idx) {
        hoveredIndex = idx;
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setHighlighted(i == idx);
        }
    }

    private void selectAnswer(int index) {
        if (answered) return;
        answered = true;
        selectedAnswer = index;
        countdownTimer.cancel();
        dispose();
    }

    public static boolean showQuiz(Frame parent, String question,
                                   String[] options, int correctIndex) {
        QuizDialog dialog = new QuizDialog(parent, question, options, correctIndex);
        dialog.setVisible(true);
        return dialog.selectedAnswer == dialog.correctIndex;
    }

    // ── Inner class: AnswerButton ────────────────────────────────────
    private static class AnswerButton extends JButton {
        private final String prefix;
        private final String labelText;
        private boolean highlighted = false;

        AnswerButton(String prefix, String label) {
            super();
            this.prefix    = prefix;
            this.labelText = label;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(220, 58));
            setFont(new Font("Arial", Font.BOLD, 14));
        }

        void setHighlighted(boolean v) {
            highlighted = v;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (highlighted) {
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(3, 3, w-2, h-2, 12, 12);
                g2.setColor(new Color(224, 248, 216));
                g2.fillRoundRect(0, 0, w-1, h-1, 12, 12);
                g2.setColor(new Color(52, 160, 52));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(0, 0, w-2, h-2, 12, 12);
                g2.setColor(new Color(52, 160, 52));
                g2.fillOval(w/2 - 6, -6, 12, 12);
                g2.setColor(new Color(190, 40, 24));
                g2.fillRoundRect(w - 82, 6, 76, 18, 8, 8);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.setColor(Color.WHITE);
                g2.drawString("SELECTED", w - 78, 19);
                g2.setColor(new Color(52, 160, 52));
                g2.fillRoundRect(12, h/2 - 16, 30, 30, 8, 8);
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(prefix, 12 + (30 - fm.stringWidth(prefix))/2, h/2 + 6);
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                g2.setColor(new Color(10, 60, 10));
                g2.drawString(labelText, 52, h/2 + 6);
            } else {
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(3, 3, w-2, h-2, 12, 12);
                g2.setColor(new Color(255, 253, 230));
                g2.fillRoundRect(0, 0, w-1, h-1, 12, 12);
                g2.setColor(new Color(192, 160, 40));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(0, 0, w-2, h-2, 12, 12);
                g2.setColor(new Color(192, 160, 40));
                g2.fillOval(w/2 - 5, -5, 10, 10);
                g2.setColor(new Color(192, 160, 40));
                g2.fillRoundRect(12, h/2 - 15, 28, 28, 7, 7);
                g2.setFont(new Font("Arial", Font.BOLD, 15));
                g2.setColor(new Color(58, 32, 0));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(prefix, 12 + (28 - fm.stringWidth(prefix))/2, h/2 + 6);
                g2.setFont(new Font("Arial", Font.PLAIN, 15));
                g2.setColor(new Color(58, 32, 0));
                g2.drawString(labelText, 50, h/2 + 6);
            }
        }
    }

    // ── Inner class: TimerPanel ──────────────────────────────────────
    private static class TimerPanel extends JPanel {
        private int seconds = COUNTDOWN_SECONDS;
        private static final int SIZE = 54;

        TimerPanel() {
            setPreferredSize(new Dimension(SIZE + 24, SIZE + 20));
            setOpaque(false);
        }

        void setSeconds(int s) {
            seconds = Math.max(0, s);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int cx = getWidth() / 2, cy = getHeight() / 2 + 4;
            int r  = SIZE / 2;
            int ox = cx - r, oy = cy - r;

            g2.setColor(new Color(20, 50, 20, 120));
            g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(ox, oy, SIZE, SIZE);

            float frac = seconds / (float) COUNTDOWN_SECONDS;
            Color arcColor = frac > 0.5f ? new Color(60, 210, 90)
                           : frac > 0.25f ? new Color(232, 192, 32)
                           : new Color(210, 50, 50);
            g2.setColor(arcColor);
            g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int arcDeg = (int)(360 * frac);
            g2.drawArc(ox, oy, SIZE, SIZE, 90, arcDeg);

            int pillW = 36, pillH = 22;
            int px = cx - pillW/2, py = cy - pillH/2;
            g2.setColor(new Color(255, 253, 230, 220));
            g2.fillRoundRect(px, py, pillW, pillH, 8, 8);
            g2.setColor(new Color(192, 160, 40));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(px, py, pillW, pillH, 8, 8);

            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(new Color(58, 32, 0));
            FontMetrics fm = g2.getFontMetrics();
            String txt = String.valueOf(seconds);
            g2.drawString(txt, cx - fm.stringWidth(txt)/2, cy + fm.getAscent()/2 - 1);
        }
    }
}

