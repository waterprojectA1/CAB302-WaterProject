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

    // Test 3: Activity name with spaces should still calculate correctly
    @Test
    void activityWithSpacesShouldCalculateCorrectly() {

        double result =
                WaterConsumptionService.calculateActivity(
                        " Shower ",
                        5,
                        2
                );

        assertEquals(
                90.0,
                result,
                0.001
        );
    }

    // Test 4: Activity name should not be case-sensitive
    @Test
    void lowercaseActivityShouldCalculateCorrectly() {

        double result =
                WaterConsumptionService.calculateActivity(
                        "shower",
                        5,
                        2
                );

        assertEquals(
                90.0,
                result,
                0.001
        );
    }

    // Test 5: Extra spaces inside activity name should still calculate correctly
    @Test
    void activityWithExtraInternalSpacesShouldCalculateCorrectly() {

        double result =
                WaterConsumptionService.calculateActivity(
                        "Floor   Cleaning",
                        5,
                        1
                );

        assertEquals(
                15.0,
                result,
                0.001
        );
    }

    // Test 6: Activity amount above maximum should return zero
    @Test
    void carWashAmountAboveMaximumShouldReturnZero() {

        double result =
                WaterConsumptionService.calculateActivity(
                        "Car Wash",
                        5,
                        4
                );

        assertEquals(
                0.0,
                result,
                0.001
        );
    }

    // Test 7: Shower duration above maximum should return zero
    @Test
    void showerDurationAboveMaximumShouldReturnZero() {

        double result =
                WaterConsumptionService.calculateActivity(
                        "Shower",
                        25,
                        1
                );

        assertEquals(
                0.0,
                result,
                0.001
        );
    }

    // Test 8: Unsupported shower duration should return zero
    @Test
    void unsupportedShowerDurationShouldReturnZero() {

        double result =WaterConsumptionService.calculateActivity
                ("Shower",3,1);

        assertEquals(0.0,result,0.001);
    }
}