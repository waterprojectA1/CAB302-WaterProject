package com.wateradvisory.database;
import com.wateradvisory.water.WaterActivityEntry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WaterRecordService {

    public static double getUserTotalWater() {

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/daily_water_records"
                                    + "?user_id=eq." + userId
                                    + "&select=total_water_consumption_day"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {

                System.out.println(
                        "Failed to get user water total: "
                                + response.body()
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
                        .get("total_water_consumption_day")
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

    public static Map<String, Double> getUserWaterSummary() {

        Map<String, Double> summary = new LinkedHashMap<>();

        try {
            String accessToken = UserSession.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/rpc/get_user_water_summary"
                    ))
                    .header(
                            "apikey",
                            SupabaseConfig.SUPABASE_KEY
                    )
                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
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
                        "Failed to get water summary: "
                                + response.body()
                );

                return summary;
            }

            JsonArray result = JsonParser
                    .parseString(response.body())
                    .getAsJsonArray();

            if (result.isEmpty()) {
                return summary;
            }

            JsonObject data =
                    result.get(0).getAsJsonObject();

            summary.put(
                    "today",
                    data.get("today_total").getAsDouble()
            );

            summary.put(
                    "week",
                    data.get("week_total").getAsDouble()
            );

            summary.put(
                    "allTime",
                    data.get("all_time_total").getAsDouble()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return summary;
    }

    public static boolean saveDailyWaterSubmission(List<WaterActivityEntry> activities) {

        if (activities == null || activities.isEmpty()) {
            return false;
        }

        try {
            String accessToken = UserSession.getAccessToken();

            JsonArray activityArray = new JsonArray();

            double totalWater = 0;

            for (WaterActivityEntry entry : activities) {

                JsonObject activityJson = new JsonObject();

                activityJson.addProperty(
                        "activity",
                        entry.getActivity()
                );

                activityJson.addProperty(
                        "duration",
                        entry.getDuration()
                );

                activityJson.addProperty(
                        "amount",
                        entry.getAmount()
                );

                activityJson.addProperty(
                        "water_litres",
                        entry.getLitres()
                );

                activityArray.add(activityJson);

                totalWater += entry.getLitres();
            }

            JsonObject requestBody = new JsonObject();

            requestBody.add(
                    "p_activities",
                    activityArray
            );

            requestBody.addProperty(
                    "p_total",
                    totalWater
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/rpc/save_daily_water_submission"
                    ))
                    .header(
                            "apikey",
                            SupabaseConfig.SUPABASE_KEY
                    )
                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    requestBody.toString()
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Daily water submission response: "
                            + response.body()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<WaterActivityEntry> getTodayActivities() {

        List<WaterActivityEntry> activities = new ArrayList<>();

        try {

            String userId = UserSession.getUserId();
            String accessToken = UserSession.getAccessToken();

            String url =
                    SupabaseConfig.SUPABASE_URL
                            + "/rest/v1/daily_water_records"
                            + "?user_id=eq." + userId
                            + "&record_date=eq." + LocalDate.now()
                            + "&select=activities";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header(
                            "apikey",
                            SupabaseConfig.SUPABASE_KEY
                    )
                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                System.out.println(
                        "Failed to load today's activities: "
                                + response.body()
                );

                return activities;
            }

            JsonArray rows =
                    JsonParser.parseString(
                            response.body()
                    ).getAsJsonArray();

            // No row exists for today yet
            if (rows.isEmpty()) {
                return activities;
            }

            JsonObject todayRow =
                    rows.get(0).getAsJsonObject();

            JsonArray savedActivities =
                    todayRow.getAsJsonArray("activities");

            if (savedActivities == null) {
                return activities;
            }

            for (JsonElement element : savedActivities) {

                JsonObject activity =
                        element.getAsJsonObject();

                String activityName =
                        activity.get("activity").getAsString();

                int duration =
                        activity.get("duration").getAsInt();

                int amount =
                        activity.get("amount").getAsInt();

                double litres =
                        activity.get("water_litres").getAsDouble();

                WaterActivityEntry entry =
                        new WaterActivityEntry(
                                activityName,
                                duration,
                                amount,
                                litres
                        );

                activities.add(entry);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return activities;
    }
}