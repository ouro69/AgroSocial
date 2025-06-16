package com.example.agrosocial.adapters;

import com.example.agrosocial.R;
import com.example.agrosocial.models.Chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    private Context context;
    private List<Chat> chatList;
    private OnChatClickListener listener;

    public ChatAdapter(Context context, List<Chat> chatList, OnChatClickListener listener) {
        this.context = context;
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat conv = chatList.get(position);
        holder.textTitle.setText(conv.getTitle());
        holder.textLastMessageAt.setText(conv.getLastMessageAt());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(conv);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textLastMessageAt;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textLastMessageAt = itemView.findViewById(R.id.textLastMessageAt);
        }
    }
}
