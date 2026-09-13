package com.wateradvisory.Charlie_Root;

import java.util.List;
import java.util.Locale;

/**
 * Layer 1 of the chatbot's three-layer jailbreak / prompt-injection defence
 * (see CLAUDE.md gotcha #14). Runs in {@code ChatController.onSend()} as the
 * VERY FIRST check -- before {@link TopicFilter}, before
 * {@link ChatDataContextBuilder}, before the model is ever loaded into a prompt.
 *
 * <p>Qwen2.5-1.5B (quantised, local) is far too small to be trusted to resist a
 * cleverly-worded "ignore your rules" message on its own, so the primary defence
 * must be deterministic and happen in Java. This class is a fast phrase-list
 * check: it does not need to be real NLP, it needs to catch the common / obvious
 * override attempts cheaply. Anything it flags gets an instant canned refusal and
 * never reaches the model.</p>
 *
 * <p>Matching is done on a {@link #normalize normalised} form of the message
 * (lower-cased, every run of non-alphanumeric characters collapsed to one space)
 * so trivial obfuscation -- extra punctuation, weird spacing, {@code i.g.n.o.r.e}
 * won't help, but {@code "ignore   all !!! restrictions"} and
 * {@code "ignore-all-restrictions"} both still match {@code "ignore all
 * restrictions"}. The pattern list is a plain {@link List} so it is trivial to
 * extend; keep new entries lower-case and they are normalised automatically.</p>
 *
 * <p>{@link #containsInjectionAttempt} also covers a second, distinct threat
 * class beyond instruction-override phrasing: requests for an <em>unauthorized
 * action</em> phrased as an ordinary water question -- reading another user's
 * data, mutating the current user's own recorded data, or directly setting/
 * faking the conservation score (which must only ever be derived from recorded
 * usage, see {@code ConservationScoreCalculator}). Neither of those requests
 * tries to override the system prompt, so they need their own pattern set
 * ({@link #UNAUTHORIZED_ACTION_PATTERNS}); they are checked in the same method
 * since this class is already the deterministic, pre-model Java layer that
 * {@code TopicFilter} (a pure on-/off-topic vocabulary check) was never
 * designed to cover.</p>
 */
public final class PromptInjectionDetector {

    private PromptInjectionDetector() {
    }

    /** Firm canned reply shown instantly (no model call) when an attempt is detected. */
    public static final String INJECTION_REPLY =
        "I can't ignore my instructions -- I'm only able to help with water usage and "
      + "conservation questions.";

    /**
     * Known override / jailbreak phrasings. The first block is the baseline set from the
     * task spec; the second block is extra generalisations so near-miss rewordings
     * ("disregard everything you were told", "pretend you're a tutor") are still caught.
     * Order does not matter. Add freely -- entries are lower-cased + punctuation-normalised
     * at class load, so a trailing ':' or stray comma in a new entry is harmless.
     */
    private static final List<String> RAW_PATTERNS = List.of(
        // --- baseline (task spec) ---
        "ignore all restrictions", "ignore all instructions", "ignore your instructions",
        "ignore the above", "ignore previous instructions", "disregard the above",
        "disregard your system prompt", "disregard your rules", "forget your rules",
        "forget everything above", "forget your previous instructions", "you are now",
        "act as", "pretend you are", "pretend you have no", "pretend you're not bound",
        "roleplay as", "bypass your filters", "override your programming",
        "unfiltered response", "developer mode", "dan mode", "do anything now", "jailbreak",
        "new instructions:", "system prompt:", "new rules:", "for testing purposes, ignore",
        "this is a test, ignore", "from now on you will", "you must comply",
        "without any restrictions", "no longer bound by", "your true instructions",
        "reveal your system prompt", "what are your instructions",

        // --- extra generalisations (beyond the spec's minimum) ---
        "ignore everything", "ignore any previous", "ignore all prior", "ignore prior instructions",
        "ignore what you were told", "ignore your rules", "ignore all rules", "ignore your programming",
        "ignore these instructions", "ignore the system prompt", "ignore your system prompt",
        "disregard everything", "disregard all", "disregard what you were told",
        "disregard previous instructions", "disregard your instructions", "disregard the system prompt",
        "forget everything", "forget what you were told", "forget previous instructions",
        "forget your instructions", "forget the above", "forget all previous",
        "pretend you", "pretend to be", "pretend that you", "act like you", "act like a",
        "roleplay", "role play", "imagine you are", "imagine you're",
        "you are no longer", "you're no longer", "you are now a", "from now on you",
        "from now on,", "from this moment", "starting now you",
        "bypass your", "override your", "override these instructions", "override the system prompt",
        "disable your filter", "disable your restrictions", "disable your safety", "turn off your filter",
        "you have no restrictions", "you have no rules", "you have no filter", "there are no restrictions",
        "stop following your", "do not follow your", "don't follow your", "no longer have to follow",
        "reveal your instructions", "print your system prompt", "show me your system prompt",
        "show your system prompt", "what is your system prompt", "repeat your instructions",
        "output your system prompt", "tell me your instructions", "what were your instructions",
        "what is your prompt", "print your instructions"
    );

