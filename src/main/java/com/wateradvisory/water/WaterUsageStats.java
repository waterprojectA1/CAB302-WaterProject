package com.wateradvisory.water;

import java.util.List;

/**
 * Pure aggregation helpers shared by the Daily/Seasonal/Compare
 * screens: average/highest/lowest and season boundaries. No
 * database access lives here - see WaterRecordService for that.
 */
public class WaterUsageStats {

    public enum Season { SUMMER, AUTUMN, WINTER, SPRING }

    /** Season boundaries defined once, by calendar month (Southern Hemisphere). */
    public static Season seasonForMonth(int month) {
        switch (month) {
            case 12: case 1: case 2: return Season.SUMMER;
            case 3: case 4: case 5: return Season.AUTUMN;
            case 6: case 7: case 8: return Season.WINTER;
            default: return Season.SPRING; // 9,10,11
        }
    }

    public static class Stats {
        public final Integer average;          // null if no data
        public final WaterUsageEntry highest;   // null if no data
        public final WaterUsageEntry lowest;    // null if no data
        public Stats(Integer average, WaterUsageEntry highest, WaterUsageEntry lowest) {
            this.average = average;
            this.highest = highest;
            this.lowest = lowest;
        }
    }

    /** Average/highest/lowest computed only from days with a recorded entry. */
    public static Stats computeStats(List<WaterUsageEntry> entries) {
        if (entries.isEmpty()) return new Stats(null, null, null);
        int sum = 0;
        WaterUsageEntry high = entries.get(0);
        WaterUsageEntry low = entries.get(0);
        for (WaterUsageEntry e : entries) {
            sum += e.getLitres();
            if (e.getLitres() > high.getLitres()) high = e;
            if (e.getLitres() < low.getLitres()) low = e;
        }
        int avg = Math.round((float) sum / entries.size());
        return new Stats(avg, high, low);
    }
}