package com.dreamteam.breakloop_analytics.controller;

import com.dreamteam.breakloop_analytics.bq12.Bq12Result;
import com.dreamteam.breakloop_analytics.bq12.Bq12Service;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
public class Bq12Controller {

    private final Bq12Service bq12Service;

    public Bq12Controller(Bq12Service bq12Service) {
        this.bq12Service = bq12Service;
    }

    @GetMapping("/bq12")
    public Bq12Result getBq12(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int minUsers) throws Exception {

        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(30);
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
        }
        if (minUsers < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'minUsers' must be at least 1");
        }
        return bq12Service.compute(start, end, minUsers);
    }
}
