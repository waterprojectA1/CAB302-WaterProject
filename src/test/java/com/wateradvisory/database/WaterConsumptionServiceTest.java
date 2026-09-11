package com.wateradvisory.database;

import com.wateradvisory.database.WaterConsumptionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WaterConsumptionServiceTest {

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
}