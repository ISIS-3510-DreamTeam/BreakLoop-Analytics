package com.dreamteam.breakloop_analytics.bq17;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
public class Bq17Aggregator {

    private final Firestore firestore;

    public Bq17Aggregator(Firestore firestore) {
        this.firestore = firestore;
    }

    public Bq17Result aggregateData() throws ExecutionException, InterruptedException {
        // Usa collectionGroup para traer focusSessions de todos los usuarios
        ApiFuture<QuerySnapshot> future = firestore.collectionGroup("focusSessions").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        Map<String, long[]> durationCounts = new HashMap<>(); // [total, completed] por categoría
        Map<String, Integer> timeBlockCounts = new HashMap<>(); // conteo por bloque horario

        for (QueryDocumentSnapshot doc : documents) {
            String status = doc.getString("status");
            // Leemos 'duration' que es la clave en Firestore (en segundos)
            Long durationSeconds = doc.getLong("duration");
            Object timestampObj = doc.get("startTime");

            if (status == null || durationSeconds == null) continue;

            boolean isCompleted = "COMPLETED".equalsIgnoreCase(status);

            // 1. Categorizar por duración
            String durationCategory = getDurationCategory(durationSeconds);
            durationCounts.putIfAbsent(durationCategory, new long[2]);
            durationCounts.get(durationCategory)[0]++; // Total de sesiones registradas
            if (isCompleted) {
                durationCounts.get(durationCategory)[1]++; // Solo completadas
            }

            // 2. Si la sesión fue completada con éxito, clasificarla por bloque de tiempo del día
            if (isCompleted && timestampObj != null) {
                String timeBlock = getTimeBlockFromTimestamp(timestampObj);
                timeBlockCounts.put(timeBlock, timeBlockCounts.getOrDefault(timeBlock, 0) + 1);
            }
        }

        Map<String, Bq17Result.DurationStats> successRateMap = new HashMap<>();
        for (Map.Entry<String, long[]> entry : durationCounts.entrySet()) {
            long total = entry.getValue()[0];
            long completed = entry.getValue()[1];
            double rate = total > 0 ? ((double) completed / total) * 100.0 : 0.0;
            successRateMap.put(entry.getKey(), new Bq17Result.DurationStats(total, completed, rate));
        }

        return new Bq17Result(successRateMap, timeBlockCounts);
    }

    private String getDurationCategory(long seconds) {
        double minutes = seconds / 60.0;
        if (minutes < 15) return "SHORT (<15m)";
        if (minutes <= 30) return "MEDIUM (15-30m)";
        return "LONG (>30m)";
    }

    private String getTimeBlockFromTimestamp(Object timestampObj) {
        try {
            Instant instant;
            if (timestampObj instanceof com.google.cloud.Timestamp) {
                instant = ((com.google.cloud.Timestamp) timestampObj).toDate().toInstant();
            } else if (timestampObj instanceof java.util.Date) {
                instant = ((java.util.Date) timestampObj).toInstant();
            } else {
                return "UNKNOWN";
            }

            ZonedDateTime zdt = instant.atZone(ZoneId.of("America/Bogota"));
            int hour = zdt.getHour();

            if (hour >= 6 && hour < 12) return "MORNING";
            if (hour >= 12 && hour < 18) return "AFTERNOON";
            if (hour >= 18 && hour < 22) return "EVENING";
            return "NIGHT";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}