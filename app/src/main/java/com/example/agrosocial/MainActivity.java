package com.example.agrosocial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://hkrjxdaljrognrbapjgf.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhrcmp4ZGFsanJvZ25yYmFwamdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MDMyNTMsImV4cCI6MjA2NDI3OTI1M30.SN7jIxSAORAzXTWSfb9NF8GwiCmynefdFyGd-nY2t4E";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", null);

        if (accessToken == null || accessToken.isEmpty()) {
            redirectToAuth();
            return;
        }

        validateSessionWithSupabase(accessToken);
    }

    private void validateSessionWithSupabase(String accessToken) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/user")
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка сети: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    redirectToAuth();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        String email = json.getString("email");
                        String userId = json.getString("id");

                        getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                                .putString("email", email)
                                .putString("user_id", userId)
                                .apply();

                        runOnUiThread(() -> setupUI());

                    } catch (JSONException e) {
                        runOnUiThread(() -> redirectToAuth());
                    }
                } else {
                    // Если access token устарел или невалиден — пробуем обновить
                    runOnUiThread(() -> tryRefreshToken());
                }
            }
        });
    }

    private void tryRefreshToken() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String refreshToken = prefs.getString("refresh_token", null);
        if (refreshToken == null) {
            redirectToAuth();
            return;
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("refresh_token", refreshToken);
        } catch (JSONException e) {
            e.printStackTrace();
            redirectToAuth();
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                okhttp3.MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> redirectToAuth());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        String newAccessToken = json.getString("access_token");
                        String newRefreshToken = json.getString("refresh_token");
                        JSONObject user = json.getJSONObject("user");
                        String userId = user.getString("id");

                        SharedPreferences.Editor editor = getSharedPreferences("app_prefs", MODE_PRIVATE).edit();
                        editor.putString("access_token", newAccessToken);
                        editor.putString("refresh_token", newRefreshToken);
                        editor.putString("user_id", userId);
                        editor.apply();

                        // Повторно валидируем с новым токеном
                        validateSessionWithSupabase(newAccessToken);

                    } catch (JSONException e) {
                        runOnUiThread(() -> redirectToAuth());
                    }
                } else {
                    runOnUiThread(() -> redirectToAuth());
                }
            }
        });
    }

    private void setupUI() {
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String nickname = prefs.getString("nickname", null);
        if (nickname != null) {
            Toast.makeText(this, "Добро пожаловать, " + nickname + "!", Toast.LENGTH_SHORT).show();
        }
    }

    private void redirectToAuth() {
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();

        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


