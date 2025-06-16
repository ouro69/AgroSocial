package com.example.agrosocial.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.example.agrosocial.models.Post;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import android.util.Log;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {

    private static final String SUPABASE_URL = "https://hkrjxdaljrognrbapjgf.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhrcmp4ZGFsanJvZ25yYmFwamdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MDMyNTMsImV4cCI6MjA2NDI3OTI1M30.SN7jIxSAORAzXTWSfb9NF8GwiCmynefdFyGd-nY2t4E";


    // --- Интерфейс обратного вызова для обновления токена ---
    public interface RefreshTokenCallback {
        void onSuccess(String newAccessToken);
        void onFailure(Exception e);
    }

    // --- Интерфейс обратного вызова для получения списка постов ---
    public interface PostListCallback {
        void onSuccess(List<Post> posts);
        void onError(Exception e);
    }

    // --- Интерфейс обратного вызова для создания поста ---
    public interface CreatePostCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // --- Метод сохранения токенов в SharedPreferences ---
    private static void saveTokens(Context context, String accessToken, String refreshToken) {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .apply();
    }

    // --- Метод обновления токена ---
    public static void refreshToken(Context context, RefreshTokenCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String refreshToken = prefs.getString("refresh_token", null);
        if (refreshToken == null) {
            callback.onFailure(new Exception("Refresh token отсутствует"));
            return;
        }

        try {
            String encodedRefreshToken = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8.toString());
            String requestBody = "grant_type=refresh_token&refresh_token=" + encodedRefreshToken;

            RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.get("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/auth/v1/token")
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post(body)
                    .build();

            OkHttpClient client = new OkHttpClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String respStr = response.body().string();
                            JSONObject json = new JSONObject(respStr);
                            String newAccessToken = json.getString("access_token");
                            String newRefreshToken = json.getString("refresh_token");

                            saveTokens(context, newAccessToken, newRefreshToken);

                            callback.onSuccess(newAccessToken);
                        } catch (JSONException e) {
                            callback.onFailure(e);
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "empty";
                        callback.onFailure(new Exception("Не удалось обновить токен: HTTP " + response.code() + " " + response.message() + " Body: " + errorBody));
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    // --- Загрузка постов ---
    public static void fetchPosts(Context context, PostListCallback callback) {
        new AsyncTask<Void, Void, List<Post>>() {
            Exception error = null;
            boolean tokenRefreshed = false;

            @Override
            protected List<Post> doInBackground(Void... voids) {
                while (true) { // Позволит повторить запрос после обновления токена
                    try {
                        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                        String accessToken = prefs.getString("access_token", null);

                        if (accessToken == null) {
                            throw new Exception("Access token not found — пользователь не авторизован");
                        }

                        URL url = new URL(SUPABASE_URL + "/rest/v1/posts?select=*,profiles(username)");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("apikey", SUPABASE_API_KEY);
                        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                        conn.setRequestProperty("Content-Type", "application/json");

                        int responseCode = conn.getResponseCode();

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            reader.close();

                            JSONArray jsonArray = new JSONArray(response.toString());
                            List<Post> posts = new ArrayList<>();

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                JSONObject profiles = obj.optJSONObject("profiles");

                                String username = (profiles != null && profiles.has("username"))
                                        ? profiles.optString("username", "Unknown")
                                        : "Unknown";

                                Post post = new Post(
                                        obj.getString("id"),
                                        obj.getString("content"),
                                        obj.getString("created_at"),
                                        username
                                );
                                posts.add(post);
                            }
                            return posts;

                        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED && !tokenRefreshed) {
                            // 401 - пробуем обновить токен и повторить запрос
                            final Object lock = new Object();
                            final Exception[] refreshError = new Exception[1];
                            final boolean[] refreshSuccess = new boolean[1];

                            refreshSuccess[0] = false;
                            refreshError[0] = null;

                            // Ждём синхронно обновления токена
                            refreshToken(context, new RefreshTokenCallback() {
                                @Override
                                public void onSuccess(String newAccessToken) {
                                    refreshSuccess[0] = true;
                                    synchronized (lock) {
                                        lock.notify();
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    refreshError[0] = e;
                                    synchronized (lock) {
                                        lock.notify();
                                    }
                                }
                            });

                            // Ждём результата обновления
                            synchronized (lock) {
                                try {
                                    lock.wait(10000); // ждём максимум 10 секунд
                                } catch (InterruptedException e) {
                                    refreshError[0] = e;
                                }
                            }

                            if (refreshSuccess[0]) {
                                tokenRefreshed = true; // Обновили токен, повторяем запрос
                                continue;
                            } else {
                                throw new Exception("Не удалось обновить токен", refreshError[0]);
                            }
                        } else {
                            throw new Exception("HTTP error code: " + responseCode);
                        }
                    } catch (Exception e) {
                        error = e;
                        return null;
                    }
                }
            }

            @Override
            protected void onPostExecute(List<Post> posts) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(posts);
                }
            }
        }.execute();
    }

    // --- Создание нового поста ---
    public static void createPost(Context context, String content, CreatePostCallback callback) {
        new AsyncTask<Void, Void, Exception>() {
            boolean tokenRefreshed = false;

            @Override
            protected Exception doInBackground(Void... voids) {
                while (true) {
                    try {
                        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                        String accessToken = prefs.getString("access_token", null);

                        if (accessToken == null) {
                            return new Exception("User is not authenticated");
                        }

                        URL url = new URL(SUPABASE_URL + "/rest/v1/posts");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");

                        conn.setRequestProperty("apikey", SUPABASE_API_KEY);
                        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        JSONObject json = new JSONObject();
                        json.put("content", content);

                        String userId = prefs.getString("user_id", null);
                        if (userId != null) {
                            json.put("user_id", userId);
                        }

                        OutputStream os = conn.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        writer.write(json.toString());
                        writer.flush();
                        writer.close();
                        os.close();

                        int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                            return null; // успех
                        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED && !tokenRefreshed) {
                            // --- Обработка обновления токена ---
                            final Object lock = new Object();
                            final Exception[] refreshError = new Exception[1];
                            final boolean[] refreshSuccess = new boolean[1];

                            refreshSuccess[0] = false;
                            refreshError[0] = null;

                            refreshToken(context, new RefreshTokenCallback() {
                                @Override
                                public void onSuccess(String newAccessToken) {
                                    refreshSuccess[0] = true;
                                    synchronized (lock) {
                                        lock.notify();
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    refreshError[0] = e;
                                    synchronized (lock) {
                                        lock.notify();
                                    }
                                }
                            });

                            synchronized (lock) {
                                try {
                                    lock.wait(10000);
                                } catch (InterruptedException e) {
                                    refreshError[0] = e;
                                }
                            }

                            if (refreshSuccess[0]) {
                                tokenRefreshed = true;
                                continue; // Повторить запрос
                            } else {
                                return new Exception("Не удалось обновить токен", refreshError[0]);
                            }
                        } else {
                            BufferedReader reader;
                            if (conn.getErrorStream() != null) {
                                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                            } else {
                                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            }

                            StringBuilder errorResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                errorResponse.append(line);
                            }
                            reader.close();

                            android.util.Log.e("SupabaseError", "HTTP " + responseCode + ": " + errorResponse.toString());

                            return new Exception("HTTP error: " + responseCode + " " + errorResponse);
                        }

                    } catch (Exception e) {
                        return e;
                    }
                }
            }

            @Override
            protected void onPostExecute(Exception error) {
                if (error == null) {
                    callback.onSuccess();
                } else {
                    callback.onError(error);
                }
            }
        }.execute();
    }

}