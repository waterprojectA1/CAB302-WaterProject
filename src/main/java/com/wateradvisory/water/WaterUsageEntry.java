package com.wateradvisory.water;

import java.time.LocalDate;

/** One day's recorded water usage. */
public class WaterUsageEntry {

    private final LocalDate date;
    private final int litres;

    public WaterUsageEntry(LocalDate date, int litres) {
        this.date = date;
        this.litres = litres;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getLitres() {
        return litres;
    }
}