package com.wateradvisory.database;
import com.wateradvisory.water.WaterActivityEntry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;

public class WaterRecordService {

    public static boolean saveWaterRecord(WaterActivityEntry entry) {

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

            String householdId = null;

            if (!accountResult.isEmpty()) {

                JsonObject account =
                        accountResult.get(0).getAsJsonObject();

                if (!account.get("household_id").isJsonNull()) {
                    householdId =
                            account.get("household_id").getAsString();
                }
            }

            // Build water record
            JsonObject json = new JsonObject();

            json.addProperty("user_id", userId);
            json.addProperty("activity", entry.getActivity());
            json.addProperty(
                    "duration_minutes",
                    entry.getDuration()
            );
            json.addProperty("amount", entry.getAmount());
            json.addProperty(
                    "water_litres",
                    entry.getLitres()
            );

            if (householdId != null) {
                json.addProperty("household_id", householdId);
            }

            // Send to Supabase
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/water_records"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )
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
                    "Water record response: " + response.body()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static double getUserTotalWater() {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/water_records"
                                    + "?user_id=eq." + userId
                                    + "&select=water_litres"
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

            if (response.statusCode() != 200) {
                System.out.println(
                        "Failed to get water total: " + response.body()
                );
                return 0;
            }

            JsonArray records = JsonParser
                    .parseString(response.body())
                    .getAsJsonArray();

            double total = 0;

            for (int i = 0; i < records.size(); i++) {

                total += records
                        .get(i)
                        .getAsJsonObject()
                        .get("water_litres")
                        .getAsDouble();
            }

            return total;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static double getHouseholdTotalWater() {

        try {
            String accessToken = UserSession.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/rpc/get_household_water_total"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )
                    .header("Content-Type", "application/json")
                    .POST(
                            HttpRequest.BodyPublishers.ofString("{}")
                    )
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                System.out.println(
                        "Failed household water total: "
                                + response.body()
                );

                return 0;
            }

            return JsonParser
                    .parseString(response.body())
                    .getAsDouble();

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static Map<String, Double> getHouseholdMemberTotals() {

        Map<String, Double> memberTotals = new LinkedHashMap<>();

        try {
            String accessToken = UserSession.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/rpc/get_household_member_totals"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                System.out.println(
                        "Failed to get member totals: "
                                + response.body()
                );

                return memberTotals;
            }

            JsonArray result = JsonParser
                    .parseString(response.body())
                    .getAsJsonArray();

            for (int i = 0; i < result.size(); i++) {

                JsonObject member =
                        result.get(i).getAsJsonObject();

                String username =
                        member.get("username").getAsString();

                double total =
                        member.get("total_water").getAsDouble();

                memberTotals.put(username, total);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return memberTotals;
    }
}