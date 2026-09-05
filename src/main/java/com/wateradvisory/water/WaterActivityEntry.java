package com.wateradvisory.water;

public class WaterActivityEntry {

    private String activity;
    private int duration;
    private int amount;
    private double litres;

    public WaterActivityEntry(
            String activity,
            int duration,
            int amount,
            double litres
    ) {
        this.activity = activity;
        this.duration = duration;
        this.amount = amount;
        this.litres = litres;
    }

    public String getActivity() {
        return activity;
    }

    public int getDuration() {
        return duration;
    }

    public int getAmount() {
        return amount;
    }

    public double getLitres() {
        return litres;
    }
}
