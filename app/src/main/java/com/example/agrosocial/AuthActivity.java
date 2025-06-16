package com.example.agrosocial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", null);

        if (accessToken != null && !accessToken.isEmpty()) {
            // Если токен есть, сразу идём в MainActivity
            Intent intent = new Intent(AuthActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;  // Важно: после finish() и перехода нельзя делать setContentView и искать кнопки
        }

        // Токена нет — показываем экран с кнопками
        setContentView(R.layout.activity_auth);

        // Найти кнопки по ID, убедись что id совпадает с тем, что в layout
        Button registerButton = findViewById(R.id.registerButton);
        Button loginButton = findViewById(R.id.loginButton);

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(AuthActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(AuthActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}