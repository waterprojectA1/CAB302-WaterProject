package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ConservationScoreCalculator.ScoreResult;
import com.wateradvisory.Charlie_Root.ConservationTipsController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubtitleForScorePositiveAdjustmentTest {

    @Test
    void positiveAdjustmentProducesUpPointsLowerUsageSentence() {
        // +10 points, usage 50% lower than average -- matches the ConservationScoreCalculator
        // worked example (previous 200.0L, current 100.0L, previousScore 50 -> newScore 60).
        ScoreResult result = new ScoreResult(60, 10, -50.0);

        String subtitle = ConservationTipsController.subtitleForScore(result);

        assertEquals("Up 10 points -- your usage was 50% lower than your average.", subtitle);
    }
}
