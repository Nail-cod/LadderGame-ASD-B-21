import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// ==========================================
// 1. SOUND MANAGER
// ==========================================
class SoundManager {
    public static void play(String filePath) {
        new Thread(() -> {
            try {
                File soundFile = new File(filePath);
                if (soundFile.exists()) {
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.start();
                }
            } catch (Exception e) {}
        }).start();
    }

    public static void playLoop(String filePath) {
        new Thread(() -> {
            try {
                File soundFile = new File(filePath);
                if (soundFile.exists()) {
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    try {
                        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        gainControl.setValue(-10.0f);
                    } catch (Exception ex) {}
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    clip.start();
                }
            } catch (Exception e) {}
        }).start();
    }
}

// ==========================================
// 2. DATA & ASSETS
// ==========================================

class PawnAsset {
    String path;
    BufferedImage image;
    Color themeColor;

    public PawnAsset(String path, Color themeColor) {
        this.path = path;
        this.themeColor = themeColor;
        try {
            this.image = ImageIO.read(new File(path));
        } catch (IOException e) {
            this.image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = this.image.createGraphics();
            g.setColor(themeColor);
            g.fillRect(0, 0, 50, 50);
            g.dispose();
        }
    }
}

class PersistentData implements Serializable {
    private static final long serialVersionUID = 1L;
    HashMap<String, PlayerStats> statsMap = new HashMap<>();
}

class PlayerStats implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    int totalWins;
    int totalScore;
    public PlayerStats(String name) { this.name = name; }
}

class Player {
    String name;
    PawnAsset asset;
    int position;
    int currentScore;
    Stack<Integer> stepHistory; 

    public Player(String name, PawnAsset asset) {
        this.name = name;
        this.asset = asset;
        this.position = 0;
        this.currentScore = 0;
        this.stepHistory = new Stack<>();
        this.stepHistory.push(0);
    }
    
    public void reset() {
        this.position = 0;
        this.currentScore = 0;
        this.stepHistory.clear();
        this.stepHistory.push(0);
    }
}

class LeaderboardManager {
    private static final String FILE_NAME = "zathura_leaderboard.dat";
    private PersistentData data;

    public LeaderboardManager() { loadData(); }

    private void loadData() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            data = (PersistentData) ois.readObject();
        } catch (Exception e) { data = new PersistentData(); }
    }

    public void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(data);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void updateStats(String name, int scoreToAdd, boolean isWin) {
        String key = name.trim().toLowerCase();
        PlayerStats stats = data.statsMap.getOrDefault(key, new PlayerStats(name));
        stats.name = name; 
        stats.totalScore += scoreToAdd;
        if (isWin) stats.totalWins++;
        data.statsMap.put(key, stats);
        saveData();
    }

    public List<PlayerStats> getTopScores() {
        return data.statsMap.values().stream().sorted((p1, p2) -> p2.totalScore - p1.totalScore).limit(5).collect(Collectors.toList());
    }
    public List<PlayerStats> getTopWins() {
        return data.statsMap.values().stream().sorted((p1, p2) -> p2.totalWins - p1.totalWins).limit(5).collect(Collectors.toList());
    }
}

class GameGraph {
    private int V;
    private List<List<Integer>> adj;
    public GameGraph(int v) { V = v; adj = new ArrayList<>(v); for (int i = 0; i < v; i++) adj.add(new ArrayList<>()); }
    public void addEdge(int src, int dest) { if (src >= 0 && src < V && dest >= 0 && dest < V) adj.get(src).add(dest); }
    public List<Integer> getShortestPath(int start, int finish) {
        int[] dist = new int[V]; int[] parent = new int[V];
        Arrays.fill(dist, Integer.MAX_VALUE); Arrays.fill(parent, -1);
        dist[start] = 0;
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[]{start, 0});
        while (!pq.isEmpty()) {
            int[] current = pq.poll(); int u = current[0];
            if (u == finish) break;
            for (int v : adj.get(u)) {
                if (dist[u] + 1 < dist[v]) { dist[v] = dist[u] + 1; parent[v] = u; pq.offer(new int[]{v, dist[v]}); }
            }
        }
        List<Integer> path = new ArrayList<>();
        int curr = finish;
        while (curr != -1) { path.add(0, curr); curr = parent[curr]; }
        if (path.isEmpty() || path.get(0) != start) return new ArrayList<>();
        return path;
    }
}

