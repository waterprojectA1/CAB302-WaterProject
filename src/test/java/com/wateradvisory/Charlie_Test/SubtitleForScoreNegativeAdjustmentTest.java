package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ConservationScoreCalculator.ScoreResult;
import com.wateradvisory.Charlie_Root.ConservationTipsController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubtitleForScoreNegativeAdjustmentTest {

    @Test
    void negativeAdjustmentProducesDownPointsHigherUsageSentence() {
        // A directly-constructed ScoreResult exercising subtitleForScore's sentence-building in
        // isolation -- NOT a real applyFormula() output (18% usage change would actually produce
        // a -9 point adjustment, not -4; this test only checks the wording, not the formula math,
        // which is covered separately by ConservationScoreCalculatorExposesAdjustmentAndPercentChangeTest).
        ScoreResult result = new ScoreResult(46, -4, 18.0);

        String subtitle = ConservationTipsController.subtitleForScore(result);

        assertEquals("Down 4 points -- your usage was 18% higher than your average.", subtitle);
    }
}
