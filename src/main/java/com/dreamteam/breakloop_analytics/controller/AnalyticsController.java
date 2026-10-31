
package com.dreamteam.breakloop_analytics.controller;

import com.dreamteam.breakloop_analytics.model.FocusAnalytics;
import com.dreamteam.breakloop_analytics.model.FocusSprint3Analytics;
import com.dreamteam.breakloop_analytics.service.AnalyticsService;
import com.google.cloud.firestore.Firestore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final Firestore firestore;
    private final AnalyticsService analyticsService;

    public AnalyticsController(
            Firestore firestore,
            AnalyticsService analyticsService) {

        this.firestore = firestore;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/test")
    public String testFirestore() {

        if (firestore == null) {
            return "Firestore not connected";
        }

        return "Analytics Service connected to Firestore";
    }

    // BQ4
    @GetMapping("/focus")
    public FocusAnalytics getFocusAnalytics() throws Exception {
        return analyticsService.getFocusAnalytics();
    }

    // BQ17 - Sprint 3
    @GetMapping("/focus/sprint3")
    public FocusSprint3Analytics getFocusSprint3Analytics()
            throws Exception {

        return analyticsService.getFocusSprint3Analytics();
    }
}
