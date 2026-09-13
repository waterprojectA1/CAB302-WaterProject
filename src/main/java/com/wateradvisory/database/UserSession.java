// Summary: Stores the currently logged-in user's session information,
// including their access token and user ID.
package com.wateradvisory.database;

public class UserSession {

    // Stores the authentication details for the current logged-in user.
    private static String accessToken;
    private static String userId;

    // Returns the current user's access token.
    public static String getAccessToken() {
        return accessToken;
    }

    // Saves the current user's access token after login.
    public static void setAccessToken(String token) {
        accessToken = token;
    }

    // Returns the ID of the currently logged-in user.
    public static String getUserId() {
        return userId;
    }

    // Saves the current user's ID after login.
    public static void setUserId(String id) {
        userId = id;
    }

    // Clears the stored session information when the user logs out.
    public static void clear() {
        accessToken = null;
        userId = null;
    }
}