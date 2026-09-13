package com.wateradvisory.Jainya_Test;

import com.wateradvisory.water.WaterUsageStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WaterUsageStatsTest {

    @Test
    void computePercentChange_returnsNegativeTwentyFivePercent_whenUsageDropsFrom400To300() {
        int result = WaterUsageStats.computePercentChange(400, 300);
        assertEquals(-25, result);
    }
}