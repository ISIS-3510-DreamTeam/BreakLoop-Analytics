package com.dreamteam.breakloop_analytics.controller;
import com.dreamteam.breakloop_analytics.bq16.Bq16Result;
import com.dreamteam.breakloop_analytics.bq16.Bq16Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class Bq16Controller {

    private final Bq16Service bq16Service;

    public Bq16Controller(Bq16Service bq16Service) {
        this.bq16Service = bq16Service;
    }

    @GetMapping("/bq16")
    public ResponseEntity<Bq16Result> getBq16Analysis() {
        Bq16Result result = bq16Service.getAnalysis();
        return ResponseEntity.ok(result);
    }
}