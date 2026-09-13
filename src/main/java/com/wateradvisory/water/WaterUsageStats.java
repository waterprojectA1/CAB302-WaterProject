package com.wateradvisory.water;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.time.LocalDate;

/**
 * Pure aggregation helpers shared by the Daily/Seasonal/Compare
 * screens: average/highest/lowest and season boundaries. No
 * database access lives here - see WaterRecordService for that.
 */
public class WaterUsageStats {

    public enum Season { SUMMER, AUTUMN, WINTER, SPRING }

    /** Season boundaries defined once, by calendar month. */
    public static Season seasonForMonth(int month) {
        switch (month) {
            case 12: case 1: case 2: return Season.SUMMER;
            case 3: case 4: case 5: return Season.AUTUMN;
            case 6: case 7: case 8: return Season.WINTER;
            default: return Season.SPRING; // 9,10,11
        }
    }
    public static Map<Season, Integer> aggregateSeasonTotals(List<WaterUsageEntry> entries) {
        Map<Season, Integer> totals = new EnumMap<>(Season.class);
        for (Season s : Season.values()) totals.put(s, 0);
        for (WaterUsageEntry e : entries) {
            Season s = seasonForMonth(e.getDate().getMonthValue());
            totals.put(s, totals.get(s) + e.getLitres());
        }
        return totals;
    }
    public static Season findBiggestSeasonalChange(Map<Season, Integer> before, Map<Season, Integer> after) {
        Season biggest = null;
        int biggestDelta = 0;
        for (Season season : Season.values()) {
            int delta = after.getOrDefault(season, 0) - before.getOrDefault(season, 0);
            if (Math.abs(delta) > Math.abs(biggestDelta)) {
                biggestDelta = delta;
                biggest = season;
            }
        }
        return biggest;
    }
    public static String validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "Please choose both a start and end date.";
        }
        if (start.isAfter(end)) {
            return "Start date must be before end date.";
        }
        LocalDate today = LocalDate.now();
        if (start.isAfter(today) || end.isAfter(today)) {
            return "Dates cannot be in the future.";
        }
        return null; // valid
    }
    public static String capitalize(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
    public static LocalDate[] computeDefaultComparisonPeriods(LocalDate latest) {
        LocalDate p2End = latest;
        LocalDate p2Start = p2End.minusDays(29);
        LocalDate p1End = p2Start.minusDays(1);
        LocalDate p1Start = p1End.minusDays(29);
        return new LocalDate[]{ p1Start, p1End, p2Start, p2End };
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
    public static int computePercentChange(int oldAvg, int newAvg) {
        if (oldAvg == 0) return 0;
        int diff = newAvg - oldAvg;
        return Math.round((diff / (float) oldAvg) * 100);
    }
}