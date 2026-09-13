package com.wateradvisory.Jainya_Test;

import com.wateradvisory.water.WaterUsageStats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultComparisonPeriodsTest {

    @Test
    void computeDefaultPeriods_periodOneEndsDayBeforePeriodTwoStarts() {
        LocalDate latest = LocalDate.of(2026, 9, 30);

        LocalDate[] periods = WaterUsageStats.computeDefaultComparisonPeriods(latest);
        // periods = [p1Start, p1End, p2Start, p2End]

        assertEquals(LocalDate.of(2026, 9, 1), periods[2]);   // p2Start
        assertEquals(LocalDate.of(2026, 8, 31), periods[1]);  // p1End
    }
}