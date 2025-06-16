package com.example.agrosocial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int ANIMATION_DURATION = 800; // миллисекунд
    private static final int SPLASH_DELAY = 2000; // задержка до перехода

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logoImage);
        if (logo != null) {
            logo.setScaleX(0f);
            logo.setScaleY(0f);
            logo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIMATION_DURATION)
                    .setStartDelay(300) // чтобы появление не было мгновенным
                    .start();
        }

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String token = prefs.getString("access_token", null);

            Intent intent;
            if (token != null && !token.isEmpty()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, AuthActivity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}