// ==========================================
// 3. KELAS UTAMA GAME
// ==========================================
public class ZathuraUltimate extends JFrame {
    
    // CONFIG
    private static final String IMG_PATH = "C:\\Users\\X1 Carbon\\Documents\\ITS\\ASD\\ASD FP\\Asset IMG\\Map Zathura Landscape.png";
    private static final String SOUND_DICE = "C:\\Users\\X1 Carbon\\Documents\\ITS\\ASD\\ASD FP\\Asset Sound\\SUARA DADU.wav";
    private static final String SOUND_MOVE = "C:\\Users\\X1 Carbon\\Documents\\ITS\\ASD\\ASD FP\\Asset Sound\\SUARA PION GERAK.wav";
    private static final String SOUND_BGM  = "C:\\Users\\X1 Carbon\\Documents\\ITS\\ASD\\ASD FP\\Asset Sound\\BACKSOUND.wav"; 

    private static final int ORG_W = 1920;
    private static final int ORG_H = 1080;

    // UI COLORS
    private static final Color COL_GREEN = new Color(46, 204, 113);
    private static final Color COL_RED = new Color(231, 76, 60);
    private static final Color COL_DARK_PANEL = new Color(0, 0, 0, 100);
    private static final Color COL_TERMINAL_BG = new Color(10, 15, 20, 220); 
    private static final Color COL_TERMINAL_TEXT = new Color(0, 255, 200); 

    public static final PawnAsset[] PAWN_ASSETS = {
        new PawnAsset("C:\\Users\\X1 Carbon\\Documents\\ITS\\ASD\\ASD FP\\Asset IMG\\PION IJO.png", Color.GREEN),
        new PawnAsset("C:\\Users\\X1 Carbon\\Documents\\ITS\\ASD\\ASD FP\\Asset IMG\\PION BIRU.png", Color.CYAN),
        new PawnAsset("C:\\Users\\X1 Carbon\\Documents\\ITS\\ASD\\ASD FP\\Asset IMG\\PION HITAM.png", Color.GRAY),
        new PawnAsset("C:\\Users\\X1 Carbon\\Documents\\ITS\\ASD\\ASD FP\\Asset IMG\\PION MERAH.png", Color.RED),
        new PawnAsset("C:\\Users\\X1 Carbon\\Documents\\ITS\\ASD\\ASD FP\\Asset IMG\\PION KUNING.png", Color.YELLOW)
    };

    private Point[] nodePoints = {
        new Point(1163, 709), new Point(1146, 638), new Point(1106, 583), new Point(1038, 577), new Point(977, 613),
        new Point(917, 651), new Point(852, 686), new Point(777, 689), new Point(702, 691), new Point(626, 695),
        new Point(558, 728), new Point(494, 766), new Point(430, 806), new Point(365, 845), new Point(295, 871),
        new Point(298, 803), new Point(335, 691), new Point(294, 592), new Point(287, 526), new Point(351, 534),
        new Point(423, 551), new Point(498, 567), new Point(569, 583), new Point(646, 599), new Point(728, 572),
        new Point(774, 513), new Point(817, 453), new Point(907, 338), new Point(951, 283), new Point(997, 226),
        new Point(1067, 200), new Point(1143, 216), new Point(1200, 265), new Point(1220, 339), new Point(1200, 412),
        new Point(1146, 466), new Point(1070, 479), new Point(997, 458), new Point(932, 431), new Point(861, 396),
        new Point(789, 362), new Point(714, 328), new Point(645, 297), new Point(575, 264), new Point(483, 221),
        new Point(387, 191), new Point(281, 210), new Point(253, 343), new Point(360, 450), new Point(518, 469),
        new Point(555, 360), new Point(376, 248), new Point(414, 355)
    };

    // --- LAYOUT DASHBOARD BARU (Dadu Paling Atas) ---
    private static final int DASH_CENTER_X = 1760;
    
