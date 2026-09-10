package com.wateradvisory.water;

/**
 * One element of a {@code daily_water_records.activities} jsonb array, parsed
 * from Supabase. Field names match the real JSON keys exactly:
 * <pre>
 *   { "amount": 5, "activity": "Shower", "duration": 2, "water_litres": 90 }
 * </pre>
 * i.e. the litres key is <b>{@code water_litres}</b> (mapped to
 * {@link #getWaterLitres()}), not "litres".
 *
 * <p>A single entry can represent MULTIPLE occurrences at once via
 * {@link #getAmount()} — the example above is 5 showers of 2 minutes each. Any
 * average-duration calculation across entries must therefore be a WEIGHTED
 * average, {@code sum(duration * amount) / sum(amount)}.</p>
 *
 * <p>This is the read-side counterpart to {@link WaterActivityEntry} (which the
 * record-water submit flow uses). They are kept separate so fixing this one's
 * field name to {@code waterLitres} does not disturb the submit path.</p>
 */
public class ActivityEntry {

    private final String activity;
    private final int duration;   // minutes
    private final int amount;     // number of occurrences this entry represents
    private final double waterLitres;

    public ActivityEntry(String activity, int duration, int amount, double waterLitres) {
        this.activity = activity;
        this.duration = duration;
        this.amount = amount;
        this.waterLitres = waterLitres;
    }

    public String getActivity() {
        return activity;
    }

    public int getDuration() {
        return duration;
    }

    public int getAmount() {
        return amount;
    }

    public double getWaterLitres() {
        return waterLitres;
    }
}
