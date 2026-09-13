package com.wateradvisory.Jainya_Test;

import com.wateradvisory.water.WaterUsageStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CapitalizeTest {

    @Test
    void capitalize_upperCasesOnlyFirstLetter() {
        String result = WaterUsageStats.capitalize("SUMMER");
        assertEquals("Summer", result);
    }
}