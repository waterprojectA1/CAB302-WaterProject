package com.wateradvisory.water;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * One row of the Supabase {@code daily_water_records} table — i.e. one user's
 * total water use for one day, plus the breakdown of activities logged that day.
 *
 * <p>Schema (verified against the live database):</p>
 * <ul>
 *   <li>{@code record_date} → {@link #getRecordDate()} — one row per user per day.</li>
 *   <li>{@code total_water_consumption_day} → {@link #getTotalWaterConsumptionDay()}
 *       — already precomputed (sum of the day's activities' litres). Use it
 *       directly; do not re-sum {@code activities} unless a per-activity
 *       breakdown is specifically needed.</li>
 *   <li>{@code household_id} → {@link #getHouseholdId()} — <b>NULLABLE</b>.</li>
 *   <li>{@code activities} (jsonb array) → {@link #getActivities()}.</li>
 * </ul>
 */
public class DailyWaterRecord {

    private final LocalDate recordDate;
    private final double totalWaterConsumptionDay;
    private final UUID householdId;                 // nullable
    private final List<ActivityEntry> activities;   // never null

    public DailyWaterRecord(LocalDate recordDate,
                            double totalWaterConsumptionDay,
                            UUID householdId,
                            List<ActivityEntry> activities) {
        this.recordDate = recordDate;
        this.totalWaterConsumptionDay = totalWaterConsumptionDay;
        this.householdId = householdId;
        this.activities = (activities == null) ? List.of() : List.copyOf(activities);
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public double getTotalWaterConsumptionDay() {
        return totalWaterConsumptionDay;
    }

    /** @return the household this record belongs to, or {@code null} if the user has no household. */
    public UUID getHouseholdId() {
        return householdId;
    }

    /** @return the day's activity breakdown; never {@code null}, possibly empty. */
    public List<ActivityEntry> getActivities() {
        return Collections.unmodifiableList(activities);
    }
}
