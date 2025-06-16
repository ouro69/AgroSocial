package com.example.agrosocial.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.agrosocial.adapters.ChatAdapter;
import com.example.agrosocial.models.Chat;
import android.content.Context;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.net.URLEncoder;

import com.example.agrosocial.MessagesActivity;
import android.widget.Toast;
import com.example.agrosocial.R;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();

    private String currentUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);

        if (currentUserId == null) {
            // Пользователь не авторизован, можно обработать ошибку или перейти на экран входа
            Toast.makeText(getContext(), "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chat, container, false);
        recyclerView = root.findViewById(R.id.recyclerViewChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter(getContext(), chatList, chat -> {
            Intent intent = new Intent(getContext(), MessagesActivity.class);
            intent.putExtra("conversation_id", chat.getId());
            intent.putExtra("chat_id", chat.getId());
            intent.putExtra("chat_title", chat.getTitle());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadChat();

        return root;
    }


    private void loadChat() {
        new AsyncTask<Void, Void, List<Chat>>() {
            Exception error = null;

            @Override
            protected List<Chat> doInBackground(Void... voids) {
                try {
                    String encodedUserId = URLEncoder.encode(currentUserId, "UTF-8");

                    String urlStr = "https://hkrjxdaljrognrbapjgf.supabase.co/rest/v1/conversations?" + "select=id,last_message_at,conversation_members!inner(user_id,profiles(username))&" + "conversation_members.user_id=eq." + encodedUserId + "&order=last_message_at.desc.nullslast";

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhrcmp4ZGFsanJvZ25yYmFwamdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MDMyNTMsImV4cCI6MjA2NDI3OTI1M30.SN7jIxSAORAzXTWSfb9NF8GwiCmynefdFyGd-nY2t4E");
                    conn.setRequestProperty("Authorization", "Bearer " + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhrcmp4ZGFsanJvZ25yYmFwamdmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg3MDMyNTMsImV4cCI6MjA2NDI3OTI1M30.SN7jIxSAORAzXTWSfb9NF8GwiCmynefdFyGd-nY2t4E");
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
                        List<Chat> result = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            String id = obj.getString("id");
                            String lastMessageAt = obj.optString("last_message_at", "Нет сообщений");

                            // conversation_members — это массив, надо взять имя того, кто не currentUserId
                            JSONArray members = obj.getJSONArray("conversation_members");
                            String title = "Без имени";
                            for (int j = 0; j < members.length(); j++) {
                                JSONObject member = members.getJSONObject(j);
                                String memberId = member.getString("user_id");
                                if (!memberId.equals(currentUserId)) {
                                    JSONObject profile = member.getJSONObject("profiles");
                                    title = profile.optString("username", "Пользователь");
                                    break;
                                }
                            }

                            result.add(new Chat(id, title, lastMessageAt));
                        }

                        return result;
                    } else {
                        throw new Exception("HTTP error: " + responseCode);
                    }
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Chat> chats) {
                if (error != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    chatList.clear();
                    if (chats != null) chatList.addAll(chats);
                    adapter.notifyDataSetChanged();
                }
            }
        }.execute();
    }
}

