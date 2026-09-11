package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ConservationScoreCalculator;
import com.wateradvisory.water.DailyWaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZeroPreviousUsageTest {

    @Test
    void previousUsageOfZeroReturnsPreviousScoreUnchanged() {
        ConservationScoreCalculator calculator = new ConservationScoreCalculator();

        DailyWaterRecord previous = new DailyWaterRecord(
                LocalDate.of(2026, 1, 1), 0.0, null, List.of());
        DailyWaterRecord current = new DailyWaterRecord(
                LocalDate.of(2026, 1, 2), 250.0, null, List.of());

        int previousScore = 62;

        int result = calculator.calculateDailyScore(previousScore, List.of(previous, current));

        assertEquals(previousScore, result,
                "Score should stay exactly at the previous score when previous usage is 0 (avoids division by zero)");
    }
}
