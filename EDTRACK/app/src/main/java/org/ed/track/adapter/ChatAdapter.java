package org.ed.track.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.ed.track.R;
import org.ed.track.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messageList;
    private String currentUserId;

    public ChatAdapter(List<ChatMessage> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            Log.e("sender", "onBindViewHolder: " + message);
            holder.sentMsg.setText(message.getMessage());
            holder.sentMsg.setVisibility(View.VISIBLE);
            holder.receivedMsg.setVisibility(View.GONE);
        } else {
            Log.e("receiver", "onBindViewHolder: " + message);

            holder.receivedMsg.setText(message.getMessage());
            holder.receivedMsg.setVisibility(View.VISIBLE);
            holder.sentMsg.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView sentMsg, receivedMsg;

        ChatViewHolder(View itemView) {
            super(itemView);
            sentMsg = itemView.findViewById(R.id.text_message_sent);
            receivedMsg = itemView.findViewById(R.id.text_message_received);
        }
    }
}

