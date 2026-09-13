package com.wateradvisory.Charlie_Test;

import com.wateradvisory.Charlie_Root.HouseholdIdValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HouseholdIdValidatorRejectsMalformedUuidTest {

    @Test
    void rejectsMalformedStringsAndAcceptsWellFormedUuids() {
        assertFalse(HouseholdIdValidator.isValidUuid("not-a-uuid"),
                "A plainly malformed string must not be accepted as a UUID");
        assertTrue(HouseholdIdValidator.isValidUuid("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
                "A canonical UUID string must be accepted");
    }
}
