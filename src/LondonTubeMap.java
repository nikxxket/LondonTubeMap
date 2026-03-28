import java.util.*;

public class LondonTubeMap {
    private Map<String, Station> stations = new HashMap<>();
    private Map<String, List<Connection>> graph = new HashMap<>();
    private String startStation = null;
    private String endStation = null;
    private List<String> shortestPath = new ArrayList<>();
    private double totalDistance = 0;

    // Агент и его состояние
    private String agentCurrentStation = null;
    private List<String> agentPath = new ArrayList<>();
    private int agentSteps = 0;
    private boolean agentReachedTarget = false;
    private Random random = new Random();

    // Параметры для вероятностной модели
    private double gamma = 0.9;
    private Map<String, Double> utilities = new HashMap<>();

    private UIListener uiListener;

    public interface UIListener {
        void onPathFound(List<String> path, double distance);
        void onAgentMoved(String currentStation, int steps, boolean reached, double utility, List<String> path);
        void onAgentReset();
        void repaintMap();
    }

    public void setUIListener(UIListener listener) {
        this.uiListener = listener;
    }

    public LondonTubeMap() {
        initializeStations();
        initializeConnections();
        buildGraph();
        initializeUtilities();
    }

    public Map<String, Station> getStations() {
        return stations;
    }

    public Map<String, List<Connection>> getGraph() {
        return graph;
    }

    public List<String> getShortestPath() {
        return shortestPath;
    }

    public String getAgentCurrentStation() {
        return agentCurrentStation;
    }

    public List<String> getAgentPath() {
        return agentPath;
    }

    public Map<String, Double> getUtilities() {
        return utilities;
    }

    class Station {
        String name;
        int x, y;
        List<String> lines;

        Station(String name, int x, int y, List<String> lines) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.lines = lines;
        }

