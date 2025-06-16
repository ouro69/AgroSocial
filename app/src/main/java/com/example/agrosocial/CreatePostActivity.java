package com.example.agrosocial;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

public class CreatePostActivity extends AppCompatActivity {

    private EditText contentEditText;
    private Button submitButton;

    private final OkHttpClient client = new OkHttpClient();

    private final String SUPABASE_URL = "https://hkrjxdaljrognrbapjgf.supabase.co";
    private final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhrcmp4ZGFsanJvZ25yYmFwamdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MDMyNTMsImV4cCI6MjA2NDI3OTI1M30.SN7jIxSAORAzXTWSfb9NF8GwiCmynefdFyGd-nY2t4E"; // Лучше вынести в безопасное место

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        contentEditText = findViewById(R.id.contentEditText);
        submitButton = findViewById(R.id.submitPostButton);

        submitButton.setOnClickListener(v -> {
            String content = contentEditText.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Введите текст поста", Toast.LENGTH_SHORT).show();
                return;
            }
            publishPost(content);
        });
    }

    private void publishPost(String content) {
        String userId = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("user_id", null);
        String accessToken = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("access_token", null);

        if (userId == null || accessToken == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("content", content);
            jsonBody.put("user_id", userId);
        } catch (JSONException e) {
            Toast.makeText(this, "Ошибка формирования данных", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/posts")
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, "Ошибка сети: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body() != null ? response.body().string() : "Пустой ответ";
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreatePostActivity.this, "Пост опубликован!", Toast.LENGTH_SHORT).show();
                        contentEditText.setText("");
                        finish();  // Возвращаемся в ленту после успешной публикации
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, "Ошибка публикации: " + respStr, Toast.LENGTH_LONG).show());
                }
            }
        });
    }
}