package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ConservationScoreCalculator;
import com.wateradvisory.water.DailyWaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoPriorPeriodStartingScoreTest {

    @Test
    void firstEverRecordReturnsExactlyStartingScore() {
        ConservationScoreCalculator calculator = new ConservationScoreCalculator();

        DailyWaterRecord onlyRecord = new DailyWaterRecord(
                LocalDate.of(2026, 1, 1), 180.0, null, List.of());

        int result = calculator.calculateDailyScore(17, List.of(onlyRecord));

        assertEquals(ConservationScoreCalculator.STARTING_SCORE, result,
                "With no prior period to compare against, the score should be exactly the starting score of 50");
    }
}
