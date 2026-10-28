package com.dreamteam.breakloop_analytics.bq12;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class FirestoreSuggestionInteractionReader implements SuggestionInteractionReader {

    // latitude / longitude are intentionally never projected.
    private static final String[] FIELDS = {
            "openedAt", "timeOfDay", "placeLabel", "shownSuggestions",
            "outcome", "acceptedActivityId", "declineReason"
    };

    private final Firestore firestore;

    public FirestoreSuggestionInteractionReader(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public List<Interaction> readAll() throws Exception {
        List<QueryDocumentSnapshot> docs = firestore.collectionGroup("suggestionInteractions")
                .select(FIELDS)
                .get().get().getDocuments();

        List<Interaction> result = new ArrayList<>(docs.size());
        for (QueryDocumentSnapshot doc : docs) {
            Long openedAt = doc.getLong("openedAt");
            if (openedAt == null) {
                continue;
            }
            result.add(new Interaction(
                    userId(doc),
                    openedAt,
                    doc.getString("timeOfDay"),
                    doc.getString("placeLabel"),
                    shown(doc.get("shownSuggestions")),
                    doc.getString("outcome"),
                    doc.getString("acceptedActivityId"),
                    doc.getString("declineReason")));
        }
        return result;
    }

    /** uid from users/{uid}/suggestionInteractions/{id}; only used to count distinct users. */
    private static String userId(QueryDocumentSnapshot doc) {
        DocumentReference parent = doc.getReference().getParent().getParent();
        return parent == null ? doc.getId() : parent.getId();
    }

    private static List<Interaction.Shown> shown(Object raw) {
        List<Interaction.Shown> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    out.add(new Interaction.Shown(str(m.get("activityId")), str(m.get("category"))));
                }
            }
        }
        return out;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
