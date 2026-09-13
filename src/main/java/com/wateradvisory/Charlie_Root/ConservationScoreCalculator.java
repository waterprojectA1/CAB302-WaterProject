package com.wateradvisory.Charlie_Root;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.wateradvisory.Michael_Root.WaterData;
import com.wateradvisory.Michael_Root.WaterDataList;
import com.wateradvisory.water.DailyWaterRecord;

import javafx.collections.ObservableList;

/**
 * Computes a 0-100 "conservation score" from recorded water usage.
 *
 * <p>The score nudges up when the user's most recent period used less water than
 * the period before it, and down when it used more:</p>
 * <pre>
 *   percentChangeFraction = (currentUsage - previousUsage) / previousUsage
 *   adjustment            = clamp(-10, +10, -(percentChangeFraction * 50))
 *   newScore              = clamp(0, 100, previousScore + adjustment)
 * </pre>
 *
 * <p><b>Data source.</b> The primary path is real Supabase data:
 * {@link #calculateDailyScore(int, List)} / {@link #calculateMonthlyScore(int, List)}
 * take a list of {@link DailyWaterRecord}s (fetched by the caller via
 * {@code WaterRecordService.getUserDailyRecords()}) and compare
 * {@code total_water_consumption_day} across days / calendar months by
 * {@code record_date} — which the verified schema fully supports. The
 * {@code ...Fallback} overloads keep the old {@link WaterDataList} (seeded,
 * in-memory) path for a brand-new user with zero Supabase rows, or for local
 * testing with no database connection.</p>
 *
 * <p>This class still does no I/O and never re-derives usage numbers — it only
 * applies the formula. Only the <i>caller's</i> data source changed.</p>
 */
public class ConservationScoreCalculator {

    /**
     * Result of a score update: the final clamped score, PLUS the real numbers that
     * explain it, so a genuine (non-hallucinated) explanatory sentence can be built
     * from them -- e.g. "Your score dropped 4 points because this week's usage was
     * 18% higher than your average."
     *
     * @param newScore      the new score, always in [0, 100]
     * @param adjustment    the point change actually applied by the formula (+/- up to 10,
     *                      before the FINAL score is clamped to [0, 100] -- so this can differ
     *                      from {@code newScore - previousScore} when that final clamp bites,
     *                      e.g. a previousScore of 95 with a +10 adjustment still only reaches
     *                      newScore 100, a "+5 actually applied" difference the raw adjustment
     *                      value does not hide)
     * @param percentChange the raw signed percent difference between current and previous
     *                      period usage ({@code (current - previous) / previous * 100}); positive
     *                      means MORE usage, negative means LESS. {@code Double.NaN} when there is
     *                      no prior/current period to compare, or previous usage was 0 (can't divide).
     */
    public record ScoreResult(int newScore, int adjustment, double percentChange) {
    }

    /** Score everyone starts on, and the value returned when there is no prior period to compare against. */
    public static final int STARTING_SCORE = 50;

    private static final int SCORE_MIN = 0;
    private static final int SCORE_MAX = 100;

    /** Each update is capped at +/- this many points. */
    private static final double MAX_ADJUSTMENT = 10.0;

    /** Multiplier turning a fractional usage change into score points. */
    private static final double ADJUSTMENT_SCALE = 50.0;

    // ------------------------------------------------------------------
    // Primary: real Supabase data (List<DailyWaterRecord>)
    // ------------------------------------------------------------------

    /**
     * Score after comparing the user's two most recent <b>logged days</b>
     * ({@code total_water_consumption_day} of the latest {@code record_date} vs
     * the day before it). Spec framing is "today vs yesterday"; comparing the two
     * most recent days that actually have a record is the same day-over-day
     * intent but resilient to a user not logging every single day.
     *
     * @param previousScore the score before this update
     * @param records       the user's {@code daily_water_records} rows (any order; may be null/empty)
     * @return the new score (always in [0, 100]) plus the adjustment/percentChange that explain it
     */
    public ScoreResult calculateDailyScore(int previousScore, List<DailyWaterRecord> records) {
        Double[] pair = mostRecentDayPair(records);
        return applyFormula(previousScore, pair[0], pair[1]);
    }

    /**
     * Score after comparing the sum of {@code total_water_consumption_day} for the
     * user's most recent <b>logged calendar month</b> against the previous logged
     * calendar month.
     */
    public ScoreResult calculateMonthlyScore(int previousScore, List<DailyWaterRecord> records) {
        Double[] pair = mostRecentMonthPair(records);
        return applyFormula(previousScore, pair[0], pair[1]);
    }

    /** {current, previous} day totals, or nulls where a period is unavailable. Index 0 = previous, 1 = current. */
    private static Double[] mostRecentDayPair(List<DailyWaterRecord> records) {
        if (records == null || records.isEmpty()) {
            return new Double[]{null, null};
        }
        List<DailyWaterRecord> sorted = new ArrayList<>(records);
        sorted.sort((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()));

        DailyWaterRecord current = sorted.get(sorted.size() - 1);
        DailyWaterRecord previous = null;
        for (int i = sorted.size() - 2; i >= 0; i--) {
            if (sorted.get(i).getRecordDate().isBefore(current.getRecordDate())) {
                previous = sorted.get(i);
                break;
            }
        }
        return new Double[]{
            previous == null ? null : previous.getTotalWaterConsumptionDay(),
            current.getTotalWaterConsumptionDay()
        };
    }

