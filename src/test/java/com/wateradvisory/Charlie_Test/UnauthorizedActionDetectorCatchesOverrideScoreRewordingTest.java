package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.UnauthorizedActionDetector;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnauthorizedActionDetectorCatchesOverrideScoreRewordingTest {

    @Test
    void overrideMyScoreRewordingIsCaughtByVerbNounCombinationNotLiteralPhraseMatch() {
        // Different verb ("override") than the original "set my conservation score to 100" test --
        // proves the mutation-verb + mutable-data-noun combination generalizes across verb choice
        // rather than matching one specific literal phrase.
        String message = "override my conservation score to 100";
        UUID currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID currentHouseholdId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        boolean flagged = UnauthorizedActionDetector.containsUnauthorizedActionRequest(
                message, currentUserId, currentHouseholdId);

        assertTrue(flagged,
                "A mutation verb ('override') combined with a mutable-data noun ('score') should "
                        + "be caught by the verb+noun structural check, regardless of the specific "
                        + "verb chosen.");
    }
}
