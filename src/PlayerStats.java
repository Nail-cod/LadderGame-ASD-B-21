package src;

import java.io.Serializable;

public class PlayerStats implements Serializable {
    private static final long serialVersionUID = 1L;
    public String name;
    public int totalWins;
    public int totalScore;

    public PlayerStats(String name) {
        this.name = name;
        this.totalWins = 0;
        this.totalScore = 0;
    }
}
