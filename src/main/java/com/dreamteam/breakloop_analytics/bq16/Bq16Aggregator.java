package com.dreamteam.breakloop_analytics.bq16;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
public class Bq16Aggregator {

    public Bq16Result aggregateData() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        
        // Consultamos todos los documentos de la colección 'phone_batches' de los usuarios
        ApiFuture<QuerySnapshot> future = db.collectionGroup("phone_batches").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        long totalPickups = 0;
        long totalIntentional = 0;
        Map<Integer, Long> impulsiveByHour = new HashMap<>();

            for (QueryDocumentSnapshot document : documents) {
            // Cada lote contiene una lista 'pickupsList'
            Object rawPickups = document.get("pickupsList");
            if (!(rawPickups instanceof List)) continue;
            
            List<?> pickupsList = (List<?>) rawPickups;
            for (Object item : pickupsList) {
                if (!(item instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> pickup = (Map<String, Object>) item;

                totalPickups++;
                Boolean isIntentional = (Boolean) pickup.get("isIntentional");
                Long hourOfDayLong = (Long) pickup.get("hourOfDay");
                int hourOfDay = hourOfDayLong != null ? hourOfDayLong.intValue() : 0;

                if (isIntentional != null && isIntentional) {
                    totalIntentional++;
                } else {
                    // Es un desbloqueo impulsivo (non-intentional)
                    impulsiveByHour.put(hourOfDay, impulsiveByHour.getOrDefault(hourOfDay, 0L) + 1);
                }
            }
        }

        double mindfulPercentage = totalPickups > 0 ? ((double) totalIntentional / totalPickups) * 100 : 0.0;
        double impulsivePercentage = 100.0 - mindfulPercentage;

        // Encontrar la hora del día con más picos impulsivos
        int peakHour = impulsiveByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        return new Bq16Result(mindfulPercentage, impulsivePercentage, impulsiveByHour, peakHour);
    }
}