        void addLine(String line) {
            if (!lines.contains(line)) {
                lines.add(line);
            }
        }
    }

    class Connection {
        String from, to;
        double distance;
        String line;

        Connection(String from, String to, double distance, String line) {
            this.from = from;
            this.to = to;
            this.distance = distance;
            this.line = line;
        }
    }

    private void initializeStations() {
        int centerX = 600;
        int centerY = 400;

        // Верхняя линия (красная)
        stations.put("Baker St.", new Station("Baker St.", centerX - 400, centerY - 180,
                Arrays.asList("Red")));
        stations.put("King's Cross st.", new Station("King's Cross st.", centerX - 200,
                centerY - 180, Arrays.asList("Red", "Green")));
        stations.put("Moorgate", new Station("Moorgate", centerX, centerY - 180,
                Arrays.asList("Red", "Blue")));
        stations.put("Liverpool st st.", new Station("Liverpool st st.", centerX + 200,
                centerY - 180, Arrays.asList("Red", "Yellow")));
        stations.put("Tower Hill", new Station("Tower Hill", centerX + 400, centerY -
                180, Arrays.asList("Red")));

        // Средняя линия (желтая)
        stations.put("Bond st.", new Station("Bond st.", centerX - 400, centerY - 60,
                Arrays.asList("Yellow")));
        stations.put("Oxford circ.", new Station("Oxford circ.", centerX - 200, centerY -
                60, Arrays.asList("Yellow")));
        stations.put("Chancery Lane", new Station("Chancery Lane", centerX, centerY - 60,
                Arrays.asList("Yellow", "Green")));
        stations.put("Bank", new Station("Bank", centerX + 200, centerY - 60,
                Arrays.asList("Yellow", "Blue")));

        // Нижняя линия (зеленая)
        stations.put("Covent Garden", new Station("Covent Garden", centerX - 200, centerY
                + 60, Arrays.asList("Green")));
        stations.put("Piccadilly circ", new Station("Piccadilly circ", centerX - 200,
                centerY + 180, Arrays.asList("Green")));
        stations.put("Green Park", new Station("Green Park", centerX - 200, centerY +
                300, Arrays.asList("Green")));

        // Дополнительные станции (синяя)
        stations.put("Old st", new Station("Old st", centerX, centerY - 300,
                Arrays.asList("Blue")));
        stations.put("Monument", new Station("Monument", centerX + 200, centerY + 60,
                Arrays.asList("Blue")));
    }

    private void initializeConnections() {
        // Красная линия
        addConnection("Baker St.", "King's Cross st.", 2.1, "Red");
        addConnection("King's Cross st.", "Moorgate", 1.8, "Red");
        addConnection("Moorgate", "Liverpool st st.", 1.2, "Red");
        addConnection("Liverpool st st.", "Tower Hill", 1.5, "Red");

        // Желтая линия
        addConnection("Bond st.", "Oxford circ.", 1.3, "Yellow");
        addConnection("Oxford circ.", "Chancery Lane", 1.5, "Yellow");
        addConnection("Chancery Lane", "Bank", 1.2, "Yellow");
        addConnection("Bank", "Liverpool st st.", 1.0, "Yellow");

        // Зеленая линия
        addConnection("King's Cross st.", "Chancery Lane", 1.4, "Green");
        addConnection("Chancery Lane", "Covent Garden", 1.6, "Green");
        addConnection("Covent Garden", "Piccadilly circ", 0.8, "Green");
        addConnection("Piccadilly circ", "Green Park", 1.1, "Green");

        // Синяя линия
        addConnection("Old st", "Moorgate", 0.9, "Blue");
        addConnection("Moorgate", "Bank", 1.3, "Blue");
        addConnection("Bank", "Monument", 0.5, "Blue");

        // Пересадки между линиями
        addConnection("King's Cross st.", "King's Cross st.", 0.0, "Transfer");
        addConnection("Chancery Lane", "Chancery Lane", 0.0, "Transfer");
        addConnection("Bank", "Bank", 0.0, "Transfer");
        addConnection("Moorgate", "Moorgate", 0.0, "Transfer");
        addConnection("Liverpool st st.", "Liverpool st st.", 0.0, "Transfer");
    }

    private void addConnection(String from, String to, double distance, String line) {
        graph.putIfAbsent(from, new ArrayList<>());
        graph.putIfAbsent(to, new ArrayList<>());
        if (!from.equals(to) || line.equals("Transfer")) {
            graph.get(from).add(new Connection(from, to, distance, line));
            if (!from.equals(to)) {
                graph.get(to).add(new Connection(to, from, distance, line));
            }
        }
    }

    private void buildGraph() {
        // Граф уже построен в initializeConnections
    }

    private void initializeUtilities() {
        for (String station : graph.keySet()) {
            utilities.put(station, 0.0);
        }
    }

    private double getReward(String station, String target) {
        if (station.equals(target)) {
            return 100.0;
        }
        Station currentStation = stations.get(station);
        Station targetStation = stations.get(target);
        if (currentStation != null && targetStation != null) {
            double distance = Math.sqrt(
                    Math.pow(currentStation.x - targetStation.x, 2) +
                            Math.pow(currentStation.y - targetStation.y, 2)
            );
            return -distance / 100.0;
        }
        return -1.0;
    }

    private double getTransitionProbability(String from, String to, String target) {
        List<Connection> connections = graph.get(from);
        if (connections == null) return 0.0;

        double totalWeight = 0.0;
        Map<String, Double> weights = new HashMap<>();
        for (Connection conn : connections) {
            if (!conn.from.equals(conn.to)) {
                double weight = 1.0 / (conn.distance + 0.1);
                if (isMovingTowardTarget(conn.to, from, target)) {
                    weight *= 2.0;
                }
                weights.put(conn.to, weight);
                totalWeight += weight;
            }
        }
        if (totalWeight == 0.0 || !weights.containsKey(to)) {
            return 0.0;
        }
        return weights.get(to) / totalWeight;
    }

    private boolean isMovingTowardTarget(String candidate, String current, String target) {
        Station candidateStation = stations.get(candidate);
        Station currentStation = stations.get(current);
        Station targetStation = stations.get(target);
        if (candidateStation == null || currentStation == null || targetStation == null)
            return false;

        double currentDistance = Math.sqrt(
                Math.pow(currentStation.x - targetStation.x, 2) +
                        Math.pow(currentStation.y - targetStation.y, 2)
        );
        double candidateDistance = Math.sqrt(
                Math.pow(candidateStation.x - targetStation.x, 2) +
                        Math.pow(candidateStation.y - targetStation.y, 2)
        );
        return candidateDistance < currentDistance;
    }

    private double calculateUtility(String station, String target) {
        double reward = getReward(station, target);
        if (station.equals(target)) {
            return reward;
        }

        List<Connection> connections = graph.get(station);
        if (connections == null) return reward;

        double maxExpectedUtility = Double.NEGATIVE_INFINITY;

        for (Connection conn : connections) {
            if (!conn.from.equals(conn.to)) {
                String nextStation = conn.to;
                double transitionProb = getTransitionProbability(station, nextStation, target);
                double expectedUtility = transitionProb * utilities.getOrDefault(nextStation, 0.0);
                if (expectedUtility > maxExpectedUtility) {
                    maxExpectedUtility = expectedUtility;
                }
            }
        }
        if (maxExpectedUtility == Double.NEGATIVE_INFINITY) {
            return reward;
        }
        return reward + gamma * maxExpectedUtility;
    }

    private void updateUtilities(String target) {
        Map<String, Double> newUtilities = new HashMap<>();
        for (String station : graph.keySet()) {
            newUtilities.put(station, calculateUtility(station, target));
        }
        utilities.putAll(newUtilities);
    }

    public void startAgent(String start, String end) {
        if (start == null || end == null) return;
        if (start.equals(end)) return;

        for (int i = 0; i < 10; i++) {
            updateUtilities(end);
        }

        agentCurrentStation = start;
        agentPath.clear();
        agentPath.add(start);
        agentSteps = 0;
        agentReachedTarget = false;

        if (uiListener != null) {
            uiListener.onAgentMoved(agentCurrentStation, agentSteps, false,
                    utilities.getOrDefault(agentCurrentStation, 0.0), agentPath);
            uiListener.repaintMap();
        }
    }

    public void stepAgent() {
        if (agentCurrentStation == null || agentReachedTarget) {
            return;
        }

        agentSteps++;
        String nextStation = selectNextStationByUtility(agentCurrentStation);

        agentCurrentStation = nextStation;
        agentPath.add(nextStation);

        if (agentCurrentStation.equals(endStation)) {
            agentReachedTarget = true;
            if (uiListener != null) {
                uiListener.onAgentMoved(agentCurrentStation, agentSteps, true, 0, agentPath);
            }
        } else {
            if (uiListener != null) {
                uiListener.onAgentMoved(agentCurrentStation, agentSteps, false,
                        utilities.getOrDefault(agentCurrentStation, 0.0), agentPath);
            }
        }

        if (uiListener != null) {
            uiListener.repaintMap();
        }
    }

    private String selectNextStationByUtility(String currentStation) {
        List<Connection> connections = graph.get(currentStation);
        if (connections == null || connections.isEmpty()) {
            return currentStation;
        }

        String bestStation = currentStation;
        double maxUtility = Double.NEGATIVE_INFINITY;
        for (Connection conn : connections) {
            if (!conn.from.equals(conn.to)) {
                String nextStation = conn.to;
                double utility = utilities.getOrDefault(nextStation, 0.0);
                if (utility > maxUtility) {
                    maxUtility = utility;
                    bestStation = nextStation;
                }
            }
        }

        if (random.nextDouble() < 0.1) {
            List<String> possibleStations = new ArrayList<>();
            for (Connection conn : connections) {
                if (!conn.from.equals(conn.to)) {
                    possibleStations.add(conn.to);
                }
            }
            if (!possibleStations.isEmpty()) {
                return possibleStations.get(random.nextInt(possibleStations.size()));
            }
        }
        return bestStation;
    }

    public void resetAgent() {
        agentCurrentStation = null;
        agentPath.clear();
        agentSteps = 0;
        agentReachedTarget = false;

        if (uiListener != null) {
            uiListener.onAgentReset();
            uiListener.repaintMap();
        }
    }

    public void findShortestPath(String start, String end) {
        startStation = start;
        endStation = end;

        if (startStation == null || endStation == null) return;
        if (startStation.equals(endStation)) return;

        shortestPath = dijkstra(startStation, endStation);

        if (uiListener != null) {
            if (shortestPath.isEmpty()) {
                uiListener.onPathFound(new ArrayList<>(), 0);
            } else {
                uiListener.onPathFound(shortestPath, totalDistance);
            }
            uiListener.repaintMap();
        }
    }

    private List<String> dijkstra(String start, String end) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(
                Comparator.comparingDouble(station -> distances.getOrDefault(station, Double.MAX_VALUE))
        );

        for (String station : graph.keySet()) {
            distances.put(station, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(end)) {
                break;
            }

            for (Connection conn : graph.get(current)) {
                double newDist = distances.get(current) + conn.distance;
                if (newDist < distances.get(conn.to)) {
                    distances.put(conn.to, newDist);
                    previous.put(conn.to, current);
                    queue.add(conn.to);
                }
            }
        }

        List<String> path = new ArrayList<>();
        String current = end;
        totalDistance = distances.get(end);
        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }
        return path.get(0).equals(start) ? path : new ArrayList<>();
    }

    public String getLineInfoForConnection(String from, String to) {
        List<Connection> connections = graph.get(from);
        if (connections != null) {
            for (Connection conn : connections) {
                if (conn.to.equals(to) && !conn.line.equals("Transfer")) {
                    switch (conn.line) {
                        case "Red": return "[Красная]";
                        case "Yellow": return "[Желтая]";
                        case "Green": return "[Зеленая]";
                        case "Blue": return "[Синяя]";
                    }
                }
            }
        }
        return "[Пересадка]";
    }

    public boolean isAgentReachedTarget() {
        return agentReachedTarget;
    }

    public int getAgentSteps() {
        return agentSteps;
    }
}