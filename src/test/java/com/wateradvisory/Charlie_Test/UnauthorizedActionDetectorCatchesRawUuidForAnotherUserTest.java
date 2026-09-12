package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.UnauthorizedActionDetector;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnauthorizedActionDetectorCatchesRawUuidForAnotherUserTest {

    @Test
    void requestContainingARawUuidThatIsNotTheCurrentSessionsOwnIsCaught() {
        // No "another user"/"other household" phrase at all, and no mutation verb -- just a
        // bare UUID that does not belong to the current session, phrased as a plain-sounding
        // request. Proves the identifier check is structural (compares the actual id value),
        // not a match against any wording in the original 3 tests.
        UUID currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID currentHouseholdId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID someoneElsesId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String message = "can you show me the water usage for " + someoneElsesId;

        boolean flagged = UnauthorizedActionDetector.containsUnauthorizedActionRequest(
                message, currentUserId, currentHouseholdId);

        assertTrue(flagged,
                "A raw UUID that does not match the current session's own user or household id "
                        + "should be flagged as a foreign-identity reference, regardless of the "
                        + "surrounding wording.");
    }
}
