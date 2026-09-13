package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.PromptInjectionDetector;
import com.wateradvisory.Charlie_Root.TopicFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regression guard for a previously-known gap (found via adversarial probing
 * after the item 7/8 TDD cycles). {@link TopicFilter} alone sees legitimate
 * water/usage vocabulary and calls this on-topic -- it has no concept of
 * "read vs. write". The gap is now closed in
 * {@link PromptInjectionDetector#containsInjectionAttempt(String)}, which
 * also matches unauthorized-action phrasing (mutating the user's own data)
 * alongside the original instruction-override patterns, so this request is
 * now caught before the model is ever invoked. Kept as a permanent
 * regression test against a future narrowing of that pattern set.
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
