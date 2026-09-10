package com.wateradvisory.Charlie_Root;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.wateradvisory.Michael_Root.WaterData;
import com.wateradvisory.Michael_Root.WaterDataList;
import com.wateradvisory.water.WaterActivityEntry;

/**
 * Builds a ranked list of personalised conservation tips from the user's REAL
 * recorded data, replacing the hardcoded placeholder tips that used to live in
 * {@link ConservationTipsController}.
 *
 * <p>Two data sources, both passed in (this class does no I/O of its own, so it
 * is easy to unit-test):</p>
 * <ul>
 *   <li><b>{@link WaterActivityEntry} list</b> (from
 *       {@code WaterRecordService.getUserActivities()}) — per-activity tips:
 *       shower duration, laundry frequency, and category usage share.</li>
 *   <li><b>{@link WaterDataList}</b> — aggregate trend / outlier tips off the
 *       user's weekly records: week-over-week trend, outlier flag, and positive
 *       reinforcement.</li>
 * </ul>
 *
 * <p><b>No time-of-day tips.</b> The Supabase schema has no per-activity
 * timestamp (only a day-level {@code record_date}), so tips like "tap usage
 * spikes 7–8am" are deliberately NOT generated — see the gotcha in CLAUDE.md.
 * Blocked on a future {@code created_at} column.</p>
 *
 * <p><b>No activity date range either.</b> Because the flattened
 * {@link WaterActivityEntry} list carries no dates, the whole returned set is
 * treated as one recent typical week ({@link #ASSUMED_LOG_PERIOD_WEEKS}). When a
 * {@code created_at} column lands, replace that assumption with a real span.</p>
 */
public class PersonalizedTipGenerator {

    /** Relative importance of a tip; {@link Enum#ordinal()} is the rank key (HIGH first). */
    public enum Impact { HIGH, MEDIUM, LOW }

    /**
     * One generated tip. {@code litresSavedPerWeek} / {@code costSavedPerWeek} are 0
     * for tips with nothing to save (e.g. positive reinforcement) — the controller
     * omits the savings line in that case.
     */
    public record TipCandidate(String sentence, Impact impact,
                               double litresSavedPerWeek, double costSavedPerWeek) {
    }

    // --- benchmarks -----------------------------------------------------------
    /** Recommended maximum average shower length (minutes). */
    private static final double SHOWER_BENCHMARK_MINUTES = 8.0;
    /** Standard showerhead flow — matches {@code WaterConsumptionService.SHOWER_LITRES_PER_MINUTE}. */
    private static final double SHOWER_LITRES_PER_MIN = 9.0;
    /** Recommended maximum laundry loads per week before "combine loads" advice kicks in. */
    private static final double LAUNDRY_LOADS_PER_WEEK_BENCHMARK = 5.0;
    /** A single category taking more than this share of total litres earns a "biggest category" tip. */
    private static final double CATEGORY_SHARE_THRESHOLD = 0.30;
    /** Week-over-week % increase over the user's own average that triggers a trend tip. */
    private static final double WEEK_TREND_THRESHOLD_PCT = 15.0;
    private static final double WEEK_TREND_HIGH_PCT = 30.0;
    private static final double SHOWER_HIGH_IMPACT_MINUTES = 12.0;

    /**
     * No per-activity date column exists yet (blocked on {@code created_at}), so the
     * full set of returned activity entries is taken to represent one recent typical
     * week. Swap this for a real measured span once activities are timestamped.
     */
    private static final double ASSUMED_LOG_PERIOD_WEEKS = 1.0;

    private final double waterRatePerLitre;

    /**
     * @param waterRatePerLitre pass {@code ConservationTipsController.WATER_RATE_PER_LITRE}
     *                          so every cost estimate uses the one Brisbane tariff constant.
     */
    public PersonalizedTipGenerator(double waterRatePerLitre) {
        this.waterRatePerLitre = waterRatePerLitre;
    }

