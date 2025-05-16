package org.ed.track.register;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.ed.track.DashBoardActivity;
import org.ed.track.databinding.ActivityPhoneVerificationBinding;
import org.ed.track.model.UserProfile;
import org.ed.track.student.StudentDashboard;
import org.ed.track.utils.App;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {

    private ActivityPhoneVerificationBinding binding;

    private FirebaseAuth mAuth;
    private String verificationId, name, email, password, subject, role, phone, budget, availbeTime,
    qualification, bio, location;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        if (!getIntent().getBooleanExtra("is_login", false)) {
            if (App.getBoolean("is_teacher")) {
                availbeTime = getIntent().getStringExtra("availableTime");
                qualification = getIntent().getStringExtra("qualification");
                bio = getIntent().getStringExtra("bio");
            } else {
                subject = getIntent().getStringExtra("subject");
                budget = getIntent().getStringExtra("budget");
            }
            name = getIntent().getStringExtra("name");
            email = getIntent().getStringExtra("email");
            password = getIntent().getStringExtra("password");
            role = getIntent().getStringExtra("role");
            phone = getIntent().getStringExtra("phone");
            location = getIntent().getStringExtra("location");
            imageUri = Uri.parse(getIntent().getStringExtra("image_uri"));
            Log.e("phon", "mobile: " + phone);
        } else {
            phone = getIntent().getStringExtra("phone");
            Log.e("phon", "mobile: " + phone);
        }

//        binding.btnGetOtp.setOnClickListener(v -> );
        sendVerificationCode();
        binding.btnVerify.setOnClickListener(v -> verifyCode());

        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void sendVerificationCode() {


        if (phone.isEmpty() || phone.length() < 10) {

            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(30L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {

                    signInWithCredential(credential);
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    binding.progressBar.setVisibility(View.GONE);

                    Log.e("error", "onVerificationFailed: "+ e.getMessage());
                    Toast.makeText(PhoneVerificationActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    super.onCodeSent(id, token);
                    binding.progressBar.setVisibility(View.GONE);

                    verificationId = id;
                    Toast.makeText(PhoneVerificationActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyCode() {
        String code = binding.etOtp.getText().toString().trim();
        if (code.isEmpty()) {
            binding.etOtp.setError("Enter OTP");
            binding.etOtp.requestFocus();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Phone number verified!
                        // Navigate to registration details screen
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Verified: " + user.getPhoneNumber(), Toast.LENGTH_SHORT).show();
                        if (!getIntent().getBooleanExtra("is_login", false))
                            uploadImageAndSaveCourse();
                        else {
                            fetchUserProfileForLogin(user.getPhoneNumber());
                        }
                        // Example: startActivity(new Intent(this, RegistrationDetailsActivity.class));
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Verification failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageAndSaveCourse() {
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String fileName = "profileImage/" + UUID.randomUUID().toString() + ".jpg";

        StorageReference storageRef = FirebaseStorage.getInstance().getReference(fileName);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();

                    // Save course data to Firestore
                    saveToFirebase(imageUrl);

                }))
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);

                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToFirebase(String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String uid = user.getUid(); // unique ID from phone auth

            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("email", email);
            userData.put("password", password); // Consider encrypting
            userData.put("role", role);
            userData.put("phone", phone);
            userData.put("imageUrl", imageUrl);
            userData.put("location", location);

            if (App.getBoolean("is_teacher")) {
                userData.put("availableTime", availbeTime);
                userData.put("qualification", qualification);
                userData.put("bio", bio);
            } else {
                userData.put("subject", subject);
                userData.put("budget", budget);
            }

            db.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {

                        // Navigate to next screen
                        Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show();
                        App.saveLogin(true);
                        fetchUserProfileForLogin(phone);

                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);

                        Toast.makeText(this, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

    }

    private void fetchUserProfileForLogin(String phone) {

        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot userDoc : snapshot.getDocuments()) {
                        Log.e("USER", "Phone: " + userDoc.getString("phone") + "name: " +
                                userDoc.getString("name") + " ID: " + userDoc.getId());
                        if (userDoc.getString("phone").equals(phone)) {
                            Toast.makeText(App.getContext(), "User found", Toast.LENGTH_SHORT).show();
                            App.saveLogin(true);
                            App.saveString("user_id", userDoc.getId());
                            App.saveString("phone", phone);
                            App.saveString("password", password);
                            App.saveString("role", userDoc.getString("role"));
                            App.saveString("name", userDoc.getString("name"));
                            App.saveString("email", userDoc.getString("email"));
                            App.saveString("location", userDoc.getString("location"));
                            App.saveString("profileImageUrl", userDoc.getString("imageUrl"));
                            if (App.getString("role").equals("Teacher")) {
                                App.saveBoolean("is_teacher", true);
                                App.saveString("qualification", userDoc.getString("qualification"));
                                App.saveString("bio", userDoc.getString("bio"));
                                App.saveString("availableTime", userDoc.getString("availableTime"));
                                startActivity(new Intent(this, DashBoardActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                                finish();
                            } else {
                                App.saveBoolean("is_teacher", false);
                                App.saveString("budget", userDoc.getString("budget"));
                                App.saveString("subject", userDoc.getString("subject"));
                                startActivity(new Intent(this, StudentDashboard.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                                finish();
                            }
                            binding.progressBar.setVisibility(View.GONE);

                        }
                    }
                }).addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);

                    Toast.makeText(App.getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

//        Log.e("verified numb", phone);
//        FirebaseFirestore.getInstance().collection("users")
//                .whereEqualTo("phone", phone.trim())
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    Log.e("profile data", documentSnapshot.toString() + " ,.,., "+documentSnapshot.size());
//                    if (!documentSnapshot.isEmpty()) {
//
//                        DocumentSnapshot userDoc = documentSnapshot.getDocuments().get(0);
//                        String userId = userDoc.getId();
//                        Log.d("Login", "Login successful. User ID: " + userId);
//
//                        App.saveLogin(true);
//                        App.saveString("user_id", userId);
//                        App.saveString("phone", phone);
//                        App.saveString("password", password);
//                        App.saveString("role", userDoc.getString("role"));
//                        App.saveString("name", userDoc.getString("name"));
//                        App.saveString("email", userDoc.getString("email"));
//                        App.saveString("location", userDoc.getString("location"));
//                        App.saveString("profileImageUrl", userDoc.getString("profileImageUrl"));
//                        if (App.getString("role").equals("Teacher")) {
//                            App.saveBoolean("is_teacher", true);
//                            App.saveString("qualification", userDoc.getString("qualification"));
//                            App.saveString("bio", userDoc.getString("bio"));
//                            App.saveString("availableTime", userDoc.getString("availableTime"));
//                            startActivity(new Intent(this, DashBoardActivity.class));
//
//                        } else {
//                            App.saveBoolean("is_teacher", false);
//                            App.saveString("budget", userDoc.getString("budget"));
//                            App.saveString("subject", userDoc.getString("subject"));
//                            startActivity(new Intent(this, StudentDashboard.class));
//
//                        }
//                    } else {
//                        Toast.makeText(App.getContext(), "User profile not found", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(App.getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
    }

}