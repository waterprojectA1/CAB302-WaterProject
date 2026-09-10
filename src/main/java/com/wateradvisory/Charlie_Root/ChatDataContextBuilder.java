package com.wateradvisory.Charlie_Root;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.wateradvisory.Michael_Root.WaterData;
import com.wateradvisory.Michael_Root.WaterDataList;

import javafx.collections.ObservableList;

/**
 * Part 1 -- grounds the chatbot in the user's REAL recorded data.
 *
 * <p>Qwen2.5-1.5B (quantised, running locally via Jlama) is far too small to be
 * trusted to decide when to fetch data or to drive a tool call. So this builder
 * does the fetching in plain Java, BEFORE the model runs, and hands the model a
 * short block of already-computed, already-verified text. Same principle as
 * {@link TipPhraser}: the model paraphrases facts, it never originates them. The
 * model is never given {@link WaterDataList} or any means to query it -- only the
 * finished string this class returns.</p>
 *
 * <p>Flow (see {@code ChatController.onSend}):</p>
 * <ol>
 *   <li>{@link #buildContext(String, int)} runs a keyword intent check on the
 *       user's message -- "is this about their own usage / score / trend?";</li>
 *   <li>if not, it returns {@code null} and the model is prompted as normal;</li>
 *   <li>if so, it pulls the user's latest daily / weekly / monthly records from
 *       {@link WaterDataList} and formats usage, the % difference vs the user's
 *       own average ({@link WaterDataList#getUserDiff(WaterData)}) and the outlier
 *       rating ({@link WaterDataList#getZScore(WaterData)});</li>
 *   <li>if the intent matched but no usable data exists, it returns a block that
 *       tells the model to admit it has no data rather than inventing an answer.</li>
 * </ol>
 *
 * <p><b>User id.</b> {@link WaterDataList}'s per-user aggregate methods
 * ({@code getUserWeeklyMean()} etc.) are hard-wired to its own {@code loggedUser}
 * field (currently {@code 1}), which matches
 * {@code ConservationTipsController.CURRENT_USER_ID}. The app is single-user for
 * now; {@code userId} is threaded through here so this class is ready for when
 * that changes, and is used directly for the "latest record" lookups.</p>
 *
 * <p><b>usageRating values.</b> {@code getZScore} sets {@code usageRating} to
 * "High" for a rounded z-score &gt;= 1 and "Normal" below it. Its "Extreme"
 * (z&nbsp;&gt;=&nbsp;2) and "Outlier" (z&nbsp;&gt;=&nbsp;3) branches are currently
 * unreachable because of the order of its {@code if} checks, so in practice only
 * "Normal" and "High" ever appear. We pass through whatever it returns verbatim
 * -- it is real computed output, and this class does not patch another package's
 * logic.</p>
 */
public final class ChatDataContextBuilder {

    private final WaterDataList data;

    public ChatDataContextBuilder(WaterDataList data) {
        this.data = data;
    }

    /** Phrases that signal the user is asking about their OWN recorded numbers. */
    private static final String[] DATA_INTENT_MARKERS = {
        "my score", "my usage", "my water", "my consumption", "my average", "my data",
        "my trend", "my bill", "my cost", "my history", "my record", "my conservation",
        "why did", "why is", "why has", "why have", "why so", "how come", "what happened",
        "this week", "last week", "this month", "last month", "this year", "today",
        "yesterday", "recently", "lately",
        "dropped", "drop", "fell", "decrease", "decreased", "declin", "went down", "gone down",
        "increase", "increased", "gone up", "went up", "rising", "risen", "spike", "spiked",
        "higher", "lower", "change", "changed", "difference", "compare", "compared", "comparison",
        "above average", "below average", "vs average", "versus average",
        "conservation score", "outlier", "z-score", "zscore",
        "am i using", "how much water am i", "how much am i using", "how many litres am i",
        "how many liters am i", "how am i doing", "how's my", "how is my", "hows my"
    };

