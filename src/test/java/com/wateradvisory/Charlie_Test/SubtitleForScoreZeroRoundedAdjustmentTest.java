package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.ConservationScoreCalculator.ScoreResult;
import com.wateradvisory.Charlie_Root.ConservationTipsController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubtitleForScoreZeroRoundedAdjustmentTest {

    @Test
    void realButTinyPercentChangeIsNotSwallowedWhenAdjustmentRoundsToZero() {
        // A real, finite percentChange (+0.9%) that rounds to a zero-point adjustment (see
        // ConservationScoreCalculator.applyFormula: adjustment = round(-(0.009 * 50)) = round(-0.45) = 0).
        // Previously this fell all the way through to the generic static score-band text, silently
        // discarding a real computed number. It should instead report the real percentChange directly.
        ScoreResult result = new ScoreResult(50, 0, 0.9);

        String subtitle = ConservationTipsController.subtitleForScore(result);

        assertEquals("Roughly unchanged -- your usage was 1% higher than your average.", subtitle);
    }
}
