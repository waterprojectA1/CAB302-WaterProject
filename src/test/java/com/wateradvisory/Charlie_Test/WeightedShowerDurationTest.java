package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.PersonalizedTipGenerator;
import com.wateradvisory.water.ActivityEntry;
import com.wateradvisory.water.DailyWaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WeightedShowerDurationTest {

    @Test
    void showerDurationTipUsesWeightedAverageNotNaiveMean() {
        // Entry A: 1 shower of 30 minutes. Entry B: 9 showers of 10 minutes each.
        // Weighted average = (30*1 + 10*9) / (1+9) = 120/10 = 12 minutes.
        // Naive (unweighted) mean of the two entries' durations = (30+10)/2 = 20 minutes.
        // These are far enough apart that the rounded integer in the tip sentence
        // unambiguously reveals which formula was actually used.
        ActivityEntry oneLongShower = new ActivityEntry("Shower", 30, 1, 270.0);
        ActivityEntry nineShortShowers = new ActivityEntry("Shower", 10, 9, 810.0);

        DailyWaterRecord record = new DailyWaterRecord(
                LocalDate.of(2026, 1, 1), 1080.0, null,
                List.of(oneLongShower, nineShortShowers));

        PersonalizedTipGenerator generator = new PersonalizedTipGenerator(0.00357);

        List<PersonalizedTipGenerator.TipCandidate> tips =
                generator.generate(List.of(record), null, null);

        PersonalizedTipGenerator.TipCandidate showerTip = tips.stream()
                .filter(t -> t.sentence().toLowerCase().contains("shower"))
                .findFirst()
                .orElse(null);

        assertTrue(showerTip != null, "Expected a shower duration tip to be generated");
        assertTrue(showerTip.sentence().contains("12 minutes"),
                "Expected the weighted average (12 minutes) in the tip sentence, got: " + showerTip.sentence());
    }
}
