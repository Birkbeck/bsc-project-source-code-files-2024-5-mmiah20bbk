/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onRequest} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

const functions = require("firebase-functions");
const admin = require("firebase-admin");
// const stripe = require("stripe")("sk_test_51RPB4z8Rm8YT1r2QgJef6V9naxa1d7fwdAguL9KoNrvpqKXEdznYE89UHbfeeY4gyGRMEgd6iBfm38pDnrNr0IOS00pJLWqonb");
const stripe = require("stripe")("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");

admin.initializeApp();

exports.createConnectedAccount = functions.https.onCall(async (data, context) => {
    // ✅ Check if user is authenticated
    if (!context.auth) {
        throw new functions.https.HttpsError(
            'unauthenticated',
            'Only authenticated users can connect Stripe accounts.'
        );
    }

    const uid = context.auth.uid;

    const account = await stripe.accounts.create({
        type: "express",
        capabilities: {
            card_payments: { requested: true },
            transfers: { requested: true }
        }
    });

    const link = await stripe.accountLinks.create({
        account: account.id,
        refresh_url: "https://edtrack-96f03.web.app/reauth.html",
        return_url: "https://edtrack-96f03.web.app/success.html",
        type: "account_onboarding"
    });

    await admin.firestore().collection("users").doc(uid).set({
        stripeAccountId: account.id
    }, { merge: true });

    return { url: link.url };
});


// Firebase Cloud Function - improved version

exports.createPaymentIntent = functions.https.onCall(async (data, context) => {
    console.log("Received full data object:", JSON.stringify(data));

    // Extract values with proper validation
    const amount = data?.amount;
    const teacherId = data?.teacherId;

    console.log("Received amount:", amount);
    console.log("Received teacherId:", teacherId);

    if (!amount || typeof amount !== "number") {
        throw new functions.https.HttpsError("invalid-argument", "Missing or invalid amount");
    }

    if (!teacherId || typeof teacherId !== "string") {
        throw new functions.https.HttpsError("invalid-argument", "Missing or invalid teacherId");
    }

    try {
        const teacherDoc = await admin.firestore().collection("users").doc(teacherId).get();

        if (!teacherDoc.exists) {
            console.error("Teacher not found in Firestore for ID:", teacherId);
            throw new functions.https.HttpsError("not-found", "Teacher does not exist");
        }

        const teacherData = teacherDoc.data();
        const stripeAccountId = teacherData?.stripeAccountId;

        if (!stripeAccountId) {
            console.error("Teacher does not have a connected Stripe account:", teacherId);
            throw new functions.https.HttpsError("failed-precondition", "Stripe account missing");
        }

        const paymentIntent = await stripe.paymentIntents.create({
            amount: parseInt(amount),
            currency: "usd",
            payment_method_types: ["card"],
            application_fee_amount: 1,
            transfer_data: {
                destination: stripeAccountId
            }
        });

        console.log("PaymentIntent created:", paymentIntent.id);

        return {
            clientSecret: paymentIntent.client_secret
        };
    } catch (error) {
        console.error("Error creating PaymentIntent:", error);
        throw new functions.https.HttpsError("internal", error.message);
    }
});
