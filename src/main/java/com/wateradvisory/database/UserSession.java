package com.wateradvisory.database;

public class UserSession {

    private static String accessToken;
    private static String userId;

    public static String getAccessToken() {
        return accessToken;
    }

    public static void setAccessToken(String token) {
        accessToken = token;
    }

    public static String getUserId() {
        return userId;
    }

    public static void setUserId(String id) {
        userId = id;
    }

    public static void clear() {
        accessToken = null;
        userId = null;
    }
}