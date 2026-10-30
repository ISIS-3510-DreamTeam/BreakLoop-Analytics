package com.dreamteam.breakloop_analytics.bq12;

import java.util.List;

/**
 * Projection of users/{uid}/suggestionInteractions/{id}. Location fields are deliberately absent.
 * {@code userId} is only used to count distinct users and is never serialized in any response.
 */
public record Interaction(
        String userId,
        long openedAt,
        String timeOfDay,
        String placeLabel,
        List<Shown> shownSuggestions,
        String outcome,
        String acceptedActivityId,
        String declineReason) {

    public record Shown(String activityId, String category) {
    }
}
