package src;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private static final String FILE_NAME = "zathura_leaderboard.dat";
    private PersistentData data;

    public LeaderboardManager() {
        loadData();
    }

    private void loadData() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            data = (PersistentData) ois.readObject();
        } catch (Exception e) {
            data = new PersistentData();
        }
    }

    public void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        return data.statsMap.values().stream()
                .sorted((p1, p2) -> p2.totalScore - p1.totalScore)
                .limit(5).collect(Collectors.toList());
    }

    public List<PlayerStats> getTopWins() {
        return data.statsMap.values().stream()
                .sorted((p1, p2) -> p2.totalWins - p1.totalWins)
                .limit(5).collect(Collectors.toList());
    }
}
