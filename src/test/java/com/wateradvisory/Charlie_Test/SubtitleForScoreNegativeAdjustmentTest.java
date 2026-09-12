package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ConservationScoreCalculator.ScoreResult;
import com.wateradvisory.Charlie_Root.ConservationTipsController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubtitleForScoreNegativeAdjustmentTest {

    @Test
    void negativeAdjustmentProducesDownPointsHigherUsageSentence() {
        // -4 points, usage 18% higher than average -- the exact worked example from the task
        // description ("Your score dropped 4 points because this week's usage was 18% higher").
        ScoreResult result = new ScoreResult(46, -4, 18.0);

        String subtitle = ConservationTipsController.subtitleForScore(result);

        assertEquals("Down 4 points -- your usage was 18% higher than your average.", subtitle);
    }
}
