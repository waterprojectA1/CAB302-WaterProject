package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.PersonalizedTipGenerator;
import com.wateradvisory.Charlie_Root.PersonalizedTipGenerator.TipCandidate;
import com.wateradvisory.water.ActivityEntry;
import com.wateradvisory.water.DailyWaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShowerDurationWeightedAverageTest {

    @Test
    void showerDurationTipUsesWeightedAverageNotNaiveMean() {
        // Entry A: 1 shower of 5 minutes. Entry B: 9 showers of 20 minutes each.
        // Naive (unweighted) mean of the two entries' durations = (5 + 20) / 2 = 12.5 -> rounds to 13.
        // Correct WEIGHTED average = (5*1 + 20*9) / (1+9) = 185/10 = 18.5 -> rounds to 19.
        ActivityEntry entryA = new ActivityEntry("Shower", 5, 1, 45.0);
        ActivityEntry entryB = new ActivityEntry("Shower", 20, 9, 1620.0);

        DailyWaterRecord record = new DailyWaterRecord(
                LocalDate.of(2026, 1, 1), 1665.0, null, List.of(entryA, entryB));

        PersonalizedTipGenerator generator = new PersonalizedTipGenerator(0.005);

        List<TipCandidate> tips = generator.generate(List.of(record), null, null);

        assertTrue(tips.stream().anyMatch(t -> t.sentence().contains("19 minutes")),
                "Expected the weighted average (18.5 -> rounds to 19 minutes) in a tip sentence");
        assertFalse(tips.stream().anyMatch(t -> t.sentence().contains("13 minutes")),
                "No tip should report the naive unweighted mean (12.5 -> rounds to 13 minutes)");
    }
}