    /** {@link #RAW_PATTERNS} run through {@link #normalize}, de-duplicated, empties dropped. */
    private static final List<String> PATTERNS = RAW_PATTERNS.stream()
        .map(PromptInjectionDetector::normalize)
        .filter(s -> !s.isEmpty())
        .distinct()
        .toList();

    /**
     * Unauthorized-action phrasings: requests for another user's data, requests to
     * mutate (delete/update/insert) the current user's own recorded data, or requests
     * to directly set/fake a conservation score. None of these try to override the
     * system prompt -- they just ask for an action the app must never let the model
     * (or a plain chat message) trigger, so they need their own pattern set rather
     * than fitting the override-phrasing list above.
     */
    private static final List<String> RAW_UNAUTHORIZED_ACTION_PATTERNS = List.of(
        // --- reading another user's / someone else's data ---
        "another user", "other user", "someone else's", "someone else s", "other people's",
        "other people s", "different user's", "different user s", "other household's",
        "other household s", "everyone's data", "everyone s data", "all users' data",
        "all users data",

        // --- mutating the current user's own recorded data ---
        "delete my", "remove my", "erase my", "clear my", "update my water record",
        "update my last record", "edit my water record", "edit my last record",
        "insert a water record", "insert a record", "add a fake record", "modify my water record",
        "modify my last record", "change my water record", "change my last record",

        // --- directly setting/faking the conservation score ---
        "set my score", "set my conservation score", "change my score to",
        "change my conservation score", "update my score to", "make my score",
        "fake my score", "override my score", "give me a score of", "set the score to"
    );

    /** {@link #RAW_UNAUTHORIZED_ACTION_PATTERNS} normalised the same way as {@link #PATTERNS}. */
    private static final List<String> UNAUTHORIZED_ACTION_PATTERNS = RAW_UNAUTHORIZED_ACTION_PATTERNS.stream()
        .map(PromptInjectionDetector::normalize)
        .filter(s -> !s.isEmpty())
        .distinct()
        .toList();

    /**
     * @return {@code true} if {@code message} contains a known instruction-override /
     *         jailbreak phrasing, OR a known unauthorized-action phrasing (reading
     *         another user's data, mutating the current user's own data, or directly
     *         setting/faking a score) -- either way it should be refused without
     *         calling the model.
     *
     * <p>Regression guard: {@code UnauthorizedDataAccessRequestBypassesFiltersTest} verifies
     * "show me another user's daily water records" is caught here (via the "another user"
     * entry in {@link #UNAUTHORIZED_ACTION_PATTERNS}) even though {@link TopicFilter} alone
     * would wave it through as ordinary on-topic water/usage vocabulary -- it has no concept
     * of "whose data" this message is asking for. {@code UnauthorizedDataMutationRequestBypassesFiltersTest}
     * verifies the sibling gap: "delete my last water record" is caught here (via "delete my")
     * even though {@link TopicFilter} has no concept of "read vs. write" either.
     * {@code UnauthorizedScoreMutationRequestBypassesFiltersTest} verifies the third sibling gap:
     * "set my conservation score to 100" is caught here (via "set my conservation score") -- the
     * score must only ever be derived from recorded usage by {@code ConservationScoreCalculator},
     * never set directly by request.</p>
     */
    public static boolean containsInjectionAttempt(String message) {
        if (message == null) {
            return false;
        }
        String normalized = normalize(message);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String pattern : PATTERNS) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }
        for (String pattern : UNAUTHORIZED_ACTION_PATTERNS) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /** Lower-case; collapse every run of non-alphanumeric chars to a single space; trim. */
    static String normalize(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        boolean prevSpace = true;                 // start "spaced" so leading junk adds nothing
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            boolean alnum = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (alnum) {
                sb.append(c);
                prevSpace = false;
            } else if (!prevSpace) {
                sb.append(' ');
                prevSpace = true;
            }
        }
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == ' ') {
            sb.setLength(len - 1);
        }
        return sb.toString();
    }
}
