package src;

import java.util.Stack;

public class Player {
    public String name;
    public PawnAsset asset;
    public int position;
    public int currentScore;
    public Stack<Integer> stepHistory;

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
