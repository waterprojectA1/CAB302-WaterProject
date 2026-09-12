package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ConservationScoreCalculator;
import com.wateradvisory.Charlie_Root.ConservationScoreCalculator.ScoreResult;
import com.wateradvisory.water.DailyWaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConservationScoreCalculatorExposesAdjustmentAndPercentChangeTest {

    @Test
    void calculateDailyScoreReturnsResultWithScoreAdjustmentAndPercentChange() {
        ConservationScoreCalculator calculator = new ConservationScoreCalculator();

        // previous day 200.0 L, current day 100.0 L -- a 50% reduction.
        // percentChangeFraction = (100-200)/200 = -0.5 -> percentChange = -50.0
        // adjustment = clamp(-10,10, -(-0.5*50)) = clamp(-10,10,25) = 10
        // newScore = clamp(0,100, 50+10) = 60
        DailyWaterRecord previousDay = new DailyWaterRecord(
                LocalDate.of(2026, 1, 1), 200.0, null, List.of());
        DailyWaterRecord currentDay = new DailyWaterRecord(
                LocalDate.of(2026, 1, 2), 100.0, null, List.of());

        ScoreResult result = calculator.calculateDailyScore(50, List.of(previousDay, currentDay));

        assertEquals(60, result.newScore(), "newScore should reflect the +10 clamped adjustment");
        assertEquals(10, result.adjustment(), "adjustment should be the +10 points actually applied");
        assertEquals(-50.0, result.percentChange(), 0.001,
                "percentChange should be the raw -50% usage difference that drove the adjustment");
    }
}
