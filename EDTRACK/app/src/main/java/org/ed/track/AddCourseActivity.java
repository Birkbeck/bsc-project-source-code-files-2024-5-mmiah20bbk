package org.ed.track;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.ed.track.databinding.ActivityAddCourseBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddCourseActivity extends AppCompatActivity {

    private ActivityAddCourseBinding binding;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String[] subjects = {"Mathematics", "English", "Physics", "Chemistry", "Biology", "Computer Science", "History", "Geography", "Economics", "Political Science",
                "Native Android", "Php", "Flutter", "Python"};
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subjects);
        binding.autoCompleteSubject.setAdapter(subjectAdapter);

        String[] availableTimes = {"Morning", "Afternoon", "Evening", "Night"};
        ArrayAdapter<String> availableTimeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, availableTimes);
        binding.autoCompleteAvailableTime.setAdapter(availableTimeAdapter);

        // Launcher to get image from gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        binding.addCourseBanner.setImageURI(selectedImageUri);
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

        binding.addCourseBanner.setOnClickListener(v -> pickImage());

        binding.btnAddCourse.setOnClickListener(v -> checkFields());

        binding.back.setOnClickListener(view -> {
            onBackPressed();
        });

    }

    private void checkFields() {
        String subject = binding.autoCompleteSubject.getText().toString();
        String rate = binding.etCourseRate.getText().toString();
        String availableTime = binding.autoCompleteAvailableTime.getText().toString();
        String details = binding.etCourseDetails.getText().toString();

        if (subject.isEmpty() && rate.isEmpty() && availableTime.isEmpty() && details.isEmpty()) {
            binding.autoCompleteSubject.setError("Required");
            binding.etCourseRate.setError("Required");
            binding.autoCompleteAvailableTime.setError("Required");
            binding.etCourseDetails.setError("Required");
        } else if (subject.isEmpty()){
            binding.autoCompleteSubject.setError("Required");

            // Proceed with adding the course to the database
        } else if (rate.isEmpty()) {
            binding.etCourseRate.setError("Required");

        } else if (availableTime.isEmpty()) {
            binding.autoCompleteAvailableTime.setError("Required");

        } else if (details.isEmpty()) {
            binding.etCourseDetails.setError("Required");

        } else {
            // Proceed with adding the course to the database

            binding.progressBar.setVisibility(View.VISIBLE);
            uploadImageAndSaveCourse(subject, rate, availableTime, details);
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

    private void uploadImageAndSaveCourse(String courseTitle, String price, String availableTime, String courseDescription) {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String fileName = "courses/" + userId + "/" + UUID.randomUUID().toString() + ".jpg";

        StorageReference storageRef = FirebaseStorage.getInstance().getReference(fileName);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();

                    // Save course data to Firestore
                    saveCourseToFirestore(courseTitle, price, availableTime, courseDescription, imageUrl, userId);

                }))
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);

                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveCourseToFirestore(String title, String price, String availableTime, String description, String imageUrl, String teacherId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> courseData = new HashMap<>();
        courseData.put("title", title);
        courseData.put("description", description);
        courseData.put("price", price);
        courseData.put("availableTime", availableTime);
        courseData.put("imageUrl", imageUrl);
        courseData.put("teacherId", teacherId);
        courseData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("courses")
                .add(courseData)
                .addOnSuccessListener(documentReference -> {
                    binding.progressBar.setVisibility(View.GONE);

                    finish();
                    Toast.makeText(this, "Course added!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);

                    Toast.makeText(this, "Failed to add course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}