package com.dreamteam.breakloop_analytics.bq8;

public record Bq8Result(
        Bq8Group withPet,
        Bq8Group withoutPet,
        Double differenceMinutes, // withPet - withoutPet average
        int minUsers
) {}