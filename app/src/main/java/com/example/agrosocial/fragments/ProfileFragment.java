package com.example.agrosocial.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.agrosocial.LoginActivity;
import com.example.agrosocial.R;
import com.example.agrosocial.TextDrawable.library.src.main.java.com.amulyakhare.textdrawable.TextDrawable;
import com.example.agrosocial.TextDrawable.library.src.main.java.com.amulyakhare.textdrawable.util.ColorGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileFragment extends Fragment {

    private static final String SUPABASE_URL = "https://hkrjxdaljrognrbapjgf.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhrcmp4ZGFsanJvZ25yYmFwamdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MDMyNTMsImV4cCI6MjA2NDI3OTI1M30.SN7jIxSAORAzXTWSfb9NF8GwiCmynefdFyGd-nY2t4E";

    private TextView emailText;
    private TextView nicknameText;
    private ImageView avatarImage;
    private Button logoutButton;

    private OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        emailText = view.findViewById(R.id.emailText);
        nicknameText = view.findViewById(R.id.nicknameText);
        avatarImage = view.findViewById(R.id.avatarImage);
        logoutButton = view.findViewById(R.id.logoutButton);

        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", null);
        String userId = prefs.getString("user_id", null);

        if (accessToken == null || userId == null) {
            Toast.makeText(getContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            // Можно отправить на LoginActivity
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return view;
        }

        loadUserProfile(accessToken, userId);

        logoutButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void loadUserProfile(String accessToken, String userId) {
        String url = SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Ошибка загрузки профиля: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();

                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        if (jsonArray.length() > 0) {
                            JSONObject userJson = jsonArray.getJSONObject(0);
                            String email = userJson.optString("email", "неизвестно");
                            String username = userJson.optString("username", "пользователь");

                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    emailText.setText("Email: " + email);
                                    nicknameText.setText("Ник: " + username);
                                });


                                if (username.length() > 0) {
                                    String firstLetter = username.substring(0, 1).toUpperCase();
                                    ColorGenerator generator = ColorGenerator.MATERIAL;
                                    int color = generator.getColor(username);
                                    TextDrawable drawable = TextDrawable.builder()
                                            .buildRound(firstLetter, color);
                                    avatarImage.setImageDrawable(drawable);
                                }
                            };
                        } else {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Профиль не найден", Toast.LENGTH_LONG).show()
                            );
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Ошибка разбора ответа", Toast.LENGTH_LONG).show()
                        );
                    }
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Ошибка сервера: " + response.message(), Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }
}