    /**
     * @return tips ranked highest-impact first (stable within an impact level).
     *         Empty if neither data source yielded anything — the caller should
     *         then show a "start logging" fallback rather than an empty section.
     */
    public List<TipCandidate> generate(int userId,
                                       List<WaterActivityEntry> activities,
                                       WaterDataList aggregateData) {
        List<WaterActivityEntry> acts = (activities == null) ? List.of() : activities;

        List<TipCandidate> tips = new ArrayList<>();
        addShowerDurationTip(tips, acts);
        addLaundryFrequencyTip(tips, acts);
        addCategoryShareTip(tips, acts);
        addTrendAndOutlierTips(tips, userId, aggregateData);

        tips.sort(Comparator.comparingInt(t -> t.impact().ordinal()));
        return tips;
    }

    // ------------------------------------------------------------------------
    // Activity-based tips (from WaterActivityEntry)
    // ------------------------------------------------------------------------

    /** (a) Average shower length above the 8-minute benchmark. */
    private void addShowerDurationTip(List<TipCandidate> tips, List<WaterActivityEntry> acts) {
        double weightedMinutes = 0;
        double sessions = 0;
        for (WaterActivityEntry e : acts) {
            if (!isActivity(e, "shower")) {
                continue;
            }
            int count = Math.max(1, e.getAmount());
            weightedMinutes += (double) e.getDuration() * count;
            sessions += count;
        }
        if (sessions <= 0) {
            return;
        }
        double avgMinutes = weightedMinutes / sessions;
        if (avgMinutes <= SHOWER_BENCHMARK_MINUTES) {
            return;
        }
        double showersPerWeek = sessions / ASSUMED_LOG_PERIOD_WEEKS;
        double litresPerWeek = (avgMinutes - SHOWER_BENCHMARK_MINUTES) * SHOWER_LITRES_PER_MIN * showersPerWeek;
        Impact impact = (avgMinutes > SHOWER_HIGH_IMPACT_MINUTES) ? Impact.HIGH : Impact.MEDIUM;
        tips.add(tip(
            String.format(Locale.ROOT,
                "Your showers average %d minutes, above the 8-minute recommendation.",
                Math.round(avgMinutes)),
            impact, litresPerWeek));
    }

    /** (b) Running laundry more than ~5 times per week. */
    private void addLaundryFrequencyTip(List<TipCandidate> tips, List<WaterActivityEntry> acts) {
        double loads = 0;
        double litres = 0;
        for (WaterActivityEntry e : acts) {
            if (!isActivity(e, "laundry")) {
                continue;
            }
            loads += Math.max(1, e.getAmount());
            litres += e.getLitres();
        }
        if (loads <= 0) {
            return;
        }
        double loadsPerWeek = loads / ASSUMED_LOG_PERIOD_WEEKS;
        if (loadsPerWeek <= LAUNDRY_LOADS_PER_WEEK_BENCHMARK) {
            return;
        }
        double avgLitresPerLoad = litres / loads;
        double litresPerWeek = (loadsPerWeek - LAUNDRY_LOADS_PER_WEEK_BENCHMARK) * avgLitresPerLoad;
        tips.add(tip(
            "You're running frequent laundry loads -- combining smaller loads could reduce water use.",
            Impact.MEDIUM, litresPerWeek));
    }

    /** (c) One activity category is more than 30% of total recorded litres. */
    private void addCategoryShareTip(List<TipCandidate> tips, List<WaterActivityEntry> acts) {
        Map<String, Double> byCategory = new LinkedHashMap<>();
        double total = 0;
        for (WaterActivityEntry e : acts) {
            String key = (e.getActivity() == null || e.getActivity().isBlank())
                ? "Other" : e.getActivity().trim();
            byCategory.merge(key, e.getLitres(), Double::sum);
            total += e.getLitres();
        }
        if (total <= 0) {
            return;
        }
        Map.Entry<String, Double> top = null;
        for (Map.Entry<String, Double> entry : byCategory.entrySet()) {
            if (top == null || entry.getValue() > top.getValue()) {
                top = entry;
            }
        }
        if (top == null) {
            return;
        }
        double share = top.getValue() / total;
        if (share <= CATEGORY_SHARE_THRESHOLD) {
            return;
        }
        // "Savings" here = the litres above a 30% share, i.e. what bringing this
        // category back in line with the rest of the household would recover.
        double excessLitres = top.getValue() - (CATEGORY_SHARE_THRESHOLD * total);
        double litresPerWeek = Math.max(0, excessLitres) / ASSUMED_LOG_PERIOD_WEEKS;
        tips.add(tip(
            String.format(Locale.ROOT,
                "%s accounts for %d%% of your total recorded water use -- your single biggest category.",
                top.getKey(), Math.round(share * 100)),
            Impact.MEDIUM, litresPerWeek));
    }

