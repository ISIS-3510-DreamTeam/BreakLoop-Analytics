
package com.dreamteam.breakloop_analytics.service;

import com.dreamteam.breakloop_analytics.model.FocusAnalytics;
import com.dreamteam.breakloop_analytics.model.FocusSprint3Analytics;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final Firestore firestore;

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    public AnalyticsService(Firestore firestore) {
        this.firestore = firestore;
    }

    // BQ4 - General Focus Analytics
    public FocusAnalytics getFocusAnalytics() throws Exception {

        ApiFuture<QuerySnapshot> future = firestore.collectionGroup("focusSessions").get();

        List<QueryDocumentSnapshot> sessions = future.get().getDocuments();

        int totalSessions = sessions.size();
        int completedSessions = 0;
        int abandonedSessions = 0;

        double totalDuration = 0;

        for (QueryDocumentSnapshot session : sessions) {

            String status = session.getString("status");
            Object durationValue = session.get("duration");

            Long duration = durationValue instanceof Number
                    ? ((Number) durationValue).longValue()
                    : null;

            if ("COMPLETED".equals(status)) {
                completedSessions++;
            }

            if ("ABANDONED".equals(status)) {
                abandonedSessions++;
            }

            if (duration != null) {
                totalDuration += duration;
            }
        }

        double completionPercentage = totalSessions == 0
                ? 0
                : (completedSessions * 100.0) / totalSessions;

        double averageDuration = totalSessions == 0
                ? 0
                : totalDuration / totalSessions;

        return new FocusAnalytics(
                totalSessions,
                completedSessions,
                abandonedSessions,
                completionPercentage,
                averageDuration);
    }

    // BQ17 - Focus Duration and Time Analysis
    public FocusSprint3Analytics getFocusSprint3Analytics()
            throws Exception {

        ApiFuture<QuerySnapshot> future = firestore.collectionGroup("focusSessions").get();

        List<QueryDocumentSnapshot> sessions = future.get().getDocuments();

        Map<String, int[]> durationGroups = new LinkedHashMap<>();

        durationGroups.put("Short", new int[2]);
        durationGroups.put("Medium", new int[2]);
        durationGroups.put("Long", new int[2]);
        durationGroups.put("Extended", new int[2]);

        Map<String, Integer> timeBlocks = new LinkedHashMap<>();

        timeBlocks.put("Morning", 0);
        timeBlocks.put("Afternoon", 0);
        timeBlocks.put("Evening", 0);
        timeBlocks.put("Night", 0);

        for (QueryDocumentSnapshot session : sessions) {

            String type = session.getString("type");
            String status = session.getString("status");

            Object durationValue = session.get("duration");

            Long duration = durationValue instanceof Number
                    ? ((Number) durationValue).longValue()
                    : null;

            Long startTime = getTimestampMillis(
                    session.get("startTime"));

            // Only analyze Focus sessions
            if (!"FOCUS".equals(type)) {
                continue;
            }

            // Group sessions by duration
            if (duration != null && duration > 0) {

                String category = getDurationCategory(duration);

                int[] values = durationGroups.get(category);

                values[0]++;

                if ("COMPLETED".equals(status)) {
                    values[1]++;
                }
            }

            // Group completed sessions by start time
            if ("COMPLETED".equals(status) && startTime != null) {

                Instant instant = Instant.ofEpochMilli(startTime);

                ZonedDateTime dateTime = instant.atZone(ZONA_BOGOTA);

                int hour = dateTime.getHour();

                String timeBlock = getTimeBlock(hour);

                timeBlocks.put(
                        timeBlock,
                        timeBlocks.get(timeBlock) + 1);
            }
        }

        // Build duration statistics
        List<FocusSprint3Analytics.DurationStats> durationStats = new ArrayList<>();

        for (Map.Entry<String, int[]> entry : durationGroups.entrySet()) {

            int total = entry.getValue()[0];
            int completed = entry.getValue()[1];

            double completionRate = total == 0
                    ? 0
                    : (completed * 100.0) / total;

            durationStats.add(
                    new FocusSprint3Analytics.DurationStats(
                            entry.getKey(),
                            total,
                            completed,
                            completionRate));
        }

        // Build time block statistics
        List<FocusSprint3Analytics.TimeBlockStats> timeBlockStats = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : timeBlocks.entrySet()) {

            timeBlockStats.add(
                    new FocusSprint3Analytics.TimeBlockStats(
                            entry.getKey(),
                            entry.getValue()));
        }

        return new FocusSprint3Analytics(
                durationStats,
                timeBlockStats);
    }

    // Convert Firestore timestamp or numeric timestamp to milliseconds
    private Long getTimestampMillis(Object value) {

        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }

        if (value instanceof Date) {
            return ((Date) value).getTime();
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return null;
    }

    // Duration categories in minutes
    private String getDurationCategory(long duration) {

        if (duration <= 15) {
            return "Short";
        }

        if (duration <= 30) {
            return "Medium";
        }

        if (duration <= 45) {
            return "Long";
        }

        return "Extended";
    }

    // Time blocks in Bogota local time
    private String getTimeBlock(int hour) {

        if (hour >= 6 && hour < 12) {
            return "Morning";
        }

        if (hour >= 12 && hour < 18) {
            return "Afternoon";
        }

        if (hour >= 18) {
            return "Evening";
        }

        return "Night";
    }
}