    // 1. Dice (Top)
    private Point dicePos = new Point(DASH_CENTER_X, 120);
    
    // 2. Status Text (Below Dice)
    private Point statusPos = new Point(DASH_CENTER_X, 200);
    
    // 3. Log Screen (Middle-Bottom)
    private Rectangle logArea = new Rectangle(DASH_CENTER_X - 110, 680, 220, 100);

    // 4. Buttons (Bottom)
    private Rectangle btnRoll = new Rectangle(DASH_CENTER_X - 90, 820, 180, 70);
    private Rectangle btnReset = new Rectangle(DASH_CENTER_X - 90, 920, 180, 60);

    // State
    private BufferedImage bgImage;
    private Player[] players;
    private int currentPlayerIdx = 0;
    private int diceValue = 1;
    private boolean isRolling = false;
    private boolean isGreenStatus = true;
    private boolean isPrimeMode = false;
    private boolean extraTurn = false;
    
    private LinkedList<String> eventLog = new LinkedList<>();
    private Map<Integer, Integer> secretShortcuts = new HashMap<>();
    private Map<Integer, Integer> scoreNodes = new HashMap<>();
    private GameGraph gameGraph;
    
    private Random rand = new Random();
    private GamePanel panel;
    private LeaderboardManager leaderboard;

    public ZathuraUltimate() {
        SoundManager.playLoop(SOUND_BGM);
        leaderboard = new LeaderboardManager();

        ZathuraSetupDialog setup = new ZathuraSetupDialog(null, leaderboard);
        setup.setVisible(true);
        if (setup.isCancelled()) System.exit(0);
        this.players = setup.getResultPlayers();

        try {
            bgImage = ImageIO.read(new File(IMG_PATH));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Gambar tidak ditemukan: " + IMG_PATH);
            System.exit(0);
        }

        // Init Panel DULUAN
        panel = new GamePanel();

        initGameLogic();
        addEvent("SYSTEM: INITIALIZED.");
        addEvent("MISSION: REACH ZATHURA.");

        setTitle("ZATHURA");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);
        
