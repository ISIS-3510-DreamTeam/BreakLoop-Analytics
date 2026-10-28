package com.dreamteam.breakloop_analytics.bq12;

import com.dreamteam.breakloop_analytics.bq12.Bq12Result.Group;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Bq12AggregatorTest {

    private static final ZoneId Z = ZoneId.of("UTC");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final long OPENED = Instant.parse("2026-10-20T10:00:00Z").toEpochMilli();

    private static Interaction.Shown s(String id, String cat) {
        return new Interaction.Shown(id, cat);
    }

    private static Interaction i(String user, String outcome, String accepted, String reason, Interaction.Shown... shown) {
        return new Interaction(user, OPENED, "MORNING", "HOME", List.of(shown), outcome, accepted, reason);
    }

    private static Bq12Result run(List<Interaction> in, int minUsers) {
        return Bq12Aggregator.aggregate(in, DAY, DAY, Z, NOW, minUsers);
    }

    private static Group group(Bq12Result r, String cat) {
        return r.groups().stream().filter(g -> g.category().equals(cat)).findFirst().orElseThrow();
    }

    @Test
    void acceptanceMatchesOnlyTheAcceptedActivity() {
        Bq12Result r = run(List.of(i("u1", "ACCEPTED", "a1", null, s("a1", "READING"), s("a2", "SPORT"))), 1);
        assertEquals(1, group(r, "READING").accepted());
        assertEquals(0, group(r, "SPORT").accepted());
        assertEquals(1, group(r, "SPORT").shown());
    }

    @Test
    void acceptedIdMatchingNothingCountsNoAcceptance() {
        Bq12Result r = run(List.of(i("u1", "ACCEPTED", "zzz", null, s("a1", "READING"))), 1);
        assertEquals(0, group(r, "READING").accepted());
    }

    @Test
    void severalSuggestionsInOneVisitCountedOncePerItemWithOneAcceptanceMax() {
        Bq12Result r = run(List.of(
                i("u1", "ACCEPTED", "a1", null, s("a1", "READING"), s("a2", "READING"), s("a3", "SPORT"))), 1);
        Group reading = group(r, "READING");
        assertEquals(2, reading.shown());
        assertEquals(1, reading.accepted());
        assertEquals(0.5, reading.acceptanceRate(), 1e-9);
        assertEquals(1, r.byTimeOfDay().get(0).accepted());
    }

    @Test
    void recentPendingIsIgnored() {
        long recent = NOW.minusSeconds(3600).toEpochMilli();
        Interaction pending = new Interaction("u1", recent, "MORNING", "HOME",
                List.of(s("a1", "READING")), "PENDING", null, null);
        Bq12Result r = Bq12Aggregator.aggregate(List.of(pending), NOW.atZone(Z).toLocalDate(),
                NOW.atZone(Z).toLocalDate(), Z, NOW, 1);
        assertEquals(0, r.interactionsConsidered());
        assertEquals(1, r.ignoredPending());
        assertTrue(r.groups().isEmpty());
    }

    @Test
    void expiredPendingBecomesInferredNoActivityWithUnknownReason() {
        Bq12Result r = run(List.of(i("u1", "PENDING", null, null, s("a1", "READING"))), 1);
        assertEquals(1, r.interactionsConsidered());
        assertEquals(1, r.outcomesByCategory().get("READING").noActivity());
        assertEquals(1, r.outcomesByCategory().get("READING").inferred());
        assertEquals(1, r.declineReasonsByCategory().get("READING").get("UNKNOWN"));
    }

    @Test
    void declineReasonsAndOutcomesPerCategory() {
        Bq12Result r = run(List.of(
                i("u1", "NO_ACTIVITY", null, "NOT_NOW", s("a1", "READING")),
                i("u2", "OTHER_ACTIVITY", null, "PREFERRED_OTHER", s("a1", "READING")),
                i("u3", "ACCEPTED", "a1", null, s("a1", "READING"))), 1);
        var o = r.outcomesByCategory().get("READING");
        assertEquals(1, o.accepted());
        assertEquals(1, o.otherActivity());
        assertEquals(1, o.noActivity());
        assertEquals(0, o.inferred());
        assertEquals(1, r.declineReasonsByCategory().get("READING").get("NOT_NOW"));
    }

    @Test
    void groupsBelowMinUsersAreSuppressed() {
        List<Interaction> in = new ArrayList<>();
        for (int u = 0; u < 3; u++) {
            in.add(i("u" + u, "ACCEPTED", "a1", null, s("a1", "READING")));
        }
        in.add(i("u0", "NO_ACTIVITY", null, "NOT_NOW", s("a2", "SPORT")));

        Bq12Result r = run(in, 3);
        assertEquals(1, r.groups().size());
        assertEquals("READING", r.groups().get(0).category());
        assertEquals(1, r.suppressedGroups());
        assertFalse(r.outcomesByCategory().containsKey("SPORT"));
        assertFalse(r.declineReasonsByCategory().containsKey("SPORT"));
        assertEquals(1, r.byCategory().size());
        assertEquals(1, r.topGroupPerCategory().size());
    }

    @Test
    void sameUserRepeatedDoesNotReachThreshold() {
        Bq12Result r = run(List.of(
                i("u1", "ACCEPTED", "a1", null, s("a1", "READING")),
                i("u1", "ACCEPTED", "a1", null, s("a1", "READING"))), 2);
        assertTrue(r.groups().isEmpty());
        assertEquals(1, r.suppressedGroups());
    }

    @Test
    void nullPlaceBecomesUnlabeled() {
        Interaction x = new Interaction("u1", OPENED, "MORNING", null,
                List.of(s("a1", "READING")), "NO_ACTIVITY", null, null);
        Bq12Result r = run(List.of(x), 1);
        assertEquals("UNLABELED", r.groups().get(0).placeLabel());
        assertEquals("UNLABELED", r.byPlaceLabel().get(0).key());
    }

    @Test
    void emptyInput() {
        Bq12Result r = run(List.of(), 5);
        assertEquals(0, r.interactionsConsidered());
        assertTrue(r.groups().isEmpty());
        assertEquals(0, r.suppressedGroups());
        assertTrue(r.topGroupPerCategory().isEmpty());
    }

    @Test
    void interactionsOutsideDateRangeAreFiltered() {
        Interaction old = new Interaction("u1", OPENED - 5L * 86_400_000, "MORNING", "HOME",
                List.of(s("a1", "READING")), "NO_ACTIVITY", null, null);
        assertEquals(0, run(List.of(old), 1).interactionsConsidered());
    }

    @Test
    void topGroupPicksHighestRate() {
        Interaction morning = i("u1", "ACCEPTED", "a1", null, s("a1", "READING"));
        Interaction evening = new Interaction("u2", OPENED, "EVENING", "HOME",
                List.of(s("a1", "READING")), "NO_ACTIVITY", null, null);
        Bq12Result r = run(List.of(morning, evening), 1);
        assertEquals("MORNING", r.topGroupPerCategory().get("READING").timeOfDay());
        assertEquals(1.0, r.topGroupPerCategory().get("READING").acceptanceRate(), 1e-9);
    }
}
