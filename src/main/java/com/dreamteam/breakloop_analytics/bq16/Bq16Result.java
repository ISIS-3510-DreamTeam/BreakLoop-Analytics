package com.dreamteam.breakloop_analytics.bq16;

import java.util.Map;

public class Bq16Result {
    private double mindfulPercentage;
    private double impulsivePercentage;
    private Map<Integer, Long> impulsivePickupsByHour;
    private int peakImpulsiveHour;

    public Bq16Result(double mindfulPercentage, double impulsivePercentage, 
                      Map<Integer, Long> impulsivePickupsByHour, int peakImpulsiveHour) {
        this.mindfulPercentage = mindfulPercentage;
        this.impulsivePercentage = impulsivePercentage;
        this.impulsivePickupsByHour = impulsivePickupsByHour;
        this.peakImpulsiveHour = peakImpulsiveHour;
    }

    public double getMindfulPercentage() {
        return mindfulPercentage;
    }

    public double getImpulsivePercentage() {
        return impulsivePercentage;
    }

    public Map<Integer, Long> getImpulsivePickupsByHour() {
        return impulsivePickupsByHour;
    }

    public int getPeakImpulsiveHour() {
        return peakImpulsiveHour;
    }
}