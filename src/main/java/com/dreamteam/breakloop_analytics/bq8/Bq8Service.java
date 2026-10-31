package com.dreamteam.breakloop_analytics.bq8;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class Bq8Service {

    private final Firestore firestore;

    public Bq8Service(Firestore firestore) {
        this.firestore = firestore;
    }

    public Bq8Result compute(int minUsers) throws Exception {

        // Which users have an active pet (activePetId in users/{uid})
        Map<String, Boolean> hasPet = new HashMap<>();
        for (QueryDocumentSnapshot user : firestore.collection("users")
                .select("activePetId")
                .get().get().getDocuments()) {
            String petId = user.getString("activePetId");
            hasPet.put(user.getId(), petId != null && !petId.isBlank());
        }

        // Put every focus session in the group of its owner
        GroupCounter withPet = new GroupCounter();
        GroupCounter withoutPet = new GroupCounter();

        for (QueryDocumentSnapshot session : firestore.collectionGroup("focusSessions")
                .select("duration", "status")
                .get().get().getDocuments()) {
            String uid = ownerId(session);
            // Users with no document or no activePetId count as "without pet"
            GroupCounter group = hasPet.getOrDefault(uid, false) ? withPet : withoutPet;
            group.add(uid, session.getString("status"), session.getLong("duration"));
        }

        // Build the result
        Bq8Group petGroup = withPet.toGroup(minUsers);
        Bq8Group noPetGroup = withoutPet.toGroup(minUsers);
        Double difference = (petGroup.hidden() || noPetGroup.hidden())
                ? null
                : petGroup.avgCompletedMinutes() - noPetGroup.avgCompletedMinutes();

        return new Bq8Result(petGroup, noPetGroup, difference, minUsers);
    }

    // uid from the path users/{uid}/focusSessions/{id}; only used to group, never returned. */
    private static String ownerId(QueryDocumentSnapshot doc) {
        DocumentReference user = doc.getReference().getParent().getParent();
        return user == null ? "" : user.getId();
    }

    // Sums sessions of one group.
    private static class GroupCounter {
        private final Set<String> users = new HashSet<>();
        private int sessions = 0;
        private int completed = 0;
        private long completedMinutes = 0;

        void add(String uid, String status, Long duration) {
            users.add(uid);
            sessions++;
            if ("COMPLETED".equals(status)) {
                completed++;
                completedMinutes += (duration != null) ? duration : 0;
            }
        }

        Bq8Group toGroup(int minUsers) {
            if (users.size() < minUsers) {
                // Too few users: hide the numbers to protect privacy
                return new Bq8Group(users.size(), 0, 0, 0, 0, true);
            }
            double completionRate = (sessions == 0) ? 0 : (double) completed / sessions;
            double avgMinutes = (completed == 0) ? 0 : (double) completedMinutes / completed;
            return new Bq8Group(users.size(), sessions, completed, completionRate, avgMinutes, false);
        }
    }
}