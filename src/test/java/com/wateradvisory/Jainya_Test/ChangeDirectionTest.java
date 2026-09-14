package com.wateradvisory.Jainya_Test;

import com.wateradvisory.water.WaterUsageStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChangeDirectionTest {

    @Test
    void determineChangeDirection_returnsDown_whenUsageDecreases() {
        String direction = WaterUsageStats.determineChangeDirection(-25);
        assertEquals("down", direction);
    }
}