package org.ed.track;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import org.ed.track.databinding.ActivityDashBoardBinding;
import org.ed.track.fragments.CalendarFragment;
import org.ed.track.fragments.HomeFragment;
import org.ed.track.fragments.MessagesFragment;
import org.ed.track.fragments.ProfileFragment;
import org.ed.track.model.UserProfile;
import org.ed.track.utils.App;

public class DashBoardActivity extends AppCompatActivity {

    private ActivityDashBoardBinding binding;
    private Fragment currentFragment;
    private long backPressedTime;
    private Toast backToast;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashBoardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            currentFragment = new HomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, currentFragment).commit();
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_messages) {
                selectedFragment = new MessagesFragment();
            }
//            else if (item.getItemId() == R.id.nav_calendar) {
//                selectedFragment = new CalendarFragment();
//            }
            else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null && !selectedFragment.getClass().equals(currentFragment.getClass())) {
                currentFragment = selectedFragment;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, currentFragment).commit();
            }
            return true;
        });

        // backpress functionality
        // Back pressed handling using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!(currentFragment instanceof HomeFragment)) {
                    // Navigate to home if not already on home
                    currentFragment = new HomeFragment();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, currentFragment).commit();
                    binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
                } else {
                    // Exit app if already on home
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        if (backToast != null) backToast.cancel();
                        finish(); // Or requireActivity().finish()
                    } else {
                        backToast = Toast.makeText(DashBoardActivity.this, "Press back again to exit", Toast.LENGTH_SHORT);
                        backToast.show();
                        backPressedTime = System.currentTimeMillis();
                    }
                }
            }
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
                            App.saveString("profile_image", user.getImageUrl());
                        }
                    } else {
                        Toast.makeText(App.getContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(App.getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    Log.e("tokeen", token);
//                    Toast.makeText(MainActivity.this, "token: " + token, Toast.LENGTH_SHORT).show();

                    Log.e("token user", App.getString("user_id"));
                    saveTokenToFireStore(App.getString("user_id"), token);

                }
            }
        });
    }

    // Assume 'userId' is the user's unique ID and 'token' is the FCM token
    private void saveTokenToFireStore(String userId, String token) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Update the token field for the user
        db.collection("users").document(userId)
                .update("token", token)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TAG", "Token saved successfully for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG", "Failed to save token for user: " + userId, e);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
//        fetchUserProfile();
        getFCMToken();
    }
}