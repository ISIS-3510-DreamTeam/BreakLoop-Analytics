
package com.dreamteam.breakloop_analytics.model;

import java.util.List;

public class FocusSprint3Analytics {

    private List<DurationStats> durationStats;
    private List<TimeBlockStats> timeBlockStats;

    public FocusSprint3Analytics(
            List<DurationStats> durationStats,
            List<TimeBlockStats> timeBlockStats) {

        this.durationStats = durationStats;
        this.timeBlockStats = timeBlockStats;
    }

    public List<DurationStats> getDurationStats() {
        return durationStats;
    }

    public List<TimeBlockStats> getTimeBlockStats() {
        return timeBlockStats;
    }

    public static class DurationStats {

        private String durationCategory;
        private int totalSessions;
        private int completedSessions;
        private double completionRate;

        public DurationStats(
                String durationCategory,
                int totalSessions,
                int completedSessions,
                double completionRate) {

            this.durationCategory = durationCategory;
            this.totalSessions = totalSessions;
            this.completedSessions = completedSessions;
            this.completionRate = completionRate;
        }

        public String getDurationCategory() {
            return durationCategory;
        }

        public int getTotalSessions() {
            return totalSessions;
        }

        public int getCompletedSessions() {
            return completedSessions;
        }

        public double getCompletionRate() {
            return completionRate;
        }
    }

    public static class TimeBlockStats {

        private String timeBlock;
        private int completedSessions;

        public TimeBlockStats(
                String timeBlock,
                int completedSessions) {

            this.timeBlock = timeBlock;
            this.completedSessions = completedSessions;
        }

        public String getTimeBlock() {
            return timeBlock;
        }

        public int getCompletedSessions() {
            return completedSessions;
        }
    }
}
