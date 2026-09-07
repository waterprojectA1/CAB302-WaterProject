package com.wateradvisory.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import java.security.SecureRandom;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HouseholdService {

    private static final String JOIN_CODE_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private static String generateJoinCode() {

        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            int index = RANDOM.nextInt(JOIN_CODE_CHARACTERS.length());

            code.append(
                    JOIN_CODE_CHARACTERS.charAt(index)
            );
        }

        return code.toString();
    }

    public static String getJoinCode() {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            // Get logged-in user's household_id
            HttpRequest accountRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?id=eq."
                                    + userId
                                    + "&select=household_id"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> accountResponse =
                    HttpClient.newHttpClient().send(
                            accountRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray accountResult = JsonParser
                    .parseString(accountResponse.body())
                    .getAsJsonArray();

            if (accountResult.isEmpty()) {
                return "N/A";
            }

            JsonObject account =
                    accountResult.get(0).getAsJsonObject();

            if (account.get("household_id").isJsonNull()) {
                return "N/A";
            }

            String householdId =
                    account.get("household_id").getAsString();

            // Get join code from household
            HttpRequest householdRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/households?id=eq."
                                    + householdId
                                    + "&select=join_code"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> householdResponse =
                    HttpClient.newHttpClient().send(
                            householdRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray result = JsonParser
                    .parseString(householdResponse.body())
                    .getAsJsonArray();

            if (!result.isEmpty()) {

                JsonObject household =
                        result.get(0).getAsJsonObject();

                if (!household.get("join_code").isJsonNull()) {
                    return household
                            .get("join_code")
                            .getAsString();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "N/A";
    }

    public static boolean joinHousehold(String joinCode) {

        try {
            String accessToken = UserSession.getAccessToken();

            JsonObject json = new JsonObject();
            json.addProperty("p_join_code", joinCode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/rpc/join_household"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    json.toString()
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Join household response: " + response.body()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String createHousehold(String householdName, String address) {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            JsonObject json = new JsonObject();
            String joinCode = generateJoinCode();

            json.addProperty("household_name", householdName);
            json.addProperty("household_size", 1);
            json.addProperty("created_by", userId);
            json.addProperty("join_code", joinCode);

            if (!address.isBlank()) {
                json.addProperty("household_address", address);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/households"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    json.toString()
                            )
                    )
                    .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Household response: " + response.body());

            if (response.statusCode() >= 200
                    && response.statusCode() < 300) {

                JsonArray result = JsonParser.parseString(response.body()).getAsJsonArray();

                if (!result.isEmpty()) {
                    return result.get(0).getAsJsonObject().get("id").getAsString();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean linkUserToHousehold(String householdId) {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            JsonObject json = new JsonObject();
            json.addProperty("household_id", householdId);
            json.addProperty("setup_done", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?id=eq."
                                    + userId
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .method(
                            "PATCH",
                            HttpRequest.BodyPublishers.ofString(
                                    json.toString()
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Link household response: " + response.body()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getHouseholdName() {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            // First get the user's household_id
            HttpRequest accountRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?id=eq."
                                    + userId
                                    + "&select=household_id"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> accountResponse =
                    HttpClient.newHttpClient().send(
                            accountRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray accountResult = JsonParser
                    .parseString(accountResponse.body())
                    .getAsJsonArray();

            if (accountResult.isEmpty()) {
                return "No Household";
            }

            JsonObject account = accountResult
                    .get(0)
                    .getAsJsonObject();

            if (account.get("household_id").isJsonNull()) {
                return "No Household";
            }

            String householdId = account
                    .get("household_id")
                    .getAsString();

            // Now get the household name
            HttpRequest householdRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/households?id=eq."
                                    + householdId
                                    + "&select=household_name"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> householdResponse =
                    HttpClient.newHttpClient().send(
                            householdRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray householdResult = JsonParser
                    .parseString(householdResponse.body())
                    .getAsJsonArray();

            if (!householdResult.isEmpty()) {
                return householdResult
                        .get(0)
                        .getAsJsonObject()
                        .get("household_name")
                        .getAsString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "No Household";
    }

    public static boolean changeHouseholdName(String newName) {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            // Get household_id from the logged-in account
            HttpRequest accountRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?id=eq."
                                    + userId
                                    + "&select=household_id"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> accountResponse =
                    HttpClient.newHttpClient().send(
                            accountRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray accountResult = JsonParser
                    .parseString(accountResponse.body())
                    .getAsJsonArray();

            if (accountResult.isEmpty()) {
                return false;
            }

            JsonObject account =
                    accountResult.get(0).getAsJsonObject();

            if (account.get("household_id").isJsonNull()) {
                return false;
            }

            String householdId =
                    account.get("household_id").getAsString();

            // Update household name
            JsonObject json = new JsonObject();
            json.addProperty("household_name", newName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/households?id=eq."
                                    + householdId
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .method(
                            "PATCH",
                            HttpRequest.BodyPublishers.ofString(
                                    json.toString()
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Rename household response: " + response.body()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> getHouseholdMembers() {

        List<String> members = new ArrayList<>();

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            // Get current user's household ID
            HttpRequest accountRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?id=eq."
                                    + userId
                                    + "&select=household_id"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> accountResponse =
                    HttpClient.newHttpClient().send(
                            accountRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray accountResult = JsonParser
                    .parseString(accountResponse.body())
                    .getAsJsonArray();

            if (accountResult.isEmpty()) {
                return members;
            }

            JsonObject account =
                    accountResult.get(0).getAsJsonObject();

            if (account.get("household_id").isJsonNull()) {
                return members;
            }

            String householdId =
                    account.get("household_id").getAsString();

            // Get all accounts in that household
            HttpRequest membersRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?household_id=eq."
                                    + householdId
                                    + "&select=username"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> membersResponse =
                    HttpClient.newHttpClient().send(
                            membersRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray result = JsonParser
                    .parseString(membersResponse.body())
                    .getAsJsonArray();

            for (int i = 0; i < result.size(); i++) {

                String username = result
                        .get(i)
                        .getAsJsonObject()
                        .get("username")
                        .getAsString();

                members.add(username);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return members;
    }

    public static String getHouseholdOwner() {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            // Get user's household ID
            HttpRequest accountRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?id=eq."
                                    + userId
                                    + "&select=household_id"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> accountResponse =
                    HttpClient.newHttpClient().send(
                            accountRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray accountResult = JsonParser
                    .parseString(accountResponse.body())
                    .getAsJsonArray();

            if (accountResult.isEmpty()) {
                return "Unknown";
            }

            JsonObject account =
                    accountResult.get(0).getAsJsonObject();

            if (account.get("household_id").isJsonNull()) {
                return "Unknown";
            }

            String householdId =
                    account.get("household_id").getAsString();

            // Get household creator ID
            HttpRequest householdRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/households?id=eq."
                                    + householdId
                                    + "&select=created_by"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> householdResponse =
                    HttpClient.newHttpClient().send(
                            householdRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray householdResult = JsonParser
                    .parseString(householdResponse.body())
                    .getAsJsonArray();

            if (householdResult.isEmpty()) {
                return "Unknown";
            }

            String ownerId = householdResult
                    .get(0)
                    .getAsJsonObject()
                    .get("created_by")
                    .getAsString();

            // Get owner's username
            HttpRequest ownerRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?id=eq."
                                    + ownerId
                                    + "&select=username"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> ownerResponse =
                    HttpClient.newHttpClient().send(
                            ownerRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray ownerResult = JsonParser
                    .parseString(ownerResponse.body())
                    .getAsJsonArray();

            if (!ownerResult.isEmpty()) {
                return ownerResult
                        .get(0)
                        .getAsJsonObject()
                        .get("username")
                        .getAsString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unknown";
    }

    public static boolean hasHousehold() {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?id=eq."
                                    + userId
                                    + "&select=household_id"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray result = JsonParser
                    .parseString(response.body())
                    .getAsJsonArray();

            if (result.isEmpty()) {
                return false;
            }

            JsonObject account =
                    result.get(0).getAsJsonObject();

            return !account.get("household_id").isJsonNull();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean removeHouseholdMember(String username) {

        try {
            String accessToken = UserSession.getAccessToken();

            JsonObject json = new JsonObject();
            json.addProperty("p_member_username", username);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/rpc/remove_household_member"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    json.toString()
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Remove member response: " + response.body()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean leaveHousehold() {

        try {
            String accessToken = UserSession.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/rpc/leave_household"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Leave household response: " + response.body()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isCurrentUserOwner() {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/households?created_by=eq."
                                    + userId
                                    + "&select=id"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray result = JsonParser
                    .parseString(response.body())
                    .getAsJsonArray();

            return !result.isEmpty();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteHousehold() {

        try {
            String accessToken = UserSession.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/rpc/delete_household"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Delete household response: " + response.body()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}