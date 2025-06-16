package com.example.agrosocial.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageView;

import com.example.agrosocial.TextDrawable.library.src.main.java.com.amulyakhare.textdrawable.TextDrawable;
import com.example.agrosocial.TextDrawable.library.src.main.java.com.amulyakhare.textdrawable.util.ColorGenerator;

import com.example.agrosocial.R;
import com.example.agrosocial.models.Post;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.textContent.setText(post.getContent());
        holder.textUser.setText(post.getUsername());
        holder.textTimestamp.setText(post.getTimestamp());
        String username = post.getUsername();
        String firstLetter = username != null && !username.isEmpty()
                ? username.substring(0, 1).toUpperCase()
                : "?";

        // Цвет и аватарка
        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(username);
        TextDrawable drawable = TextDrawable.builder()
                .buildRound(firstLetter, color);

        holder.avatarImage.setImageDrawable(drawable);
        holder.textContent.setText(post.getContent());
        holder.textUser.setText(username);
        holder.textTimestamp.setText(post.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView textContent, textUser, textTimestamp;
        ImageView avatarImage;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            textContent = itemView.findViewById(R.id.textContent);
            textUser = itemView.findViewById(R.id.textUser);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            avatarImage = itemView.findViewById(R.id.avatarImage);
        }
    }
}