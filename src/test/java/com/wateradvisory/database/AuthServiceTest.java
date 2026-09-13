package com.wateradvisory.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthServiceTest {

    // Test 1: Blank email should be rejected
    @Test
    void blankEmailShouldReturnFalse() {

        boolean result = AuthService.isValidLoginInput("   ","password123");
        assertFalse(result);
    }
}