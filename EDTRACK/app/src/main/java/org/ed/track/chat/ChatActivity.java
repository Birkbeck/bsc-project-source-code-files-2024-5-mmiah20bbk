package org.ed.track.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.ed.track.adapter.ChatAdapter;
import org.ed.track.callsession.CallActivity;
import org.ed.track.databinding.ActivityChatBinding;
import org.ed.track.model.ChatMessage;
import org.ed.track.payment.StripePaymentActivity;
import org.ed.track.utils.App;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private FirebaseFirestore db;
    private String senderId, receiverId, chatId, callChannelName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        senderId = App.getString("user_id");
        receiverId = getIntent().getStringExtra("receiverId");
        if (receiverId == null) {
            finish();
            return;
        }
        chatId = getChatId(senderId, receiverId);
        callChannelName = "session_" + chatId.hashCode();
        db = FirebaseFirestore.getInstance();

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, senderId);
        binding.recyclerChat.setAdapter(chatAdapter);
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(this));

        Log.e("sender recerive", senderId + ",.,." + receiverId);
        binding.btnSend.setOnClickListener(v -> sendMessage());


        binding.startCall.setVisibility(View.VISIBLE);
        binding.startCall.setOnClickListener(view -> {
            Intent intent = new Intent(App.getContext(), CallActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            App.getContext().startActivity(intent);
        });

        listenForMessages();

        binding.back.setOnClickListener(view -> {
            onBackPressed();
        });

        if (App.getString("role").equals("Student")) {
            binding.pay.setVisibility(View.VISIBLE);

            binding.pay.setOnClickListener(view -> {
                Intent intent = new Intent(App.getContext(), StripePaymentActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("teacher_id", receiverId);
                App.getContext().startActivity(intent);
            });
        } else {
            binding.pay.setVisibility(View.GONE);

        }

    }

    private String getChatId(String id1, String id2) {
        return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    private void sendMessage() {
        String msg = binding.editMessage.getText().toString().trim();
        if (msg.isEmpty()) return;

        ChatMessage message = new ChatMessage(senderId, receiverId, msg, System.currentTimeMillis());

        // 1. Update or create the main chat document
        Map<String, Object> chatMeta = new HashMap<>();
        chatMeta.put("participants", Arrays.asList(senderId, receiverId));
        chatMeta.put("lastMessage", msg);
        chatMeta.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .set(chatMeta);

// 2. Add the message to the messages subcollection
        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message);

        binding.back.setOnClickListener(view -> onBackPressed());

        binding.editMessage.setText("");
    }

    private void listenForMessages() {

        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    chatMessages.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        if (msg != null && (
                                (msg.getSenderId().equals(senderId) && msg.getReceiverId().equals(receiverId)) ||
                                        (msg.getSenderId().equals(receiverId) && msg.getReceiverId().equals(senderId))
                        )) {
                            chatMessages.add(msg);
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    binding.recyclerChat.scrollToPosition(chatMessages.size() - 1);
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        listenForMessages();
    }
}