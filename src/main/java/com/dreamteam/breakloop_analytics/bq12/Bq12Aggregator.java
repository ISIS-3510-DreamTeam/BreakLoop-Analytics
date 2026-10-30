package com.dreamteam.breakloop_analytics.bq12;

import com.dreamteam.breakloop_analytics.bq12.Bq12Result.Bucket;
import com.dreamteam.breakloop_analytics.bq12.Bq12Result.Group;
import com.dreamteam.breakloop_analytics.bq12.Bq12Result.Outcomes;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Pure BQ12 computation: no Firestore, no I/O. */
public final class Bq12Aggregator {

    public static final Duration OPEN_THRESHOLD = Duration.ofHours(2);
    static final String UNLABELED = "UNLABELED";
    static final String UNKNOWN = "UNKNOWN";

    private Bq12Aggregator() {
    }

    private static final class Acc {
        int shown;
        int accepted;
        final Set<String> users = new HashSet<>();

        void add(String user, int shown, int accepted) {
            this.shown += shown;
            this.accepted += accepted;
            users.add(user);
        }

        double rate() {
            return shown == 0 ? 0 : (double) accepted / shown;
        }
    }

    private static final class OutcomeAcc {
        int accepted, otherActivity, noActivity, inferred;
    }

    private record GroupKey(String category, String timeOfDay, String placeLabel) {
    }

    public static Bq12Result aggregate(List<Interaction> interactions, LocalDate from, LocalDate to,
                                       ZoneId zone, Instant now, int minUsers) {
        long fromMs = from.atStartOfDay(zone).toInstant().toEpochMilli();
        long toMs = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long openLimit = now.minus(OPEN_THRESHOLD).toEpochMilli();

        Map<GroupKey, Acc> groups = new LinkedHashMap<>();
        Map<String, Acc> categories = new TreeMap<>();
        Map<String, Acc> times = new TreeMap<>();
        Map<String, Acc> places = new TreeMap<>();
        Map<String, OutcomeAcc> outcomes = new TreeMap<>();
        Map<String, Map<String, Integer>> reasons = new TreeMap<>();
        int considered = 0;
        int ignoredPending = 0;

        for (Interaction i : interactions) {
            if (i.openedAt() < fromMs || i.openedAt() >= toMs) {
                continue;
            }
            List<Interaction.Shown> shown = i.shownSuggestions() == null ? List.of()
                    : i.shownSuggestions().stream().filter(s -> s.category() != null).toList();
            if (shown.isEmpty() || i.outcome() == null) {
                continue;
            }

            String outcome = i.outcome();
            String reason = i.declineReason();
            boolean inferred = false;
            if ("PENDING".equals(outcome)) {
                if (i.openedAt() > openLimit) {
                    ignoredPending++;
                    continue;
                }
                outcome = "NO_ACTIVITY";
                reason = UNKNOWN;
                inferred = true;
            }
            considered++;

            String tod = i.timeOfDay() == null ? UNKNOWN : i.timeOfDay();
            String place = i.placeLabel() == null ? UNLABELED : i.placeLabel();
            String user = i.userId();

            // At most one acceptance per interaction: the first shown item matching acceptedActivityId.
            Interaction.Shown acceptedItem = null;
            if ("ACCEPTED".equals(outcome) && i.acceptedActivityId() != null) {
                for (Interaction.Shown s : shown) {
                    if (i.acceptedActivityId().equals(s.activityId())) {
                        acceptedItem = s;
                        break;
                    }
                }
            }

            for (Interaction.Shown s : shown) {
                int acc = s == acceptedItem ? 1 : 0;
                groups.computeIfAbsent(new GroupKey(s.category(), tod, place), k -> new Acc()).add(user, 1, acc);
                categories.computeIfAbsent(s.category(), k -> new Acc()).add(user, 1, acc);
                times.computeIfAbsent(tod, k -> new Acc()).add(user, 1, acc);
                places.computeIfAbsent(place, k -> new Acc()).add(user, 1, acc);
            }

            if ("ACCEPTED".equals(outcome)) {
                if (acceptedItem != null) {
                    outcomes.computeIfAbsent(acceptedItem.category(), k -> new OutcomeAcc()).accepted++;
                }
                continue;
            }
            String declineReason = reason == null ? UNKNOWN : reason;
            for (String category : shown.stream().map(Interaction.Shown::category).distinct().toList()) {
                OutcomeAcc o = outcomes.computeIfAbsent(category, k -> new OutcomeAcc());
                if ("OTHER_ACTIVITY".equals(outcome)) {
                    o.otherActivity++;
                } else {
                    o.noActivity++;
                    if (inferred) {
                        o.inferred++;
                    }
                }
                reasons.computeIfAbsent(category, k -> new TreeMap<>()).merge(declineReason, 1, Integer::sum);
            }
        }

        List<Group> visibleGroups = new ArrayList<>();
        int suppressed = 0;
        for (Map.Entry<GroupKey, Acc> e : groups.entrySet()) {
            Acc a = e.getValue();
            if (a.users.size() < minUsers) {
                suppressed++;
                continue;
            }
            GroupKey k = e.getKey();
            visibleGroups.add(new Group(k.category(), k.timeOfDay(), k.placeLabel(),
                    a.shown, a.accepted, a.rate(), a.users.size()));
        }
        visibleGroups.sort(Comparator.comparing(Group::category)
                .thenComparing(Group::timeOfDay).thenComparing(Group::placeLabel));

        Map<String, Group> top = new TreeMap<>();
        for (Group g : visibleGroups) {
            top.merge(g.category(), g, (a, b) -> better(b, a) ? b : a);
        }

        Set<String> visibleCategories = new HashSet<>();
        for (Map.Entry<String, Acc> e : categories.entrySet()) {
            if (e.getValue().users.size() >= minUsers) {
                visibleCategories.add(e.getKey());
            }
        }
        Map<String, Outcomes> visibleOutcomes = new TreeMap<>();
        outcomes.forEach((c, o) -> {
            if (visibleCategories.contains(c)) {
                visibleOutcomes.put(c, new Outcomes(o.accepted, o.otherActivity, o.noActivity, o.inferred));
            }
        });
        Map<String, Map<String, Integer>> visibleReasons = new TreeMap<>();
        reasons.forEach((c, r) -> {
            if (visibleCategories.contains(c)) {
                visibleReasons.put(c, r);
            }
        });

        return new Bq12Result(from.toString(), to.toString(), minUsers, considered, ignoredPending,
                visibleGroups, suppressed,
                buckets(categories, minUsers), buckets(times, minUsers), buckets(places, minUsers),
                top, visibleOutcomes, visibleReasons);
    }

    private static boolean better(Group candidate, Group current) {
        if (candidate.acceptanceRate() != current.acceptanceRate()) {
            return candidate.acceptanceRate() > current.acceptanceRate();
        }
        return candidate.shown() > current.shown();
    }

    private static List<Bucket> buckets(Map<String, Acc> map, int minUsers) {
        List<Bucket> out = new ArrayList<>();
        map.forEach((k, a) -> {
            if (a.users.size() >= minUsers) {
                out.add(new Bucket(k, a.shown, a.accepted, a.rate(), a.users.size()));
            }
        });
        return out;
    }
}
