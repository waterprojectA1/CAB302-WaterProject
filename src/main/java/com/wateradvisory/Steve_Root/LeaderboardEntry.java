package com.wateradvisory.Steve_Root;

public class LeaderboardEntry {

    private String userId;
    private String username;
    private int points;


    public LeaderboardEntry(String userId, String username, int points) {
        this.userId = userId;
        this.username = username;
        this.points = points;

        if (points < 0) {
            throw new IllegalArgumentException("Negative points are invalid");
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getPoints() {
        return points;
    }
}
