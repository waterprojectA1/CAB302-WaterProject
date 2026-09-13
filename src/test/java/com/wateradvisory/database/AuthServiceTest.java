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

    // Test 2: Invalid email format should be rejected
    @Test
    void invalidEmailFormatShouldReturnFalse() {

        boolean result = AuthService.isValidLoginInput("not-an-email","password123");
        assertFalse(result);
    }

    // Test 3: Email should remove surrounding spaces
    @Test
    void emailShouldRemoveSurroundingSpaces() {

        String result = AuthService.normaliseEmail("  user@gmail.com  ");
        assertEquals("user@gmail.com", result);
    }

    // Test 4: Blank username should be rejected during registration
    @Test
    void blankUsernameShouldReturnFalse() {

        boolean result = AuthService.isValidRegistrationInput("user@gmail.com","   ","password123");
        assertFalse(result);
    }

    // Test 5: Username should remove surrounding spaces
    @Test
    void usernameShouldRemoveSurroundingSpaces() {

        String result = AuthService.normaliseUsername("  Pogoy  ");
        assertEquals("Pogoy", result);
    }
}