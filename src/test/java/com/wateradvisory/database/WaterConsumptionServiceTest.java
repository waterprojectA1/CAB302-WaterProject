package com.wateradvisory.database;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WaterConsumptionServiceTest {

    // Test 1 - Negative Duration should return 0
    @Test
    void negativeDurationShouldReturnZero() {

        double result =
                WaterConsumptionService.calculateActivity(
                        "Shower",
                        -5,
                        2
                );

        assertEquals(
                0.0,
                result,
                0.001
        );
    }

    // Test 2 - Null Activity should return 0
    @Test
    void nullActivityShouldReturnZero() {

        double result =
                WaterConsumptionService.calculateActivity(
                        null,
                        5,
                        2
                );

        assertEquals(
                0.0,
                result,
                0.001
        );
    }
}