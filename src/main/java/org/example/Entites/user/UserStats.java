package org.example.Entites.user;

public class UserStats {
    private int userId;
    private int totalPoints;
    private int currentLevel;
    private int experiencePoints;
    private int nextLevelPoints;
    private int totalReservations;
    private int totalReviews;
    private double totalSpent;
    private String rank;

    // Constructeurs
    public UserStats() {}

    public UserStats(int userId, int totalPoints, int currentLevel, int experiencePoints,
                     int totalReservations, int totalReviews, double totalSpent) {
        this.userId = userId;
        this.totalPoints = totalPoints;
        this.currentLevel = currentLevel;
        this.experiencePoints = experiencePoints;
        this.totalReservations = totalReservations;
        this.totalReviews = totalReviews;
        this.totalSpent = totalSpent;
        this.rank = calculateRank(totalPoints);
        this.nextLevelPoints = calculateNextLevelPoints(currentLevel);
    }

    // Getters et Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
        this.rank = calculateRank(totalPoints);
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
        this.nextLevelPoints = calculateNextLevelPoints(currentLevel);
    }

    public int getExperiencePoints() {
        return experiencePoints;
    }

    public void setExperiencePoints(int experiencePoints) {
        this.experiencePoints = experiencePoints;
    }

    public int getNextLevelPoints() {
        return nextLevelPoints;
    }

    public void setNextLevelPoints(int nextLevelPoints) {
        this.nextLevelPoints = nextLevelPoints;
    }

    public int getTotalReservations() {
        return totalReservations;
    }

    public void setTotalReservations(int totalReservations) {
        this.totalReservations = totalReservations;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    // Méthodes utilitaires
    private String calculateRank(int points) {
        if (points < 100) return "Novice";
        if (points < 500) return "Bronze";
        if (points < 1000) return "Argent";
        if (points < 5000) return "Or";
        if (points < 10000) return "Platine";
        return "Diamant";
    }

    private int calculateNextLevelPoints(int level) {
        return level * 100;
    }

    public int getPointsToNextLevel() {
        return nextLevelPoints - experiencePoints;
    }

    public double getProgressPercentage() {
        return (double) experiencePoints / nextLevelPoints * 100;
    }

    @Override
    public String toString() {
        return "UserStats{" +
                "userId=" + userId +
                ", totalPoints=" + totalPoints +
                ", currentLevel=" + currentLevel +
                ", experiencePoints=" + experiencePoints +
                ", nextLevelPoints=" + nextLevelPoints +
                ", totalReservations=" + totalReservations +
                ", totalReviews=" + totalReviews +
                ", totalSpent=" + totalSpent +
                ", rank='" + rank + '\'' +
                '}';
    }
}