package com.wateradvisory.Charlie_Root;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.wateradvisory.Michael_Root.WaterData;
import com.wateradvisory.Michael_Root.WaterDataList;
import com.wateradvisory.water.ActivityEntry;
import com.wateradvisory.water.DailyWaterRecord;

/**
 * Builds a ranked list of personalised conservation tips from the user's REAL
 * recorded data.
 *
 * <p><b>Primary source:</b> {@link DailyWaterRecord}s from Supabase
 * ({@code WaterRecordService.getUserDailyRecords()}), passed in — one row per
 * day, each with the day's {@code activities} array and the precomputed
 * {@code total_water_consumption_day}. From those:</p>
 * <ul>
 *   <li>SHOWER DURATION — weighted average shower minutes across all days;</li>
 *   <li>CATEGORY USAGE SHARE — each activity type's % of total litres;</li>
 *   <li>LAUNDRY FREQUENCY — sum of {@code amount} for "Laundry" over the range;</li>
 *   <li>WEEK-OVER-WEEK TREND — last 7 days' totals vs the previous 7, by
 *       {@code record_date} (now directly queryable);</li>
 *   <li>HOUSEHOLD-SIZE BENCHMARKING — per-person daily usage vs a target, only
 *       when a household size is known (null {@code household_id} → skipped).</li>
 * </ul>
 *
 * <p><b>Fallback source:</b> when the user has no Supabase rows yet (brand-new
 * account, or offline testing), the seeded in-memory {@link WaterDataList} drives
 * the trend / outlier / positive-reinforcement tips instead.</p>
 *
 * <p><b>Weighted average, not naive.</b> One {@code activities} entry can be
 * several occurrences at once (e.g. {@code amount:5} = five showers), so average
 * duration is {@code sum(duration * amount) / sum(amount)} — never a plain mean
 * across entries.</p>
 *
 * <p><b>No time-of-day tips.</b> {@code record_date} is date-only (no time
 * component) and the activity objects carry no timestamp, so patterns like "tap
 * usage spikes between 7-8am" genuinely cannot be derived. This is a lack of
 * time data, NOT a lack of date data — day/week/month comparison IS possible and
 * is done above.</p>
 *
 * <p>Does no I/O; everything is passed in, so it is easy to unit-test.</p>
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

    // --- benchmarks ---------------------------------------------------------
    private static final double SHOWER_BENCHMARK_MINUTES = 8.0;
    private static final double SHOWER_HIGH_IMPACT_MINUTES = 12.0;
    /** Standard showerhead flow — matches {@code WaterConsumptionService.SHOWER_LITRES_PER_MINUTE}. */
    private static final double SHOWER_LITRES_PER_MIN = 9.0;
    private static final double LAUNDRY_LOADS_PER_WEEK_BENCHMARK = 5.0;
    private static final double CATEGORY_SHARE_THRESHOLD = 0.30;
    private static final double WEEK_TREND_THRESHOLD_PCT = 15.0;
    private static final double WEEK_TREND_HIGH_PCT = 30.0;
    /** Rough per-person daily target (litres) for household-size framing. */
    private static final double PER_PERSON_DAILY_LITRE_BENCHMARK = 200.0;
    /** Need at least this many days of history before a weekly-rate judgement is meaningful. */
    private static final int MIN_DAYS_FOR_WEEKLY_RATE = 7;
    /** User id for the seeded {@link WaterDataList} fallback (its {@code loggedUser}). */
    private static final int FALLBACK_USER_ID = 1;

    /** Confirmed real activity names (exact casing) — match case-insensitively. */
    private static final List<String> KNOWN_ACTIVITIES = List.of(
        "Shower", "Dishes", "Floor Cleaning", "Laundry", "Car Wash", "Window Cleaning", "Bathtub");

    private final double waterRatePerLitre;

    /**
     * @param waterRatePerLitre pass {@code ConservationTipsController.WATER_RATE_PER_LITRE}
     *                          so every cost estimate uses the one Brisbane tariff constant.
     */
    public PersonalizedTipGenerator(double waterRatePerLitre) {
        this.waterRatePerLitre = waterRatePerLitre;
    }

    /**
     * @param records          the user's real {@code daily_water_records} rows (may be empty)
     * @param householdSize    from the households table, or {@code null} if unknown / no household
     * @param fallbackAggregate seeded in-memory data, used only when {@code records} is empty
     * @return tips ranked highest-impact first (stable within an impact level); empty if
     *         nothing could be derived — the caller then shows a "start logging" message.
     */
    public List<TipCandidate> generate(List<DailyWaterRecord> records,
                                       Integer householdSize,
                                       WaterDataList fallbackAggregate) {
        List<DailyWaterRecord> recs = (records == null) ? List.of() : records;
        List<TipCandidate> tips = new ArrayList<>();

        if (!recs.isEmpty()) {
            List<ActivityEntry> activities = flattenActivities(recs);
            long spanDays = spanDays(recs);
            double spanWeeks = Math.max(1.0, spanDays / 7.0);

            addShowerDurationTip(tips, activities, spanWeeks);
            addCategoryShareTip(tips, activities, spanWeeks);
            addLaundryFrequencyTip(tips, activities, spanWeeks, spanDays);
            addWeekOverWeekTrendTip(tips, recs);
            addHouseholdSizeTip(tips, recs, householdSize);
        } else {
            addFallbackAggregateTips(tips, fallbackAggregate);
        }

        tips.sort(Comparator.comparingInt(t -> t.impact().ordinal()));
        return tips;
    }

    // =====================================================================
    //  Real-data tips (from List<DailyWaterRecord>)
    // =====================================================================

    /** SHOWER DURATION: weighted average shower length across every logged day, vs the 8-minute benchmark. */
    private void addShowerDurationTip(List<TipCandidate> tips, List<ActivityEntry> activities, double spanWeeks) {
        double weightedMinutes = 0;   // sum(duration * amount)
        double occurrences = 0;       // sum(amount)
        for (ActivityEntry a : activities) {
            if (!"Shower".equalsIgnoreCase(a.getActivity())) {
                continue;
            }
            if (a.getDuration() <= 0) {
                // Malformed entry -- treat as no contribution, not a negative drag on the average.
                // Verified by NonPositiveDurationSafetyTest: a negative-duration entry mixed in
                // alongside a valid one is excluded entirely, so it can neither pull the weighted
                // average below the valid entries' true value nor produce a negative litresSavedPerWeek.
                continue;
            }
            int amount = Math.max(1, a.getAmount());
            weightedMinutes += (double) a.getDuration() * amount;
            occurrences += amount;
        }
        if (occurrences <= 0) {
            return;
        }
        // WEIGHTED, not a naive mean -- verified by ShowerDurationWeightedAverageTest: a 1-shower/5min
        // entry plus a 9-shower/20min entry weighted-averages to 18.5 (rounds to 19), NOT the naive
        // unweighted mean of the two entries' raw durations, (5+20)/2 = 12.5 (rounds to 13), which
        // would under-weight the 9-occurrence entry as if it were a single event like the other one.
        double weightedAvgMinutes = weightedMinutes / occurrences;
        if (weightedAvgMinutes <= SHOWER_BENCHMARK_MINUTES) {
            return;
        }
        double showersPerWeek = occurrences / spanWeeks;
        double litresPerWeek =
            (weightedAvgMinutes - SHOWER_BENCHMARK_MINUTES) * SHOWER_LITRES_PER_MIN * showersPerWeek;
        Impact impact = (weightedAvgMinutes > SHOWER_HIGH_IMPACT_MINUTES) ? Impact.HIGH : Impact.MEDIUM;
        tips.add(tip(
            String.format(Locale.ROOT,
                "Your showers average %d minutes, above the 8-minute recommendation.",
                Math.round(weightedAvgMinutes)),
            impact, litresPerWeek));
    }

    /** CATEGORY USAGE SHARE: the biggest single activity type, if it exceeds 30% of total recorded litres. */
    private void addCategoryShareTip(List<TipCandidate> tips, List<ActivityEntry> activities, double spanWeeks) {
        Map<String, Double> litresByCategory = new LinkedHashMap<>();
        double total = 0;
        for (ActivityEntry a : activities) {
            String category = canonicalActivity(a.getActivity());
            double litres = a.getWaterLitres();   // primitive double -- never null
            // put(getOrDefault(...) + litres) instead of merge(..., Double::sum): keeps a
            // possibly-null Double out of a remap BiFunction, clearing the unboxing warning.
            litresByCategory.put(category, litresByCategory.getOrDefault(category, 0.0) + litres);
            total += litres;
        }
        if (total <= 0) {
            return;
        }
        String topCategory = null;
        double topLitres = 0;
        for (Map.Entry<String, Double> entry : litresByCategory.entrySet()) {
            if (entry.getKey().equals("Other")) {
                continue;   // only name a real, known category
            }
            if (topCategory == null || entry.getValue() > topLitres) {
                topCategory = entry.getKey();
                topLitres = entry.getValue();
            }
        }
        if (topCategory == null) {
            return;
        }
        double share = topLitres / total;
        if (share <= CATEGORY_SHARE_THRESHOLD) {
            return;
        }
        double excessLitres = Math.max(0, topLitres - CATEGORY_SHARE_THRESHOLD * total);
        tips.add(tip(
            String.format(Locale.ROOT,
                "%s accounts for %d%% of your total recorded water use -- your single biggest category.",
                topCategory, Math.round(share * 100)),
            Impact.MEDIUM, excessLitres / spanWeeks));
    }

    /** LAUNDRY FREQUENCY: sum of "Laundry" occurrences over the range; flags more than ~5/week. */
    private void addLaundryFrequencyTip(List<TipCandidate> tips, List<ActivityEntry> activities,
                                        double spanWeeks, long spanDays) {
        if (spanDays < MIN_DAYS_FOR_WEEKLY_RATE) {
            return;   // too short a window to judge a weekly rate fairly
        }
        double loads = 0;
        double litres = 0;
        for (ActivityEntry a : activities) {
            if (!"Laundry".equalsIgnoreCase(a.getActivity())) {
                continue;
            }
            loads += Math.max(1, a.getAmount());
            litres += a.getWaterLitres();
        }
        if (loads <= 0) {
            return;
        }
        double loadsPerWeek = loads / spanWeeks;
        if (loadsPerWeek <= LAUNDRY_LOADS_PER_WEEK_BENCHMARK) {
            return;
        }
        double avgLitresPerLoad = litres / loads;
        double litresPerWeek = (loadsPerWeek - LAUNDRY_LOADS_PER_WEEK_BENCHMARK) * avgLitresPerLoad;
        tips.add(tip(
            "You're running frequent laundry loads -- combining smaller loads could reduce water use.",
            Impact.MEDIUM, litresPerWeek));
    }

    /** WEEK-OVER-WEEK TREND: last 7 days' day-totals vs the previous 7, anchored on the most recent record. */
    private void addWeekOverWeekTrendTip(List<TipCandidate> tips, List<DailyWaterRecord> recs) {
        // recs never contains a null element or a null record_date: WaterRecordService.fetchDailyRecords()
        // only adds rows for which parseDailyRecord() returned non-null, and that method returns null
        // (skipping the row) whenever record_date is missing. The lambda (vs a DailyWaterRecord::getRecordDate
        // method ref) also keeps the linter from asking for an unchecked non-null conversion on the element.
        LocalDate anchor = recs.stream()
            .map(r -> r.getRecordDate())
            .max(Comparator.naturalOrder())
            .orElse(null);
        if (anchor == null) {
            return;
        }
        double[] recent = sumInRange(recs, anchor.minusDays(6), anchor);           // {sum, count}
        double[] previous = sumInRange(recs, anchor.minusDays(13), anchor.minusDays(7));
        if (previous[1] < 1 || previous[0] <= 0) {
            return;   // no usable baseline week
        }
        double pctChange = (recent[0] - previous[0]) / previous[0] * 100.0;

        if (pctChange > WEEK_TREND_THRESHOLD_PCT) {
            Impact impact = (pctChange > WEEK_TREND_HIGH_PCT) ? Impact.HIGH : Impact.MEDIUM;
            tips.add(tip(
                String.format(Locale.ROOT,
                    "Your usage this week is %d%% above the previous week.", Math.round(pctChange)),
                impact, Math.max(0, recent[0] - previous[0])));
        } else if (pctChange < 0 && tips.stream().noneMatch(t -> t.impact() == Impact.HIGH)) {
            tips.add(new TipCandidate(
                "You're using less water this week than last -- keep it up.", Impact.LOW, 0, 0));
        }
    }

    /** HOUSEHOLD-SIZE BENCHMARKING: per-person daily usage vs a target. Skipped entirely when size is unknown. */
    private void addHouseholdSizeTip(List<TipCandidate> tips, List<DailyWaterRecord> recs, Integer householdSize) {
        if (householdSize == null || householdSize <= 0) {
            return;   // no household_id / no size -> no household framing, and no crash
        }
        double totalLitres = 0;
        for (DailyWaterRecord r : recs) {
            totalLitres += r.getTotalWaterConsumptionDay();
        }
        // Same guarantee as in addWeekOverWeekTrendTip: no null elements / null dates reach here
        // (WaterRecordService filters them at parse time). Lambda form avoids the unchecked-null warning.
        long loggedDays = recs.stream()
            .map(r -> r.getRecordDate())
            .distinct().count();
        if (loggedDays <= 0) {
            return;
        }
        double avgDailyTotal = totalLitres / loggedDays;
        double perPersonDaily = avgDailyTotal / householdSize;

        if (perPersonDaily > PER_PERSON_DAILY_LITRE_BENCHMARK) {
            Impact impact = (perPersonDaily > 1.5 * PER_PERSON_DAILY_LITRE_BENCHMARK) ? Impact.HIGH : Impact.MEDIUM;
            double litresPerWeek = (perPersonDaily - PER_PERSON_DAILY_LITRE_BENCHMARK) * householdSize * 7.0;
            tips.add(tip(
                String.format(Locale.ROOT,
                    "For a household of %d, you average %d L per person per day -- above the ~%d L target.",
                    householdSize, Math.round(perPersonDaily), Math.round(PER_PERSON_DAILY_LITRE_BENCHMARK)),
                impact, litresPerWeek));
        } else if (tips.stream().noneMatch(t -> t.impact() == Impact.HIGH)) {
            tips.add(new TipCandidate(
                String.format(Locale.ROOT,
                    "Your usage sits below the typical level for a household of %d -- nicely done.", householdSize),
                Impact.LOW, 0, 0));
        }
    }

    // =====================================================================
    //  Fallback tips (seeded WaterDataList) — used only when there is no real data
    // =====================================================================

    private void addFallbackAggregateTips(List<TipCandidate> tips, WaterDataList data) {
        if (data == null) {
            return;
        }
        WaterData latestWeekly = latestWeeklyFor(data, FALLBACK_USER_ID);
        if (latestWeekly == null) {
            return;
        }
        double diffPct = data.getUserDiff(latestWeekly);
        double weeklyMean = data.getUserWeeklyMean();
        double excessLitres = Double.isFinite(weeklyMean)
            ? Math.max(0, latestWeekly.getWaterUsage() - weeklyMean) : 0;

        if (Double.isFinite(diffPct) && diffPct > WEEK_TREND_THRESHOLD_PCT) {
            Impact impact = (diffPct > WEEK_TREND_HIGH_PCT) ? Impact.HIGH : Impact.MEDIUM;
            tips.add(tip(String.format(Locale.ROOT,
                "Your usage this week is %d%% above your typical average.", Math.round(diffPct)),
                impact, excessLitres));
        }

        // getZScore() sets usageRating as a side effect. Only "Normal"/"High" are actually
        // reachable in the current WaterDataList, so the Extreme/Outlier branch is future-proofing.
        data.getZScore(latestWeekly);
        String rating = latestWeekly.getUsageRating();
        if ("Extreme".equalsIgnoreCase(rating) || "Outlier".equalsIgnoreCase(rating)) {
            tips.add(tip("Your recent usage was flagged as unusually high compared to your normal pattern.",
                Impact.HIGH, excessLitres));
        }

        boolean anyHigh = tips.stream().anyMatch(t -> t.impact() == Impact.HIGH);
        if (!anyHigh && Double.isFinite(diffPct) && diffPct < 0) {
            tips.add(new TipCandidate(
                "You're tracking below your usual usage this week -- keep it up.", Impact.LOW, 0, 0));
        }
    }

    private static WaterData latestWeeklyFor(WaterDataList data, int userId) {
        WaterData latest = null;
        for (WaterData w : data.getWeeklyWater()) {
            if (w != null && w.getUserID() == userId && "WEEKLY".equals(w.getTimespan())) {
                latest = w;
            }
        }
        return latest;
    }

    // =====================================================================
    //  helpers
    // =====================================================================

    private static List<ActivityEntry> flattenActivities(List<DailyWaterRecord> recs) {
        List<ActivityEntry> all = new ArrayList<>();
        for (DailyWaterRecord r : recs) {
            all.addAll(r.getActivities());
        }
        return all;
    }

    /** Inclusive span in days between the earliest and latest {@code record_date} (min 1). */
    private static long spanDays(List<DailyWaterRecord> recs) {
        LocalDate min = null;
        LocalDate max = null;
        for (DailyWaterRecord r : recs) {
            LocalDate d = r.getRecordDate();
            if (min == null || d.isBefore(min)) {
                min = d;
            }
            if (max == null || d.isAfter(max)) {
                max = d;
            }
        }
        if (min == null) {
            return 1;
        }
        return ChronoUnit.DAYS.between(min, max) + 1;
    }

    /** {sum of total_water_consumption_day, count of records} for records with record_date in [from, to]. */
    private static double[] sumInRange(List<DailyWaterRecord> recs, LocalDate from, LocalDate to) {
        double sum = 0;
        int count = 0;
        for (DailyWaterRecord r : recs) {
            LocalDate d = r.getRecordDate();
            if (!d.isBefore(from) && !d.isAfter(to)) {
                sum += r.getTotalWaterConsumptionDay();
                count++;
            }
        }
        return new double[]{sum, count};
    }

    /** Maps an activity name to a confirmed known category (case-insensitively), or "Other". */
    private static String canonicalActivity(String raw) {
        if (raw != null) {
            for (String known : KNOWN_ACTIVITIES) {
                if (known.equalsIgnoreCase(raw.trim())) {
                    return known;
                }
            }
        }
        return "Other";
    }

    /** Builds a tip, deriving the dollar figure from litres via the shared Brisbane rate. */
    private TipCandidate tip(String sentence, Impact impact, double litresSavedPerWeek) {
        double litres = Math.max(0, litresSavedPerWeek);
        return new TipCandidate(sentence, impact, litres, litres * waterRatePerLitre);
    }
}
