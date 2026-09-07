package com.wateradvisory.database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

public class AuthService {

    public static boolean login(String email, String password) {

        try {
            String json = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL +
                                    "/auth/v1/token?grant_type=password"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(response.body());

            if (response.statusCode() == 200) {

                JsonObject responseJson =
                        JsonParser.parseString(response.body())
                                .getAsJsonObject();

                String accessToken =
                        responseJson.get("access_token").getAsString();

                String userId =
                        responseJson.getAsJsonObject("user")
                                .get("id")
                                .getAsString();

                UserSession.setAccessToken(accessToken);
                UserSession.setUserId(userId);

                System.out.println("Logged in user: " + userId);

                return true;
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void logout() {

        String accessToken = UserSession.getAccessToken();

        try {
            if (accessToken != null) {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                SupabaseConfig.SUPABASE_URL + "/auth/v1/logout"
                        ))
                        .header("apikey", SupabaseConfig.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpClient client = HttpClient.newHttpClient();

                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            UserSession.clear();
            System.out.println("Logged out successful!");
        }
    }

    public static boolean register(String email,String username, String password) {

        try {
            String json = """
                    {
                        "email": "%s",
                        "password": "%s",
                        "data": {
                            "username": "%s"
                        }
                    }
                    """.formatted(email, password, username);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL + "/auth/v1/signup"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(response.body());

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isSetupComplete() {

        try {
            String userId = UserSession.getUserId();
            String accessToken = UserSession.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL +
                                    "/rest/v1/accounts?id=eq." +
                                    userId +
                                    "&select=setup_done"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() == 200) {

                JsonArray result =
                        JsonParser.parseString(response.body())
                                .getAsJsonArray();

                if (!result.isEmpty()) {
                    return result.get(0)
                            .getAsJsonObject()
                            .get("setup_done")
                            .getAsBoolean();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void markSetupComplete() throws IOException, InterruptedException {

        String userId = UserSession.getUserId();
        String accessToken = UserSession.getAccessToken();

        String url = SupabaseConfig.SUPABASE_URL
                + "/rest/v1/accounts?id=eq."
                + userId;

        String jsonBody = """
            {
                "setup_done": true
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SupabaseConfig.SUPABASE_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Failed to update setup status: " + response.body()
            );
        }
    }

    public static String getUsername() {

        try {
            String userId = UserSession.getUserId();
            String accessToken = UserSession.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            SupabaseConfig.SUPABASE_URL
                                    + "/rest/v1/accounts?id=eq."
                                    + userId
                                    + "&select=username"
                    ))
                    .header("apikey", SupabaseConfig.SUPABASE_KEY)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {

                JsonArray result = JsonParser
                        .parseString(response.body())
                        .getAsJsonArray();

                if (!result.isEmpty()) {
                    return result.get(0)
                            .getAsJsonObject()
                            .get("username")
                            .getAsString();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "User";
    }
}