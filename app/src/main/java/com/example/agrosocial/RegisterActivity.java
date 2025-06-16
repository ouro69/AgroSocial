package com.example.agrosocial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput;
    private Button registerButton;
    private TextView goToLoginText;
    private ProgressBar progressBar;

    private final OkHttpClient client = new OkHttpClient();

    private final String SUPABASE_URL = "https://hkrjxdaljrognrbapjgf.supabase.co";
    private final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhrcmp4ZGFsanJvZ25yYmFwamdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MDMyNTMsImV4cCI6MjA2NDI3OTI1M30.SN7jIxSAORAzXTWSfb9NF8GwiCmynefdFyGd-nY2t4E";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        registerButton = findViewById(R.id.registerButton);
        goToLoginText = findViewById(R.id.goToLoginText);
        progressBar = findViewById(R.id.progressBar);

        registerButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Введите все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            // Показываем индикатор загрузки
            progressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);

            registerUser(username, email, password);
        });

        goToLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser(String username, String email, String password) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);

            JSONObject userData = new JSONObject();
            userData.put("username", username);

            jsonBody.put("data", userData);
        } catch (JSONException e) {
            runOnUiThread(() -> {
                Toast.makeText(RegisterActivity.this, "Ошибка создания JSON", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                registerButton.setEnabled(true);
            });
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/signup")
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    registerButton.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Ошибка сети: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    registerButton.setEnabled(true);
                });

                if (response.isSuccessful()) {
                    String respStr = response.body().string();
                    try {
                        JSONObject json = new JSONObject(respStr);
                        String accessToken = json.getString("access_token");
                        String refreshToken = json.getString("refresh_token");
                        
                        JSONObject user = json.getJSONObject("user");
                        String userId = user.getString("id");
                        String emailFromResponse = user.getString("email"); // email из ответа

                        // Сохраняем в SharedPreferences
                        getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                                .putString("access_token", accessToken)
                                .putString("user_id", userId)
                                .putString("email", emailFromResponse)
                                .putString("nickname", username)  // ник из параметра метода
                                .apply();

                        runOnUiThread(() -> {
                            Toast.makeText(RegisterActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } catch (JSONException e) {
                        runOnUiThread(() ->
                                Toast.makeText(RegisterActivity.this, "Ошибка парсинга ответа", Toast.LENGTH_LONG).show()
                        );
                    }
                } else {
                    String respStr = response.body().string();
                    runOnUiThread(() ->
                            Toast.makeText(RegisterActivity.this, "Ошибка: " + respStr, Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }
}