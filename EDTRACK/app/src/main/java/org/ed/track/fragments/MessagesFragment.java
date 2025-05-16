package org.ed.track.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.ed.track.chat.ChatActivity;
import org.ed.track.R;
import org.ed.track.adapter.ChatListAdapter;
import org.ed.track.databinding.FragmentMessagesBinding;
import org.ed.track.model.ChatListItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment {

    private FragmentMessagesBinding binding;

    private ChatListAdapter adapter;
    private List<ChatListItem> chatList;

    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMessagesBinding.bind(inflater.inflate(R.layout.fragment_messages, container, false));

        binding.chatListRv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize list and adapter
        chatList = new ArrayList<>();
        adapter = new ChatListAdapter(chatList, getContext(), item -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("receiverId", item.getOtherUserId()); // This is the actual user you're chatting with
            startActivity(intent);
        });

        binding.chatListRv.setAdapter(adapter);

        return binding.getRoot();
    }

    private void getChatList() {

        // Map to store the latest message per chat partner
        Map<String, ChatListItem> latestChatsMap = new HashMap<>();

        db.collection("chats")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        String receiverId = doc.getString("receiverId");
                        String message = doc.getString("message");
                        long timestamp = doc.getLong("timestamp");

                        if (senderId == null || receiverId == null) continue;

                        // Check if current user is involved
                        if (!senderId.equals(currentUserId) && !receiverId.equals(currentUserId)) {
                            continue;
                        }

                        // Get the ID of the other user
                        String otherUserId = senderId.equals(currentUserId) ? receiverId : senderId;

                        // Only add the latest message (map ensures one per user)
                        if (!latestChatsMap.containsKey(otherUserId)) {
                            ChatListItem item = new ChatListItem(
                                    doc.getId(), // chatId can be the message ID or you can build one using both user IDs
                                    otherUserId,
                                    "", // name (we'll fetch it next)
                                    "", // imageUrl (we'll fetch it next)
                                    message
                            );
                            latestChatsMap.put(otherUserId, item);
                        }
                    }

                    // Now fetch user data for each other user
                    for (ChatListItem item : latestChatsMap.values()) {
                        db.collection("users").document(item.getOtherUserId())
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    item.setName(userDoc.getString("name"));
                                    item.setImageUrl(userDoc.getString("imageUrl"));

                                    chatList.add(item);
                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }

    private void getChatList(String currentUserId) {
        chatList.clear();
        FirebaseFirestore.getInstance()
                .collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        String chatId = doc.getId();
                        List<String> participants = (List<String>) doc.get("participants");

                        if (participants == null || participants.size() != 2) continue;

                        // Find the other participant
                        String otherUserId = participants.get(0).equals(currentUserId) ? participants.get(1) : participants.get(0);

                        String lastMessage = doc.getString("lastMessage");

                        // Fetch other user details
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(otherUserId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    String imageUrl = userDoc.getString("imageUrl");

                                    ChatListItem item = new ChatListItem(
                                            chatId,
                                            otherUserId,
                                            name,
                                            imageUrl,
                                            lastMessage
                                    );

                                    chatList.add(item);
                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            Log.e("TAG", "getChatList: " + firebaseUser.getUid());

            currentUserId = firebaseUser.getUid();
            db = FirebaseFirestore.getInstance();
            getChatList(currentUserId);
        }
    }
}