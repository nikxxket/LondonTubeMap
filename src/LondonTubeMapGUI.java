import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class LondonTubeMapGUI extends JFrame implements LondonTubeMap.UIListener {
    private static final int WIDTH = 1300;
    private static final int HEIGHT = 900;

    private LondonTubeMap tubeMap;
    private MapPanel mapPanel;

    private JComboBox<String> startComboBox, endComboBox;
    private JLabel pathLabel, distanceLabel, agentStatusLabel;
    private JButton startAgentButton, stepAgentButton, resetAgentButton;
    private JButton findPathButton;

    public LondonTubeMapGUI() {
        tubeMap = new LondonTubeMap();
        tubeMap.setUIListener(this);

        setTitle("Карта Лондонского Метро - Агент");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Верхняя панель управления
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Панель выбора станций
        JPanel controlPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Выбор маршрута"));

        String[] stationNames = tubeMap.getStations().keySet().toArray(new String[0]);
        Arrays.sort(stationNames);
        startComboBox = new JComboBox<>(stationNames);
        endComboBox = new JComboBox<>(stationNames);
        endComboBox.setSelectedItem("Tower Hill");

        findPathButton = new JButton("Найти кратчайший путь");
        findPathButton.addActionListener(e -> {
            tubeMap.findShortestPath((String) startComboBox.getSelectedItem(),
                    (String) endComboBox.getSelectedItem());
        });

        controlPanel.add(new JLabel("Начальная станция:"));
        controlPanel.add(new JLabel("Конечная станция:"));
        controlPanel.add(startComboBox);
        controlPanel.add(endComboBox);

        // Панель кнопок агента
        JPanel agentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        agentPanel.setBorder(BorderFactory.createTitledBorder("Управление агентом"));

        startAgentButton = new JButton("Запустить агента");
        stepAgentButton = new JButton("Шаг агента");
        resetAgentButton = new JButton("Сбросить агента");

        startAgentButton.addActionListener(e -> {
            tubeMap.startAgent((String) startComboBox.getSelectedItem(),
                    (String) endComboBox.getSelectedItem());
        });
        stepAgentButton.addActionListener(e -> tubeMap.stepAgent());
        resetAgentButton.addActionListener(e -> tubeMap.resetAgent());

        agentPanel.add(startAgentButton);
        agentPanel.add(stepAgentButton);
        agentPanel.add(resetAgentButton);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(findPathButton, BorderLayout.NORTH);
        buttonPanel.add(agentPanel, BorderLayout.SOUTH);

        topPanel.add(controlPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Нижняя панель результатов
        JPanel resultPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        resultPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        pathLabel = new JLabel("Выберите станции");
        distanceLabel = new JLabel("Расстояние: 0.0 км");
        agentStatusLabel = new JLabel("Агент: не активен");

        Font smallFont = new Font("Arial", Font.PLAIN, 12);
        pathLabel.setFont(smallFont);
        distanceLabel.setFont(smallFont);
        agentStatusLabel.setFont(smallFont);

        pathLabel.setBackground(new Color(240, 240, 240));
        pathLabel.setOpaque(true);
        pathLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        resultPanel.add(pathLabel);
        resultPanel.add(distanceLabel);
        resultPanel.add(agentStatusLabel);

        // Панель карты
        mapPanel = new MapPanel();
        mapPanel.addMouseListener(new MapMouseListener());

        add(topPanel, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        add(resultPanel, BorderLayout.SOUTH);
    }

    @Override
    public void onPathFound(java.util.List<String> path, double distance) {
        if (path.isEmpty()) {
            pathLabel.setText("Путь не найден");
            distanceLabel.setText("Расстояние: 0.0 км");
        } else {
            StringBuilder pathText = new StringBuilder("<html><b>Кратчайший маршрут:</b> ");
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) {
                    String from = path.get(i - 1);
                    String to = path.get(i);
                    String lineInfo = tubeMap.getLineInfoForConnection(from, to);
                    pathText.append(" ").append(lineInfo).append(" → ");
                }
                pathText.append(path.get(i));
            }
            pathText.append("</html>");
            pathLabel.setText(pathText.toString());
            distanceLabel.setText(String.format("Расстояние: %.1f км", distance));
        }
    }

    @Override
    public void onAgentMoved(String currentStation, int steps, boolean reached, double utility, java.util.List<String> path) {
        if (reached) {
            agentStatusLabel.setText("<html>Агент: достиг цели за " + steps + " шагов!<br>Путь: " +
                    String.join(" → ", path) + "</html>");
            startAgentButton.setEnabled(true);
            stepAgentButton.setEnabled(true);
        } else {
            agentStatusLabel.setText(String.format("Агент: шаг %d, текущая позиция: %s → цель: %s | Utility: %.2f",
                    steps, currentStation, endComboBox.getSelectedItem(), utility));
        }
    }

    @Override
    public void onAgentReset() {
        agentStatusLabel.setText("Агент: сброшен");
        startAgentButton.setEnabled(true);
        stepAgentButton.setEnabled(true);
    }

    @Override
    public void repaintMap() {
        mapPanel.repaint();
    }

    class MapPanel extends JPanel {
        private final Map<String, Color> lineColors = Map.of(
                "Red", new Color(255, 59, 48),
                "Yellow", new Color(255, 204, 0),
                "Green", new Color(52, 199, 89),
                "Blue", new Color(0, 122, 255),
                "Transfer", Color.GRAY
        );

        public MapPanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT - 200));
            setBackground(new Color(250, 250, 250));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            drawConnections(g2d);
            drawDistanceLabels(g2d);
            drawStations(g2d);
            drawShortestPath(g2d);
            drawAgent(g2d);
            drawLegend(g2d);
            drawUtilities(g2d);
        }

        private void drawConnections(Graphics2D g2d) {
            for (java.util.List<LondonTubeMap.Connection> connections : tubeMap.getGraph().values()) {
                for (LondonTubeMap.Connection conn : connections) {
                    if (!conn.from.equals(conn.to)) {
                        LondonTubeMap.Station fromStation = tubeMap.getStations().get(conn.from);
                        LondonTubeMap.Station toStation = tubeMap.getStations().get(conn.to);
                        if (fromStation != null && toStation != null) {
                            g2d.setColor(lineColors.get(conn.line));
                            g2d.setStroke(new BasicStroke(5));
                            g2d.drawLine(fromStation.x, fromStation.y, toStation.x, toStation.y);
                        }
                    }
                }
            }
        }

        private void drawDistanceLabels(Graphics2D g2d) {
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            for (java.util.List<LondonTubeMap.Connection> connections : tubeMap.getGraph().values()) {
                for (LondonTubeMap.Connection conn : connections) {
                    if (!conn.from.equals(conn.to) && conn.distance > 0) {
                        LondonTubeMap.Station fromStation = tubeMap.getStations().get(conn.from);
                        LondonTubeMap.Station toStation = tubeMap.getStations().get(conn.to);
                        if (fromStation != null && toStation != null) {
                            drawDistanceLabel(g2d, fromStation, toStation, conn.distance);
                        }
                    }
                }
            }
        }

        private void drawDistanceLabel(Graphics2D g2d, LondonTubeMap.Station from,
                                       LondonTubeMap.Station to, double distance) {
            int midX = (from.x + to.x) / 2;
            int midY = (from.y + to.y) / 2;
            int offsetX = 0, offsetY = 0;
            if (Math.abs(from.x - to.x) > Math.abs(from.y - to.y)) {
                offsetY = -12;
            } else {
                offsetX = 12;
            }
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.fillRoundRect(midX + offsetX - 18, midY + offsetY - 8, 36, 16, 8, 8);
            g2d.setColor(Color.BLACK);
            String distanceText = String.format("%.1fкм", distance);
            g2d.drawString(distanceText, midX + offsetX - 15, midY + offsetY + 3);
        }

        private void drawStations(Graphics2D g2d) {
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            for (LondonTubeMap.Station station : tubeMap.getStations().values()) {
                drawStationCircle(g2d, station);
                g2d.setColor(Color.BLACK);
                drawStationLabel(g2d, station);
            }
        }

        private void drawStationCircle(Graphics2D g2d, LondonTubeMap.Station station) {
            int size = station.lines.size() > 1 ? 16 : 12;

            if (station.lines.size() > 1) {
                g2d.setColor(Color.WHITE);
                g2d.fillOval(station.x - size/2, station.y - size/2, size, size);
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(station.x - size/2, station.y - size/2, size, size);
            }

            int smallSize = 8;
            int offset = 0;
            for (String line : station.lines) {
                g2d.setColor(lineColors.get(line));
                int xOffset = 0, yOffset = 0;
                if (station.lines.size() == 2) {
                    xOffset = offset == 0 ? -3 : 3;
                    yOffset = offset == 0 ? -3 : 3;
                } else if (station.lines.size() > 2) {
                    xOffset = (offset % 2 == 0) ? -2 : 2;
                    yOffset = (offset / 2 == 0) ? -2 : 2;
                }
                g2d.fillOval(station.x - smallSize/2 + xOffset, station.y - smallSize/2 + yOffset,
                        smallSize, smallSize);
                offset++;
            }
        }

        private void drawStationLabel(Graphics2D g2d, LondonTubeMap.Station station) {
            int offsetX = 12;
            int offsetY = -12;

            if (station.y < 150) {
                offsetY = 15;
            } else if (station.y > HEIGHT - 200) {
                offsetY = -20;
            }
            if (station.x > WIDTH - 200) {
                offsetX = -100;
            }

            g2d.drawString(station.name, station.x + offsetX, station.y + offsetY);
            if (station.lines.size() > 1) {
                g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                String linesText = String.join(", ", station.lines);
                g2d.drawString("(" + linesText + ")", station.x + offsetX, station.y + offsetY + 12);
                g2d.setFont(new Font("Arial", Font.BOLD, 11));
            }
        }

        private void drawShortestPath(Graphics2D g2d) {
            java.util.List<String> shortestPath = tubeMap.getShortestPath();
            if (shortestPath.size() < 2) return;

            g2d.setColor(new Color(255, 0, 255, 180));
            g2d.setStroke(new BasicStroke(7));
            for (int i = 0; i < shortestPath.size() - 1; i++) {
                LondonTubeMap.Station from = tubeMap.getStations().get(shortestPath.get(i));
                LondonTubeMap.Station to = tubeMap.getStations().get(shortestPath.get(i + 1));
                if (from != null && to != null) {
                    g2d.drawLine(from.x, from.y, to.x, to.y);
                }
            }

            for (String stationName : shortestPath) {
                LondonTubeMap.Station station = tubeMap.getStations().get(stationName);
                if (station != null) {
                    g2d.setColor(new Color(255, 0, 255));
                    g2d.fillOval(station.x - 8, station.y - 8, 16, 16);
                    g2d.setColor(Color.WHITE);
                    g2d.drawOval(station.x - 8, station.y - 8, 16, 16);
                }
            }
        }

        private void drawAgent(Graphics2D g2d) {
            String agentCurrentStation = tubeMap.getAgentCurrentStation();
            if (agentCurrentStation != null) {
                LondonTubeMap.Station agentStation = tubeMap.getStations().get(agentCurrentStation);
                if (agentStation != null) {
                    g2d.setColor(new Color(52, 199, 89));
                    int[] xPoints = {agentStation.x, agentStation.x - 12, agentStation.x + 12};
                    int[] yPoints = {agentStation.y - 18, agentStation.y + 10, agentStation.y + 10};
                    g2d.fillPolygon(xPoints, yPoints, 3);
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawPolygon(xPoints, yPoints, 3);

                    java.util.List<String> agentPath = tubeMap.getAgentPath();
                    if (agentPath.size() > 1) {
                        g2d.setColor(new Color(52, 199, 89, 100));
                        g2d.setStroke(new BasicStroke(4));
                        for (int i = 0; i < agentPath.size() - 1; i++) {
                            LondonTubeMap.Station from = tubeMap.getStations().get(agentPath.get(i));
                            LondonTubeMap.Station to = tubeMap.getStations().get(agentPath.get(i + 1));
                            if (from != null && to != null) {
                                g2d.drawLine(from.x, from.y, to.x, to.y);
                            }
                        }
                    }
                }
            }
        }

        private void drawUtilities(Graphics2D g2d) {
            if (tubeMap.getAgentCurrentStation() == null) return;
            g2d.setFont(new Font("Arial", Font.PLAIN, 9));
            g2d.setColor(new Color(100, 100, 100));
            for (LondonTubeMap.Station station : tubeMap.getStations().values()) {
                double utility = tubeMap.getUtilities().getOrDefault(station.name, 0.0);
                String utilityText = String.format("%.1f", utility);
                g2d.drawString(utilityText, station.x - 18, station.y - 18);
            }
        }

        private void drawLegend(Graphics2D g2d) {
            int legendX = WIDTH - 210;
            int legendY = 20;
            g2d.setColor(new Color(255, 255, 255, 240));
            g2d.fillRoundRect(legendX - 10, legendY - 10, 200, 230, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRoundRect(legendX - 10, legendY - 10, 200, 230, 10, 10);
            g2d.setFont(new Font("Arial", Font.BOLD, 13));
            g2d.drawString("Легенда:", legendX, legendY + 10);

            String[] lines = {"Красная: Bakerloo Line", "Желтая: Central Line",
                    "Зеленая: Piccadilly Line", "Синяя: Northern Line"};
            Color[] colors = {new Color(255, 59, 48), new Color(255, 204, 0),
                    new Color(52, 199, 89), new Color(0, 122, 255)};
            for (int i = 0; i < lines.length; i++) {
                g2d.setColor(colors[i]);
                g2d.fillRect(legendX, legendY + 25 + i * 25, 20, 4);
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.PLAIN, 11));
                g2d.drawString(lines[i], legendX + 25, legendY + 30 + i * 25);
            }

            g2d.setColor(new Color(52, 199, 89));
            int[] xPoints = {legendX + 10, legendX + 3, legendX + 17};
            int[] yPoints = {legendY + 130, legendY + 145, legendY + 145};
            g2d.fillPolygon(xPoints, yPoints, 3);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Агент", legendX + 25, legendY + 140);

            g2d.setColor(new Color(100, 100, 100));
            g2d.drawString("Utility значения", legendX, legendY + 165);
            g2d.drawString("показаны сверху", legendX, legendY + 180);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Станции пересадок:", legendX, legendY + 200);
            g2d.drawString("• Двойные круги", legendX + 10, legendY + 215);
        }
    }

    class MapMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            for (LondonTubeMap.Station station : tubeMap.getStations().values()) {
                double distance = Math.sqrt(Math.pow(e.getX() - station.x, 2) +
                        Math.pow(e.getY() - station.y, 2));
                if (distance < 15) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        startComboBox.setSelectedItem(station.name);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        endComboBox.setSelectedItem(station.name);
                    }
                    tubeMap.findShortestPath((String) startComboBox.getSelectedItem(),
                            (String) endComboBox.getSelectedItem());
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            LondonTubeMapGUI gui = new LondonTubeMapGUI();
            gui.setVisible(true);
        });
    }
}