package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class ZathuraSetupDialog extends JDialog {
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