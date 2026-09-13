package com.wateradvisory.Charlie_Root;

import java.util.regex.Pattern;

/**
 * Validates that a string is a well-formed UUID before it is trusted as a
 * Supabase {@code household_id} / {@code user_id} value (see CLAUDE.md
 * gotcha #15 -- both columns are nullable uuid, not int). Callers should use
 * this before {@code UUID.fromString(...)} on any string sourced from user
 * input or an untrusted external source, to avoid an {@code
 * IllegalArgumentException} deeper in the call stack.
 */
public final class HouseholdIdValidator {

    private HouseholdIdValidator() {
    }

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    /** @return {@code true} if {@code value} is a canonical 8-4-4-4-12 hex UUID string. */
    public static boolean isValidUuid(String value) {
        return value != null && UUID_PATTERN.matcher(value).matches();
    }
}
