// Message sending notification functions for Firebase Cloud Functions
// These functions work with your existing Android FCM implementation

const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK if not already initialized
if (admin.apps.length === 0) {
    admin.initializeApp();
}

/**
 * Trigger function that sends a notification when a new chat message is created
 */
exports.onNewMessage = onDocumentCreated({
    document: "chats/{chatId}/messages/{messageId}",
    maxInstances: 20,
}, async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
        console.log("No data associated with the event");
        return null;
    }

    const message = snapshot.data();
    const senderId = message.senderId;
    const receiverId = message.receiverId;
    const messageText = message.msg || message.text || "";

    try {
        // Get the sender's information
        const senderDoc = await admin.firestore()
            .collection("users")
            .doc(senderId)
            .get();

        if (!senderDoc.exists) {
            console.log("Sender not found");
            return null;
        }

        const sender = senderDoc.data();
        const senderName = sender.displayName || sender.name || "User";

        // Get the receiver's token
        const receiverDoc = await admin.firestore()
            .collection("users")
            .doc(receiverId)
            .get();

        if (!receiverDoc.exists) {
            console.log("Receiver not found");
            return null;
        }

        const receiver = receiverDoc.data();
        const receiverToken = receiver.token;

        // If no token, can't send notification
        if (!receiverToken) {
            console.log("Receiver has no FCM token");
            return null;
        }

        // Create and send message - using data payload for compatibility with your service
        const fcmMessage = {
            data: {
                title: `New message from ${senderName}`,
                message: messageText.length > 100 ?
                    messageText.substring(0, 97) + "..." :
                    messageText,
                senderId: senderId,
                chatId: event.params.chatId,
                type: "new_message",
                timestamp: Date.now().toString()
            },
            token: receiverToken
        };

        await admin.messaging().send(fcmMessage);
        console.log(`Sent chat notification to user ${receiverId}`);
        return null;
    } catch (error) {
        console.error("Error in onNewMessage:", error);
        return null;
    }
});

/**
 * Send a direct message notification to a user
 * Call this function from your client code using Firebase Functions SDK
 */
exports.sendChatNotification = onCall({
    maxInstances: 10,
}, async (request) => {
    // Verify the user is authenticated
    if (!request.auth) {
        throw new HttpsError(
            "unauthenticated",
            "The function must be called while authenticated.",
        );
    }

    const {userId, message, chatId} = request.data;
    const senderId = request.auth.uid;

    try {
        // Get the sender's information
        const senderDoc = await admin.firestore()
            .collection("users")
            .doc(senderId)
            .get();

        if (!senderDoc.exists) {
            throw new HttpsError(
                "not-found",
                "Sender not found",
            );
        }

        const sender = senderDoc.data();
        const senderName = sender.displayName || sender.name || "User";

        // Get the receiver's token
        const receiverDoc = await admin.firestore()
            .collection("users")
            .doc(userId)
            .get();

        if (!receiverDoc.exists) {
            throw new HttpsError(
                "not-found",
                "Receiver not found",
            );
        }

        const receiverToken = receiverDoc.data().token;

        if (!receiverToken) {
            throw new HttpsError(
                "failed-precondition",
                "Receiver does not have a registered device token",
            );
        }

        // Create and send message - using data payload format for your service
        const fcmMessage = {
            data: {
                title: "Message from ${senderName}",
                message: message.length > 100 ? message.substring(0, 97) + "..." : message,
                senderId: senderId,
                chatId: chatId,
                type: "new_message",
                timestamp: Date.now().toString()
            },
            token: receiverToken
        };

        const response = await admin.messaging().send(fcmMessage);
        return {success: true, messageId: response};
    } catch (error) {
        console.error("Error sending chat notification:", error);
        throw new HttpsError(
            "internal",
            "Error sending notification",
            error,
        );
    }
});
