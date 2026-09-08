package com.wateradvisory.Charlie_Root;

import java.util.ArrayList;
import java.util.List;

import com.wateradvisory.Michael_Root.WaterData;
import com.wateradvisory.Michael_Root.WaterDataList;

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
 * <p>Usage data comes straight from {@link WaterDataList}
 * ({@link WaterDataList#getDailyWater()} / {@link WaterDataList#getMonthlyWater()});
 * this class never re-queries or re-derives usage numbers. The percent-change
 * expression is the same signed-relative-change shape as
 * {@link WaterDataList#getUserDiff(WaterData)} -- only measured against the previous
 * period instead of the running mean, and kept as a fraction rather than a rounded
 * percentage.</p>
 */
public class ConservationScoreCalculator {

    /** Score everyone starts on, and the value returned when there is no prior period to compare against. */
    public static final int STARTING_SCORE = 50;

    private static final int SCORE_MIN = 0;
    private static final int SCORE_MAX = 100;

    /** Each update is capped at +/- this many points. */
    private static final double MAX_ADJUSTMENT = 10.0;

    /** Multiplier turning a fractional usage change into score points. */
    private static final double ADJUSTMENT_SCALE = 50.0;

    /**
     * Score after comparing the user's two most recent DAILY records.
     *
     * @param userId        which user's records to look at
     * @param previousScore the score before this update
     * @param data          the in-memory usage model (may be null)
     * @return the new score, always in [0, 100]
     */
    public int calculateDailyScore(int userId, int previousScore, WaterDataList data) {
        if (data == null) {
            return STARTING_SCORE;
        }
        return scoreFrom(previousScore, userRecords(userId, data.getDailyWater()));
    }

    /** Score after comparing the user's two most recent MONTHLY records. See {@link #calculateDailyScore}. */
    public int calculateMonthlyScore(int userId, int previousScore, WaterDataList data) {
        if (data == null) {
            return STARTING_SCORE;
        }
        return scoreFrom(previousScore, userRecords(userId, data.getMonthlyWater()));
    }

    /** Filters a usage list down to one user's records, keeping the list's (chronological) order. */
    private static List<WaterData> userRecords(int userId, ObservableList<WaterData> all) {
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

    /** Applies the score formula to the last two entries of {@code records}. */
    private int scoreFrom(int previousScore, List<WaterData> records) {
        // No prior period (first-ever record, or no records): start fresh, no adjustment, no crash.
        if (records == null || records.size() < 2) {
            return STARTING_SCORE;
        }

        WaterData previous = records.get(records.size() - 2);
        WaterData current = records.get(records.size() - 1);

        // Missing data for either period: leave the score untouched.
        if (previous == null || current == null) {
            return clampScore(previousScore);
        }

        double previousUsage = previous.getWaterUsage();
        double currentUsage = current.getWaterUsage();

        // Can't divide by a zero previous usage: skip the adjustment.
        if (previousUsage == 0.0) {
            return clampScore(previousScore);
        }

        double adjustment;
        if (currentUsage == 0.0) {
            // Used no water this period: maximum reward (still clamped to <= 100 below).
            adjustment = MAX_ADJUSTMENT;
        } else {
            double percentChangeFraction = (currentUsage - previousUsage) / previousUsage;
            adjustment = clamp(-MAX_ADJUSTMENT, MAX_ADJUSTMENT, -(percentChangeFraction * ADJUSTMENT_SCALE));
        }

        return clampScore((int) Math.round(previousScore + adjustment));
    }

    private static int clampScore(int score) {
        return (int) Math.round(clamp(SCORE_MIN, SCORE_MAX, score));
    }

    private static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }
}
