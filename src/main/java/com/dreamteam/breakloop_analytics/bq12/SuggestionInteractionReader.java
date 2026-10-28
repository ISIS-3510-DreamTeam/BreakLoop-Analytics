package com.dreamteam.breakloop_analytics.bq12;

import java.util.List;

/** Source of suggestion interactions; lets the calculation be tested without Firestore. */
public interface SuggestionInteractionReader {

    List<Interaction> readAll() throws Exception;
}
