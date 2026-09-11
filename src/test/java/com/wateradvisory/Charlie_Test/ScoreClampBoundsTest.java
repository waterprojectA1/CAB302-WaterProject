package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ConservationScoreCalculator;
import com.wateradvisory.water.DailyWaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScoreClampBoundsTest {

    @Test
    void scoreNeverExceeds100EvenWhenAdjustmentWouldPushItHigher() {
        ConservationScoreCalculator calculator = new ConservationScoreCalculator();

        DailyWaterRecord previous = new DailyWaterRecord(
                LocalDate.of(2026, 1, 1), 100.0, null, List.of());
        DailyWaterRecord current = new DailyWaterRecord(
                LocalDate.of(2026, 1, 2), 0.0, null, List.of());

        int previousScore = 95;

        int result = calculator.calculateDailyScore(previousScore, List.of(previous, current));

        assertTrue(result <= 100, "Score should be clamped to a maximum of 100 but was " + result);
    }

    @Test
    void scoreNeverGoesBelow0EvenWhenAdjustmentWouldPushItLower() {
        ConservationScoreCalculator calculator = new ConservationScoreCalculator();

        DailyWaterRecord previous = new DailyWaterRecord(
                LocalDate.of(2026, 1, 1), 100.0, null, List.of());
        DailyWaterRecord current = new DailyWaterRecord(
                LocalDate.of(2026, 1, 2), 1000.0, null, List.of());

        int previousScore = 5;

        int result = calculator.calculateDailyScore(previousScore, List.of(previous, current));

        assertTrue(result >= 0, "Score should be clamped to a minimum of 0 but was " + result);
    }
}
