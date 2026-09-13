package com.wateradvisory.Jainya_Test;

import com.wateradvisory.water.WaterUsageEntry;
import com.wateradvisory.water.WaterUsageStats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeasonalAggregationTest {

    @Test
    void aggregateBySeason_sumsLitresCorrectlyPerSeason() {
        List<WaterUsageEntry> entries = List.of(
                new WaterUsageEntry(LocalDate.of(2026, 1, 5), 400),  // Summer
                new WaterUsageEntry(LocalDate.of(2026, 1, 15), 300), // Summer
                new WaterUsageEntry(LocalDate.of(2026, 7, 1), 200)   // Winter
        );

        Map<WaterUsageStats.Season, Integer> totals = WaterUsageStats.aggregateSeasonTotals(entries);

        assertEquals(700, totals.get(WaterUsageStats.Season.SUMMER));
        assertEquals(200, totals.get(WaterUsageStats.Season.WINTER));
        assertEquals(0, totals.get(WaterUsageStats.Season.AUTUMN));
    }
}