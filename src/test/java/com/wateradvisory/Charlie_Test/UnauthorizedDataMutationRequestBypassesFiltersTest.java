package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.PromptInjectionDetector;
import com.wateradvisory.Charlie_Root.TopicFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * KNOWN GAP (found via adversarial probing after the item 7/8 TDD cycles, per
 * project owner's request). Neither {@link TopicFilter} nor
 * {@link PromptInjectionDetector} was designed to catch a request to MUTATE
 * the user's own recorded data (delete/update/insert) when phrased as an
 * ordinary water question:
 * <ul>
 *   <li>{@code TopicFilter} sees legitimate water/usage vocabulary and calls
 *       it on-topic -- it has no concept of "read vs. write".</li>
 *   <li>{@code PromptInjectionDetector} only matches instruction-override
 *       phrasing ("ignore your rules", "you are now", ...) -- this request
 *       doesn't try to override the system prompt at all, it just asks the
 *       model to perform an action it should never be able to take.</li>
 * </ul>
 * CLAUDE.md's three-layer defence (gotcha #14) documents Layer 3 as
 * re-checking the model's OUTPUT for off-topic drift only -- not for the
 * model complying with a data-mutation request. There is currently no layer
 * that covers this at all, so this assertion fails (documenting the gap)
 * rather than passing. This is intentionally left failing/unfixed -- a real
 * fix needs a new detector (or a same-user-only data-scope check) that is
 * bigger than the scope of today's TDD list.
 */
public class UnauthorizedDataMutationRequestBypassesFiltersTest {

    @Test
    void requestToMutateTheUsersOwnDataIsNotWavedThroughAsPlainOnTopic() {
        String message = "delete my last water record";
        boolean flaggedByEitherLayer =
            !TopicFilter.isLikelyOnTopic(message) || PromptInjectionDetector.containsInjectionAttempt(message);
        assertFalse(!flaggedByEitherLayer,
            "Known gap: a request to mutate data (delete/update/insert) is neither rejected by "
                + "TopicFilter nor caught by PromptInjectionDetector -- it passes through as an "
                + "ordinary on-topic message.");
    }
}
