package org.ed.track.register;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.ed.track.R;
import org.ed.track.databinding.ActivityUpdateProfileBinding;
import org.ed.track.model.UserProfile;
import org.ed.track.utils.App;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpdateProfile extends AppCompatActivity {

    private ActivityUpdateProfileBinding binding;
    private Uri imageUri = null; // To store selected image URI
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateProfileBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        uid = App.getString("user_id");
        if (App.getBoolean("is_teacher")) {
            binding.etteacherBio.setVisibility(View.VISIBLE);
        } else {
            binding.teacherBio.setVisibility(View.GONE);
        }

        binding.etName.setText(App.getString("name"));
        binding.etEmail.setText(App.getString("email"));
        binding.etLocation.setText(App.getString("location"));
        binding.etteacherBio.setText(App.getString("bio"));
        Glide.with(this).load(App.getString("profileImageUrl")).error(R.drawable.baseline_person_24).into(binding.profileImage);

        binding.back.setOnClickListener(view -> {
            onBackPressed();
        });

        // Launcher to get image from gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        binding.profileImage.setImageURI(imageUri);
                    }
                }
        );

        // Launcher for permission
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        binding.profileImage.setOnClickListener(view -> {

            pickImage();
        });

        binding.btnUpdate.setOnClickListener(view -> {
            getDetails();
        });

    }

    private void getDetails() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String location = binding.etLocation.getText().toString().trim();
        String bio = binding.etteacherBio.getText().toString().trim();

        if (App.getString("role").equals("Teacher")){
            binding.etteacherBio.setError(bio.isEmpty() ? "Required" : null);
        }
        if (name.isEmpty() || email.isEmpty() || location.isEmpty()) {
            // Show error or toast
            binding.etName.setError(name.isEmpty() ? "Required" : null);
            binding.etEmail.setError(email.isEmpty() ? "Required" : null);
            binding.etLocation.setError(location.isEmpty() ? "Required" : null);
        } else {
            binding.progressBar.setVisibility(View.VISIBLE);
            updateProfile(name, email, location, bio);
        }


    }

    private void updateProfile(String name, String email, String location, String bio) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("location", location);
        if (App.getBoolean("is_teacher")) {
            updates.put("bio", bio);
        }

        if (imageUri != null) {
            // Upload new image first
            String fileName = "profileImage/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference(fileName);

            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updates.put("imageUrl", uri.toString());
                        Toast.makeText(this, "Image upload done: " + uri, Toast.LENGTH_SHORT).show();

                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }))
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } else {
            // No image selected, just update text fields
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        binding.progressBar.setVisibility(View.GONE);
                        if (App.getString("role").equals("Student"))
                            fetchUserProfile();
                        else
                            fetchTeacherProfile();
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void pickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            // Below Android 13
            requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        imagePickerLauncher.launch(intent);
    }

    private void fetchUserProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.e("student profile data", documentSnapshot.toString());
                    if (documentSnapshot.exists()) {
                        UserProfile user = documentSnapshot.toObject(UserProfile.class);

                        if (user != null) {
                            // Display the data in your UI
                            App.saveString("user_id", userId);

                            App.saveString("name", user.getName());
                            App.saveString("email", user.getEmail());
                            App.saveString("location", user.getLocation());
//                            App.saveString("qualification", user.getQualification());
//                            App.saveString("bio", user.getBio());
//                            App.saveString("phone", user.getPhone());
                            App.saveString("subject", user.getSubject());
                            App.saveString("budget", user.getBudget());
                            App.saveString("profileImageUrl", user.getImageUrl());
                            finish();

                        }
                    } else {
                        Toast.makeText(App.getContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(App.getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchTeacherProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.e("profile data", documentSnapshot.toString());
                    if (documentSnapshot.exists()) {
                        UserProfile user = documentSnapshot.toObject(UserProfile.class);

                        if (user != null) {
                            // Display the data in your UI
                            App.saveString("name", user.getName());
                            App.saveString("email", user.getEmail());
                            App.saveString("location", user.getLocation());
                            App.saveString("qualification", user.getQualification());
                            App.saveString("bio", user.getBio());
                            App.saveString("phone", user.getPhone());
                            App.saveString("profileImageUrl", user.getImageUrl());
                            finish();

                        }
                    } else {
                        Toast.makeText(App.getContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(App.getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileToFirebase(Map<String, Object> profileData) {
        binding.progressBar.setVisibility(View.VISIBLE);
        // Save to Firestore or local DB here
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            return;
        }
        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid)
                .update(profileData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    // Move to next activity or finish
                    binding.progressBar.setVisibility(View.GONE);
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                    App.saveBoolean("is_profile_done", true);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);

                });

    }
}