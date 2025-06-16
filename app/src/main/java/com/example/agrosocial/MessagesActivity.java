package com.example.agrosocial;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrosocial.TextDrawable.library.src.main.java.com.amulyakhare.textdrawable.TextDrawable;
import com.example.agrosocial.TextDrawable.library.src.main.java.com.amulyakhare.textdrawable.util.ColorGenerator;
import com.example.agrosocial.adapters.MessageAdapter;
import com.example.agrosocial.models.Message;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    private String conversationId;
    private String currentUserId;
    private String accessToken;
    private String refreshToken;

    private RecyclerView recyclerView;
    private EditText editMessage;
    private ImageButton buttonSend;
    private ImageView avatarImage;
    private TextView chatUsername;

    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String SUPABASE_URL = "https://hkrjxdaljrognrbapjgf.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhrcmp4ZGFsanJvZ25yYmFwamdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MDMyNTMsImV4cCI6MjA2NDI3OTI1M30.SN7jIxSAORAzXTWSfb9NF8GwiCmynefdFyGd-nY2t4E"; // ← Вставь свой ключ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        conversationId = getIntent().getStringExtra("chat_id");

        if (conversationId == null) {
            Toast.makeText(this, "Ошибка: отсутствует chat_id", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentUserId = prefs.getString("user_id", null);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);

        recyclerView = findViewById(R.id.recyclerViewMessages);
        editMessage = findViewById(R.id.editMessage);
        buttonSend = findViewById(R.id.buttonSend);
        avatarImage = findViewById(R.id.avatarImage);
        chatUsername = findViewById(R.id.chatUsername);

        adapter = new MessageAdapter(messages, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchAndSetChatPartnerInfo();
        loadMessages();

        buttonSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                editMessage.setText("");
            }
        });
    }

    private void loadMessages() {
        new Thread(() -> {
            List<Message> loadedMessages = new ArrayList<>();
            try {
                String fullUrl = SUPABASE_URL + "/rest/v1/messages?conversation_id=eq." + conversationId + "&order=created_at.asc";
                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", API_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    conn.disconnect();

                    JSONArray arr = new JSONArray(response.toString());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        loadedMessages.add(new Message(
                                obj.getString("id"),
                                obj.getString("sender_id"),
                                obj.getString("content"),
                                obj.getString("created_at")
                        ));
                    }

                    handler.post(() -> {
                        messages.clear();
                        messages.addAll(loadedMessages);
                        adapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messages.size() - 1);
                    });
                } else if (conn.getResponseCode() == 401) {
                    handler.post(() -> refreshAccessToken(this::loadMessages));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessage(String text) {
        new Thread(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/messages");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", API_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("conversation_id", conversationId);
                json.put("sender_id", currentUserId);
                json.put("content", text);

                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(json.toString());
                writer.flush();
                writer.close();

                if (conn.getResponseCode() == 201 || conn.getResponseCode() == 200) {
                    handler.post(this::loadMessages);
                } else if (conn.getResponseCode() == 401) {
                    handler.post(() -> refreshAccessToken(() -> sendMessage(text)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchAndSetChatPartnerInfo() {
        new Thread(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/conversation_members?select=user_id&conversation_id=eq." + conversationId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", API_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    conn.disconnect();

                    JSONArray arr = new JSONArray(response.toString());
                    for (int i = 0; i < arr.length(); i++) {
                        String id = arr.getJSONObject(i).getString("user_id");
                        if (!id.equals(currentUserId)) {
                            JSONObject userObj = fetchUserById(id);
                            if (userObj != null) {
                                String username = userObj.getString("username");

                                handler.post(() -> {
                                    chatUsername.setText(username);
                                    String firstLetter = username != null && !username.isEmpty()
                                            ? username.substring(0, 1).toUpperCase()
                                            : "?";
                                    ColorGenerator generator = ColorGenerator.MATERIAL;
                                    int color = generator.getColor(username);
                                    TextDrawable drawable = TextDrawable.builder().buildRound(firstLetter, color);
                                    avatarImage.setImageDrawable(drawable);
                                });
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private JSONObject fetchUserById(String userId) {
        try {
            URL url = new URL(SUPABASE_URL + "/rest/v1/profiles?select=username&id=eq." + userId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                conn.disconnect();

                JSONArray arr = new JSONArray(response.toString());
                if (arr.length() > 0) {
                    return arr.getJSONObject(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void refreshAccessToken(Runnable onSuccess) {
        new Thread(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("refresh_token", refreshToken);

                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(json.toString());
                writer.flush();
                writer.close();

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject res = new JSONObject(response.toString());
                    accessToken = res.getString("access_token");

                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("access_token", accessToken)
                            .apply();

                    handler.post(onSuccess);
                } else {
                    handler.post(() -> Toast.makeText(MessagesActivity.this, "Ошибка авторизации. Войдите снова.", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(MessagesActivity.this, "Ошибка авторизации. Войдите снова.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