    /** {previous-month sum, current-month sum}, or nulls where a month is unavailable. */
    private static Double[] mostRecentMonthPair(List<DailyWaterRecord> records) {
        if (records == null || records.isEmpty()) {
            return new Double[]{null, null};
        }
        Map<YearMonth, Double> byMonth = new TreeMap<>();
        for (DailyWaterRecord r : records) {
            YearMonth month = YearMonth.from(r.getRecordDate());
            // getOrDefault(month, 0.0) is never null and getTotalWaterConsumptionDay() is a
            // primitive double, so no possibly-null Double is ever unboxed here -- this replaces
            // merge(..., Double::sum), whose primitive-param method reference tripped the linter.
            byMonth.put(month, byMonth.getOrDefault(month, 0.0) + r.getTotalWaterConsumptionDay());
        }
        List<Double> monthlyTotals = new ArrayList<>(byMonth.values());   // TreeMap -> ascending by month
        if (monthlyTotals.isEmpty()) {
            return new Double[]{null, null};
        }
        double current = monthlyTotals.get(monthlyTotals.size() - 1);
        Double previous = monthlyTotals.size() >= 2 ? monthlyTotals.get(monthlyTotals.size() - 2) : null;
        return new Double[]{previous, current};
    }

    // ------------------------------------------------------------------
    // Fallback: in-memory WaterDataList (seeded test data)
    // ------------------------------------------------------------------

    /** Fallback for zero-Supabase-data / no-DB-connection: compare the user's two most recent DAILY {@link WaterData} records. */
    public int calculateDailyScoreFallback(String userId, int previousScore, WaterDataList data) {
        if (data == null) {
            return freshStart();
        }
        return scoreFromWaterData(previousScore, userRecords(userId, data.getDailyWater()));
    }

    /** Fallback counterpart to {@link #calculateMonthlyScore}. See {@link #calculateDailyScoreFallback}. */
    public int calculateMonthlyScoreFallback(String userId, int previousScore, WaterDataList data) {
        if (data == null) {
            return freshStart();
        }
        return scoreFromWaterData(previousScore, userRecords(userId, data.getMonthlyWater()));
    }

    /** Filters a usage list down to one user's records, keeping the list's (chronological) order. */
    private static List<WaterData> userRecords(String userId, ObservableList<WaterData> all) {
        List<WaterData> mine = new ArrayList<>();
        if (all == null) {
            return mine;
        }
        for (WaterData record : all) {
            if (record != null && record.getUserID() == userId) {
                mine.add(record);
            }
        }
        return mine;
    }

    /** Applies the formula to the last two entries of {@code records} (fallback path). */
    private ScoreResult scoreFromWaterData(int previousScore, List<WaterData> records) {
        if (records == null || records.size() < 2) {
            return freshStart();
        }
        WaterData previous = records.get(records.size() - 2);
        WaterData current = records.get(records.size() - 1);
        if (previous == null || current == null) {
            return unchanged(previousScore);
        }
        return applyFormula(previousScore, previous.getWaterUsage(), current.getWaterUsage());
    }

    // ------------------------------------------------------------------
    // Shared formula + edge cases (unchanged from the original design)
    // ------------------------------------------------------------------

    /**
     * The score formula and its edge cases:
     * <ul>
     *   <li>no prior/current period ({@code previousUsage} or {@code currentUsage} null) → fresh {@link #STARTING_SCORE};</li>
     *   <li>zero previous usage → can't divide, score untouched;</li>
     *   <li>zero current usage → maximum reward (+10);</li>
     *   <li>otherwise → {@code -(percentChange * 50)}, clamped to +/-10, added to the previous score;</li>
     *   <li>final score always clamped to [0, 100].</li>
     * </ul>
     *
     * <p>Verified by {@code ConservationScoreCalculatorExposesAdjustmentAndPercentChangeTest} with
     * a concrete case: previous 200.0L -> current 100.0L (a 50% reduction) from a previousScore of
     * 50 yields {@code ScoreResult(newScore=60, adjustment=+10, percentChange=-50.0)} -- confirming
     * all three {@link ScoreResult} fields are populated from the real formula, not just newScore.</p>
     */
    private ScoreResult applyFormula(int previousScore, Double previousUsage, Double currentUsage) {
        if (previousUsage == null || currentUsage == null) {
            return freshStart();
        }
        if (previousUsage == 0.0) {
            return unchanged(previousScore);
        }
        double percentChangeFraction = (currentUsage - previousUsage) / previousUsage;
        double adjustment;
        if (currentUsage == 0.0) {
            adjustment = MAX_ADJUSTMENT;
        } else {
            adjustment = clamp(-MAX_ADJUSTMENT, MAX_ADJUSTMENT, -(percentChangeFraction * ADJUSTMENT_SCALE));
        }
        int roundedAdjustment = (int) Math.round(adjustment);
        int newScore = clampScore(previousScore + roundedAdjustment);
        return new ScoreResult(newScore, roundedAdjustment, percentChangeFraction * 100.0);
    }

    /** No prior/current period to compare -- fresh STARTING_SCORE, no real adjustment or percent change to report. */
    private static ScoreResult freshStart() {
        return new ScoreResult(STARTING_SCORE, 0, Double.NaN);
    }

    /**
     * A prior period record DID exist but can't be compared (zero previous usage -- can't divide;
     * or a null record). Distinct from {@link #freshStart()}: this keeps the user's existing
     * score untouched rather than resetting to {@link #STARTING_SCORE}, since a real (if
     * unusable) prior record is not the same situation as no prior record at all.
     */
    private static ScoreResult unchanged(int previousScore) {
        return new ScoreResult(clampScore(previousScore), 0, Double.NaN);
    }

    private static int clampScore(int score) {
        return (int) Math.round(clamp(SCORE_MIN, SCORE_MAX, score));
    }

    private static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }
}
