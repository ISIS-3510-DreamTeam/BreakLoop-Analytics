package com.dreamteam.breakloop_analytics.bq12;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class Bq12Service {

    // Day boundaries for from/to are interpreted in the app's home timezone.
    static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private final SuggestionInteractionReader reader;

    public Bq12Service(SuggestionInteractionReader reader) {
        this.reader = reader;
    }

    public Bq12Result compute(LocalDate from, LocalDate to, int minUsers) throws Exception {
        return Bq12Aggregator.aggregate(reader.readAll(), from, to, ZONE, Instant.now(), minUsers);
    }
}
