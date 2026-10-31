package com.dreamteam.breakloop_analytics.controller;
import com.dreamteam.breakloop_analytics.bq17.Bq17Result;
import com.dreamteam.breakloop_analytics.bq17.Bq17Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/analytics/bq17")
public class Bq17Controller {

    private final Bq17Service service;

    public Bq17Controller(Bq17Service service) {
        this.service = service;
    }

    @GetMapping
    public Bq17Result getAnalytics() {
        return service.getAnalysis();
    }
}