    private static final String GROUNDING_INSTRUCTION =
        "\nBase your answer ONLY on the figures above. Do not invent, estimate, re-round, or guess "
      + "any number that is not shown here. If these figures do not actually answer the user's "
      + "question, say so honestly rather than making something up.";

    private static final String NO_DATA_BLOCK =
        "User's recorded water data: none is available for this question.\n"
      + "Tell the user you do not have the recorded data needed to answer that, and do NOT invent, "
      + "estimate, or guess any figures.";

    /**
     * @return a formatted, self-contained context block to inject into the prompt,
     *         or {@code null} if the message is not about the user's own data.
     */
    public String buildContext(String userMessage, int userId) {
        if (userMessage == null) {
            return null;
        }
        String lower = userMessage.toLowerCase(Locale.ROOT);
        if (!looksDataRelated(lower)) {
            return null;
        }

        WaterData daily = latestFor(data.getDailyWater(), userId);
        WaterData weekly = latestFor(data.getWeeklyWater(), userId);
        WaterData monthly = latestFor(data.getMonthlyWater(), userId);

        List<String> lines = new ArrayList<>();
        addUsageLine(lines, "daily", daily);
        addUsageLine(lines, "weekly", weekly);
        addUsageLine(lines, "monthly", monthly);

        String outlier = outlierLine(firstNonNull(weekly, monthly, daily));
        if (outlier != null) {
            lines.add(outlier);
        }

        if (lines.isEmpty()) {
            return NO_DATA_BLOCK;
        }

        StringBuilder sb = new StringBuilder("User's recorded water data (current user):\n");
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        sb.append(GROUNDING_INSTRUCTION);
        return sb.toString();
    }

    private static boolean looksDataRelated(String lowerMessage) {
        for (String marker : DATA_INTENT_MARKERS) {
            if (lowerMessage.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /** Latest = last matching record; the seeded lists are already in chronological order. */
    private static WaterData latestFor(ObservableList<WaterData> records, int userId) {
        WaterData latest = null;
        if (records != null) {
            for (WaterData record : records) {
                if (record != null && record.getUserID() == userId) {
                    latest = record;
                }
            }
        }
        return latest;
    }

    private void addUsageLine(List<String> lines, String label, WaterData record) {
        if (record == null) {
            return;
        }
        double diff = data.getUserDiff(record);          // signed % vs this user's own mean for the period
        double mean = meanFor(record.getTimespan());
        if (!Double.isFinite(diff) || !Double.isFinite(mean)) {
            return;                                       // not enough history to compare -> skip, don't guess
        }
        String direction = diff >= 0 ? "higher than" : "lower than";
        lines.add(String.format(Locale.ROOT,
            "- Latest %s usage: %.0f L (%s to %s) -- %.2f%% %s their average %s usage of %.0f L.",
            label,
            record.getWaterUsage(),
            record.getDate1(), record.getDate2(),
            Math.abs(diff), direction, label, mean));
    }

    private String outlierLine(WaterData record) {
        if (record == null) {
            return null;
        }
        double z = data.getZScore(record);               // side effect: sets record.usageRating
        if (!Double.isFinite(z)) {
            return null;
        }
        String rating = record.getUsageRating();
        return String.format(Locale.ROOT,
            "- Outlier check on the latest %s figure: rated \"%s\" (z-score %.2f vs all households).",
            record.getTimespan().toLowerCase(Locale.ROOT),
            rating == null ? "Normal" : rating,
            z);
    }

    private double meanFor(String timespan) {
        if (timespan == null) {
            return Double.NaN;
        }
        return switch (timespan) {
            case "DAILY" -> data.getUserDailyMean();
            case "WEEKLY" -> data.getUserWeeklyMean();
            case "MONTHLY" -> data.getUserMonthlyMean();
            default -> Double.NaN;
        };
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
