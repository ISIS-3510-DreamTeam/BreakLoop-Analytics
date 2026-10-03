package com.dreamteam.breakloop_analytics.controller;

import com.dreamteam.breakloop_analytics.model.FocusAnalytics;
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

    @GetMapping("/focus")
    public FocusAnalytics getFocusAnalytics() throws Exception {
        return analyticsService.getFocusAnalytics();
    }
}