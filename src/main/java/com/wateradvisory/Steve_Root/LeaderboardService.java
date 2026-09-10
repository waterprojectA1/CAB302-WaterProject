package com.wateradvisory.Steve_Root;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wateradvisory.database.SupabaseConfig;
import com.wateradvisory.database.UserSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LeaderboardService {

    public static List<LeaderboardEntry> getHouseholdLeaderboard() {

        List<LeaderboardEntry> entries = new ArrayList<>();

        try {
            String accessToken = UserSession.getAccessToken();
            String userId = UserSession.getUserId();

            if (accessToken == null || userId == null) {
                return entries;
            }

            String householdId = getCurrentHouseholdId(accessToken, userId);

            if (householdId == null) {
                return entries;
            }

            JsonArray householdMembers = getHouseholdMembers(accessToken, householdId);
            JsonArray pointRecords = getPointRecords(accessToken, getCurrentSeason());

            for (JsonElement memberElement : householdMembers) {
                JsonObject member = memberElement.getAsJsonObject();

                String memberId = member.get("id").getAsString();
                String username = member.get("username").getAsString();
                int points = 0;

                for (JsonElement pointElement : pointRecords) {
                    JsonObject pointRecord = pointElement.getAsJsonObject();

                    String pointUserId = pointRecord.get("user_id").getAsString();

                    if (memberId.equals(pointUserId)) {
                        points = pointRecord.get("points").getAsInt();
                        break;
                    }
                }

                entries.add(new LeaderboardEntry(memberId, username, points));
            }

            Collections.sort(entries, new Comparator<LeaderboardEntry>() {
                @Override
                public int compare(LeaderboardEntry first, LeaderboardEntry second) {
                    return Integer.compare(second.getPoints(), first.getPoints());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        return entries;
    }

    private static String getCurrentHouseholdId(String accessToken, String userId) throws Exception {

        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/accounts?id=eq."
                + userId
                + "&select=household_id";

        JsonArray result = sendGetRequest(url, accessToken);

        if (result.isEmpty()) {
            return null;
        }

        JsonObject account = result.get(0).getAsJsonObject();

        if (account.get("household_id").isJsonNull()) {
            return null;
        }

        return account.get("household_id").getAsString();
    }

    private static JsonArray getHouseholdMembers(String accessToken, String householdId) throws Exception {

        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/accounts?household_id=eq."
                + householdId
                + "&select=id,username&order=username.asc";

        return sendGetRequest(url, accessToken);
    }

    private static JsonArray getPointRecords(String accessToken, String season) throws Exception {

        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/user_points?season=eq."
                + season
                + "&select=user_id,points";

        return sendGetRequest(url, accessToken);
    }

    private static JsonArray sendGetRequest(String url, String accessToken) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SupabaseConfig.SUPABASE_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Leaderboard request failed: " + response.body());
        }

        return JsonParser.parseString(response.body()).getAsJsonArray();
    }

    public static String getCurrentSeason() {

        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        String season;

        if (month == 12 || month == 1 || month == 2) {
            season = "summer";
        } else if (month >= 3 && month <= 5) {
            season = "autumn";
        } else if (month >= 6 && month <= 8) {
            season = "winter";
        } else {
            season = "spring";
        }

        return season + "-" + today.getYear();
    }
}
