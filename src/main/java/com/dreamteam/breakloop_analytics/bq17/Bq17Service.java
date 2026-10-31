package com.dreamteam.breakloop_analytics.bq17;

import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutionException;

@Service
public class Bq17Service {

    private final Bq17Aggregator aggregator;

    public Bq17Service(Bq17Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    public Bq17Result getAnalysis() {
        try {
            return aggregator.aggregateData();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error calculating BQ17 analytics from Firestore", e);
        }
    }
}