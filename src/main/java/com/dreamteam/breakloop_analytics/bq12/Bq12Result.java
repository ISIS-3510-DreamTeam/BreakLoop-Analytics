package com.dreamteam.breakloop_analytics.bq12;

import java.util.List;
import java.util.Map;

/** Response of GET /api/analytics/bq12. Never contains uids or coordinates. */
public record Bq12Result(
        String from,
        String to,
        int minUsers,
        int interactionsConsidered,
        int ignoredPending,
        List<Group> groups,
        int suppressedGroups,
        List<Bucket> byCategory,
        List<Bucket> byTimeOfDay,
        List<Bucket> byPlaceLabel,
        Map<String, Group> topGroupPerCategory,
        Map<String, Outcomes> outcomesByCategory,
        Map<String, Map<String, Integer>> declineReasonsByCategory) {

    public record Group(String category, String timeOfDay, String placeLabel,
                        int shown, int accepted, double acceptanceRate, int users) {
    }

    public record Bucket(String key, int shown, int accepted, double acceptanceRate, int users) {
    }

    /** {@code inferred} is the subset of {@code noActivity} that came from expired PENDING interactions. */
    public record Outcomes(int accepted, int otherActivity, int noActivity, int inferred) {
    }
}
