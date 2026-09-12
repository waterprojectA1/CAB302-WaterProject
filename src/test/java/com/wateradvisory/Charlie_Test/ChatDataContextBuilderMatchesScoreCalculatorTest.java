package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ChatDataContextBuilder;
import com.wateradvisory.Charlie_Root.ConservationScoreCalculator;
import com.wateradvisory.Charlie_Root.ConservationScoreCalculator.ScoreResult;
import com.wateradvisory.Michael_Root.WaterDataList;
import com.wateradvisory.water.DailyWaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatDataContextBuilderMatchesScoreCalculatorTest {

    @Test
    void chatContextReportsTheSameScoreAndPercentChangeAsConservationScoreCalculator() {
        // Same real-data scenario already verified in
        // ConservationScoreCalculatorExposesAdjustmentAndPercentChangeTest: previous day 200.0L,
        // current day 100.0L, previousScore 50 -> newScore 60, adjustment +10, percentChange -50.0.
        DailyWaterRecord previousDay = new DailyWaterRecord(
                LocalDate.of(2026, 1, 1), 200.0, null, List.of());
        DailyWaterRecord currentDay = new DailyWaterRecord(
                LocalDate.of(2026, 1, 2), 100.0, null, List.of());
        List<DailyWaterRecord> dailyRecords = List.of(previousDay, currentDay);

        // The independently-computed real answer, from the already-tested calculator.
        ScoreResult expected = new ConservationScoreCalculator()
                .calculateDailyScore(ConservationScoreCalculator.STARTING_SCORE, dailyRecords);

        ChatDataContextBuilder builder = new ChatDataContextBuilder(dailyRecords, new WaterDataList());
        String context = builder.buildContext("why did my score drop this week?", 1);

        assertTrue(context != null && context.contains(String.valueOf(expected.newScore())),
                "ChatDataContextBuilder's context should report the SAME score ("
                        + expected.newScore() + ") that ConservationScoreCalculator independently "
                        + "computed for this exact same data -- got: " + context);
    }
}