    // ------------------------------------------------------------------------
    // Aggregate tips (from WaterDataList)
    // ------------------------------------------------------------------------

    /** (d) week-over-week trend, (e) outlier flag, (f) positive reinforcement. */
    private void addTrendAndOutlierTips(List<TipCandidate> tips, int userId, WaterDataList data) {
        if (data == null) {
            return;
        }
        WaterData latestWeekly = latestWeeklyFor(data, userId);
        if (latestWeekly == null) {
            return;
        }

        double diffPct = data.getUserDiff(latestWeekly);          // signed % vs the user's own weekly mean
        double weeklyMean = data.getUserWeeklyMean();
        double excessLitres = (Double.isFinite(weeklyMean))
            ? Math.max(0, latestWeekly.getWaterUsage() - weeklyMean)
            : 0;

        // (d) trend
        if (Double.isFinite(diffPct) && diffPct > WEEK_TREND_THRESHOLD_PCT) {
            Impact impact = (diffPct > WEEK_TREND_HIGH_PCT) ? Impact.HIGH : Impact.MEDIUM;
            tips.add(tip(
                String.format(Locale.ROOT,
                    "Your usage this week is %d%% above your typical average.",
                    Math.round(diffPct)),
                impact, excessLitres / ASSUMED_LOG_PERIOD_WEEKS));
        }

        // (e) outlier flag -- getZScore() sets usageRating as a side effect.
        // NOTE: in the current WaterDataList, getZScore()'s if-order means only
        // "Normal"/"High" are ever actually produced ("Extreme"/"Outlier" are
        // unreachable), so this tip is effectively future-proofing for when that
        // is fixed. We still check the ratings the spec named, verbatim.
        data.getZScore(latestWeekly);
        String rating = latestWeekly.getUsageRating();
        if ("Extreme".equalsIgnoreCase(rating) || "Outlier".equalsIgnoreCase(rating)) {
            tips.add(tip(
                "Your recent usage was flagged as unusually high compared to your normal pattern.",
                Impact.HIGH, excessLitres / ASSUMED_LOG_PERIOD_WEEKS));
        }

        // (f) positive reinforcement -- only when usage is BELOW average and
        // nothing high-impact was found.
        boolean anyHighImpact = tips.stream().anyMatch(t -> t.impact() == Impact.HIGH);
        if (!anyHighImpact && Double.isFinite(diffPct) && diffPct < 0) {
            tips.add(new TipCandidate(
                "You're tracking below your usual usage this week -- keep it up.",
                Impact.LOW, 0, 0));
        }
    }

    // ------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------

    /** Case-insensitive "activity name contains this word" (e.g. "Shower" matches "shower"). */
    private static boolean isActivity(WaterActivityEntry e, String keyword) {
        String name = e.getActivity();
        return name != null && name.toLowerCase(Locale.ROOT).contains(keyword);
    }

    /** Latest weekly record for the user; the seeded lists are already in chronological order. */
    private static WaterData latestWeeklyFor(WaterDataList data, int userId) {
        WaterData latest = null;
        for (WaterData w : data.getWeeklyWater()) {
            if (w != null && w.getUserID() == userId && "WEEKLY".equals(w.getTimespan())) {
                latest = w;
            }
        }
        return latest;
    }

    /** Builds a tip, deriving the dollar figure from litres via the shared Brisbane rate. */
    private TipCandidate tip(String sentence, Impact impact, double litresSavedPerWeek) {
        double litres = Math.max(0, litresSavedPerWeek);
        return new TipCandidate(sentence, impact, litres, litres * waterRatePerLitre);
    }
}
