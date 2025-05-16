package org.ed.track.model;

public class ChatListItem {
    private String chatId;
    private String otherUserId;
    private String name;
    private String imageUrl;
    private String lastMessage;

    public ChatListItem(String chatId, String otherUserId, String name, String imageUrl, String lastMessage) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.lastMessage = lastMessage;
    }

    // Getters and setters
    public String getChatId() { return chatId; }
    public String getOtherUserId() { return otherUserId; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public String getLastMessage() { return lastMessage; }

    public void setName(String name) { this.name = name; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}


