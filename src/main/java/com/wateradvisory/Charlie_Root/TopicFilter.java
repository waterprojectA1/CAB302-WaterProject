package com.wateradvisory.Charlie_Root;

import java.util.Locale;
import java.util.Set;

/**
 * Part 2 -- fast, zero-latency Java pre-check that keeps the chatbot on topic.
 *
 * <p>{@link #isLikelyOnTopic(String)} runs BEFORE the model is touched at all, so
 * a clearly off-topic message ("write me a poem about cats", "capital of France")
 * gets an instant canned reply and never pays the cost of local generation. It is
 * deliberately lenient: a broad allowlist of water / conservation / app terms
 * plus a few "about the app itself" phrases and greetings. Anything borderline is
 * waved through to the model, whose system prompt then does the finer-grained
 * refusal (see {@code ChatController.SYSTEM_PROMPT}). The only job here is to
 * catch the obvious junk cheaply.</p>
 */
public final class TopicFilter {

    private TopicFilter() {
    }

    /** Shown instantly (no model call) when {@link #isLikelyOnTopic} returns false. */
    public static final String OFF_TOPIC_REPLY =
        "I can only help with water usage and conservation questions -- try asking about your "
      + "usage, tips, or your conservation score.";

    /** Broad allowlist. Substring matching is fine: this is meant to over-accept, not to be precise. */
    private static final Set<String> TOPIC_TERMS = Set.of(
        "water", "usage", "consumption", "consume", "using", "litre", "liter", "litres", "liters",
        "gallon", "kilolitre", "leak", "drip", "shower", "bath", "tap", "faucet", "toilet", "flush",
        "hose", "sprinkler", "irrigation", "garden", "lawn", "watering", "dishwasher", "laundry",
        "washing machine", "greywater", "rainwater", "tank", "conserve", "conservation", "save",
        "saving", "efficient", "efficiency", "reduce", "cut back", "score", "tip", "tips", "advice",
        "recommend", "bill", "cost", "charge", "tariff", "rate", "household", "drought",
        "restriction", "rebate", "regulation", "benchmark", "average", "trend", "outlier",
        "z-score", "zscore", "spike", "meter", "reading", "data", "analytics", "dashboard",
        "seasonal", "summer", "winter", "flow rate", "aerator", "runoff", "evaporation",
        "consumption data", "my usage", "my score"
    );

    /** Short "what is this thing / what can you do" style questions about the app itself. */
    private static final Set<String> APP_PHRASES = Set.of(
        "what can you", "what do you do", "who are you", "what are you", "how do you work",
        "how does this work", "what is this", "what can this", "can you help", "help me",
        "help with", "what should i ask", "how do i use", "your name"
    );

    /** Harmless conversational openers -- let them reach the model so it can greet naturally. */
    private static final Set<String> GREETINGS = Set.of(
        "hi", "hello", "hey", "hiya", "yo", "howdy", "thanks", "thank you", "ok", "okay", "cheers"
    );

    public static boolean isLikelyOnTopic(String message) {
        if (message == null) {
            return false;
        }
        String m = message.toLowerCase(Locale.ROOT).strip();
        if (m.isEmpty()) {
            return false;
        }

        String bare = m.replaceAll("[!.?]+$", "").strip();
        if (GREETINGS.contains(bare)) {
            return true;
        }

        for (String phrase : APP_PHRASES) {
            if (m.contains(phrase)) {
                return true;
            }
        }
        for (String term : TOPIC_TERMS) {
            if (m.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