        add(panel);
        setVisible(true);
    }

    private void addEvent(String msg) {
        eventLog.addFirst("> " + msg);
        if (eventLog.size() > 5) eventLog.removeLast();
        if (panel != null) panel.repaint();
    }

    private void initGameLogic() {
        secretShortcuts.clear();
        scoreNodes.clear();
        while (secretShortcuts.size() < 5) {
            int start = rand.nextInt(40) + 1;
            int end = start + rand.nextInt(10) + 5;
            if (end < nodePoints.length - 1 && !secretShortcuts.containsKey(start)) {
                secretShortcuts.put(start, end);
            }
        }
        while (scoreNodes.size() < 15) {
            int idx = rand.nextInt(nodePoints.length - 2) + 1;
            if (!scoreNodes.containsKey(idx)) scoreNodes.put(idx, (rand.nextInt(3) + 1) * 50);
        }
        buildGraph();
    }

    private void buildGraph() {
        gameGraph = new GameGraph(nodePoints.length);
        for(int i=0; i<nodePoints.length-1; i++) gameGraph.addEdge(i, i+1);
        for (Map.Entry<Integer, Integer> entry : secretShortcuts.entrySet()) {
            gameGraph.addEdge(entry.getKey(), entry.getValue());
        }
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) if (n % i == 0) return false;
        return true;
    }

    private boolean isStarNode(int index) {
        return ((index + 1) % 5 == 0) && (index + 1 < nodePoints.length);
    }

    private void onRollClick() {
        if (isRolling) return;
        extraTurn = false;
        int prob = rand.nextInt(100);
        isGreenStatus = (prob < 80);
        isPrimeMode = isPrime(players[currentPlayerIdx].position + 1);

        new Thread(() -> {
            isRolling = true;
            SoundManager.play(SOUND_DICE);
            addEvent("ROLLING DICE...");
            
            for (int i = 0; i < 15; i++) {
                diceValue = rand.nextInt(6) + 1;
                panel.repaint();
                try { Thread.sleep(60); } catch (Exception e) {}
            }
            diceValue = rand.nextInt(6) + 1;
            isRolling = false;
            
            String status = isGreenStatus ? "GREEN (FORWARD)" : "RED (RETREAT)";
            // addEvent("RESULT: " + diceValue + " | " + status); // Redundant if shown below dice
            if(isPrimeMode && isGreenStatus) addEvent("PRIME MODE: SHORTEST PATH!");
            
            panel.repaint();
            movePlayerWithStack();
        }).start();
    }

    private void movePlayerWithStack() {
        new Thread(() -> {
            Player p = players[currentPlayerIdx];
            int finish = nodePoints.length - 1;

            if (!isGreenStatus) { 
                for (int i = 0; i < diceValue; i++) {
                    if (p.stepHistory.size() > 1) { 
                        p.stepHistory.pop(); 
                        int prevPos = p.stepHistory.peek(); 
                        if (Math.abs(p.position - prevPos) > 1) addEvent("ALERT: RETREATING THRU WORMHOLE");
                        p.position = prevPos;
                        SoundManager.play(SOUND_MOVE);
                        panel.repaint();
                        try { Thread.sleep(400); } catch (Exception e) {}
                    } else break; 
                }
            } 
            else { 
                if (isPrimeMode) { 
                    List<Integer> path = gameGraph.getShortestPath(p.position, finish);
                    if (path.size() > 1) {
                        int stepsTaken = 0;
                        for (int i = 1; i < path.size() && stepsTaken < diceValue; i++) {
                            int nextNode = path.get(i);
                            if (Math.abs(nextNode - p.position) > 1) addEvent("SYSTEM: SHORTCUT TAKEN!");
                            p.position = nextNode;
                            p.stepHistory.push(nextNode); 
                            SoundManager.play(SOUND_MOVE);
                            panel.repaint();
                            try { Thread.sleep(400); } catch (Exception e) {}
                            stepsTaken++;
                        }
                    }
                } else {
                    for (int i = 0; i < diceValue; i++) {
                        if (p.position < finish) { 
                            p.position++; 
                            p.stepHistory.push(p.position); 
                            SoundManager.play(SOUND_MOVE);
                            panel.repaint(); 
                            try { Thread.sleep(300); } catch (Exception e) {} 
                        }
                    }
                }
            }

            if (scoreNodes.containsKey(p.position)) {
                int gained = scoreNodes.get(p.position);
                p.currentScore += gained;
                scoreNodes.remove(p.position);
                addEvent("DATA MINED: +" + gained + " PTS");
                panel.repaint();
            }

            if (isStarNode(p.position) && p.position != finish) {
                extraTurn = true;
                addEvent("STAR NODE REACHED! EXTRA TURN.");
            }

            if (p.position >= finish) {
                p.position = finish;
                panel.repaint();
                for(Player pl : players) leaderboard.updateStats(pl.name, pl.currentScore, pl.equals(p));
                addEvent("VICTORY! " + p.name.toUpperCase() + " WINS!");
                JOptionPane.showMessageDialog(this, p.name + " WINS!");
                resetGame();
            } else {
                if (!extraTurn) currentPlayerIdx = (currentPlayerIdx + 1) % players.length;
                isPrimeMode = false;
                panel.repaint();
            }
        }).start();
    }

    private void resetGame() {
        for (Player p : players) p.reset(); 
        currentPlayerIdx = 0;
        initGameLogic();
        diceValue = 1;
        addEvent("SYSTEM RESET. NEW GAME.");
        panel.repaint();
    }

    private class GamePanel extends JPanel {
        private double scale = 1.0;
        private int offX = 0, offY = 0;

        public GamePanel() {
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int mx = (int) ((e.getX() - offX) / scale);
                    int my = (int) ((e.getY() - offY) / scale);
                    if (btnRoll.contains(mx, my)) onRollClick();
                    else if (btnReset.contains(mx, my)) { if(JOptionPane.showConfirmDialog(null,"Reset?")==0) resetGame(); }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                public void mouseMoved(MouseEvent e) {
                    int mx = (int) ((e.getX() - offX) / scale);
                    int my = (int) ((e.getY() - offY) / scale);
                    if (btnRoll.contains(mx, my) || btnReset.contains(mx, my)) setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    else setCursor(Cursor.getDefaultCursor());
                }
            });
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            double sw = (double) getWidth() / ORG_W;
            double sh = (double) getHeight() / ORG_H;
            scale = Math.min(sw, sh);
            int dw = (int) (ORG_W * scale);
            int dh = (int) (ORG_H * scale);
            offX = (getWidth() - dw) / 2;
            offY = (getHeight() - dh) / 2;

            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (bgImage != null) g2.drawImage(bgImage, offX, offY, dw, dh, null);

            drawGameContent(g2, scale, offX, offY);
        }
        
        private void drawGameContent(Graphics2D g2, double s, int ox, int oy) {
            class C { int x(int v){return ox+(int)(v*s);} int y(int v){return oy+(int)(v*s);} int z(int v){return(int)(v*s);} }
            C c = new C();

            // 1. Trails
            for(Map.Entry<Integer, Integer> e : secretShortcuts.entrySet()){
                Point p1 = nodePoints[e.getKey()]; Point p2 = nodePoints[e.getValue()];
                QuadCurve2D q = new QuadCurve2D.Float();
                int ctrlX = (c.x(p1.x) + c.x(p2.x)) / 2 + c.z(50);
                int ctrlY = (c.y(p1.y) + c.y(p2.y)) / 2 - c.z(100);
                q.setCurve(c.x(p1.x), c.y(p1.y), ctrlX, ctrlY, c.x(p2.x), c.y(p2.y));
                g2.setPaint(new GradientPaint(c.x(p1.x), c.y(p1.y), new Color(255,200,0,180), c.x(p2.x), c.y(p2.y), new Color(255,50,0,50)));
                g2.setStroke(new BasicStroke(c.z(6), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(q);
            }

            // 2. Nodes & Scores
            for(int i=0; i<nodePoints.length; i++){
                Point p = nodePoints[i];
                int nx = c.x(p.x), ny = c.y(p.y);
                if(isStarNode(i)) { 
                    g2.setColor(Color.YELLOW); g2.setStroke(new BasicStroke(c.z(3)));
                    g2.drawOval(nx-c.z(15), ny-c.z(15), c.z(30), c.z(30)); 
                }
                if(scoreNodes.containsKey(i)) {
                    int val = scoreNodes.get(i);
                    g2.setColor(new Color(0, 255, 255, 180));
                    int r = c.z(12);
                    int[] xP = {nx, nx+r, nx, nx-r}, yP = {ny-r-c.z(20), ny-c.z(20), ny+r-c.z(20), ny-c.z(20)};
                    g2.fillPolygon(xP, yP, 4);
                    g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, c.z(10)));
                    g2.drawString("+"+val, nx-c.z(10), ny-c.z(25));
                }
                g2.setColor(Color.BLACK); g2.setFont(new Font("Arial", Font.BOLD, c.z(14)));
                g2.drawString(String.valueOf(i+1), nx-c.z(5), ny+c.z(5));
                g2.setColor(Color.WHITE);
                g2.drawString(String.valueOf(i+1), nx-c.z(7), ny+c.z(3));
            }

            // --- UI DASHBOARD (REARRANGED) ---
            
            // A. Dadu (Paling Atas)
            int dx = c.x(dicePos.x), dy = c.y(dicePos.y);
            int dSz = c.z(100);
            g2.setColor(isRolling ? Color.WHITE : (isGreenStatus ? COL_GREEN : COL_RED));
            g2.fillRoundRect(dx-dSz/2, dy-dSz/2, dSz, dSz, 20, 20);
            g2.setColor(Color.BLACK); g2.setStroke(new BasicStroke(c.z(4)));
            g2.drawRoundRect(dx-dSz/2, dy-dSz/2, dSz, dSz, 20, 20);
            drawDicePips(g2, dx, dy, dSz, diceValue);

            // B. Status (Di Bawah Dadu)
            g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, c.z(20)));
            String stTxt = isRolling ? "ROLLING..." : (isGreenStatus ? "GREEN (MAJU)" : "RED (MUNDUR)");
            g2.drawString(stTxt, c.x(statusPos.x) - g2.getFontMetrics().stringWidth(stTxt)/2, c.y(statusPos.y));

            // C. Turn & Score (Di Bawah Status)
            Player curr = players[currentPlayerIdx];
            g2.setColor(curr.asset.themeColor); g2.setFont(new Font("Impact", Font.BOLD, c.z(40)));
            String tTxt = "TURN: "+curr.name.toUpperCase();
            g2.drawString(tTxt, c.x(DASH_CENTER_X) - g2.getFontMetrics().stringWidth(tTxt)/2, c.y(260));
            g2.setColor(Color.WHITE); g2.setFont(new Font("Arial", Font.BOLD, c.z(20)));
            g2.drawString("SCORE: " + curr.currentScore, c.x(DASH_CENTER_X), c.y(300));

            // D. Leaderboards (Tengah)
            int lbX = c.x(DASH_CENTER_X) - c.z(110); 
            int lbW = c.z(220);
            int lbH = c.z(140);
            drawLBPanel(g2, lbX, c.y(350), lbW, lbH, "TOP 5 WINS", COL_GREEN, leaderboard.getTopWins(), true, c);
            drawLBPanel(g2, lbX, c.y(510), lbW, lbH, "TOP 5 SCORES", new Color(255, 215, 0), leaderboard.getTopScores(), false, c);

            // E. Log Screen (Di Bawah LB)
            int lgX = c.x(logArea.x), lgY = c.y(logArea.y);
            int lgW = c.z(logArea.width), lgH = c.z(logArea.height);
            g2.setColor(COL_TERMINAL_BG); g2.fillRoundRect(lgX, lgY, lgW, lgH, 10, 10);
            g2.setColor(COL_TERMINAL_TEXT); g2.setStroke(new BasicStroke(c.z(2)));
            g2.drawRoundRect(lgX, lgY, lgW, lgH, 10, 10);
            g2.setFont(new Font("Monospaced", Font.PLAIN, c.z(12)));
            int txtY = lgY + c.z(20);
            for(String msg : eventLog) {
                g2.drawString(msg, lgX + c.z(10), txtY);
                txtY += c.z(15);
            }

            // F. Buttons (Paling Bawah)
            drawStyledButton(g2, c, btnRoll, "ROLL", COL_GREEN);
            drawStyledButton(g2, c, btnReset, "RESET", COL_RED);

            // 4. Players
            for(int i=0; i<players.length; i++){
                Point p = nodePoints[players[i].position];
                int shift = (i-players.length/2)*c.z(15);
                int pw = c.z(50), ph = c.z(50);
                int px = c.x(p.x) + shift - pw/2, py = c.y(p.y) - ph;

                if(i == currentPlayerIdx) {
                    g2.setColor(new Color(255, 255, 255, 100)); g2.fillOval(px-5, py-5, pw+10, ph+10);
                }
                if (players[i].asset.image != null) g2.drawImage(players[i].asset.image, px, py, pw, ph, null);
                else { g2.setColor(players[i].asset.themeColor); g2.fillOval(px, py, pw, ph); }
            }
        }

        private void drawStyledButton(Graphics2D g2, Object helper, Rectangle rect, String text, Color baseColor) {
            try {
                java.lang.reflect.Method zM = helper.getClass().getDeclaredMethod("z", int.class);
                java.lang.reflect.Method xM = helper.getClass().getDeclaredMethod("x", int.class);
                java.lang.reflect.Method yM = helper.getClass().getDeclaredMethod("y", int.class);
                int x = (int)xM.invoke(helper, rect.x);
                int y = (int)yM.invoke(helper, rect.y);
                int w = (int)zM.invoke(helper, rect.width);
                int h = (int)zM.invoke(helper, rect.height);
                GradientPaint gp = new GradientPaint(x, y, baseColor.brighter(), x, y+h, baseColor.darker());
                g2.setPaint(gp); g2.fillRoundRect(x, y, w, h, 20, 20);
                g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(x, y, w, h, 20, 20);
                g2.setFont(new Font("Arial", Font.BOLD, (int)zM.invoke(helper, 24)));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, x + (w - fm.stringWidth(text))/2, y + (h + fm.getAscent())/2 - 5);
            } catch (Exception e) {}
        }

        private void drawLBPanel(Graphics2D g2, int x, int y, int w, int h, String title, Color titleColor, List<PlayerStats> data, boolean isWins, Object helper) {
            try {
                java.lang.reflect.Method zMethod = helper.getClass().getDeclaredMethod("z", int.class);
                int fontSize = (int) zMethod.invoke(helper, 14);
                int titleSize = (int) zMethod.invoke(helper, 16);
                int rowH = (int) zMethod.invoke(helper, 20);
                g2.setColor(COL_DARK_PANEL); g2.fillRoundRect(x, y, w, h, 15, 15);
                g2.setColor(titleColor); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(x, y, w, h, 15, 15);
                g2.setFont(new Font("Arial", Font.BOLD, titleSize));
                g2.drawString(title, x + w/2 - g2.getFontMetrics().stringWidth(title)/2, y + rowH);
                g2.setColor(Color.WHITE); g2.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
                int ty = y + rowH * 2 + 5;
                for(int i=0; i<Math.min(5, data.size()); i++) {
                    PlayerStats ps = data.get(i);
                    String val = isWins ? ps.totalWins + " W" : String.valueOf(ps.totalScore);
                    String row = (i+1) + ". " + ps.name + " (" + val + ")";
                    g2.drawString(row, x + 15, ty);
                    ty += rowH;
                }
            } catch (Exception e) {}
        }

        private void drawDicePips(Graphics2D g, int x, int y, int s, int val) {
            g.setColor(Color.BLACK);
            int r = s/5, c=0, l=-s/3, rgt=s/3, t=-s/3, b=s/3;
            if (val % 2 != 0) g.fillOval(x+c-r/2, y+c-r/2, r, r);
            if (val > 1) { g.fillOval(x+l-r/2, y+t-r/2, r, r); g.fillOval(x+rgt-r/2, y+b-r/2, r, r); }
            if (val > 3) { g.fillOval(x+rgt-r/2, y+t-r/2, r, r); g.fillOval(x+l-r/2, y+b-r/2, r, r); }
            if (val == 6) { g.fillOval(x+l-r/2, y+c-r/2, r, r); g.fillOval(x+rgt-r/2, y+c-r/2, r, r); }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ZathuraUltimate::new);
    }
}

