package com.wateradvisory.Jainya_Test;

import com.wateradvisory.water.WaterUsageStats;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BiggestSeasonalChangeTest {

    @Test
    void findBiggestSeasonalChange_picksLargestAbsoluteDelta_evenWhenNegative() {
        Map<WaterUsageStats.Season, Integer> before = Map.of(
                WaterUsageStats.Season.SUMMER, 500,
                WaterUsageStats.Season.WINTER, 200
        );
        Map<WaterUsageStats.Season, Integer> after = Map.of(
                WaterUsageStats.Season.SUMMER, 480,   // delta -20
                WaterUsageStats.Season.WINTER, 350    // delta +150
        );

        WaterUsageStats.Season biggest = WaterUsageStats.findBiggestSeasonalChange(before, after);
        assertEquals(WaterUsageStats.Season.WINTER, biggest);
    }
}