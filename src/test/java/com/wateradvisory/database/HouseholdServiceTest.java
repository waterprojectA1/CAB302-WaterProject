package com.wateradvisory.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HouseholdServiceTest {

    // Test 1: Valid 6-character household join code should be accepted
    @Test
    void validJoinCodeShouldReturnTrue() {

        boolean result = HouseholdService.isValidJoinCode("ABC234");
        assertTrue(result);
    }
}