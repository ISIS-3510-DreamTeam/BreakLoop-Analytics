package com.dreamteam.breakloop_analytics.service;

import com.dreamteam.breakloop_analytics.model.FocusAnalytics;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {

    private final Firestore firestore;

    public AnalyticsService(Firestore firestore) {
        this.firestore = firestore;
    }

    public FocusAnalytics getFocusAnalytics() throws Exception {

        ApiFuture<QuerySnapshot> future =
                firestore.collectionGroup("focusSessions").get();

        List<DocumentSnapshot> sessions =
                future.get().getDocuments();

        int totalSessions = sessions.size();
        int completedSessions = 0;
        int abandonedSessions = 0;

        double totalDuration = 0;

        for (DocumentSnapshot session : sessions) {

            String status = session.getString("status");
            Long duration = session.getLong("duration");

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

        double completionPercentage =
                totalSessions == 0
                        ? 0
                        : (completedSessions * 100.0) / totalSessions;

        double averageDuration =
                totalSessions == 0
                        ? 0
                        : totalDuration / totalSessions;

        return new FocusAnalytics(
                totalSessions,
                completedSessions,
                abandonedSessions,
                completionPercentage,
                averageDuration
        );
    }
}