// ==========================================
// SETUP DIALOG & COMPONENTS
// ==========================================
class ZathuraSetupDialog extends JDialog {
    private boolean cancelled = true;
    private List<PlayerSlotPanel> playerPanels = new ArrayList<>();
    private JPanel listContainer;
    private JSpinner spinnerCount;
    private Player[] resultPlayers;
    private LeaderboardManager leaderboard;

    public ZathuraSetupDialog(Frame owner, LeaderboardManager lb) {
        super(owner, "Setup", true);
        this.leaderboard = lb;
        setUndecorated(true);
        setSize(600, 700);
        setLocationRelativeTo(null);
        setBackground(new Color(0,0,0,0));

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 15, 30), 0, getHeight(), new Color(30, 20, 50));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(0, 200, 255)); g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 30, 30);
            }
        };
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("MISSION CONFIG", JLabel.CENTER);
        title.setFont(new Font("Impact", Font.PLAIN, 32));
        title.setForeground(new Color(0, 200, 255));
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ctrlPanel.setOpaque(false);
        JLabel lblCount = new JLabel("CREW: ");
        lblCount.setForeground(Color.WHITE);
        spinnerCount = new JSpinner(new SpinnerNumberModel(2, 2, 5, 1));
        spinnerCount.setPreferredSize(new Dimension(50, 30));
        spinnerCount.addChangeListener(e -> updatePlayerSlots());
        
        ctrlPanel.add(lblCount); ctrlPanel.add(spinnerCount);

        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);
        
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(ctrlPanel, BorderLayout.NORTH);
        centerWrap.add(new JScrollPane(listContainer) {{setOpaque(false); getViewport().setOpaque(false); setBorder(null);}}, BorderLayout.CENTER);
        mainPanel.add(centerWrap, BorderLayout.CENTER);

        JButton btnLaunch = new JButton("LAUNCH");
        btnLaunch.setBackground(new Color(0, 180, 50)); btnLaunch.setForeground(Color.WHITE);
        btnLaunch.setFont(new Font("Arial", Font.BOLD, 14));
        btnLaunch.addActionListener(e -> { if (saveData()) { cancelled = false; dispose(); } });
        
        JButton btnExit = new JButton("EXIT");
        btnExit.setBackground(new Color(150, 50, 50)); btnExit.setForeground(Color.WHITE);
        btnExit.setFont(new Font("Arial", Font.BOLD, 14));
        btnExit.addActionListener(e -> System.exit(0));

        JPanel btnPanel = new JPanel(); btnPanel.setOpaque(false);
        btnPanel.add(btnExit); btnPanel.add(btnLaunch);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
        updatePlayerSlots();
        
        MouseAdapter drag = new MouseAdapter() {
            int px, py;
            public void mousePressed(MouseEvent e) { px=e.getX(); py=e.getY(); }
            public void mouseDragged(MouseEvent e) { setLocation(e.getXOnScreen()-px, e.getYOnScreen()-py); }
        };
        mainPanel.addMouseListener(drag); mainPanel.addMouseMotionListener(drag);
    }

    private void updatePlayerSlots() {
        int count = (int) spinnerCount.getValue();
        listContainer.removeAll();
        playerPanels.clear();
        for (int i = 0; i < count; i++) {
            PawnAsset asset = ZathuraUltimate.PAWN_ASSETS[i % ZathuraUltimate.PAWN_ASSETS.length];
            PlayerSlotPanel p = new PlayerSlotPanel(i + 1, asset);
            playerPanels.add(p); listContainer.add(p); listContainer.add(Box.createVerticalStrut(10));
        }
        listContainer.revalidate(); listContainer.repaint();
    }

    private boolean saveData() {
        List<Player> list = new ArrayList<>();
        for (PlayerSlotPanel p : playerPanels) {
            String name = p.getNameInput();
            if (name.isEmpty()) return false;
            list.add(new Player(name, p.getSelectedAsset()));
        }
        resultPlayers = list.toArray(new Player[0]);
        return true;
    }

    public boolean isCancelled() { return cancelled; }
    public Player[] getResultPlayers() { return resultPlayers; }

    class PlayerSlotPanel extends JPanel {
        private JTextField txtName; private PawnAsset selectedAsset; private JLabel imgPreview;
        public PlayerSlotPanel(int index, PawnAsset defaultAsset) {
            this.selectedAsset = defaultAsset;
            setLayout(new BorderLayout(10, 5)); setOpaque(false);
            setBorder(new EmptyBorder(5, 5, 5, 5));
            setBackground(new Color(255, 255, 255, 20));
            txtName = new JTextField("Cadet " + index);
            txtName.setForeground(Color.WHITE); txtName.setBackground(new Color(50,50,60));
            txtName.setCaretColor(Color.WHITE); txtName.setBorder(new MatteBorder(0,0,1,0,Color.CYAN));
            
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); btnRow.setOpaque(false);
            for (PawnAsset asset : ZathuraUltimate.PAWN_ASSETS) {
                JButton b = new JButton(new ImageIcon(asset.image.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                b.setPreferredSize(new Dimension(30, 30));
                b.setBackground(Color.DARK_GRAY);
                b.addActionListener(e -> { 
                    selectedAsset = asset; 
                    imgPreview.setIcon(new ImageIcon(asset.image.getScaledInstance(30, 30, Image.SCALE_SMOOTH))); 
                });
                btnRow.add(b);
            }
            imgPreview = new JLabel(new ImageIcon(selectedAsset.image.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
            add(txtName, BorderLayout.CENTER); add(imgPreview, BorderLayout.EAST); add(btnRow, BorderLayout.SOUTH);
        }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(50, 50, 70, 200)); g.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            super.paintComponent(g);
        }
        public String getNameInput() { return txtName.getText().trim(); }
        public PawnAsset getSelectedAsset() { return selectedAsset; }
    }
}