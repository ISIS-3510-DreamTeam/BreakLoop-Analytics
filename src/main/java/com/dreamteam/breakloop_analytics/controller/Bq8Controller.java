package com.dreamteam.breakloop_analytics.controller;

import com.dreamteam.breakloop_analytics.bq8.Bq8Result;
import com.dreamteam.breakloop_analytics.bq8.Bq8Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/analytics")
public class Bq8Controller {

    private final Bq8Service bq8Service;

    public Bq8Controller(Bq8Service bq8Service) {
        this.bq8Service = bq8Service;
    }

    @GetMapping("/bq8")
    public Bq8Result getBq8(@RequestParam(defaultValue = "1") int minUsers) throws Exception {
        if (minUsers < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'minUsers' must be at least 1");
        }
        return bq8Service.compute(minUsers);
    }
}