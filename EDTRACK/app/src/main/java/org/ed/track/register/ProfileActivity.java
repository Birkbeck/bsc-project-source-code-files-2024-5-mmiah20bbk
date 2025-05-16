package org.ed.track.register;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.ed.track.DashBoardActivity;
import org.ed.track.databinding.ActivityProfileBinding;
import org.ed.track.utils.App;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    // to check the gallery permissions and pic the image
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;
    private String name, email, password, role, phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        name = getIntent().getStringExtra("name");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");
        role = getIntent().getStringExtra("role");
        phone = getIntent().getStringExtra("phone");

        String[] subjects = {"Mathematics", "English", "Physics", "Chemistry", "Biology", "Computer Science", "History", "Geography", "Economics", "Political Science",
                "Native Android", "Php", "Flutter", "Python"};
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subjects);
        binding.autoCompleteSubject.setAdapter(subjectAdapter);

        String[] budgets = {"< 5000", "5000 - 10000", "10000 - 15000", "> 15000"};
        ArrayAdapter<String> budgetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, budgets);
        binding.autoCompleteBudget.setAdapter(budgetAdapter);

        String[] availableTimes = {"Morning", "Afternoon", "Evening", "Night"};
        ArrayAdapter<String> availableTimeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, availableTimes);
        binding.autoCompleteAvailableTime.setAdapter(availableTimeAdapter);

        String[] qualifications = {"High School Diploma", "Bachelor's Degree", "Master's Degree", "PhD"};
        ArrayAdapter<String> qualificationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, qualifications);
        binding.autoCompleteQualification.setAdapter(qualificationAdapter);

        binding.btnContinue.setOnClickListener(v -> checkFields());

        binding.back.setOnClickListener(view -> {
            onBackPressed();
        });

        if (App.getBoolean("is_teacher")) {
            binding.layoutSubject.setVisibility(View.GONE);
            binding.layoutBudget.setVisibility(View.GONE);
        } else {
            binding.layoutAvailableTime.setVisibility(View.GONE);
            binding.layoutQualification.setVisibility(View.GONE);
            binding.teacherBio.setVisibility(View.GONE);
        }

        // Launcher to get image from gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        binding.profileImage.setImageURI(selectedImageUri);
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
    }

    private void checkFields() {
        // common field
        String location = binding.etLocation.getText().toString();

        // student specific data
        String subject = binding.autoCompleteSubject.getText().toString();
        String budget = binding.autoCompleteBudget.getText().toString();

        // teacher specific details
        String availableTime = binding.autoCompleteAvailableTime.getText().toString();
        String qualification = binding.autoCompleteQualification.getText().toString();
        String bio = binding.etteacherBio.getText().toString();

        Intent intent = new Intent(this, PhoneVerificationActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        intent.putExtra("role", role);
        intent.putExtra("phone", phone);

        if (App.getBoolean("is_teacher")) {
            if (availableTime.isEmpty() || qualification.isEmpty() || bio.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            intent.putExtra("availableTime", availableTime);
            intent.putExtra("qualification", qualification);
            intent.putExtra("bio", bio);

        } else {

            if (subject.isEmpty() || budget.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            intent.putExtra("subject", subject);
            intent.putExtra("budget", budget);

        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
        } else if (location.isEmpty()) {
            binding.etLocation.setError("Required");
        } else {
            intent.putExtra("image_uri", selectedImageUri.toString());
            intent.putExtra("location", location);
            startActivity(intent);
        }

    }


    private void saveMoreData() {
        // common field
        String location = binding.etLocation.getText().toString();

        // student specific data
        String subject = binding.autoCompleteSubject.getText().toString();
        String budget = binding.autoCompleteBudget.getText().toString();

        // teacher specific details
        String availableTime = binding.autoCompleteAvailableTime.getText().toString();
        String qualification = binding.autoCompleteQualification.getText().toString();
        String bio = binding.etteacherBio.getText().toString();

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("location", location);

        if (App.getBoolean("is_teacher")) {
            if (availableTime.isEmpty() || qualification.isEmpty() || bio.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            profileData.put("availableTime", availableTime);
            profileData.put("qualification", qualification);
            profileData.put("bio", bio);

            // put the data into the profile
//            updateProfileToFirebase(profileData);
        } else {

            if (subject.isEmpty() || location.isEmpty() || budget.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            profileData.put("subject", subject);
            profileData.put("budget", budget);

            // put the data into the profile
//            updateProfileToFirebase(profileData);

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