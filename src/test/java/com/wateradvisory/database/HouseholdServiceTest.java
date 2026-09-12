package com.wateradvisory.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class HouseholdServiceTest {

    // Test 1: Valid 6-character household join code should be accepted
    @Test
    void validJoinCodeShouldReturnTrue() {

        boolean result = HouseholdService.isValidJoinCode("ABC234");
        assertTrue(result);
    }

    // Test 2: Join code containing special characters should be rejected
    @Test
    void joinCodeWithSpecialCharactersShouldReturnFalse() {

        boolean result = HouseholdService.isValidJoinCode("AB#234");
        assertFalse(result);
    }
}