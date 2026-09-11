package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.PersonalizedTipGenerator;
import com.wateradvisory.water.ActivityEntry;
import com.wateradvisory.water.DailyWaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NonPositiveDurationSafetyTest {

    @Test
    void negativeDurationEntryDoesNotCorruptWeightedShowerAverage() {
        // A single valid shower of 15 minutes should drive the weighted average to
        // 15 minutes (well above the 8-minute benchmark, and clearly above the
        // 12-minute high-impact threshold). A malformed entry with a NEGATIVE
        // duration is mixed in alongside it. If the negative entry were allowed to
        // contribute to the weighted sum like a normal entry, it would drag the
        // average down (potentially even negative), which is a nonsensical result
        // for a duration. The negative entry should instead be treated as no
        // contribution at all, leaving the average exactly at the valid entry's 15
        // minutes.
        ActivityEntry validShower = new ActivityEntry("Shower", 15, 1, 135.0);
        ActivityEntry malformedShower = new ActivityEntry("Shower", -100, 1, 0.0);

        DailyWaterRecord record = new DailyWaterRecord(
                LocalDate.of(2026, 1, 1), 135.0, null,
                List.of(validShower, malformedShower));

        PersonalizedTipGenerator generator = new PersonalizedTipGenerator(0.00357);

        List<PersonalizedTipGenerator.TipCandidate> tips =
                generator.generate(List.of(record), null, null);

        PersonalizedTipGenerator.TipCandidate showerTip = tips.stream()
                .filter(t -> t.sentence().toLowerCase().contains("shower"))
                .findFirst()
                .orElse(null);

        assertTrue(showerTip != null, "Expected a shower duration tip to be generated");
        assertFalse(showerTip.litresSavedPerWeek() < 0,
                "Savings should never be negative, got: " + showerTip.litresSavedPerWeek());
        assertTrue(showerTip.sentence().contains("15 minutes"),
                "Negative-duration entry should be excluded, leaving the average at the valid "
                        + "entry's 15 minutes. Got: " + showerTip.sentence());
    }
}
