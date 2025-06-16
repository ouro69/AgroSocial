package com.example.agrosocial.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrosocial.R;
import com.example.agrosocial.adapters.PostAdapter;
import com.example.agrosocial.models.Post;
import com.example.agrosocial.data.SupabaseClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FeedFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(getContext(), postList);
        recyclerView.setAdapter(postAdapter);

        loadPosts();

        FloatingActionButton fab = view.findViewById(R.id.addPostButton);
        fab.setOnClickListener(v -> showCreatePostDialog());

        return view;
    }

    private void loadPosts() {
        SupabaseClient.fetchPosts(getContext(), new SupabaseClient.PostListCallback() {
            @Override
            public void onSuccess(List<Post> posts) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    postList.clear();
                    postList.addAll(posts);
                    postAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Ошибка загрузки постов", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showCreatePostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Новый пост");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("Введите текст поста");
        builder.setView(input);

        builder.setPositiveButton("Опубликовать", (dialog, which) -> {
            String content = input.getText().toString().trim();
            if (!content.isEmpty()) {
                SupabaseClient.createPost(requireContext(), content, new SupabaseClient.CreatePostCallback() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Пост опубликован", Toast.LENGTH_SHORT).show();
                            loadPosts();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            } else {
                Toast.makeText(getContext(), "Пост не может быть пустым", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}