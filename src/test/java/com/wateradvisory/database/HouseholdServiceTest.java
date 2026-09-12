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

    // Test 3: Lowercase join codes should be rejected
    @Test
    void lowercaseJoinCodeShouldReturnFalse() {

        boolean result =HouseholdService.isValidJoinCode("abc234");
        assertFalse(result);
    }

    // Test 4: Ambiguous characters should be rejected
    @Test
    void joinCodeWithAmbiguousCharactersShouldReturnFalse() {

        boolean result = HouseholdService.isValidJoinCode("ABO234");
        assertFalse(result);
    }

    // Test 5: Blank household names should be rejected
    @Test
    void blankHouseholdNameShouldReturnFalse() {

        boolean result = HouseholdService.isValidHouseholdName("   ");
        assertFalse(result);
    }
}