package com.dreamteam.breakloop_analytics.bq17;

import java.util.Map;

public class Bq17Result {
    private Map<String, DurationStats> successRateByCategory; // Ej: "SHORT", "MEDIUM", "LONG"
    private Map<String, Integer> successfulSessionsByTimeBlock; // Ej: "MORNING", "AFTERNOON", etc.

    public Bq17Result(Map<String, DurationStats> successRateByCategory, Map<String, Integer> successfulSessionsByTimeBlock) {
        this.successRateByCategory = successRateByCategory;
        this.successfulSessionsByTimeBlock = successfulSessionsByTimeBlock;
    }

    public Map<String, DurationStats> getSuccessRateByCategory() {
        return successRateByCategory;
    }

    public Map<String, Integer> getSuccessfulSessionsByTimeBlock() {
        return successfulSessionsByTimeBlock;
    }

    public static class DurationStats {
        private long total;
        private long completed;
        private double successRate;

        public DurationStats(long total, long completed, double successRate) {
            this.total = total;
            this.completed = completed;
            this.successRate = successRate;
        }

        public long getTotal() { return total; }
        public long getCompleted() { return completed; }
        public double getSuccessRate() { return successRate; }
    }
}