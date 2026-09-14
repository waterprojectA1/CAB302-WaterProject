package com.wateradvisory.Jainya_Test;

import com.wateradvisory.water.WaterUsageStats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateRangeValidationTest {

    @Test
    void validateDateRange_rejectsStartAfterEnd() {
        LocalDate start = LocalDate.of(2026, 6, 10);
        LocalDate end = LocalDate.of(2026, 6, 1);

        String error = WaterUsageStats.validateDateRange(start, end);

        assertEquals("Start date must be before end date.", error);
    }
}