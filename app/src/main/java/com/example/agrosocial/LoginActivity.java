package com.example.agrosocial;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView registerLink;

    private static final String SUPABASE_URL = "https://hkrjxdaljrognrbapjgf.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhrcmp4ZGFsanJvZ25yYmFwamdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MDMyNTMsImV4cCI6MjA2NDI3OTI1M30.SN7jIxSAORAzXTWSfb9NF8GwiCmynefdFyGd-nY2t4E";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.goToRegisterText);

        loginButton.setOnClickListener(v -> attemptLogin());

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", password);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Ошибка сети: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respStr = response.body().string();

                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(respStr);

                            String accessToken = json.getString("access_token");
                            String refreshToken = json.getString("refresh_token");

                            Log.d("LoginActivity", "access_token: " + accessToken);
                            Log.d("LoginActivity", "refresh_token: " + refreshToken);


                            JSONObject user = json.getJSONObject("user");
                            String userId = user.getString("id");

                            saveAuthData(accessToken, refreshToken, userId);

                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Успешный вход", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            });

                        } catch (JSONException e) {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Ошибка парсинга ответа", Toast.LENGTH_SHORT).show()
                            );
                        }
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Ошибка входа: " + respStr, Toast.LENGTH_LONG).show()
                        );
                    }
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка формирования запроса", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAuthData(String accessToken, String refreshToken, String userId) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putString("user_id", userId)
                .apply();
    }
}

