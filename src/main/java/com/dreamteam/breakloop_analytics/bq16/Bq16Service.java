package com.dreamteam.breakloop_analytics.bq16;

import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutionException;

@Service
public class Bq16Service {

    private final Bq16Aggregator aggregator;

    public Bq16Service(Bq16Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    public Bq16Result getAnalysis() {
        try {
            return aggregator.aggregateData();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error calculating BQ16 analytics from Firestore", e);
        }
    }
}