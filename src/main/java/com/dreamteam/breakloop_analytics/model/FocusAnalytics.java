package com.dreamteam.breakloop_analytics.model;

public class FocusAnalytics {

    private int totalSessions;
    private int completedSessions;
    private int abandonedSessions;
    private double completionPercentage;
    private double averageDurationSeconds;

    public FocusAnalytics(
            int totalSessions,
            int completedSessions,
            int abandonedSessions,
            double completionPercentage,
            double averageDurationSeconds) {

        this.totalSessions = totalSessions;
        this.completedSessions = completedSessions;
        this.abandonedSessions = abandonedSessions;
        this.completionPercentage = completionPercentage;
        this.averageDurationSeconds = averageDurationSeconds;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public int getCompletedSessions() {
        return completedSessions;
    }

    public int getAbandonedSessions() {
        return abandonedSessions;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public double getAverageDurationSeconds() {
        return averageDurationSeconds;
    }
}