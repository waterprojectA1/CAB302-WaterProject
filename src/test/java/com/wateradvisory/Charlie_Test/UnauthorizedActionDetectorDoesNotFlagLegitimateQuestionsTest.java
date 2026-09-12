package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.UnauthorizedActionDetector;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class UnauthorizedActionDetectorDoesNotFlagLegitimateQuestionsTest {

    @Test
    void ordinaryOnTopicQuestionsAboutOwnDataAreNotFlagged() {
        UUID currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID currentHouseholdId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        String[] legitimateMessages = {
            "why did my score drop this week?",
            "how can I save water?",
            "what's my usage this month?",
            "how does my household compare to average?",
            "why is my conservation score lower than last week?",
            "what tips do you have for reducing my shower time?"
        };

        for (String message : legitimateMessages) {
            assertFalse(UnauthorizedActionDetector.containsUnauthorizedActionRequest(
                    message, currentUserId, currentHouseholdId),
                "Legitimate, on-topic question about the user's own data must not be flagged: "
                    + message);
        }
    }
}
