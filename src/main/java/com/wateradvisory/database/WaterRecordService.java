package com.wateradvisory.database;
import com.wateradvisory.water.ActivityEntry;
import com.wateradvisory.water.DailyWaterRecord;
import com.wateradvisory.water.WaterActivityEntry;
import com.wateradvisory.water.WaterUsageEntry;

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
import java.util.UUID;

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

    // =====================================================================
    //  daily_water_records reads for the Conservation Tips feature.
    //
    //  The real table is `daily_water_records` (NOT "water_records"): ONE ROW
    //  PER USER PER DAY, with the day's activities embedded as a jsonb array
    //  and the day total already precomputed in `total_water_consumption_day`.
    //  Same connection / auth pattern as getUserTotalWater() above -- plain
    //  HttpClient + Gson, apikey header + Bearer <session access token>.
    //  All of these return an empty list / null on any error or when there is
    //  no logged-in session (e.g. the isolated charlie-preview run), so callers
    //  can treat "no data" as "fall back", never as a crash.
    // =====================================================================

    /**
     * Every {@code daily_water_records} row for {@code userId} whose
     * {@code record_date} falls in {@code [from, to]} (inclusive), oldest first.
     *
     * <p>{@code record_date} makes real day-over-day / week-over-week /
     * month-over-month comparison directly possible. What is still NOT possible
     * is time-of-day analysis: {@code record_date} is date-only, and the
     * embedded activity objects carry no timestamp either.</p>
     */
    public static List<DailyWaterRecord> getUserDailyRecords(UUID userId, LocalDate from, LocalDate to) {
        if (userId == null || from == null || to == null) {
            return new ArrayList<>();
        }
        return fetchDailyRecords("user_id=eq." + userId, from, to);
    }

    /**
     * One {@link WaterUsageEntry} ({date, litres}) per day the current session
     * user has a record for, within {@code [from, to]}. Backs Jainya's
     * Daily / Seasonal / Compare graph screens; a thin projection of
     * {@link #getUserDailyRecords} onto just the day total.
     */
    public static List<WaterUsageEntry> getDailyUsageEntries(LocalDate from, LocalDate to) {
        List<WaterUsageEntry> out = new ArrayList<>();
        String sessionUserId = UserSession.getUserId();
        if (sessionUserId == null || from == null || to == null) {
            return out;
        }
        for (DailyWaterRecord record : fetchDailyRecords("user_id=eq." + sessionUserId, from, to)) {
            out.add(new WaterUsageEntry(
                    record.getRecordDate(),
                    (int) Math.round(record.getTotalWaterConsumptionDay())
            ));
        }
        return out;
    }

    /**
     * {@code household_size} from the {@code households} table, or {@code null}
     * if {@code householdId} is null / unknown / unreadable. Callers must treat
     * a null return as "no household-size context available" and fall back to a
     * generic comparison rather than assuming a size.
     */
    public static Integer getHouseholdSize(UUID householdId) {
        if (householdId == null) {
            return null;
        }
        try {
            String accessToken = UserSession.getAccessToken();
            if (accessToken == null) {
                return null;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/households"
                                    + "?id=eq." + householdId
                                    + "&select=household_size"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Failed to get household size: " + response.body());
                return null;
            }

            JsonArray rows = JsonParser.parseString(response.body()).getAsJsonArray();
            if (rows.isEmpty()) {
                return null;
            }
            JsonElement size = rows.get(0).getAsJsonObject().get("household_size");
            if (size == null || size.isJsonNull()) {
                return null;
            }
            int value = size.getAsInt();
            return value > 0 ? value : null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Shared GET + parse for {@code daily_water_records}. {@code rowFilter} is a ready PostgREST clause, e.g. "user_id=eq.<uuid>". */
    private static List<DailyWaterRecord> fetchDailyRecords(String rowFilter, LocalDate from, LocalDate to) {
        List<DailyWaterRecord> records = new ArrayList<>();
        try {
            String accessToken = UserSession.getAccessToken();
            if (accessToken == null) {
                return records;   // not signed in (e.g. isolated preview) -> no data
            }

            String url = SupabaseConfig.SUPABASE_URL
                    + "/rest/v1/daily_water_records"
                    + "?" + rowFilter
                    + "&record_date=gte." + from
                    + "&record_date=lte." + to
                    + "&select=record_date,total_water_consumption_day,household_id,activities"
                    + "&order=record_date.asc";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Failed to get daily water records: " + response.body());
                return records;
            }

            JsonArray rows = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement rowElement : rows) {
                DailyWaterRecord parsed = parseDailyRecord(rowElement.getAsJsonObject());
                if (parsed != null) {
                    records.add(parsed);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    }

    /** Parses one {@code daily_water_records} row; returns null if it has no usable {@code record_date}. */
    private static DailyWaterRecord parseDailyRecord(JsonObject row) {
        JsonElement dateEl = row.get("record_date");
        if (dateEl == null || dateEl.isJsonNull()) {
            return null;
        }
        LocalDate recordDate = LocalDate.parse(dateEl.getAsString());

        double total = 0;
        JsonElement totalEl = row.get("total_water_consumption_day");
        if (totalEl != null && !totalEl.isJsonNull()) {
            total = totalEl.getAsDouble();
        }

        UUID householdId = null;
        JsonElement hhEl = row.get("household_id");
        if (hhEl != null && !hhEl.isJsonNull()) {
            try {
                householdId = UUID.fromString(hhEl.getAsString());
            } catch (IllegalArgumentException ignored) {
                householdId = null;
            }
        }

        List<ActivityEntry> activities = new ArrayList<>();
        JsonElement actsEl = row.get("activities");
        if (actsEl != null && actsEl.isJsonArray()) {
            for (JsonElement el : actsEl.getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject a = el.getAsJsonObject();
                activities.add(new ActivityEntry(
                        jsonString(a, "activity", ""),
                        jsonInt(a, "duration", 0),
                        jsonInt(a, "amount", 1),
                        jsonDouble(a, "water_litres", 0)
                ));
            }
        }

        return new DailyWaterRecord(recordDate, total, householdId, activities);
    }

    private static String jsonString(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? fallback : e.getAsString();
    }

    private static int jsonInt(JsonObject o, String key, int fallback) {
        JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? fallback : e.getAsInt();
    }

    private static double jsonDouble(JsonObject o, String key, double fallback) {
        JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? fallback : e.getAsDouble();
    }
}